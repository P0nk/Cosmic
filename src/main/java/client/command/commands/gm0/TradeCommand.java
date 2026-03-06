package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import scripting.npc.NPCScriptManager;

public class TradeCommand extends Command {
    {
        setDescription("Open the untradable equipment escrow trade menu.");
    }

    @Override
    public void execute(Client c, String[] params) {
        NPCScriptManager.getInstance().start(c, 9010009, "trade", c.getPlayer());
    }
}
