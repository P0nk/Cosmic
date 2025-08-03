/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

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
package scripting.event;

import client.Character;
import config.YamlConfig;
import constants.game.GameConstants;
import database.drop.DropProvider;
import net.server.Server;
import net.server.channel.Channel;
import net.server.guild.Guild;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.event.scheduler.EventScriptScheduler;
import server.Marriage;
import server.ThreadManager;
import server.expeditions.Expedition;
import server.life.LifeFactory;
import server.life.Monster;
import server.maps.MapleMap;
import server.quest.Quest;
import tools.exceptions.EventInstanceInProgressException;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Matze
 * @author Ronan
 */
public class EventManager {
    private static final Logger log = LoggerFactory.getLogger(EventManager.class);
    private Invocable iv;
    private Channel cserv;
    private World wserv;
    private Server server;
    private final DropProvider dropProvider;
    private final EventScriptScheduler ess = new EventScriptScheduler();
    private final Map<String, EventInstanceManager> instances = new HashMap<>();
    private final Map<String, Integer> instanceLocks = new HashMap<>();
    private final Queue<Integer> queuedGuilds = new LinkedList<>();
    private final Map<Integer, Integer> queuedGuildLeaders = new HashMap<>();
    private final List<Boolean> openedLobbys;
    private final List<EventInstanceManager> readyInstances = new LinkedList<>();
    private Integer readyId = 0, onLoadInstances = 0;
    private final Properties props = new Properties();
    private final String name;
    private final Lock lobbyLock = new ReentrantLock();
    private final Lock queueLock = new ReentrantLock();
    private final Lock startLock = new ReentrantLock();

    private final Set<Integer> playerPermit = new HashSet<>();
    private final Semaphore startSemaphore = new Semaphore(7);

    private static final int maxLobbys = 8;     // an event manager holds up to this amount of concurrent lobbys

    public EventManager(Channel cserv, Invocable iv, String name, DropProvider dropProvider) {
        this.server = Server.getInstance();
        this.iv = iv;
        this.cserv = cserv;
        this.wserv = server.getWorld(cserv.getWorld());
        this.name = name;
        this.dropProvider = dropProvider;

        this.openedLobbys = new ArrayList<>();
        for (int i = 0; i < maxLobbys; i++) {
            this.openedLobbys.add(false);
        }
    }

    private boolean isDisposed() {
        return onLoadInstances <= -1000;
    }

    public void cancel() {  // make sure to only call this when there are NO PLAYERS ONLINE to mess around with the event manager!
        ess.dispose();

        try {
            iv.invokeFunction("cancelSchedule", (Object) null);
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }

        Collection<EventInstanceManager> eimList;
        synchronized (instances) {
            eimList = getInstances();
            instances.clear();
        }

        for (EventInstanceManager eim : eimList) {
            eim.dispose(true);
        }

        List<EventInstanceManager> readyEims;
        queueLock.lock();
        try {
            readyEims = new ArrayList<>(readyInstances);
            readyInstances.clear();
            onLoadInstances = Integer.MIN_VALUE / 2;
        } finally {
            queueLock.unlock();
        }

        for (EventInstanceManager eim : readyEims) {
            eim.dispose(true);
        }

        props.clear();
        cserv = null;
        wserv = null;
        server = null;
        iv = null;
    }

    private List<Integer> convertToIntegerList(List<Object> objects) {
        List<Integer> intList = new ArrayList<>();

        for (Object object : objects) {
            intList.add((Integer) object);
        }

        return intList;
    }

    public long getLobbyDelay() {
        return YamlConfig.config.server.EVENT_LOBBY_DELAY;
    }

    private int getMaxLobbies() {
        try {
            return (int) iv.invokeFunction("getMaxLobbies");
        } catch (ScriptException | NoSuchMethodException ex) { // they didn't define a lobby range
            return maxLobbys;
        }
    }

    public EventScheduledFuture schedule(String methodName, long delay) {
        return schedule(methodName, null, delay);
    }

