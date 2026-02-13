package server.ban;

import database.ban.IpBan;
import database.ban.IpBanRepository;
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
public class IpBanManager {
    private final IpBanRepository ipBanRepository;
    private final Map<String, Integer> bannedIps = new HashMap<>();

    public IpBanManager(IpBanRepository ipBanRepository) {
        this.ipBanRepository = ipBanRepository;
    }

    public synchronized void loadIpBans() {
        List<IpBan> ipBans = ipBanRepository.getAllIpBans();
        log.debug("Loaded {} ip bans", ipBans.size());
        ipBans.forEach(ipBan -> bannedIps.put(ipBan.ip(), ipBan.accountId()));
    }

    public synchronized boolean isBanned(String ip) {
        return bannedIps.containsKey(ip);
    }

    public synchronized void banIp(String ip, int accountId) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null");
        }
        // TODO: validate ip format. Or create "Ip" model class.

        bannedIps.put(ip, accountId);
        ipBanRepository.saveIpBan(ip, accountId);
    }

    public synchronized void unbanAccountIps(int accountId) {
        Set<String> ipsToUnban = new HashSet<>();
        for (Map.Entry<String, Integer> bannedIp : bannedIps.entrySet()) {
            if (bannedIp.getValue() == accountId) {
                ipsToUnban.add(bannedIp.getKey());
            }
        }

        ipsToUnban.forEach(this::unbanIp);
    }

    private void unbanIp(String ip) {
        bannedIps.remove(ip);
        ipBanRepository.deleteIpBan(ip);
    }

}
