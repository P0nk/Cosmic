package net.server.channel.handlers;

import client.Character;
import client.Client;
import client.inventory.Item;
import net.packet.InPacket;
import server.maps.MapItem;
import server.maps.MapObjectType;
import server.movement.LifeMovementFragment;
import tools.PacketCreator;
import tools.exceptions.EmptyMovementException;
import constants.inventory.ItemConstants;
import config.YamlConfig;

import java.util.List;

public final class MovePetHandler extends AbstractMovementPacketHandler {

    private static final int BASE_PICKUP_RANGE = 60000;
    private static final int AUTO_LOOT_MULTIPLIER = 20;
    private static final List<Integer> EXCLUDED_ITEM_IDS = List.of(4036553, 4036550); // Items to be excluded from pet loot //change this to map later

    @Override
    public final void handlePacket(InPacket p, Client c) {
        int petId = p.readInt();
        p.readLong();
        List<LifeMovementFragment> res;

        try {
            res = parseMovement(p);
        } catch (EmptyMovementException e) {
            return;
        }

        Character player = c.getPlayer();
        byte slot = player.getPetIndex(petId);
        if (slot == -1) {
            return;
        }
        player.getPet(slot).updatePosition(res);
        player.getMap().broadcastMessage(player, PacketCreator.movePet(player.getId(), petId, slot, res), false);

        if (player.isGM() && player.isHidden()) {
            return;
        }

        if (!res.isEmpty()) {
            // Set pickup range based on AUTOLOOT_ITEM_ID presence
            int pickupRange = hasAutoLootItem(player) ? BASE_PICKUP_RANGE * AUTO_LOOT_MULTIPLIER : BASE_PICKUP_RANGE;

            List<MapItem> lst = player.getMap()
                    .getMapObjectsInRange(res.get(0).getPosition(), pickupRange, List.of(MapObjectType.ITEM))
                    .stream().map(e -> (MapItem) e)
                    .filter(e -> !e.isPlayerDrop()
                            && (!player.isEquippedPetItemIgnore()
                            || player.getExcludedItems().isEmpty()
                            || !player.getExcludedItems().contains(e.getItemId()))
                            && e.canBePickedBy(player, true)
                            && !EXCLUDED_ITEM_IDS.contains(e.getItemId()) // Exclude specified item IDs
                    )
                    .toList();

            for (MapItem mapItem : lst) {
                player.pickupItem(mapItem, slot);
            }
        }
    }

    /**
     * Checks if the player has AUTOLOOT_ITEM_ID in their inventory.
     *
     * @param player The player character.
     * @return True if the item is in the player's inventory; false otherwise.
     */
    private boolean hasAutoLootItem(Character player) {
        int autoLootItemId = YamlConfig.config.server.AUTOLOOT_ITEM_ID;
        var inventoryType = ItemConstants.getInventoryType(autoLootItemId);

        // Directly check the count of AUTOLOOT_ITEM_ID in the inventory
        return player.getInventory(inventoryType).countById(autoLootItemId) > 0;
    }
}
