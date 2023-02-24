package database;

import database.note.NoteRowMapper;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class PgDatabaseConnection {
    private final DataSource dataSource;
    private final Jdbi jdbi;

    public PgDatabaseConnection(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbi = Jdbi.create(dataSource)
                .registerRowMapper(new NoteRowMapper()); // TODO: configure jdbi elsewhere
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public Handle getHandle() {
        return jdbi.open();
    }
}