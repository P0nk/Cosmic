package database.maker;

import database.DatabaseException;
import database.PgDatabaseConnection;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;

import java.util.List;
import java.util.Optional;

public class MakerRepository {
    private final PgDatabaseConnection connection;

    public MakerRepository(PgDatabaseConnection connection) {
        this.connection = connection;
    }

    public Optional<MakerReagent> getReagent(int itemId) {
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery("""
                            SELECT *
                            FROM maker_reagent
                            WHERE item_id = ?;""")
                    .bind(0, itemId)
                    .mapTo(MakerReagent.class)
                    .findOne();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to get maker reagent with item id: %d".formatted(itemId), e);
        }
    }

    public Optional<MakerRecipe> getRecipe(int itemId) {
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery("""
                            SELECT *
                            FROM maker_recipe
                            WHERE item_id = ?;""")
                    .bind(0, itemId)
                    .mapTo(MakerRecipe.class)
                    .findOne();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to get maker recipe with item id: %d".formatted(itemId), e);
        }
    }

    public List<MakerIngredient> getIngredients(int recipeItemId) {
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery("""
                            SELECT *
                            FROM maker_ingredient
                            WHERE maker_recipe = ?;""")
                    .bind(0, recipeItemId)
                    .mapTo(MakerIngredient.class)
                    .list();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to get maker ingredients for recipe item id %d".formatted(recipeItemId), e);
        }
    }
}
