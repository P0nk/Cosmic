/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.server.coordinator.session;

import client.Character;
import client.Client;
import config.YamlConfig;
import constants.id.NpcId;
import net.server.Server;
import net.server.coordinator.login.LoginStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Ronan
 */
public class SessionCoordinator {
    private static final Logger log = LoggerFactory.getLogger(SessionCoordinator.class);
    private static final SessionCoordinator instance = new SessionCoordinator();

    public static SessionCoordinator getInstance() {
        return instance;
    }

    public enum AntiMulticlientResult {
        SUCCESS,
        REMOTE_LOGGEDIN,
        REMOTE_REACHED_LIMIT,
        REMOTE_PROCESSING,
        REMOTE_NO_MATCH,
        MANY_ACCOUNT_ATTEMPTS,
        COORDINATOR_ERROR
    }

    private final SessionInitialization sessionInit = new SessionInitialization();
    private final LoginStorage loginStorage = new LoginStorage();
    private final Map<Integer, Client> onlineClients = new HashMap<>(); // Key: account id
    private final Set<Hwid> onlineRemoteHwids = new HashSet<>(); // Hwid/nibblehwid
    private final Map<String, Client> loginRemoteHosts = new ConcurrentHashMap<>(); // Key: Ip (+ nibblehwid)
    private final HostHwidCache hostHwidCache = new HostHwidCache();

    private SessionCoordinator() {
    }

    private static boolean attemptAccountAccess(int accountId, Hwid hwid, boolean routineCheck) {
        try (Connection con = DatabaseConnection.getConnection()) {
            List<HwidRelevance> hwidRelevances = SessionDAO.getHwidRelevance(con, accountId);
            for (HwidRelevance hwidRelevance : hwidRelevances) {
                if (hwidRelevance.hwid().endsWith(hwid.hwid())) {
                    if (!routineCheck) {
                        // better update HWID relevance as soon as the login is authenticated
                        Instant expiry = HwidAssociationExpiry.getHwidAccountExpiry(hwidRelevance.relevance());
                        SessionDAO.updateAccountAccess(con, hwid, accountId, expiry, hwidRelevance.getIncrementedRelevance());
                    }

                    return true;
                }
            }

            if (hwidRelevances.size() < YamlConfig.config.server.MAX_ALLOWED_ACCOUNT_HWID) {
                return true;
            }
        } catch (SQLException e) {
            log.warn("Failed to update account access. Account id: {}, nibbleHwid: {}", accountId, hwid, e);
        }

        return false;
    }

    public static String getSessionRemoteHost(Client client) {
        Hwid hwid = client.getHwid();

        if (hwid != null) {
            return client.getRemoteAddress() + "-" + hwid.hwid();
        } else {
            return client.getRemoteAddress();
        }
    }

    public boolean canStartLoginSession(Client client) {
        if (!YamlConfig.config.server.DETERRED_MULTICLIENT) {
            return true;
        }

        String remoteHost = getSessionRemoteHost(client);
        final InitializationResult initResult = sessionInit.initialize(remoteHost);
        switch (initResult.getAntiMulticlientResult()) {
            case REMOTE_PROCESSING -> {
                return false;
            }
            case COORDINATOR_ERROR -> {
                return true;
            }
        }

        try {
            final HostHwid knownHwid = hostHwidCache.getEntry(remoteHost);
            if (knownHwid != null && onlineRemoteHwids.contains(knownHwid.hwid())) {
                return false;
            } else if (loginRemoteHosts.containsKey(remoteHost)) {
                return false;
            }

            loginRemoteHosts.put(remoteHost, client);
            return true;
        } finally {
            sessionInit.finalize(remoteHost);
        }
    }

    public void closeLoginSession(Client client) {
        clearLoginRemoteHost(client);

        Hwid nibbleHwid = client.getHwid();
        client.setHwid(null);
        if (nibbleHwid != null) {
            onlineRemoteHwids.remove(nibbleHwid);

            if (client != null) {
                Client loggedClient = onlineClients.get(client.getAccID());

                // do not remove an online game session here, only login session
                if (loggedClient != null && loggedClient.getSessionId() == client.getSessionId()) {
                    onlineClients.remove(client.getAccID());
                }
            }
        }
    }

    private void clearLoginRemoteHost(Client client) {
        String remoteHost = getSessionRemoteHost(client);
        loginRemoteHosts.remove(client.getRemoteAddress());
        loginRemoteHosts.remove(remoteHost);
    }

