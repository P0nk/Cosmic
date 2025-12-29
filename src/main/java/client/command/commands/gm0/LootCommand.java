package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import server.maps.MapItem;
import server.maps.MapObject;
import server.maps.MapObjectType;
import tools.PacketCreator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class LootCommand extends Command {

    private static final HashMap<Integer, Long> cooldowns = new HashMap<>();
    private static final HashMap<Integer, Integer> penalties = new HashMap<>();
    private static final int BASE_COOLDOWN_TIME = 10000; // Base cooldown time in milliseconds (10 seconds)
    private static final int PENALTY_TIME = 1000; // Initial penalty time in milliseconds (1 second)
    private static final int MAX_PENALTY_TIME = 5000; // Cap penalty time to 5 seconds max

    {
        setDescription("Loots all items that belong to you. Has a base cooldown of 10 seconds, with penalties for spamming that increase with repeated infractions.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Integer playerId = c.getPlayer().getId();
        long currentTime = System.currentTimeMillis();
        int currentPenaltyCount = penalties.getOrDefault(playerId, 0);

        // Cap the penalty time to avoid excessive cooldown
        int penaltyTime = Math.min(currentPenaltyCount * PENALTY_TIME * currentPenaltyCount, MAX_PENALTY_TIME);
        long effectiveCooldown = BASE_COOLDOWN_TIME + penaltyTime; // Apply penalty cooldown

        // Remove expired cooldown entries
        cooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() > BASE_COOLDOWN_TIME * 2); // Cleanup after a reasonable period

        if (cooldowns.containsKey(playerId)) {
            long timePassed = currentTime - cooldowns.get(playerId);
            if (timePassed < effectiveCooldown) {
                long timeLeft = (effectiveCooldown - timePassed) / 1000; // Convert to seconds
                String message = String.format("You must wait %d more second(s) before using this command again. Repeated attempts have triggered %d penalty(ies), increasing your cooldown by an additional %d second(s).",
                        timeLeft, currentPenaltyCount, currentPenaltyCount * (PENALTY_TIME / 1000) * currentPenaltyCount);
                c.getPlayer().dropMessage(5, message);
                penalties.put(playerId, currentPenaltyCount + 1); // Increase penalty count for next time
                return;
            }
        }

        List<MapObject> items = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.ITEM));
        for (MapObject item : items) {
            MapItem mapItem = (MapItem) item;
            boolean is_player_kill = mapItem.getOwnerId() == c.getPlayer().getId();
            boolean is_party_kill = mapItem.getOwnerId() == c.getPlayer().getPartyId();
            boolean common_or_meso_item = mapItem.getQuest() <= 0; // QuestID <=0 because mesos quest id is -1
            boolean is_quest_item_and_active = c.getPlayer().getQuestStatus(mapItem.getQuest()) == 1;
            if ((is_player_kill || is_party_kill) && (common_or_meso_item || is_quest_item_and_active)) {
                // Get pet ignore list
                if (c.getPlayer().isEquippedPetItemIgnore()) {
                    final Set<Integer> petIgnore = c.getPlayer().getExcludedItems();
                    if (!petIgnore.isEmpty() && !petIgnore.contains(mapItem.getItem().getItemId())) {
                        c.getPlayer().pickupItem(mapItem);
                    }
                } else {
                    c.getPlayer().pickupItem(mapItem);
                }
            }
        }

        cooldowns.put(playerId, currentTime); // Update last used time
        penalties.put(playerId, 0); // Reset penalty count on successful use
    }
}
