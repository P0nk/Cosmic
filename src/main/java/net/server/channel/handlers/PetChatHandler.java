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

import client.Client;
import client.autoban.AutobanFactory;
import net.AbstractPacketHandler;
import net.netty.GameViolationException;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ChatLogger;
import tools.PacketCreator;

public final class PetChatHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(PetChatHandler.class);

    @Override
    public void handlePacket(InPacket p, Client c) {
        int petId = p.readInt();
        p.readInt();
        p.readByte();
        int act = p.readByte();
        byte pet = c.getPlayer().getPetIndex(petId);
        if ((pet < 0 || pet > 3) || (act < 0 || act > 9)) {
            return;
        }
        String text = p.readString();
        if (text.length() > Byte.MAX_VALUE) {
            AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit with pets.");
            log.warn("Chr {} tried to send text with length of {}", c.getPlayer().getName(), text.length());
            throw GameViolationException.textLength(text);
        }

        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PacketCreator.petChat(c.getPlayer().getId(), pet, act, text), true);
        ChatLogger.log(c, "Pet", text);
    }
}
