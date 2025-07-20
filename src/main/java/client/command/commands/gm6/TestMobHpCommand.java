package client.command.commands.gm6;

import client.command.Command;
import client.Character;
import client.Client;
import net.packet.Packet;
import server.life.Monster;
import server.maps.MapObject;
import server.maps.MapObjectType;
import tools.PacketCreator;

import java.util.Arrays;
import java.util.List;

public class TestMobHpCommand extends Command {

    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        player.dropMessage(5, "Testing for Mob HP Bar");
        List<MapObject> targets = player.getMap().getMapObjectsInRange(player.getPosition(), 500000, Arrays.asList(MapObjectType.MONSTER));
        for (int i = 0; i < targets.size(); i++) {
            Monster mob = (Monster) targets.get(i);
            Packet packet = PacketCreator.showMonsterHP(mob.getObjectId(), 1);
            System.out.println(packet);
            mob.broadcastMobHpBar(player);
            player.sendPacket(packet);
        }
    }
}
