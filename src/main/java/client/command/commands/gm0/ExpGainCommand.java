package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;

public class ExpGainCommand extends Command {
    {
        setDescription("Start tracking EXP gain for 60 seconds");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character chr = client.getPlayer();
        if (chr == null) {
            return;
        }

        chr.startExpTracking();
        chr.dropMessage(5, "Start Tracking Exp Gain For 60s");
    }
} 