package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;

public class SetAutoLoginCommand extends Command {
    {
        setDescription("Toggle Auto Login. Based off your current channel and character.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        player.setAutoLogin();
    }
}
