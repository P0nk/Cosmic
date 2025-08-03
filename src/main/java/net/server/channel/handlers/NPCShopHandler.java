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
import constants.inventory.ItemConstants;
import net.AbstractPacketHandler;
import net.netty.GameViolationException;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Matze
 */
public final class NPCShopHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(NPCShopHandler.class);

    @Override
    public void handlePacket(InPacket p, Client c) {
        byte bmode = p.readByte();
        switch (bmode) {
        case 0: { // mode 0 = buy :)
            short slot = p.readShort();// slot
            int itemId = p.readInt();
            short quantity = p.readShort();
            if (quantity < 1) {
                AutobanFactory.PACKET_EDIT.alert(c.getPlayer(),
                        c.getPlayer().getName() + " tried to packet edit a npc shop.");
                log.warn("Chr {} tried to buy quantity {} of itemid {}", c.getPlayer().getName(), quantity, itemId);
                throw new GameViolationException("Buy invalid amount of items");
            }
            c.getPlayer().getShop().buy(c, slot, itemId, quantity);
            break;
        }
        case 1: { // sell ;)
            short slot = p.readShort();
            int itemId = p.readInt();
            short quantity = p.readShort();
            c.getPlayer().getShop().sell(c, ItemConstants.getInventoryType(itemId), slot, quantity);
            break;
        }
        case 2: { // recharge ;)

            byte slot = (byte) p.readShort();
            c.getPlayer().getShop().recharge(c, slot);
            break;
        }
        case 3: // leaving :(
            c.getPlayer().setShop(null);
            break;
        }

    }
}
