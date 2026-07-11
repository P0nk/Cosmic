package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;

public class BackPackToggleCommand extends Command {
    private static final long COOLDOWN_TIME = 120_000; // 10 minutes in milliseconds

    {
        setDescription("Toggle the backpack system on/off.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        long currentTime = System.currentTimeMillis();

        // Check if enough time has passed since the last use
        if (currentTime - player.getLastBackpackCommandTime() < COOLDOWN_TIME) {
            long timeLeftMillis = COOLDOWN_TIME - (currentTime - player.getLastBackpackCommandTime());

            // Convert time left to minutes and seconds
            long minutesLeft = (timeLeftMillis / 1000) / 60;
            long secondsLeft = (timeLeftMillis / 1000) % 60;

            // Show hint with remaining cooldown time
            player.showHint(String.format("You can use the @backpack command again in #r%d#k minutes and #r%d#k seconds.", minutesLeft, secondsLeft));
            return;
        }

        // Validate command parameters
        if (params.length < 1) {
            player.showHint("Please use @backpack on or @backpack off.");
            return;
        }

        String action = params[0].toLowerCase();

        // Toggle backpack system based on command parameter
        if (action.equals("on")) {
            player.setUseModifiedPickupItem(true);
            player.showHint("Backpack system #bENABLED#k. Loot will now go into your backpack.");
        } else if (action.equals("off")) {
            player.setUseModifiedPickupItem(false);
            player.showHint("Backpack system #rDISABLED#k. Loot will now go into your inventory.");
        } else {
            player.showHint("#rInvalid option#k. Use @backpack on or @backpack off.");
            return;
        }

        // Update the last used time
        player.setLastBackpackCommandTime(currentTime);
    }
}
