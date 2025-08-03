package model;

import java.util.Objects;

public record Note(int id, String message, String sender, String receiver, long timestamp, int fame) {
    private static final int PLACEHOLDER_ID = -1;

    public Note {
        Objects.requireNonNull(message);
        Objects.requireNonNull(sender);
        Objects.requireNonNull(receiver);
    }

    public static Note createNormal(String message, String sender, String receiver, long timestamp) {
        return new Note(PLACEHOLDER_ID, message, sender, receiver, timestamp, 0);
    }

    public static Note createGift(String message, String sender, String receiver, long timestamp) {
        return new Note(PLACEHOLDER_ID, message, sender, receiver, timestamp, 1);
    }
}
