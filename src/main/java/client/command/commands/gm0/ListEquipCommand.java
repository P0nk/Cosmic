package client.command.commands.gm0;

import client.Client;
import client.Character;
import client.command.Command;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import server.ItemInformationProvider;

public class ListEquipCommand extends Command {

    {
        setDescription("Lists all EQUIP items in your inventory with itemId and name.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character chr = c.getPlayer();
        Inventory equipInv = chr.getInventory(InventoryType.EQUIP);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        if (equipInv == null || equipInv.list().isEmpty()) {
            chr.yellowMessage("Your EQUIP inventory is empty.");
            return;
        }

        chr.yellowMessage("=== EQUIP INVENTORY LIST ===");

        int count = 0;

        for (Item item : equipInv.list()) {
            if (item == null) continue;

            int itemId = item.getItemId();
            String name = ii.getName(itemId);

            if (name == null) {
                name = "(Unknown Item)";
            }

            chr.yellowMessage(
                    "(" + item.getPosition() +
                            ")" + itemId +
                            " - " + name
            );
            count++;
        }

        chr.yellowMessage("Total equips: " + count);
    }
}
