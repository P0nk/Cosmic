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
package net.server.world;

import client.BuddyList;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.BuddylistEntry;
import client.Character;
import client.Family;
import config.YamlConfig;
import constants.game.GameConstants;
import net.packet.Packet;
import net.server.PlayerStorage;
import net.server.Server;
import net.server.channel.Channel;
import net.server.channel.CharacterIdChannelPair;
import net.server.coordinator.matchchecker.MatchCheckerCoordinator;
import net.server.coordinator.partysearch.PartySearchCoordinator;
import net.server.coordinator.world.InviteCoordinator;
import net.server.coordinator.world.InviteCoordinator.InviteResultType;
import net.server.coordinator.world.InviteCoordinator.InviteType;
import net.server.guild.Guild;
import net.server.guild.GuildCharacter;
import net.server.guild.GuildPackets;
import net.server.guild.GuildSummary;
import net.server.services.BaseService;
import net.server.services.ServicesManager;
import net.server.services.type.WorldServices;
import net.server.task.CharacterHpDecreaseTask;
import net.server.task.FamilyDailyResetTask;
import net.server.task.FishingTask;
import net.server.task.HiredMerchantTask;
import net.server.task.MapOwnershipTask;
import net.server.task.MountTirednessTask;
import net.server.task.PartySearchTask;
import net.server.task.PetFullnessTask;
import net.server.task.ServerMessageTask;
import net.server.task.TimedMapObjectTask;
import net.server.task.TimeoutTask;
import net.server.task.WeddingReservationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.event.EventInstanceManager;
import server.Storage;
import server.TimerManager;
import server.maps.AbstractMapObject;
import server.maps.HiredMerchant;
import server.maps.MapleMap;
import server.maps.MiniDungeon;
import server.maps.MiniDungeonInfo;
import server.maps.PlayerShop;
import server.maps.PlayerShopItem;
import service.TransitionService;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;
import tools.packets.Fishing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author kevintjuh93
 * @author Ronan - thread-oriented (world schedules + guild queue + marriages + party chars)
 */
public class World {
    private static final Logger log = LoggerFactory.getLogger(World.class);

    private final int id;
    private int flag;
    private int exprate;
    private int droprate;
    private int bossdroprate;
    private int mesorate;
    private int questrate;
    private int travelrate;
    private int fishingrate;
    private final String eventmsg;
    private final List<Channel> channels = new ArrayList<>();
    private final Map<Integer, Byte> pnpcStep = new HashMap<>();
    private final Map<Integer, Short> pnpcPodium = new HashMap<>();
    private final Map<Integer, Messenger> messengers = new HashMap<>();
    private final AtomicInteger runningMessengerId = new AtomicInteger();
    private final Map<Integer, Family> families = new LinkedHashMap<>();
    private final Map<Integer, Integer> relationships = new HashMap<>();
    private final Map<Integer, Pair<Integer, Integer>> relationshipCouples = new HashMap<>();
    private final Map<Integer, GuildSummary> gsStore = new HashMap<>();
    private PlayerStorage players;
    private final ServicesManager services = new ServicesManager(WorldServices.SAVE_CHARACTER);
    private final MatchCheckerCoordinator matchChecker = new MatchCheckerCoordinator();
    private final PartySearchCoordinator partySearch = new PartySearchCoordinator();

    private final Lock chnRLock;
    private final Lock chnWLock;

    private final Map<Integer, SortedMap<Integer, Character>> accountChars = new HashMap<>();
    private final Map<Integer, Storage> accountStorages = new HashMap<>();
    private final Lock accountCharsLock = new ReentrantLock(true);

    private final Set<Integer> queuedGuilds = new HashSet<>();
    private final Map<Integer, Pair<Pair<Boolean, Boolean>, Pair<Integer, Integer>>> queuedMarriages = new HashMap<>();
    private final Map<Integer, Set<Integer>> marriageGuests = new ConcurrentHashMap<>();

    private final Map<Integer, Integer> partyChars = new HashMap<>();
    private final Map<Integer, Party> parties = new HashMap<>();
    private final AtomicInteger runningPartyId = new AtomicInteger();
    private final Lock partyLock = new ReentrantLock(true);

    private final Map<Integer, Integer> owlSearched = new LinkedHashMap<>();
    private final List<Map<Integer, Integer>> cashItemBought = new ArrayList<>(9);

    private final Lock suggestRLock;
    private final Lock suggestWLock;

    private final Map<Integer, Integer> disabledServerMessages = new HashMap<>();    // reuse owl lock
    private final Lock srvMessagesLock = new ReentrantLock();
    private ScheduledFuture<?> srvMessagesSchedule;

    private Lock activePetsLock = new ReentrantLock(true);
    private final Map<Integer, Integer> activePets = new LinkedHashMap<>();
    private ScheduledFuture<?> petsSchedule;
    private long petUpdate;

    private Lock activeMountsLock = new ReentrantLock(true);
    private final Map<Integer, Integer> activeMounts = new LinkedHashMap<>();
    private ScheduledFuture<?> mountsSchedule;
    private long mountUpdate;

    private Lock activePlayerShopsLock = new ReentrantLock(true);
    private final Map<Integer, PlayerShop> activePlayerShops = new LinkedHashMap<>();

    private Lock activeMerchantsLock = new ReentrantLock(true);
    private final Map<Integer, Pair<HiredMerchant, Integer>> activeMerchants = new LinkedHashMap<>();
    private ScheduledFuture<?> merchantSchedule;
    private long merchantUpdate;

    private final Map<Runnable, Long> registeredTimedMapObjects = new LinkedHashMap<>();
    private ScheduledFuture<?> timedMapObjectsSchedule;
    private Lock timedMapObjectLock = new ReentrantLock(true);

    private final Map<Character, Integer> fishingAttempters = Collections.synchronizedMap(new WeakHashMap<>());
    private Map<Character, Integer> playerHpDec = Collections.synchronizedMap(new WeakHashMap<>());

    private ScheduledFuture<?> charactersSchedule;
    private ScheduledFuture<?> marriagesSchedule;
    private ScheduledFuture<?> mapOwnershipSchedule;
    private ScheduledFuture<?> fishingSchedule;
    private ScheduledFuture<?> partySearchSchedule;
    private ScheduledFuture<?> timeoutSchedule;
    private ScheduledFuture<?> hpDecSchedule;

