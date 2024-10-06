package database.shop;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ShopRowMapper implements RowMapper<Shop> {

    @Override
    public Shop map(ResultSet rs, StatementContext ctx) throws SQLException {
        final int shopId = rs.getInt("id");
        final int npcId = rs.getInt("npc_id");
        return new Shop(shopId, npcId);
    }
}
