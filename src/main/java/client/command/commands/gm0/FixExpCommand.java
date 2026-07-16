package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.Stat;
import client.command.Command;

public class FixExpCommand extends Command {

    public FixExpCommand() {
        setDescription("Reset invalid or stuck experience.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        player.setExp(0);
        player.updateSingleStat(Stat.EXP, 0);

        player.dropMessage(
                5,
                "Your experience has been reset and synchronized."
        );

        player.dropMessage(
                5,
                "You remain level " + player.getLevel() + " with 0 EXP."
        );
    }
}