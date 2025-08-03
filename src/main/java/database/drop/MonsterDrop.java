package database.drop;

public record MonsterDrop(int monsterId, int itemId, int minQuantity, int maxQuantity, Integer questId, int chance) {
}
