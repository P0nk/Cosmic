package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;

public class GfxCommand extends Command {
    @Override
    public void execute(Client client, String[] params) {
        Character player = client.getPlayer();

        if (params.length < 1) {
            player.showHint("Please use @gfx on or @gfx off.");
            return;
        }

        String action = params[0].toLowerCase();

        if (action.equals("on")) {
            player.setshowAttackSkill(true);
            player.showHint("Skill Effects are #n#bON#k.");
        } else if (action.equals("off")) {
            player.setshowAttackSkill(false);
            player.showHint("Skill Effects are #n#rOFF#k.");
        } else {
            player.showHint("Use #n#bON#k or #n#rOFF#k.");
            return;
        }
    }
}


