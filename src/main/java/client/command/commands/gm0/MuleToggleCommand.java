package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;

public class MuleToggleCommand extends Command {


    {
        setDescription("Toggle the Mule system on/off.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();


        // Validate command parameters
        if (params.length < 1) {
            player.showHint("Please use @Muletoggle.");
            return;
        }

        String action = params[0].toLowerCase();

        // Toggle Mule system based on command parameter
        if (action.equals("on")) {
            player.setUseModifiedPickupItem(true);
            player.showHint("Mule system #bSUMMONED#k. Loot will now be delivered to your Mule.");
        } else if (action.equals("off")) {
            player.setUseModifiedPickupItem(false);
            player.showHint("Mule system #rDISABLED#k. Loot will now be delivered to your inventory.");
        } else {
            player.showHint("#rInvalid option#k. Use @Muletoggle.");
            return;
        }

    }
}
