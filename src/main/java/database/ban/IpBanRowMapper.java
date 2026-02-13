package database.ban;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class IpBanRowMapper implements RowMapper<IpBan> {

    @Override
    public IpBan map(ResultSet rs, StatementContext ctx) throws SQLException {
        return IpBan.builder()
                .ip(rs.getString("ip"))
                .accountId(rs.getObject("account_id", Integer.class))
                .build();
    }
}
