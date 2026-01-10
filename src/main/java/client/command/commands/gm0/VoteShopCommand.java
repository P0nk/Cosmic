package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import scripting.npc.NPCScriptManager;

public class VoteShopCommand extends Command {

    {
        setDescription("Opens the Vote Reward Center (dashboard + shop).");
    }

    @Override
    public void execute(Client c, String[] params) {
        // Using NPC 9010000 (Maple Admin) and script "voteshop_custom"
        // The script itself handles the VoteManager.linkPendingVotes() call
        NPCScriptManager.getInstance().start(c, 9010000, "voteshop", null);
    }
}