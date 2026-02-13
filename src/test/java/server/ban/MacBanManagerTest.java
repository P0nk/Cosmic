package server.ban;

import database.DatabaseTest;
import database.ban.MacBan;
import database.ban.MacBanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.AnyValues;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacBanManagerTest extends DatabaseTest {
    private MacBanRepository macBanRepository;
    private MacBanManager macBanManager;

    @BeforeEach
    void setUp() {
        this.macBanRepository = new MacBanRepository(connection);
        this.macBanManager = new MacBanManager(macBanRepository);
    }

    @AfterEach
    void deleteMacBans() {
        clearTable("mac_ban");
    }

    @Test
    void loadMacBans_shouldLoadFromRepository() {
        String mac = "4A-16-A2-9C-B0-6D";
        assertFalse(macBanManager.isBanned(mac));

        macBanManager.loadMacBans();
        assertFalse(macBanManager.isBanned(mac));

        macBanRepository.saveMacBan(mac, AnyValues.integer());
        macBanManager.loadMacBans();

        assertTrue(macBanManager.isBanned(mac));
    }

    @Test
    void banIp_shouldSaveInRepository() {
        String mac = "1F-45-B0-FB-2E-DF";
        assertFalse(macBanManager.isBanned(mac));

        macBanManager.banMac(mac, 10733);

        assertTrue(macBanManager.isBanned(mac));
        List<MacBan> macBans = macBanRepository.getAllMacBans();
        assertEquals(1, macBans.size());
        assertEquals(new MacBan(mac, 10733), macBans.getFirst());
    }
}
