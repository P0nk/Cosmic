package client.command.commands.gm0;

import client.Client;
import client.command.Command;

public class BeltCommand extends Command {

    private static final int BELT_EXCHANGE_NPC = 1022003;

    public BeltCommand() {
        setDescription("Open the Belt Exchange.");
    }

    @Override
    public void execute(Client c, String[] params) {
        c.getAbstractPlayerInteraction().openNpc(BELT_EXCHANGE_NPC);
    }
}
