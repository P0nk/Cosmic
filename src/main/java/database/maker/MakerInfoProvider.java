package database.maker;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import constants.id.ItemId;
import net.jcip.annotations.ThreadSafe;
import server.MakerItemFactory.MakerItemCreateEntry;

import java.util.List;
import java.util.Optional;

@ThreadSafe
public class MakerInfoProvider {
    private final MakerRepository makerRepository;
    private final Cache<Integer, Optional<MakerReagent>> reagentCache = Caffeine.newBuilder().build();
    private final Cache<Integer, Optional<MakerRecipe>> recipeCache = Caffeine.newBuilder().build();
    private final Cache<Integer, List<MakerIngredient>> ingredientsCache = Caffeine.newBuilder().build();

    public MakerInfoProvider(MakerRepository makerRepository) {
        if (makerRepository == null) {
            throw new IllegalArgumentException("MakerDao must not be null");
        }
        this.makerRepository = makerRepository;
    }

    public Optional<MakerReagent> getMakerReagent(int itemId) {
        return reagentCache.get(itemId, makerRepository::getReagent);
    }

    public Optional<MakerRecipe> getMakerRecipe(int itemId) {
        return recipeCache.get(itemId, makerRepository::getRecipe);
    }

    public Optional<Integer> getStimulant(int itemId) {
        return getMakerRecipe(itemId).map(MakerRecipe::catalyst);
    }

    public List<MakerIngredient> getIngredients(int recipeItemId) {
        return ingredientsCache.get(recipeItemId, makerRepository::getIngredients);
    }

    public Optional<MakerDisassemblyInfo> getDisassemblyInfo(int itemId) {
        Optional<MakerRecipe> recipe = getMakerRecipe(itemId);
        if (recipe.isEmpty()) {
            return Optional.empty();
        }
        int fee = calculateDisassemblyFee(recipe.get().mesoCost());

        List<MakerIngredient> gainedItems = getIngredients(itemId).stream()
                .filter(i -> ItemId.isMonsterCrystal(i.itemId()))
                .map(i -> new MakerIngredient(i.itemId(), (short) (i.count() / 2)))
                .toList();

        return Optional.of(new MakerDisassemblyInfo(fee, gainedItems));
    }

    private int calculateDisassemblyFee(int creationCost) {
        // cost is 13.6363~ % of the original value, trim by 1000.
        float val = (float) (creationCost * 0.13636363636364);
        int fee = (int) (val / 1000);
        fee *= 1000;
        return fee;
    }

    public Optional<MakerItemCreateEntry> getMakerItemEntry(int itemId) {
        Optional<MakerRecipe> optionalRecipe = getMakerRecipe(itemId);
        if (optionalRecipe.isEmpty()) {
            return Optional.empty();
        }

        final MakerRecipe recipe = optionalRecipe.get();
        final MakerItemCreateEntry makerEntry = new MakerItemCreateEntry(recipe.mesoCost(), recipe.requiredLevel(),
                recipe.requiredMakerLevel());
        makerEntry.addGainItem(itemId, recipe.quantity());

        final List<MakerIngredient> ingredients = getIngredients(itemId);
        ingredients.forEach(i -> makerEntry.addReqItem(i.itemId(), i.count()));

        return Optional.of(makerEntry);
    }
}
