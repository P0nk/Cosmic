package database;

import client.CharacterStats;
import client.LoginState;
import config.ServerConfig;
import config.YamlConfig;
import database.account.Account;
import database.account.AccountRepository;
import database.character.CharacterRepository;
import database.migration.FlywayRunner;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import testutil.GeneratedIds;
import tools.DatabaseConnection;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class DatabaseTest {
    private static final String MYSQL_VERSION = "8.4";
    private static final String POSTGRES_VERSION = "16.4";
    private static final String SCHEMA_NAME = "cosmic";

    @Container
    static MySQLContainer<?> mySql = new MySQLContainer<>("mysql:%s".formatted(MYSQL_VERSION));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:%s".formatted(POSTGRES_VERSION));

    protected PgDatabaseConnection connection;
    protected GeneratedIds testIds;

    @BeforeAll
    void setUpDatabase() {
        prepareMysqlConnection();
        runDbMigrations();
        this.connection = createPgConnection();
    }

    // Not using this, but due to the nature of how the db connections are set up, the application requires
    // a real database to connect to.
    private void prepareMysqlConnection() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.DB_URL_FORMAT = "%s";
        serverConfig.DB_HOST = mySql.getJdbcUrl();
        serverConfig.DB_USER = mySql.getUsername();
        serverConfig.DB_PASS = mySql.getPassword();
        serverConfig.INIT_CONNECTION_POOL_TIMEOUT = 60;
        YamlConfig.config.server = serverConfig;
        DatabaseConnection.initializeConnectionPool();
    }

    private void runDbMigrations() {
        PgDatabaseConfig config = PgDatabaseConfig.builder()
                .url(postgres.getJdbcUrl())
                .schema(SCHEMA_NAME)
                .adminUsername(postgres.getUsername())
                .adminPassword(postgres.getPassword())
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .poolInitTimeout(Duration.ofSeconds(60))
                .clean(false)
                .build();
        new FlywayRunner(config).migrate();
    }

    private PgDatabaseConnection createPgConnection() {
        return new PgDatabaseConnection(createDataSource());
    }

    private PGSimpleDataSource createDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setCurrentSchema(SCHEMA_NAME);
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        return dataSource;
    }

    @BeforeEach
    void insertTestData() {
        int accountId = insertAccount(connection);
        try (Handle handle = connection.getHandle()) {
            int chrId = insertChr(handle, accountId);
            this.testIds = new GeneratedIds(accountId, chrId);
        }
    }

    private static int insertAccount(PgDatabaseConnection connection) {
        Account account = Account.builder()
                .name("accountname")
                .password("accountpassword")
                .birthdate(LocalDate.now())
                .loginState(LoginState.LOGGED_OUT)
                .build();
        return new AccountRepository(connection).insert(account);
    }

    private static int insertChr(Handle handle, int accountId) {
        CharacterRepository chrRepository = new CharacterRepository();
        CharacterStats stats = CharacterStats.builder()
                .account(accountId)
                .name("chrname")
                .build();
        return chrRepository.insert(handle, stats);
    }

    @AfterEach
    void deleteTestData() {
        List.of("chr", "account").forEach(this::clearTable);
    }

    protected void clearTable(String tableName) {
        String sql = "DELETE FROM %s".formatted(tableName);
        connection.getHandle().execute(sql);
    }
}
