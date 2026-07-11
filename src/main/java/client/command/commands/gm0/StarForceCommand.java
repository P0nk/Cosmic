// created by Mark

package client.command.commands.gm0;

import client.Client;
import client.command.Command;



public class StarForceCommand extends Command {
    {
        setDescription("Stores and opens crafting material storage for maker skill.");
    }

    @Override
    public void execute(Client c, String[] params) {
        c.getAbstractPlayerInteraction().openNpc(2616, null);
    }
}

