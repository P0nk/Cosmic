package server.loot;

import java.awt.Point;
import java.util.*;
import server.maps.MapleMap;
import server.maps.MapObject;
import server.life.Monster;
import client.Character;
import client.inventory.Item;

/**
 * FoodDropper — tiered ingredient drop manager.
 * Auto-generated from Food_tier.csv.
 */
public final class FoodDropper {

    private static final Random RNG = new Random();

    public enum Tier { T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, LEGENDARY }

    /** Tier -> item ids (quantities default to 1). */
    private static final Map<Tier, int[]> TIER_ITEMS = new EnumMap<>(Tier.class);
    static {
        TIER_ITEMS.put(Tier.T1, new int[] { 4036173, 4036174 });

        TIER_ITEMS.put(Tier.T2, new int[] { 4036175, 4036176, 4036177 });

        TIER_ITEMS.put(Tier.T3, new int[] { 4036178, 4036179, 4036180, 4036181, 4036182, 4036183 });

        TIER_ITEMS.put(Tier.T4, new int[] { 4036184, 4036185, 4036186, 4036187, 4036188, 4036189 });

        TIER_ITEMS.put(Tier.T5, new int[] { 4036190, 4036191, 4036192, 4036193, 4036194, 4036195, 4036196, 4036197, 4036198, 4036199, 4036200 });

        TIER_ITEMS.put(Tier.T6, new int[] { 4036201, 4036202, 4036203, 4036204, 4036205, 4036206, 4036207, 4036208, 4036209, 4036210 });

        TIER_ITEMS.put(Tier.T7, new int[] { 4036211, 4036212, 4036213, 4036214, 4036215, 4036216, 4036217, 4036218, 4036219, 4036220, 4036221, 4036222,
                4036223, 4036224, 4036225 });

        TIER_ITEMS.put(Tier.T8, new int[] { 4036226, 4036227, 4036228, 4036229, 4036230, 4036231, 4036232, 4036233, 4036234, 4036235, 4036236, 4036237,
                4036238, 4036239, 4036240 });

        TIER_ITEMS.put(Tier.T9, new int[] { 4036241, 4036242, 4036243, 4036244, 4036245, 4036246, 4036247, 4036248, 4036249, 4036250, 4036251, 4036252,
                4036253, 4036254, 4036255, 4036256, 4036257, 4036258, 4036259, 4036260, 4036261, 4036262, 4036263, 4036264,
                4036265, 4036266 });

        TIER_ITEMS.put(Tier.T10, new int[] { 4036267, 4036268, 4036269, 4036270, 4036271, 4036272, 4036273, 4036274, 4036275, 4036276, 4036277, 4036278,
                4036279, 4036280, 4036281, 4036282, 4036283, 4036284, 4036285, 4036286, 4036287, 4036288 });

        TIER_ITEMS.put(Tier.LEGENDARY, new int[] { 4036289, 4036290, 4036291, 4036292, 4036293, 4036294, 4036295, 4036296, 4036297, 4036298, 4036299, 4036300,
                4036301, 4036302, 4036303, 4036304, 4036305, 4036306, 4036307 });
    }

    /** Level bands -> tier mapping; tweak as needed. */
    private static final NavigableMap<Integer, Tier> LEVEL_TO_TIER = new TreeMap<>();
    static {
        LEVEL_TO_TIER.put(30,  Tier.T1);
        LEVEL_TO_TIER.put(50,  Tier.T2);
        LEVEL_TO_TIER.put(70,  Tier.T3);
        LEVEL_TO_TIER.put(120,  Tier.T4);
        LEVEL_TO_TIER.put(150,  Tier.T5);
        LEVEL_TO_TIER.put(200,  Tier.T6);
        LEVEL_TO_TIER.put(215, Tier.T7);
        LEVEL_TO_TIER.put(230, Tier.T8);
        LEVEL_TO_TIER.put(240, Tier.T9);
        LEVEL_TO_TIER.put(255, Tier.T10);
//        LEVEL_TO_TIER.put(Integer.MAX_VALUE, Tier.LEGENDARY);
    }

    private static Tier tierForLevel(int level) {
        Map.Entry<Integer, Tier> e = LEVEL_TO_TIER.ceilingEntry(Math.max(1, level));
        return (e != null) ? e.getValue() : Tier.T1;
    }

    /** Drops N items for a mob by its level's tier. */
    public static void dropForMonster(
            MapleMap map,
            Monster mob,
            Character owner,
            Point seedPos,
            int rolls,
            byte dropType,
            boolean playerDrop
    ) {
        if (map == null || mob == null || owner == null) return;
        Tier tier = tierForLevel(mob.getLevel());
        int[] items = TIER_ITEMS.get(tier);
        if (items == null || items.length == 0) return;

        rolls = Math.max(1, rolls);
        for (int i = 0; i < rolls; i++) {
            int itemId = items[RNG.nextInt(items.length)];
            Item item = new Item(itemId, (short)0, (short)1);
            Random rng = new Random();
            int dropchance = rng.nextInt(1_000_000) + 1; // 1 to 1,000,000 inclusive
            if (dropchance <= 100) {
                map.spawnItemDrop((MapObject) mob, owner, item, seedPos, dropType, playerDrop);
            }
        }
    }

    /** Convenience overload with sensible defaults. */
    public static void dropForMonster(MapleMap map, Monster mob, Character owner) {
        byte dropType = (byte) (owner.getParty() != null ? 1 : 0);
        dropForMonster(map, mob, owner, mob.getPosition(), 1, dropType, false);
    }

    // ---- Runtime customization helpers ----
    public void setTierItems(Tier tier, int[] itemIds) { TIER_ITEMS.put(tier, itemIds); }

    public void setLevelBand(int maxLevelInclusive, Tier tier) {
        LEVEL_TO_TIER.put(maxLevelInclusive, tier);
    }
}
