package database.maker;

import java.util.Collection;

public record MakerDisassemblyInfo(int fee, Collection<MakerIngredient> gainedItems) {
}
