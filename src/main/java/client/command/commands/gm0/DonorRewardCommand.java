package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import scripting.npc.NPCScriptManager;

public class DonorRewardCommand extends Command {

    {
        setDescription("Opens Donor Rewards (dashboard + shop).");
    }

    @Override
    public void execute(Client c, String[] params) {
        NPCScriptManager.getInstance().start(c, 9900000, "donorreward", null);
    }
}