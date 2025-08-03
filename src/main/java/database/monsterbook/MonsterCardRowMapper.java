package database.monsterbook;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MonsterCardRowMapper implements RowMapper<MonsterCard> {

    @Override
    public MonsterCard map(ResultSet rs, StatementContext ctx) throws SQLException {
        int cardId = rs.getInt("card_id");
        byte level = rs.getByte("level");
        return new MonsterCard(cardId, level);
    }
}
