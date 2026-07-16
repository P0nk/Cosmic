package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.Skill;
import client.SkillFactory;
import client.Stat;
import client.command.Command;
import client.command.ForgeBuffConstants;
import net.server.world.Party;
import net.server.world.PartyCharacter;

public class BuffPartyCommand extends Command {

    public BuffPartyCommand() {
        setDescription("Buff party members in your current map.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        Party party = player.getParty();

        if (party == null) {
            player.dropMessage(5, "You are not currently in a party.");
            return;
        }

        int buffedMembers = 0;

        for (PartyCharacter partyMember : party.getMembers()) {
            Character target = c.getChannelServer()
                    .getPlayerStorage()
                    .getCharacterById(partyMember.getId());

            // Offline or in another channel.
            if (target == null) {
                continue;
            }

            // Only buff party members in the caster's current map.
            if (target.getMapId() != player.getMapId()) {
                continue;
            }

            applyForgeBuffs(target);
            buffedMembers++;
        }

        if (buffedMembers == 0) {
            player.dropMessage(
                    5,
                    "No eligible party members were found in your current map."
            );
            return;
        }

        player.dropMessage(
                5,
                "The forge strengthens your party. " +
                        buffedMembers +
                        " companion(s) have been blessed."
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