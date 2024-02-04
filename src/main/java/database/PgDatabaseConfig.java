package database;

import java.time.Duration;

public record PgDatabaseConfig(
        String databaseName, String host, String schema,
        String adminUsername, String adminPassword,
        String username, String password,
        Duration poolInitTimeout
) {
    public PgDatabaseConfig {
        verifyNotBlank(databaseName);
        verifyNotBlank(host);
        verifyNotBlank(schema);
        verifyNotBlank(adminUsername);
        verifyNotBlank(adminPassword);
        verifyNotBlank(username);
        verifyNotBlank(password);
    }

    private void verifyNotBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing or blank value in PG database config");
        }
    }

    public String getJdbcUrl() {
        return "jdbc:postgresql://%s:5432/%s".formatted(host, databaseName);
    }
}
