package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import constants.id.NpcId;
import server.events.gm.Event;
import server.maps.FieldLimit;
import scripting.npc.NPCScriptManager;

public class EditShopCommand extends Command {
    public void execute(Client c, String[] splitted) {
        NPCScriptManager.getInstance().start(c, NpcId.MAPLE_ADMINISTRATOR, "ShopEditor", c.getPlayer());
    }
}
