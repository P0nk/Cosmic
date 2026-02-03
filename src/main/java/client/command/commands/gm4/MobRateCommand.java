package client.command.commands.gm4;

import client.Character;
import client.Client;
import client.command.Command;
import tools.PacketCreator;

public class MobRateCommand extends Command {
    {
        setDescription("Set mob spawn rate.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !mobrate <newrate>");
            player.yellowMessage("Current Mob Rate: " + c.getWorldServer().getMobrate());
            return;
        }

        float mobrate = Math.max(Float.parseFloat(params[0]), 1f);
        c.getWorldServer().setMobrate(mobrate);
        c.getWorldServer().broadcastPacket(PacketCreator.serverNotice(6, "[Rate] Mob Spawn Rate has been changed to " + mobrate + "x."));
    }
}