    public World(int world, int flag, String eventmsg, int exprate, int droprate, int bossdroprate, int mesorate,
                 int questrate, int travelrate, int fishingrate, TransitionService transitionService) {
        this.id = world;
        this.flag = flag;
        this.eventmsg = eventmsg;
        this.exprate = exprate;
        this.droprate = droprate;
        this.bossdroprate = bossdroprate;
        this.mesorate = mesorate;
        this.questrate = questrate;
        this.travelrate = travelrate;
        this.fishingrate = fishingrate;
        runningPartyId.set(1000000001); // partyid must not clash with charid to solve update item looting issues, found thanks to Vcoc
        runningMessengerId.set(1);

        this.players = new PlayerStorage(transitionService);

        ReadWriteLock channelLock = new ReentrantReadWriteLock(true);
        this.chnRLock = channelLock.readLock();
        this.chnWLock = channelLock.writeLock();

        ReadWriteLock suggestLock = new ReentrantReadWriteLock(true);
        this.suggestRLock = suggestLock.readLock();
        this.suggestWLock = suggestLock.writeLock();

        petUpdate = Server.getInstance().getCurrentTime();
        mountUpdate = petUpdate;

        for (int i = 0; i < 9; i++) {
            cashItemBought.add(new LinkedHashMap<>());
        }

        TimerManager tman = TimerManager.getInstance();
        petsSchedule = tman.register(new PetFullnessTask(this), MINUTES.toMillis(1), MINUTES.toMillis(1));
        srvMessagesSchedule = tman.register(new ServerMessageTask(this), SECONDS.toMillis(10), SECONDS.toMillis(10));
        mountsSchedule = tman.register(new MountTirednessTask(this), MINUTES.toMillis(1), MINUTES.toMillis(1));
        merchantSchedule = tman.register(new HiredMerchantTask(this), 10 * MINUTES.toMillis(1), 10 * MINUTES.toMillis(1));
        timedMapObjectsSchedule = tman.register(new TimedMapObjectTask(this), MINUTES.toMillis(1), MINUTES.toMillis(1));
        marriagesSchedule = tman.register(new WeddingReservationTask(this), MINUTES.toMillis(YamlConfig.config.server.WEDDING_RESERVATION_INTERVAL), MINUTES.toMillis(YamlConfig.config.server.WEDDING_RESERVATION_INTERVAL));
        mapOwnershipSchedule = tman.register(new MapOwnershipTask(this), SECONDS.toMillis(20), SECONDS.toMillis(20));
        fishingSchedule = tman.register(new FishingTask(this), SECONDS.toMillis(10), SECONDS.toMillis(10));
        partySearchSchedule = tman.register(new PartySearchTask(this), SECONDS.toMillis(10), SECONDS.toMillis(10));
        timeoutSchedule = tman.register(new TimeoutTask(this, transitionService), SECONDS.toMillis(10), SECONDS.toMillis(10));
        hpDecSchedule = tman.register(new CharacterHpDecreaseTask(this), YamlConfig.config.server.MAP_DAMAGE_OVERTIME_INTERVAL, YamlConfig.config.server.MAP_DAMAGE_OVERTIME_INTERVAL);

        if (YamlConfig.config.server.USE_FAMILY_SYSTEM) {
            long timeLeft = Server.getTimeLeftForNextDay();
            FamilyDailyResetTask.resetEntitlementUsage(this);
            tman.register(new FamilyDailyResetTask(this), DAYS.toMillis(1), timeLeft);
        }
    }

    public int getChannelsSize() {
        chnRLock.lock();
        try {
            return channels.size();
        } finally {
            chnRLock.unlock();
        }
    }

    public List<Channel> getChannels() {
        chnRLock.lock();
        try {
            return new ArrayList<>(channels);
        } finally {
            chnRLock.unlock();
        }
    }

    public Channel getChannel(int channel) {
        chnRLock.lock();
        try {
            try {
                return channels.get(channel - 1);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        } finally {
            chnRLock.unlock();
        }
    }

    public boolean addChannel(Channel channel) {
        chnWLock.lock();
        try {
            if (channel.getId() == channels.size() + 1) {
                channels.add(channel);
                return true;
            } else {
                return false;
            }
        } finally {
            chnWLock.unlock();
        }
    }

    public void setFlag(byte b) {
        this.flag = b;
    }

    public int getFlag() {
        return flag;
    }

    public String getEventMessage() {
        return eventmsg;
    }

    public int getExpRate() {
        return exprate;
    }

    public void setExpRate(int exp) {
        Collection<Character> list = getPlayerStorage().getAllCharacters();

        for (Character chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.revertWorldRates();
        }
        this.exprate = exp;
        for (Character chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.setWorldRates();
        }
    }

    public int getDropRate() {
        return droprate;
    }

    public void setDropRate(int drop) {
        Collection<Character> list = getPlayerStorage().getAllCharacters();

        for (Character chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.revertWorldRates();
        }
        this.droprate = drop;
        for (Character chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.setWorldRates();
        }
    }

    public int getBossDropRate() {  // boss rate concept thanks to Lapeiro
        return bossdroprate;
    }

    public void setBossDropRate(int bossdrop) {
        bossdroprate = bossdrop;
    }

    public int getMesoRate() {
        return mesorate;
    }

    public void setMesoRate(int meso) {
        Collection<Character> list = getPlayerStorage().getAllCharacters();

        for (Character chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.revertWorldRates();
        }
        this.mesorate = meso;
        for (Character chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.setWorldRates();
        }
    }

    public int getQuestRate() {
        return questrate;
    }

    public void setQuestRate(int quest) {
        this.questrate = quest;
    }

    public int getTravelRate() {
        return travelrate;
    }

    public void setTravelRate(int travel) {
        this.travelrate = travel;
    }

    public int getTransportationTime(int travelTime) {
        return (int) Math.ceil((double) travelTime / travelrate);
    }

    public int getFishingRate() {
        return fishingrate;
    }

    public void setFishingRate(int quest) {
        this.fishingrate = quest;
    }

    public void loadAccountCharactersView(Integer accountId, List<Character> chars) {
        SortedMap<Integer, Character> charsMap = new TreeMap<>();
        for (Character chr : chars) {
            charsMap.put(chr.getId(), chr);
        }

        accountCharsLock.lock();    // accountCharsLock should be used after server's lgnWLock for compliance
        try {
            accountChars.put(accountId, charsMap);
        } finally {
            accountCharsLock.unlock();
        }
    }

    public void registerAccountCharacterView(Integer accountId, Character chr) {
        accountCharsLock.lock();
        try {
            accountChars.get(accountId).put(chr.getId(), chr);
        } finally {
            accountCharsLock.unlock();
        }
    }

    public void unregisterAccountCharacterView(Integer accountId, Integer chrId) {
        accountCharsLock.lock();
        try {
            accountChars.get(accountId).remove(chrId);
        } finally {
            accountCharsLock.unlock();
        }
    }

    public void loadAccountStorage(Integer accountId) {
        if (getAccountStorage(accountId) == null) {
            registerAccountStorage(accountId);
        }
    }

    private void registerAccountStorage(Integer accountId) {
        Storage storage = Storage.loadOrCreateFromDB(accountId, this.id);
        accountCharsLock.lock();
        try {
            accountStorages.put(accountId, storage);
        } finally {
            accountCharsLock.unlock();
        }
    }

    public void unregisterAccountStorage(Integer accountId) {
        accountCharsLock.lock();
        try {
            accountStorages.remove(accountId);
        } finally {
            accountCharsLock.unlock();
        }
    }

    public Storage getAccountStorage(Integer accountId) {
        return accountStorages.get(accountId);
    }

    private static List<Entry<Integer, SortedMap<Integer, Character>>> getSortedAccountCharacterView(Map<Integer, SortedMap<Integer, Character>> map) {
        List<Entry<Integer, SortedMap<Integer, Character>>> list = new ArrayList<>(map.size());
        list.addAll(map.entrySet());

        list.sort((o1, o2) -> o1.getKey() - o2.getKey());

        return list;
    }

    public List<Character> loadAndGetAllCharactersView() {
        Server.getInstance().loadAllAccountsCharactersView();
        return getAllCharactersView();
    }

    public List<Character> getAllCharactersView() {    // sorting by accountid, charid
        List<Character> chrList = new LinkedList<>();
        Map<Integer, SortedMap<Integer, Character>> accChars;

        accountCharsLock.lock();
        try {
            accChars = new HashMap<>(accountChars);
        } finally {
            accountCharsLock.unlock();
        }

        for (Entry<Integer, SortedMap<Integer, Character>> e : getSortedAccountCharacterView(accChars)) {
            chrList.addAll(e.getValue().values());
        }

        return chrList;
    }

    public List<Character> getAccountCharactersView(int accountId) {
        final List<Character> chrList;

        accountCharsLock.lock();
        try {
            SortedMap<Integer, Character> accChars = accountChars.get(accountId);

            if (accChars != null) {
                chrList = new LinkedList<>(accChars.values());
            } else {
                accountChars.put(accountId, new TreeMap<>());
                chrList = null;
            }
        } finally {
            accountCharsLock.unlock();
        }

        return chrList;
    }

    public PlayerStorage getPlayerStorage() {
        return players;
    }

    public MatchCheckerCoordinator getMatchCheckerCoordinator() {
        return matchChecker;
    }

    public PartySearchCoordinator getPartySearchCoordinator() {
        return partySearch;
    }

    public void addPlayer(Character chr) {
        players.addPlayer(chr);
    }

    public void removePlayer(Character chr) {
        Channel cserv = chr.getClient().getChannelServer();

        if (cserv != null) {
            if (!cserv.removePlayer(chr)) {
                // oy the player is not where they should be, find this mf

                for (Channel ch : getChannels()) {
                    if (ch.removePlayer(chr)) {
                        break;
                    }
                }
            }
        }

        players.removePlayer(chr.getId());
    }

    public int getId() {
        return id;
    }

    public void addFamily(int id, Family f) {
        synchronized (families) {
            if (!families.containsKey(id)) {
                families.put(id, f);
            }
        }
    }

    public void removeFamily(int id) {
        synchronized (families) {
            families.remove(id);
        }
    }

    public Family getFamily(int id) {
        synchronized (families) {
            if (families.containsKey(id)) {
                return families.get(id);
            }
            return null;
        }
    }

    public Collection<Family> getFamilies() {
        synchronized (families) {
            return Collections.unmodifiableCollection(families.values());
        }
    }

    public Guild getGuild(GuildCharacter mgc) {
        if (mgc == null) {
            return null;
        }

        int gid = mgc.getGuildId();
        Guild g = Server.getInstance().getGuild(gid, mgc.getWorld(), mgc.getCharacter());
        if (gsStore.get(gid) == null) {
            gsStore.put(gid, new GuildSummary(g));
        }
        return g;
    }

    public boolean isWorldCapacityFull() {
        return getWorldCapacityStatus() == 2;
    }

    public int getWorldCapacityStatus() {
        int worldCap = getChannelsSize() * YamlConfig.config.server.CHANNEL_LOAD;
        int num = players.getSize();

        int status;
        if (num >= worldCap) {
            status = 2;
        } else if (num >= worldCap * .8) { // More than 80 percent o___o
            status = 1;
        } else {
            status = 0;
        }

        return status;
    }

    public GuildSummary getGuildSummary(int gid, int wid) {
        if (gsStore.containsKey(gid)) {
            return gsStore.get(gid);
        } else {
            Guild g = Server.getInstance().getGuild(gid, wid, null);
            if (g != null) {
                gsStore.put(gid, new GuildSummary(g));
            }
            return gsStore.get(gid);
        }
    }

    public void updateGuildSummary(int gid, GuildSummary mgs) {
        gsStore.put(gid, mgs);
    }

    public void reloadGuildSummary() {
        Guild g;
        Server server = Server.getInstance();
        for (int i : gsStore.keySet()) {
            g = server.getGuild(i, getId(), null);
            if (g != null) {
                gsStore.put(i, new GuildSummary(g));
            } else {
                gsStore.remove(i);
            }
        }
    }

    public void setGuildAndRank(List<Integer> cids, int guildid, int rank, int exception) {
        for (int cid : cids) {
            if (cid != exception) {
                setGuildAndRank(cid, guildid, rank);
            }
        }
    }

    public void setOfflineGuildStatus(int guildid, int guildrank, int cid) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?")) {
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, cid);
            ps.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public void setGuildAndRank(int cid, int guildid, int rank) {
        Character mc = getPlayerStorage().getCharacterById(cid);
        if (mc == null) {
            return;
        }
        boolean bDifferentGuild;
        if (guildid == -1 && rank == -1) {
            bDifferentGuild = true;
        } else {
            bDifferentGuild = guildid != mc.getGuildId();
            mc.getMGC().setGuildId(guildid);
            mc.getMGC().setGuildRank(rank);

            if (bDifferentGuild) {
                mc.getMGC().setAllianceRank(5);
            }

            mc.saveGuildStatus();
        }
        if (bDifferentGuild) {
            if (mc.isLoggedinWorld()) {
                Guild guild = Server.getInstance().getGuild(guildid);
                if (guild != null) {
                    mc.getMap().broadcastPacket(mc, GuildPackets.guildNameChanged(cid, guild.getName()));
                    mc.getMap().broadcastPacket(mc, GuildPackets.guildMarkChanged(cid, guild));
                } else {
                    mc.getMap().broadcastPacket(mc, GuildPackets.guildNameChanged(cid, ""));
                }
            }
        }
    }

