package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import config.YamlConfig;

public class ExpGainCommand extends Command {
    {
        setDescription("Start tracking EXP gain");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character chr = client.getPlayer();
        if (chr == null) {
            return;
        }

        chr.startExpTracking();
        int duration = YamlConfig.config.server.EXP_TRACKING_DURATION / 1000;
        chr.dropMessage(5, "Start Tracking Exp Gain For " + duration + " seconds");
    }
} 