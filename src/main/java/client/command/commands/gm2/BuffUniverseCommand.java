package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.SkillFactory;
import client.command.Command;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;

public class BuffUniverseCommand extends Command {

    {
        setDescription("Give GM buffs to all characters in all worlds.");
    }

    @Override
    public void execute(Client c, String[] params) {
        for (World world : Server.getInstance().getWorlds()) {
            for (Channel ch : world.getChannels()) {
                for (Character chr : ch.getPlayerStorage().getAllCharacters()) {
                    SkillFactory.getSkill(9101001).getEffect(SkillFactory.getSkill(9101001).getMaxLevel()).applyTo(chr,
                            true);
                    SkillFactory.getSkill(9101002).getEffect(SkillFactory.getSkill(9101002).getMaxLevel()).applyTo(chr,
                            true);
                    SkillFactory.getSkill(9101003).getEffect(SkillFactory.getSkill(9101003).getMaxLevel()).applyTo(chr,
                            true);
                    SkillFactory.getSkill(9101008).getEffect(SkillFactory.getSkill(9101008).getMaxLevel()).applyTo(chr,
                            true);
                    SkillFactory.getSkill(1005).getEffect(SkillFactory.getSkill(1005).getMaxLevel()).applyTo(chr, true);
                    SkillFactory.getSkill(5121009).getEffect(SkillFactory.getSkill(5121009).getMaxLevel()).applyTo(chr,
                            true);
                    SkillFactory.getSkill(3121002).getEffect(SkillFactory.getSkill(3121002).getMaxLevel()).applyTo(chr,
                            true);
                    SkillFactory.getSkill(4111001).getEffect(SkillFactory.getSkill(4111001).getMaxLevel()).applyTo(chr,
                            true);
                }
            }
        }
    }
}