    public void changeEmblem(int gid, List<Integer> affectedPlayers, GuildSummary mgs) {
        updateGuildSummary(gid, mgs);
        sendPacket(affectedPlayers, GuildPackets.guildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1);
        setGuildAndRank(affectedPlayers, -1, -1, -1);    //respawn player
    }

    public void sendPacket(List<Integer> targetIds, Packet packet, int exception) {
        Character chr;
        for (int i : targetIds) {
            if (i == exception) {
                continue;
            }
            chr = getPlayerStorage().getCharacterById(i);
            if (chr != null) {
                chr.sendPacket(packet);
            }
        }
    }

    public boolean isGuildQueued(int guildId) {
        return queuedGuilds.contains(guildId);
    }

    public void putGuildQueued(int guildId) {
        queuedGuilds.add(guildId);
    }

    public void removeGuildQueued(int guildId) {
        queuedGuilds.remove(guildId);
    }

    public boolean isMarriageQueued(int marriageId) {
        return queuedMarriages.containsKey(marriageId);
    }

    public Pair<Boolean, Boolean> getMarriageQueuedLocation(int marriageId) {
        Pair<Pair<Boolean, Boolean>, Pair<Integer, Integer>> qm = queuedMarriages.get(marriageId);
        return (qm != null) ? qm.getLeft() : null;
    }

    public Pair<Integer, Integer> getMarriageQueuedCouple(int marriageId) {
        Pair<Pair<Boolean, Boolean>, Pair<Integer, Integer>> qm = queuedMarriages.get(marriageId);
        return (qm != null) ? qm.getRight() : null;
    }

    public void putMarriageQueued(int marriageId, boolean cathedral, boolean premium, int groomId, int brideId) {
        queuedMarriages.put(marriageId, new Pair<>(new Pair<>(cathedral, premium), new Pair<>(groomId, brideId)));
        marriageGuests.put(marriageId, new HashSet());
    }

    public Pair<Boolean, Set<Integer>> removeMarriageQueued(int marriageId) {
        Boolean type = queuedMarriages.remove(marriageId).getLeft().getRight();
        Set<Integer> guests = marriageGuests.remove(marriageId);

        return new Pair<>(type, guests);
    }

    public boolean addMarriageGuest(int marriageId, int playerId) {
        Set<Integer> guests = marriageGuests.get(marriageId);
        if (guests != null) {
            if (guests.contains(playerId)) {
                return false;
            }

            guests.add(playerId);
            return true;
        }

        return false;
    }

    public Pair<Integer, Integer> getWeddingCoupleForGuest(int guestId, Boolean cathedral) {
        for (Channel ch : getChannels()) {
            Pair<Integer, Integer> p = ch.getWeddingCoupleForGuest(guestId, cathedral);
            if (p != null) {
                return p;
            }
        }

        List<Integer> possibleWeddings = new LinkedList<>();
        for (Entry<Integer, Set<Integer>> mg : new HashSet<>(marriageGuests.entrySet())) {
            if (mg.getValue().contains(guestId)) {
                Pair<Boolean, Boolean> loc = getMarriageQueuedLocation(mg.getKey());
                if (loc != null && cathedral.equals(loc.getLeft())) {
                    possibleWeddings.add(mg.getKey());
                }
            }
        }

        int pwSize = possibleWeddings.size();
        if (pwSize == 0) {
            return null;
        } else if (pwSize > 1) {
            int selectedPw = -1;
            int selectedPos = Integer.MAX_VALUE;

            for (Integer pw : possibleWeddings) {
                for (Channel ch : getChannels()) {
                    int pos = ch.getWeddingReservationStatus(pw, cathedral);
                    if (pos != -1) {
                        if (pos < selectedPos) {
                            selectedPos = pos;
                            selectedPw = pw;
                            break;
                        }
                    }
                }
            }

            if (selectedPw == -1) {
                return null;
            }

            possibleWeddings.clear();
            possibleWeddings.add(selectedPw);
        }

        return getMarriageQueuedCouple(possibleWeddings.get(0));
    }

