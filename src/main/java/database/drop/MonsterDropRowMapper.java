package database.drop;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MonsterDropRowMapper implements RowMapper<MonsterDrop> {

    @Override
    public MonsterDrop map(ResultSet rs, StatementContext ctx) throws SQLException {
        final int monsterId = rs.getInt("monster_id");
        final int itemId = rs.getInt("item_id");
        final int minQuantity = rs.getInt("min_quantity");
        final int maxQuantity = rs.getInt("max_quantity");
        final Integer questId = rs.getObject("quest_id", Integer.class);
        final int chance = rs.getInt("chance");
        return new MonsterDrop(monsterId, itemId, minQuantity, maxQuantity, questId, chance);
    }
}