    public AntiMulticlientResult attemptLoginSession(Client client, Hwid hwid, int accountId, boolean routineCheck) {
        if (!YamlConfig.config.server.DETERRED_MULTICLIENT) {
            client.setHwid(hwid);
            return AntiMulticlientResult.SUCCESS;
        }

        String remoteHost = getSessionRemoteHost(client);
        InitializationResult initResult = sessionInit.initialize(remoteHost);
        if (initResult != InitializationResult.SUCCESS) {
            return initResult.getAntiMulticlientResult();
        }

        try {
            if (!loginStorage.registerLogin(accountId)) {
                return AntiMulticlientResult.MANY_ACCOUNT_ATTEMPTS;
            } else if (routineCheck && !attemptAccountAccess(accountId, hwid, routineCheck)) {
                return AntiMulticlientResult.REMOTE_REACHED_LIMIT;
            } else if (onlineRemoteHwids.contains(hwid)) {
                return AntiMulticlientResult.REMOTE_LOGGEDIN;
            } else if (!attemptAccountAccess(accountId, hwid, routineCheck)) {
                return AntiMulticlientResult.REMOTE_REACHED_LIMIT;
            }

            client.setHwid(hwid);
            onlineRemoteHwids.add(hwid);

            return AntiMulticlientResult.SUCCESS;
        } finally {
            sessionInit.finalize(remoteHost);
        }
    }

    public AntiMulticlientResult attemptGameSession(Client client, int accountId, Hwid hwid) {
        final String remoteHost = getSessionRemoteHost(client);
        if (!YamlConfig.config.server.DETERRED_MULTICLIENT) {
            hostHwidCache.addEntry(remoteHost, hwid);
            hostHwidCache.addEntry(client.getRemoteAddress(), hwid); // no HWID information on the loggedin newcomer session...
            return AntiMulticlientResult.SUCCESS;
        }

        final InitializationResult initResult = sessionInit.initialize(remoteHost);
        if (initResult != InitializationResult.SUCCESS) {
            return initResult.getAntiMulticlientResult();
        }

        try {
            Hwid clientHwid = client.getHwid(); // thanks Paxum for noticing account stuck after PIC failure
            if (clientHwid == null) {
                return AntiMulticlientResult.REMOTE_NO_MATCH;
            }

            onlineRemoteHwids.remove(clientHwid);

            if (!hwid.equals(clientHwid)) {
                return AntiMulticlientResult.REMOTE_NO_MATCH;
            } else if (onlineRemoteHwids.contains(hwid)) {
                return AntiMulticlientResult.REMOTE_LOGGEDIN;
            }

            // assumption: after a SUCCESSFUL login attempt, the incoming client WILL receive a new IoSession from the game server

            // updated session CLIENT_HWID attribute will be set when the player log in the game
            onlineRemoteHwids.add(hwid);
            hostHwidCache.addEntry(remoteHost, hwid);
            hostHwidCache.addEntry(client.getRemoteAddress(), hwid);
            associateHwidAccountIfAbsent(hwid, accountId);

            return AntiMulticlientResult.SUCCESS;
        } finally {
            sessionInit.finalize(remoteHost);
        }
    }

