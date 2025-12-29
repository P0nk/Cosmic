package server.subordinate;

import client.Client;
import client.Character;
import client.inventory.Equip;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import server.ItemInformationProvider;

import java.util.Objects;

public final class SubordinateManager {

    private SubordinateManager() {}

    /* =========================
       Rebirth Min-Level Rules
       =========================
       hands == rebirth count (your code uses hands as RB count)
       Example interpretation of your request:
       - RB1: minimum required level becomes RB_MIN_LVL_RB1
       - RB2: minimum required level becomes RB_MIN_LVL_RB2 (higher than RB1)
       - RB3+: you can extend later
    */
// SubordinateManager.java

    public static final int RB_MIN_LVL_RB1 = 70;
    public static final int RB_MIN_LVL_RB2 = 120;
    public static final int RB_MIN_LVL_RB3 = 160;
    public static final int RB_MIN_LVL_RB4 = 180;
    public static final int RB_MIN_LVL_RB5 = 225;
    public static final int RB_MIN_LVL_RB6 = 250;

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


    /* =========================
       Helpers migrated from NPCConversationManager
       ========================= */

    public static void removeEquipFromSlot(Client c, short itemSlot) {
        InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, itemSlot, (short) 1, false);
    }

    public static boolean isBlacklistedItem(ItemInformationProvider ii, int itemId, String itemName) {
        // You can keep your original logic; passing name in avoids re-looking up
        return itemName.contains("Reverse")
                || itemName.contains("Timeless")
                || itemName.contains("Fearless")
                || itemName.contains("Balrog's Fur Shoes");
    }

    public static short replaceWithCleanCopy(Client c, Character chr, short slot) {
        Inventory eqpInv = chr.getInventory(InventoryType.EQUIP);
        Equip old = (Equip) eqpInv.getItem(slot);
        if (old == null) return -1;

        int itemId = old.getItemId();

        // remove old
        InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, slot, (short) 1, false);

        // decide where new item will land
        short newSlot = eqpInv.getNextFreeSlot();
        if (newSlot <= 0) return -1;

        // add clean copy
        InventoryManipulator.addById(c, itemId, (short) 1, "", -1);

        // =========================
        // 🔹 CLEAR REQ LEVEL OVERRIDE HERE 🔹
        // =========================
        Equip newItem = (Equip) eqpInv.getItem(newSlot);
        if (newItem != null) {
            newItem.setReqLevelOverride((short) 0); // 0 = fallback to WZ req level
            chr.forceUpdateItem(newItem);
        }

        return newSlot;
    }


    /** This is your existing rebirthItem() logic, moved here with minimal changes. */
    public static void rebirthItem(Client c, Character chr, short itemSlot) {
        Inventory eqpInv = chr.getInventory(InventoryType.EQUIP);
        Equip selectedItem = (Equip) eqpInv.getItem(itemSlot);
        if (selectedItem == null) return;

        int newItemId = selectedItem.getItemId();
        int newItemType = newItemId / 10000;
        int hands = selectedItem.getHands(); // your rebirth count

        // remove old equip
        InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, itemSlot, (short) 1, false);

        // pick destination slot
        short newItemSlot = eqpInv.getNextFreeSlot();
        if (newItemSlot <= 0) return;

        // add fresh equip
        InventoryManipulator.addById(c, newItemId, (short) 1, "", -1);

        // fetch the newly created equip
        Equip newItem = (Equip) eqpInv.getItem(newItemSlot);
        if (newItem == null) return;

// ... keep your existing stat math exactly the same ...

// hands is the CURRENT rebirth count on the old item
        int newRebirthCount = hands + 1;

// apply rebirth count
        newItem.setHands((short) newRebirthCount);

// set req level override based on base reqlevel from WZ + your RB rules
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        int baseReq = ii.getEquipLevelReq(newItemId);
        int enforcedReq = getEffectiveReqLevel(baseReq, newRebirthCount);
        newItem.setReqLevelOverride((short) enforcedReq);

// persist + update client
        chr.forceUpdateItem(newItem);

    }


    public static String getWeaponType(int itemId) {
        int prefix = itemId / 10000;
        switch (prefix) {
            case 130: return "One-Handed Sword";
            case 131: return "One-Handed Axe";
            case 132: return "One-Handed Mace";
            case 133: return "Dagger";
            case 137: return "Wand";
            case 138: return "Staff";
            case 140: return "Two-Handed Sword";
            case 141: return "Two-Handed Axe";
            case 142: return "Two-Handed Mace";
            case 143: return "Spear";
            case 144: return "Pole Arm";
            case 145: return "Bow";
            case 146: return "Crossbow";
            case 147: return "Claw";
            case 148: return "Knuckle";
            case 149: return "Gun";
            default:  return "Unknown";
        }
    }
    public static int getEffectiveReqLevelForEquip(Equip eq) {
        int base;
        short ov = eq.getReqLevelOverride();
        if (ov > 0) base = ov;
        else base = ItemInformationProvider.getInstance().getEquipLevelReq(eq.getItemId());

        int rebirthCount = eq.getHands(); // your RB count storage
        return getEffectiveReqLevel(base, rebirthCount);
    }

}