    public void debugMarriageStatus() {
        log.debug("Queued marriages: {}", queuedMarriages);
        log.debug("Guest list: {}", marriageGuests);
    }

    private void registerCharacterParty(Integer chrid, Integer partyid) {
        partyLock.lock();
        try {
            partyChars.put(chrid, partyid);
        } finally {
            partyLock.unlock();
        }
    }

    private void unregisterCharacterPartyInternal(Integer chrid) {
        partyChars.remove(chrid);
    }

    private void unregisterCharacterParty(Integer chrid) {
        partyLock.lock();
        try {
            unregisterCharacterPartyInternal(chrid);
        } finally {
            partyLock.unlock();
        }
    }

    public Integer getCharacterPartyid(Integer chrid) {
        partyLock.lock();
        try {
            return partyChars.get(chrid);
        } finally {
            partyLock.unlock();
        }
    }

    public Party createParty(PartyCharacter chrfor) {
        int partyid = runningPartyId.getAndIncrement();
        Party party = new Party(partyid, chrfor);

        partyLock.lock();
        try {
            parties.put(party.getId(), party);
            registerCharacterParty(chrfor.getId(), partyid);
        } finally {
            partyLock.unlock();
        }

        party.addMember(chrfor);
        return party;
    }

    public Party getParty(int partyid) {
        partyLock.lock();
        try {
            return parties.get(partyid);
        } finally {
            partyLock.unlock();
        }
    }

    private Party disbandParty(int partyid) {
        partyLock.lock();
        try {
            return parties.remove(partyid);
        } finally {
            partyLock.unlock();
        }
    }

    private void updateCharacterParty(Party party, PartyOperation operation, PartyCharacter target, Collection<PartyCharacter> partyMembers) {
        switch (operation) {
            case JOIN:
                registerCharacterParty(target.getId(), party.getId());
                break;

            case LEAVE:
            case EXPEL:
                unregisterCharacterParty(target.getId());
                break;

            case DISBAND:
                partyLock.lock();
                try {
                    for (PartyCharacter partychar : partyMembers) {
                        unregisterCharacterPartyInternal(partychar.getId());
                    }
                } finally {
                    partyLock.unlock();
                }
                break;

            default:
                break;
        }
    }

