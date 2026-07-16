package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import scripting.npc.NPCScriptManager;

public class StorageCommand extends Command {

    public StorageCommand() {
        setDescription("Open account storage.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (player.getTrade() != null) {
            player.dropMessage(5, "You cannot use @storage while trading.");
            return;
        }

        if (player.getJailExpirationTimeLeft() > 0) {
            player.dropMessage(5, "You cannot use @storage while jailed.");
            return;
        }

        if (player.isChangingMaps()) {
            return;
        }

        NPCScriptManager.getInstance().start(
                c,
                1061008,
                "1061008",
                player
        );
    }
}