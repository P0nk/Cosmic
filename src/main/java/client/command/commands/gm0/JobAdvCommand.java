package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import scripting.npc.NPCScriptManager;

public class JobAdvCommand extends Command {

    {
        setDescription("Open the Explorer job advancement NPC.");
    }

    @Override
    public void execute(Client c, String[] params) {
        NPCScriptManager.getInstance().start(
                c,
                9200000,
                "jobadv",
                c.getPlayer()
        );
    }
}