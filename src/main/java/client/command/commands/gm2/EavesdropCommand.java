package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.guild.Guild;
import net.server.Server;

public class EavesdropCommand extends Command {
    {
        setDescription("Listen to a guild's chat messages");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character chr = client.getPlayer();
        if (params.length < 1) {
            chr.yellowMessage("Syntax: !eavesdrop <guild-name>");
            return;
        }

        String guildName = params[0];
        Guild guild = Server.getInstance().getGuildByName(guildName);
        
        if (guild == null) {
            chr.yellowMessage("Guild '" + guildName + "' not found.");
            return;
        }

        if (chr.isEavesdroppingGuild(guild.getId())) {
            chr.stopEavesdroppingGuild(guild.getId());
            chr.yellowMessage("Stopped eavesdropping on guild '" + guildName + "'.");
        } else {
            chr.startEavesdroppingGuild(guild.getId());
            chr.yellowMessage("Now eavesdropping on guild '" + guildName + "'.");
        }
    }
} 