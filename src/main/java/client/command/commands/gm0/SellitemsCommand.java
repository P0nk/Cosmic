// created by Mark

package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import constants.inventory.ItemConstants;
import server.ItemInformationProvider;

import java.util.ArrayList;

public class SellitemsCommand extends Command {
    {
        setDescription("Sell all items in an inventory tab.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: @sell <equip, use, or etc.>");
            return;
        }
        String type = params[0];

        switch (type) {
            case "equip":
                ArrayList<Item> equipItems = new ArrayList<Item>(c.getPlayer().getInventory(InventoryType.EQUIP).list());

                for (Item tempItem : equipItems) {
                    if (tempItem.isUntradeable()) {
                        continue;
                    }

                    ItemInformationProvider iii = ItemInformationProvider.getInstance();
                    int itemPrice = iii.getPrice(tempItem.getItemId(), tempItem.getQuantity());
                    player.gainMeso(itemPrice, true);

                    InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, (byte) tempItem.getPosition(), tempItem.getQuantity(), false, false);
                }
                player.yellowMessage("Equipment Slot Sold.");
                break;
            case "use":
                ArrayList<Item> useItems = new ArrayList<Item>(c.getPlayer().getInventory(InventoryType.USE).list());

                for (Item tempItem : useItems) {
                    if (tempItem.isUntradeable() || isSellBlockedForInventoryType(InventoryType.USE, tempItem.getItemId())) {
                        continue;
                    }

                    ItemInformationProvider iii = ItemInformationProvider.getInstance();
                    int itemPrice = iii.getPrice(tempItem.getItemId(), tempItem.getQuantity());
                    player.gainMeso(itemPrice, true);

                    InventoryManipulator.removeFromSlot(c, InventoryType.USE, (byte) tempItem.getPosition(), tempItem.getQuantity(), false, false);
                }
                player.yellowMessage("Equipment Slot Sold.");
                break;
            case "etc":
                ArrayList<Item> etcItems = new ArrayList<Item>(c.getPlayer().getInventory(InventoryType.ETC).list());

                for (Item tempItem : etcItems) {
                    if (tempItem.isUntradeable() || isSellBlockedForInventoryType(InventoryType.ETC, tempItem.getItemId())) {
                        continue;
                    }

                    ItemInformationProvider iii = ItemInformationProvider.getInstance();
                    int itemPrice = iii.getPrice(tempItem.getItemId(), tempItem.getQuantity());
                    player.gainMeso(itemPrice, true);

                    InventoryManipulator.removeFromSlot(c, InventoryType.ETC, (byte) tempItem.getPosition(), tempItem.getQuantity(), false, false);
                }
                player.yellowMessage("Equipment Slot Sold.");
                break;
            default:
                player.yellowMessage("Slot" + type + " does not exist!");
                break;
        }
    }
    public boolean isSellBlockedForInventoryType(InventoryType invType, int itemId){
        switch (invType) {
            case USE:
                return ItemConstants.useBlockedIds.contains(itemId);
            case ETC:
                return ItemConstants.etcBlockedIds.contains(itemId);
        }

        return true;
    }
}


