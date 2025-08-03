package database.monsterbook;

import database.DatabaseException;
import database.PgDatabaseConnection;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.util.List;

public class MonsterCardRepository {
    private final PgDatabaseConnection connection;

    public MonsterCardRepository(PgDatabaseConnection connection) {
        this.connection = connection;
    }

    public List<MonsterCard> load(int chrId) {
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery("""
                            SELECT *
                            FROM monster_card
                            WHERE chr_id = ?;""")
                    .bind(0, chrId)
                    .mapTo(MonsterCard.class)
                    .list();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to find monster cards (chrId %d)".formatted(chrId), e);
        }
    }

    public void save(Handle handle, int chrId, List<MonsterCard> cards) {
        try {
            PreparedBatch batch = handle.prepareBatch("""
                    INSERT INTO monster_card (chr_id, card_id, level)
                    VALUES (?, ?, ?)
                    ON CONFLICT (chr_id, card_id)
                    DO UPDATE SET level = excluded.level;""");
            cards.forEach(card -> {
                batch.bind(0, chrId);
                batch.bind(1, card.cardId());
                batch.bind(2, card.level());
                batch.add();
            });
            batch.execute();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to save monster cards (chrId %d)".formatted(chrId), e);
        }
    }
}
