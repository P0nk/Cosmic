package client;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Ponk
 */
public enum LoginState {
    LOGGED_OUT(0),
    SERVER_TRANSITION(1),
    LOGGED_IN(2);

    private final byte value;

    LoginState(int value) {
        this.value = (byte) value;
    }

    public byte getValue() {
        return value;
    }

    public static Optional<LoginState> fromValue(int value) {
        return Arrays.stream(values())
                .filter(v -> v.getValue() == value)
                .findFirst();
    }
}
