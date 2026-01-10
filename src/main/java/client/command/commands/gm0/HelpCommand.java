package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import constants.id.NpcId;

public class HelpCommand extends Command {
    {
        setDescription("Show available commands.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Opens the Steward NPC to show the commands list script
        client.getAbstractPlayerInteraction().openNpc(NpcId.STEWARD, "commands");
    }
}