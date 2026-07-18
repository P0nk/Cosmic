package client.command.commands.gm0;

import client.Client;
import client.command.Command;

public class MsiCommand extends Command {

    private static final int MSI_EXCHANGE_NPC = 1022004;

    public MsiCommand() {
        setDescription("Open the MSI Exchange.");
    }

    @Override
    public void execute(Client c, String[] params) {
        c.getAbstractPlayerInteraction().openNpc(MSI_EXCHANGE_NPC);
    }
}