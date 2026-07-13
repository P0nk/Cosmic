package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import config.YamlConfig;

public class AutoRbCommand extends Command {

    {
        setDescription("Toggle automatic rebirth on or off.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (!YamlConfig.config.server.USE_AUTO_REBIRTH) {
            player.yellowMessage("Auto Rebirth is currently disabled.");
            return;
        }

        if (YamlConfig.config.server.REQUIRE_FIRST_MANUAL_REBIRTH
                && player.getReborns() < 1) {
            player.yellowMessage(
                    "You must complete your first rebirth manually before enabling Auto Rebirth."
            );
            return;
        }

        boolean enabled = !player.isAutoRebirth();
        player.setAutoRebirth(enabled);

        if (enabled) {
            player.yellowMessage("Auto Rebirth enabled.");
        } else {
            player.yellowMessage("Auto Rebirth disabled.");
        }
    }
}