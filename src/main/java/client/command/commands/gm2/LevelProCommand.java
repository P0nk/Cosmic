package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;

public class LevelProCommand extends Command {
    {
        setDescription("Set a level. Usage: !levelpro <level> [ign]");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        Character target = player; // Default target is self
        int targetLevel = 0;

        // Validation: Check arguments
        if (params.length < 1) {
            player.yellowMessage("Syntax: !levelpro <level> [ign]");
            return;
        }

        // Step 1: Always parse the first parameter as the Level
        try {
            targetLevel = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            player.yellowMessage("Invalid Level. Please enter a number first. Syntax: !levelpro <level> [ign]");
            return;
        }

        // Step 2: Check for optional target name (Second parameter)
        if (params.length > 1) {
            String targetName = params[1];
            target = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);

            if (target == null) {
                player.yellowMessage("Player '" + targetName + "' could not be found in this channel.");
                return;
            }
        }

        // Step 3: Execute Leveling Logic
        int currentLevel = target.getLevel();
        int maxPossible = target.getMaxClassLevel();
        // Ensure we don't exceed the class max level
        int finalLevel = Math.min(maxPossible, targetLevel);

        if (currentLevel >= finalLevel) {
            player.yellowMessage(target.getName() + " is already level " + currentLevel + " (Targeting: " + finalLevel + ")");
            return;
        }

        // Loop level up until target is reached
        while (target.getLevel() < finalLevel) {
            target.levelUp(false);
        }

        // Confirmation message
        if (player != target) {
            player.yellowMessage("Leveled up " + target.getName() + " to " + target.getLevel());
        } else {
            player.yellowMessage("Leveled yourself to " + target.getLevel());
        }
    }
}