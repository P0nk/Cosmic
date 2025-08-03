package database.ban;

import lombok.Builder;

import java.util.Objects;

@Builder
public record IpBan(String ip, Integer accountId) {
    public IpBan {
        Objects.requireNonNull(ip);
    }
}
