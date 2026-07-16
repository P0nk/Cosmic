package client.command.commands.gm0;

import client.Client;
import client.Character;
import client.command.Command;
import scripting.npc.NPCScriptManager;

public class TaxiCommand extends Command {

    public TaxiCommand() {
        setDescription("Open the ForgeMS Travel Wagon.");
    }

    @Override
    public void execute(Client c, String[] params) {

        Character player = c.getPlayer();

        if (player.getTrade() != null) {
            player.dropMessage(5, "You cannot use @taxi while trading.");
            return;
        }

        if (player.getJailExpirationTimeLeft() > 0) {
            player.dropMessage(5, "You cannot use @taxi while jailed.");
            return;
        }

        if (player.isChangingMaps()) {
            return;
        }

        NPCScriptManager.getInstance().start(
                c,
                1012000,
                "1012000",
                player
        );
    }
}