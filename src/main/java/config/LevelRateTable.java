package config;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Whole-number rate multipliers by character level, read from a "level: multiplier" table in the config file.
 * A level uses the multiplier of the closest listed level at or below it. Levels below the first listed level use 1.
 */
public final class LevelRateTable {
    public static final LevelRateTable EMPTY = new LevelRateTable(new TreeMap<>());

    private final NavigableMap<Integer, Integer> ratesByLevel;

    private LevelRateTable(NavigableMap<Integer, Integer> ratesByLevel) {
        this.ratesByLevel = ratesByLevel;
    }

    /**
     * @param settingName name of the config setting the entries come from, used in error messages
     * @param entries     level to multiplier entries; keys and values may be strings or numbers
     * @throws IllegalArgumentException if a level or multiplier is not a whole number of 1 or more,
     *                                  or the same level is listed more than once
     */
    public static LevelRateTable parse(String settingName, Map<?, ?> entries) {
        if (entries == null || entries.isEmpty()) {
            return EMPTY;
        }

        NavigableMap<Integer, Integer> ratesByLevel = new TreeMap<>();
        for (Map.Entry<?, ?> entry : entries.entrySet()) {
            int level = parseWholeNumber(settingName, entry, entry.getKey(), "level");
            int rate = parseWholeNumber(settingName, entry, entry.getValue(), "multiplier");
            if (ratesByLevel.put(level, rate) != null) {
                throw invalidEntry(settingName, entry, "level " + level + " is listed more than once");
            }
        }

        return new LevelRateTable(ratesByLevel);
    }

    public int getRate(int level) {
        Map.Entry<Integer, Integer> entry = ratesByLevel.floorEntry(level);
        return entry != null ? entry.getValue() : 1;
    }

    private static int parseWholeNumber(String settingName, Map.Entry<?, ?> entry, Object rawValue, String fieldName) {
        int value;
        try {
            value = Integer.parseInt(String.valueOf(rawValue).trim());
        } catch (NumberFormatException e) {
            throw invalidEntry(settingName, entry, "the " + fieldName + " must be a whole number");
        }

        if (value < 1) {
            throw invalidEntry(settingName, entry, "the " + fieldName + " must be 1 or higher");
        }

        return value;
    }

    private static IllegalArgumentException invalidEntry(String settingName, Map.Entry<?, ?> entry, String reason) {
        return new IllegalArgumentException(String.format("Invalid %s entry \"%s: %s\" in %s: %s",
                settingName, entry.getKey(), entry.getValue(), YamlConfig.CONFIG_FILE_NAME, reason));
    }
}
