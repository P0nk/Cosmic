package server.loot;

import java.awt.Point;
import java.util.*;

import client.inventory.Pet;
import constants.inventory.ItemConstants;
import server.maps.MapleMap;
import server.maps.MapObject;
import server.life.Monster;
import client.Character;
import client.inventory.Item;

import static java.util.concurrent.TimeUnit.DAYS;

public final class BossDropper {

    private static final Random RNG = new Random();

    // MonsterID -> list of DropEntry (itemId + dropRate)
    private static final Map<Integer, List<DropEntry>> MONSTER_DROPS = new HashMap<>();

    static {
        // Example drops
        // dropRate is in 1/1000000 (like FoodDropper)
//        addDrop(8840000, 5000330, 5000);   // Vonleon drops item 5000330 at 0.5%
//        addDrop(100100, 2001, 100000); // Monster 100100 drops item 2001 at 10%
//        addDrop(200200, 3000, 1000);   // Monster 200200 drops item 3000 at 0.1%
    }

    public static class DropEntry {
        public final int itemId;
        public final int chance; // out of 1,000,000

        public DropEntry(int itemId, int chance) {
            this.itemId = itemId;
            this.chance = chance;
        }
    }

    private static void addDrop(int monsterId, int itemId, int chance) {
        MONSTER_DROPS.computeIfAbsent(monsterId, k -> new ArrayList<>())
                .add(new DropEntry(itemId, chance));
    }

    /** Drops items for a specific monster ID. */
    public static void dropForMonster(
            MapleMap map,
            Monster mob,
            Character owner,
            Point seedPos,
            byte dropType,
            boolean playerDrop
    ) {
        if (map == null || mob == null || owner == null) return;

        List<DropEntry> drops = MONSTER_DROPS.get(mob.getId());
        if (drops == null || drops.isEmpty()) return;

        for (DropEntry entry : drops) {
            int roll = RNG.nextInt(1_000_000) + 1;
            if (roll <= entry.chance) {
                Item item = new Item(entry.itemId, (short) 0, (short) 1);
                if (ItemConstants.isPet(item.getItemId())) { // Checks if pet
                    int petid = Pet.createPet(item.getItemId());
                    Item toDrop = new Item(item.getItemId(), (short) 0, (short) 1, petid);
                    long days = 999;
                    long expiration = System.currentTimeMillis() + DAYS.toMillis(days);
                    toDrop.setExpiration(expiration);
                    toDrop.setOwner("");

                    map.spawnItemDrop((MapObject) mob, owner, toDrop, seedPos, dropType, playerDrop);

                }
                else {
                    map.spawnItemDrop((MapObject) mob, owner, item, seedPos, dropType, playerDrop);
                }
            }
        }
    }

    /** Overload with sensible defaults. */
    public static void dropForMonster(MapleMap map, Monster mob, Character owner) {
        byte dropType = (byte) (owner.getParty() != null ? 1 : 0);
        dropForMonster(map, mob, owner, mob.getPosition(), dropType, false);
    }
}
