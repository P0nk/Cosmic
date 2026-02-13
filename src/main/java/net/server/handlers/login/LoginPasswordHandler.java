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
import client.LoginState;
import config.YamlConfig;
import constants.game.GameConstants;
import database.account.Account;
import net.PacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.coordinator.session.Hwid;
import net.server.coordinator.session.SessionCoordinator;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.AccountService;
import service.BanService;
import service.TransitionService;
import tools.BCrypt;
import tools.HexTool;
import tools.PacketCreator;

import java.util.Objects;
import java.util.Optional;

public final class LoginPasswordHandler implements PacketHandler {
    private static final Logger log = LoggerFactory.getLogger(LoginPasswordHandler.class);

    private final AccountService accountService;
    private final TransitionService transitionService;
    private final BanService banService;

    public LoginPasswordHandler(AccountService accountService, TransitionService transitionService,
                                BanService banService) {
        this.accountService = accountService;
        this.transitionService = transitionService;
        this.banService = banService;
    }

    @Override
    public boolean validateState(Client c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        String remoteHost = c.getRemoteAddress();
        if (remoteHost.contentEquals("null")) {
            c.sendPacket(PacketCreator.getLoginFailed(14));          // thanks Alchemist for noting remoteHost could be null
            return;
        }

        String login = p.readString();
        String pwd = p.readString();

        p.skip(6);   // localhost masked the initial part with zeroes...
        byte[] hwidNibbles = p.readBytes(4);
        c.setHwid(new Hwid(HexTool.toCompactHexString(hwidNibbles)));

        if (!c.attemptLogin()) {
            c.sendPacket(PacketCreator.getLoginFailed(10));
            SessionCoordinator.getInstance().closeSession(c, false);
            return;
        }

        Optional<Account> foundAccount = accountService.getAccount(login);
        if (foundAccount.isEmpty()) {
            if (YamlConfig.config.server.AUTOMATIC_REGISTER) {
                Account newAccount = accountService.createAccount(login, pwd);
                foundAccount = Optional.of(newAccount);
            } else {
                c.sendPacket(PacketCreator.getLoginFailed(5));
                return;
            }
        }

        Account account = foundAccount.get();

        if (!correctPassword(pwd, account)) {
            c.sendPacket(PacketCreator.getLoginFailed(4));
            return;
        }

        if (account.banned()) {
            byte banReason = Objects.requireNonNullElse(account.banReason(), (byte) 0);
            if (account.bannedUntil() != null) {
                c.sendPacket(PacketCreator.getTempBan(banReason, account.bannedUntil().toEpochMilli()));
            } else {
                c.sendPacket(PacketCreator.getPermBan(banReason));
            }
            return;
        }

        if (banService.isBanned(c)) {
            c.sendPacket(PacketCreator.getLoginFailed(3));
            return;
        }

        if (account.loginState() != LoginState.LOGGED_OUT) {
            c.sendPacket(PacketCreator.getLoginFailed(7));
            return;
        }

        Integer failureCode = checkMultiClient(c);
        if (failureCode != null) {
            c.sendPacket(PacketCreator.getLoginFailed(failureCode));
            return;
        }

        c.setAccount(account);

        if (!account.acceptedTos()) {
            c.sendPacket(PacketCreator.getLoginFailed(23));
            return;
        }

        if (!accountService.setLoggedIn(c)) {
            c.sendPacket(PacketCreator.getLoginFailed(7));
        }
        removeOnlineAccountChrs(c);

        c.sendPacket(PacketCreator.getAuthSuccess(c));
        Server.getInstance().registerLoginState(c);
    }

    private boolean correctPassword(String input, Account account) {
        return BCrypt.checkpw(input, account.password());
    }

    private Integer checkMultiClient(Client c) {
        SessionCoordinator.AntiMulticlientResult res = SessionCoordinator.getInstance()
                .attemptLoginSession(c, c.getHwid(), c.getAccID(), false);

        return switch (res) {
            case SUCCESS -> null;
            case REMOTE_LOGGEDIN -> 17;
            case REMOTE_REACHED_LIMIT -> 13;
            case REMOTE_PROCESSING -> 10;
            case MANY_ACCOUNT_ATTEMPTS -> 16;
            default -> 8;
        };
    }

    private void removeOnlineAccountChrs(Client c) { // issue with multiple chars from same account login found by shavit, resinate
        if (!YamlConfig.config.server.USE_CHARACTER_ACCOUNT_CHECK) {
            return;
        }

        final int accId = c.getAccID();
        for (World w : Server.getInstance().getWorlds()) {
            for (Character chr : w.getPlayerStorage().getAllCharacters()) {
                if (accId == chr.getAccountID()) {
                    log.warn("Chr {} has been removed from world {}. Possible Dupe attempt.", chr.getName(), GameConstants.WORLD_NAMES[w.getId()]);
                    transitionService.forceDisconnect(c);
                    w.getPlayerStorage().removePlayer(chr.getId());
                }
            }
        }
    }
}
