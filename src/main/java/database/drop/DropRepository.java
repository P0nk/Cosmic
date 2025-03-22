package database.drop;

import database.DatabaseException;
import database.PgDatabaseConnection;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;

import java.util.List;

public class DropRepository {
    private final PgDatabaseConnection connection;

    public DropRepository(PgDatabaseConnection connection) {
        this.connection = connection;
    }

    public List<MonsterDrop> getMonsterDrops(int monsterId) {
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery("""
                            SELECT *
                            FROM monster_drop
                            WHERE monster_id = ?;""")
                    .bind(0, monsterId)
                    .mapTo(MonsterDrop.class)
                    .list();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to get monster drops for id %d".formatted(monsterId), e);
        }
    }

    public List<GlobalMonsterDrop> getGlobalMonsterDrops() {
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery("""
                            SELECT *
                            FROM global_monster_drop;""")
                    .mapTo(GlobalMonsterDrop.class)
                    .list();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to get global monster drops", e);
        }
    }
}
