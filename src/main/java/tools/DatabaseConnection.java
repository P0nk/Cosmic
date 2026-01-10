package tools;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import config.YamlConfig;
import database.note.NoteRowMapper;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.TimerManager;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Frz (Big Daddy)
 * @author The Real Spookster
 * @author Ronan
 * @author MerogieMS - pool diagnostics & leak debugging
 */
public class DatabaseConnection {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    private static HikariDataSource dataSource;
    private static Jdbi jdbi;

    /* =========================
     *  CONNECTION ACCESS
     * ========================= */

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("Unable to get connection - connection pool is uninitialized");
        }

        long start = System.nanoTime();
        Connection con = dataSource.getConnection();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        // Warn only when pool pressure is real
        if (elapsedMs >= 200) {
            log.warn("[DB-BORROW-SLOW] {} ms to borrow connection", elapsedMs);
        }

        return con;
    }

    public static Handle getHandle() {
        if (jdbi == null) {
            throw new IllegalStateException("Unable to get handle - connection pool is uninitialized");
        }
        return jdbi.open();
    }

    /* =========================
     *  CONFIG
     * ========================= */

    private static String getDbUrl() {
        String hostOverride = System.getenv("DB_HOST");
        String host = hostOverride != null ? hostOverride : YamlConfig.config.server.DB_HOST;
        return String.format(YamlConfig.config.server.DB_URL_FORMAT, host);
    }

    private static HikariConfig getConfig() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(getDbUrl());
        config.setUsername(YamlConfig.config.server.DB_USER);
        config.setPassword(YamlConfig.config.server.DB_PASS);

        // === DEBUGGING / SAFETY ===
        config.setPoolName("MaplePool");
        config.setRegisterMbeans(true);
        config.setLeakDetectionThreshold(5_000); // logs stacktrace if not closed within 5s

        final int initFailTimeoutSeconds = YamlConfig.config.server.INIT_CONNECTION_POOL_TIMEOUT;
        config.setInitializationFailTimeout(SECONDS.toMillis(initFailTimeoutSeconds));
        config.setConnectionTimeout(SECONDS.toMillis(30));
        config.setMaximumPoolSize(30);

        // MySQL optimizations
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 25);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);

        return config;
    }

    /* =========================
     *  INITIALIZATION
     * ========================= */

    public static boolean initializeConnectionPool() {
        if (dataSource != null) {
            return true;
        }

        final HikariConfig config = getConfig();
        log.info("Initializing DB pool -> '{}' as '{}'", config.getJdbcUrl(), config.getUsername());

        Instant initStart = Instant.now();
        try {
            dataSource = new HikariDataSource(config);
            initializeJdbi(dataSource);
//            startPoolMonitor(); // uncomment for debugging DB Connection.

            long initDuration = Duration.between(initStart, Instant.now()).toMillis();
            log.info("DB pool initialized in {} ms", initDuration);
            return true;

        } catch (Exception e) {
            long timeout = Duration.between(initStart, Instant.now()).getSeconds();
            log.error("Failed to initialize DB pool after {} seconds", timeout, e);
            return false;
        }
    }

    private static void initializeJdbi(DataSource dataSource) {
        jdbi = Jdbi.create(dataSource)
                .registerRowMapper(new NoteRowMapper());
    }

    /* =========================
     *  POOL MONITOR (1 line)
     * ========================= */

    private static void startPoolMonitor() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName pool = new ObjectName(
                    "com.zaxxer.hikari:type=Pool (" + dataSource.getPoolName() + ")"
            );

            // Once per minute – increase to 10s while actively debugging if needed
            TimerManager.getInstance().register(() -> {
                try {
                    Integer active = (Integer) mbs.getAttribute(pool, "ActiveConnections");
                    Integer idle   = (Integer) mbs.getAttribute(pool, "IdleConnections");
                    Integer wait   = (Integer) mbs.getAttribute(pool, "ThreadsAwaitingConnection");
                    Integer total  = (Integer) mbs.getAttribute(pool, "TotalConnections");

                    log.info("[DB-POOL] act={} idle={} wait={} total={}",
                            active, idle, wait, total);

                } catch (Exception ignored) {
                    // silent by design
                }
            }, 60_000);

        } catch (Exception e) {
            log.warn("DB pool monitor unavailable", e);
        }
    }
}
