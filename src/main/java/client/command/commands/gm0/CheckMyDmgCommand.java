package client.command.commands.gm0;

import client.BuffStat;
import client.Character;
import client.Client;
import client.command.Command;

public class CheckMyDmgCommand extends Command{

/*
    @Author: Tom Chew
    @Description: Allows any player to check their own stats.
*/


    {
        setDescription("Check your own stats and base damage.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        int maxBase = player.calculateMaxBaseDamage(player.getTotalWatk());
        Integer watkBuff = player.getBuffedValue(BuffStat.WATK);
        Integer matkBuff = player.getBuffedValue(BuffStat.MATK);
        int blessing = player.getSkillLevel(10000000 * player.getJobType() + 12);

        if (watkBuff == null) watkBuff = 0;
        if (matkBuff == null) matkBuff = 0;

        player.dropMessage(5, "Your Stats:");
        player.dropMessage(5, "STR: " + player.getTotalStr() + ", DEX: " + player.getTotalDex() +
                ", INT: " + player.getTotalInt() + ", LUK: " + player.getTotalLuk());
        player.dropMessage(5, "WATK: " + player.getTotalWatk() + ", MATK: " + player.getTotalMagic());
        player.dropMessage(5, "WATK Buff: " + watkBuff + ", MATK Buff: " + matkBuff);
        player.dropMessage(5, "Blessing Level: " + blessing);
        player.dropMessage(5, "Your max base damage (before skills) is " + maxBase);
    }
}