    private static void associateHwidAccountIfAbsent(Hwid hwid, int accountId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            List<Hwid> hwids = SessionDAO.getHwidsForAccount(con, accountId);

            boolean containsRemoteHwid = hwids.stream().anyMatch(accountHwid -> accountHwid.equals(hwid));
            if (containsRemoteHwid) {
                return;
            }

            if (hwids.size() < YamlConfig.config.server.MAX_ALLOWED_ACCOUNT_HWID) {
                Instant expiry = HwidAssociationExpiry.getHwidAccountExpiry(0);
                SessionDAO.registerAccountAccess(con, accountId, hwid, expiry);
            }
        } catch (SQLException ex) {
            log.warn("Failed to associate hwid {} with account id {}", hwid, accountId, ex);
        }
    }

    private static Client fetchInTransitionSessionClient(Client client) {
        Hwid hwid = SessionCoordinator.getInstance().getGameSessionHwid(client);
        if (hwid == null) {   // maybe this session was currently in-transition?
            return null;
        }

        Client fakeClient = Client.createMock();
        fakeClient.setHwid(hwid);
        Integer chrId = Server.getInstance().freeCharacteridInTransition(client);
        if (chrId != null) {
            try {
                fakeClient.setAccID(Character.loadCharFromDB(chrId, client, false).getAccountID());
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }
        }

        return fakeClient;
    }

    public void closeSession(Client client, boolean immediately) {
        if (client == null) {
            client = fetchInTransitionSessionClient(client);
        }

        final Hwid hwid = client.getHwid();
        client.setHwid(null); // making sure to clean up calls to this function on login phase
        if (hwid != null) {
            onlineRemoteHwids.remove(hwid);
        }

        final boolean isGameSession = hwid != null;
        if (isGameSession) {
            onlineClients.remove(client.getAccID());
        } else {
            Client loggedClient = onlineClients.get(client.getAccID());

            // do not remove an online game session here, only login session
            if (loggedClient != null && loggedClient.getSessionId() == client.getSessionId()) {
                onlineClients.remove(client.getAccID());
            }
        }

        if (immediately) {
            client.closeSession();
        }
    }

    public Hwid pickLoginSessionHwid(Client client) {
        String remoteHost = client.getRemoteAddress();
        // thanks BHB, resinate for noticing players from same network not being able to login
        return hostHwidCache.removeEntryAndGetItsHwid(remoteHost);
    }

    public Hwid getGameSessionHwid(Client client) {
        String remoteHost = getSessionRemoteHost(client);
        return hostHwidCache.getEntryHwid(remoteHost);
    }

    public void clearExpiredHwidHistory() {
        hostHwidCache.clearExpired();
    }

    public void runUpdateLoginHistory() {
        loginStorage.clearExpiredAttempts();
    }

    public void printSessionTrace() {
        if (!onlineClients.isEmpty()) {
            List<Entry<Integer, Client>> elist = new ArrayList<>(onlineClients.entrySet());
            String commaSeparatedClients = elist.stream()
                    .map(Entry::getKey)
                    .sorted(Integer::compareTo)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            log.debug("Current online clients: {}", commaSeparatedClients);
        }

        if (!onlineRemoteHwids.isEmpty()) {
            List<Hwid> hwids = new ArrayList<>(onlineRemoteHwids);
            hwids.sort(Comparator.comparing(Hwid::hwid));

            log.debug("Current online HWIDs: {}", hwids.stream()
                    .map(Hwid::hwid)
                    .collect(Collectors.joining(" ")));
        }

        if (!loginRemoteHosts.isEmpty()) {
            List<Entry<String, Client>> elist = new ArrayList<>(loginRemoteHosts.entrySet());
            elist.sort(Entry.comparingByKey());

            log.debug("Current login sessions: {}", loginRemoteHosts.entrySet().stream()
                    .sorted(Entry.comparingByKey())
                    .map(entry -> "(" + entry.getKey() + ", client: " + entry.getValue())
                    .collect(Collectors.joining(", ")));
        }
    }

    public void printSessionTrace(Client c) {
        String str = "Opened server sessions:\r\n\r\n";

        if (!onlineClients.isEmpty()) {
            List<Entry<Integer, Client>> elist = new ArrayList<>(onlineClients.entrySet());
            elist.sort(Entry.comparingByKey());

            str += ("Current online clients:\r\n");
            for (Entry<Integer, Client> e : elist) {
                str += ("  " + e.getKey() + "\r\n");
            }
        }

        if (!onlineRemoteHwids.isEmpty()) {
            List<Hwid> hwids = new ArrayList<>(onlineRemoteHwids);
            hwids.sort(Comparator.comparing(Hwid::hwid));

            str += ("Current online HWIDs:\r\n");
            for (Hwid s : hwids) {
                str += ("  " + s + "\r\n");
            }
        }

        if (!loginRemoteHosts.isEmpty()) {
            List<Entry<String, Client>> elist = new ArrayList<>(loginRemoteHosts.entrySet());

            elist.sort((e1, e2) -> e1.getKey().compareTo(e2.getKey()));

            str += ("Current login sessions:\r\n");
            for (Entry<String, Client> e : elist) {
                str += ("  " + e.getKey() + ", IP: " + e.getValue().getRemoteAddress() + "\r\n");
            }
        }

        c.getAbstractPlayerInteraction().npcTalk(NpcId.TEMPLE_KEEPER, str);
    }
}
