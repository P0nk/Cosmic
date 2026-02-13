package database.maker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.MakerItemFactory;
import testutil.AnyValues;
import tools.Pair;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MakerInfoProviderTest {

    @Mock
    private MakerRepository makerRepository;

    private MakerInfoProvider makerInfoProvider;

    @BeforeEach
    void reset() {
        MockitoAnnotations.openMocks(this);
        this.makerInfoProvider = new MakerInfoProvider(makerRepository);
    }

    @Test
    void requireNonNullDao() {
        assertThrows(IllegalArgumentException.class, () -> new MakerInfoProvider(null));
    }

    @Test
    void getReagentFromDb() {
        int itemId = 5783;
        MakerReagent reagent = createReagent(itemId);
        when(makerRepository.getReagent(anyInt())).thenReturn(Optional.of(reagent));

        Optional<MakerReagent> retrievedReagent = makerInfoProvider.getMakerReagent(itemId);

        assertTrue(retrievedReagent.isPresent());
        assertEquals(reagent, retrievedReagent.get());
    }

    @Test
    void getCachedReagent() {
        int itemId = 90123444;
        MakerReagent reagent = createReagent(itemId);
        when(makerRepository.getReagent(anyInt())).thenReturn(Optional.of(reagent));

        Optional<MakerReagent> firstReagent = makerInfoProvider.getMakerReagent(itemId);
        Optional<MakerReagent> secondReagent = makerInfoProvider.getMakerReagent(itemId);

        assertTrue(firstReagent.isPresent());
        assertEquals(reagent, firstReagent.get());
        assertTrue(secondReagent.isPresent());
        assertEquals(reagent, secondReagent.get());
        verify(makerRepository, times(1)).getReagent(itemId);
    }

    private MakerReagent createReagent(int itemId) {
        return new MakerReagent(itemId, "incPAD", 3);
    }

    @Test
    void getRecipeFromDb() {
        int itemId = 43893;
        MakerRecipe recipe = createRecipe(itemId);
        when(makerRepository.getRecipe(itemId)).thenReturn(Optional.of(recipe));

        Optional<MakerRecipe> retrievedRecipe = makerInfoProvider.getMakerRecipe(itemId);

        assertTrue(retrievedRecipe.isPresent());
        assertEquals(recipe, retrievedRecipe.get());
    }

    @Test
    void getRecipeFromDb_notFound() {
        givenNoRecipe();

        Optional<MakerRecipe> recipe = makerInfoProvider.getMakerRecipe(AnyValues.integer());

        assertTrue(recipe.isEmpty());
    }

    @Test
    void getCachedRecipe() {
        int itemId = 10848;
        MakerRecipe recipe = createRecipe(itemId);
        when(makerRepository.getRecipe(anyInt())).thenReturn(Optional.of(recipe));

        Optional<MakerRecipe> firstRecipe = makerInfoProvider.getMakerRecipe(itemId);
        Optional<MakerRecipe> secondRecipe = makerInfoProvider.getMakerRecipe(itemId);

        assertTrue(firstRecipe.isPresent());
        assertEquals(recipe, firstRecipe.get());
        assertTrue(secondRecipe.isPresent());
        assertEquals(recipe, secondRecipe.get());
        verify(makerRepository, times(1)).getRecipe(itemId);
    }

    private MakerRecipe createRecipe(int itemId) {
        return new MakerRecipe(itemId, (short) 0, (short) 45, (short) 1, 100_000, null, null, null, (short) 1, (short) 1);
    }

    @Test
    void getStimulant() {
        int catalyst = 4031200;
        MakerRecipe recipeWithCatalyst = new MakerRecipe(0, (short) 0, (short) 0, (short) 0, 0, null, null, catalyst, (short) 0, (short) 0);
        when(makerRepository.getRecipe(anyInt())).thenReturn(Optional.of(recipeWithCatalyst));

        Optional<Integer> stimulant = makerInfoProvider.getStimulant(AnyValues.integer());

        assertTrue(stimulant.isPresent());
        assertEquals(catalyst, stimulant.get());
    }

    @Test
    void getStimulant_noRecipe() {
        givenNoRecipe();

        Optional<Integer> stimulant = makerInfoProvider.getStimulant(AnyValues.integer());

        assertTrue(stimulant.isEmpty());
    }

    @Test
    void getMakerItemCreateEntry_noRecipe() {
        givenNoRecipe();

        Optional<MakerItemFactory.MakerItemCreateEntry> createEntry = makerInfoProvider.getMakerItemEntry(345093);

        assertTrue(createEntry.isEmpty());
    }

    @Test
    void getMakerItemCreateEntry() {
        final int itemId = 458945;
        MakerRecipe recipe = createRecipe(itemId);
        when(makerRepository.getRecipe(anyInt())).thenReturn(Optional.of(recipe));
        MakerIngredient ingredient = new MakerIngredient(1002003, (short) 5);
        when(makerRepository.getIngredients(anyInt())).thenReturn(List.of(ingredient));

        Optional<MakerItemFactory.MakerItemCreateEntry> optionalCreateEntry = makerInfoProvider.getMakerItemEntry(itemId);

        assertTrue(optionalCreateEntry.isPresent());
        MakerItemFactory.MakerItemCreateEntry createEntry = optionalCreateEntry.get();
        assertEquals(recipe.mesoCost(), createEntry.getCost());
        assertEquals(recipe.requiredLevel(), createEntry.getReqLevel());
        assertEquals(recipe.requiredMakerLevel(), createEntry.getReqSkillLevel());
        assertEquals(1, createEntry.getReqItems().size());
        Pair<Integer, Integer> ingredientPair = createEntry.getReqItems().get(0);
        assertEquals(ingredient.itemId(), ingredientPair.left);
        assertEquals(ingredient.count(), ingredientPair.right);
        assertEquals(1, createEntry.getGainItems().size());
        Pair<Integer, Integer> resultPair = createEntry.getGainItems().get(0);
        assertEquals(itemId, resultPair.left);
        assertEquals(recipe.quantity(), resultPair.right);
    }

    private void givenNoRecipe() {
        when(makerRepository.getRecipe(anyInt())).thenReturn(Optional.empty());
    }
}
