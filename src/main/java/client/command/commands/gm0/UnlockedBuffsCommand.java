package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.SkillFactory;
import client.command.Command;
import constants.skills.Assassin;
import constants.skills.Bowmaster;
import constants.skills.Buccaneer;
import constants.skills.Hermit;
import constants.skills.Priest;

public class UnlockedBuffsCommand extends Command {
    {
        setDescription("Activate unlocked buffs on self.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        for (int skillId : player.getUnlockedBuffs()) {
            switch (skillId) {
//                case Hermit.SHADOW_PARTNER: //Maybe only for XBOW/BOWMASTERS since they can't use sharpeyes?
                case Hermit.MESO_UP: //LVL 15
                case Priest.HOLY_SYMBOL: // lvl 15
                    SkillFactory.getSkill(skillId)
                            .getEffect(15)
                            .applyTo(player);
                    break;
                case Bowmaster.SHARP_EYES: // lvl 20
                    SkillFactory.getSkill(skillId)
                            .getEffect(20)
                            .applyTo(player);
                    break;
                case Assassin.HASTE: // max
                case Buccaneer.SPEED_INFUSION: // max
                    SkillFactory.getSkill(skillId)
                            .getEffect(SkillFactory.getSkill(skillId).getMaxLevel())
                            .applyTo(player);
                    break;

                default:
                    break;
            }
        }
    }
}

