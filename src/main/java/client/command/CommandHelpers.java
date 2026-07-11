package client.command;

import client.Character;
import config.YamlConfig;

public class CommandHelpers {
    public static Integer parseApAmount(String[] params, Character player, int current) {
        int amount;
        if (params.length > 0) {
            try {
                amount = Integer.parseInt(params[0]);
            } catch (NumberFormatException e) {
                player.dropMessage("That is not a valid number!");
                return null;
            }
        } else {
            amount = YamlConfig.config.server.MAX_AP - current;
        }
        return amount;
    }
}
