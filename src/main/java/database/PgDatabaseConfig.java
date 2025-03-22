package database;

import lombok.Builder;

import java.time.Duration;

@Builder
public record PgDatabaseConfig(
        String url,
        String schema,
        String adminUsername, String adminPassword,
        String username, String password,
        Duration poolInitTimeout,
        boolean clean
) {
    public PgDatabaseConfig {
        verifyNotBlank(url);
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
}
