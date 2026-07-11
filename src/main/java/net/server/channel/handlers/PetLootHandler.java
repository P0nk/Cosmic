package net.server.channel.handlers;

import client.Character;
import client.Client;
import client.inventory.Pet;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.maps.MapItem;
import server.maps.MapObject;
import server.maps.MapObjectType;
import tools.PacketCreator;
import constants.inventory.ItemConstants;
import config.YamlConfig;

import java.util.Set;
import java.util.List;
import java.util.Arrays;

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

            // Check if the pet is trying to loot meso
            if (mapitem.getMeso() > 0) {
                // Ensure that the pet only loots meso if the player is equipped with Meso Magnet
                if (!chr.isEquippedMesoMagnet()) {
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }

                // Respect the petIgnore list for meso
                if (chr.isEquippedPetItemIgnore()) {
                    final Set<Integer> petIgnore = chr.getExcludedItems();
                    if (!petIgnore.isEmpty() && petIgnore.contains(Integer.MAX_VALUE)) {
                        c.sendPacket(PacketCreator.enableActions());
                        return;
                    }
                }
            } else {
                // Pet is trying to loot an item
                if (!chr.isEquippedItemPouch()) {
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }

                // Respect the petIgnore list for items
                if (chr.isEquippedPetItemIgnore()) {
                    final Set<Integer> petIgnore = chr.getExcludedItems();
                    if (!petIgnore.isEmpty() && petIgnore.contains(mapitem.getItem().getItemId())) {
                        c.sendPacket(PacketCreator.enableActions());
                        return;
                    }
                }
            }

            // Handle autoloot if AUTOLOOT_ITEM_ID is equipped
//             List<Integer> restrictedMaps = Arrays.asList(910000008, 910000009);
//            if (!restrictedMaps.contains(c.getPlayer().getMap().getId())) {
//                // Define default loot range
//                int defaultRangeX = 600; // Adjust range X as needed
//                int defaultRangeY = 600; // Adjust range Y as needed
//
//                // Check if the player has the autoloot item
//                if (c.getPlayer().getInventory(ItemConstants.getInventoryType(YamlConfig.config.server.AUTOLOOT_ITEM_ID))
//                        .countById(YamlConfig.config.server.AUTOLOOT_ITEM_ID) > 0) {
//
//                    // Get all items in range and loot them if they are not ignored
//                    List<MapObject> items = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(),
//                            Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.ITEM));
//
//                    final Set<Integer> petIgnore = chr.getExcludedItems();
//
//                    for (MapObject item : items) {
//                        MapItem mapItem = (MapItem) item;
//
//                        // Skip if the item is 2430030
//                        if (mapItem.getItem() != null && mapItem.getItem().getItemId() == 2430030) {
//                            continue; // Skip looting this item
//                        }
//
//                        // Check if the item is in the petIgnore set
//                        if (chr.isEquippedPetItemIgnore() && !petIgnore.isEmpty()) {
//                            if (mapItem.getMeso() > 0 && petIgnore.contains(Integer.MAX_VALUE)) {
//                                continue; // Ignore meso if it is excluded
//                            } else if (mapItem.getItem() != null && petIgnore.contains(mapItem.getItem().getItemId())) {
//                                continue; // Ignore item if it is in the petIgnore list
//                            }
//                        }
//
//                        // Proceed with picking up the item if not ignored
//                        if (mapItem.getOwnerId() == c.getPlayer().getId() || mapItem.getOwnerId() == c.getPlayer().getPartyId()) {
//                            c.getPlayer().pickupItem(mapItem);
//                        }
//                    }
//                } else {
//                    // No autoloot item, apply default loot range with increased size
//                    List<MapObject> items = c.getPlayer().getMap().getMapObjectsInRange(
//                            c.getPlayer().getPosition(),
//                            Double.POSITIVE_INFINITY,
//                            Arrays.asList(MapObjectType.ITEM));
//
//                    // Filter items within default increased range (600x600)
//                    for (MapObject item : items) {
//                        double deltaX = Math.abs(item.getPosition().getX() - chr.getPosition().getX());
//                        double deltaY = Math.abs(item.getPosition().getY() - chr.getPosition().getY());
//
//                        if (deltaX <= defaultRangeX / 2 && deltaY <= defaultRangeY / 2) {
//                            chr.pickupItem(item, petIndex);
//                        }
//                    }
//                }
//            } else {
//                // Restricted map, use normal pet loot logic
//                chr.pickupItem(ob, petIndex);
//            }
            chr.pickupItem(ob, petIndex);
        } catch (NullPointerException | ClassCastException e) {
            c.sendPacket(PacketCreator.enableActions());
        }
    }
}