    public EventScheduledFuture schedule(final String methodName, final EventInstanceManager eim, long delay) {
        Runnable r = () -> {
            try {
                iv.invokeFunction(methodName, eim);
            } catch (ScriptException | NoSuchMethodException ex) {
                log.error("Event script schedule", ex);
            }
        };

        ess.registerEntry(r, delay);

        // hate to do that, but those schedules can still be cancelled, so well... Let GC do it's job
        return new EventScheduledFuture(r, ess);
    }

    public EventScheduledFuture scheduleAtTimestamp(final String methodName, long timestamp) {
        Runnable r = () -> {
            try {
                iv.invokeFunction(methodName, (Object) null);
            } catch (ScriptException | NoSuchMethodException ex) {
                log.error("Event script scheduleAtTimestamp", ex);
            }
        };

        ess.registerEntry(r, timestamp - server.getCurrentTime());
        return new EventScheduledFuture(r, ess);
    }

    public World getWorldServer() {
        return wserv;
    }

    public Channel getChannelServer() {
        return cserv;
    }

    public Invocable getIv() {
        return iv;
    }

    public EventInstanceManager getInstance(String name) {
        return instances.get(name);
    }

    public Collection<EventInstanceManager> getInstances() {
        synchronized (instances) {
            return new LinkedList<>(instances.values());
        }
    }

    public EventInstanceManager newInstance(String name) throws EventInstanceInProgressException {
        EventInstanceManager ret = getReadyInstance();

        if (ret == null) {
            ret = new EventInstanceManager(this, name, dropProvider);
        } else {
            ret.setName(name);
        }

        synchronized (instances) {
            if (instances.containsKey(name)) {
                throw new EventInstanceInProgressException(name, this.getName());
            }

            instances.put(name, ret);
        }
        return ret;
    }

    public Marriage newMarriage(String name) throws EventInstanceInProgressException {
        Marriage ret = new Marriage(this, name, dropProvider);

        synchronized (instances) {
            if (instances.containsKey(name)) {
                throw new EventInstanceInProgressException(name, this.getName());
            }

            instances.put(name, ret);
        }
        return ret;
    }

