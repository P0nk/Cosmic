package client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.quest.Quest;
import tools.StringUtil;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class QuestStatusTest {
    private QuestStatus questStatus;
    private Quest mockQuest;
    private static final int MOB_ID = 100;
    private static final int MAX_MOB_AMOUNT = 10;

    @BeforeEach
    void setUp() throws Exception {
        mockQuest = mock(Quest.class);
        when(mockQuest.getId()).thenReturn((short) 1);
        when(mockQuest.getMobAmountNeeded(MOB_ID)).thenReturn(MAX_MOB_AMOUNT);
        
        questStatus = new QuestStatus(mockQuest, QuestStatus.Status.STARTED);
        questStatus.setProgress(MOB_ID, "000"); // Set all tests to 0 to start

        setQuestMobCountModifier(1); // Default multiplier
    }

    private void setQuestMobCountModifier(int value) throws Exception {
        Field field = Class.forName("config.YamlConfig").getDeclaredField("config");
        field.setAccessible(true);
        Object config = field.get(null);
        Field serverField = config.getClass().getDeclaredField("server");
        serverField.setAccessible(true);
        Object server = serverField.get(config);
        Field modifierField = server.getClass().getDeclaredField("QUEST_MOB_COUNT_MODIFIER");
        modifierField.setAccessible(true);
        modifierField.set(server, value);
    }

    private void setQuestCount(int value) {
        questStatus.setProgress(MOB_ID, StringUtil.getLeftPaddedStr(Integer.toString(value), '0', 3));
    }

    @Test
    void testProgressDefaultIncrementSuccessfully() {
        setQuestCount(5);
        boolean result = questStatus.progress(MOB_ID);
        assertTrue(result);
        assertEquals("006", questStatus.getProgress(MOB_ID));
    }

    @Test
    void testProgressReturnsFalseForUnknownMobId() {
        boolean result = questStatus.progress(999);
        assertFalse(result);
        assertEquals("000", questStatus.getProgress(MOB_ID));
    }

    @Test
    void testProgressDoesNotExceedMax() throws Exception {
        questStatus.setProgress(MOB_ID, StringUtil.getLeftPaddedStr("8", '0', 3));
        setQuestMobCountModifier(9999);

        boolean result = questStatus.progress(MOB_ID);
        assertTrue(result);

        assertEquals("010", questStatus.getProgress(MOB_ID));
    }

    @Test
    void testProgressWithZeroMultiplier() throws Exception {
        setQuestCount(5);
        setQuestMobCountModifier(0);

        boolean result = questStatus.progress(MOB_ID);

        assertTrue(result);
        assertEquals("005", questStatus.getProgress(MOB_ID));
    }

    @Test
    void testProgressWithNegativeMultiplier() throws Exception {
        setQuestCount(0);
        setQuestMobCountModifier(-1);

        boolean result = questStatus.progress(MOB_ID);

        assertTrue(result);
        assertEquals("000", questStatus.getProgress(MOB_ID));
    }

    // Would be surprised if anyone ever does this but it's there
    @Test
    void testProgressedQuestWithNegativeMultiplier() throws Exception {
        setQuestCount(5);
        setQuestMobCountModifier(-1);

        boolean result = questStatus.progress(MOB_ID);

        assertTrue(result);
        assertEquals("004", questStatus.getProgress(MOB_ID));
    }
}