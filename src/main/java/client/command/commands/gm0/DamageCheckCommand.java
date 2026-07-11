package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;

public class DamageCheckCommand extends Command {
    {
        setDescription("Begin a damage check over X amount of seconds time eg @damage 15 sets damage check for 15 seconds.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        // Check if params are provided
        if (params.length < 1) {
            player.showHint("Add the amount of seconds you want the test to last.");
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            player.showHint("#rInvalid duration.#k Please enter a numeric value greater than #r0#k.");
            return;
        }

        // Ensure the duration is greater than 0
        if (duration <= 0) {
            player.showHint("Duration must be at least #r1#k second.");
            return;
        }

        player.damageChecks = true;
        player.damageTestLength = duration;
        player.showHint("Damage check will last for #r" + duration + "#k seconds on next attack.");
    }
}
