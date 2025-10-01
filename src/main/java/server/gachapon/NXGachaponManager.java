package server.gachapon;

import client.Character;
import client.inventory.Pet;
import client.inventory.manipulator.InventoryManipulator;
import constants.inventory.ItemConstants;
import server.ItemInformationProvider;
import tools.PacketCreator;

import java.util.*;

public class NXGachaponManager {

    private static final Random random = new Random();

    public static class LootEntry {
        public final int itemId;
        public final String rarity;

        public LootEntry(int itemId, String rarity) {
            this.itemId = itemId;
            this.rarity = rarity;
        }
    }

    public static boolean roll(Character player, int currencyItemId,
                               Map<String, Double> rarityRates,
                               Map<String, List<LootEntry>> lootTable) {
        if (!player.getAbstractPlayerInteraction().haveItem(currencyItemId, 1)) return false;
        if (!player.canHold(1)) return false; // Simple inventory check

        player.getAbstractPlayerInteraction().gainItem(currencyItemId, (short) -1); // consume NXT

        // Roll rarity
        String rarity = pickRarity(rarityRates);
        if (rarity == null || !lootTable.containsKey(rarity)) return false;

        List<LootEntry> tierList = lootTable.get(rarity);
        if (tierList.isEmpty()) return false;

        LootEntry reward = tierList.get(random.nextInt(tierList.size()));
        if (ItemConstants.isPet(reward.itemId)) { // Checks if reward is pet
            int petid = Pet.createPet(reward.itemId);
            InventoryManipulator.addById(player.getClient(), reward.itemId, (short) 1, player.getName(), petid, (long) 365);
        } else {
            player.getAbstractPlayerInteraction().gainItem(reward.itemId, (short) 1);
        }

        String itemName = ItemInformationProvider.getInstance().getName(reward.itemId);
        player.dropMessage(6, "[NX Gachapon] You won: " + itemName + " (" + reward.rarity + ")");

        if ("ULTRA_RARE".equalsIgnoreCase(reward.rarity)) {
            player.getMap().broadcastMessage(PacketCreator.serverNotice(6,
                    player.getName() + " just hit the JACKPOT on the Premium NX Gachapon and got a rare " + itemName + "!"));
        }

        return true;
    }

    private static String pickRarity(Map<String, Double> rarityRates) {
        double roll = random.nextDouble() * 100;
        double cumulative = 0.0;

        for (Map.Entry<String, Double> entry : rarityRates.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }
        return null;
    }
}
