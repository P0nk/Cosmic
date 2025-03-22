package database.migration;

import database.PgDatabaseConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

import java.util.Map;

public class FlywayRunner {
    private final PgDatabaseConfig dbConfig;

    public FlywayRunner(PgDatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public void migrate() throws FlywayException {
        Flyway flyway = Flyway.configure()
                .dataSource(dbConfig.url(), dbConfig.adminUsername(), dbConfig.adminPassword())
                .schemas(dbConfig.schema())
                .createSchemas(true)
                .connectRetries(10)
                .connectRetriesInterval(5)
                .placeholders(Map.of(
                        "server-username", dbConfig.username(),
                        "server-password", dbConfig.password())
                )
                .cleanDisabled(!dbConfig.clean())
                .load();
        if (dbConfig.clean()) {
            flyway.clean();
        }
        flyway.migrate();
    }
}
