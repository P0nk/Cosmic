package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;

public class LevelProCommand extends Command {
    {
        setDescription("Level up to target (Gains AP/SP). Usage: !levelpro <level> [ign]");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        Character target = player;
        int targetLevel = 0;

        if (params.length < 1) {
            player.yellowMessage("Syntax: !levelpro <level> [ign]");
            return;
        }

        try {
            targetLevel = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            player.yellowMessage("Invalid Number.");
            return;
        }

        if (params.length > 1) {
            String targetName = params[1];
            target = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
            if (target == null) {
                player.yellowMessage("Player '" + targetName + "' not found.");
                return;
            }
        }

        int currentLevel = target.getLevel();
        int maxPossible = target.getMaxClassLevel();
        int finalLevel = Math.min(maxPossible, targetLevel);

        if (currentLevel >= finalLevel) {
            player.yellowMessage(target.getName() + " is already level " + currentLevel);
            return;
        }

        // [FIX] Calculate the exact number of level-ups needed beforehand
        int levelsNeeded = finalLevel - currentLevel;

        // Run the loop exactly that many times.
        // We do NOT check target.getLevel() inside the condition to avoid race conditions.
        for (int i = 0; i < levelsNeeded; i++) {
            target.levelUp(false);
        }

        // If target is self, message is redundant as level up effect plays, but good for logs
        if (player != target) {
            player.yellowMessage("Leveled up " + target.getName() + " to " + target.getLevel());
        } else {
            player.yellowMessage("You are now Level " + target.getLevel());
        }
    }
}