package database.ban;

import database.PgDatabaseConnection;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;

import java.util.List;

/**
 * @author Ponk
 */
@Slf4j
public class IpBanRepository {
    private final PgDatabaseConnection connection;

    public IpBanRepository(PgDatabaseConnection connection) {
        this.connection = connection;
    }

    public List<IpBan> getAllIpBans() {
        String sql = """
                SELECT ip, account_id
                FROM ip_ban""";
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery(sql)
                    .mapTo(IpBan.class)
                    .list();
        }
    }

    public boolean saveIpBan(String ip, int accountId) {
        String sql = """
                INSERT INTO ip_ban (account_id, ip)
                VALUES (:accountId, :ip)""";
        try (Handle handle = connection.getHandle()) {
            return handle.createUpdate(sql)
                    .bind("accountId", accountId)
                    .bind("ip", ip)
                    .execute() > 0;
        } catch (Exception e) {
            log.error("Failed to save ip ban. The ip is already banned? accountId: {}, ip: {}", accountId, ip, e);
            return false;
        }
    }

    public void deleteIpBan(String ip) {
        String sql = """
                DELETE FROM ip_ban
                WHERE ip = :ip""";
        try (Handle handle = connection.getHandle()) {
            handle.createUpdate(sql)
                    .bind("ip", ip)
                    .execute();
        }
    }
}
