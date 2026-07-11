package client;

import config.YamlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharacterManipulator {
    private static final Logger log = LoggerFactory.getLogger(CharacterManipulator.class);

    private static final int statMin = 0;

    public static boolean adjustStat(Client c, Stat statType, int amount) {
        if (amount == 0) {
            return true;
        }

        Character player = c.getPlayer();
        int remainingAp = player.getRemainingAp();
        int currentStat;

        // Retrieve the current stat value based on the provided stat type
        switch (statType) {
            case STR:
                currentStat = player.getStr();
                break;
            case DEX:
                currentStat = player.getDex();
                break;
            case INT:
                currentStat = player.getInt();
                break;
            case LUK:
                currentStat = player.getLuk();
                break;
            default:
                player.dropMessage("Invalid stat type.");
                log.error("Invalid stat type provided: {}", statType);
                return false;
        }

        // Adjust the stat value based on the provided amount
        if (amount > 0) {
            // Increasing stat
            amount = Math.min(amount, remainingAp);
            amount = Math.min(amount, YamlConfig.config.server.MAX_AP - currentStat);
        } else {
            // Decreasing stat
            amount = Math.max(amount, statMin - currentStat);
        }

        // Apply the stat adjustment
        boolean result = switch (statType) {
            case STR -> player.assignStr(amount);
            case DEX -> player.assignDex(amount);
            case INT -> player.assignInt(amount);
            case LUK -> player.assignLuk(amount);
            default -> false;
        };

        // Log the result of the stat adjustment
        if (!result) {
            log.error("Failed to adjust {} for player {}: amount {}, remaining AP {}, current stat {}",
                    statType, player.getName(), amount, remainingAp, currentStat);
        }

        return result;
    }
}
