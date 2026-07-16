package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.Stat;
import client.command.Command;

public class AutoStatCommand extends Command {

    public AutoStatCommand() {
        setDescription("Automatically assign newly gained AP for this login session.");
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
            case "str":
                enableAutoStat(player, Stat.STR, "STR");
                break;

            case "dex":
                enableAutoStat(player, Stat.DEX, "DEX");
                break;

            case "int":
                enableAutoStat(player, Stat.INT, "INT");
                break;

            case "luk":
                enableAutoStat(player, Stat.LUK, "LUK");
                break;

            case "off":
                player.setAutoStatTarget(null);
                player.dropMessage(
                        5,
                        "Autostat has been disabled."
                );
                break;

            default:
                showUsage(player);
                break;
        }
    }

    private void enableAutoStat(
            Character player,
            Stat target,
            String statName
    ) {
        player.setAutoStatTarget(target);

        player.dropMessage(
                5,
                "Autostat enabled for "
                        + statName
                        + " until log out or disabled by @autostat off."
        );
    }

    private void showUsage(Character player) {
        player.dropMessage(
                5,
                "Usage: @autostat <str|dex|int|luk|off>"
        );

        player.dropMessage(
                5,
                "Accepted inputs: str, dex, int, luk, off"
        );

        player.dropMessage(
                5,
                "Autostat only assigns AP gained after it is enabled."
        );
    }
}