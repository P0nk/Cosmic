package net.server.channel.handlers;

import client.Client;
import client.inventory.InventoryType;
import constants.id.MapId;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.maps.FieldLimit;
import server.maps.MapleMap;
import client.Character;
import client.inventory.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Shahar
 */
public class HyperTPHandler extends AbstractPacketHandler {
    private static final int REQUIRED_ITEM_ID = 5590001; // Item ID for Hyper Teleport Rock
    private static final long COOLDOWN_TIME = 300000; // 5 minute in milliseconds

    // Store the last teleport time for each player
    private static final Map<Integer, Long> lastTeleportTime = new HashMap<>();

    @Override
    public void handlePacket(InPacket p, Client c) {
        if (p.available() > 0) {
            Character player = c.getPlayer();
            int map_id = p.readInt();

            // Check if player has the required item in inventory
            if (!hasRequiredItem(player)) {
                long currentTime = System.currentTimeMillis();
                long lastTimeUsed = lastTeleportTime.getOrDefault(player.getId(), 0L);
                long timeSinceLastUse = currentTime - lastTimeUsed;

                if (timeSinceLastUse < COOLDOWN_TIME) {
                    long timeLeft = (COOLDOWN_TIME - timeSinceLastUse) / 1000; // in seconds
                    player.dropMessage(1, "You need to wait " + timeLeft + " seconds before teleporting again.");
                    return;
                }
            }

            try {
                MapleMap target = c.getChannelServer().getMapFactory().getMap(map_id);
                if (target == null) {
                    player.yellowMessage("Map is invalid.");
                    return;
                }

                if (isRestrictedMap(map_id)) {
                    player.dropMessage(1, "You are not allowed to teleport to this map.");
                    return;
                }

                if (player.getReborns() < 1 && isLionKingMap(map_id)) {
                    player.dropMessage(1, "You need to be Rebirth level 1 or higher.");
                    return;
                }

                if (player.getReborns() < 2 && isTwilightPerion(map_id)) {
                    player.dropMessage(1, "You need to be Rebirth level 2 or higher.");
                    return;
                }

                if (player.getReborns() < 3 && isGrandisandArcane(map_id)) {
                    player.dropMessage(1, "You need to be Rebirth level 3 or higher.");
                    return;
                }

                if (FieldLimit.CANNOTVIPROCK.check(target.getFieldLimit())) {
                    player.dropMessage(1, "Unable to approach due to the force of the ground.");
                    return;
                }

                // Only update the cooldown time if all checks pass and the teleportation is successful
                player.saveLocationOnWarp();
                player.changeMap(target, target.getRandomPlayerSpawnpoint());

                // Now that the teleportation was successful, update the last teleport time
                lastTeleportTime.put(player.getId(), System.currentTimeMillis());

            } catch (Exception ex) {
                player.yellowMessage("Map is invalid.");
            }
        }
    }


    // Helper method to check if the player has the required item in their inventory
    private boolean hasRequiredItem(Character player) {
        Item item = player.getInventory(InventoryType.CASH).findById(REQUIRED_ITEM_ID);
        return item != null;
    }

    public static boolean isLionKingMap(int mapid) {
        return mapid >= 211060000 && mapid <= 211070550;
    }

    public static boolean isTwilightPerion(int mapid) {
        return mapid >= 271000000 && mapid <= 273060200;
    }

    public static boolean isGrandisandArcane(int mapid) {
        return mapid >= 400000000 && mapid <= 460000000;
    }

    public static boolean isRestrictedMap(int mapid) {
        return mapid == 401053002 || (mapid >= 271030000 && mapid <= 271040300);
    }
}
