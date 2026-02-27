package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;

public class GuildLevelSpamCommand extends Command {
    {
        setDescription("Toggles guild level-up announcements in the chat.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        boolean nowSuppressed = !player.isSuppressGuildLvSpam();
        player.setSuppressGuildLvSpam(nowSuppressed);

        if (nowSuppressed) {
            player.dropMessage(5, "Guild level-up announcements are now HIDDEN.");
        } else {
            player.dropMessage(5, "Guild level-up announcements are now SHOWN.");
        }
    }
}
