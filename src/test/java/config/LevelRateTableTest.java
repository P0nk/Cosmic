package config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelRateTableTest {
    private static final String SETTING = "EXP_RATE_BY_LEVEL";

    private static LevelRateTable parse(Map<?, ?> entries) {
        return LevelRateTable.parse(SETTING, entries);
    }

    @Test
    void listedLevelUsesItsOwnMultiplier() {
        LevelRateTable table = parse(Map.of("1", "1", "10", "2", "20", "3"));

        assertEquals(1, table.getRate(1));
        assertEquals(2, table.getRate(10));
        assertEquals(3, table.getRate(20));
    }

    @Test
    void unlistedLevelUsesClosestListedLevelBelowIt() {
        LevelRateTable table = parse(Map.of("1", "1", "10", "2", "20", "3"));

        assertEquals(1, table.getRate(9));
        assertEquals(2, table.getRate(15));
        assertEquals(2, table.getRate(19));
        assertEquals(3, table.getRate(200));
    }

    @Test
    void levelBelowFirstListedLevelUsesOne() {
        LevelRateTable table = parse(Map.of("10", "5"));

        assertEquals(1, table.getRate(1));
        assertEquals(1, table.getRate(9));
        assertEquals(5, table.getRate(10));
    }

    @Test
    void emptyOrMissingTableUsesOneForEveryLevel() {
        assertSame(LevelRateTable.EMPTY, parse(Map.of()));
        assertSame(LevelRateTable.EMPTY, parse(null));
        assertEquals(1, LevelRateTable.EMPTY.getRate(1));
        assertEquals(1, LevelRateTable.EMPTY.getRate(200));
    }

    @Test
    void entryOrderDoesNotMatter() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("20", "3");
        entries.put("1", "1");
        entries.put("10", "2");
        LevelRateTable table = parse(entries);

        assertEquals(1, table.getRate(5));
        assertEquals(2, table.getRate(10));
        assertEquals(3, table.getRate(25));
    }

    @Test
    void acceptsNumbersAndSurroundingSpaces() {
        Map<Object, Object> entries = new HashMap<>();
        entries.put(10, 2);
        entries.put(" 20 ", " 3 ");
        LevelRateTable table = parse(entries);

        assertEquals(2, table.getRate(10));
        assertEquals(3, table.getRate(20));
    }

    @ParameterizedTest
    @CsvSource({
            "abc, 2, the level must be a whole number",
            "1.5, 2, the level must be a whole number",
            "0, 2, the level must be 1 or higher",
            "-5, 2, the level must be 1 or higher",
            "10, two, the multiplier must be a whole number",
            "10, 1.5, the multiplier must be a whole number",
            "10, 0, the multiplier must be 1 or higher",
            "10, -1, the multiplier must be 1 or higher"
    })
    void rejectsInvalidEntryAndNamesIt(String level, String multiplier, String reason) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> parse(Map.of(level, multiplier)));

        assertTrue(e.getMessage().contains(SETTING), e.getMessage());
        assertTrue(e.getMessage().contains("\"" + level + ": " + multiplier + "\""), e.getMessage());
        assertTrue(e.getMessage().contains(reason), e.getMessage());
    }

    @Test
    void rejectsMissingMultiplier() {
        Map<String, String> entries = new HashMap<>();
        entries.put("10", null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parse(entries));
        assertTrue(e.getMessage().contains("the multiplier must be a whole number"), e.getMessage());
    }

    @Test
    void rejectsSameLevelListedTwice() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("10", "2");
        entries.put("010", "3");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parse(entries));
        assertTrue(e.getMessage().contains("level 10 is listed more than once"), e.getMessage());
    }
}
