/*
   @Author: Alec
*/
package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import server.expeditions.BossLogData;
import server.expeditions.ExpeditionBossLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BossLogCommand extends Command {
    {
        setDescription("Check your weekly boss attempts.");
    }

    @Override
    public void execute(Client c, String[] params) {
        c.getAbstractPlayerInteraction().openNpc(3004030, "3004015");
    }
        /*
        List<Character> chars = new ArrayList<>();
        Character player = c.getPlayer();
        if (player.gmLevel() > 1 && params.length > 0) { // we can check all the players boss records across hwid
            Character victim = c.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
            if (victim != null) {
                List<Character> allChars = victim.getCharactersByHWID();
                chars.addAll(allChars);
            } else
                player.dropMessage(6, "Unknown player.");
        } else {
            chars.add(c.getPlayer());
        }
        for (Character character : chars) {
            Map<ExpeditionBossLog.BossLogEntry, BossLogData> bossMap = ExpeditionBossLog.getWeeklyBossEntries(character.getId(), player.gmLevel() <= 1 || params.length == 0);
            //MapleExpeditionBossLog.getDailyBossEntries(mapleCharacter.getId(), player.gmLevel() <= 1 || params.length == 0);
            //bossMap.putAll(MapleExpeditionBossLog.getWeeklyBossEntries(mapleCharacter.getId(), player.gmLevel() <= 1 || params.length == 0));
            if (!bossMap.isEmpty()) {
                if (player.gmLevel() > 1)
                    player.dropMessage(5, character.getName() + "'s boss count for the week: ");
                else
                    player.dropMessage(5, "Your boss count for the week: ");

                for (Map.Entry<ExpeditionBossLog.BossLogEntry, BossLogData> entry : bossMap.entrySet()) {
                    ExpeditionBossLog.BossLogEntry bossLogEntry = entry.getKey();
                    BossLogData bossLogData = entry.getValue();

                    String message = "You have killed " + bossLogEntry + ": " + bossLogData.getAttempts() + "/" + bossLogEntry.getEntries() + " attempts.";

                    if (bossLogData.getFailures() > 0) {
                        message += " Failed: " + bossLogData.getFailures() + " times." ;
                    }

                    player.dropMessage(5, message);
                }
            }
        }
    }
         */
}
