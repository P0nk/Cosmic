package database.account;

import client.LoginState;
import lombok.Builder;
import net.server.coordinator.session.Hwid;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Ponk
 */
@Builder
public record Account(int id, String name, String password, boolean acceptedTos, Byte gender, LocalDate birthdate,
                      String pin, String pic, byte chrSlots, LoginState loginState, LocalDateTime lastLogin,
                      boolean banned, Instant bannedUntil, Byte banReason, String banDescription, String ip,
                      String macs, Hwid hwid) {
    public Account {
        Objects.requireNonNull(name);
        Objects.requireNonNull(password);
        Objects.requireNonNull(birthdate);
        Objects.requireNonNull(loginState);
    }
}
