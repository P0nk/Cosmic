package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;
import tools.PacketCreator;

public class UnstuckCommand extends Command {

    public UnstuckCommand() {
        setDescription("Move your character to the default portal of the current map.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (player.isChangingMaps()) {
            return;
        }

        if (player.getTrade() != null) {
            player.dropMessage(
                    5,
                    "You cannot use @unstuck while trading."
            );
            return;
        }

        /*
         * Clear any stuck NPC or quest conversation before moving.
         */
        NPCScriptManager.getInstance().dispose(c);
        QuestScriptManager.getInstance().dispose(c);
        c.removeClickedNPC();
        c.sendPacket(PacketCreator.enableActions());

        /*
         * Move the player to portal 0 of their current map.
         */
        if (player.getMap().getPortal(0) == null) {
            player.dropMessage(
                    5,
                    "This map does not have a valid recovery portal."
            );
            return;
        }

        player.changeMap(
                player.getMap(),
                player.getMap().getPortal(0)
        );

        player.dropMessage(
                5,
                "You have been moved to a safe location in the current map."
        );
    }
}