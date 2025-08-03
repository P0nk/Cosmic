package database.note;

import database.DatabaseException;
import database.PgDatabaseConnection;
import model.Note;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;

import java.util.List;
import java.util.Optional;

public class NoteDao {
    private final PgDatabaseConnection connection;

    public NoteDao(PgDatabaseConnection connection) {
        this.connection = connection;
    }

    public void save(Note note) {
        try (Handle handle = connection.getHandle()) {
            handle.createUpdate("""
                            INSERT INTO note (message, sender, receiver, timestamp, fame, deleted)
                            VALUES (?, ?, ?, ?, ?, ?)""")
                    .bind(0, note.message())
                    .bind(1, note.sender())
                    .bind(2, note.receiver())
                    .bind(3, note.timestamp())
                    .bind(4, note.fame())
                    .bind(5, 0)
                    .execute();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to save note: %s".formatted(note.toString()), e);
        }
    }

    public List<Note> findAllByTo(String receiver) {
        try (Handle handle = connection.getHandle()) {
            // Using LOWER as a workaround for notes not appearing when being sent to name with wrongly typed casing.
            // Ideally, the character ids should be the identifier rather than name.
            return handle.createQuery("""
                            SELECT *
                            FROM note
                            WHERE deleted = 0
                            AND LOWER(receiver) = LOWER(?)""")
                    .bind(0, receiver)
                    .mapTo(Note.class)
                    .list();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to find notes sent to: %s".formatted(receiver), e);
        }
    }

    public Optional<Note> delete(int id) {
        try (Handle handle = connection.getHandle()) {
            Optional<Note> note = findById(handle, id);
            if (note.isEmpty()) {
                return Optional.empty();
            }
            deleteById(handle, id);

            return note;
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to delete note with id: %d".formatted(id), e);
        }
    }

    private Optional<Note> findById(Handle handle, int id) {
        final Optional<Note> note;
        try {
            note = handle.createQuery("""
                            SELECT *
                            FROM note
                            WHERE deleted = 0
                            AND id = ?""")
                    .bind(0, id)
                    .mapTo(Note.class)
                    .findOne();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed find note with id %s".formatted(id), e);
        }
        return note;
    }

    private void deleteById(Handle handle, int id) {
        try {
            handle.createUpdate("""
                        UPDATE note
                        SET deleted = 1
                        WHERE id = ?""")
                    .bind(0, id)
                    .execute();
        } catch (JdbiException e) {
            throw new DatabaseException("Failed to delete note with id %d".formatted(id), e);
        }
    }
}
