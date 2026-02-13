package database.drop;

public record GlobalMonsterDrop(int itemId, int continent, int minQuantity, int maxQuantity, Integer questId, int chance) {
}
