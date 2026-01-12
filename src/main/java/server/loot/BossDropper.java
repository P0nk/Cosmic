package server.loot;

import java.awt.Point;
import java.util.*;

import client.inventory.Equip; // Required for Equip objects
import client.inventory.Item;
import client.inventory.Pet;
import constants.inventory.ItemConstants; // Assuming you have this
import server.maps.MapleMap;
import server.maps.MapObject;
import server.life.Monster;
import client.Character;

import static java.util.concurrent.TimeUnit.DAYS;

public final class BossDropper {

    private static final Random RNG = new Random();
    private static final Map<Integer, BossDropTable> BOSS_DROPS = new HashMap<>();

    static {
        // --- EXAMPLE CONFIGURATION ---

        // Example: Boss 8840000
        // 1. Guaranteed Drops (ID, Quantity)
        // Note: For Equips, quantity is always forced to 1 automatically.
//        addGuaranteed(8840000, 2000005, 50); // Drop 50 Power Elixirs
//        addGuaranteed(8840000, 4000000, 10); // Drop 10 Shells

        // 2. Random Pool (ID, Quantity)
//        addToPool(8840000, 1002357, 1); // Equip: Sword (Qty 1)
//        addToPool(8840000, 4001126, 5); // Item: 5 Maple Leaves
//        addToPool(8840000, 5000000, 1); // Pet: 1 Pet

        // Pick 1 to 2 options from the pool
//        setPoolPicks(8840000, 1, 2);
    }

    // =========================================
    //             DATA STRUCTURES
    // =========================================

    private static class BossDropTable {
        List<DropEntry> guaranteedItems = new ArrayList<>();
        List<DropEntry> poolItems = new ArrayList<>();
        int minPoolPicks = 1;
        int maxPoolPicks = 1;
    }

    public static class DropEntry {
        public final int itemId;
        public final int quantity;

        public DropEntry(int itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    // =========================================
    //             CONFIGURATION
    // =========================================

    private static BossDropTable getTable(int bossId) {
        return BOSS_DROPS.computeIfAbsent(bossId, k -> new BossDropTable());
    }

    /** Add a Guaranteed drop with specific quantity. */
    public static void addGuaranteed(int bossId, int itemId, int quantity) {
        getTable(bossId).guaranteedItems.add(new DropEntry(itemId, quantity));
    }

    /** Add a Pool drop with specific quantity. */
    public static void addToPool(int bossId, int itemId, int quantity) {
        getTable(bossId).poolItems.add(new DropEntry(itemId, quantity));
    }

    public static void setPoolPicks(int bossId, int min, int max) {
        BossDropTable table = getTable(bossId);
        table.minPoolPicks = min;
        table.maxPoolPicks = max;
    }

    // =========================================
    //             DROP LOGIC
    // =========================================

    public static void dropForMonster(
            MapleMap map, Monster mob, Character owner,
            Point seedPos, byte dropType, boolean playerDrop) {

        if (map == null || mob == null || owner == null) return;

        BossDropTable table = BOSS_DROPS.get(mob.getId());
        if (table == null) return;

        // 1. Process Guaranteed
        for (DropEntry entry : table.guaranteedItems) {
            spawnDrop(map, mob, owner, seedPos, dropType, playerDrop, entry);
        }

        // 2. Process Pool
        if (!table.poolItems.isEmpty()) {
            int range = table.maxPoolPicks - table.minPoolPicks + 1;
            int picks = table.minPoolPicks + RNG.nextInt(range);

            List<DropEntry> shuffledPool = new ArrayList<>(table.poolItems);
            Collections.shuffle(shuffledPool);

            for (int i = 0; i < picks && i < shuffledPool.size(); i++) {
                spawnDrop(map, mob, owner, seedPos, dropType, playerDrop, shuffledPool.get(i));
            }
        }
    }

    private static void spawnDrop(
            MapleMap map, Monster mob, Character owner,
            Point seedPos, byte dropType, boolean playerDrop,
            DropEntry entry) {

        Item itemToDrop;
        int itemId = entry.itemId;
        int qty = entry.quantity;

        // --- TYPE DETECTION ---
        boolean isEquip = (itemId < 2000000); // Standard check: IDs < 2mil are equips
        boolean isPet = ItemConstants.isPet(itemId);

        if (isPet) {
            // Pet Handling (Force Qty 1)
            int petId = Pet.createPet(itemId);
            if (petId == -1) return;

            itemToDrop = new Item(itemId, (short) 0, (short) 1, petId);
            long days = 90;
            long expiration = System.currentTimeMillis() + DAYS.toMillis(days);
            itemToDrop.setExpiration(expiration);
            itemToDrop.setOwner("");

        } else if (isEquip) {
            // Equip Handling (Uses Equip Class, Force Qty 1)
            // Constructor: (id, position, ringId, flag) - varies by source version
            // Common v83/v62 constructor: new Equip(id, (short) 0, (byte) -1, (byte) 0);
            itemToDrop = new Equip(itemId, (short) 0, (byte) -1);

        } else {
            // Standard Item Handling (Uses Item Class, Respects Qty)
            itemToDrop = new Item(itemId, (short) 0, (short) qty, (short) 0);
        }

        map.spawnItemDrop((MapObject) mob, owner, itemToDrop, seedPos, dropType, playerDrop);
    }

    /** Overload for simple calling */
    public static void dropForMonster(MapleMap map, Monster mob, Character owner) {
        if (mob == null || owner == null) return;
        byte dropType = (byte) (owner.getParty() != null ? 1 : 0);
        dropForMonster(map, mob, owner, mob.getPosition(), dropType, false);
    }
}