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
import client.Gender;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import service.AccountService;
import tools.PacketCreator;

/**
 * @author kevintjuh93
 * @author Ponk
 */
public class SetGenderHandler extends AbstractPacketHandler {
    private final AccountService accountService;

    public SetGenderHandler(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        if (c.getGender() != Gender.NOT_SET) { // Packet shouldn't come if Gender isn't 10.
            logOut(c);
            return;
        }

        byte confirmed = p.readByte();
        if (confirmed != 0x01) {
            logOut(c);
            return;
        }

        byte gender = p.readByte();
        if (gender != Gender.MALE && gender != Gender.FEMALE) {
            logOut(c);
            return;
        }

        if (!accountService.setGender(c.getAccID(), gender)) {
            logOut(c);
            return;
        }

        c.setGender(gender);
        c.sendPacket(PacketCreator.getAuthSuccess(c));
        Server.getInstance().registerLoginState(c);
    }

    private void logOut(Client c) {
        accountService.setLoggedOutAndDisconnect(c);
    }

}
