/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
              Matthias Butz <matze@odinms.de>
              Jan Christian Meyer <vimes@odinms.de>
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
        // [Debug removed]

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
        // PART 1: Handle the specific item the pet clicked
        // ---------------------------------------------------------
        if (ob instanceof MapItem) {
            MapItem mapitem = (MapItem) ob;

            // [GLOBAL EXCLUSION] Pink Bean Summon Item
            if (mapitem.getItemId() == 4001193) {
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            // [FEATURE] Auto-Sell (Single Item Click)
            if (chr.isAutoSellEnabled()) {
                if (mapitem.getItem().getInventoryType() == InventoryType.EQUIP) {
                    int gain = client.command.commands.gm0.SellAllCommand.sellItem(c, chr, mapitem.getItem(), null);
                    if (gain > 0) {
                        chr.gainMeso(gain, true);
                        if (chr.getMapKillCount() % 5 == 0) {
                            chr.dropMessage(5, "Pet Auto-sold item for " + gain + " mesos.");
                        }
                        // Remove from map
                        // Remove from map using proper method
                        chr.getMap().pickItemDrop(
                                PacketCreator.removeItemFromMap(ob.getObjectId(), 2, chr.getId()),
                                mapitem);
                        c.sendPacket(PacketCreator.enableActions());
                        // return; // Removed to allow Vac Loop
                    }
                }
            }

            // Meso Magnet Check
            if (mapitem.getMeso() > 0) {
                if (!chr.isEquippedMesoMagnet()) {
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }
            }
            // Item Pouch Check
            else {
                if (!chr.isEquippedItemPouch()) {
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }
                // Ignore List Check
                if (chr.isEquippedPetItemIgnore()) {
                    final Set<Integer> petIgnore = chr.getExcludedItems();
                    if (!petIgnore.isEmpty() && petIgnore.contains(mapitem.getItem().getItemId())) {
                        c.sendPacket(PacketCreator.enableActions());
                        return;
                    }
                }
            }

            // Pickup Trigger Item
            try {
                chr.pickupItem(ob, petIndex);
            } catch (Exception e) {
                // If the main item is bugged, enable actions to unfreeze client
                c.sendPacket(PacketCreator.enableActions());
            }
        } else {
            // This handles the "Ghost Items" (items already vacced by previous packets)
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        // ---------------------------------------------------------
        // PART 2: Smart Vac Loop
        // ---------------------------------------------------------

        // 1. Fail-fast Inventory Checks (Optimization)
        // Note: Auto-Sell bypasses inventory checks, hence we might need to check full
        // inventory ONLY if auto-sell didn't handle it.
        // But for performance, if inventories are full AND auto-sell is OFF, we stop.
        // If Auto-Sell is ON, we should probably continue even if inventory is full,
        // because we might sell stuff.
        boolean fullEtc = c.getPlayer().getInventory(InventoryType.ETC).getNumFreeSlot() < 1;
        boolean fullEquip = c.getPlayer().getInventory(InventoryType.EQUIP).getNumFreeSlot() < 1;
        boolean fullUse = c.getPlayer().getInventory(InventoryType.USE).getNumFreeSlot() < 1;

        if (!chr.isAutoSellEnabled() && fullEtc && fullEquip && fullUse) {
            chr.showHint("Pet vac stopped: All inventories are full.", 300);
            return;
        }

        List<MapObject> items = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(),
                Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.ITEM));
        final Set<Integer> petIgnore = chr.getExcludedItems();

        for (MapObject item : items) {
            if (!(item instanceof MapItem)) {
                continue;
            }

            MapItem mapItem = (MapItem) item;

            // [GLOBAL EXCLUSION] Pink Bean Summon Item
            if (mapItem.getItemId() == 4001193) {
                continue;
            }

            // We wrap individual pickup attempts in try-catch.
            // If one specific item is bugged/null, we ignore it and continue looping.
            try {
                // [FEATURE] Auto-Sell (Vac Loop)
                if (chr.isAutoSellEnabled()) {
                    // [RESTRICTION] Auto-Sell only applies to Equips
                    if (mapItem.getItem().getInventoryType() == InventoryType.EQUIP) {
                        // Check if we can sell it
                        // NOTE: Auto-Sell function checks for valid price. If 0 (untradable/no price),
                        // it returns 0.
                        int gain = client.command.commands.gm0.SellAllCommand.sellItem(c, chr, mapItem.getItem(), null);
                        if (gain > 0) {
                            chr.gainMeso(gain, true);
                            // Limit spam
                            if (chr.getMapKillCount() % 5 == 0) {
                                chr.dropMessage(5, "Pet Auto-sold item for " + gain + " mesos.");
                            }
                            // Remove from map
                            // Use pickItemDrop to ensure correct cleanup of counters and registry
                            chr.getMap().pickItemDrop(
                                    PacketCreator.removeItemFromMap(item.getObjectId(), 2, chr.getId()),
                                    mapItem);
                            continue; // Successfully sold and removed, next item
                        }
                    }
                    // If not sold (e.g. untradable OR not equip), fall through to normal pickup
                    // logic
                }

                // 2. Smart Inventory Filter
                if (mapItem.getMeso() > 0) {
                    // Mesos always looted
                } else {
                    int itemId = mapItem.getItemId();
                    int typePrefix = itemId / 1000000;

                    // Skip specific item types if that inventory is full
                    if (typePrefix == 1 && fullEquip)
                        continue;
                    else if (typePrefix == 2 && fullUse)
                        continue;
                    else if (typePrefix >= 4 && fullEtc)
                        continue;

                    if (petIgnore.contains(itemId))
                        continue;
                }

                // 3. Ownership & Quest Checks
                boolean is_player_kill = mapItem.getOwnerId() == c.getPlayer().getId();
                boolean is_party_kill = mapItem.getOwnerId() == c.getPlayer().getPartyId();
                boolean common_or_meso_item = mapItem.getQuest() <= 0;
                boolean is_quest_item_and_active = c.getPlayer().getQuestStatus(mapItem.getQuest()) == 1;

                if ((is_player_kill || is_party_kill) && (common_or_meso_item || is_quest_item_and_active)) {
                    chr.pickupItem(mapItem, petIndex);
                }
            } catch (Exception e) {
                // Silently ignore buggy items to keep server running smooth
                continue;
            }
        }
    }
}