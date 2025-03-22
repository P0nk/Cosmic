package service;

import database.note.NoteDao;
import model.Note;
import net.packet.out.ShowNotesPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import testutil.AnyValues;
import testutil.Mocks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testutil.AnyValues.dbException;

class NoteServiceTest {

    @Mock
    private NoteDao noteDao;

    private NoteService noteService;

    @BeforeEach
    void reset() {
        MockitoAnnotations.openMocks(this);
        this.noteService = new NoteService(noteDao);
    }

    @Test
    void sendNormalSuccess() {
        String message = "message";
        String from = "from";
        String to = "to";

        boolean success = noteService.sendNormal(message, from, to);

        assertTrue(success);
        var noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteDao).save(noteCaptor.capture());
        var note = noteCaptor.getValue();
        assertEquals(message, note.message());
        assertEquals(from, note.sender());
        assertEquals(to, note.receiver());
        assertEquals(0, note.fame());
    }

    @Test
    void sendWithFameSuccess() {
        String message = "fameMessage";
        String from = "fameFrom";
        String to = "fameTo";

        boolean success = noteService.sendWithFame(message, from, to);

        assertTrue(success);
        var noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteDao).save(noteCaptor.capture());
        var note = noteCaptor.getValue();
        assertEquals(message, note.message());
        assertEquals(from, note.sender());
        assertEquals(to, note.receiver());
        assertEquals(1, note.fame());
    }

    @Test
    void sendFailure() {
        doThrow(dbException()).when(noteDao).save(any());

        boolean success = noteService.sendNormal(AnyValues.string(), AnyValues.string(), AnyValues.string());

        assertFalse(success);
        verify(noteDao).save(any());
    }

    @Test
    void showRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> noteService.show(null));
    }

    @Test
    void showOneNote() {
        String chrName = "showMeNotes";
        var chr = Mocks.chr(chrName);
        when(noteDao.findAllByTo(chrName)).thenReturn(List.of(anyNote()));

        noteService.show(chr);

        verify(chr).sendPacket(any(ShowNotesPacket.class));
    }

    private Note anyNote() {
        return new Note(1, "message", "from", "to", 100200300400L, 0);
    }

    @Test
    void showZeroNotes_shouldNotSendPacket() {
        var chr = Mocks.chr("mockChr");
        when(noteDao.findAllByTo(any())).thenReturn(Collections.emptyList());

        noteService.show(chr);

        verify(chr, never()).sendPacket(any());
    }

    @Test
    void showNotesFailure_shouldNotSendPacket() {
        var chr = Mocks.chr("mockChr");
        when(noteDao.findAllByTo(any())).thenThrow(dbException());

        noteService.show(chr);

        verify(chr, never()).sendPacket(any());
    }

    @Test
    void deleteNoteSuccess() {
        int noteId = 1056;
        var note = anyNote();
        when(noteDao.delete(noteId)).thenReturn(Optional.of(note));

        Optional<Note> deletedNote = noteDao.delete(noteId);

        assertTrue(deletedNote.isPresent());
        assertEquals(note, deletedNote.get());
    }

    @Test
    void deleteNoteFailure() {
        when(noteDao.delete(anyInt())).thenThrow(dbException());

        Optional<Note> deletedNote = noteService.delete(4382);

        assertTrue(deletedNote.isEmpty());
    }
}
