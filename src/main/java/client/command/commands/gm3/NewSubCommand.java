package client.command.commands.gm3;

import client.Client;
import client.command.Command;
import scripting.npc.NPCScriptManager;

public class NewSubCommand extends Command{
    {
        setDescription("Open New Subordinate.");
    }

    @Override
    public void execute(Client c, String[] params) {
        NPCScriptManager.getInstance().start(c, 9201600, "Subordinate", null);
    }
}
