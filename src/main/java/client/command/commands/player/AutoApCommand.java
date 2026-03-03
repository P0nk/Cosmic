package client.command.commands.player;

import client.Character;
import client.Client;
import client.command.Command;

public class AutoApCommand extends Command {
    @Override
    public void execute(Client c, String[] params) {
        Character chr = c.getPlayer();
        if (chr.isAutoAssignAp()) {
            chr.setAutoAssignAp(false);
            chr.dropMessage(5, "Auto-AP allocation is now OFF. Unassigned AP will go into your AP pool.");
        } else {
            chr.setAutoAssignAp(true);
            chr.dropMessage(5,
                    "Auto-AP allocation is now ON. When you level up, AP will be automatically assigned to your primary stat.");
        }
    }
}
