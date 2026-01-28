package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.manipulator.InventoryManipulator;
import server.maps.HiredMerchant;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import net.server.Server;

public class CloseShopCommand extends Command {

    {
        setDescription(
                "Closes your Hired Merchant and retrieves all funds/items. Items fitting in inventory are retrieved; others remain in storage.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character chr = c.getPlayer();

        // 1. Close Active Shop if Open
        HiredMerchant hm = Server.getInstance().getWorld(chr.getWorld()).getHiredMerchant(chr.getId());
        if (hm != null) {
            chr.dropMessage(5, "Closing active Hired Merchant...");
            // closing the shop will trigger its internal save and partial return logic
            // but we will run our own comprehensive retrieval after to catch leftovers
            hm.closeOwnerMerchant(chr);
        } else {
            // Debug message so user knows why nothing happened if they thought it was open
            chr.dropMessage(5, "No active Hired Merchant found to close.");
        }

        // 2. Withdraw Funds (Mesos + B-Coins)
        chr.withdrawMerchantMesos(); // Uses our new DB-driven logic from Character.java

        // 3. Retrieve Items (Partial/Overflow Support)
        try {
            List<Pair<Item, InventoryType>> items = ItemFactory.MERCHANT.loadItems(chr.getId(), false);

            if (items.isEmpty()) {
                chr.dropMessage(5, "No items found in merchant storage.");
                return;
            }

            int retrieved = 0;
            int remaining = 0;

            try (Connection con = DatabaseConnection.getConnection()) {
                for (Pair<Item, InventoryType> pair : items) {
                    Item item = pair.getLeft();

                    // Attempt to add to inventory
                    if (InventoryManipulator.addFromDrop(c, item, false)) {
                        // Success: Remove specific item from DB to prevent duping/persistence
                        try (PreparedStatement ps = con
                                .prepareStatement("DELETE FROM inventoryitems WHERE inventoryitemid = ?")) {
                            ps.setInt(1, item.getUniqueId());
                            ps.executeUpdate();
                        }
                        retrieved++;
                    } else {
                        // Fail: Inventory full, item remains in DB
                        remaining++;
                    }
                }
            }

            chr.dropMessage(1, "Detailed Report:\n" +
                    "- Funds Withdrawn.\n" +
                    "- Items Retrieved: " + retrieved + "\n" +
                    "- Items Remaining (Full Inv): " + remaining);

            if (remaining > 0) {
                chr.dropMessage(5, "Some items remain in storage. Make space and use @closeshop again.");
            }

            // Save character state to persist the new inventory items
            chr.saveCharToDB(false);

        } catch (SQLException e) {
            chr.dropMessage(5, "Error retrieving items: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
