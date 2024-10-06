package database.monsterbook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MonsterCardTest {

    @Test
    void invalidCardId() {
        assertThrows(IllegalArgumentException.class, () -> new MonsterCard(123456, validLevel()));
    }

    @ParameterizedTest
    @ValueSource(bytes = {-1, 6})
    void invalidLevel(byte invalidLevel) {
        assertThrows(IllegalArgumentException.class, () -> new MonsterCard(validCardId(), invalidLevel));
    }

    @Test
    public void createValidCard() {
        assertDoesNotThrow(() -> new MonsterCard(validCardId(), validLevel()));
    }

    @Test
    void specialCardIsSpecial() {
        var specialCard = new MonsterCard(2388000, validLevel());

        assertTrue(specialCard.isSpecial());
    }

    @Test
    void normalCardIsNotSpecial() {
        var normalCard = new MonsterCard(2381234, validLevel());

        assertFalse(normalCard.isSpecial());
    }

    @Test
    void notMaxLevel() {
        var nonMaxedCard = new MonsterCard(validCardId(), (byte) 4);

        assertFalse(nonMaxedCard.isMaxLevel());
    }

    @Test
    void isMaxLevel() {
        var maxedCard = new MonsterCard(validCardId(), (byte) 5);

        assertTrue(maxedCard.isMaxLevel());
    }

    private int validCardId() {
        return 2380000;
    }

    private byte validLevel() {
        return 1;
    }
}
