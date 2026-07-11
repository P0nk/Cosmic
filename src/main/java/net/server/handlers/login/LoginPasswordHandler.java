/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.handlers.login;

import client.Client;
import client.Character;
import client.DefaultDates;
import config.YamlConfig;
import constants.net.ServerConstants;
import net.PacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.coordinator.session.Hwid;
import net.server.coordinator.session.SessionCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.maps.MapleMap;
import tools.*;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Calendar;

public final class LoginPasswordHandler implements PacketHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginPasswordHandler.class);

    @Override
    public boolean validateState(Client c) {
        return !c.isLoggedIn();
    }

    private static String hashpwSHA512(String pwd) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digester = MessageDigest.getInstance("SHA-512");
        digester.update(pwd.getBytes(StandardCharsets.UTF_8), 0, pwd.length());
        return HexTool.toHexString(digester.digest()).replace(" ", "").toLowerCase();
    }

    @Override
    public final void handlePacket(InPacket p, Client c) {
        String remoteHost = c.getRemoteAddress();
        if (remoteHost.contentEquals("null")) {
            c.sendPacket(PacketCreator.getLoginFailed(14));          // thanks Alchemist for noting remoteHost could be null
            return;
        }



        String login = p.readString();
        String pwd = p.readString();
        c.setAccountName(login);

        p.skip(6);   // localhost masked the initial part with zeroes...
        byte[] hwidNibbles = p.readBytes(4);
        Hwid hwid = new Hwid(HexTool.toCompactHexString(hwidNibbles));
        int loginok = c.login(login, pwd, hwid, false);
        if (YamlConfig.config.server.AUTOMATIC_REGISTER && loginok == 5) {
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("INSERT INTO accounts (name, password, birthday, tempban) VALUES (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS)) { //Jayd: Added birthday, tempban
                ps.setString(1, login);
                ps.setString(2, YamlConfig.config.server.BCRYPT_MIGRATION ? BCrypt.hashpw(pwd, BCrypt.gensalt(12)) : hashpwSHA512(pwd));
                ps.setDate(3, Date.valueOf(DefaultDates.getBirthday()));
                ps.setTimestamp(4, Timestamp.valueOf(DefaultDates.getTempban()));
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    c.setAccID(rs.getInt(1));
                }

                try {
                    DiscordWebhook webhookMessage = getDiscordRegistrationWebhookMessage(c, login, remoteHost);
                    webhookMessage.execute();
                } catch (IOException e) {
                    //log.error("Failed to notify discord via webhook for new registration.");
                }
            } catch (SQLException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
                c.setAccID(-1);
                e.printStackTrace();
            } finally {
                loginok = c.login(login, pwd, hwid, false);
            }
        }

        if (loginok == 7) { // player already logged in (but pw is already verified)
            try (Connection con = DatabaseConnection.getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE name = ?")) {
                    ps.setString(1, login);
                    System.out.println(ps.executeUpdate());
                    loginok = 0;
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT `name` FROM characters WHERE accountid = ?")) {
                    ps.setInt(1, c.getAccID());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String name = rs.getString("name");
                            System.out.println("[LoginPasswordHandler] Looked into character: " + name);
                            Character player = Server.getInstance().getWorld(0).getPlayerStorage().getCharacterByName(name);
                            if (player != null) {
                                MapleMap map = player.getMap();
                                player.getChannel().removePlayer(player);
                                map.removePlayer(player);
                                player.getClient().disconnect(false, false);
                                System.out.println("[LoginPasswordHandler] Unstucked: " + name);
                                break; // only 1 character should be stuck... otherwise its really fucked
                            }
                        }
                    }
                }

                Client oldClient = Server.getInstance().getConnectedClient(login);
                if (oldClient != null) {
                    System.out.println("removing old client");
                    oldClient.disconnect(false, false);
                    Server.getInstance().unregisterLoginState(oldClient);
                    Server.getInstance().unregisterClient(login);
                }
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }
        }

        if (c.autolog > 0) {
            loginok = 0;
        }

        if (YamlConfig.config.server.BCRYPT_MIGRATION && (loginok <= -10)) { // -10 means migration to bcrypt, -23 means TOS wasn't accepted
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE accounts SET password = ? WHERE name = ?;")) {
                ps.setString(1, BCrypt.hashpw(pwd, BCrypt.gensalt(12)));
                ps.setString(2, login);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                loginok = (loginok == -10) ? 0 : 23;
            }
        }

        if (c.hasBannedIP() || c.hasBannedMac()) {
            c.sendPacket(PacketCreator.getLoginFailed(3));
            return;
        }
        Calendar tempban = c.getTempBanCalendarFromDB();
        if (tempban != null) {
            if (tempban.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
                c.sendPacket(PacketCreator.getTempBan(tempban.getTimeInMillis(), c.getGReason()));
                return;
            }
        }
        if (loginok == 3) {
            c.sendPacket(PacketCreator.getPermBan(c.getGReason()));//crashes but idc :D
            return;
        } else if (loginok != 0) {
            c.sendPacket(PacketCreator.getLoginFailed(loginok));
            SessionCoordinator.getInstance().closeLoginSession(c);
            return;
        }
        if (c.finishLogin() == 0) {
            c.checkChar(c.getAccID());
            login(c);
        } else {
            c.sendPacket(PacketCreator.getLoginFailed(7));
        }
    }

    private static DiscordWebhook getDiscordRegistrationWebhookMessage(Client c, String login, String remoteHost) {
        DiscordWebhook webhookMessage = new DiscordWebhook(ServerConstants.REGISTRATION_URL);
        DiscordWebhook.EmbedObject messageEmbed = new DiscordWebhook.EmbedObject()
                .setTitle("New Account Registration")
                .setColor(ServerConstants.EMBED_COLOR)
                .addField("AccountId", "" + c.getAccID(), false)
                .addField("Username", login, false)
                .addField("RemoteHost", remoteHost, false);;

        webhookMessage.addEmbed(messageEmbed);
        return webhookMessage;
    }

    private static void login(Client c) {
        c.sendPacket(PacketCreator.getAuthSuccess(c));
        Server.getInstance().registerLoginState(c);
        Server.getInstance().registerClient(c);
    }
}
