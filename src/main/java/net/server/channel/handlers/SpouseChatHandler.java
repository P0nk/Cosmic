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
package net.server.channel.handlers;

import client.Character;
import client.Client;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.ChatLogger;
import tools.PacketCreator;

public final class SpouseChatHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        p.readString();//recipient
        String msg = p.readString();

        int partnerId = c.getPlayer().getPartnerId();
        if (partnerId > 0) { // yay marriage
            Character spouse = c.getWorldServer().getPlayerStorage().getCharacterById(partnerId);
            if (spouse != null) {
                spouse.sendPacket(PacketCreator.OnCoupleMessage(c.getPlayer().getName(), msg, true));
                c.sendPacket(PacketCreator.OnCoupleMessage(c.getPlayer().getName(), msg, true));
                ChatLogger.log(c, "Spouse", msg);
            } else {
                c.getPlayer().dropMessage(5, "Your spouse is currently offline.");
            }
        } else {
            c.getPlayer().dropMessage(5, "You don't have a spouse.");
        }
    }
}