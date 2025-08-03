package database.ban;

import database.PgDatabaseConnection;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;

import java.util.List;

@Slf4j
public class HwidBanRepository {
    private final PgDatabaseConnection connection;

    public HwidBanRepository(PgDatabaseConnection connection) {
        this.connection = connection;
    }

    public List<HwidBan> getAllHwidBans() {
        String sql = """
                SELECT hwid, account_id
                FROM hwid_ban""";
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery(sql)
                    .mapTo(HwidBan.class)
                    .list();
        }
    }

    public boolean saveHwidBan(String hwid, int accountId) {
        String sql = """
                INSERT INTO hwid_ban (hwid, account_id)
                VALUES (:hwid, :accountId)""";
        try (Handle handle = connection.getHandle()) {
            return handle.createUpdate(sql)
                    .bind("hwid", hwid)
                    .bind("accountId", accountId)
                    .execute() > 0;
        } catch (Exception e) {
            log.error("Failed to save hwid ban. The hwid is already banned? accountId: {}, hwid: {}", accountId, hwid, e);
            return false;
        }
    }

    public void deleteHwidBan(String hwid) {
        String sql = """
                DELETE FROM hwid_ban
                WHERE hwid = :hwid""";
        try (Handle handle = connection.getHandle()) {
            handle.createUpdate(sql)
                    .bind("hwid", hwid)
                    .execute();
        }
    }
}
