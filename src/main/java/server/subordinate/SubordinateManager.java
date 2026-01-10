package server.subordinate;

import client.Client;
import client.Character;
import client.inventory.Equip;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import scripting.AbstractPlayerInteraction;
import server.ItemInformationProvider;

public class SubordinateManager {

    // ==========================================
    //           CONFIGURATION SECTION
    // ==========================================

    // 1. Base Carry-Over for Non-Attack Stats & DEFENSE
    private static final double RATE_BASE_STATS = 0.30; // 30%

    // 2. Custom Attack Rate Configuration (By Item Type)
    public static double getAttackRateForType(int type) {
        switch (type) {
            // --- WEAPONS ---
            case 130: return 0.30;  // 1H Sword
            case 131: return 0.30;  // 1H Axe
            case 132: return 0.30;  // 1H Blunt
            case 133: return 0.305; // Dagger
            case 137: return 0.29;  // Wand
            case 138: return 0.29;  // Staff

            case 140: return 0.30; // 2H Sword
            case 141: return 0.30; // 2H Axe
            case 142: return 0.30; // 2H Blunt
            case 143: return 0.30; // Spear
            case 144: return 0.30; // Polearm

            case 145: return 0.30;  // Bow
            case 146: return 0.30;  // Crossbow

            case 147: return 0.35;  // Claw
            case 148: return 0.33;  // Knuckle
            case 149: return 0.33;  // Gun

            // --- ARMORS ---
            case 100: return 0.28;  // Hats
            case 107: return 0.30;  // Shoes
            case 108: return 0.30;  // Gloves
            case 105: return 0.28;  // Overall
            case 110: return 0.28;  // Cape

            // --- DEFAULT ---
            default: return 0.30;
        }
    }

    // 3. System Limits
    private static final int MAX_STAT_CAP = 32767;
    private static final boolean DEBUG_MODE = true;

    // ==========================================
    //           CONSTANTS & HELPERS
    // ==========================================
    public static final int RB_MIN_LVL_RB1 = 70;
    public static final int RB_MIN_LVL_RB2 = 110;
    public static final int RB_MIN_LVL_RB3 = 140;
    public static final int RB_MIN_LVL_RB4 = 160;
    public static final int RB_MIN_LVL_RB5 = 180;
    public static final int RB_MIN_LVL_RB6 = 200;

    public static int getEffectiveReqLevel(int baseReqLevel, int rebirthCount) {
        int enforced = baseReqLevel;
        switch (rebirthCount) {
            case 1: enforced = Math.max(enforced, RB_MIN_LVL_RB1); break;
            case 2: enforced = Math.max(enforced, RB_MIN_LVL_RB2); break;
            case 3: enforced = Math.max(enforced, RB_MIN_LVL_RB3); break;
            case 4: enforced = Math.max(enforced, RB_MIN_LVL_RB4); break;
            case 5: enforced = Math.max(enforced, RB_MIN_LVL_RB5); break;
            default:
                if (rebirthCount >= 6) enforced = Math.max(enforced, RB_MIN_LVL_RB6);
                break;
        }
        return enforced;
    }

    public static int getEffectiveReqLevelForEquip(Equip eq) {
        int base;
        short ov = eq.getReqLevelOverride();
        if (ov > 0) base = ov;
        else base = ItemInformationProvider.getInstance().getEquipLevelReq(eq.getItemId());
        return getEffectiveReqLevel(base, eq.getHands());
    }

    // ==========================================
    //           LEVEL UP LOGIC
    // ==========================================
    public static void applyUpgrade(Character chr, Equip item, SubordinateMath.StatsResult stats) {
        item.setStr(stats.str);
        item.setDex(stats.dex);
        item.setInt(stats.int_);
        item.setLuk(stats.luk);
        item.setWatk(stats.watk);
        item.setMatk(stats.matk);
        item.setWdef(stats.wdef);
        item.setMdef(stats.mdef);
        item.setItemLevel(stats.level);
        item.setLevel(stats.hiddenLevel);
        chr.forceUpdateItem(item);
    }

    // ==========================================
    //           REBIRTH LOGIC
    // ==========================================
    public static void rebirthItem(Client c, Character chr, short itemSlot) {
        Inventory eqpInv = chr.getInventory(InventoryType.EQUIP);
        Equip selectedItem = (Equip) eqpInv.getItem(itemSlot);

        if (selectedItem == null) {
            return;
        }

        // 1. Capture Old Data
        int newItemId = selectedItem.getItemId();
        int oldHands = selectedItem.getHands(); // Current Rebirth count

        // Capture old stats specifically for the formula
        short oldStr = selectedItem.getStr();
        short oldDex = selectedItem.getDex();
        short oldInt = selectedItem.getInt();
        short oldLuk = selectedItem.getLuk();
        short oldWatk = selectedItem.getWatk();
        short oldMatk = selectedItem.getMatk();

        // 2. Remove Old Item
        // We remove it first to free up the slot (and potentially reuse it)
        InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, itemSlot, (short) 1, false);

