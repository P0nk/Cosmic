package database.ban;

import lombok.Builder;

import java.util.Objects;

@Builder
public record MacBan(String mac, Integer accountId) {
    public MacBan {
        Objects.requireNonNull(mac);
    }
}
