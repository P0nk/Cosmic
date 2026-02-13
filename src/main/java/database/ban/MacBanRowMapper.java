package database.ban;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MacBanRowMapper implements RowMapper<MacBan> {

    @Override
    public MacBan map(ResultSet rs, StatementContext ctx) throws SQLException {
        return MacBan.builder()
                .mac(rs.getString("mac"))
                .accountId(rs.getObject("account_id", Integer.class))
                .build();
    }
}
