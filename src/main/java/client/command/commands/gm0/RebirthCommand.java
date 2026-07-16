package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;

public class RebirthCommand extends Command {

    {
        setDescription("Perform a manual rebirth.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (player == null) {
            return;
        }

        player.executeReborn();
    }
}