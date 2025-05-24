package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import constants.game.GameConstants;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;

public class OnlineThreeCommand extends Command {

    {
        setDescription("Show all online players across all worlds.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        int totalOnline = 0;

        // Loop over all worlds
        for (World world : Server.getInstance().getWorlds()) {

            String worldName = (world.getId() < GameConstants.WORLD_NAMES.length)
                    ? GameConstants.WORLD_NAMES[world.getId()]
                    : "World " + world.getId();

            player.dropMessage(6, "Players in " + worldName + ":");

            for (Channel channel : world.getChannels()) {
                int size = channel.getPlayerStorage().getAllCharacters().size();
                totalOnline += size;

                if (size > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("  Channel ").append(channel.getId()).append(" (").append(size).append(" online): ");

                    for (Character chr : channel.getPlayerStorage().getAllCharacters()) {
                        sb.append(Character.makeMapleReadable(chr.getName())).append(", ");
                    }

                    // Trim trailing comma
                    if (sb.length() >= 2) {
                        sb.setLength(sb.length() - 2);
                    }

                    player.dropMessage(6, sb.toString());
                }
            }
        }

        player.dropMessage(6, "Total players online across all worlds: " + totalOnline);
    }
}
