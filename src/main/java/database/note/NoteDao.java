package database.note;

import database.DaoException;
import model.Note;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;
import tools.DatabaseConnection;

import java.util.List;
import java.util.Optional;

public class NoteDao {

    public void save(Note note) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            handle.createUpdate("""
                            INSERT INTO notes (`message`, `from`, `to`, `timestamp`, `fame`, `deleted`)
                            VALUES (?, ?, ?, ?, ?, ?)""")
                    .bind(0, note.message())
                    .bind(1, note.from())
                    .bind(2, note.to())
                    .bind(3, note.timestamp())
                    .bind(4, note.fame())
                    .bind(5, 0)
                    .execute();
        } catch (JdbiException e) {
            throw new DaoException("Failed to save note: %s".formatted(note.toString()), e);
        }
    }
  
    public List<Note> findAllByTo(String to) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            return handle.createQuery("""
                            SELECT * 
                            FROM notes
                            WHERE `deleted` = 0
                            AND `to` = ?""")
                    .bind(0, to)
                    .mapTo(Note.class)
                    .list();
        } catch (JdbiException e) {
            throw new DaoException("Failed to find notes sent to: %s".formatted(to), e);
        } catch (Exception e) {
            System.out.println("I suspect here might be a problem causing DC when login but not sure."); // nasi
            return List.of();
        }
    }

    public Optional<Note> delete(int id) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            Optional<Note> note = findById(handle, id);
            if (note.isEmpty()) {
                return Optional.empty();
            }
            deleteById(handle, id);

            return note;
        } catch (JdbiException e) {
            throw new DaoException("Failed to delete note with id: %d".formatted(id), e);
        }
    }

    private Optional<Note> findById(Handle handle, int id) {
        final Optional<Note> note;
        try {
            note = handle.createQuery("""
                            SELECT *
                            FROM notes
                            WHERE `deleted` = 0
                            AND `id` = ?""")
                    .bind(0, id)
                    .mapTo(Note.class)
                    .findOne();
        } catch (JdbiException e) {
            throw new DaoException("Failed find note with id %s".formatted(id), e);
        }
        return note;
    }

    private void deleteById(Handle handle, int id) {
        try {
            handle.createUpdate("""
                        UPDATE notes
                        SET `deleted` = 1
                        WHERE `id` = ?""")
                    .bind(0, id)
                    .execute();
        } catch (JdbiException e) {
            throw new DaoException("Failed to delete note with id %d".formatted(id), e);
        }
    }
}
