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
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import config.YamlConfig;
import net.AbstractPacketHandler;
import net.netty.GameViolationException;
import net.packet.InPacket;
import net.server.Server;
import server.ItemInformationProvider;
import tools.PacketCreator;

public final class InventoryMergeHandler extends AbstractPacketHandler {

    @Override
    public final void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        p.readInt();
        chr.getAutobanManager().setTimestamp(2, Server.getInstance().getCurrentTimestamp(), 4);

        if (!YamlConfig.config.server.USE_ITEM_SORT) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        byte invType = p.readByte();
        if (invType < 1 || invType > 5) {
            throw GameViolationException.inventoryType(invType);
        }

        InventoryType inventoryType = InventoryType.getByType(invType);
        Inventory inventory = c.getPlayer().getInventory(inventoryType);
        inventory.lockInventory();
        try {
            //------------------- RonanLana's SLOT MERGER -----------------

            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            Item srcItem, dstItem;

            for (short dst = 1; dst <= inventory.getSlotLimit(); dst++) {
                dstItem = inventory.getItem(dst);
                if (dstItem == null) {
                    continue;
                }

                for (short src = (short) (dst + 1); src <= inventory.getSlotLimit(); src++) {
                    srcItem = inventory.getItem(src);
                    if (srcItem == null) {
                        continue;
                    }

                    if (dstItem.getItemId() != srcItem.getItemId()) {
                        continue;
                    }
                    if (dstItem.getQuantity() == ii.getSlotMax(c, inventory.getItem(dst).getItemId())) {
                        break;
                    }

                    InventoryManipulator.move(c, inventoryType, src, dst);
                }
            }

            //------------------------------------------------------------

            inventory = c.getPlayer().getInventory(inventoryType);
            boolean sorted = false;

            while (!sorted) {
                short freeSlot = inventory.getNextFreeSlot();

                if (freeSlot != -1) {
                    short itemSlot = -1;
                    for (short i = (short) (freeSlot + 1); i <= inventory.getSlotLimit(); i = (short) (i + 1)) {
                        if (inventory.getItem(i) != null) {
                            itemSlot = i;
                            break;
                        }
                    }
                    if (itemSlot > 0) {
                        InventoryManipulator.move(c, inventoryType, itemSlot, freeSlot);
                    } else {
                        sorted = true;
                    }
                } else {
                    sorted = true;
                }
            }
        } finally {
            inventory.unlockInventory();
        }

        c.sendPacket(PacketCreator.finishedSort(inventoryType.getType()));
        c.sendPacket(PacketCreator.enableActions());
    }
}
