package client.command.commands.gm0;

import client.Client;
import client.command.Command;

public class StyleCommand extends Command {

    private static final int INGRID_NPC_ID = 9120101;

    public StyleCommand() {
        setDescription("Open Ingrid's cosmetic selector.");
    }

    @Override
    public void execute(Client c, String[] params) {
        c.getAbstractPlayerInteraction().openNpc(INGRID_NPC_ID);
    }
}