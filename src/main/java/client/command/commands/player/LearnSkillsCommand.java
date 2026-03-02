package client.command.commands.player;

import client.Character;
import client.Client;
import client.Skill;
import client.SkillFactory;
import client.command.Command;
import provider.Data;
import provider.DataProviderFactory;
import provider.wz.WZFiles;
import constants.game.GameConstants;

public class LearnSkillsCommand extends Command {
    {
        setDescription("Learn missing 4th job skills if you have rebirthed at least once.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (player.getReborns() < 1) {
            player.dropMessage(1, "You must have rebirthed at least once to use this command.");
            return;
        }

        int jobId = player.getJob().getId();
        if (GameConstants.getSkillBook(jobId) != 4) {
            player.dropMessage(1, "You must be in your 4th job to learn missing skills.");
            return;
        }

        int skillsLearned = 0;

        for (Data skill_ : DataProviderFactory.getDataProvider(WZFiles.STRING).getData("Skill.img").getChildren()) {
            try {
                int skillId = Integer.parseInt(skill_.getName());
                // Only look at 4th job skills (8 digits or specific ranges)
                // A reliable way is checking if the skill belongs to the player's job tree
                // and if it's a 4th job skill based on its ID.
                if (GameConstants.isInJobTree(skillId, jobId)) {
                    Skill skill = SkillFactory.getSkill(skillId);
                    if (skill != null && skill.isFourthJob()) {
                        if (player.getSkillLevel(skill) == 0 && player.getMasterLevel(skill) == 0) {
                            if (!skill.isBeginnerSkill()) {
                                player.changeSkillLevel(skill, (byte) 0, 0, -1);
                                skillsLearned++;
                            }
                        }
                    }
                }

            } catch (NumberFormatException e) {
                // Ignore non-integer skill IDs in WZ
            }
        }

        if (skillsLearned > 0) {
            player.dropMessage(1, "You have successfully learned " + skillsLearned + " missing 4th job skills.");
        } else {
            player.dropMessage(1, "You do not have any missing 4th job skills to learn.");
        }
    }
}
