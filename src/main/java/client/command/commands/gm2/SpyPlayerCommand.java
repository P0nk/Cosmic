package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;

public class SpyPlayerCommand extends Command {

    {
        setDescription("Spy on a player.");
    }

    @Override
    public void execute(Client c, String[] params) {
        c.getAbstractPlayerInteraction().openNpc(3002029, "spyOnPlayers");
    }
}