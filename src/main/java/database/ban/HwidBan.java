package database.ban;

import lombok.Builder;

import java.util.Objects;

@Builder
public record HwidBan(String hwid, Integer accountId) {
    public HwidBan {
        Objects.requireNonNull(hwid);
    }
}
