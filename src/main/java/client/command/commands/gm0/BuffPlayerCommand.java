package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.Skill;
import client.SkillFactory;
import client.Stat;
import client.command.Command;
import client.command.ForgeBuffConstants;

public class BuffPlayerCommand extends Command {

    public BuffPlayerCommand() {
        setDescription("Receive the standard ForgeMS buff package.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        applyForgeBuffs(player);

        player.dropMessage(
                5,
                "The forge strengthens your resolve. All blessings have been bestowed."
        );
    }

    private void applyForgeBuffs(Character target) {
        for (int skillId : ForgeBuffConstants.BUFF_SKILL_IDS) {
            Skill skill = SkillFactory.getSkill(skillId);

            if (skill == null) {
                continue;
            }

            skill.getEffect(skill.getMaxLevel()).applyTo(target);
        }

        // Fully restore HP and MP.
        target.healHpMp();

        // Remove negative status effects.
        target.dispelDebuffs();
    }
}