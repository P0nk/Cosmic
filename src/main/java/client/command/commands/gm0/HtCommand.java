package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.commands.CommandMapConstants;

public class HtCommand extends Command {

    public HtCommand() {
        setDescription("Travel to the entrance of Horntail.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (player.getTrade() != null) {
            player.dropMessage(
                    5,
                    "You cannot use @ht while trading."
            );
            return;
        }

        if (player.getJailExpirationTimeLeft() > 0) {
            player.dropMessage(
                    5,
                    "You cannot use @ht while jailed."
            );
            return;
        }

        if (player.isChangingMaps()) {
            return;
        }

        player.changeMap(CommandMapConstants.HORNTAIL);

        player.dropMessage(
                5,
                "You have arrived at the entrance to Horntail's Cave. "
                        + "Speak with the entrance NPC to proceed."
        );
    }
}