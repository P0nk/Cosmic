package database.ban;

import database.PgDatabaseConnection;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;

import java.util.List;

/**
 * @author Ponk
 */
@Slf4j
public class MacBanRepository {
    private final PgDatabaseConnection connection;

    public MacBanRepository(PgDatabaseConnection connection) {
        this.connection = connection;
    }

    public List<MacBan> getAllMacBans() {
        String sql = """
                SELECT mac, account_id
                FROM mac_ban""";
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery(sql)
                    .mapTo(MacBan.class)
                    .list();
        }
    }

    public void saveMacBan(String mac, int accountId) {
        String sql = """
                INSERT INTO mac_ban (account_id, mac)
                VALUES (:accountId, :mac)""";
        try (Handle handle = connection.getHandle()) {
            handle.createUpdate(sql)
                    .bind("accountId", accountId)
                    .bind("mac", mac)
                    .execute();
        } catch (Exception e) {
            log.error("Failed to save mac ban. The mac is already banned? accountId: {}, mac: {}", accountId, mac, e);
        }
    }

    public void deleteMacBan(String mac) {
        String sql = """
                DELETE FROM mac_ban
                WHERE mac = :mac""";
        try (Handle handle = connection.getHandle()) {
            handle.createUpdate(sql)
                    .bind("mac", mac)
                    .execute();
        }
    }
}
