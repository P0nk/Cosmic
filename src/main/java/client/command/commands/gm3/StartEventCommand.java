/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana
*/
package client.command.commands.gm3;

import client.Client;
import client.command.Command;
import scripting.npc.NPCScriptManager;

/**
 * Opens the Event Organizer Interface using NPC 9010005
 */
public class StartEventCommand extends Command {
    {
        setDescription("Opens the Event Management Interface.");
    }

    @Override
    public void execute(Client c, String[] params) {
        // We pass 'c.getPlayer()' as the 4th argument to match the existing
        // start(Client, int, String, Character) method in your NPCScriptManager.
        NPCScriptManager.getInstance().start(c, 9010000, "EventOrganizer", c.getPlayer());
    }
}