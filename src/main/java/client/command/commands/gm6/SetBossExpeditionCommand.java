package client.command.commands.gm6;

import client.Character;
import client.Client;
import client.command.Command;
import server.expeditions.ExpeditionBossLog;
import server.expeditions.ExpeditionType;

public class SetBossExpeditionCommand extends Command {
    {
        setDescription("Set a player's boss count for a specific expedition");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 2 || params.length > 3) {
            player.yellowMessage("Syntax: !setboss/bossexped [<playername>] <boss name> <newcount>");
            return;
        }

        Character victim;
        ExpeditionBossLog.BossLogEntry bossType;
        int newCount;

        int target = 0; // 0 if self, 1 if other player
        if (params.length == 2) {
            victim = c.getWorldServer().getPlayerStorage().getCharacterByName(player.getName());
        } else {
            victim = c.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
            target = 1;
        }

        if (victim != null) {
            try {
                bossType = ExpeditionBossLog.BossLogEntry.valueOf(params[target].toUpperCase());
            } catch (Exception e) {
                player.yellowMessage("Boss type must be one of the boss expedition names.");
                return;
            }
            try {
                newCount = Integer.parseInt(params[1 + target]);
            } catch (Exception e) {
                player.yellowMessage("The new count must be an integer");
                return;
            }
            if (newCount >= 0) {
                int currentCount = ExpeditionBossLog.countPlayerEntries(victim.getId(), bossType);
                if (newCount < currentCount) { // remove entries
                    ExpeditionBossLog.removePlayerEntry(victim.getId(), bossType, currentCount - newCount);
                } else { // add entries
                    while (newCount-- > currentCount) {
                        ExpeditionBossLog.insertPlayerEntry(victim.getId(), bossType);
                        ExpeditionBossLog.setExpeditionCompleted(victim.getId(), ExpeditionType.valueOf(bossType.name()));
                    }
                }
                player.message("New boss count set for player: '" + victim.getName());
            } else {
                player.yellowMessage("New count must be >= 0");
            }
        } else {
            player.message("Player '" + params[0] + "' could not be found.");
        }
    }

}