    private void updateParty(Party party, PartyOperation operation, PartyCharacter target) {
        Collection<PartyCharacter> partyMembers = party.getMembers();
        updateCharacterParty(party, operation, target, partyMembers);

        for (PartyCharacter partychar : partyMembers) {
            Character chr = getPlayerStorage().getCharacterById(partychar.getId());
            if (chr != null) {
                if (operation == PartyOperation.DISBAND) {
                    chr.setParty(null);
                    chr.setMPC(null);
                } else {
                    chr.setParty(party);
                    chr.setMPC(partychar);
                }
                chr.sendPacket(PacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
            }
        }
        switch (operation) {
            case LEAVE:
            case EXPEL:
                Character chr = getPlayerStorage().getCharacterById(target.getId());
                if (chr != null) {
                    chr.sendPacket(PacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                    chr.setParty(null);
                    chr.setMPC(null);
                }
            default:
                break;
        }
    }

    public void updateParty(int partyid, PartyOperation operation, PartyCharacter target) {
        Party party = getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        switch (operation) {
            case JOIN:
                party.addMember(target);
                break;
            case EXPEL:
            case LEAVE:
                party.removeMember(target);
                break;
            case DISBAND:
                disbandParty(partyid);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                party.updateMember(target);
                break;
            case CHANGE_LEADER:
                Character mc = party.getLeader().getPlayer();
                if (mc != null) {
                    EventInstanceManager eim = mc.getEventInstance();

                    if (eim != null && eim.isEventLeader(mc)) {
                        eim.changedLeader(target);
                    } else {
                        int oldLeaderMapid = mc.getMapId();

                        if (MiniDungeonInfo.isDungeonMap(oldLeaderMapid)) {
                            if (oldLeaderMapid != target.getMapId()) {
                                MiniDungeon mmd = mc.getClient().getChannelServer().getMiniDungeon(oldLeaderMapid);
                                if (mmd != null) {
                                    mmd.close();
                                }
                            }
                        }
                    }
                    party.setLeader(target);
                }
                break;
            default:
                log.warn("Unhandled updateParty operation: {}", operation.name());
        }
        updateParty(party, operation, target);
    }

    public void removeMapPartyMembers(int partyid) {
        Party party = getParty(partyid);
        if (party == null) {
            return;
        }

        for (PartyCharacter mpc : party.getMembers()) {
            Character mc = mpc.getPlayer();
            if (mc != null) {
                MapleMap map = mc.getMap();
                if (map != null) {
                    map.removeParty(partyid);
                }
            }
        }
    }

    public int find(String name) {
        int channel = -1;
        Character chr = getPlayerStorage().getCharacterByName(name);
        if (chr != null) {
            channel = chr.getClient().getChannel();
        }
        return channel;
    }

    public int find(int id) {
        int channel = -1;
        Character chr = getPlayerStorage().getCharacterById(id);
        if (chr != null) {
            channel = chr.getClient().getChannel();
        }
        return channel;
    }

    public void partyChat(Party party, String chattext, String namefrom) {
        for (PartyCharacter partychar : party.getMembers()) {
            if (!(partychar.getName().equals(namefrom))) {
                Character chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    chr.sendPacket(PacketCreator.multiChat(namefrom, chattext, 1));
                }
            }
        }
    }

    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) {
        PlayerStorage playerStorage = getPlayerStorage();
        for (int characterId : recipientCharacterIds) {
            Character chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(cidFrom)) {
                    chr.sendPacket(PacketCreator.multiChat(nameFrom, chattext, 0));
                }
            }
        }
    }

    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) {
        List<CharacterIdChannelPair> foundsChars = new ArrayList<>(characterIds.length);
        for (Channel ch : getChannels()) {
            for (int charid : ch.multiBuddyFind(charIdFrom, characterIds)) {
                foundsChars.add(new CharacterIdChannelPair(charid, ch.getId()));
            }
        }
        return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
    }

    public Messenger getMessenger(int messengerid) {
        return messengers.get(messengerid);
    }

    public void leaveMessenger(int messengerid, MessengerCharacter target) {
        Messenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        int position = messenger.getPositionByName(target.getName());
        messenger.removeMember(target);
        removeMessengerPlayer(messenger, position);
    }

    public void messengerInvite(String sender, int messengerid, String target, int fromchannel) {
        if (isConnected(target)) {
            Character targetChr = getPlayerStorage().getCharacterByName(target);
            if (targetChr != null) {
                Messenger messenger = targetChr.getMessenger();
                if (messenger == null) {
                    Character from = getChannel(fromchannel).getPlayerStorage().getCharacterByName(sender);
                    if (from != null) {
                        if (InviteCoordinator.createInvite(InviteType.MESSENGER, from, messengerid, targetChr.getId())) {
                            targetChr.sendPacket(PacketCreator.messengerInvite(sender, messengerid));
                            from.sendPacket(PacketCreator.messengerNote(target, 4, 1));
                        } else {
                            from.sendPacket(PacketCreator.messengerChat(sender + " : " + target + " is already managing a Maple Messenger invitation"));
                        }
                    }
                } else {
                    Character from = getChannel(fromchannel).getPlayerStorage().getCharacterByName(sender);
                    from.sendPacket(PacketCreator.messengerChat(sender + " : " + target + " is already using Maple Messenger"));
                }
            }
        }
    }

    public void addMessengerPlayer(Messenger messenger, String namefrom, int fromchannel, int position) {
        for (MessengerCharacter messengerchar : messenger.getMembers()) {
            Character chr = getPlayerStorage().getCharacterByName(messengerchar.getName());
            if (chr == null) {
                continue;
            }
            if (!messengerchar.getName().equals(namefrom)) {
                Character from = getChannel(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                chr.sendPacket(PacketCreator.addMessengerPlayer(namefrom, from, position, (byte) (fromchannel - 1)));
                from.sendPacket(PacketCreator.addMessengerPlayer(chr.getName(), chr, messengerchar.getPosition(), (byte) (messengerchar.getChannel() - 1)));
            } else {
                chr.sendPacket(PacketCreator.joinMessenger(messengerchar.getPosition()));
            }
        }
    }

    public void removeMessengerPlayer(Messenger messenger, int position) {
        for (MessengerCharacter messengerchar : messenger.getMembers()) {
            Character chr = getPlayerStorage().getCharacterByName(messengerchar.getName());
            if (chr != null) {
                chr.sendPacket(PacketCreator.removeMessengerPlayer(position));
            }
        }
    }

    public void messengerChat(Messenger messenger, String chattext, String namefrom) {
        String from = "";
        String to1 = "";
        String to2 = "";
        for (MessengerCharacter messengerchar : messenger.getMembers()) {
            if (!(messengerchar.getName().equals(namefrom))) {
                Character chr = getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.sendPacket(PacketCreator.messengerChat(chattext));
                    if (to1.equals("")) {
                        to1 = messengerchar.getName();
                    } else if (to2.equals("")) {
                        to2 = messengerchar.getName();
                    }
                }
            } else {
                from = messengerchar.getName();
            }
        }
    }

    public void declineChat(String sender, Character player) {
        if (isConnected(sender)) {
            Character senderChr = getPlayerStorage().getCharacterByName(sender);
            if (senderChr != null && senderChr.getMessenger() != null) {
                if (InviteCoordinator.answerInvite(InviteType.MESSENGER, player.getId(), senderChr.getMessenger().getId(), false).result == InviteResultType.DENIED) {
                    senderChr.sendPacket(PacketCreator.messengerNote(player.getName(), 5, 0));
                }
            }
        }
    }

    public void updateMessenger(int messengerid, String namefrom, int fromchannel) {
        Messenger messenger = getMessenger(messengerid);
        int position = messenger.getPositionByName(namefrom);
        updateMessenger(messenger, namefrom, position, fromchannel);
    }

    public void updateMessenger(Messenger messenger, String namefrom, int position, int fromchannel) {
        for (MessengerCharacter messengerchar : messenger.getMembers()) {
            Channel ch = getChannel(fromchannel);
            if (!(messengerchar.getName().equals(namefrom))) {
                Character chr = ch.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.sendPacket(PacketCreator.updateMessengerPlayer(namefrom, getChannel(fromchannel).getPlayerStorage().getCharacterByName(namefrom), position, (byte) (fromchannel - 1)));
                }
            }
        }
    }

    public void silentLeaveMessenger(int messengerid, MessengerCharacter target) {
        Messenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target, target.getPosition());
    }

    public void joinMessenger(int messengerid, MessengerCharacter target, String from, int fromchannel) {
        Messenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target, target.getPosition());
        addMessengerPlayer(messenger, from, fromchannel, target.getPosition());
    }

    public void silentJoinMessenger(int messengerid, MessengerCharacter target, int position) {
        Messenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target, position);
    }

    public Messenger createMessenger(MessengerCharacter chrfor) {
        int messengerid = runningMessengerId.getAndIncrement();
        Messenger messenger = new Messenger(messengerid, chrfor);
        messengers.put(messenger.getId(), messenger);
        return messenger;
    }

    public boolean isConnected(String charName) {
        return getPlayerStorage().getCharacterByName(charName) != null;
    }

    public BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom) {
        Character addChar = getPlayerStorage().getCharacterByName(addName);
        if (addChar != null) {
            BuddyList buddylist = addChar.getBuddylist();
            if (buddylist.isFull()) {
                return BuddyAddResult.BUDDYLIST_FULL;
            }
            if (!buddylist.contains(cidFrom)) {
                buddylist.addBuddyRequest(addChar.getClient(), cidFrom, nameFrom, channelFrom);
            } else if (buddylist.containsVisible(cidFrom)) {
                return BuddyAddResult.ALREADY_ON_LIST;
            }
        }
        return BuddyAddResult.OK;
    }

    public void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation) {
        Character addChar = getPlayerStorage().getCharacterById(cid);
        if (addChar != null) {
            BuddyList buddylist = addChar.getBuddylist();
            switch (operation) {
                case ADDED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, "Default Group", cidFrom, channel, true));
                        addChar.sendPacket(PacketCreator.updateBuddyChannel(cidFrom, (byte) (channel - 1)));
                    }
                    break;
                case DELETED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, "Default Group", cidFrom, (byte) -1, buddylist.get(cidFrom).isVisible()));
                        addChar.sendPacket(PacketCreator.updateBuddyChannel(cidFrom, (byte) -1));
                    }
                    break;
            }
        }
    }

    public void loggedOff(String name, int characterId, int channel, int[] buddies) {
        updateBuddies(characterId, channel, buddies, true);
    }

    public void loggedOn(String name, int characterId, int channel, int[] buddies) {
        updateBuddies(characterId, channel, buddies, false);
    }

    private void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
        PlayerStorage playerStorage = getPlayerStorage();
        for (int buddy : buddies) {
            Character chr = playerStorage.getCharacterById(buddy);
            if (chr != null) {
                BuddylistEntry ble = chr.getBuddylist().get(characterId);
                if (ble != null && ble.isVisible()) {
                    int mcChannel;
                    if (offline) {
                        ble.setChannel((byte) -1);
                        mcChannel = -1;
                    } else {
                        ble.setChannel(channel);
                        mcChannel = (byte) (channel - 1);
                    }
                    chr.getBuddylist().put(ble);
                    chr.sendPacket(PacketCreator.updateBuddyChannel(ble.getCharacterId(), mcChannel));
                }
            }
        }
    }

    private static Integer getPetKey(Character chr, byte petSlot) {    // assuming max 3 pets
        return (chr.getId() << 2) + petSlot;
    }

    public void addOwlItemSearch(Integer itemid) {
        suggestWLock.lock();
        try {
            Integer cur = owlSearched.get(itemid);
            if (cur != null) {
                owlSearched.put(itemid, cur + 1);
            } else {
                owlSearched.put(itemid, 1);
            }
        } finally {
            suggestWLock.unlock();
        }
    }

    public List<Pair<Integer, Integer>> getOwlSearchedItems() {
        if (YamlConfig.config.server.USE_ENFORCE_ITEM_SUGGESTION) {
            return new ArrayList<>(0);
        }

        suggestRLock.lock();
        try {
            List<Pair<Integer, Integer>> searchCounts = new ArrayList<>(owlSearched.size());

            for (Entry<Integer, Integer> e : owlSearched.entrySet()) {
                searchCounts.add(new Pair<>(e.getKey(), e.getValue()));
            }

            return searchCounts;
        } finally {
            suggestRLock.unlock();
        }
    }

    public void addCashItemBought(Integer snid) {
        suggestWLock.lock();
        try {
            Map<Integer, Integer> tabItemBought = cashItemBought.get(snid / 10000000);

            Integer cur = tabItemBought.get(snid);
            if (cur != null) {
                tabItemBought.put(snid, cur + 1);
            } else {
                tabItemBought.put(snid, 1);
            }
        } finally {
            suggestWLock.unlock();
        }
    }

    private List<List<Pair<Integer, Integer>>> getBoughtCashItems() {
        if (YamlConfig.config.server.USE_ENFORCE_ITEM_SUGGESTION) {
            List<List<Pair<Integer, Integer>>> boughtCounts = new ArrayList<>(9);

            // thanks GabrielSin for pointing out an issue here
            for (int i = 0; i < 9; i++) {
                List<Pair<Integer, Integer>> tabCounts = new ArrayList<>(0);
                boughtCounts.add(tabCounts);
            }

            return boughtCounts;
        }

        suggestRLock.lock();
        try {
            List<List<Pair<Integer, Integer>>> boughtCounts = new ArrayList<>(cashItemBought.size());

            for (Map<Integer, Integer> tab : cashItemBought) {
                List<Pair<Integer, Integer>> tabItems = new LinkedList<>();
                boughtCounts.add(tabItems);

                for (Entry<Integer, Integer> e : tab.entrySet()) {
                    tabItems.add(new Pair<>(e.getKey(), e.getValue()));
                }
            }

            return boughtCounts;
        } finally {
            suggestRLock.unlock();
        }
    }

    private List<Integer> getMostSellerOnTab(List<Pair<Integer, Integer>> tabSellers) {
        List<Integer> tabLeaderboards;

        // descending order
        Comparator<Pair<Integer, Integer>> comparator = (p1, p2) -> p2.getRight().compareTo(p1.getRight());

        PriorityQueue<Pair<Integer, Integer>> queue = new PriorityQueue<>(Math.max(1, tabSellers.size()), comparator);
        queue.addAll(tabSellers);

        tabLeaderboards = new LinkedList<>();
        for (int i = 0; i < Math.min(tabSellers.size(), 5); i++) {
            tabLeaderboards.add(queue.remove().getLeft());
        }

        return tabLeaderboards;
    }

    public List<List<Integer>> getMostSellerCashItems() {
        List<List<Pair<Integer, Integer>>> mostSellers = this.getBoughtCashItems();
        List<List<Integer>> cashLeaderboards = new ArrayList<>(9);
        List<Integer> tabLeaderboards;
        List<Integer> allLeaderboards = null;

        for (List<Pair<Integer, Integer>> tabSellers : mostSellers) {
            if (tabSellers.size() < 5) {
                if (allLeaderboards == null) {
                    List<Pair<Integer, Integer>> allSellers = new LinkedList<>();
                    for (List<Pair<Integer, Integer>> tabItems : mostSellers) {
                        allSellers.addAll(tabItems);
                    }

                    allLeaderboards = getMostSellerOnTab(allSellers);
                }

                tabLeaderboards = new LinkedList<>();
                if (allLeaderboards.size() < 5) {
                    for (int i : GameConstants.CASH_DATA) {
                        tabLeaderboards.add(i);
                    }
                } else {
                    tabLeaderboards.addAll(allLeaderboards);
                }
            } else {
                tabLeaderboards = getMostSellerOnTab(tabSellers);
            }

            cashLeaderboards.add(tabLeaderboards);
        }

        return cashLeaderboards;
    }

    public void registerPetHunger(Character chr, byte petSlot) {
        if (chr.isGM() && YamlConfig.config.server.GM_PETS_NEVER_HUNGRY || YamlConfig.config.server.PETS_NEVER_HUNGRY) {
            return;
        }

        Integer key = getPetKey(chr, petSlot);

        activePetsLock.lock();
        try {
            int initProc;
            if (Server.getInstance().getCurrentTime() - petUpdate > 55000) {
                initProc = YamlConfig.config.server.PET_EXHAUST_COUNT - 2;
            } else {
                initProc = YamlConfig.config.server.PET_EXHAUST_COUNT - 1;
            }

            activePets.put(key, initProc);
        } finally {
            activePetsLock.unlock();
        }
    }

    public void unregisterPetHunger(Character chr, byte petSlot) {
        Integer key = getPetKey(chr, petSlot);

        activePetsLock.lock();
        try {
            activePets.remove(key);
        } finally {
            activePetsLock.unlock();
        }
    }

    public void runPetSchedule() {
        Map<Integer, Integer> deployedPets;

        activePetsLock.lock();
        try {
            petUpdate = Server.getInstance().getCurrentTime();
            deployedPets = new HashMap<>(activePets);   // exception here found thanks to MedicOP
        } finally {
            activePetsLock.unlock();
        }

        for (Map.Entry<Integer, Integer> dp : deployedPets.entrySet()) {
            Character chr = this.getPlayerStorage().getCharacterById(dp.getKey() / 4);
            if (chr == null || !chr.isLoggedinWorld()) {
                continue;
            }

            int dpVal = dp.getValue() + 1;
            if (dpVal == YamlConfig.config.server.PET_EXHAUST_COUNT) {
                chr.runFullnessSchedule(dp.getKey() % 4);
                dpVal = 0;
            }

            activePetsLock.lock();
            try {
                activePets.put(dp.getKey(), dpVal);
            } finally {
                activePetsLock.unlock();
            }
        }
    }

    public void registerMountHunger(Character chr) {
        if (chr.isGM() && YamlConfig.config.server.GM_PETS_NEVER_HUNGRY || YamlConfig.config.server.PETS_NEVER_HUNGRY) {
            return;
        }

        Integer key = chr.getId();
        activeMountsLock.lock();
        try {
            int initProc;
            if (Server.getInstance().getCurrentTime() - mountUpdate > 45000) {
                initProc = YamlConfig.config.server.MOUNT_EXHAUST_COUNT - 2;
            } else {
                initProc = YamlConfig.config.server.MOUNT_EXHAUST_COUNT - 1;
            }

            activeMounts.put(key, initProc);
        } finally {
            activeMountsLock.unlock();
        }
    }

    public void unregisterMountHunger(Character chr) {
        Integer key = chr.getId();

        activeMountsLock.lock();
        try {
            activeMounts.remove(key);
        } finally {
            activeMountsLock.unlock();
        }
    }

    public void runMountSchedule() {
        Map<Integer, Integer> deployedMounts;
        activeMountsLock.lock();
        try {
            mountUpdate = Server.getInstance().getCurrentTime();
            deployedMounts = new HashMap<>(activeMounts);
        } finally {
            activeMountsLock.unlock();
        }

        for (Map.Entry<Integer, Integer> dp : deployedMounts.entrySet()) {
            Character chr = this.getPlayerStorage().getCharacterById(dp.getKey());
            if (chr == null || !chr.isLoggedinWorld()) {
                continue;
            }

            int dpVal = dp.getValue() + 1;
            if (dpVal == YamlConfig.config.server.MOUNT_EXHAUST_COUNT) {
                if (!chr.runTirednessSchedule()) {
                    continue;
                }
                dpVal = 0;
            }

            activeMountsLock.lock();
            try {
                activeMounts.put(dp.getKey(), dpVal);
            } finally {
                activeMountsLock.unlock();
            }
        }
    }

    public void registerPlayerShop(PlayerShop ps) {
        activePlayerShopsLock.lock();
        try {
            activePlayerShops.put(ps.getOwner().getId(), ps);
        } finally {
            activePlayerShopsLock.unlock();
        }
    }

    public void unregisterPlayerShop(PlayerShop ps) {
        activePlayerShopsLock.lock();
        try {
            activePlayerShops.remove(ps.getOwner().getId());
        } finally {
            activePlayerShopsLock.unlock();
        }
    }

    public List<PlayerShop> getActivePlayerShops() {
        List<PlayerShop> psList = new ArrayList<>();
        activePlayerShopsLock.lock();
        try {
            psList.addAll(activePlayerShops.values());

            return psList;
        } finally {
            activePlayerShopsLock.unlock();
        }
    }

    public PlayerShop getPlayerShop(int ownerid) {
        activePlayerShopsLock.lock();
        try {
            return activePlayerShops.get(ownerid);
        } finally {
            activePlayerShopsLock.unlock();
        }
    }

    public void registerHiredMerchant(HiredMerchant hm) {
        activeMerchantsLock.lock();
        try {
            int initProc;
            if (Server.getInstance().getCurrentTime() - merchantUpdate > MINUTES.toMillis(5)) {
                initProc = 1;
            } else {
                initProc = 0;
            }

            activeMerchants.put(hm.getOwnerId(), new Pair<>(hm, initProc));
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public void unregisterHiredMerchant(HiredMerchant hm) {
        activeMerchantsLock.lock();
        try {
            activeMerchants.remove(hm.getOwnerId());
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public void runHiredMerchantSchedule() {
        Map<Integer, Pair<HiredMerchant, Integer>> deployedMerchants;
        activeMerchantsLock.lock();
        try {
            merchantUpdate = Server.getInstance().getCurrentTime();
            deployedMerchants = new LinkedHashMap<>(activeMerchants);

            for (Map.Entry<Integer, Pair<HiredMerchant, Integer>> dm : deployedMerchants.entrySet()) {
                int timeOn = dm.getValue().getRight();
                HiredMerchant hm = dm.getValue().getLeft();

                if (timeOn <= 144) {   // 1440 minutes == 24hrs
                    activeMerchants.put(hm.getOwnerId(), new Pair<>(dm.getValue().getLeft(), timeOn + 1));
                } else {
                    hm.forceClose();
                    this.getChannel(hm.getChannel()).removeHiredMerchant(hm.getOwnerId());

                    activeMerchants.remove(dm.getKey());
                }
            }
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public List<HiredMerchant> getActiveMerchants() {
        List<HiredMerchant> hmList = new ArrayList<>();
        activeMerchantsLock.lock();
        try {
            for (Pair<HiredMerchant, Integer> hmp : activeMerchants.values()) {
                HiredMerchant hm = hmp.getLeft();
                if (hm.isOpen()) {
                    hmList.add(hm);
                }
            }

            return hmList;
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public HiredMerchant getHiredMerchant(int ownerid) {
        activeMerchantsLock.lock();
        try {
            if (activeMerchants.containsKey(ownerid)) {
                return activeMerchants.get(ownerid).getLeft();
            }

            return null;
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public void registerTimedMapObject(Runnable r, long duration) {
        timedMapObjectLock.lock();
        try {
            long expirationTime = Server.getInstance().getCurrentTime() + duration;
            registeredTimedMapObjects.put(r, expirationTime);
        } finally {
            timedMapObjectLock.unlock();
        }
    }

    public void runTimedMapObjectSchedule() {
        List<Runnable> toRemove = new LinkedList<>();

        timedMapObjectLock.lock();
        try {
            long timeNow = Server.getInstance().getCurrentTime();

            for (Entry<Runnable, Long> rtmo : registeredTimedMapObjects.entrySet()) {
                if (rtmo.getValue() <= timeNow) {
                    toRemove.add(rtmo.getKey());
                }
            }

            for (Runnable r : toRemove) {
                registeredTimedMapObjects.remove(r);
            }
        } finally {
            timedMapObjectLock.unlock();
        }

        for (Runnable r : toRemove) {
            r.run();
        }
    }
    
    public void addPlayerHpDecrease(Character chr) {
        playerHpDec.putIfAbsent(chr, 0);
    }
    
    public void removePlayerHpDecrease(Character chr) {
        playerHpDec.remove(chr);
    }
    
    public void runPlayerHpDecreaseSchedule() {
        Map<Character, Integer> m = new HashMap<>();
        m.putAll(playerHpDec);
        
        for (Entry<Character, Integer> e : m.entrySet()) {
            Character chr = e.getKey();
            
            if (!chr.isAwayFromWorld()) {
                int c = e.getValue();
                c = (c + 1) % YamlConfig.config.server.MAP_DAMAGE_OVERTIME_COUNT;
                playerHpDec.replace(chr, c);

                if (c == 0) {
                    chr.doHurtHp();
                }
            }
        }
    }

    public void resetDisabledServerMessages() {
        srvMessagesLock.lock();
        try {
            disabledServerMessages.clear();
        } finally {
            srvMessagesLock.unlock();
        }
    }

    public boolean registerDisabledServerMessage(int chrid) {
        srvMessagesLock.lock();
        try {
            boolean alreadyDisabled = disabledServerMessages.containsKey(chrid);
            disabledServerMessages.put(chrid, 0);

            return alreadyDisabled;
        } finally {
            srvMessagesLock.unlock();
        }
    }

    public boolean unregisterDisabledServerMessage(int chrid) {
        srvMessagesLock.lock();
        try {
            return disabledServerMessages.remove(chrid) != null;
        } finally {
            srvMessagesLock.unlock();
        }
    }

    public void runDisabledServerMessagesSchedule() {
        List<Integer> toRemove = new LinkedList<>();

        srvMessagesLock.lock();
        try {
            for (Entry<Integer, Integer> dsm : disabledServerMessages.entrySet()) {
                int b = dsm.getValue();
                if (b >= 4) {   // ~35sec duration, 10sec update
                    toRemove.add(dsm.getKey());
                } else {
                    disabledServerMessages.put(dsm.getKey(), ++b);
                }
            }

            for (Integer chrid : toRemove) {
                disabledServerMessages.remove(chrid);
            }
        } finally {
            srvMessagesLock.unlock();
        }

        if (!toRemove.isEmpty()) {
            for (Integer chrid : toRemove) {
                Character chr = players.getCharacterById(chrid);

                if (chr != null && chr.isLoggedinWorld()) {
                    chr.sendPacket(PacketCreator.serverMessage(chr.getClient().getChannelServer().getServerMessage()));
                }
            }
        }
    }

    public void setPlayerNpcMapStep(int mapid, int step) {
        setPlayerNpcMapData(mapid, step, -1, false);
    }

    public void setPlayerNpcMapPodiumData(int mapid, int podium) {
        setPlayerNpcMapData(mapid, -1, podium, false);
    }

    public void setPlayerNpcMapData(int mapid, int step, int podium) {
        setPlayerNpcMapData(mapid, step, podium, true);
    }

    private static void executePlayerNpcMapDataUpdate(Connection con, boolean isPodium, Map<Integer, ?> pnpcData, int value, int worldid, int mapid) throws SQLException {
        final String query;
        if (pnpcData.containsKey(mapid)) {
            query = "UPDATE playernpcs_field SET " + (isPodium ? "podium" : "step") + " = ? WHERE world = ? AND map = ?";
        } else {
            query = "INSERT INTO playernpcs_field (" + (isPodium ? "podium" : "step") + ", world, map) VALUES (?, ?, ?)";
        }

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, value);
            ps.setInt(2, worldid);
            ps.setInt(3, mapid);
            ps.executeUpdate();
        }
    }

    private void setPlayerNpcMapData(int mapid, int step, int podium, boolean silent) {
        if (!silent) {
            try (Connection con = DatabaseConnection.getConnection()) {
                if (step != -1) {
                    executePlayerNpcMapDataUpdate(con, false, pnpcStep, step, id, mapid);
                }

                if (podium != -1) {
                    executePlayerNpcMapDataUpdate(con, true, pnpcPodium, podium, id, mapid);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (step != -1) {
            pnpcStep.put(mapid, (byte) step);
        }
        if (podium != -1) {
            pnpcPodium.put(mapid, (short) podium);
        }
    }

    public int getPlayerNpcMapStep(int mapid) {
        try {
            return pnpcStep.get(mapid);
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    public int getPlayerNpcMapPodiumData(int mapid) {
        try {
            return pnpcPodium.get(mapid);
        } catch (NullPointerException npe) {
            return 1;
        }
    }

    public void resetPlayerNpcMapData() {
        pnpcStep.clear();
        pnpcPodium.clear();
    }

    public void setServerMessage(String msg) {
        for (Channel ch : getChannels()) {
            ch.setServerMessage(msg);
        }
    }

    public void broadcastPacket(Packet packet) {
        for (Character chr : players.getAllCharacters()) {
            chr.sendPacket(packet);
        }
    }

    public List<Pair<PlayerShopItem, AbstractMapObject>> getAvailableItemBundles(int itemid) {
        List<Pair<PlayerShopItem, AbstractMapObject>> hmsAvailable = new ArrayList<>();

        for (HiredMerchant hm : getActiveMerchants()) {
            List<PlayerShopItem> itemBundles = hm.sendAvailableBundles(itemid);

            for (PlayerShopItem mpsi : itemBundles) {
                hmsAvailable.add(new Pair<>(mpsi, hm));
            }
        }

        for (PlayerShop ps : getActivePlayerShops()) {
            List<PlayerShopItem> itemBundles = ps.sendAvailableBundles(itemid);

            for (PlayerShopItem mpsi : itemBundles) {
                hmsAvailable.add(new Pair<>(mpsi, ps));
            }
        }

        hmsAvailable.sort((p1, p2) -> p1.getLeft().getPrice() - p2.getLeft().getPrice());

        hmsAvailable.subList(0, Math.min(hmsAvailable.size(), 200));    //truncates the list to have up to 200 elements
        return hmsAvailable;
    }

    private void pushRelationshipCouple(Pair<Integer, Pair<Integer, Integer>> couple) {
        int mid = couple.getLeft(), hid = couple.getRight().getLeft(), wid = couple.getRight().getRight();
        relationshipCouples.put(mid, couple.getRight());
        relationships.put(hid, mid);
        relationships.put(wid, mid);
    }

    public Pair<Integer, Integer> getRelationshipCouple(int relationshipId) {
        Pair<Integer, Integer> rc = relationshipCouples.get(relationshipId);

        if (rc == null) {
            Pair<Integer, Pair<Integer, Integer>> couple = getRelationshipCoupleFromDb(relationshipId, true);
            if (couple == null) {
                return null;
            }

            pushRelationshipCouple(couple);
            rc = couple.getRight();
        }

        return rc;
    }

    public int getRelationshipId(int playerId) {
        Integer ret = relationships.get(playerId);

        if (ret == null) {
            Pair<Integer, Pair<Integer, Integer>> couple = getRelationshipCoupleFromDb(playerId, false);
            if (couple == null) {
                return -1;
            }

            pushRelationshipCouple(couple);
            ret = couple.getLeft();
        }

        return ret;
    }

    private static Pair<Integer, Pair<Integer, Integer>> getRelationshipCoupleFromDb(int id, boolean usingMarriageId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            Integer mid = null, hid = null, wid = null;

            PreparedStatement ps;
            if (usingMarriageId) {
                ps = con.prepareStatement("SELECT * FROM marriages WHERE marriageid = ?");
                ps.setInt(1, id);
            } else {
                ps = con.prepareStatement("SELECT * FROM marriages WHERE husbandid = ? OR wifeid = ?");
                ps.setInt(1, id);
                ps.setInt(2, id);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    mid = rs.getInt("marriageid");
                    hid = rs.getInt("husbandid");
                    wid = rs.getInt("wifeid");
                }
            }

            ps.close();

            return (mid == null) ? null : new Pair<>(mid, new Pair<>(hid, wid));
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }

    public int createRelationship(int groomId, int brideId) {
        int ret = addRelationshipToDb(groomId, brideId);

        pushRelationshipCouple(new Pair<>(ret, new Pair<>(groomId, brideId)));
        return ret;
    }

    private static int addRelationshipToDb(int groomId, int brideId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO marriages (husbandid, wifeid) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, groomId);
            ps.setInt(2, brideId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                int ret = rs.getInt(1);
                return ret;
            }
        } catch (SQLException se) {
            se.printStackTrace();
            return -1;
        }
    }

    public void deleteRelationship(int playerId, int partnerId) {
        int relationshipId = relationships.get(playerId);
        deleteRelationshipFromDb(relationshipId);

        relationshipCouples.remove(relationshipId);
        relationships.remove(playerId);
        relationships.remove(partnerId);
    }

    private static void deleteRelationshipFromDb(int playerId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM marriages WHERE marriageid = ?")) {
            ps.setInt(1, playerId);
            ps.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public void dropMessage(int type, String message) {
        for (Character player : getPlayerStorage().getAllCharacters()) {
            player.dropMessage(type, message);
        }
    }

    public boolean registerFisherPlayer(Character chr, int baitLevel) {
        synchronized (fishingAttempters) {
            if (fishingAttempters.containsKey(chr)) {
                return false;
            }

            fishingAttempters.put(chr, baitLevel);
            return true;
        }
    }

    public int unregisterFisherPlayer(Character chr) {
        Integer baitLevel = fishingAttempters.remove(chr);
        if (baitLevel != null) {
            return baitLevel;
        } else {
            return 0;
        }
    }

    public void runCheckFishingSchedule() {
        double[] fishingLikelihoods = Fishing.fetchFishingLikelihood();
        double yearLikelihood = fishingLikelihoods[0], timeLikelihood = fishingLikelihoods[1];

        if (!fishingAttempters.isEmpty()) {
            List<Character> fishingAttemptersList;

            synchronized (fishingAttempters) {
                fishingAttemptersList = new ArrayList<>(fishingAttempters.keySet());
            }

            for (Character chr : fishingAttemptersList) {
                int baitLevel = unregisterFisherPlayer(chr);
                Fishing.doFishing(chr, baitLevel, yearLikelihood, timeLikelihood);
            }
        }
    }

    public void runPartySearchUpdateSchedule() {
        partySearch.updatePartySearchStorage();
        partySearch.runPartySearch();
    }

    public BaseService getServiceAccess(WorldServices sv) {
        return services.getAccess(sv).getService();
    }

    private void closeWorldServices() {
        services.shutdown();
    }

    private void clearWorldData() {
        List<Party> pList;
        partyLock.lock();
        try {
            pList = new ArrayList<>(parties.values());
        } finally {
            partyLock.unlock();
        }

        closeWorldServices();
    }

    public final void shutdown() {
        for (Channel ch : getChannels()) {
            ch.shutdown();
        }

        if (petsSchedule != null) {
            petsSchedule.cancel(false);
            petsSchedule = null;
        }

        if (srvMessagesSchedule != null) {
            srvMessagesSchedule.cancel(false);
            srvMessagesSchedule = null;
        }

        if (mountsSchedule != null) {
            mountsSchedule.cancel(false);
            mountsSchedule = null;
        }

        if (merchantSchedule != null) {
            merchantSchedule.cancel(false);
            merchantSchedule = null;
        }

        if (timedMapObjectsSchedule != null) {
            timedMapObjectsSchedule.cancel(false);
            timedMapObjectsSchedule = null;
        }

        if (marriagesSchedule != null) {
            marriagesSchedule.cancel(false);
            marriagesSchedule = null;
        }

        if (mapOwnershipSchedule != null) {
            mapOwnershipSchedule.cancel(false);
            mapOwnershipSchedule = null;
        }

        if (fishingSchedule != null) {
            fishingSchedule.cancel(false);
            fishingSchedule = null;
        }

        if (partySearchSchedule != null) {
            partySearchSchedule.cancel(false);
            partySearchSchedule = null;
        }

        if (timeoutSchedule != null) {
            timeoutSchedule.cancel(false);
            timeoutSchedule = null;
        }
        
        if(hpDecSchedule != null) {
            hpDecSchedule.cancel(false);
            hpDecSchedule = null;
        }

        players.disconnectAll();
        players = null;

        clearWorldData();
        log.info("Finished shutting down world {}", id);
    }
}
