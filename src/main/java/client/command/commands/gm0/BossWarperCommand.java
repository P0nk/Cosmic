package client.command.commands.gm0;

import client.Client;
import client.command.Command;

public class BossWarperCommand extends Command {

    private static final int BOSS_WARPER_NPC = 2042002; // Spiegelmann (temporary)

    public BossWarperCommand() {
        setDescription("Open the Boss Warper.");
    }

    @Override
    public void execute(Client c, String[] params) {

        c.getAbstractPlayerInteraction().openNpc(BOSS_WARPER_NPC);
    }
}