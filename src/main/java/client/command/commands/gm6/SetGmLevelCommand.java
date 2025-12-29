package client.command.commands.gm6;

import client.Character;
import client.Client;
import client.command.Command;

public class SetGmLevelCommand extends Command {
    {
        setDescription("Set GM level of a player.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (params.length < 2) {
            player.yellowMessage("Syntax: !setgmlevel <playername> <newlevel>");
            return;
        }

        int newLevel;
        try {
            newLevel = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            player.dropMessage("GM level must be a number.");
            return;
        }

        Character target = c.getChannelServer()
                .getPlayerStorage()
                .getCharacterByName(params[0]);

        if (target == null) {
            player.dropMessage("Player '" + params[0] + "' was not found on this channel.");
            return;
        }

        // Apply GM level
        target.setGMLevel(newLevel);
        target.getClient().setGMLevel(newLevel);

        // Determine message
        String message;
        switch (newLevel) {
            case 0:
                message = "You are now a normal player.";
                break;
            case 1:
                message = "You are now a donator.";
                break;
            case 2:
                message = "You are now a Jr GM.";
                break;
            case 3:
                message = "You are now a normal GM.";
                break;
            case 4:
                message = "You are now a Super GM.";
                break;
            default:
                message = "You are now a level " + newLevel +
                        " GM. See @commands for a list of available commands.";
                break;
        }

        // Notify target and executor
        target.dropMessage(message);
        player.dropMessage(target.getName() + " is now GM level " + newLevel + ".");
    }
}
