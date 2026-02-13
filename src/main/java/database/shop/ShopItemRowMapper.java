package database.shop;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ShopItemRowMapper implements RowMapper<ShopItem> {

    @Override
    public ShopItem map(ResultSet rs, StatementContext ctx) throws SQLException {
        final int itemId = rs.getInt("item_id");
        final int price = rs.getInt("price");
        final Integer pitch = rs.getObject("pitch", Integer.class);
        final int position = rs.getInt("position");
        return new ShopItem(itemId, price, pitch, position);
    }
}
