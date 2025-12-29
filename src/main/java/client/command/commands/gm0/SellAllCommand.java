package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import server.ItemInformationProvider;
import server.ItemBuybackManager;
import tools.Pair;
import scripting.npc.NPCScriptManager;
import constants.id.NpcId;
import constants.inventory.ItemConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SellAllCommand extends Command {

    private static final HashMap<Integer, Long> cooldowns = new HashMap<>();
    private static final HashMap<Integer, Integer> penalties = new HashMap<>();
    private static final int BASE_COOLDOWN_TIME = 10000; // Base cooldown time in milliseconds (10 seconds)
    private static final int PENALTY_TIME = 1000; // Initial penalty time in milliseconds (1 second)
    private static final int MAX_PENALTY_TIME = 5000; // Cap penalty time to 5 seconds max

    {
        setDescription("Sell all items from inventory. Usage: @sell [equip/use/etc/all]");
    }

    @Override
    public void execute(Client c, String[] params) {
        Integer playerId = c.getPlayer().getId();
        long currentTime = System.currentTimeMillis();
        int currentPenaltyCount = penalties.getOrDefault(playerId, 0);

        // Cap the penalty time to avoid excessive cooldown
        int penaltyTime = Math.min(currentPenaltyCount * PENALTY_TIME * currentPenaltyCount, MAX_PENALTY_TIME);
        long effectiveCooldown = BASE_COOLDOWN_TIME + penaltyTime; // Apply penalty cooldown

        // Remove expired cooldown entries
        cooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() > BASE_COOLDOWN_TIME * 2); // Cleanup after a reasonable period

        if (cooldowns.containsKey(playerId)) {
            long timePassed = currentTime - cooldowns.get(playerId);
            if (timePassed < effectiveCooldown) {
                long timeLeft = (effectiveCooldown - timePassed) / 1000; // Convert to seconds
                String message = String.format("You must wait %d more second(s) before using this command again. Repeated attempts have triggered %d penalty(ies), increasing your cooldown by an additional %d second(s).",
                        timeLeft, currentPenaltyCount, currentPenaltyCount * (PENALTY_TIME / 1000) * currentPenaltyCount);
                c.getPlayer().dropMessage(5, message);
                penalties.put(playerId, currentPenaltyCount + 1); // Increase penalty count for next time
                return;
            }
        }

        Character player = c.getPlayer();

        if (params.length > 0) {
            // Quick sell mode
            String command = params[0].toLowerCase();
            int totalMesos = 0;
            int totalItems = 0;

            switch(command) {
                case "equip":
                case "equipment":
                    Pair<Integer, Integer> result = sellAllItems(c, player, InventoryType.EQUIP);
                    totalMesos = result.getLeft();
                    totalItems = result.getRight();
                    break;

                case "use":
                    result = sellAllItems(c, player, InventoryType.USE);
                    totalMesos = result.getLeft();
                    totalItems = result.getRight();
                    break;

                case "etc":
                    result = sellAllItems(c, player, InventoryType.ETC);
                    totalMesos = result.getLeft();
                    totalItems = result.getRight();
                    break;

                case "all":
                case "everything":
                    // Sell from all inventories
                    Pair<Integer, Integer> result1 = sellAllItems(c, player, InventoryType.EQUIP);
                    Pair<Integer, Integer> result2 = sellAllItems(c, player, InventoryType.USE);
                    Pair<Integer, Integer> result3 = sellAllItems(c, player, InventoryType.ETC);
                    totalMesos = result1.getLeft() + result2.getLeft() + result3.getLeft();
                    totalItems = result1.getRight() + result2.getRight() + result3.getRight();
                    break;

                default:
                    player.dropMessage("Usage: @sellall [equip/use/etc/all] - or just @sellall for GUI");
                    return;
            }

            // Show results
            if (totalMesos > 0) {
                player.gainMeso(totalMesos, true);
                player.yellowMessage("Success! Sold " + totalItems + " items for " + totalMesos + " mesos!");
            } else {
                player.yellowMessage("No items were sold. Check if items are locked or untradeable.");
            }
        } else {
            // Open GUI
            NPCScriptManager.getInstance().start(c, NpcId.MAPLE_ADMINISTRATOR, "sellAllAssistant", player);
        }

        cooldowns.put(playerId, currentTime); // Update last used time
        penalties.put(playerId, 0); // Reset penalty count on successful use
    }

    public static Pair<Integer, Integer> sellAllItems(Client c, Character player, InventoryType type) {
        Inventory inventory = player.getInventory(type);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        List<Item> itemsToSell = new ArrayList<>();

        // Collect sellable items
        for (Item item : inventory.list()) {
            if (item != null
                    && item.getItemId() > 0
                    && item.getQuantity() > 0
                    && !ii.isDropRestricted(item.getItemId())
                    && !item.isUntradeable()) {
                // Check if item is protected
                if (type == InventoryType.EQUIP) {
                    Equip selectedItem = (Equip) inventory.getItem(item.getPosition());
                    if ((selectedItem.getHands() > 0) || (selectedItem.getItemLevel() > 1)) {
                        continue;
                    }
                }
                if ((item.getFlag() & ItemConstants.SELLALL_PROTECTED) != ItemConstants.SELLALL_PROTECTED) {
                    itemsToSell.add(item);
                }
            }
        }

        int mesoGain = 0;
        int itemCount = 0;

        // Process each item
        for (Item item : itemsToSell) {
            int itemId = item.getItemId();
            boolean isThrowingStar = itemId / 10000 == 207;
            short sellQuantity = isThrowingStar ? (short) 1 : item.getQuantity();

            int sellPrice = 0;
            try {
                sellPrice = ii.getPrice(itemId, sellQuantity);
            } catch (Exception e) {
                continue; // Skip items with no price data
            }

            if (sellPrice > 0) {
                // Add to buyback BEFORE removing
                ItemBuybackManager.getInstance().addBuybackItem(
                        player,
                        item.copy(),
                        sellPrice,
                        sellQuantity
                );

                // Remove from inventory
                InventoryManipulator.removeFromSlot(c, type, item.getPosition(), sellQuantity, false, true);

                mesoGain += sellPrice;
                itemCount++;
            }
        }

        return new Pair<>(mesoGain, itemCount);
    }
}
