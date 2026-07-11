package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.Server;
import net.server.world.World;
import tools.PacketCreator;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CheckCommand extends Command {
    @Override
    public void execute(Client client, String[] params) {
        Character player = client.getPlayer();
        World world = player.getWorldServer();
        int expRate = world.getExpRate();
        int dropRate = world.getDropRate();
        int mesoRate = world.getMesoRate();


        String message = "EXP Rate: #b" + expRate + "x #n#k| Meso Rate: #b" + mesoRate + "x #n#k| Drop Rate: #b" + dropRate + "x#k\r\n";
        message += "#nServer Time: #b" + formatTime(System.currentTimeMillis(),"MM/dd/yyyy   HH:mm:ss") + "\r\n";
        message += "#n#kTime until daily reset: #b" + formatMillis(Server.getTimeLeftForNextDay(), "%dhours  %02dminutes", false) + "\r\n";
        message += "#n#kTime until weekly reset: #b" + formatMillis(Server.getTimeLeftUntilNextThursday(), "%ddays  %dhours  %02dminutes", true);

        player.showHint(message);
//        String[] msg = message.split("\n");
//        for(String m:msg) {
//            sendMessageToPlayer(client, m);
//        }
    }

    private void sendMessageToPlayer(Client client, String string) {
        client.sendPacket(PacketCreator.serverNotice(6, string));
    }

    private String formatTime(long millis, String pattern) {
        Date date = new Date(millis);
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    private String formatMillis(long millis, String pattern, boolean needDays) {
        long totalseconds = millis/1000;
        long days = totalseconds/86400;
        long hours = totalseconds/3600;
        if(needDays) hours = (totalseconds%86400) / 3600;
        long minutes = (totalseconds %3600)/60;
        long seconds = totalseconds%60;
        if(needDays) return String.format(pattern, days, hours, minutes);
        else return String.format(pattern, hours, minutes);
    }




}
