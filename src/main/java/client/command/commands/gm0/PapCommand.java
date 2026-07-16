package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.commands.CommandMapConstants;

public class PapCommand extends Command {

    public PapCommand() {
        setDescription("Travel to the entrance of Papulatus.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (player.getTrade() != null) {
            player.dropMessage(
                    5,
                    "You cannot use @pap while trading."
            );
            return;
        }

        if (player.getJailExpirationTimeLeft() > 0) {
            player.dropMessage(
                    5,
                    "You cannot use @pap while jailed."
            );
            return;
        }

        if (player.isChangingMaps()) {
            return;
        }

        player.changeMap(CommandMapConstants.PAPULATUS);

        player.dropMessage(
                5,
                "You have arrived deep inside the Clocktower. "
                        + "Speak with the entrance NPC to challenge Papulatus."
        );
    }
}