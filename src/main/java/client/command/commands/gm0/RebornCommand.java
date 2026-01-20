package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.Job;
import client.command.Command;
import scripting.npc.NPCScriptManager;

public class RebornCommand extends Command {
    {
        setDescription("Trigger the Rebirth dialogue if you are max level.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        // 1. Check Level Limit (255)
        if (player.getLevel() < 255) {
            // [ENHANCED] Show current reborn count even if they can't rebirth yet
            player.dropMessage(5, "You have currently performed " + player.getReborns() + " rebirth(s).");
            player.dropMessage(5, "You must be Level 255 to perform another rebirth.");
            return;
        }

        // 2. Check Job Validity
        // We block Aran, Cygnus, and Evan as the rebirth logic is designed for Adventurers
        if (player.isAran() || player.isCygnus() || player.getJob().isA(Job.EVAN1)) {
            player.dropMessage(5, "This class cannot rebirth.");
            return;
        }

        // 3. Trigger the Script
        // We use NPC 11009 as the 'speaker' for this script.
        // This opens scripts/npc/rebirth.js
        NPCScriptManager.getInstance().start(c, 11009, "rebirth", player);
    }
}