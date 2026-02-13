package database.maker;

public record MakerRecipe(int itemId, short jobCategory, short requiredLevel, short requiredMakerLevel, int mesoCost,
                          Integer requiredItem, Integer requiredEquip, Integer catalyst, short quantity,
                          short reagentSlots) {}
