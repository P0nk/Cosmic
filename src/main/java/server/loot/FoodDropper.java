package server.loot;

import java.awt.Point;
import java.util.*;

import config.YamlConfig;
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

    // -- CONFIGURATION --
    // Base chance out of 1,000,000 (1,000,000 = 100%)
    // 5000 = 0.5%
    private static final int CHANCE_PRIMARY = 2000;       // The main tier for the mob's level
    private static final int CHANCE_SECONDARY = 1000;     // +/- 1 Tier
    private static final int CHANCE_TERTIARY = 500;       // +/- 2 Tiers

    private static final int BOSS_MULTIPLIER = 20;        // 20x chance for bosses

    public enum Tier { T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, LEGENDARY }

    /** Tier -> item ids (quantities default to 1). */
    private static final Map<Tier, int[]> TIER_ITEMS = new EnumMap<>(Tier.class);
    static {
        TIER_ITEMS.put(Tier.T1, new int[] { 4036173 });
        TIER_ITEMS.put(Tier.T2, new int[] { 4036175 });
        TIER_ITEMS.put(Tier.T3, new int[] { 4036178 });
        TIER_ITEMS.put(Tier.T4, new int[] { 4036184 });
        TIER_ITEMS.put(Tier.T5, new int[] { 4036190 });
        TIER_ITEMS.put(Tier.T6, new int[] { 4036201 });
        TIER_ITEMS.put(Tier.T7, new int[] { 4036211 });
        TIER_ITEMS.put(Tier.T8, new int[] { 4036226 });
        TIER_ITEMS.put(Tier.T9, new int[] { 4036241 });
        TIER_ITEMS.put(Tier.T10, new int[] { 4036267 });
        TIER_ITEMS.put(Tier.LEGENDARY, new int[] { 4036289 });
    }

    /** Level bands -> tier mapping; tweak as needed. */
    private static final NavigableMap<Integer, Tier> LEVEL_TO_TIER = new TreeMap<>();
    static {
        LEVEL_TO_TIER.put(30,  Tier.T1);
        LEVEL_TO_TIER.put(50,  Tier.T2);
        LEVEL_TO_TIER.put(70,  Tier.T3);
        LEVEL_TO_TIER.put(120, Tier.T4);
        LEVEL_TO_TIER.put(150, Tier.T5);
        LEVEL_TO_TIER.put(200, Tier.T6);
        LEVEL_TO_TIER.put(215, Tier.T7);
        LEVEL_TO_TIER.put(230, Tier.T8);
        LEVEL_TO_TIER.put(240, Tier.T9);
        LEVEL_TO_TIER.put(Integer.MAX_VALUE, Tier.T10);
    }

    private static Tier tierForLevel(int level) {
        Map.Entry<Integer, Tier> e = LEVEL_TO_TIER.ceilingEntry(Math.max(1, level));
        return (e != null) ? e.getValue() : Tier.T1;
    }

    /** * Rolls for drops independently across multiple tiers centered on the mob's level.
     * EXCLUDES Legendary tier from automatic dropping.
     */
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

        // 1. Identify the "Center" Tier based on level
        Tier primaryTier = tierForLevel(mob.getLevel());
        int primaryOrdinal = primaryTier.ordinal();
        Tier[] allTiers = Tier.values();

        // 2. Determine Multipliers
        boolean isBoss = mob.getStats().isBoss();
        long bossMulti = isBoss ? BOSS_MULTIPLIER : 1;
        int serverRate = YamlConfig.config.worlds.get(0).drop_rate;

        // 3. Iterate through offsets: -2 to +2
        for (int offset = -2; offset <= 2; offset++) {

            int targetOrdinal = primaryOrdinal + offset;

            // Bounds check
            if (targetOrdinal < 0 || targetOrdinal >= allTiers.length) {
                continue;
            }

            Tier currentTier = allTiers[targetOrdinal];

            // --- GUARD: Never auto-drop Legendary ---
            if (currentTier == Tier.LEGENDARY) {
                continue;
            }
            // ----------------------------------------

            int[] items = TIER_ITEMS.get(currentTier);
            if (items == null || items.length == 0) continue;

            // Determine Base Chance
            int baseChance;
            int absOffset = Math.abs(offset);
            if (absOffset == 0) {
                baseChance = CHANCE_PRIMARY;
            } else if (absOffset == 1) {
                baseChance = CHANCE_SECONDARY;
            } else {
                baseChance = CHANCE_TERTIARY;
            }

            // Calculate final probability threshold
            long finalThreshold = baseChance * bossMulti * serverRate;

            // Perform rolls
            int attempts = Math.max(1, rolls);
            for (int i = 0; i < attempts; i++) {
                if (RNG.nextInt(1_000_000) < finalThreshold) {
                    int itemId = items[RNG.nextInt(items.length)];
                    Item item = new Item(itemId, (short)0, (short)1);
                    map.spawnItemDrop((MapObject) mob, owner, item, seedPos, dropType, playerDrop);
                }
            }
        }
    }

    /** Convenience overload with sensible defaults. */
    public static void dropForMonster(MapleMap map, Monster mob, Character owner) {
        byte dropType = (byte) (owner.getParty() != null ? 1 : 0);
        dropForMonster(map, mob, owner, mob.getPosition(), 1, dropType, false);
    }

    public void setTierItems(Tier tier, int[] itemIds) { TIER_ITEMS.put(tier, itemIds); }
    public void setLevelBand(int maxLevelInclusive, Tier tier) { LEVEL_TO_TIER.put(maxLevelInclusive, tier); }
}