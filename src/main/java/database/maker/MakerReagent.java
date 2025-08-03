package database.maker;

import java.util.Objects;

public record MakerReagent(int itemId, String stat, int value) {
    public MakerReagent {
        Objects.requireNonNull(stat);
    }
}
