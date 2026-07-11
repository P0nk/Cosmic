package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoteTest {

    @Test
    void requireNonNullMessage() {
        assertThrows(NullPointerException.class, () -> new Note(1, null, "from", "to", System.currentTimeMillis(), 0));
    }

    @Test
    void requireNonNullFrom() {
        assertThrows(NullPointerException.class, () -> new Note(2, "message", null, "to", System.currentTimeMillis(), 0));
    }

    @Test
    void requireNonNullTo() {
        assertThrows(NullPointerException.class, () -> new Note(3, "message", "from", null, System.currentTimeMillis(), 0));
    }

    @Test
    void createNew() {
        assertDoesNotThrow(() -> new Note(4, "message", "from", "to", System.currentTimeMillis(), 5));
    }
}