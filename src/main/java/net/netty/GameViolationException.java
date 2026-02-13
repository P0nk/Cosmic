package net.netty;

public class GameViolationException extends RuntimeException {

    public GameViolationException(String message) {
        super(message);
    }

    public static GameViolationException inventoryType(int type) {
        return new GameViolationException("Invalid inventory type: " + type);
    }

    public static GameViolationException textLength(String text) {
        int length = text != null ? text.length() : 0;
        return new GameViolationException("Text too long: " + length);
    }
}
