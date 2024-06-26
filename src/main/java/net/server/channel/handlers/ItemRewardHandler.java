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
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import constants.inventory.ItemConstants;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.ItemInformationProvider;
import server.ItemInformationProvider.RewardItem;
import tools.PacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.util.List;

/**
 * @author Jay Estrella
 * @author kevintjuh93
 */
public final class ItemRewardHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        byte slot = (byte) p.readShort();
        int itemId = p.readInt(); // will load from xml I don't care.

        Item it = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);   // null check here thanks to Thora
        if (it == null || it.getItemId() != itemId || c.getPlayer().getInventory(InventoryType.USE).countById(itemId) < 1) {
            return;
        }

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Pair<Integer, List<RewardItem>> rewards = ii.getItemReward(itemId);

        // Check that ALL inventories have at least 1 free slot.. otherwise this can be exploited
        for (byte i = 1; i <= 4; i++) {
            if (c.getPlayer().getInventory(InventoryType.getByType(i)).isFull()) {
                c.getPlayer().dropMessage(1, "Please make sure you have a enough space in your inventory.");
                c.sendPacket(PacketCreator.enableActions());
                return;
            }
        }

        int randomNumber = Randomizer.nextInt(rewards.getLeft());
        int runningProb = 0;

        for (RewardItem reward : rewards.getRight()) {
            runningProb += reward.prob;
            if (randomNumber < runningProb) {//Is it even possible to get an item with prob 1?
                Item item;
                if (ItemConstants.getInventoryType(reward.itemid) == InventoryType.EQUIP) {
                    item = ii.getEquipById(reward.itemid);
                    if (reward.period != -1) {
                        // TODO is this a bug, meant to be 60 * 60 * 1000?
                        item.setExpiration(currentServerTime() + reward.period * 60 * 60 * 10);
                    }
                } else {
                    item = new Item(reward.itemid, (short) 0, reward.quantity, -1);
                }

                InventoryManipulator.addFromDrop(c, item, false);
                InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, false, false);
                c.sendPacket(PacketCreator.getShowItemGain(reward.itemid, reward.quantity, true));

                if (reward.worldmsg != null) {
                    String msg = reward.worldmsg;
                    msg.replaceAll("/name", c.getPlayer().getName());
                    msg.replaceAll("/item", ii.getName(reward.itemid));
                    Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.serverNotice(6, msg));
                }
                break;
            }
        }
        c.sendPacket(PacketCreator.enableActions());
    }
}
