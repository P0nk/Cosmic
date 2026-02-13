package database.monsterbook;

import constants.id.ItemId;

public record MonsterCard(int cardId, byte level) {
    private static final int MAX_LEVEL = 5;

    public MonsterCard {
        if (!ItemId.isMonsterCard(cardId)) {
            throw new IllegalArgumentException("Invalid monster card id: %d".formatted(cardId));
        }
        if (level < 0 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Invalid monster card level: %d".formatted(level));
        }
    }

    public boolean isSpecial() {
        return cardId / 1000 == 2388;
    }

    public boolean isMaxLevel() {
        return level == MAX_LEVEL;
    }
}
