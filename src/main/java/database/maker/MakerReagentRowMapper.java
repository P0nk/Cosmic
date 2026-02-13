package database.maker;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MakerReagentRowMapper implements RowMapper<MakerReagent> {

    @Override
    public MakerReagent map(ResultSet rs, StatementContext ctx) throws SQLException {
        int itemId = rs.getInt("item_id");
        String stat = rs.getString("stat");
        int value = rs.getInt("value");
        return new MakerReagent(itemId, stat, value);
    }
}
