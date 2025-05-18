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
import client.Disease;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import constants.id.ItemId;
import constants.inventory.ItemConstants;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.ItemInformationProvider;
import server.StatEffect;
import tools.PacketCreator;

/**
 * @author Matze
 */
public final class UseItemHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();

        if (!chr.isAlive()) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        p.readInt();
        short slot = p.readShort();
        int itemId = p.readInt();
        Item toUse = chr.getInventory(InventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemId) {
            if (itemId == ItemId.ALL_CURE_POTION) {
                chr.dispelDebuffs();
                remove(c, slot);
                return;
            } else if (itemId == ItemId.EYEDROP) {
                chr.dispelDebuff(Disease.DARKNESS);
                remove(c, slot);
                return;
            } else if (itemId == ItemId.TONIC) {
                chr.dispelDebuff(Disease.WEAKEN);
                chr.dispelDebuff(Disease.SLOW);
                remove(c, slot);
                return;
            } else if (itemId == ItemId.HOLY_WATER) {
                chr.dispelDebuff(Disease.SEAL);
                chr.dispelDebuff(Disease.CURSE);
                remove(c, slot);
                return;
            } else if (ItemConstants.isTownScroll(itemId)) {
                if (ii.getItemEffect(toUse.getItemId()).applyTo(chr)) {
                    remove(c, slot);
                }
                return;
            } else if (itemId == ItemId.HP_CAP_INCREASE_1) {
                Character player = c.getPlayer();
                if (player.getLevel() < 120) {
                    player.yellowMessage("You need to be level 120 to use this item!");
                    return;
                } else {
                    player.updateMaxHp(player.getMaxHp() + 100);
                    remove(c, slot);
                    return;
                }
            } else if (itemId == ItemId.MP_CAP_INCREASE_1) {
                Character player = c.getPlayer();
                player.updateMaxMp(player.getMaxMp() + 100);
                remove(c, slot);
                return;
            } else if (itemId == ItemId.HP_CAP_INCREASE_2) {
                Character player = c.getPlayer();
                if (player.getLevel() < 150) {
                    player.yellowMessage("You need to be level 150 to use this item!");
                    return;
                } else {
                    player.updateMaxHp(player.getMaxHp() + 300);
                    remove(c, slot);
                    return;
                }
            } else if (itemId == ItemId.MP_CAP_INCREASE_2) {
                Character player = c.getPlayer();
                player.updateMaxMp(player.getMaxMp() + 300);
                remove(c, slot);
                return;
            }else if (itemId == ItemId.HP_CAP_INCREASE_3) {
                Character player = c.getPlayer();
                if (player.getLevel() < 180) {
                    player.yellowMessage("You need to be level 180 to use this item!");
                    return;
                } else {
                    player.updateMaxHp(player.getMaxHp() +500);
                    remove(c, slot);
                    return;
                }
            } else if (itemId == ItemId.MP_CAP_INCREASE_3) {
                Character player = c.getPlayer();
                player.updateMaxMp(player.getMaxMp() + 500);
                remove(c, slot);
                return;
            }

            remove(c, slot);

            if (toUse.getItemId() != ItemId.HAPPY_BIRTHDAY) {
                ii.getItemEffect(toUse.getItemId()).applyTo(chr);
            } else {
                StatEffect mse = ii.getItemEffect(toUse.getItemId());
                for (Character player : chr.getMap().getCharacters()) {
                    mse.applyTo(player);
                }
            }
        }
    }

    private void remove(Client c, short slot) {
        InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, (short) 1, false);
        c.sendPacket(PacketCreator.enableActions());
    }
}
