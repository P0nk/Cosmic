package server.ban;

import database.ban.HwidBan;
import database.ban.HwidBanRepository;
import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.ThreadSafe;
import net.server.coordinator.session.Hwid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ponk
 */
@ThreadSafe
@Slf4j
public class HwidBanManager {
    private final HwidBanRepository hwidBanRepository;
    private final Map<Hwid, Integer> bannedHwids = new HashMap<>();

    public HwidBanManager(HwidBanRepository hwidBanRepository) {
        this.hwidBanRepository = hwidBanRepository;
    }

    public synchronized void loadHwidBans() {
        List<HwidBan> hwidBans = hwidBanRepository.getAllHwidBans();
        log.debug("Loaded {} hwid bans", hwidBans.size());
        hwidBans.forEach(hwidBan -> bannedHwids.put(new Hwid(hwidBan.hwid()), hwidBan.accountId()));
    }

    public synchronized boolean isBanned(Hwid hwid) {
        return bannedHwids.containsKey(hwid);
    }

    public synchronized void banHwid(Hwid hwid, int accountId) {
        if (hwid == null) {
            throw new IllegalArgumentException("hwid cannot be null");
        }

        bannedHwids.put(hwid, accountId);
        hwidBanRepository.saveHwidBan(hwid.hwid(), accountId);
    }

    public synchronized void unbanAccountHwids(int accountId) {
        Set<Hwid> hwidsToUnban = new HashSet<>();
        for (Map.Entry<Hwid, Integer> bannedHwid : bannedHwids.entrySet()) {
            if (bannedHwid.getValue() == accountId) {
                hwidsToUnban.add(bannedHwid.getKey());
            }
        }

        hwidsToUnban.forEach(this::unbanHwid);
    }

    private void unbanHwid(Hwid hwid) {
        bannedHwids.remove(hwid);
        hwidBanRepository.deleteHwidBan(hwid.hwid());
    }
}