    public void disposeInstance(final String name) {
        ess.registerEntry(() -> {
            freeLobbyInstance(name);

            synchronized (instances) {
                instances.remove(name);
            }
        }, SECONDS.toMillis(YamlConfig.config.server.EVENT_LOBBY_DELAY));
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public void setIntProperty(String key, int value) {
        setProperty(key, value);
    }

    public void setProperty(String key, int value) {
        props.setProperty(key, value + "");
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public int getIntProperty(String key) {
        return Integer.parseInt(props.getProperty(key));
    }

    private void setLockLobby(int lobbyId, boolean lock) {
        lobbyLock.lock();
        try {
            openedLobbys.set(lobbyId, lock);
        } finally {
            lobbyLock.unlock();
        }
    }

    private boolean startLobbyInstance(int lobbyId) {
        lobbyLock.lock();
        try {
            if (lobbyId < 0) {
                lobbyId = 0;
            } else if (lobbyId >= maxLobbys) {
                lobbyId = maxLobbys - 1;
            }

            if (!openedLobbys.get(lobbyId)) {
                openedLobbys.set(lobbyId, true);
                return true;
            }

            return false;
        } finally {
            lobbyLock.unlock();
        }
    }

    private void freeLobbyInstance(String lobbyName) {
        Integer i = instanceLocks.get(lobbyName);
        if (i == null) {
            return;
        }

        instanceLocks.remove(lobbyName);
        if (i > -1) {
            setLockLobby(i, false);
        }
    }

    public String getName() {
        return name;
    }

    private int availableLobbyInstance() {
        int maxLobbies = getMaxLobbies();

        if (maxLobbies > 0) {
            for (int i = 0; i < maxLobbies; i++) {
                if (startLobbyInstance(i)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private String getInternalScriptExceptionMessage(Throwable a) {
        if (!(a instanceof ScriptException)) {
            return null;
        }

        while (true) {
            Throwable t = a;
            a = a.getCause();

            if (a == null) {
                return t.getMessage();
            }
        }
    }

    private EventInstanceManager createInstance(String name, Object... args) throws ScriptException, NoSuchMethodException {
        return (EventInstanceManager) iv.invokeFunction(name, args);
    }

    private void registerEventInstance(String eventName, int lobbyId) {
        Integer oldLobby = instanceLocks.get(eventName);
        if (oldLobby != null) {
            setLockLobby(oldLobby, false);
        }

        instanceLocks.put(eventName, lobbyId);
    }

    public boolean startInstance(Expedition exped) {
        return startInstance(-1, exped);
    }

    public boolean startInstance(int lobbyId, Expedition exped) {
        return startInstance(lobbyId, exped, exped.getLeader());
    }

    //Expedition method: starts an expedition
    public boolean startInstance(int lobbyId, Expedition exped, Character leader) {
        if (this.isDisposed()) {
            return false;
        }

        try {
            if (!playerPermit.contains(leader.getId()) && startSemaphore.tryAcquire(7777, MILLISECONDS)) {
                playerPermit.add(leader.getId());

                startLock.lock();
                try {
                    try {
                        if (lobbyId == -1) {
                            lobbyId = availableLobbyInstance();
                            if (lobbyId == -1) {
                                return false;
                            }
                        } else {
                            if (!startLobbyInstance(lobbyId)) {
                                return false;
                            }
                        }

                        EventInstanceManager eim;
                        try {
                            eim = createInstance("setup", leader.getClient().getChannel());
                            registerEventInstance(eim.getName(), lobbyId);
                        } catch (ScriptException | NullPointerException e) {
                            String message = getInternalScriptExceptionMessage(e);
                            if (message != null && !message.startsWith(EventInstanceInProgressException.EIIP_KEY)) {
                                throw e;
                            }

                            if (lobbyId > -1) {
                                setLockLobby(lobbyId, false);
                            }
                            return false;
                        }

                        eim.setLeader(leader);

                        exped.start();
                        eim.registerExpedition(exped);

                        eim.startEvent();
                    } catch (ScriptException | NoSuchMethodException ex) {
                        log.error("Event script startInstance", ex);
                    }

                    return true;
                } finally {
                    startLock.unlock();
                    playerPermit.remove(leader.getId());
                    startSemaphore.release();
                }
            }
        } catch (InterruptedException ie) {
            playerPermit.remove(leader.getId());
        }

        return false;
    }

    //Regular method: player 
    public boolean startInstance(Character chr) {
        return startInstance(-1, chr);
    }

    public boolean startInstance(int lobbyId, Character leader) {
        return startInstance(lobbyId, leader, leader, 1);
    }

    public boolean startInstance(int lobbyId, Character chr, Character leader, int difficulty) {
        if (this.isDisposed()) {
            return false;
        }

        try {
            if (!playerPermit.contains(leader.getId()) && startSemaphore.tryAcquire(7777, MILLISECONDS)) {
                playerPermit.add(leader.getId());

                startLock.lock();
                try {
                    try {
                        if (lobbyId == -1) {
                            lobbyId = availableLobbyInstance();
                            if (lobbyId == -1) {
                                return false;
                            }
                        } else {
                            if (!startLobbyInstance(lobbyId)) {
                                return false;
                            }
                        }

                        EventInstanceManager eim;
                        try {
                            eim = createInstance("setup", difficulty, (lobbyId > -1) ? lobbyId : leader.getId());
                            registerEventInstance(eim.getName(), lobbyId);
                        } catch (ScriptException | NullPointerException e) {
                            String message = getInternalScriptExceptionMessage(e);
                            if (message != null && !message.startsWith(EventInstanceInProgressException.EIIP_KEY)) {
                                throw e;
                            }

                            if (lobbyId > -1) {
                                setLockLobby(lobbyId, false);
                            }
                            return false;
                        }
                        eim.setLeader(leader);

                        if (chr != null) {
                            eim.registerPlayer(chr);
                        }

                        eim.startEvent();
                    } catch (ScriptException | NoSuchMethodException ex) {
                        log.error("Event script startInstance", ex);
                    }

                    return true;
                } finally {
                    startLock.unlock();
                    playerPermit.remove(leader.getId());
                    startSemaphore.release();
                }
            }
        } catch (InterruptedException ie) {
            playerPermit.remove(leader.getId());
        }

        return false;
    }

    //PQ method: starts a PQ
    public boolean startInstance(Party party, MapleMap map) {
        return startInstance(-1, party, map);
    }

    public boolean startInstance(int lobbyId, Party party, MapleMap map) {
        return startInstance(lobbyId, party, map, party.getLeader().getPlayer());
    }

    public boolean startInstance(int lobbyId, Party party, MapleMap map, Character leader) {
        if (this.isDisposed()) {
            return false;
        }

        try {
            if (!playerPermit.contains(leader.getId()) && startSemaphore.tryAcquire(7777, MILLISECONDS)) {
                playerPermit.add(leader.getId());

                startLock.lock();
                try {
                    try {
                        if (lobbyId == -1) {
                            lobbyId = availableLobbyInstance();
                            if (lobbyId == -1) {
                                return false;
                            }
                        } else {
                            if (!startLobbyInstance(lobbyId)) {
                                return false;
                            }
                        }

                        EventInstanceManager eim;
                        try {
                            eim = createInstance("setup", (Object) null);
                            registerEventInstance(eim.getName(), lobbyId);
                        } catch (ScriptException | NullPointerException e) {
                            String message = getInternalScriptExceptionMessage(e);
                            if (message != null && !message.startsWith(EventInstanceInProgressException.EIIP_KEY)) {
                                throw e;
                            }

                            if (lobbyId > -1) {
                                setLockLobby(lobbyId, false);
                            }
                            return false;
                        }

                        eim.setLeader(leader);

                        eim.registerParty(party, map);
                        party.setEligibleMembers(null);

                        eim.startEvent();
                    } catch (ScriptException | NoSuchMethodException ex) {
                        log.error("Event script startInstance", ex);
                    }

                    return true;
                } finally {
                    startLock.unlock();
                    playerPermit.remove(leader.getId());
                    startSemaphore.release();
                }
            }
        } catch (InterruptedException ie) {
            playerPermit.remove(leader.getId());
        }

        return false;
    }

    //PQ method: starts a PQ with a difficulty level, requires function setup(difficulty, leaderid) instead of setup()
    public boolean startInstance(Party party, MapleMap map, int difficulty) {
        return startInstance(-1, party, map, difficulty);
    }

    public boolean startInstance(int lobbyId, Party party, MapleMap map, int difficulty) {
        return startInstance(lobbyId, party, map, difficulty, party.getLeader().getPlayer());
    }

    public boolean startInstance(int lobbyId, Party party, MapleMap map, int difficulty, Character leader) {
        if (this.isDisposed()) {
            return false;
        }

        try {
            if (!playerPermit.contains(leader.getId()) && startSemaphore.tryAcquire(7777, MILLISECONDS)) {
                playerPermit.add(leader.getId());

                startLock.lock();
                try {
                    try {
                        if (lobbyId == -1) {
                            lobbyId = availableLobbyInstance();
                            if (lobbyId == -1) {
                                return false;
                            }
                        } else {
                            if (!startLobbyInstance(lobbyId)) {
                                return false;
                            }
                        }

                        EventInstanceManager eim;
                        try {
                            eim = createInstance("setup", difficulty, (lobbyId > -1) ? lobbyId : party.getLeaderId());
                            registerEventInstance(eim.getName(), lobbyId);
                        } catch (ScriptException | NullPointerException e) {
                            String message = getInternalScriptExceptionMessage(e);
                            if (message != null && !message.startsWith(EventInstanceInProgressException.EIIP_KEY)) {
                                throw e;
                            }

                            if (lobbyId > -1) {
                                setLockLobby(lobbyId, false);
                            }
                            return false;
                        }

                        eim.setLeader(leader);

                        eim.registerParty(party, map);
                        party.setEligibleMembers(null);

                        eim.startEvent();
                    } catch (ScriptException | NoSuchMethodException ex) {
                        log.error("Event script startInstance", ex);
                    }

                    return true;
                } finally {
                    startLock.unlock();
                    playerPermit.remove(leader.getId());
                    startSemaphore.release();
                }
            }
        } catch (InterruptedException ie) {
            playerPermit.remove(leader.getId());
        }

        return false;
    }

    //non-PQ method for starting instance
    public boolean startInstance(EventInstanceManager eim, String ldr) {
        return startInstance(-1, eim, ldr);
    }

    public boolean startInstance(EventInstanceManager eim, Character ldr) {
        return startInstance(-1, eim, ldr.getName(), ldr);
    }

    public boolean startInstance(int lobbyId, EventInstanceManager eim, String ldr) {
        return startInstance(-1, eim, ldr, eim.getEm().getChannelServer().getPlayerStorage().getCharacterByName(ldr));  // things they make me do...
    }

    public boolean startInstance(int lobbyId, EventInstanceManager eim, String ldr, Character leader) {
        if (this.isDisposed()) {
            return false;
        }

        try {
            if (!playerPermit.contains(leader.getId()) && startSemaphore.tryAcquire(7777, MILLISECONDS)) {
                playerPermit.add(leader.getId());

                startLock.lock();
                try {
                    try {
                        if (lobbyId == -1) {
                            lobbyId = availableLobbyInstance();
                            if (lobbyId == -1) {
                                return false;
                            }
                        } else {
                            if (!startLobbyInstance(lobbyId)) {
                                return false;
                            }
                        }

                        if (eim == null) {
                            if (lobbyId > -1) {
                                setLockLobby(lobbyId, false);
                            }
                            return false;
                        }
                        registerEventInstance(eim.getName(), lobbyId);
                        eim.setLeader(leader);

                        iv.invokeFunction("setup", eim);
                        eim.setProperty("leader", ldr);

                        eim.startEvent();
                    } catch (ScriptException | NoSuchMethodException ex) {
                        log.error("Event script startInstance", ex);
                    }

                    return true;
                } finally {
                    startLock.unlock();
                    playerPermit.remove(leader.getId());
                    startSemaphore.release();
                }
            }
        } catch (InterruptedException ie) {
            playerPermit.remove(leader.getId());
        }

        return false;
    }

    public List<PartyCharacter> getEligibleParty(Party party) {
        if (party == null) {
            return new ArrayList<>();
        }
        try {
            Object o = iv.invokeFunction("getEligibleParty", party.getPartyMembersOnline());

            if (o instanceof PartyCharacter[] partyChrs) {
                final List<PartyCharacter> eligibleParty = new ArrayList<>(Arrays.asList(partyChrs));
                party.setEligibleMembers(eligibleParty);
                return eligibleParty;
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }

        return new ArrayList<>();
    }

    public void clearPQ(EventInstanceManager eim) {
        try {
            iv.invokeFunction("clearPQ", eim);
        } catch (ScriptException | NoSuchMethodException ex) {
            log.error("Event script clearPQ", ex);
        }
    }

    public void clearPQ(EventInstanceManager eim, MapleMap toMap) {
        try {
            iv.invokeFunction("clearPQ", eim, toMap);
        } catch (ScriptException | NoSuchMethodException ex) {
            log.error("Event script clearPQ", ex);
        }
    }

    public Monster getMonster(int mid) {
        return (LifeFactory.getMonster(mid));
    }

    private void exportReadyGuild(Integer guildId) {
        Guild mg = server.getGuild(guildId);
        String callout = "[Guild Quest] Your guild has been registered to attend to the Sharenian Guild Quest at channel " + this.getChannelServer().getId()
                + " and HAS JUST STARTED THE STRATEGY PHASE. After 3 minutes, no more guild members will be allowed to join the effort."
                + " Check out Shuang at the excavation site in Perion for more info.";

        mg.dropMessage(6, callout);
    }

    private void exportMovedQueueToGuild(Integer guildId, int place) {
        Guild mg = server.getGuild(guildId);
        String callout = "[Guild Quest] Your guild has been registered to attend to the Sharenian Guild Quest at channel " + this.getChannelServer().getId()
                + " and is currently on the " + GameConstants.ordinal(place) + " place on the waiting queue.";

        mg.dropMessage(6, callout);
    }

    private List<Integer> getNextGuildQueue() {
        synchronized (queuedGuilds) {
            Integer guildId = queuedGuilds.poll();
            if (guildId == null) {
                return null;
            }

            wserv.removeGuildQueued(guildId);
            Integer leaderId = queuedGuildLeaders.remove(guildId);

            int place = 1;
            for (Integer i : queuedGuilds) {
                exportMovedQueueToGuild(i, place);
                place++;
            }

            List<Integer> list = new ArrayList<>(2);
            list.add(guildId);
            list.add(leaderId);
            return list;
        }
    }

    public boolean isQueueFull() {
        synchronized (queuedGuilds) {
            return queuedGuilds.size() >= YamlConfig.config.server.EVENT_MAX_GUILD_QUEUE;
        }
    }

    public int getQueueSize() {
        synchronized (queuedGuilds) {
            return queuedGuilds.size();
        }
    }

    public byte addGuildToQueue(Integer guildId, Integer leaderId) {
        if (wserv.isGuildQueued(guildId)) {
            return -1;
        }

        if (!isQueueFull()) {
            boolean canStartAhead;
            synchronized (queuedGuilds) {
                canStartAhead = queuedGuilds.isEmpty();

                queuedGuilds.add(guildId);
                wserv.putGuildQueued(guildId);
                queuedGuildLeaders.put(guildId, leaderId);

                int place = queuedGuilds.size();
                exportMovedQueueToGuild(guildId, place);
            }

            if (canStartAhead) {
                if (!attemptStartGuildInstance()) {
                    synchronized (queuedGuilds) {
                        queuedGuilds.add(guildId);
                        wserv.putGuildQueued(guildId);
                        queuedGuildLeaders.put(guildId, leaderId);
                    }
                } else {
                    return 2;
                }
            }

            return 1;
        } else {
            return 0;
        }
    }

    public boolean attemptStartGuildInstance() {
        Character chr = null;
        List<Integer> guildInstance = null;
        while (chr == null) {
            guildInstance = getNextGuildQueue();
            if (guildInstance == null) {
                return false;
            }

            chr = cserv.getPlayerStorage().getCharacterById(guildInstance.get(1));
        }

        if (startInstance(chr)) {
            exportReadyGuild(guildInstance.get(0));
            return true;
        } else {
            return false;
        }
    }

    public void startQuest(Character chr, int id, int npcid) {
        try {
            Quest.getInstance(id).forceStart(chr, npcid);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    public void completeQuest(Character chr, int id, int npcid) {
        try {
            Quest.getInstance(id).forceComplete(chr, npcid);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    public int getTransportationTime(int travelTime) {
        return this.getWorldServer().getTransportationTime(travelTime);
    }

    private void fillEimQueue() {
        ThreadManager.getInstance().newTask(new EventManagerTask());  //call new thread to fill up readied instances queue
    }

    private EventInstanceManager getReadyInstance() {
        queueLock.lock();
        try {
            if (readyInstances.isEmpty()) {
                fillEimQueue();
                return null;
            }

            EventInstanceManager eim = readyInstances.remove(0);
            fillEimQueue();

            return eim;
        } finally {
            queueLock.unlock();
        }
    }

    private void instantiateQueuedInstance() {
        int nextEventId;
        queueLock.lock();
        try {
            if (this.isDisposed() || readyInstances.size() + onLoadInstances >= Math.ceil((double) maxLobbys / 3.0)) {
                return;
            }

            onLoadInstances++;
            nextEventId = readyId;
            readyId++;
        } finally {
            queueLock.unlock();
        }

        EventInstanceManager eim = new EventInstanceManager(this, "sampleName" + nextEventId, dropProvider);
        queueLock.lock();
        try {
            if (this.isDisposed()) {  // EM already disposed
                return;
            }

            readyInstances.add(eim);
            onLoadInstances--;
        } finally {
            queueLock.unlock();
        }

        instantiateQueuedInstance();    // keep filling the queue until reach threshold.
    }

    private class EventManagerTask implements Runnable {

        @Override
        public void run() {
            instantiateQueuedInstance();
        }
    }
}
