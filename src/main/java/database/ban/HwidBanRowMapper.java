package database.ban;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class HwidBanRowMapper implements RowMapper<HwidBan> {

    @Override
    public HwidBan map(ResultSet rs, StatementContext ctx) throws SQLException {
        return HwidBan.builder()
                .hwid(rs.getString("hwid"))
                .accountId(rs.getObject("account_id", Integer.class))
                .build();
    }
}
