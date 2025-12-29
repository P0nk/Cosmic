package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.SkillFactory;
import client.command.Command;
import constants.skills.Assassin;
import constants.skills.Bowmaster;
import constants.skills.Buccaneer;
import constants.skills.Hermit;
import constants.skills.Priest;

import java.util.HashMap;

public class UnlockedBuffsCommand extends Command {

    private static final HashMap<Integer, Long> cooldowns = new HashMap<>();
    private static final HashMap<Integer, Integer> penalties = new HashMap<>();
    private static final int BASE_COOLDOWN_TIME = 10000; // Base cooldown time in milliseconds (10 seconds)
    private static final int PENALTY_TIME = 1000; // Initial penalty time in milliseconds (1 second)
    private static final int MAX_PENALTY_TIME = 5000; // Cap penalty time to 5 seconds max

    {
        setDescription("Activate unlocked buffs on self with cooldown and penalties for repeated use.");
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

        for (int skillId : player.getUnlockedBuffs()) {
            switch (skillId) {
//                case Hermit.SHADOW_PARTNER: // Maybe only for XBOW/BOWMASTERS since they can't use Sharpeyes?
                case Hermit.MESO_UP: // LVL 15
                case Priest.HOLY_SYMBOL: // lvl 15
                    SkillFactory.getSkill(skillId)
                            .getEffect(15)
                            .applyTo(player);
                    break;
                case Bowmaster.SHARP_EYES: // lvl 20
                    SkillFactory.getSkill(skillId)
                            .getEffect(20)
                            .applyTo(player);
                    break;
                case Assassin.HASTE: // max
                case Buccaneer.SPEED_INFUSION: // max
                    SkillFactory.getSkill(skillId)
                            .getEffect(SkillFactory.getSkill(skillId).getMaxLevel())
                            .applyTo(player);
                    break;

                default:
                    break;
            }
        }

        cooldowns.put(playerId, currentTime); // Update last used time
        penalties.put(playerId, 0); // Reset penalty count on successful use
    }
}
