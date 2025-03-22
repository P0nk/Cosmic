package database.shop;

import database.DatabaseException;
import database.PgDatabaseConnection;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;

import java.util.List;
import java.util.Optional;

public class ShopDao {
    private final PgDatabaseConnection connection;

    public ShopDao(PgDatabaseConnection connection) {
        this.connection = connection;
    }

    public Optional<Shop> getShop(int shopId) {
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery("""
                            SELECT *
                            FROM shop
                            WHERE id = ?;""")
                    .bind(0, shopId)
                    .mapTo(Shop.class)
                    .findOne();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to get shop with id %d".formatted(shopId), e);
        }
    }

    public List<ShopItem> getShopItems(int shopId) {
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery("""
                            SELECT *
                            FROM shop_item
                            WHERE shop = ?
                            ORDER BY position DESC;""")
                    .bind(0, shopId)
                    .mapTo(ShopItem.class)
                    .list();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to get shop items for shop %d".formatted(shopId), e);
        }
    }
}
