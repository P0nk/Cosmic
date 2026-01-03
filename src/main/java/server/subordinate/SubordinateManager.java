package server.subordinate;

import client.Client;
import client.Character;
import client.inventory.Equip;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import server.ItemInformationProvider;

public class SubordinateManager {

    // ==========================================
    //           CONFIGURATION SECTION
    // ==========================================

    // 1. Base Carry-Over for Non-Attack Stats & DEFENSE
    private static final double RATE_BASE_STATS = 0.17; // 17%

    // 2. Custom Attack Rate Configuration (By Item Type)
    public static double getAttackRateForType(int type) {
        switch (type) {
            // --- WEAPONS ---
            case 130: return 0.18;  // 1H Sword
            case 131: return 0.18;  // 1H Axe
            case 132: return 0.18;  // 1H Blunt
            case 133: return 0.18;  // Dagger
            case 137: return 0.18;  // Wand
            case 138: return 0.18;  // Staff

            case 140: return 0.175; // 2H Sword
            case 141: return 0.175; // 2H Axe
            case 142: return 0.175; // 2H Blunt
            case 143: return 0.175; // Spear
            case 144: return 0.175; // Polearm

            case 145: return 0.19;  // Bow
            case 146: return 0.19;  // Crossbow

            case 147: return 0.20;  // Claw
            case 148: return 0.18;  // Knuckle
            case 149: return 0.20;  // Gun

            // --- ARMORS ---
            case 100: return 0.15;  // Hats
            case 108: return 0.20;  // Gloves
            case 105: return 0.15;  // Overall
            case 110: return 0.15;  // Cape

            // --- DEFAULT ---
            default: return 0.17;
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
        if (selectedItem == null) return;

        // Save old stats for Debug
        short oldWatk = selectedItem.getWatk();
        short oldWdef = selectedItem.getWdef();
        int oldHands = selectedItem.getHands();
        int newItemId = selectedItem.getItemId();

        // Determine Type
        int newItemType = newItemId / 10000;

        // 1. Swap Item
        InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, itemSlot, (short) 1, false);
        short newItemSlot = eqpInv.getNextFreeSlot();
        InventoryManipulator.addById(c, newItemId, (short) 1, null, -1);
        Equip newItem = (Equip) eqpInv.getItem(newItemSlot);
        if (newItem == null) return;

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        int itemReqLevel = ii.getEquipLevelReq(newItemId);

        short addStr = 0, addDex = 0, addInt = 0, addLuk = 0, addWatk = 0, addMatk = 0;
        short addWdef = 0, addMdef = 0;
        int nextHands = oldHands + 1;

        // --- FORMULA START ---

        double currentTypeAttackRate = getAttackRateForType(newItemType);

        if (newItemType >= 130) { // Is Weapon
            addStr = (short) (selectedItem.getStr() * RATE_BASE_STATS);
            addDex = (short) (selectedItem.getDex() * RATE_BASE_STATS);
            addInt = (short) (selectedItem.getInt() * RATE_BASE_STATS);
            addLuk = (short) (selectedItem.getLuk() * RATE_BASE_STATS);
            addMatk = (short) (selectedItem.getMatk() * currentTypeAttackRate + ((double) itemReqLevel / 3));
            addWatk = (short) (selectedItem.getWatk() * currentTypeAttackRate + ((double) itemReqLevel / 3));
        } else { // Is Armor
            addStr = (short) (selectedItem.getStr() * RATE_BASE_STATS);
            addDex = (short) (selectedItem.getDex() * RATE_BASE_STATS);
            addInt = (short) (selectedItem.getInt() * RATE_BASE_STATS);
            addLuk = (short) (selectedItem.getLuk() * RATE_BASE_STATS);
            addWatk = (short) (selectedItem.getWatk() * currentTypeAttackRate);
            addMatk = (short) (selectedItem.getMatk() * currentTypeAttackRate);
        }

        // Defense
        addWdef = (short) (selectedItem.getWdef() * RATE_BASE_STATS);
        addMdef = (short) (selectedItem.getMdef() * RATE_BASE_STATS);

        // --- FORMULA END ---

        // Apply Stats
        newItem.setStr((short) Math.min(MAX_STAT_CAP, newItem.getStr() + addStr));
        newItem.setDex((short) Math.min(MAX_STAT_CAP, newItem.getDex() + addDex));
        newItem.setInt((short) Math.min(MAX_STAT_CAP, newItem.getInt() + addInt));
        newItem.setLuk((short) Math.min(MAX_STAT_CAP, newItem.getLuk() + addLuk));
        newItem.setWatk((short) Math.min(MAX_STAT_CAP, newItem.getWatk() + addWatk));
        newItem.setMatk((short) Math.min(MAX_STAT_CAP, newItem.getMatk() + addMatk));
        newItem.setWdef((short) Math.min(MAX_STAT_CAP, newItem.getWdef() + addWdef));
        newItem.setMdef((short) Math.min(MAX_STAT_CAP, newItem.getMdef() + addMdef));

        // Finalize
        newItem.setHands((short) nextHands);
        int enforcedReq = getEffectiveReqLevel(itemReqLevel, nextHands);
        newItem.setReqLevelOverride((short) enforcedReq);

        // Debug Log
        if (DEBUG_MODE) {
            System.out.println("[Rebirth Debug] Item Type: " + newItemType + " | Atk Rate: " + (currentTypeAttackRate * 100) + "%");
            System.out.println("Watk: " + oldWatk + " -> " + newItem.getWatk());
        }

        chr.forceUpdateItem(newItem);
    }

    // ==========================================
    //        HELPER FOR SALVAGE / BOOM
    // ==========================================
    public static void removeEquipFromSlot(Client c, short slot) {
        InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, slot, (short) 1, false);
    }
}