package server.ban;

import database.DatabaseTest;
import database.ban.IpBan;
import database.ban.IpBanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.AnyValues;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpBanManagerTest extends DatabaseTest {
    private IpBanRepository ipBanRepository;
    private IpBanManager ipBanManager;

    @BeforeEach
    void setUp() {
        this.ipBanRepository = new IpBanRepository(connection);
        this.ipBanManager = new IpBanManager(ipBanRepository);
    }

    @AfterEach
    void deleteIpBans() {
        clearTable("ip_ban");
    }

    @Test
    void loadIpBans_shouldLoadFromRepository() {
        String ip = "157.210.75.9";
        assertFalse(ipBanManager.isBanned(ip));

        ipBanManager.loadIpBans();
        assertFalse(ipBanManager.isBanned(ip));

        ipBanRepository.saveIpBan(ip, AnyValues.integer());
        ipBanManager.loadIpBans();

        assertTrue(ipBanManager.isBanned(ip));
    }

    @Test
    void banIp_shouldSaveInRepository() {
        String ip = "123.231.312.123";
        assertFalse(ipBanManager.isBanned(ip));

        ipBanManager.banIp(ip, 1001);

        assertTrue(ipBanManager.isBanned(ip));
        List<IpBan> ipBans = ipBanRepository.getAllIpBans();
        assertEquals(1, ipBans.size());
        assertEquals(new IpBan(ip, 1001), ipBans.getFirst());
    }
}
