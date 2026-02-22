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
import client.processor.npc.DueyProcessor;
import config.YamlConfig;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import tools.PacketCreator;

public final class DueyHandler extends AbstractPacketHandler {

    @Override
    public final void handlePacket(InPacket p, Client c) {
        if (!YamlConfig.config.server.USE_DUEY) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        if (c.getPlayer().getWorld() == 1) {
            c.getPlayer().message("Duey Deliveries are disabled in this world.");
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        byte operation = p.readByte();
        if (operation == DueyProcessor.Actions.TOSERVER_RECV_ITEM.getCode()) { // on click 'O' Button, thanks inhyuk
            DueyProcessor.dueySendTalk(c, false);
        } else if (operation == DueyProcessor.Actions.TOSERVER_SEND_ITEM.getCode()) {
            byte inventId = p.readByte();
            short itemPos = p.readShort();
            short amount = p.readShort();
            int mesos = p.readInt();
            String recipient = p.readString();
            boolean quick = p.readByte() != 0;
            String message = quick ? p.readString() : null;

            DueyProcessor.dueySendItem(c, inventId, itemPos, amount, mesos, message, recipient, quick);
        } else if (operation == DueyProcessor.Actions.TOSERVER_REMOVE_PACKAGE.getCode()) {
            int packageid = p.readInt();

            DueyProcessor.dueyRemovePackage(c, packageid, true);
        } else if (operation == DueyProcessor.Actions.TOSERVER_CLAIM_PACKAGE.getCode()) {
            int packageid = p.readInt();

            DueyProcessor.dueyClaimPackage(c, packageid);
        } else if (operation == DueyProcessor.Actions.TOSERVER_CLAIM_PACKAGE.getCode()) {
            DueyProcessor.dueySendTalk(c, false);
        }
    }
}
