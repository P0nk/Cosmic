package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import scripting.npc.NPCScriptManager;

public class TPCommand extends Command{
    {
        setDescription("Open the Sell shop.");
    }

    @Override
    public void execute(Client c, String[] params) {
        NPCScriptManager.getInstance().start(c, 9201600, "9201600", null);
    }
}
