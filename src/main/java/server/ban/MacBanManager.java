package server.ban;

import database.ban.MacBan;
import database.ban.MacBanRepository;
import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.ThreadSafe;

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
public class MacBanManager {
    private final MacBanRepository macBanRepository;
    private final Map<String, Integer> bannedMacs = new HashMap<>();

    public MacBanManager(MacBanRepository macBanRepository) {
        this.macBanRepository = macBanRepository;
    }

    public synchronized void loadMacBans() {
        List<MacBan> macBans = macBanRepository.getAllMacBans();
        log.debug("Loaded {} mac bans", macBans.size());
        macBans.forEach(macBan -> bannedMacs.put(macBan.mac(), macBan.accountId()));
    }

    public synchronized boolean isBanned(String mac) {
        return bannedMacs.containsKey(mac);
    }

    public synchronized void banMac(String mac, int accountId) {
        if (mac == null) {
            throw new IllegalArgumentException("mac cannot be null");
        }
        // TODO: validate mac format. Or create "Mac" model class.

        bannedMacs.put(mac, accountId);
        macBanRepository.saveMacBan(mac, accountId);
    }

    public synchronized void unbanAccountMacs(int accountId) {
        Set<String> macsToUnban = new HashSet<>();
        for (Map.Entry<String, Integer> bannedMac : bannedMacs.entrySet()) {
            if (bannedMac.getValue() == accountId) {
                macsToUnban.add(bannedMac.getKey());
            }
        }

        macsToUnban.forEach(this::unbanMac);
    }

    private void unbanMac(String ip) {
        bannedMacs.remove(ip);
        macBanRepository.deleteMacBan(ip);
    }
}
