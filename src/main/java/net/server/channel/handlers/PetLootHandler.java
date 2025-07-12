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
import client.inventory.InventoryType;
import client.inventory.Pet;
import config.YamlConfig;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.CashShop;
import server.maps.MapItem;
import server.maps.MapObject;
import server.maps.MapObjectType;
import tools.PacketCreator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.sql.DriverManager.println;

/**
 * @author TheRamon
 * @author Ronan
 */
public final class PetLootHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();

        int petIndex = chr.getPetIndex(p.readInt());
        Pet pet = chr.getPet(petIndex);
        if (pet == null || !pet.isSummoned()) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        p.skip(13);
        int oid = p.readInt();
        MapObject ob = chr.getMap().getMapObject(oid);
        try {
            MapItem mapitem = (MapItem) ob;
            if (mapitem.getMeso() > 0) {
                if (!chr.isEquippedMesoMagnet()) {
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }

                if (chr.isEquippedPetItemIgnore()) {
                    final Set<Integer> petIgnore = chr.getExcludedItems();
                    if (!petIgnore.isEmpty() && petIgnore.contains(Integer.MAX_VALUE)) {
                        c.sendPacket(PacketCreator.enableActions());
                        return;
                    }
                }
            } else {
                if (!chr.isEquippedItemPouch()) {
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }

                if (chr.isEquippedPetItemIgnore()) {
                    final Set<Integer> petIgnore = chr.getExcludedItems();
                    if (!petIgnore.isEmpty() && petIgnore.contains(mapitem.getItem().getItemId())) {
                        c.sendPacket(PacketCreator.enableActions());
                        return;
                    }
                }
            }
            // --------- Added the below code ---------
            // Get list of map item
            int pethunger = pet.getFullness();
            final double pickupRadius = (double) (500000 * pethunger) / 100;
            System.out.println(pethunger);
            boolean hasFreeEquip = chr.getInventory(InventoryType.EQUIP).getNumFreeSlot() > 0;
            boolean hasFreeUse   = chr.getInventory(InventoryType.USE).getNumFreeSlot() > 0;
            boolean hasFreeEtc   = chr.getInventory(InventoryType.ETC).getNumFreeSlot() > 0;

            List<MapObject> items = c.getPlayer().getMap().getMapObjectsInRange(pet.getPos(), pickupRadius, Arrays.asList(MapObjectType.ITEM));
            final Set<Integer> petIgnore = chr.getExcludedItems(); // get list of item ignore
            if (!hasFreeEquip || !hasFreeUse || !hasFreeEtc) {
                chr.showHint(
                        "Pet can't loot: please free up some EQUIP, USE or ETC slots.",
                        300
                );
                c.sendPacket(PacketCreator.enableActions());
                return;
            }
            for (MapObject item : items) { // loop thorugh map item
                MapItem mapItem = (MapItem) item; // assign map item to mapItem variable
                // Check loot details
                boolean is_player_kill = mapItem.getOwnerId() == c.getPlayer().getId();
                boolean is_party_kill = mapItem.getOwnerId() == c.getPlayer().getPartyId();
                boolean common_or_meso_item = mapItem.getQuest() <= 0; // QuestID <=0 because mesos quest id is -1
                boolean is_quest_item_and_active = c.getPlayer().getQuestStatus(mapItem.getQuest()) == 1;
                if ((is_player_kill || is_party_kill) && (common_or_meso_item || is_quest_item_and_active)) {
                    try {
                        if (!petIgnore.contains(mapItem.getItemId())) { // !petIgnore.isEmpty() &&
                            chr.pickupItem(mapItem, petIndex);
//                            System.out.println("Looted!");
//                            System.out.println();
                        } else {
                            c.sendPacket(PacketCreator.enableActions());
                        }
                    } catch (NullPointerException | ClassCastException e) {
                        c.sendPacket(PacketCreator.enableActions());
                    }
                }
//                System.out.println("Looted!");
//                System.out.println();
            } // up to here;
        } catch (NullPointerException | ClassCastException e) {
            c.sendPacket(PacketCreator.enableActions());
        }
    }
}