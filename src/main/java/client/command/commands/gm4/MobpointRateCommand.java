package client.command.commands.gm4;

import client.Character;
import client.Client;
import client.command.Command;
import tools.PacketCreator;

public class MobpointRateCommand extends Command {
    {
        setDescription("Set max mob per spawnpoint rate. (Should always be equal or higher than mob rate)");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !mobpoint <newrate>");
            player.yellowMessage("Current Mob Point Rate: " + c.getWorldServer().getMobperspawnpoint());
            return;
        }

        int maxmobperspawnpoint = Math.max(Integer.parseInt(params[0]), 1);
        c.getWorldServer().setMobperspawnpoint(maxmobperspawnpoint);
        c.getWorldServer().broadcastPacket(PacketCreator.serverNotice(6, "[Rate] Max Mob Per Spawnpoint Rate has been changed to " + maxmobperspawnpoint + "x."));
    }
}
