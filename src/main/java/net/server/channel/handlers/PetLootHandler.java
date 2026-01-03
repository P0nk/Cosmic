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
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.maps.MapItem;
import server.maps.MapObject;
import server.maps.MapObjectType;
import tools.PacketCreator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

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

        // ---------------------------------------------------------
        // PART 1: Logic for the specific item the pet was clicking
        // ---------------------------------------------------------
        if (ob instanceof MapItem) {
            MapItem mapitem = (MapItem) ob;
            // Check Meso Magnet
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
            }
            // Check Item Pouch
            else {
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
            chr.pickupItem(ob, petIndex);
        } else {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        // ---------------------------------------------------------
        // PART 2: Smart Vac Logic
        // ---------------------------------------------------------

        // 1. Pre-calculate inventory status
        // We check these ONCE to avoid calling database/inventory logic inside the loop
        boolean fullEtc = c.getPlayer().getInventory(InventoryType.ETC).getNumFreeSlot() < 1;
        boolean fullEquip = c.getPlayer().getInventory(InventoryType.EQUIP).getNumFreeSlot() < 1;
        boolean fullUse = c.getPlayer().getInventory(InventoryType.USE).getNumFreeSlot() < 1;

        // Optimization: If ALL relevant inventories are full, stop immediately.
        if (fullEtc && fullEquip && fullUse) {
            chr.showHint("Pet vac stopped: All inventories (EQUIP, USE, ETC) are full.", 300);
            return;
        }

        List<MapObject> items = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.ITEM));
        final Set<Integer> petIgnore = chr.getExcludedItems();

        for (MapObject item : items) {
            if (!(item instanceof MapItem)) {
                continue;
            }

            MapItem mapItem = (MapItem) item;

            // 2. Smart Inventory Filter
            // We skip specific items based on which inventory is full
            if (mapItem.getMeso() > 0) {
                // Mesos are always looted unless ignored (logic handled inside pickupItem usually)
            } else {
                int itemId = mapItem.getItemId();
                int typePrefix = itemId / 1000000; // Fast integer math to get type

                if (typePrefix == 1 && fullEquip) continue; // Skip Equips if Equip full
                else if (typePrefix == 2 && fullUse) continue;   // Skip Use if Use full
                else if (typePrefix >= 4 && fullEtc) continue;   // Skip Etc if Etc full
            }

            // 3. Ownership & Quest Checks
            boolean is_player_kill = mapItem.getOwnerId() == c.getPlayer().getId();
            boolean is_party_kill = mapItem.getOwnerId() == c.getPlayer().getPartyId();
            boolean common_or_meso_item = mapItem.getQuest() <= 0;
            boolean is_quest_item_and_active = c.getPlayer().getQuestStatus(mapItem.getQuest()) == 1;

            if ((is_player_kill || is_party_kill) && (common_or_meso_item || is_quest_item_and_active)) {
                if (mapItem.getMeso() > 0 || !petIgnore.contains(mapItem.getItemId())) {
                    chr.pickupItem(mapItem, petIndex);
                }
            }
        }
    }
}