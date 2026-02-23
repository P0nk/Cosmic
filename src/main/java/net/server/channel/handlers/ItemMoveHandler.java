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
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import tools.PacketCreator;

/**
 * @author Matze
 */
public final class ItemMoveHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        p.skip(4);
        if (c.getPlayer().getAutobanManager().getLastSpam(6) + 300 > currentServerTime()) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        InventoryType type = InventoryType.getByType(p.readByte());
        short src = p.readShort(); // is there any reason to use byte instead of short in src and action?
        short action = p.readShort();
        short quantity = p.readShort();

        if (src < 0 && action > 0) {
            InventoryManipulator.unequip(c, src, action);
        } else if (action < 0) {
            InventoryManipulator.equip(c, src, action);
        } else if (action == 0) {

            InventoryManipulator.drop(c, type, src, quantity);
        } else {
            InventoryManipulator.move(c, type, src, action);
        }

        c.getPlayer().getAutobanManager().spam(6);
    }
}