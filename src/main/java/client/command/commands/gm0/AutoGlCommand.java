package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.ForgeEconomyConstants;

public class AutoGlCommand extends Command {

    public AutoGlCommand() {
        setDescription(
                "Toggle automatic Golden Maple Leaf conversion for this login session."
        );
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (params.length != 1) {
            showUsage(player);
            return;
        }

        String selection = params[0].trim().toLowerCase();

        switch (selection) {
            case "on":
                if (player.isAutoGoldLeafEnabled()) {
                    player.dropMessage(
                            5,
                            "Automatic Golden Maple Leaf conversion is already enabled."
                    );
                    return;
                }

                player.setAutoGoldLeafEnabled(true);

                player.dropMessage(
                        5,
                        "Automatic Golden Maple Leaf conversion enabled until logout."
                );

                player.dropMessage(
                        5,
                        String.format(
                                "%,d mesos will automatically convert into 1 Golden Maple Leaf.",
                                ForgeEconomyConstants.MESOS_PER_GOLDEN_MAPLE_LEAF
                        )
                );

                /*
                 * Immediately convert if the player already has enough mesos.
                 */
                player.tryAutoGoldLeafConversion();
                break;

            case "off":
                if (!player.isAutoGoldLeafEnabled()) {
                    player.dropMessage(
                            5,
                            "Automatic Golden Maple Leaf conversion is already disabled."
                    );
                    return;
                }

                player.setAutoGoldLeafEnabled(false);

                player.dropMessage(
                        5,
                        "Automatic Golden Maple Leaf conversion disabled."
                );
                break;

            default:
                showUsage(player);
                break;
        }
    }

    private void showUsage(Character player) {
        player.dropMessage(
                5,
                "Usage: @autogl <on|off>"
        );

        player.dropMessage(
                5,
                "Current status: "
                        + (player.isAutoGoldLeafEnabled()
                        ? "Enabled"
                        : "Disabled")
        );

        player.dropMessage(
                5,
                String.format(
                        "%,d mesos converts into 1 Golden Maple Leaf.",
                        ForgeEconomyConstants.MESOS_PER_GOLDEN_MAPLE_LEAF
                )
        );
    }
}