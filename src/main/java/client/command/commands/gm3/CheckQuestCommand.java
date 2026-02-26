package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.QuestStatus;
import client.command.Command;
import server.quest.Quest;
import tools.StringUtil;
import java.util.Map;

public class CheckQuestCommand extends Command {
    {
        setDescription("Check the state of an active or completed quest for debugging.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (params.length < 1) {
            player.yellowMessage("Syntax: !checkquest <questid>");
            return;
        }

        int questId = -1;
        try {
            questId = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            player.yellowMessage("Please enter a valid quest ID.");
            return;
        }

        QuestStatus qs = player.getQuest(questId);

        if (qs == null) {
            player.yellowMessage("Quest ID " + questId + " status is null (not found for player).");
            return;
        }

        player.yellowMessage("=== Quest ID " + questId + " ===");
        player.yellowMessage("Status: " + qs.getStatus().name());
        player.yellowMessage("NPC: " + qs.getNpc());

        if (qs.getCustomData() != null && !qs.getCustomData().isEmpty()) {
            player.yellowMessage("Custom Data: " + qs.getCustomData());
        }

        if (qs.madeProgress()) {
            player.yellowMessage("Progress Mobs: " + qs.getProgress().size());
            for (Map.Entry<Integer, String> entry : qs.getProgress().entrySet()) {
                player.yellowMessage(" - MobID/ReqID " + entry.getKey() + ": " + entry.getValue());
            }
            player.yellowMessage("Combined Progress Data: " + qs.getProgressData());
        }

        if (qs.getStatus() == QuestStatus.Status.STARTED) {
            Quest q = Quest.getInstance(questId);
            if (q != null) {
                player.yellowMessage("Quest Info: " + q.getName());
            }
        }
    }
}
