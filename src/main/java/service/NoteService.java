package service;

import client.Character;
import database.DatabaseException;
import database.note.NoteDao;
import model.Note;
import net.packet.out.ShowNotesPacket;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NoteService {
    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private final NoteDao noteDao;

    public NoteService(NoteDao noteDao) {
        this.noteDao = noteDao;
    }

    /**
     * Send normal note from one character to another
     *
     * @return Send success
     */
    public boolean sendNormal(String message, String senderName, String receiverName) {
        Note normalNote = Note.createNormal(message, senderName, receiverName, Server.getInstance().getCurrentTime());
        return send(normalNote);
    }

    /**
     * Send note which will increase the receiver's fame by one.
     *
     * @return Send success
     */
    public boolean sendWithFame(String message, String senderName, String receiverName) {
        Note noteWithFame = Note.createGift(message, senderName, receiverName, Server.getInstance().getCurrentTime());
        return send(noteWithFame);
    }

    private boolean send(Note note) {
        // TODO: handle the following cases (originally listed at PacketCreator#noteError)
        /*
         *  0 = Player online, use whisper
         *  1 = Check player's name
         *  2 = Receiver inbox full
         */
        try {
            noteDao.save(note);
            return true;
        } catch (DatabaseException e) {
            log.error("Failed to send note {}", note, e);
            return false;
        }
    }

    /**
     * Show unread notes
     *
     * @param chr Note recipient
     */
    public void show(Character chr) {
        if (chr == null) {
            throw new IllegalArgumentException("Unable to show notes - chr is null");
        }

        List<Note> notes = getNotes(chr.getName());
        if (notes.isEmpty()) {
            return;
        }

        chr.sendPacket(new ShowNotesPacket(notes));
    }

    private List<Note> getNotes(String to) {
        final List<Note> notes;
        try {
            notes = noteDao.findAllByTo(to);
        } catch (DatabaseException e) {
            log.error("Failed to find notes sent to chr name {}", to, e);
            return Collections.emptyList();
        }

        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }

        return notes;
    }

    /**
     * Delete a read note
     *
     * @param noteId Id of note to discard
     * @return Discarded note. Empty optional if failed to discard.
     */
    public Optional<Note> delete(int noteId) {
        try {
            return noteDao.delete(noteId);
        } catch (DatabaseException e) {
            log.error("Failed to discard note with id {}", noteId, e);
            return Optional.empty();
        }
    }

}
