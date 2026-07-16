package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.commands.CommandMapConstants;

public class ZakCommand extends Command {

    public ZakCommand() {
        setDescription("Travel to the entrance of Zakum.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (player.getTrade() != null) {
            player.dropMessage(
                    5,
                    "You cannot use @zak while trading."
            );
            return;
        }

        if (player.getJailExpirationTimeLeft() > 0) {
            player.dropMessage(
                    5,
                    "You cannot use @zak while jailed."
            );
            return;
        }

        if (player.isChangingMaps()) {
            return;
        }

        player.changeMap(CommandMapConstants.ZAKUM);

        player.dropMessage(
                5,
                "You have arrived at the Door to Zakum. "
                        + "Speak with the expedition guide to enter."
        );
    }
}