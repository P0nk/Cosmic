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
package net.server.channel;

import client.Character;
import config.YamlConfig;
import constants.id.MapId;
import database.drop.DropProvider;
import net.ChannelDependencies;
import net.netty.ChannelServer;
import net.packet.Packet;
import net.server.PlayerStorage;
import net.server.Server;
import net.server.services.BaseService;
import net.server.services.ServicesManager;
import net.server.services.type.ChannelServices;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.event.EventScriptManager;
import server.TimerManager;
import server.events.gm.Event;
import server.expeditions.Expedition;
import server.expeditions.ExpeditionType;
import server.maps.HiredMerchant;
import server.maps.MapManager;
import server.maps.MapleMap;
import server.maps.MiniDungeon;
import server.maps.MiniDungeonInfo;
import service.TransitionService;
import tools.PacketCreator;
import tools.Pair;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class Channel {
    private static final Logger log = LoggerFactory.getLogger(Channel.class);
    private static final int BASE_PORT = 7575;

    private final int port;
    private final String ip;
    private final int world;
    private final int channel;

    private PlayerStorage players;
    private ChannelServer channelServer;
    private String serverMessage;
    private MapManager mapManager;
    private EventScriptManager eventSM;
    private ServicesManager services;
    private final DropProvider dropProvider;
    private final TransitionService transitionService;
    private final Map<Integer, HiredMerchant> hiredMerchants = new HashMap<>();
    private final Map<Integer, Integer> storedVars = new HashMap<>();
    private final Set<Integer> playersAway = new HashSet<>();
    private final Map<ExpeditionType, Expedition> expeditions = new HashMap<>();
    private final Map<Integer, MiniDungeon> dungeons = new HashMap<>();
    private final List<ExpeditionType> expedType = new ArrayList<>();
    private final Set<MapleMap> ownedMaps = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private Event event;
    private boolean finishedShutdown = false;
    private final Set<Integer> usedMC = new HashSet<>();

    private int usedDojo = 0;
    private int[] dojoStage;
    private long[] dojoFinishTime;
    private ScheduledFuture<?>[] dojoTask;
    private final Map<Integer, Integer> dojoParty = new HashMap<>();

    private final List<Integer> chapelReservationQueue = new LinkedList<>();
    private final List<Integer> cathedralReservationQueue = new LinkedList<>();
    private ScheduledFuture<?> chapelReservationTask;
    private ScheduledFuture<?> cathedralReservationTask;

    private Integer ongoingChapel = null;
    private Boolean ongoingChapelType = null;
    private Set<Integer> ongoingChapelGuests = null;
    private Integer ongoingCathedral = null;
    private Boolean ongoingCathedralType = null;
    private Set<Integer> ongoingCathedralGuests = null;
    private long ongoingStartTime;

    private final Lock lock = new ReentrantLock(true);;
    private final Lock merchRlock;
    private final Lock merchWlock;

    public Channel(final int world, final int channel, long startTime, ChannelDependencies channelDependencies) {
        this.world = world;
        this.channel = channel;
        this.dropProvider = channelDependencies.dropProvider();
        this.transitionService = channelDependencies.transitionService();
        this.players = new PlayerStorage(channelDependencies.transitionService());

        this.ongoingStartTime = startTime + 10000;  // rude approach to a world's last channel boot time, placeholder for the 1st wedding reservation ever
        this.mapManager = new MapManager(null, world, channel, dropProvider);
        this.port = BASE_PORT + (this.channel - 1) + (world * 100);
        this.ip = YamlConfig.config.server.HOST + ":" + port;

        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
        this.merchRlock = rwLock.readLock();
        this.merchWlock = rwLock.writeLock();

        try {
            this.channelServer = initServer(port, world, channel, channelDependencies.transitionService());
            expedType.addAll(Arrays.asList(ExpeditionType.values()));

            if (Server.getInstance().isOnline()) {  // postpone event loading to improve boot time... thanks Riizade, daronhudson for noticing slow startup times
                eventSM = new EventScriptManager(this, getEvents(), dropProvider);
                eventSM.init();
            } else {
                String[] ev = {"0_EXAMPLE"};
                eventSM = new EventScriptManager(this, ev, dropProvider);
            }

            dojoStage = new int[20];
            dojoFinishTime = new long[20];
            dojoTask = new ScheduledFuture<?>[20];
            for (int i = 0; i < 20; i++) {
                dojoStage[i] = 0;
                dojoFinishTime[i] = 0;
                dojoTask[i] = null;
            }

            services = new ServicesManager(ChannelServices.OVERALL);

            log.info("Channel {}: Listening on port {}", getId(), port);
        } catch (Exception e) {
            log.warn("Error during channel initialization", e);
        }
    }

    private ChannelServer initServer(int port, int world, int channel, TransitionService transitionService) {
        ChannelServer channelServer = new ChannelServer(port, world, channel, transitionService);
        channelServer.start();
        return channelServer;
    }

    public synchronized void reloadEventScriptManager() {
        if (finishedShutdown) {
            return;
        }

        eventSM.cancel();
        eventSM = null;
        eventSM = new EventScriptManager(this, getEvents(), dropProvider);
    }

    public synchronized void shutdown() {
        try {
            if (finishedShutdown) {
                return;
            }

            log.info("Shutting down channel {} in world {}", channel, world);

            closeAllMerchants();
            disconnectAwayPlayers();
            players.disconnectAll();

            eventSM.dispose();
            eventSM = null;

            mapManager.dispose();
            mapManager = null;

            closeChannelSchedules();
            players = null;

            channelServer.stop();

            finishedShutdown = true;
            log.info("Successfully shut down channel {} in world {}", channel, world);
        } catch (Exception e) {
            log.error("Error while shutting down channel {} in world {}", channel, world, e);
        }
    }

    private void closeChannelServices() {
        services.shutdown();
    }

    private void closeChannelSchedules() {
        lock.lock();
        try {
            for (int i = 0; i < dojoTask.length; i++) {
                if (dojoTask[i] != null) {
                    dojoTask[i].cancel(false);
                    dojoTask[i] = null;
                }
            }
        } finally {
            lock.unlock();
        }

        closeChannelServices();
    }

    private void closeAllMerchants() {
        try {
            List<HiredMerchant> merchs;

            merchWlock.lock();
            try {
                merchs = new ArrayList<>(hiredMerchants.values());
                hiredMerchants.clear();
            } finally {
                merchWlock.unlock();
            }

            for (HiredMerchant merch : merchs) {
                merch.forceClose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MapManager getMapFactory() {
        return mapManager;
    }

    public BaseService getServiceAccess(ChannelServices sv) {
        return services.getAccess(sv).getService();
    }

    public int getWorld() {
        return world;
    }

    public World getWorldServer() {
        return Server.getInstance().getWorld(world);
    }

    public void addPlayer(Character chr) {
        players.addPlayer(chr);
        chr.sendPacket(PacketCreator.serverMessage(serverMessage));
    }

    public String getServerMessage() {
        return serverMessage;
    }

    public PlayerStorage getPlayerStorage() {
        return players;
    }

    public boolean removePlayer(Character chr) {
        return players.removePlayer(chr.getId()) != null;
    }

    public int getChannelCapacity() {
        return (int) (Math.ceil(((float) players.getAllCharacters().size() / YamlConfig.config.server.CHANNEL_LOAD) * 800));
    }

    public void broadcastPacket(Packet packet) {
        for (Character chr : players.getAllCharacters()) {
            chr.sendPacket(packet);
        }
    }

    public final int getId() {
        return channel;
    }

    public String getIP() {
        return ip;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public EventScriptManager getEventSM() {
        return eventSM;
    }

    public void broadcastGMPacket(Packet packet) {
        for (Character chr : players.getAllCharacters()) {
            if (chr.isGM()) {
                chr.sendPacket(packet);
            }
        }
    }

    public List<Character> getPartyMembers(Party party) {
        List<Character> partym = new ArrayList<>(8);
        for (PartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getId()) {
                Character chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }

    public void insertPlayerAway(int chrId) {   // either they in CS or MTS
        playersAway.add(chrId);
    }

    public void removePlayerAway(int chrId) {
        playersAway.remove(chrId);
    }

    private void disconnectAwayPlayers() {
        World wserv = getWorldServer();
        for (Integer cid : playersAway) {
            Character chr = wserv.getPlayerStorage().getCharacterById(cid);
            if (chr != null && chr.isLoggedin()) {
                transitionService.forceDisconnect(chr.getClient());
            }
        }
    }

    public Map<Integer, HiredMerchant> getHiredMerchants() {
        merchRlock.lock();
        try {
            return Collections.unmodifiableMap(hiredMerchants);
        } finally {
            merchRlock.unlock();
        }
    }

    public void addHiredMerchant(int chrid, HiredMerchant hm) {
        merchWlock.lock();
        try {
            hiredMerchants.put(chrid, hm);
        } finally {
            merchWlock.unlock();
        }
    }

    public void removeHiredMerchant(int chrid) {
        merchWlock.lock();
        try {
            hiredMerchants.remove(chrid);
        } finally {
            merchWlock.unlock();
        }
    }

    public int[] multiBuddyFind(int charIdFrom, int[] characterIds) {
        List<Integer> ret = new ArrayList<>(characterIds.length);
        PlayerStorage playerStorage = getPlayerStorage();
        for (int characterId : characterIds) {
            Character chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(charIdFrom)) {
                    ret.add(characterId);
                }
            }
        }
        int[] retArr = new int[ret.size()];
        int pos = 0;
        for (Integer i : ret) {
            retArr[pos++] = i;
        }
        return retArr;
    }

    public boolean addExpedition(Expedition exped) {
        synchronized (expeditions) {
            if (expeditions.containsKey(exped.getType())) {
                return false;
            }

            expeditions.put(exped.getType(), exped);
            exped.beginRegistration();  // thanks Conrad for noticing leader still receiving packets on failure-to-register cases
            return true;
        }
    }

    public void removeExpedition(Expedition exped) {
        synchronized (expeditions) {
            expeditions.remove(exped.getType());
        }
    }

    public Expedition getExpedition(ExpeditionType type) {
        return expeditions.get(type);
    }

    public List<Expedition> getExpeditions() {
        synchronized (expeditions) {
            return new ArrayList<>(expeditions.values());
        }
    }

    public boolean isConnected(String name) {
        return getPlayerStorage().getCharacterByName(name) != null;
    }

    public boolean isActive() {
        EventScriptManager esm = this.getEventSM();
        return esm != null && esm.isActive();
    }

    public boolean finishedShutdown() {
        return finishedShutdown;
    }

    public void setServerMessage(String message) {
        this.serverMessage = message;
        broadcastPacket(PacketCreator.serverMessage(message));
        getWorldServer().resetDisabledServerMessages();
    }

    private static String[] getEvents() {
        List<String> events = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of("scripts/event"))) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                events.add(fileName.substring(0, fileName.length() - 3));
            }
        } catch (IOException e) {
            log.warn("Unable to load events !");
            e.printStackTrace();
        }
        return events.toArray(new String[0]);
    }

    public int getStoredVar(int key) {
        if (storedVars.containsKey(key)) {
            return storedVars.get(key);
        }

        return 0;
    }

    public void setStoredVar(int key, int val) {
        this.storedVars.put(key, val);
    }

    public int lookupPartyDojo(Party party) {
        if (party == null) {
            return -1;
        }

        Integer i = dojoParty.get(party.hashCode());
        return (i != null) ? i : -1;
    }

    public int ingressDojo(boolean isPartyDojo, int fromStage) {
        return ingressDojo(isPartyDojo, null, fromStage);
    }

    public int ingressDojo(boolean isPartyDojo, Party party, int fromStage) {
        lock.lock();
        try {
            int dojoList = this.usedDojo;
            int range, slot = 0;

            if (!isPartyDojo) {
                dojoList = dojoList >> 5;
                range = 15;
            } else {
                range = 5;
            }

            while ((dojoList & 1) != 0) {
                dojoList = (dojoList >> 1);
                slot++;
            }

            if (slot < range) {
                int slotMapid = (isPartyDojo ? MapId.DOJO_PARTY_BASE : MapId.DOJO_SOLO_BASE) + (100 * (fromStage + 1)) + slot;
                int dojoSlot = getDojoSlot(slotMapid);

                if (party != null) {
                    if (dojoParty.containsKey(party.hashCode())) {
                        return -2;
                    }
                    dojoParty.put(party.hashCode(), dojoSlot);
                }

                this.usedDojo |= (1 << dojoSlot);

                this.resetDojo(slotMapid);
                this.startDojoSchedule(slotMapid);
                return slot;
            } else {
                return -1;
            }
        } finally {
            lock.unlock();
        }
    }

    private void freeDojoSlot(int slot, Party party) {
        int mask = 0b11111111111111111111;
        mask ^= (1 << slot);

        lock.lock();
        try {
            usedDojo &= mask;
        } finally {
            lock.unlock();
        }

        if (party != null) {
            if (dojoParty.remove(party.hashCode()) != null) {
                return;
            }
        }

        if (dojoParty.containsValue(slot)) {    // strange case, no party there!
            Set<Entry<Integer, Integer>> es = new HashSet<>(dojoParty.entrySet());

            for (Entry<Integer, Integer> e : es) {
                if (e.getValue() == slot) {
                    dojoParty.remove(e.getKey());
                    break;
                }
            }
        }
    }

    private static int getDojoSlot(int dojoMapId) {
        return (dojoMapId % 100) + ((dojoMapId / 10000 == 92502) ? 5 : 0);
    }

    public void resetDojoMap(int fromMapId) {
        for (int i = 0; i < (((fromMapId / 100) % 100 <= 36) ? 5 : 2); i++) {
            this.getMapFactory().getMap(fromMapId + (100 * i)).resetMapObjects();
        }
    }

    public void resetDojo(int dojoMapId) {
        resetDojo(dojoMapId, -1);
    }

    private void resetDojo(int dojoMapId, int thisStg) {
        int slot = getDojoSlot(dojoMapId);
        this.dojoStage[slot] = thisStg;
    }

    public void freeDojoSectionIfEmpty(int dojoMapId) {
        final int slot = getDojoSlot(dojoMapId);
        final int delta = (dojoMapId) % 100;
        final int stage = (dojoMapId / 100) % 100;
        final int dojoBaseMap = (dojoMapId >= MapId.DOJO_PARTY_BASE) ? MapId.DOJO_PARTY_BASE : MapId.DOJO_SOLO_BASE;

        for (int i = 0; i < 5; i++) { //only 32 stages, but 38 maps
            if (stage + i > 38) {
                break;
            }
            MapleMap dojoMap = getMapFactory().getMap(dojoBaseMap + (100 * (stage + i)) + delta);
            if (!dojoMap.getAllPlayers().isEmpty()) {
                return;
            }
        }

        freeDojoSlot(slot, null);
    }

    private void startDojoSchedule(final int dojoMapId) {
        final int slot = getDojoSlot(dojoMapId);
        final int stage = (dojoMapId / 100) % 100;
        if (stage <= dojoStage[slot]) {
            return;
        }

        long clockTime = (stage > 36 ? 15 : (stage / 6) + 5) * 60000;

        lock.lock();
        try {
            if (this.dojoTask[slot] != null) {
                this.dojoTask[slot].cancel(false);
            }
            this.dojoTask[slot] = TimerManager.getInstance().schedule(() -> {
                final int delta = (dojoMapId) % 100;
                final int dojoBaseMap = (slot < 5) ? MapId.DOJO_PARTY_BASE : MapId.DOJO_SOLO_BASE;
                Party party = null;

                for (int i = 0; i < 5; i++) { //only 32 stages, but 38 maps
                    if (stage + i > 38) {
                        break;
                    }

                    MapleMap dojoExit = getMapFactory().getMap(MapId.DOJO_EXIT);
                    for (Character chr : getMapFactory().getMap(dojoBaseMap + (100 * (stage + i)) + delta).getAllPlayers()) {
                        if (MapId.isDojo(chr.getMap().getId())) {
                            chr.changeMap(dojoExit);
                        }
                        party = chr.getParty();
                    }
                }

                freeDojoSlot(slot, party);
            }, clockTime + 3000);   // let the TIMES UP display for 3 seconds, then warp
        } finally {
            lock.unlock();
        }

        dojoFinishTime[slot] = Server.getInstance().getCurrentTime() + clockTime;
    }

    public void dismissDojoSchedule(int dojoMapId, Party party) {
        int slot = getDojoSlot(dojoMapId);
        int stage = (dojoMapId / 100) % 100;
        if (stage <= dojoStage[slot]) {
            return;
        }

        lock.lock();
        try {
            if (this.dojoTask[slot] != null) {
                this.dojoTask[slot].cancel(false);
                this.dojoTask[slot] = null;
            }
        } finally {
            lock.unlock();
        }

        freeDojoSlot(slot, party);
    }

    public boolean setDojoProgress(int dojoMapId) {
        int slot = getDojoSlot(dojoMapId);
        int dojoStg = (dojoMapId / 100) % 100;

        if (this.dojoStage[slot] < dojoStg) {
            this.dojoStage[slot] = dojoStg;
            return true;
        } else {
            return false;
        }
    }

    public long getDojoFinishTime(int dojoMapId) {
        return dojoFinishTime[getDojoSlot(dojoMapId)];
    }

    public boolean addMiniDungeon(int dungeonid) {
        lock.lock();
        try {
            if (dungeons.containsKey(dungeonid)) {
                return false;
            }

            MiniDungeonInfo mmdi = MiniDungeonInfo.getDungeon(dungeonid);
            MiniDungeon mmd = new MiniDungeon(mmdi.getBase(), this.getMapFactory().getMap(mmdi.getDungeonId()).getTimeLimit());   // thanks Conrad for noticing hardcoded time limit for minidungeons

            dungeons.put(dungeonid, mmd);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public MiniDungeon getMiniDungeon(int dungeonid) {
        lock.lock();
        try {
            return dungeons.get(dungeonid);
        } finally {
            lock.unlock();
        }
    }

    public void removeMiniDungeon(int dungeonid) {
        lock.lock();
        try {
            dungeons.remove(dungeonid);
        } finally {
            lock.unlock();
        }
    }

    public Pair<Boolean, Pair<Integer, Set<Integer>>> getNextWeddingReservation(boolean cathedral) {
        Integer ret;

        lock.lock();
        try {
            List<Integer> weddingReservationQueue = (cathedral ? cathedralReservationQueue : chapelReservationQueue);
            if (weddingReservationQueue.isEmpty()) {
                return null;
            }

            ret = weddingReservationQueue.remove(0);
            if (ret == null) {
                return null;
            }
        } finally {
            lock.unlock();
        }

        World wserv = getWorldServer();

        Pair<Integer, Integer> coupleId = wserv.getMarriageQueuedCouple(ret);
        Pair<Boolean, Set<Integer>> typeGuests = wserv.removeMarriageQueued(ret);

        Pair<String, String> couple = new Pair<>(Character.getNameById(coupleId.getLeft()), Character.getNameById(coupleId.getRight()));
        wserv.dropMessage(6, couple.getLeft() + " and " + couple.getRight() + "'s wedding is going to be started at " + (cathedral ? "Cathedral" : "Chapel") + " on Channel " + channel + ".");

        return new Pair<>(typeGuests.getLeft(), new Pair<>(ret, typeGuests.getRight()));
    }

    public boolean isWeddingReserved(Integer weddingId) {
        World wserv = getWorldServer();

        lock.lock();
        try {
            return wserv.isMarriageQueued(weddingId) || weddingId.equals(ongoingCathedral) || weddingId.equals(ongoingChapel);
        } finally {
            lock.unlock();
        }
    }

    public int getWeddingReservationStatus(Integer weddingId, boolean cathedral) {
        if (weddingId == null) {
            return -1;
        }

        lock.lock();
        try {
            if (cathedral) {
                if (weddingId.equals(ongoingCathedral)) {
                    return 0;
                }

                for (int i = 0; i < cathedralReservationQueue.size(); i++) {
                    if (weddingId.equals(cathedralReservationQueue.get(i))) {
                        return i + 1;
                    }
                }
            } else {
                if (weddingId.equals(ongoingChapel)) {
                    return 0;
                }

                for (int i = 0; i < chapelReservationQueue.size(); i++) {
                    if (weddingId.equals(chapelReservationQueue.get(i))) {
                        return i + 1;
                    }
                }
            }

            return -1;
        } finally {
            lock.unlock();
        }
    }

    public int pushWeddingReservation(Integer weddingId, boolean cathedral, boolean premium, Integer groomId, Integer brideId) {
        if (weddingId == null || isWeddingReserved(weddingId)) {
            return -1;
        }

        World wserv = getWorldServer();
        wserv.putMarriageQueued(weddingId, cathedral, premium, groomId, brideId);

        lock.lock();
        try {
            List<Integer> weddingReservationQueue = (cathedral ? cathedralReservationQueue : chapelReservationQueue);

            int delay = YamlConfig.config.server.WEDDING_RESERVATION_DELAY - 1 - weddingReservationQueue.size();
            for (int i = 0; i < delay; i++) {
                weddingReservationQueue.add(null);  // push empty slots to fill the waiting time
            }

            weddingReservationQueue.add(weddingId);
            return weddingReservationQueue.size();
        } finally {
            lock.unlock();
        }
    }

    public boolean isOngoingWeddingGuest(boolean cathedral, int playerId) {
        lock.lock();
        try {
            if (cathedral) {
                return ongoingCathedralGuests != null && ongoingCathedralGuests.contains(playerId);
            } else {
                return ongoingChapelGuests != null && ongoingChapelGuests.contains(playerId);
            }
        } finally {
            lock.unlock();
        }
    }

    public Integer getOngoingWedding(boolean cathedral) {
        lock.lock();
        try {
            return cathedral ? ongoingCathedral : ongoingChapel;
        } finally {
            lock.unlock();
        }
    }

    public boolean getOngoingWeddingType(boolean cathedral) {
        lock.lock();
        try {
            return cathedral ? ongoingCathedralType : ongoingChapelType;
        } finally {
            lock.unlock();
        }
    }

    public void closeOngoingWedding(boolean cathedral) {
        lock.lock();
        try {
            if (cathedral) {
                ongoingCathedral = null;
                ongoingCathedralType = null;
                ongoingCathedralGuests = null;
            } else {
                ongoingChapel = null;
                ongoingChapelType = null;
                ongoingChapelGuests = null;
            }
        } finally {
            lock.unlock();
        }
    }

    public void setOngoingWedding(final boolean cathedral, Boolean premium, Integer weddingId, Set<Integer> guests) {
        lock.lock();
        try {
            if (cathedral) {
                ongoingCathedral = weddingId;
                ongoingCathedralType = premium;
                ongoingCathedralGuests = guests;
            } else {
                ongoingChapel = weddingId;
                ongoingChapelType = premium;
                ongoingChapelGuests = guests;
            }
        } finally {
            lock.unlock();
        }

        ongoingStartTime = System.currentTimeMillis();
        if (weddingId != null) {
            ScheduledFuture<?> weddingTask = TimerManager.getInstance().schedule(() -> closeOngoingWedding(cathedral), MINUTES.toMillis(YamlConfig.config.server.WEDDING_RESERVATION_TIMEOUT));

            if (cathedral) {
                cathedralReservationTask = weddingTask;
            } else {
                chapelReservationTask = weddingTask;
            }
        }
    }

    public synchronized boolean acceptOngoingWedding(final boolean cathedral) {     // couple succeeded to show up and started the ceremony
        if (cathedral) {
            if (cathedralReservationTask == null) {
                return false;
            }

            cathedralReservationTask.cancel(false);
            cathedralReservationTask = null;
        } else {
            if (chapelReservationTask == null) {
                return false;
            }

            chapelReservationTask.cancel(false);
            chapelReservationTask = null;
        }

        return true;
    }

    private static String getTimeLeft(long futureTime) {
        StringBuilder str = new StringBuilder();
        long leftTime = futureTime - System.currentTimeMillis();

        if (leftTime < 0) {
            return null;
        }

        byte mode = 0;
        if (leftTime / (MINUTES.toMillis(1)) > 0) {
            mode++;     //counts minutes

            if (leftTime / (HOURS.toMillis(1)) > 0) {
                mode++;     //counts hours
            }
        }

        switch (mode) {
            case 2:
                int hours = (int) ((leftTime / (HOURS.toMillis(1))));
                str.append(hours + " hours, ");

            case 1:
                int minutes = (int) ((leftTime / (MINUTES.toMillis(1))) % 60);
                str.append(minutes + " minutes, ");

            default:
                int seconds = (int) (leftTime / SECONDS.toMillis(1)) % 60;
                str.append(seconds + " seconds");
        }

        return str.toString();
    }

    public long getWeddingTicketExpireTime(int resSlot) {
        return ongoingStartTime + getRelativeWeddingTicketExpireTime(resSlot);
    }

    public static long getRelativeWeddingTicketExpireTime(int resSlot) {
        return MINUTES.toMillis((long) resSlot * YamlConfig.config.server.WEDDING_RESERVATION_INTERVAL);
    }

    public String getWeddingReservationTimeLeft(Integer weddingId) {
        if (weddingId == null) {
            return null;
        }

        lock.lock();
        try {
            boolean cathedral = true;

            int resStatus;
            resStatus = getWeddingReservationStatus(weddingId, true);
            if (resStatus < 0) {
                cathedral = false;
                resStatus = getWeddingReservationStatus(weddingId, false);

                if (resStatus < 0) {
                    return null;
                }
            }

            String venue = (cathedral ? "Cathedral" : "Chapel");
            if (resStatus == 0) {
                return venue + " - RIGHT NOW";
            }

            return venue + " - " + getTimeLeft(ongoingStartTime + MINUTES.toMillis((long) resStatus * YamlConfig.config.server.WEDDING_RESERVATION_INTERVAL)) + " from now";
        } finally {
            lock.unlock();
        }
    }

    public Pair<Integer, Integer> getWeddingCoupleForGuest(int guestId, boolean cathedral) {
        lock.lock();
        try {
            return (isOngoingWeddingGuest(cathedral, guestId)) ? getWorldServer().getRelationshipCouple(getOngoingWedding(cathedral)) : null;
        } finally {
            lock.unlock();
        }
    }

    public void dropMessage(int type, String message) {
        for (Character player : getPlayerStorage().getAllCharacters()) {
            player.dropMessage(type, message);
        }
    }

    public void registerOwnedMap(MapleMap map) {
        ownedMaps.add(map);
    }

    public void unregisterOwnedMap(MapleMap map) {
        ownedMaps.remove(map);
    }

    public void runCheckOwnedMapsSchedule() {
        if (!ownedMaps.isEmpty()) {
            List<MapleMap> ownedMapsList;

            synchronized (ownedMaps) {
                ownedMapsList = new ArrayList<>(ownedMaps);
            }

            for (MapleMap map : ownedMapsList) {
                map.checkMapOwnerActivity();
            }
        }
    }

    private static int getMonsterCarnivalRoom(boolean cpq1, int field) {
        return (cpq1 ? 0 : 100) + field;
    }

    public void initMonsterCarnival(boolean cpq1, int field) {
        usedMC.add(getMonsterCarnivalRoom(cpq1, field));
    }

    public void finishMonsterCarnival(boolean cpq1, int field) {
        usedMC.remove(getMonsterCarnivalRoom(cpq1, field));
    }

    public boolean canInitMonsterCarnival(boolean cpq1, int field) {
        return !usedMC.contains(getMonsterCarnivalRoom(cpq1, field));
    }

    public void debugMarriageStatus() {
        log.debug(" ----- WORLD DATA -----");
        getWorldServer().debugMarriageStatus();

        log.debug(" ----- CH. {} -----", channel);
        log.debug(" ----- CATHEDRAL -----");
        log.debug("Current Queue: {}", cathedralReservationQueue);
        log.debug("Cancel Task?: {}", cathedralReservationTask != null);
        log.debug("Ongoing wid: {}", ongoingCathedral);
        log.debug("Ongoing wid: {}, isPremium: {}", ongoingCathedral, ongoingCathedralType);
        log.debug("Guest list: {}", ongoingCathedralGuests);
        log.debug(" ----- CHAPEL -----");
        log.debug("Current Queue: {}", chapelReservationQueue);
        log.debug("Cancel Task?: {}", chapelReservationTask != null);
        log.debug("Ongoing wid: {}", ongoingChapel);
        log.debug("Ongoing wid: {}, isPremium: {}", ongoingChapel, ongoingChapelType);
        log.debug("Guest list: {}", ongoingChapelGuests);
        log.debug("Starttime: {}", ongoingStartTime);
    }
}
