package database;

import org.jdbi.v3.core.JdbiException;

public class DatabaseException extends JdbiException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
