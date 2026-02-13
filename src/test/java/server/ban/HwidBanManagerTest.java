package server.ban;

import database.DatabaseTest;
import database.ban.HwidBanRepository;
import net.server.coordinator.session.Hwid;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HwidBanManagerTest extends DatabaseTest {
    private HwidBanRepository hwidBanRepository;
    private HwidBanManager hwidBanManager;

    @BeforeEach
    void setUp() {
        this.hwidBanRepository = new HwidBanRepository(connection);
        this.hwidBanManager = new HwidBanManager(hwidBanRepository);
    }

    @AfterEach
    void deleteHwidBans() {
        clearTable("hwid_ban");
    }

    @Test
    void loadHwidBans_shouldLoadFromRepository() {
        Hwid hwid = new Hwid("ABC1DEF2");
        assertFalse(hwidBanManager.isBanned(hwid));

        hwidBanManager.loadHwidBans();
        assertFalse(hwidBanManager.isBanned(hwid));

        saveHwidBan(hwid);
        hwidBanManager.loadHwidBans();

        assertTrue(hwidBanManager.isBanned(hwid));
    }

    private void saveHwidBan(Hwid hwid) {
        try (Handle handle = connection.getHandle()) {
            handle.execute("INSERT INTO hwid_ban (hwid) VALUES (?)", hwid.hwid());
        }
    }
}
