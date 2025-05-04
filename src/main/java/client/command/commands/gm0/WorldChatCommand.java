package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.Server;
import net.server.world.World;
import tools.PacketCreator;


public class WorldChatCommand extends Command {
    private int world;

    {
        setDescription("Sends chat to the whole world");
    }
    @Override
    public void execute (Client c, String[] params) {
        Character player = c.getPlayer();
        Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.serverNotice(6, "[" + player.getName() + "]: " + player.getLastCommandMessage()));
    }
}
