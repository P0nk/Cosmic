package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import config.YamlConfig;

import java.util.HashMap;

public class StatStrCommand extends Command {

    private static final HashMap<Integer, Long> cooldowns = new HashMap<>();
    private static final HashMap<Integer, Integer> penalties = new HashMap<>();
    private static final int BASE_COOLDOWN_TIME = 10000; // Base cooldown time in milliseconds (10 seconds)
    private static final int PENALTY_TIME = 1000; // Initial penalty time in milliseconds (1 second)
    private static final int MAX_PENALTY_TIME = 5000; // Cap penalty time to 5 seconds max

    {
        setDescription("Assign AP into STR with cooldown and penalties for repeated use.");
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

        Character player = c.getPlayer();
        int remainingAp = player.getRemainingAp();
        int amount;
        if (params.length > 0) {
            try {
                amount = Math.min(Integer.parseInt(params[0]), remainingAp);
            } catch (NumberFormatException e) {
                player.dropMessage("That is not a valid number!");
                return;
            }
        } else {
            amount = Math.min(remainingAp, YamlConfig.config.server.MAX_AP - player.getStr());
        }

        if (!player.assignStr(Math.max(amount, 0))) {
            player.dropMessage("Please make sure your AP is not over " + YamlConfig.config.server.MAX_AP + " and you have enough to distribute.");
        }

        cooldowns.put(playerId, currentTime); // Update last used time
        penalties.put(playerId, 0); // Reset penalty count on successful use
    }
}
