package client.command.commands.gm6;

import client.Character;
import client.Client;
import client.command.Command;
import net.packet.Packet;
import net.server.Server;
import net.server.world.World;
import server.gambling.FourDResultManager;
import tools.PacketCreator;

import java.time.LocalDate;
import java.util.*;

public class Draw4DCommand extends Command {

    {
        setDescription("Manually runs today's 4D draw and evaluates winners.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        LocalDate today = LocalDate.now();

        // Check if draw already exists
        if (FourDResultManager.hasDrawToday(today)) {
            player.dropMessage("[4D] Draw already exists for today (" + today + ").");
            return;
        }

        // Generate 4D numbers
       String first = generateNumber();
  //      String first = String.format("%04d", 3597);
        String second = generateNumber();
        String third = generateNumber();
        List<String> starters = generateSet(10);
        List<String> consolations = generateSet(10);

        // Store and evaluate
        FourDResultManager.storeDraw(today, first, second, third, starters, consolations);
        FourDResultManager.evaluateBets(today);

        // Format draw result broadcast
        String message = "[4D] Manual draw triggered by " + player.getName() +
                ": 1st=" + first + ", 2nd=" + second + ", 3rd=" + third;
        Packet packet = PacketCreator.serverNotice(6, message);

        for (World world : Server.getInstance().getWorlds()) {
            Server.getInstance().broadcastMessage(world.getId(), packet);
        }
    }

    private String generateNumber() {
        return String.format("%04d", new Random().nextInt(10000));
    }

    private List<String> generateSet(int count) {
        Set<String> values = new LinkedHashSet<>();
        while (values.size() < count) {
            values.add(generateNumber());
        }
        return new ArrayList<>(values);
    }
}
