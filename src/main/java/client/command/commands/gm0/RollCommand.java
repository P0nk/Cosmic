package client.command.commands.gm0;

import client.Character;
import client.Client;
import java.util.Random;
import client.command.Command;
import tools.PacketCreator;

/**
 *
 * @author noodle#0151
 */

public class RollCommand extends Command {
    {
        setDescription("Rolls a number between 0 to indicated number. Default: 100.");
    }

    @Override
    public void execute(Client c, String[] params) {

        Random rand = new Random();
        int max = 100;

        if (params.length > 0) {
            max = Integer.parseInt(params[0]);
            if (max == 0 || max > Integer.MAX_VALUE) {
                max = 100;
            }
        }
        Character player = c.getPlayer();
        int roll = rand.nextInt(max) + 1;
        String sendStr = player.getName() + " has rolled " + roll;

        sendStr += max==100 ? "!" : " out of " + max + "!";

        player.getMap().broadcastMessage(PacketCreator.getChatText(player.getId(), sendStr, false, 1));
        for(Character chr : player.getMap().getAllPlayers()){
            chr.message(player.getName() + " rolled a " + max + "-sided die and rolled a: " + roll + "!");
        }

    }
}