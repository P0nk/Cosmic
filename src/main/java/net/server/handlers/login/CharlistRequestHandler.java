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

import client.Character;
import client.Client;
import model.CharacterIdentity;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.packet.Packet;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CharlistRequestHandler extends AbstractPacketHandler {

    @Override
    public final void handlePacket(InPacket p, Client c) {
        p.readByte();
        int world = p.readByte();

        World wserv = Server.getInstance().getWorld(world);
        if (wserv == null || wserv.isWorldCapacityFull()) {
            c.sendPacket(PacketCreator.getServerStatus(2));
            return;
        }

        int channel = p.readByte() + 1;
        Channel ch = wserv.getChannel(channel);
        if (ch == null) {
            c.sendPacket(PacketCreator.getServerStatus(2));
            return;
        }

        c.setWorld(world);
        c.setChannel(channel);
        sendChrList(c, world);
    }

    private void sendChrList(Client c, int worldId) {
        List<Character> chrs = loadChrs(c, worldId);
        Packet charListPacket = PacketCreator.getCharList(c, chrs, 0);
        c.sendPacket(charListPacket);
    }

    private List<Character> loadChrs(Client c, int worldId) {
        List<Character> chars = new ArrayList<>();
        try {
            for (CharacterIdentity identity : loadChrIdentities(c.getAccID(), worldId)) {
                chars.add(Character.loadCharFromDB(identity.id(), c, false));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chars;
    }

    private List<CharacterIdentity> loadChrIdentities(int accountId, int worldId) {
        List<CharacterIdentity> chars = new ArrayList<>(15);
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ?")) {
            ps.setInt(1, accountId);
            ps.setInt(2, worldId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    chars.add(new CharacterIdentity(rs.getString("name"), rs.getInt("id")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chars;
    }
}
