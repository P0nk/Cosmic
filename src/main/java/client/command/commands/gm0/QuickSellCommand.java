/*
    @author noodle#0151
*/
package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.InventoryType;
import client.inventory.Item;
import constants.game.GameConstants;

public class QuickSellCommand extends Command {
    {
        setDescription("Quickly sells items (use @qs syntax for more)");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        boolean hasSpecialItem = false;
        for (Item item : player.getInventory(InventoryType.EQUIP)) {
            if (item.getItemId() == 1113232) {
                hasSpecialItem = true;
                break;
            }
        }

        if (hasSpecialItem) {
            String showMsg = "You can't sell your items if your Monsterbook Ring is present in your inventory.";
            player.showHint(showMsg, 200);
            return; // Exit if the special item is present in the equipment inventory
        }

        if (!player.isAlive()) {
            String showMsg = "This command cannot be used when you're dead.";
            player.showHint(showMsg, 200);
            return; // Exit if the player is not alive
        }

        if (player.getEventInstance() != null) {
            String showMsg1 = "This command cannot be used in expeditions or special instances.";
            player.showHint(showMsg1, 200);
            return; // Exit if the player is in an event instance
        }

        int sellPrice = 0;
        switch(params.length){
            case 1:
                if(params[0].toLowerCase().contains("syntax")){
                    String showMsg2 = "#b#e@qs##k - Opens NPC\r\n";
                    showMsg2 += "#b#e@qs#k #b<first slot>#k - Sells #r#eALL UNLEVELED equips & items#k from the first slot\r\n";
                    showMsg2 += "#b#e@qs#k #b<first slot> <last slot>#k - Sells #r#eALL UNLEVELED equips & items#k from the first slot to your inputted last slot";
                    player.showHint(showMsg2, 250);
                }
                else
                    sellPrice = player.sellAllPosLast(InventoryType.EQUIP, Short.parseShort(params[0]), (short) 96);
                break;
            case 2:
                sellPrice = player.sellAllPosLast(InventoryType.EQUIP, Short.parseShort(params[0]), Short.parseShort(params[1]));
                break;
            default:
                c.getAbstractPlayerInteraction().openNpc(9000041, "quicksell"); //This uses Donation Box NPC
                break;
        }

        if (sellPrice != 0)
        {
            String showMsg3 = "Sold items for " + GameConstants.numberWithCommas(sellPrice) + " mesos.";
            player.showHint(showMsg3, 250);
        }
    }
}