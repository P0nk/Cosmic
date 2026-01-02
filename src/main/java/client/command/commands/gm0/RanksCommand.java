package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import constants.id.NpcId;
import net.server.Server;
import net.server.guild.GuildPackets;
import tools.Pair;

import java.util.List;

public class RanksCommand extends Command {
    {
        setDescription("Show player rankings.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        List<Pair<String, Integer>> worldRanking =
                Server.getInstance().getWorldPlayerRanking(player.getWorld());

        // Minimal diagnostic (safe for long-term maintenance)
        System.out.println("[Ranks] world=" + player.getWorld()
                + ", entries=" + worldRanking.size());

        player.sendPacket(
                GuildPackets.showPlayerRanks(
                        NpcId.MAPLE_ADMINISTRATOR,
                        worldRanking
                )
        );
    }
}
