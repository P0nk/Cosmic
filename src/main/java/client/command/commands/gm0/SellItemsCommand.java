package client.command.commands.gm0;

import client.Client;
import client.command.Command;

public class SellItemsCommand extends Command {
    {
        setDescription("Open the Unified Sell Assistant.");
    }

    @Override
    public void execute(Client c, String[] params) {
        // Redirect to SellAllCommand's unified GUI logic (no params)
        new SellAllCommand().execute(c, new String[] {});
    }
}
