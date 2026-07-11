package client.command.commands.gm0;

import client.Client;
import client.command.Command;

public class ExpeditionLeaderboardCommand extends Command {
    @Override
    public void execute(Client client, String[] params) {
        client.getAbstractPlayerInteraction().openNpc(9000041, "expeditionLeaderboard"); //This uses Donation Box NPC
    }
}
