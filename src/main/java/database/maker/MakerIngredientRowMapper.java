package database.maker;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MakerIngredientRowMapper implements RowMapper<MakerIngredient> {

    @Override
    public MakerIngredient map(ResultSet rs, StatementContext ctx) throws SQLException {
        int itemId = rs.getInt("item_id");
        short count = rs.getShort("count");
        return new MakerIngredient(itemId, count);
    }
}
