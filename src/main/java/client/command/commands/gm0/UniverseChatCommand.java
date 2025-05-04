package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import net.packet.Packet;
import net.server.Server;
import net.server.world.World;
import tools.PacketCreator;
import constants.game.GameConstants;

public class UniverseChatCommand extends Command {

    {
        setDescription("Sends chat to the entire universe (all worlds)");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        // Validate command usage
        if (params.length < 1) {
            player.dropMessage("Usage: !universechat <message>");
            return;
        }

        // Concatenate the message from the parameters
        String message = String.join(" ", params);

        // Retrieve the player's current world name using world ID
        String worldName = (c.getWorld() < GameConstants.WORLD_NAMES.length)
                ? GameConstants.WORLD_NAMES[c.getWorld()]
                : "Unknown World";

        // Create the final broadcast message
        String broadcastMessage = "[" + worldName + " - " + player.getName() + "] : " + message;

        // Create the packet
        Packet packet = PacketCreator.serverNotice(6, broadcastMessage);

        // Broadcast the message to all worlds
        for (World world : Server.getInstance().getWorlds()) {
            Server.getInstance().broadcastMessage(world.getId(), packet);
        }
    }
}