        // 3. Add New Clean Item (Using the static helper)
        // This adds the item to the inventory AND returns the object so we can edit it
        Equip newItem = (Equip) AbstractPlayerInteraction.gainItem(c, newItemId, (short) 1);

        // SAFETY CHECK: If inventory was full (despite removal) or item invalid
        if (newItem == null) {
            chr.dropMessage(1, "Error: Could not gain new item. Rebirth cancelled.");
            return;
        }

        // 4. Calculate New Stats
        int nextHands = oldHands + 1;
        int newItemType = newItemId / 10000;
        double currentTypeAttackRate = getAttackRateForType(newItemType);

        short addStr, addDex, addInt, addLuk, addWatk, addMatk;
        short addWdef = 0, addMdef = 0;

        // --- FORMULA START ---
        // Note: We use the OLD stats (captured above) to calculate the bonus
        if (newItemType >= 130) { // Is Weapon
            addStr = (short) (oldStr * RATE_BASE_STATS);
            addDex = (short) (oldDex * RATE_BASE_STATS);
            addInt = (short) (oldInt * RATE_BASE_STATS);
            addLuk = (short) (oldLuk * RATE_BASE_STATS);
            addMatk = (short) (oldMatk * currentTypeAttackRate);
            addWatk = (short) (oldWatk * currentTypeAttackRate);
        } else { // Is Armor
            addStr = (short) (oldStr * RATE_BASE_STATS);
            addDex = (short) (oldDex * RATE_BASE_STATS);
            addInt = (short) (oldInt * RATE_BASE_STATS);
            addLuk = (short) (oldLuk * RATE_BASE_STATS);
            addWatk = (short) (oldWatk * currentTypeAttackRate);
            addMatk = (short) (oldMatk * currentTypeAttackRate);
        }

        // Defense Bonus Logic
        if (newItemType == 105) { // Overalls
            addWdef = (short) (150 * nextHands);
            addMdef = (short) (150 * nextHands);
        } else if (newItemType >= 100 && newItemType < 120) { // Other Armors
            addWdef = (short) (75 * nextHands);
            addMdef = (short) (75 * nextHands);
        }
        // --- FORMULA END ---

        // 5. Apply Stats to the NEW Item
        // We add the calculated bonus to the *clean* stats of the new item
        newItem.setStr((short) Math.min(MAX_STAT_CAP, newItem.getStr() + addStr));
        newItem.setDex((short) Math.min(MAX_STAT_CAP, newItem.getDex() + addDex));
        newItem.setInt((short) Math.min(MAX_STAT_CAP, newItem.getInt() + addInt));
        newItem.setLuk((short) Math.min(MAX_STAT_CAP, newItem.getLuk() + addLuk));
        newItem.setWatk((short) Math.min(MAX_STAT_CAP, newItem.getWatk() + addWatk));
        newItem.setMatk((short) Math.min(MAX_STAT_CAP, newItem.getMatk() + addMatk));
        newItem.setWdef((short) Math.min(MAX_STAT_CAP, newItem.getWdef() + addWdef));
        newItem.setMdef((short) Math.min(MAX_STAT_CAP, newItem.getMdef() + addMdef));

        // 6. Finalize Rebirth Counters
        newItem.setHands((short) nextHands);
        int itemReqLevel = ItemInformationProvider.getInstance().getEquipLevelReq(newItemId);
        int enforcedReq = getEffectiveReqLevel(itemReqLevel, nextHands);
        newItem.setReqLevelOverride((short) enforcedReq);

        // 7. Update User
        // Because we modified the object returned by gainItem (which is in the inventory),
        // we just need to tell the client to refresh that specific item.
        chr.forceUpdateItem(newItem);

        // Debug Log
        if (DEBUG_MODE) {
            System.out.println("[Rebirth Debug] Item: " + newItemId + " | RB: " + nextHands);
            System.out.println("Old Watk: " + oldWatk + " -> New Bonus: " + addWatk + " -> Final: " + newItem.getWatk());
        }
    }

    // ==========================================
    //        HELPER FOR SALVAGE / BOOM
    // ==========================================
    public static void removeEquipFromSlot(Client c, short slot) {
        InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, slot, (short) 1, false);
    }
}