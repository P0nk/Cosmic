package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import server.maps.MapItem;
import server.maps.MapObject;
import server.maps.MapObjectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class LootCommand extends Command {

    private static final HashMap<Integer, Long> cooldowns = new HashMap<>();
    private static final HashMap<Integer, Integer> penalties = new HashMap<>();
    private static final int BASE_COOLDOWN_TIME = 10000; // Base cooldown time in milliseconds (60 seconds)
    private static final int PENALTY_TIME = 1000; // Initial penalty time in milliseconds (10 seconds)

    {
        setDescription("Loots all items that belong to you. Has a base cooldown of 60 seconds, with penalties for spamming that increase with repeated infractions.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Integer playerId = c.getPlayer().getId();
        long currentTime = System.currentTimeMillis();
        int currentPenaltyCount = penalties.getOrDefault(playerId, 0);
        long effectiveCooldown = BASE_COOLDOWN_TIME + (currentPenaltyCount * PENALTY_TIME * currentPenaltyCount); // Penalty increases with the square of the number of infractions

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
            if (mapItem.getOwnerId() == c.getPlayer().getId() || (mapItem.getOwnerId() == c.getPlayer().getPartyId())) {
                c.getPlayer().pickupItem(mapItem);
            }
        }

        cooldowns.put(playerId, currentTime); // Update last used time
        penalties.put(playerId, 0); // Reset penalty count on successful use
    }
}
