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
package server.maps;

import client.BuffStat;
import client.Character;
import client.Client;
import client.autoban.AutobanFactory;
import client.inventory.Equip;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.Pet;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.id.MapId;
import constants.id.MobId;
import constants.inventory.ItemConstants;
import database.drop.DropProvider;
import net.packet.Packet;
import net.server.Server;
import net.server.channel.Channel;
import net.server.coordinator.world.MonsterAggroCoordinator;
import net.server.services.task.channel.MobMistService;
import net.server.services.task.channel.OverallService;
import net.server.services.type.ChannelServices;
import net.server.world.Party;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.event.EventInstanceManager;
import scripting.map.MapScriptManager;
import server.ItemInformationProvider;
import server.StatEffect;
import server.TimerManager;
import server.events.gm.Coconut;
import server.events.gm.Fitness;
import server.events.gm.Ola;
import server.events.gm.OxQuiz;
import server.events.gm.Snowball;
import server.life.LifeFactory;
import server.life.LifeFactory.selfDestruction;
import server.life.Monster;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;
import server.life.MonsterInformationProvider;
import server.life.MonsterListener;
import server.life.NPC;
import server.life.PlayerNPC;
import server.life.SpawnPoint;
import server.partyquest.CarnivalFactory;
import server.partyquest.CarnivalFactory.MCSkill;
import server.partyquest.GuardianSpawnPoint;
import tools.PacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class MapleMap {
    private static final Logger log = LoggerFactory.getLogger(MapleMap.class);
    private static final List<MapObjectType> rangedMapobjectTypes = Arrays.asList(MapObjectType.SHOP, MapObjectType.ITEM, MapObjectType.NPC, MapObjectType.MONSTER, MapObjectType.DOOR, MapObjectType.SUMMON, MapObjectType.REACTOR);
    private static final Map<Integer, Pair<Integer, Integer>> dropBoundsCache = new HashMap<>(100);

    private final Map<Integer, MapObject> mapobjects = new LinkedHashMap<>();
    private final Set<Integer> selfDestructives = new LinkedHashSet<>();
    private final Collection<SpawnPoint> monsterSpawn = Collections.synchronizedList(new LinkedList<>());
    private final Collection<SpawnPoint> allMonsterSpawn = Collections.synchronizedList(new LinkedList<>());
    private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private final AtomicInteger droppedItemCount = new AtomicInteger(0);
    private final Collection<Character> characters = new LinkedHashSet<>();
    private final Map<Integer, Set<Integer>> mapParty = new LinkedHashMap<>();
    private final Map<Integer, Portal> portals = new HashMap<>();
    private final Map<Integer, Integer> backgroundTypes = new HashMap<>();
    private final Map<String, Integer> environment = new LinkedHashMap<>();
    private final Map<MapItem, Long> droppedItems = new LinkedHashMap<>();
    private final LinkedList<WeakReference<MapObject>> registeredDrops = new LinkedList<>();
    private final List<Runnable> statUpdateRunnables = new ArrayList(50);
    private final List<Rectangle> areas = new ArrayList<>();
    private FootholdTree footholds = null;
    private Pair<Integer, Integer> xLimits;  // caches the min and max x's with available footholds
    private final Rectangle mapArea = new Rectangle();
    private final int mapid;
    private final AtomicInteger runningOid = new AtomicInteger(1000000001);
    private final int returnMapId;
    private final int channel;
    private final int world;
    private int seats;
    private byte monsterRate;
    private final DropProvider dropProvider;
    private boolean clock;
    private boolean boat;
    private boolean docked = false;
    private EventInstanceManager event = null;
    private String mapName;
    private String streetName;
    private MapEffect mapEffect = null;
    private boolean everlast = false;
    private int forcedReturnMap = MapId.NONE;
    private int timeLimit;
    private long mapTimer;
    private int decHP = 0;
    private float recovery = 1.0f;
    private int protectItem = 0;
    private boolean town;
    private OxQuiz ox;
    private boolean isOxQuiz = false;
    private boolean dropsOn = true;
    private String onFirstUserEnter;
    private String onUserEnter;
    private int fieldType;
    private int fieldLimit = 0;
    private int mobCapacity = -1;
    private MonsterAggroCoordinator aggroMonitor = null;   // aggroMonitor activity in sync with itemMonitor
    private ScheduledFuture<?> itemMonitor = null;
    private ScheduledFuture<?> expireItemsTask = null;
    private ScheduledFuture<?> characterStatUpdateTask = null;
    private short itemMonitorTimeout;
    private Pair<Integer, String> timeMob = null;
    private short mobInterval = 5000;
    private boolean allowSummons = true; // All maps should have this true at the beginning
    private Character mapOwner = null;
    private long mapOwnerLastActivityTime = Long.MAX_VALUE;

    // events
    private boolean eventstarted = false, isMuted = false;
    private Snowball snowball0 = null;
    private Snowball snowball1 = null;
    private Coconut coconut;

    //CPQ
    private int maxMobs;
    private int maxReactors;
    private int deathCP;
    private int timeDefault;
    private int timeExpand;

    //locks
    private final Lock chrRLock;
    private final Lock chrWLock;
    private final Lock objectRLock;
    private final Lock objectWLock;

    private final Lock lootLock = new ReentrantLock(true);

    // due to the nature of loadMapFromWz (synchronized), sole function that calls 'generateMapDropRangeCache', this lock remains optional.
    private static final Lock bndLock = new ReentrantLock(true);

    public MapleMap(int mapid, int world, int channel, int returnMapId, float monsterRate, DropProvider dropProvider) {
        this.mapid = mapid;
        this.channel = channel;
        this.world = world;
        this.returnMapId = returnMapId;
        this.monsterRate = (byte) Math.ceil(monsterRate);
        if (this.monsterRate == 0) {
            this.monsterRate = 1;
        }
        this.dropProvider = dropProvider;

        final ReadWriteLock chrLock = new ReentrantReadWriteLock(true);
        chrRLock = chrLock.readLock();
        chrWLock = chrLock.writeLock();

        final ReadWriteLock objectLock = new ReentrantReadWriteLock(true);
        objectRLock = objectLock.readLock();
        objectWLock = objectLock.writeLock();

        aggroMonitor = new MonsterAggroCoordinator();
    }

    public void setEventInstance(EventInstanceManager eim) {
        event = eim;
    }

    public EventInstanceManager getEventInstance() {
        return event;
    }

    public Rectangle getMapArea() {
        return mapArea;
    }

    public int getWorld() {
        return world;
    }

    public void broadcastPacket(Character source, Packet packet) {
        broadcastPacket(packet, chr -> chr != source);
    }

    public void broadcastGMPacket(Character source, Packet packet) {
        broadcastPacket(packet, chr -> chr != source && chr.gmLevel() >= source.gmLevel());
    }

    private void broadcastPacket(Packet packet, Predicate<Character> chrFilter) {
        chrRLock.lock();
        try {
            characters.stream()
                    .filter(chrFilter)
                    .forEach(chr -> chr.sendPacket(packet));
        } finally {
            chrRLock.unlock();
        }
    }

    public void toggleDrops() {
        this.dropsOn = !dropsOn;
    }

    private static double getRangedDistance() {
        return YamlConfig.config.server.USE_MAXRANGE ? Double.POSITIVE_INFINITY : 722500;
    }

    public List<MapObject> getMapObjectsInRect(Rectangle box, List<MapObjectType> types) {
        objectRLock.lock();
        final List<MapObject> ret = new LinkedList<>();
        try {
            for (MapObject l : mapobjects.values()) {
                if (types.contains(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return ret;
    }

    public int getId() {
        return mapid;
    }

    public Channel getChannelServer() {
        return Server.getInstance().getWorld(world).getChannel(channel);
    }

    public World getWorldServer() {
        return Server.getInstance().getWorld(world);
    }

    public MapleMap getReturnMap() {
        if (returnMapId == MapId.NONE) {
            return this;
        }
        return getChannelServer().getMapFactory().getMap(returnMapId);
    }

    public int getReturnMapId() {
        return returnMapId;
    }

    public MapleMap getForcedReturnMap() {
        return getChannelServer().getMapFactory().getMap(forcedReturnMap);
    }

    public int getForcedReturnId() {
        return forcedReturnMap;
    }

    public void setForcedReturnMap(int map) {
        this.forcedReturnMap = map;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getTimeLeft() {
        return (int) ((mapTimer - System.currentTimeMillis()) / 1000);
    }

    public void setReactorState() {
        for (MapObject o : getMapObjects()) {
            if (o.getType() == MapObjectType.REACTOR) {
                if (((Reactor) o).getState() < 1) {
                    Reactor mr = (Reactor) o;
                    mr.lockReactor();
                    try {
                        mr.resetReactorActions(1);
                        broadcastMessage(PacketCreator.triggerReactor((Reactor) o, 1));
                    } finally {
                        mr.unlockReactor();
                    }
                }
            }
        }
    }

    public final void limitReactor(final int rid, final int num) {
        List<Reactor> toDestroy = new ArrayList<>();
        Map<Integer, Integer> contained = new LinkedHashMap<>();

        for (MapObject obj : getReactors()) {
            Reactor mr = (Reactor) obj;
            if (contained.containsKey(mr.getId())) {
                if (contained.get(mr.getId()) >= num) {
                    toDestroy.add(mr);
                } else {
                    contained.put(mr.getId(), contained.get(mr.getId()) + 1);
                }
            } else {
                contained.put(mr.getId(), 1);
            }
        }

        for (Reactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public boolean isAllReactorState(final int reactorId, final int state) {
        for (MapObject mo : getReactors()) {
            Reactor r = (Reactor) mo;

            if (r.getId() == reactorId && r.getState() != state) {
                return false;
            }
        }
        return true;
    }

    public int getCurrentPartyId() {
        for (Character chr : this.getCharacters()) {
            if (chr.getPartyId() != -1) {
                return chr.getPartyId();
            }
        }
        return -1;
    }

    public void addPlayerNPCMapObject(PlayerNPC pnpcobject) {
        objectWLock.lock();
        try {
            this.mapobjects.put(pnpcobject.getObjectId(), pnpcobject);
        } finally {
            objectWLock.unlock();
        }
    }

    public void addMapObject(MapObject mapobject) {
        int curOID = getUsableOID();

        objectWLock.lock();
        try {
            mapobject.setObjectId(curOID);
            this.mapobjects.put(curOID, mapobject);
        } finally {
            objectWLock.unlock();
        }
    }

    public void addSelfDestructive(Monster mob) {
        if (mob.getStats().selfDestruction() != null) {
            this.selfDestructives.add(mob.getObjectId());
        }
    }

    public boolean removeSelfDestructive(int mapobjectid) {
        return this.selfDestructives.remove(mapobjectid);
    }

    private void spawnAndAddRangedMapObject(MapObject mapobject, DelayedPacketCreation packetbakery) {
        spawnAndAddRangedMapObject(mapobject, packetbakery, null);
    }

    private void spawnAndAddRangedMapObject(MapObject mapobject, DelayedPacketCreation packetbakery, SpawnCondition condition) {
        List<Character> inRangeCharacters = new LinkedList<>();
        int curOID = getUsableOID();

        chrRLock.lock();
        objectWLock.lock();
        try {
            mapobject.setObjectId(curOID);
            this.mapobjects.put(curOID, mapobject);
            for (Character chr : characters) {
                if (condition == null || condition.canSpawn(chr)) {
                    if (chr.getPosition().distanceSq(mapobject.getPosition()) <= getRangedDistance()) {
                        inRangeCharacters.add(chr);
                        chr.addVisibleMapObject(mapobject);
                    }
                }
            }
        } finally {
            objectWLock.unlock();
            chrRLock.unlock();
        }

        for (Character chr : inRangeCharacters) {
            packetbakery.sendPackets(chr.getClient());
        }
    }

    private void spawnRangedMapObject(MapObject mapobject, DelayedPacketCreation packetbakery, SpawnCondition condition) {
        List<Character> inRangeCharacters = new LinkedList<>();

        chrRLock.lock();
        try {
            int curOID = getUsableOID();
            mapobject.setObjectId(curOID);
            for (Character chr : characters) {
                if (condition == null || condition.canSpawn(chr)) {
                    if (chr.getPosition().distanceSq(mapobject.getPosition()) <= getRangedDistance()) {
                        inRangeCharacters.add(chr);
                        chr.addVisibleMapObject(mapobject);
                    }
                }
            }
        } finally {
            chrRLock.unlock();
        }

        for (Character chr : inRangeCharacters) {
            packetbakery.sendPackets(chr.getClient());
        }
    }

    private int getUsableOID() {
        objectRLock.lock();
        try {
            int curOid;

            // clashes with playernpc on curOid >= 2147000000, developernpc uses >= 2147483000
            do {
                if ((curOid = runningOid.incrementAndGet()) >= 2147000000) {
                    runningOid.set(curOid = 1000000001);
                }
            } while (mapobjects.containsKey(curOid));

            return curOid;
        } finally {
            objectRLock.unlock();
        }
    }

    public void removeMapObject(int num) {
        objectWLock.lock();
        try {
            this.mapobjects.remove(num);
        } finally {
            objectWLock.unlock();
        }
    }

    public void removeMapObject(final MapObject obj) {
        removeMapObject(obj.getObjectId());
    }

    private Point calcPointBelow(Point initial) {
        Foothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            double s1 = Math.abs(fh.getY2() - fh.getY1());
            double s2 = Math.abs(fh.getX2() - fh.getX1());
            double s5 = Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2)));
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) s5;
            } else {
                dropY = fh.getY1() + (int) s5;
            }
        }
        return new Point(initial.x, dropY);
    }

    public void generateMapDropRangeCache() {
        bndLock.lock();
        try {
            Pair<Integer, Integer> bounds = dropBoundsCache.get(mapid);

            if (bounds != null) {
                xLimits = bounds;
            } else {
                // assuming MINIMAP always have an equal-greater picture representation of the map area (players won't walk beyond the area known by the minimap).
                Point lp = new Point(mapArea.x, mapArea.y);
                Point rp = new Point(mapArea.x + mapArea.width, mapArea.y);
                Point fallback = new Point(mapArea.x + (mapArea.width / 2), mapArea.y);

                lp = bsearchDropPos(lp, fallback);  // approximated leftmost fh node position
                rp = bsearchDropPos(rp, fallback);  // approximated rightmost fh node position

                xLimits = new Pair<>(lp.x + 14, rp.x - 14);
                dropBoundsCache.put(mapid, xLimits);
            }
        } finally {
            bndLock.unlock();
        }
    }

    private Point bsearchDropPos(Point initial, Point fallback) {
        Point res, dropPos = null;

        int awayx = fallback.x;
        int homex = initial.x;

        int y = initial.y - 85;

        do {
            int distx = awayx - homex;
            int dx = distx / 2;

            int searchx = homex + dx;
            if ((res = calcPointBelow(new Point(searchx, y))) != null) {
                awayx = searchx;
                dropPos = res;
            } else {
                homex = searchx;
            }
        } while (Math.abs(homex - awayx) > 5);

        return (dropPos != null) ? dropPos : fallback;
    }

    public Point calcDropPos(Point initial, Point fallback) {
        if (initial.x < xLimits.left) {
            initial.x = xLimits.left;
        } else if (initial.x > xLimits.right) {
            initial.x = xLimits.right;
        }

        Point ret = calcPointBelow(new Point(initial.x, initial.y - 85));   // actual drop ranges: default - 120, explosive - 360
        if (ret == null) {
            ret = bsearchDropPos(initial, fallback);
        }

        if (!mapArea.contains(ret)) { // found drop pos outside the map :O
            return fallback;
        }

        return ret;
    }

    public boolean canDeployDoor(Point pos) {
        Point toStep = calcPointBelow(pos);
        return toStep != null && toStep.distance(pos) <= 42;
    }

    /**
     * Fetches angle relative between spawn and door points where 3 O'Clock is 0
     * and 12 O'Clock is 270 degrees
     *
     * @param spawnPoint
     * @param doorPoint
     * @return angle in degress from 0-360.
     */
    private static double getAngle(Point doorPoint, Point spawnPoint) {
        double dx = doorPoint.getX() - spawnPoint.getX();
        // Minus to correct for coord re-mapping
        double dy = -(doorPoint.getY() - spawnPoint.getY());

        double inRads = Math.atan2(dy, dx);

        // We need to map to coord system when 0 degree is at 3 O'clock, 270 at 12 O'clock
        if (inRads < 0) {
            inRads = Math.abs(inRads);
        } else {
            inRads = 2 * Math.PI - inRads;
        }

        return Math.toDegrees(inRads);
    }

    /**
     * Converts angle in degrees to rounded cardinal coordinate.
     *
     * @param angle
     * @return correspondent coordinate.
     */
    public static String getRoundedCoordinate(double angle) {
        String[] directions = {"E", "SE", "S", "SW", "W", "NW", "N", "NE", "E"};
        return directions[(int) Math.round(((angle % 360) / 45))];
    }

    public Pair<String, Integer> getDoorPositionStatus(Point pos) {
        Portal portal = findClosestPlayerSpawnpoint(pos);

        double angle = getAngle(portal.getPosition(), pos);
        double distn = pos.distanceSq(portal.getPosition());

        if (distn <= 777777.7) {
            return null;
        }

        distn = Math.sqrt(distn);
        return new Pair<>(getRoundedCoordinate(angle), (int) distn);
    }

    private static void sortDropEntries(List<MonsterDropEntry> from, List<MonsterDropEntry> item, List<MonsterDropEntry> visibleQuest, List<MonsterDropEntry> otherQuest, Character chr) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        for (MonsterDropEntry mde : from) {
            if (!ii.isQuestItem(mde.itemId)) {
                item.add(mde);
            } else {
                if (chr.needQuestItem(mde.questid, mde.itemId)) {
                    visibleQuest.add(mde);
                } else {
                    otherQuest.add(mde);
                }
            }
        }
    }

    private byte dropItemsFromMonsterOnMap(List<MonsterDropEntry> dropEntry, Point pos, byte index, int chRate,
                                           byte droptype, int mobpos, Character chr, Monster mob, short delay) {
        if (dropEntry.isEmpty()) {
            return index;
        }

        Collections.shuffle(dropEntry);

        Item idrop;
        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        for (final MonsterDropEntry de : dropEntry) {
            float cardRate = chr.getCardRate(de.itemId);
            int dropChance = (int) Math.min((float) de.chance * chRate * cardRate, Integer.MAX_VALUE);

            if (Randomizer.nextInt(999999) < dropChance) {
                if (droptype == 3) {
                    pos.x = mobpos + ((index % 2 == 0) ? (40 * ((index + 1) / 2)) : -(40 * (index / 2)));
                } else {
                    pos.x = mobpos + ((index % 2 == 0) ? (25 * ((index + 1) / 2)) : -(25 * (index / 2)));
                }
                if (de.itemId == 0) { // meso
                    int mesos = Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum;

                    if (mesos > 0) {
                        if (chr.getBuffedValue(BuffStat.MESOUP) != null) {
                            mesos = (int) (mesos * chr.getBuffedValue(BuffStat.MESOUP).doubleValue() / 100.0);
                        }
                        mesos = mesos * chr.getMesoRate();
                        if (mesos <= 0) {
                            mesos = Integer.MAX_VALUE;
                        }

                        spawnMesoDrop(mesos, calcDropPos(pos, mob.getPosition()), mob, chr, false, droptype,
                                delay);
                    }
                } else {
                    if (ItemConstants.getInventoryType(de.itemId) == InventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                    } else {
                        idrop = new Item(de.itemId, (short) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1));
                    }
                    spawnDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.questid, delay);
                }
                index++;
            }
        }

        return index;
    }

    private byte dropGlobalItemsFromMonsterOnMap(List<MonsterGlobalDropEntry> globalEntry, Point pos, byte d,
                                                 byte droptype, int mobpos, Character chr, Monster mob, short delay) {
        Collections.shuffle(globalEntry);

        Item idrop;
        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        for (final MonsterGlobalDropEntry de : globalEntry) {
            if (Randomizer.nextInt(999999) < de.chance) {
                if (droptype == 3) {
                    pos.x = mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2)));
                } else {
                    pos.x = mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2)));
                }
                if (de.itemId != 0) {
                    if (ItemConstants.getInventoryType(de.itemId) == InventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                    } else {
                        idrop = new Item(de.itemId, (short) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1));
                    }
                    spawnDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.questid, delay);
                    d++;
                }
            }
        }

        return d;
    }

    private void dropFromMonster(final Character chr, final Monster mob, final boolean useBaseRate, short delay) {
        if (mob.dropsDisabled() || !dropsOn) {
            return;
        }

        final byte droptype = (byte) (mob.getStats().isExplosiveReward() ? 3 : mob.getStats().isFfaLoot() ? 2 : chr.getParty() != null ? 1 : 0);
        final int mobpos = mob.getPosition().x;
        int chRate = !mob.isBoss() ? chr.getDropRate() : chr.getBossDropRate();
        Point pos = new Point(0, mob.getPosition().y);

        MonsterStatusEffect stati = mob.getStati(MonsterStatus.SHOWDOWN);
        if (stati != null) {
            chRate *= (stati.getStati().get(MonsterStatus.SHOWDOWN).doubleValue() / 100.0 + 1.0);
        }

        if (useBaseRate) {
            chRate = 1;
        }

        final List<MonsterGlobalDropEntry> globalEntry = new ArrayList<>(dropProvider.getRelevantGlobalDrops(this.getId()));

        final List<MonsterDropEntry> dropEntry = new ArrayList<>();
        final List<MonsterDropEntry> visibleQuestEntry = new ArrayList<>();
        final List<MonsterDropEntry> otherQuestEntry = new ArrayList<>();

        List<MonsterDropEntry> lootEntry = YamlConfig.config.server.USE_SPAWN_RELEVANT_LOOT ?
                mob.retrieveRelevantDrops(dropProvider) : dropProvider.getMonsterDropEntries(mob.getId());
        sortDropEntries(lootEntry, dropEntry, visibleQuestEntry, otherQuestEntry, chr);     // thanks Articuno, Limit, Rohenn for noticing quest loots not showing up in only-quest item drops scenario

        if (lootEntry.isEmpty()) {   // thanks resinate
            return;
        }


        byte index = 1;
        // Normal Drops
        index = dropItemsFromMonsterOnMap(dropEntry, pos, index, chRate, droptype, mobpos, chr, mob, delay);

        // Global Drops
        index = dropGlobalItemsFromMonsterOnMap(globalEntry, pos, index, droptype, mobpos, chr, mob, delay);

        // Quest Drops
        index = dropItemsFromMonsterOnMap(visibleQuestEntry, pos, index, chRate, droptype, mobpos, chr, mob, delay);
        dropItemsFromMonsterOnMap(otherQuestEntry, pos, index, chRate, droptype, mobpos, chr, mob, delay);
    }

    public void dropItemsFromMonster(List<MonsterDropEntry> list, final Character chr, final Monster mob, short delay) {
        if (mob.dropsDisabled() || !dropsOn) {
            return;
        }

        final byte droptype = (byte) (chr.getParty() != null ? 1 : 0);
        final int mobpos = mob.getPosition().x;
        int chRate = 1000000;   // guaranteed item drop
        byte d = 1;
        Point pos = new Point(0, mob.getPosition().y);

        dropItemsFromMonsterOnMap(list, pos, d, chRate, droptype, mobpos, chr, mob, delay);
    }

    public void dropFromFriendlyMonster(final Character chr, final Monster mob) {
        dropFromMonster(chr, mob, true, (short) 0);
    }

    public void dropFromReactor(final Character chr, final Reactor reactor, Item drop, Point dropPos, short questid,
                                short delay) {
        spawnDrop(drop, this.calcDropPos(dropPos, reactor.getPosition()), reactor, chr,
                (byte) (chr.getParty() != null ? 1 : 0), questid, delay);
    }

    private void stopItemMonitor() {
        itemMonitor.cancel(false);
        itemMonitor = null;

        expireItemsTask.cancel(false);
        expireItemsTask = null;

        characterStatUpdateTask.cancel(false);
        characterStatUpdateTask = null;
    }

    private void cleanItemMonitor() {
        objectWLock.lock();
        try {
            registeredDrops.removeAll(Collections.singleton(null));
        } finally {
            objectWLock.unlock();
        }
    }

    private void startItemMonitor() {
        chrWLock.lock();
        try {
            if (itemMonitor != null) {
                return;
            }

            itemMonitor = TimerManager.getInstance().register(() -> {
                chrWLock.lock();
                try {
                    if (characters.isEmpty()) {
                        if (itemMonitorTimeout == 0) {
                            if (itemMonitor != null) {
                                stopItemMonitor();
                                aggroMonitor.stopAggroCoordinator();
                            }

                            return;
                        } else {
                            itemMonitorTimeout--;
                        }
                    } else {
                        itemMonitorTimeout = 1;
                    }
                } finally {
                    chrWLock.unlock();
                }

                boolean tryClean;
                objectRLock.lock();
                try {
                    tryClean = registeredDrops.size() > 70;
                } finally {
                    objectRLock.unlock();
                }

                if (tryClean) {
                    cleanItemMonitor();
                }
            }, YamlConfig.config.server.ITEM_MONITOR_TIME, YamlConfig.config.server.ITEM_MONITOR_TIME);

            expireItemsTask = TimerManager.getInstance().register(() -> makeDisappearExpiredItemDrops(), YamlConfig.config.server.ITEM_EXPIRE_CHECK, YamlConfig.config.server.ITEM_EXPIRE_CHECK);

            characterStatUpdateTask = TimerManager.getInstance().register(() -> runCharacterStatUpdate(), 200, 200);

            itemMonitorTimeout = 1;
        } finally {
            chrWLock.unlock();
        }
    }

    private boolean hasItemMonitor() {
        chrRLock.lock();
        try {
            return itemMonitor != null;
        } finally {
            chrRLock.unlock();
        }
    }

    public int getDroppedItemCount() {
        return droppedItemCount.get();
    }

    private void instantiateItemDrop(MapItem mdrop) {
        if (droppedItemCount.get() >= YamlConfig.config.server.ITEM_LIMIT_ON_MAP) {
            MapObject mapobj;

            do {
                mapobj = null;

                objectWLock.lock();
                try {
                    while (mapobj == null) {
                        if (registeredDrops.isEmpty()) {
                            break;
                        }
                        mapobj = registeredDrops.remove(0).get();
                    }
                } finally {
                    objectWLock.unlock();
                }
            } while (!makeDisappearItemFromMap(mapobj));
        }

        objectWLock.lock();
        try {
            registerItemDrop(mdrop);
            registeredDrops.add(new WeakReference<>(mdrop));
        } finally {
            objectWLock.unlock();
        }

        droppedItemCount.incrementAndGet();
    }

    private void registerItemDrop(MapItem mdrop) {
        droppedItems.put(mdrop, !everlast ? Server.getInstance().getCurrentTime() + YamlConfig.config.server.ITEM_EXPIRE_TIME : Long.MAX_VALUE);
    }

    private void unregisterItemDrop(MapItem mdrop) {
        objectWLock.lock();
        try {
            droppedItems.remove(mdrop);
        } finally {
            objectWLock.unlock();
        }
    }

    private void makeDisappearExpiredItemDrops() {
        List<MapItem> toDisappear = new LinkedList<>();

        objectRLock.lock();
        try {
            long timeNow = Server.getInstance().getCurrentTime();

            for (Entry<MapItem, Long> it : droppedItems.entrySet()) {
                if (it.getValue() < timeNow) {
                    toDisappear.add(it.getKey());
                }
            }
        } finally {
            objectRLock.unlock();
        }

        for (MapItem mmi : toDisappear) {
            makeDisappearItemFromMap(mmi);
        }

        objectWLock.lock();
        try {
            for (MapItem mmi : toDisappear) {
                droppedItems.remove(mmi);
            }
        } finally {
            objectWLock.unlock();
        }
    }

    private List<MapItem> getDroppedItems() {
        objectRLock.lock();
        try {
            return new LinkedList<>(droppedItems.keySet());
        } finally {
            objectRLock.unlock();
        }
    }

    public int getDroppedItemsCountById(int itemid) {
        int count = 0;
        for (MapItem mmi : getDroppedItems()) {
            if (mmi.getItemId() == itemid) {
                count++;
            }
        }

        return count;
    }

    public void pickItemDrop(Packet pickupPacket, MapItem mdrop) { // mdrop must be already locked and not-pickedup checked at this point
        broadcastMessage(pickupPacket, mdrop.getPosition());

        droppedItemCount.decrementAndGet();
        this.removeMapObject(mdrop);
        mdrop.setPickedUp(true);
        unregisterItemDrop(mdrop);
    }

    public List<MapItem> updatePlayerItemDropsToParty(int partyid, int charid, List<Character> partyMembers, Character partyLeaver) {
        List<MapItem> partyDrops = new LinkedList<>();

        for (MapItem mdrop : getDroppedItems()) {
            if (mdrop.getOwnerId() == charid) {
                mdrop.lockItem();
                try {
                    if (mdrop.isPickedUp()) {
                        continue;
                    }

                    mdrop.setPartyOwnerId(partyid);

                    Packet removePacket = PacketCreator.silentRemoveItemFromMap(mdrop.getObjectId());
                    Packet updatePacket = PacketCreator.updateMapItemObject(mdrop, partyLeaver == null);

                    for (Character mc : partyMembers) {
                        if (this.equals(mc.getMap())) {
                            mc.sendPacket(removePacket);

                            if (mc.needQuestItem(mdrop.getQuest(), mdrop.getItemId())) {
                                mc.sendPacket(updatePacket);
                            }
                        }
                    }

                    if (partyLeaver != null) {
                        if (this.equals(partyLeaver.getMap())) {
                            partyLeaver.sendPacket(removePacket);

                            if (partyLeaver.needQuestItem(mdrop.getQuest(), mdrop.getItemId())) {
                                partyLeaver.sendPacket(PacketCreator.updateMapItemObject(mdrop, true));
                            }
                        }
                    }
                } finally {
                    mdrop.unlockItem();
                }
            } else if (partyid != -1 && mdrop.getPartyOwnerId() == partyid) {
                partyDrops.add(mdrop);
            }
        }

        return partyDrops;
    }

    public void updatePartyItemDropsToNewcomer(Character newcomer, List<MapItem> partyItems) {
        for (MapItem mdrop : partyItems) {
            mdrop.lockItem();
            try {
                if (mdrop.isPickedUp()) {
                    continue;
                }

                Packet removePacket = PacketCreator.silentRemoveItemFromMap(mdrop.getObjectId());
                Packet updatePacket = PacketCreator.updateMapItemObject(mdrop, true);

                if (newcomer != null) {
                    if (this.equals(newcomer.getMap())) {
                        newcomer.sendPacket(removePacket);

                        if (newcomer.needQuestItem(mdrop.getQuest(), mdrop.getItemId())) {
                            newcomer.sendPacket(updatePacket);
                        }
                    }
                }
            } finally {
                mdrop.unlockItem();
            }
        }
    }

    private void spawnDrop(final Item idrop, final Point dropPos, final MapObject dropper, final Character chr,
                           final byte droptype, final short questid, short delay) {
        final MapItem mdrop = new MapItem(idrop, dropPos, dropper, chr, chr.getClient(), droptype, false, questid);
        mdrop.setDropTime(Server.getInstance().getCurrentTime());
        spawnAndAddRangedMapObject(mdrop, c -> {
            Character chr1 = c.getPlayer();

            if (chr1.needQuestItem(questid, idrop.getItemId())) {
                mdrop.lockItem();
                try {
                    c.sendPacket(PacketCreator.dropItemFromMapObject(chr1, mdrop, dropper.getPosition(), dropPos,
                            (byte) 1, delay));
                } finally {
                    mdrop.unlockItem();
                }
            }
        }, null);

        instantiateItemDrop(mdrop);
        activateItemReactors(mdrop, chr.getClient());
    }

    public final void spawnMesoDrop(final int meso, final Point position, final MapObject dropper,
                                    final Character owner, final boolean playerDrop, final byte droptype, short delay) {
        final Point droppos = calcDropPos(position, position);
        final MapItem mdrop = new MapItem(meso, droppos, dropper, owner, owner.getClient(), droptype, playerDrop);
        mdrop.setDropTime(Server.getInstance().getCurrentTime());

        spawnAndAddRangedMapObject(mdrop, c -> {
            mdrop.lockItem();
            try {
                c.sendPacket(PacketCreator.dropItemFromMapObject(c.getPlayer(), mdrop, dropper.getPosition(), droppos,
                        (byte) 1, delay));
            } finally {
                mdrop.unlockItem();
            }
        }, null);

        instantiateItemDrop(mdrop);
    }

    public final void disappearingItemDrop(final MapObject dropper, final Character owner, final Item item, final Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapItem mdrop = new MapItem(item, droppos, dropper, owner, owner.getClient(), (byte) 1, false);

        mdrop.lockItem();
        try {
            broadcastItemDropMessage(mdrop, dropper.getPosition(), droppos, (byte) 3, (short) 0, mdrop.getPosition());
        } finally {
            mdrop.unlockItem();
        }
    }

    public final void disappearingMesoDrop(final int meso, final MapObject dropper, final Character owner, final Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapItem mdrop = new MapItem(meso, droppos, dropper, owner, owner.getClient(), (byte) 1, false);

        mdrop.lockItem();
        try {
            broadcastItemDropMessage(mdrop, dropper.getPosition(), droppos, (byte) 3, (short) 0, mdrop.getPosition());
        } finally {
            mdrop.unlockItem();
        }
    }

    public Monster getMonsterById(int id) {
        objectRLock.lock();
        try {
            for (MapObject obj : mapobjects.values()) {
                if (obj.getType() == MapObjectType.MONSTER) {
                    if (((Monster) obj).getId() == id) {
                        return (Monster) obj;
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return null;
    }

    public int countMonster(int id) {
        return countMonster(id, id);
    }

    public int countMonster(int minid, int maxid) {
        int count = 0;
        for (MapObject m : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.MONSTER))) {
            Monster mob = (Monster) m;
            if (mob.getId() >= minid && mob.getId() <= maxid) {
                count++;
            }
        }
        return count;
    }

    public int countMonsters() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.MONSTER)).size();
    }

    public int countReactors() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.REACTOR)).size();
    }

    public final List<MapObject> getReactors() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.REACTOR));
    }

    public final List<MapObject> getMonsters() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.MONSTER));
    }

    public final List<Reactor> getAllReactors() {
        List<Reactor> list = new LinkedList<>();
        for (MapObject mmo : getReactors()) {
            list.add((Reactor) mmo);
        }

        return list;
    }

    public final List<Monster> getAllMonsters() {
        List<Monster> list = new LinkedList<>();
        for (MapObject mmo : getMonsters()) {
            list.add((Monster) mmo);
        }

        return list;
    }

    public int countItems() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.ITEM)).size();
    }

    public final List<MapObject> getItems() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.ITEM));
    }

    public int countPlayers() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.PLAYER)).size();
    }

    public List<MapObject> getPlayers() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.PLAYER));
    }

    public List<Character> getAllPlayers() {
        List<Character> character;
        chrRLock.lock();
        try {
            character = new ArrayList<>(characters);
        } finally {
            chrRLock.unlock();
        }

        return character;
    }

    public Map<Integer, Character> getMapAllPlayers() {
        Map<Integer, Character> pchars = new HashMap<>();
        for (Character chr : this.getAllPlayers()) {
            pchars.put(chr.getId(), chr);
        }

        return pchars;
    }

    public List<Character> getPlayersInRange(Rectangle box) {
        List<Character> character = new LinkedList<>();
        chrRLock.lock();
        try {
            for (Character chr : characters) {
                if (box.contains(chr.getPosition())) {
                    character.add(chr);
                }
            }
        } finally {
            chrRLock.unlock();
        }

        return character;
    }

    public int countAlivePlayers() {
        int count = 0;

        for (Character mc : getAllPlayers()) {
            if (mc.isAlive()) {
                count++;
            }
        }

        return count;
    }

    public int countBosses() {
        int count = 0;

        for (Monster mob : getAllMonsters()) {
            if (mob.isBoss()) {
                count++;
            }
        }

        return count;
    }

     public boolean damageMonster(Character chr, Monster monster, int damage) {
        return damageMonster(chr, monster, damage, (short) 0);
     }

    public boolean damageMonster(final Character chr, final Monster monster, final int damage, short delay) {
        if (monster.getId() == MobId.ZAKUM_1) {
            for (MapObject object : chr.getMap().getMapObjects()) {
                Monster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= MobId.ZAKUM_ARM_1 && mons.getId() <= MobId.ZAKUM_ARM_8) {
                        return true;
                    }
                }
            }
        }
        if (!monster.isAlive()) {
            return false;
        }

        boolean killed = monster.damage(chr, damage, false);

        selfDestruction selfDestr = monster.getStats().selfDestruction();
        if (selfDestr != null && selfDestr.getHp() > -1) {// should work ;p
            if (monster.getHp() <= selfDestr.getHp()) {
                killMonster(monster, chr, true, selfDestr.getAction());
                return true;
            }
        }
        if (killed) {
            killMonster(monster, chr, true, delay);
        }
        return true;
    }

    public void broadcastBalrogVictory(String leaderName) {
        getWorldServer().dropMessage(6, "[Victory] " + leaderName + "'s party has successfully defeated the Balrog! Praise to them, they finished with " + countAlivePlayers() + " players alive.");
    }

    public void broadcastHorntailVictory() {
        getWorldServer().dropMessage(6, "[Victory] To the crew that have finally conquered Horned Tail after numerous attempts, I salute thee! You are the true heroes of Leafre!!");
    }

    public void broadcastZakumVictory() {
        getWorldServer().dropMessage(6, "[Victory] At last, the tree of evil that for so long overwhelmed Ossyria has fallen. To the crew that managed to finally conquer Zakum, after numerous attempts, victory! You are the true heroes of Ossyria!!");
    }

    public void broadcastPinkBeanVictory(int channel) {
        getWorldServer().dropMessage(6, "[Victory] In a swift stroke of sorts, the crew that has attempted Pink Bean at channel " + channel + " has ultimately defeated it. The Temple of Time shines radiantly once again, the day finally coming back, as the crew that managed to finally conquer it returns victoriously from the battlefield!!");
    }

    private boolean removeKilledMonsterObject(Monster monster) {
        monster.lockMonster();
        try {
            if (monster.getHp() < 0) {
                return false;
            }

            spawnedMonstersOnMap.decrementAndGet();
            removeMapObject(monster);
            monster.disposeMapObject();
            if (monster.hasBossHPBar()) {   // thanks resinate for noticing boss HPbar not clearing after mob defeat in certain scenarios
                broadcastBossHpMessage(monster, monster.hashCode(), monster.makeBossHPBarPacket(), monster.getPosition());
            }

            return true;
        } finally {
            monster.unlockMonster();
        }
    }

    public void killMonster(final Monster monster, final Character chr, final boolean withDrops, short dropDelay) {
        killMonster(monster, chr, withDrops, 1, dropDelay);
    }

    public void killMonster(final Monster monster, final Character chr, final boolean withDrops, int animation,
                            short dropDelay) {
        if (monster == null) {
            return;
        }

        if (chr == null) {
            if (removeKilledMonsterObject(monster)) {
                monster.dispatchMonsterKilled(false);
                broadcastMessage(PacketCreator.killMonster(monster.getObjectId(), animation), monster.getPosition());
                monster.aggroSwitchController(null, false);
            }
            return;
        }

        if (!removeKilledMonsterObject(monster)) {
            return;
        }

        try {
            if (monster.getStats().getLevel() >= chr.getLevel() + 30 && !chr.isGM()) {
                AutobanFactory.GENERAL.alert(chr, " for killing a " + monster.getName() + " which is over 30 levels higher.");
            }

                    /*if (chr.getQuest(Quest.getInstance(29400)).getStatus().equals(QuestStatus.Status.STARTED)) {
                     if (chr.getLevel() >= 120 && monster.getStats().getLevel() >= 120) {
                     //FIX MEDAL SHET
                     } else if (monster.getStats().getLevel() >= chr.getLevel()) {
                     }
                     }*/

            if (monster.getCP() > 0 && chr.getMap().isCPQMap()) {
                chr.gainCP(monster.getCP());
            }

            int buff = monster.getBuffToGive();
            if (buff > -1) {
                ItemInformationProvider mii = ItemInformationProvider.getInstance();
                for (MapObject mmo : this.getPlayers()) {
                    Character character = (Character) mmo;
                    if (character.isAlive()) {
                        StatEffect statEffect = mii.getItemEffect(buff);
                        character.sendPacket(PacketCreator.showOwnBuffEffect(buff, 1));
                        broadcastMessage(character, PacketCreator.showBuffEffect(character.getId(), buff, 1), false);
                        statEffect.applyTo(character);
                    }
                }
            }

            if (MobId.isZakumArm(monster.getId())) {
                boolean makeZakReal = true;
                Collection<MapObject> objects = getMapObjects();
                for (MapObject object : objects) {
                    Monster mons = getMonsterByOid(object.getObjectId());
                    if (mons != null) {
                        if (MobId.isZakumArm(mons.getId())) {
                            makeZakReal = false;
                            break;
                        }
                    }
                }
                if (makeZakReal) {
                    MapleMap map = chr.getMap();

                    for (MapObject object : objects) {
                        Monster mons = map.getMonsterByOid(object.getObjectId());
                        if (mons != null) {
                            if (mons.getId() == MobId.ZAKUM_1) {
                                makeMonsterReal(mons);
                                break;
                            }
                        }
                    }
                }
            }

            Character dropOwner = monster.killBy(chr);
            if (withDrops && !monster.dropsDisabled()) {
                if (dropOwner == null) {
                    dropOwner = chr;
                }
                dropFromMonster(dropOwner, monster, false, dropDelay);
            }

            if (monster.hasBossHPBar()) {
                for (Character mc : this.getAllPlayers()) {
                    if (mc.getTargetHpBarHash() == monster.hashCode()) {
                        mc.resetPlayerAggro();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {     // thanks resinate for pointing out a memory leak possibly from an exception thrown
            monster.dispatchMonsterKilled(true);
            broadcastMessage(PacketCreator.killMonster(monster.getObjectId(), animation), monster.getPosition());
        }


    }

    public void killFriendlies(Monster mob) {
        this.killMonster(mob, (Character) getPlayers().get(0), false, (short) 0);
    }

    public void killMonster(int mobId) {
        Character chr = (Character) getPlayers().get(0);
        List<Monster> mobList = getAllMonsters();

        for (Monster mob : mobList) {
            if (mob.getId() == mobId) {
                this.killMonster(mob, chr, false, (short) 0);
            }
        }
    }

    public void killMonsterWithDrops(int mobId) {
        Map<Integer, Character> mapChars = this.getMapPlayers();

        if (!mapChars.isEmpty()) {
            Character defaultChr = mapChars.entrySet().iterator().next().getValue();
            List<Monster> mobList = getAllMonsters();

            for (Monster mob : mobList) {
                if (mob.getId() == mobId) {
                    Character chr = mapChars.get(mob.getHighestDamagerId());
                    if (chr == null) {
                        chr = defaultChr;
                    }

                    this.killMonster(mob, chr, true, (short) 0);
                }
            }
        }
    }

    public void softKillAllMonsters() {
        closeMapSpawnPoints();

        for (MapObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.MONSTER))) {
            Monster monster = (Monster) monstermo;
            if (monster.getStats().isFriendly()) {
                continue;
            }

            if (removeKilledMonsterObject(monster)) {
                monster.dispatchMonsterKilled(false);
            }
        }
    }

    public void killAllMonstersNotFriendly() {
        closeMapSpawnPoints();

        for (MapObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.MONSTER))) {
            Monster monster = (Monster) monstermo;
            if (monster.getStats().isFriendly()) {
                continue;
            }

            killMonster(monster, null, false, 1, (short) 0);
        }
    }

    public void killAllMonsters() {
        closeMapSpawnPoints();

        for (MapObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.MONSTER))) {
            Monster monster = (Monster) monstermo;

            killMonster(monster, null, false, 1, (short) 0);
        }
    }

    public final void destroyReactors(final int first, final int last) {
        List<Reactor> toDestroy = new ArrayList<>();
        List<MapObject> reactors = getReactors();

        for (MapObject obj : reactors) {
            Reactor mr = (Reactor) obj;
            if (mr.getId() >= first && mr.getId() <= last) {
                toDestroy.add(mr);
            }
        }

        for (Reactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public void destroyReactor(int oid) {
        final Reactor reactor = getReactorByOid(oid);

        if (reactor != null) {
            if (reactor.destroy()) {
                removeMapObject(reactor);
            }
        }
    }

    public void resetReactors() {
        List<Reactor> list = new ArrayList<>();

        objectRLock.lock();
        try {
            for (MapObject o : mapobjects.values()) {
                if (o.getType() == MapObjectType.REACTOR) {
                    final Reactor r = ((Reactor) o);
                    list.add(r);
                }
            }
        } finally {
            objectRLock.unlock();
        }

        resetReactors(list);
    }

    public final void resetReactors(List<Reactor> list) {
        for (Reactor r : list) {
            if (r.forceDelayedRespawn()) {  // thanks Conrad for suggesting reactor with delay respawning immediately
                continue;
            }

            r.lockReactor();
            try {
                r.resetReactorActions(0);
                r.setAlive(true);
                broadcastMessage(PacketCreator.triggerReactor(r, 0));
            } finally {
                r.unlockReactor();
            }
        }
    }

    public void shuffleReactors() {
        List<Point> points = new ArrayList<>();
        objectRLock.lock();
        try {
            for (MapObject o : mapobjects.values()) {
                if (o.getType() == MapObjectType.REACTOR) {
                    points.add(o.getPosition());
                }
            }
            Collections.shuffle(points);
            for (MapObject o : mapobjects.values()) {
                if (o.getType() == MapObjectType.REACTOR) {
                    o.setPosition(points.remove(points.size() - 1));
                }
            }
        } finally {
            objectRLock.unlock();
        }
    }

    public final void shuffleReactors(int first, int last) {
        List<Point> points = new ArrayList<>();
        List<MapObject> reactors = getReactors();
        List<MapObject> targets = new LinkedList<>();

        for (MapObject obj : reactors) {
            Reactor mr = (Reactor) obj;
            if (mr.getId() >= first && mr.getId() <= last) {
                points.add(mr.getPosition());
                targets.add(obj);
            }
        }
        Collections.shuffle(points);
        for (MapObject obj : targets) {
            Reactor mr = (Reactor) obj;
            mr.setPosition(points.remove(points.size() - 1));
        }
    }

    public final void shuffleReactors(List<Object> list) {
        List<Point> points = new ArrayList<>();
        List<MapObject> listObjects = new ArrayList<>();
        List<MapObject> targets = new LinkedList<>();

        objectRLock.lock();
        try {
            for (Object ob : list) {
                if (ob instanceof MapObject mmo) {

                    if (mapobjects.containsValue(mmo) && mmo.getType() == MapObjectType.REACTOR) {
                        listObjects.add(mmo);
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }

        for (MapObject obj : listObjects) {
            Reactor mr = (Reactor) obj;

            points.add(mr.getPosition());
            targets.add(obj);
        }
        Collections.shuffle(points);
        for (MapObject obj : targets) {
            Reactor mr = (Reactor) obj;
            mr.setPosition(points.remove(points.size() - 1));
        }
    }

    private Map<Integer, MapObject> getCopyMapObjects() {
        objectRLock.lock();
        try {
            return new HashMap<>(mapobjects);
        } finally {
            objectRLock.unlock();
        }
    }

    public List<MapObject> getMapObjects() {
        objectRLock.lock();
        try {
            return new LinkedList(mapobjects.values());
        } finally {
            objectRLock.unlock();
        }
    }

    public NPC getNPCById(int id) {
        for (MapObject obj : getMapObjects()) {
            if (obj.getType() == MapObjectType.NPC) {
                NPC npc = (NPC) obj;
                if (npc.getId() == id) {
                    return npc;
                }
            }
        }

        return null;
    }

    public boolean containsNPC(int npcid) {
        objectRLock.lock();
        try {
            for (MapObject obj : mapobjects.values()) {
                if (obj.getType() == MapObjectType.NPC) {
                    if (((NPC) obj).getId() == npcid) {
                        return true;
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return false;
    }

    public void destroyNPC(int npcid) {     // assumption: there's at most one of the same NPC in a map.
        List<MapObject> npcs = getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.NPC));

        chrRLock.lock();
        objectWLock.lock();
        try {
            for (MapObject obj : npcs) {
                if (((NPC) obj).getId() == npcid) {
                    broadcastMessage(PacketCreator.removeNPCController(obj.getObjectId()));
                    broadcastMessage(PacketCreator.removeNPC(obj.getObjectId()));

                    this.mapobjects.remove(obj.getObjectId());
                }
            }
        } finally {
            objectWLock.unlock();
            chrRLock.unlock();
        }
    }

    public MapObject getMapObject(int oid) {
        objectRLock.lock();
        try {
            return mapobjects.get(oid);
        } finally {
            objectRLock.unlock();
        }
    }

    /**
     * returns a monster with the given oid, if no such monster exists returns
     * null
     *
     * @param oid
     * @return
     */
    public Monster getMonsterByOid(int oid) {
        MapObject mmo = getMapObject(oid);
        return (mmo != null && mmo.getType() == MapObjectType.MONSTER) ? (Monster) mmo : null;
    }

    public Reactor getReactorByOid(int oid) {
        MapObject mmo = getMapObject(oid);
        return (mmo != null && mmo.getType() == MapObjectType.REACTOR) ? (Reactor) mmo : null;
    }

    public Reactor getReactorById(int Id) {
        objectRLock.lock();
        try {
            for (MapObject obj : mapobjects.values()) {
                if (obj.getType() == MapObjectType.REACTOR) {
                    if (((Reactor) obj).getId() == Id) {
                        return (Reactor) obj;
                    }
                }
            }
            return null;
        } finally {
            objectRLock.unlock();
        }
    }

    public List<Reactor> getReactorsByIdRange(final int first, final int last) {
        List<Reactor> list = new LinkedList<>();

        objectRLock.lock();
        try {
            for (MapObject obj : mapobjects.values()) {
                if (obj.getType() == MapObjectType.REACTOR) {
                    Reactor mr = (Reactor) obj;

                    if (mr.getId() >= first && mr.getId() <= last) {
                        list.add(mr);
                    }
                }
            }

            return list;
        } finally {
            objectRLock.unlock();
        }
    }

    public Reactor getReactorByName(String name) {
        objectRLock.lock();
        try {
            for (MapObject obj : mapobjects.values()) {
                if (obj.getType() == MapObjectType.REACTOR) {
                    if (((Reactor) obj).getName().equals(name)) {
                        return (Reactor) obj;
                    }
                }
            }
        } finally {
            objectRLock.unlock();
        }
        return null;
    }

    public void spawnMonsterOnGroundBelow(int id, int x, int y) {
        Monster mob = LifeFactory.getMonster(id);
        spawnMonsterOnGroundBelow(mob, new Point(x, y));
    }

    public void spawnMonsterOnGroundBelow(Monster mob, Point pos) {
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        spos.y--;
        mob.setPosition(spos);
        spawnMonster(mob);
    }

    public void spawnCPQMonster(Monster mob, Point pos, int team) {
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        spos.y--;
        mob.setPosition(spos);
        mob.setTeam(team);
        spawnMonster(mob);
    }

    private void monsterItemDrop(final Monster m, long delay) {
        m.dropFromFriendlyMonster(delay);
    }

    public void spawnFakeMonsterOnGroundBelow(Monster mob, Point pos) {
        Point spos = getGroundBelow(pos);
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    public Point getGroundBelow(Point pos) {
        Point spos = new Point(pos.x, pos.y - 14); // Using -14 fixes spawning pets causing a lot of issues.
        spos = calcPointBelow(spos);
        spos.y--;//shouldn't be null!
        return spos;
    }

    public Point getPointBelow(Point pos) {
        return calcPointBelow(pos);
    }

    public void spawnRevives(final Monster monster) {
        monster.setMap(this);
        if (getEventInstance() != null) {
            getEventInstance().registerMonster(monster);
        }

        spawnAndAddRangedMapObject(monster, c -> c.sendPacket(PacketCreator.spawnMonster(monster, false)));

        monster.aggroUpdateController();
        updateBossSpawn(monster);

        spawnedMonstersOnMap.incrementAndGet();
        addSelfDestructive(monster);
        applyRemoveAfter(monster);
    }

    private void applyRemoveAfter(final Monster monster) {
        final selfDestruction selfDestruction = monster.getStats().selfDestruction();
        if (monster.getStats().removeAfter() > 0 || selfDestruction != null && selfDestruction.getHp() < 0) {
            Runnable removeAfterAction;

            if (selfDestruction == null) {
                removeAfterAction = () -> killMonster(monster, null, false, (short) 0);

                registerMapSchedule(removeAfterAction, SECONDS.toMillis(monster.getStats().removeAfter()));
            } else {
                removeAfterAction = () -> killMonster(monster, null, false, selfDestruction.getAction());

                registerMapSchedule(removeAfterAction, SECONDS.toMillis(selfDestruction.removeAfter()));
            }

            monster.pushRemoveAfterAction(removeAfterAction);
        }
    }

    public void dismissRemoveAfter(final Monster monster) {
        Runnable removeAfterAction = monster.popRemoveAfterAction();
        if (removeAfterAction != null) {
            OverallService service = (OverallService) this.getChannelServer().getServiceAccess(ChannelServices.OVERALL);
            service.forceRunOverallAction(mapid, removeAfterAction);
        }
    }

    private List<SpawnPoint> getMonsterSpawn() {
        synchronized (monsterSpawn) {
            return new ArrayList<>(monsterSpawn);
        }
    }

    private List<SpawnPoint> getAllMonsterSpawn() {
        synchronized (allMonsterSpawn) {
            return new ArrayList<>(allMonsterSpawn);
        }
    }

    public void spawnAllMonsterIdFromMapSpawnList(int id) {
        spawnAllMonsterIdFromMapSpawnList(id, 1, false);
    }

    public void spawnAllMonsterIdFromMapSpawnList(int id, int difficulty, boolean isPq) {
        for (SpawnPoint sp : getAllMonsterSpawn()) {
            if (sp.getMonsterId() == id && sp.shouldForceSpawn()) {
                spawnMonster(sp.getMonster(), difficulty, isPq);
            }
        }
    }

    public void spawnAllMonstersFromMapSpawnList() {
        spawnAllMonstersFromMapSpawnList(1, false);
    }

    public void spawnAllMonstersFromMapSpawnList(int difficulty, boolean isPq) {
        for (SpawnPoint sp : getAllMonsterSpawn()) {
            spawnMonster(sp.getMonster(), difficulty, isPq);
        }
    }

    public void spawnMonster(final Monster monster) {
        spawnMonster(monster, 1, false);
    }

    public void spawnMonster(final Monster monster, int difficulty, boolean isPq) {
        if (mobCapacity != -1 && mobCapacity == spawnedMonstersOnMap.get()) {
            return;//PyPQ
        }

        monster.changeDifficulty(difficulty, isPq);

        monster.setMap(this);
        if (getEventInstance() != null) {
            getEventInstance().registerMonster(monster);
        }

        spawnAndAddRangedMapObject(monster, c -> c.sendPacket(PacketCreator.spawnMonster(monster, true)), null);

        monster.aggroUpdateController();
        updateBossSpawn(monster);

        if ((monster.getTeam() == 1 || monster.getTeam() == 0) && (isCPQMap() || isCPQMap2())) {
            List<MCSkill> teamS = null;
            if (monster.getTeam() == 0) {
                teamS = redTeamBuffs;
            } else if (monster.getTeam() == 1) {
                teamS = blueTeamBuffs;
            }
            if (teamS != null) {
                for (MCSkill skil : teamS) {
                    if (skil != null) {
                        skil.getSkill().applyEffect(null, monster, false, null);
                    }
                }
            }
        }

        if (monster.getDropPeriodTime() > 0) { //9300102 - Watchhog, 9300061 - Moon Bunny (HPQ), 9300093 - Tylus
            if (monster.getId() == MobId.WATCH_HOG) {
                monsterItemDrop(monster, monster.getDropPeriodTime());
            } else if (monster.getId() == MobId.MOON_BUNNY) {
                monsterItemDrop(monster, monster.getDropPeriodTime() / 3);
            } else if (monster.getId() == MobId.TYLUS) {
                monsterItemDrop(monster, monster.getDropPeriodTime());
            } else if (monster.getId() == MobId.GIANT_SNOWMAN_LV5_EASY || monster.getId() == MobId.GIANT_SNOWMAN_LV5_MEDIUM || monster.getId() == MobId.GIANT_SNOWMAN_LV5_HARD) {
                monsterItemDrop(monster, monster.getDropPeriodTime());
            } else {
                log.error("UNCODED TIMED MOB DETECTED: {}", monster.getId());
            }
        }

        spawnedMonstersOnMap.incrementAndGet();
        addSelfDestructive(monster);
        applyRemoveAfter(monster);  // thanks LightRyuzaki for pointing issues with spawned CWKPQ mobs not applying this
    }

    public void spawnDojoMonster(final Monster monster) {
        Point[] pts = {new Point(140, 0), new Point(190, 7), new Point(187, 7)};
        spawnMonsterWithEffect(monster, 15, pts[Randomizer.nextInt(3)]);
    }

    public void spawnMonsterWithEffect(final Monster monster, final int effect, Point pos) {
        monster.setMap(this);
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        if (spos == null) {
            return;
        }

        if (getEventInstance() != null) {
            getEventInstance().registerMonster(monster);
        }

        spos.y--;
        monster.setPosition(spos);
        monster.setSpawnEffect(effect);

        spawnAndAddRangedMapObject(monster, c -> c.sendPacket(PacketCreator.spawnMonster(monster, true, effect)));

        monster.aggroUpdateController();
        updateBossSpawn(monster);

        spawnedMonstersOnMap.incrementAndGet();
        addSelfDestructive(monster);
        applyRemoveAfter(monster);
    }

    public void spawnFakeMonster(final Monster monster) {
        monster.setMap(this);
        monster.setFake(true);
        spawnAndAddRangedMapObject(monster, c -> c.sendPacket(PacketCreator.spawnFakeMonster(monster, 0)));

        spawnedMonstersOnMap.incrementAndGet();
        addSelfDestructive(monster);
    }

    public void makeMonsterReal(final Monster monster) {
        monster.setFake(false);
        broadcastMessage(PacketCreator.makeMonsterReal(monster));
        monster.aggroUpdateController();
        updateBossSpawn(monster);
    }

    public void spawnReactor(final Reactor reactor) {
        reactor.setMap(this);
        spawnAndAddRangedMapObject(reactor, c -> c.sendPacket(reactor.makeSpawnData()));
    }

    public void spawnDoor(final DoorObject door) {
        spawnAndAddRangedMapObject(door, c -> {
            Character chr = c.getPlayer();
            if (chr != null) {
                door.sendSpawnData(c, false);
                chr.addVisibleMapObject(door);
            }
        }, chr -> chr.getMapId() == door.getFrom().getId());
    }

    public Portal getDoorPortal(int doorid) {
        Portal doorPortal = portals.get(0x80 + doorid);
        if (doorPortal == null) {
            log.warn("[Door] {} ({}) does not contain door portalid {}", mapName, mapid, doorid);
            return portals.get(0x80);
        }

        return doorPortal;
    }

    public void spawnSummon(final Summon summon) {
        spawnAndAddRangedMapObject(summon, c -> {
            if (summon != null) {
                c.sendPacket(PacketCreator.spawnSummon(summon, true));
            }
        }, null);
    }

    public void spawnMist(final Mist mist, final int duration, boolean poison, boolean fake, boolean recovery) {
        addMapObject(mist);
        broadcastMessage(fake ? mist.makeFakeSpawnData(30) : mist.makeSpawnData());
        TimerManager tMan = TimerManager.getInstance();
        final ScheduledFuture<?> poisonSchedule;
        if (poison) {
            Runnable poisonTask = () -> {
                List<MapObject> affectedMonsters = getMapObjectsInBox(mist.getBox(), Collections.singletonList(MapObjectType.MONSTER));
                for (MapObject mo : affectedMonsters) {
                    if (mist.makeChanceResult()) {
                        MonsterStatusEffect poisonEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist.getSourceSkill(), null, false);
                        ((Monster) mo).applyStatus(mist.getOwner(), poisonEffect, true, duration);
                    }
                }
            };
            poisonSchedule = tMan.register(poisonTask, 2000, 2500);
        } else if (recovery) {
            Runnable poisonTask = () -> {
                List<MapObject> players = getMapObjectsInBox(mist.getBox(), Collections.singletonList(MapObjectType.PLAYER));
                for (MapObject mo : players) {
                    if (mist.makeChanceResult()) {
                        Character chr = (Character) mo;
                        if (mist.getOwner().getId() == chr.getId() || mist.getOwner().getParty() != null && mist.getOwner().getParty().containsMembers(chr.getMPC())) {
                            chr.addMP(mist.getSourceSkill().getEffect(chr.getSkillLevel(mist.getSourceSkill().getId())).getX() * chr.getMp() / 100);
                        }
                    }
                }
            };
            poisonSchedule = tMan.register(poisonTask, 2000, 2500);
        } else {
            poisonSchedule = null;
        }

        Runnable mistSchedule = () -> {
            removeMapObject(mist);
            if (poisonSchedule != null) {
                poisonSchedule.cancel(false);
            }
            broadcastMessage(mist.makeDestroyData());
        };

        MobMistService service = (MobMistService) this.getChannelServer().getServiceAccess(ChannelServices.MOB_MIST);
        service.registerMobMistCancelAction(mapid, mistSchedule, duration);
    }

    public void spawnKite(final Kite kite) {
        addMapObject(kite);
        broadcastMessage(kite.makeSpawnData());

        Runnable expireKite = () -> {
            removeMapObject(kite);
            broadcastMessage(kite.makeDestroyData());
        };

        getWorldServer().registerTimedMapObject(expireKite, YamlConfig.config.server.KITE_EXPIRE_TIME);
    }

    public final void spawnItemDrop(final MapObject dropper, final Character owner, final Item item, Point pos,
                                    final boolean ffaDrop, final boolean playerDrop) {
        spawnItemDrop(dropper, owner, item, pos, (byte) (ffaDrop ? 2 : 0), playerDrop);
    }

    public final void spawnItemDrop(final MapObject dropper, final Character owner, final Item item, Point pos,
                                    final byte dropType, final boolean playerDrop) {
        if (FieldLimit.DROP_LIMIT.check(this.getFieldLimit())) { // thanks Conrad for noticing some maps shouldn't have loots available
            this.disappearingItemDrop(dropper, owner, item, pos);
            return;
        }

        final Point droppos = calcDropPos(pos, pos);
        final MapItem mdrop = new MapItem(item, droppos, dropper, owner, owner.getClient(), dropType, playerDrop);
        mdrop.setDropTime(Server.getInstance().getCurrentTime());

        spawnAndAddRangedMapObject(mdrop, c -> {
            mdrop.lockItem();
            try {
                c.sendPacket(PacketCreator.dropItemFromMapObject(c.getPlayer(), mdrop, dropper.getPosition(), droppos,
                        (byte) 1, (short) 0));
            } finally {
                mdrop.unlockItem();
            }
        }, null);

        mdrop.lockItem();
        try {
            broadcastItemDropMessage(mdrop, dropper.getPosition(), droppos, (byte) 0, (short) 0);
        } finally {
            mdrop.unlockItem();
        }

        instantiateItemDrop(mdrop);
        activateItemReactors(mdrop, owner.getClient());
    }

    private void registerMapSchedule(Runnable r, long delay) {
        OverallService service = (OverallService) this.getChannelServer().getServiceAccess(ChannelServices.OVERALL);
        service.registerOverallAction(mapid, r, delay);
    }

    private void activateItemReactors(final MapItem drop, final Client c) {
        final Item item = drop.getItem();

        for (final MapObject o : getReactors()) {
            final Reactor react = (Reactor) o;

            if (react.getReactorType() == 100) {
                if (react.getReactItem(react.getEventState()).getLeft() == item.getItemId() && react.getReactItem(react.getEventState()).getRight() == item.getQuantity()) {

                    if (react.getArea().contains(drop.getPosition())) {
                        registerMapSchedule(new ActivateItemReactor(drop, react, c), 5000);
                        break;
                    }
                }
            }
        }
    }

    public void searchItemReactors(final Reactor react) {
        if (react.getReactorType() == 100) {
            Pair<Integer, Integer> reactProp = react.getReactItem(react.getEventState());
            int reactItem = reactProp.getLeft(), reactQty = reactProp.getRight();
            Rectangle reactArea = react.getArea();

            List<MapItem> list;
            objectRLock.lock();
            try {
                list = new ArrayList<>(droppedItems.keySet());
            } finally {
                objectRLock.unlock();
            }

            for (final MapItem drop : list) {
                drop.lockItem();
                try {
                    if (!drop.isPickedUp()) {
                        final Item item = drop.getItem();

                        if (item != null && reactItem == item.getItemId() && reactQty == item.getQuantity()) {
                            if (reactArea.contains(drop.getPosition())) {
                                Client owner = drop.getOwnerClient();
                                if (owner != null) {
                                    registerMapSchedule(new ActivateItemReactor(drop, react, owner), 5000);
                                }
                            }
                        }
                    }
                } finally {
                    drop.unlockItem();
                }
            }
        }
    }

    public void changeEnvironment(String mapObj, int newState) {
        broadcastMessage(PacketCreator.environmentChange(mapObj, newState));
    }

    public void startMapEffect(String msg, int itemId) {
        startMapEffect(msg, itemId, 30000);
    }

    public void startMapEffect(String msg, int itemId, long time) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapEffect(msg, itemId);
        broadcastMessage(mapEffect.makeStartData());

        Runnable r = () -> {
            broadcastMessage(mapEffect.makeDestroyData());
            mapEffect = null;
        };

        registerMapSchedule(r, time);
    }

    public Character getAnyCharacterFromParty(int partyid) {
        for (Character chr : this.getAllPlayers()) {
            if (chr.getPartyId() == partyid) {
                return chr;
            }
        }

        return null;
    }

    private void addPartyMemberInternal(Character chr, int partyid) {
        if (partyid == -1) {
            return;
        }

        Set<Integer> partyEntry = mapParty.get(partyid);
        if (partyEntry == null) {
            partyEntry = new LinkedHashSet<>();
            partyEntry.add(chr.getId());

            mapParty.put(partyid, partyEntry);
        } else {
            partyEntry.add(chr.getId());
        }
    }

    private void removePartyMemberInternal(Character chr, int partyid) {
        if (partyid == -1) {
            return;
        }

        Set<Integer> partyEntry = mapParty.get(partyid);
        if (partyEntry != null) {
            if (partyEntry.size() > 1) {
                partyEntry.remove(chr.getId());
            } else {
                mapParty.remove(partyid);
            }
        }
    }

    public void addPartyMember(Character chr, int partyid) {
        chrWLock.lock();
        try {
            addPartyMemberInternal(chr, partyid);
        } finally {
            chrWLock.unlock();
        }
    }

    public void removePartyMember(Character chr, int partyid) {
        chrWLock.lock();
        try {
            removePartyMemberInternal(chr, partyid);
        } finally {
            chrWLock.unlock();
        }
    }

    public void removeParty(int partyid) {
        chrWLock.lock();
        try {
            mapParty.remove(partyid);
        } finally {
            chrWLock.unlock();
        }
    }

    public void addPlayer(final Character chr) {
        int chrSize;
        Party party = chr.getParty();
        chrWLock.lock();
        try {
            characters.add(chr);
            chrSize = characters.size();

            if (party != null && party.getMemberById(chr.getId()) != null) {
                addPartyMemberInternal(chr, party.getId());
            }
            itemMonitorTimeout = 1;
        } finally {
            chrWLock.unlock();
        }

        chr.setMapId(mapid);
        chr.updateActiveEffects();
        
        if (this.getHPDec() > 0) {
            getWorldServer().addPlayerHpDecrease(chr);
        } else {
            getWorldServer().removePlayerHpDecrease(chr);
        }

        MapScriptManager msm = MapScriptManager.getInstance();
        if (chrSize == 1) {
            if (!hasItemMonitor()) {
                startItemMonitor();
                aggroMonitor.startAggroCoordinator();
            }

            if (onFirstUserEnter.length() != 0) {
                msm.runMapScript(chr.getClient(), "onFirstUserEnter/" + onFirstUserEnter, true);
            }
        }
        if (onUserEnter.length() != 0) {
            if (onUserEnter.equals("cygnusTest") && !MapId.isCygnusIntro(mapid)) {
                chr.saveLocation("INTRO");
            }

            msm.runMapScript(chr.getClient(), "onUserEnter/" + onUserEnter, false);
        }
        if (FieldLimit.CANNOTUSEMOUNTS.check(fieldLimit) && chr.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
            chr.cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
            chr.cancelBuffStats(BuffStat.MONSTER_RIDING);
        }

        if (mapid == MapId.FROM_LITH_TO_RIEN) { // To Rien
            int travelTime = getWorldServer().getTransportationTime((int) MINUTES.toMillis(1));
            chr.sendPacket(PacketCreator.getClock(travelTime / 1000));
            TimerManager.getInstance().schedule(() -> {
                if (chr.getMapId() == MapId.FROM_LITH_TO_RIEN) {
                    chr.changeMap(MapId.DANGEROUS_FOREST, 0);
                }
            }, travelTime);
        } else if (mapid == MapId.FROM_RIEN_TO_LITH) { // To Lith Harbor
            int travelTime = getWorldServer().getTransportationTime((int) MINUTES.toMillis(1));
            chr.sendPacket(PacketCreator.getClock(travelTime / 1000));
            TimerManager.getInstance().schedule(() -> {
                if (chr.getMapId() == MapId.FROM_RIEN_TO_LITH) {
                    chr.changeMap(MapId.LITH_HARBOUR, 3);
                }
            }, travelTime);
        } else if (mapid == MapId.FROM_ELLINIA_TO_EREVE) { // To Ereve (SkyFerry)
            int travelTime = getWorldServer().getTransportationTime((int) MINUTES.toMillis(2));
            chr.sendPacket(PacketCreator.getClock(travelTime / 1000));
            TimerManager.getInstance().schedule(() -> {
                if (chr.getMapId() == MapId.FROM_ELLINIA_TO_EREVE) {
                    chr.changeMap(MapId.SKY_FERRY, 0);
                }
            }, travelTime);
        } else if (mapid == MapId.FROM_EREVE_TO_ELLINIA) { // To Victoria Island (SkyFerry)
            int travelTime = getWorldServer().getTransportationTime((int) MINUTES.toMillis(2));
            chr.sendPacket(PacketCreator.getClock(travelTime / 1000));
            TimerManager.getInstance().schedule(() -> {
                if (chr.getMapId() == MapId.FROM_EREVE_TO_ELLINIA) {
                    chr.changeMap(MapId.ELLINIA_SKY_FERRY, 0);
                }
            }, travelTime);
        } else if (mapid == MapId.FROM_EREVE_TO_ORBIS) { // To Orbis (SkyFerry)
            int travelTime = getWorldServer().getTransportationTime((int) MINUTES.toMillis(8));
            chr.sendPacket(PacketCreator.getClock(travelTime / 1000));
            TimerManager.getInstance().schedule(() -> {
                if (chr.getMapId() == MapId.FROM_EREVE_TO_ORBIS) {
                    chr.changeMap(MapId.ORBIS_STATION, 0);
                }
            }, travelTime);
        } else if (mapid == MapId.FROM_ORBIS_TO_EREVE) { // To Ereve From Orbis (SkyFerry)
            int travelTime = getWorldServer().getTransportationTime((int) MINUTES.toMillis(8));
            chr.sendPacket(PacketCreator.getClock(travelTime / 1000));
            TimerManager.getInstance().schedule(() -> {
                if (chr.getMapId() == MapId.FROM_ORBIS_TO_EREVE) {
                    chr.changeMap(MapId.SKY_FERRY, 0);
                }
            }, travelTime);
        } else if (MiniDungeonInfo.isDungeonMap(mapid)) {
            MiniDungeon mmd = chr.getClient().getChannelServer().getMiniDungeon(mapid);
            if (mmd != null) {
                mmd.registerPlayer(chr);
            }
        } else if (GameConstants.isAriantColiseumArena(mapid)) {
            int pqTimer = (int) MINUTES.toMillis(10);
            chr.sendPacket(PacketCreator.getClock(pqTimer / 1000));
        }

        Pet[] pets = chr.getPets();
        for (Pet pet : pets) {
            if (pet != null) {
                pet.setPos(getGroundBelow(chr.getPosition()));
                chr.sendPacket(PacketCreator.showPet(chr, pet, false, false));
            } else {
                break;
            }
        }
        chr.commitExcludedItems();  // thanks OishiiKawaiiDesu for noticing pet item ignore registry erasing upon changing maps

        if (chr.getMonsterCarnival() != null) {
            chr.sendPacket(PacketCreator.getClock(chr.getMonsterCarnival().getTimeLeftSeconds()));
            if (isCPQMap()) {
                int team = -1;
                int oposition = -1;
                if (chr.getTeam() == 0) {
                    team = 0;
                    oposition = 1;
                }
                if (chr.getTeam() == 1) {
                    team = 1;
                    oposition = 0;
                }
                chr.sendPacket(PacketCreator.startMonsterCarnival(chr, team, oposition));
            }
        }

        chr.removeSandboxItems();

        if (chr.getChalkboard() != null) {
            if (!GameConstants.isFreeMarketRoom(mapid)) {
                chr.sendPacket(PacketCreator.useChalkboard(chr, false)); // update player's chalkboard when changing maps found thanks to Vcoc
            } else {
                chr.setChalkboard(null);
            }
        }

        if (chr.isHidden()) {
            broadcastGMSpawnPlayerMapObjectMessage(chr, chr, true);
            chr.sendPacket(PacketCreator.getGMEffect(0x10, (byte) 1));

            List<Pair<BuffStat, Integer>> dsstat = Collections.singletonList(new Pair<>(BuffStat.DARKSIGHT, 0));
            broadcastGMMessage(chr, PacketCreator.giveForeignBuff(chr.getId(), dsstat), false);
        } else {
            broadcastSpawnPlayerMapObjectMessage(chr, chr, true);
        }

        sendObjectPlacement(chr.getClient());

        if (isStartingEventMap() && !eventStarted()) {
            chr.getMap().getPortal("join00").setPortalStatus(false);
        }
        if (hasForcedEquip()) {
            chr.sendPacket(PacketCreator.showForcedEquip(-1));
        }
        if (specialEquip()) {
            chr.sendPacket(PacketCreator.coconutScore(0, 0));
            chr.sendPacket(PacketCreator.showForcedEquip(chr.getTeam()));
        }
        objectWLock.lock();
        try {
            this.mapobjects.put(chr.getObjectId(), chr);
        } finally {
            objectWLock.unlock();
        }

        if (chr.getPlayerShop() != null) {
            addMapObject(chr.getPlayerShop());
        }

        final Dragon dragon = chr.getDragon();
        if (dragon != null) {
            dragon.setPosition(chr.getPosition());
            this.addMapObject(dragon);
            if (chr.isHidden()) {
                this.broadcastGMPacket(chr, PacketCreator.spawnDragon(dragon));
            } else {
                this.broadcastPacket(chr, PacketCreator.spawnDragon(dragon));
            }
        }

        StatEffect summonStat = chr.getStatForBuff(BuffStat.SUMMON);
        if (summonStat != null) {
            Summon summon = chr.getSummonByKey(summonStat.getSourceId());
            summon.setPosition(chr.getPosition());
            chr.getMap().spawnSummon(summon);
            updateMapObjectVisibility(chr, summon);
        }
        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        chr.sendPacket(PacketCreator.resetForcedStats());
        if (MapId.isGodlyStatMap(mapid)) {
            chr.sendPacket(PacketCreator.aranGodlyStats());
        }
        if (chr.getEventInstance() != null && chr.getEventInstance().isTimerStarted()) {
            chr.sendPacket(PacketCreator.getClock((int) (chr.getEventInstance().getTimeLeft() / 1000)));
        }
        if (chr.getFitness() != null && chr.getFitness().isTimerStarted()) {
            chr.sendPacket(PacketCreator.getClock((int) (chr.getFitness().getTimeLeft() / 1000)));
        }

        if (chr.getOla() != null && chr.getOla().isTimerStarted()) {
            chr.sendPacket(PacketCreator.getClock((int) (chr.getOla().getTimeLeft() / 1000)));
        }

        if (mapid == MapId.EVENT_SNOWBALL) {
            chr.sendPacket(PacketCreator.rollSnowBall(true, 0, null, null));
        }

        if (hasClock()) {
            Calendar cal = Calendar.getInstance();
            chr.sendPacket(PacketCreator.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND)));
        }
        if (hasBoat() > 0) {
            if (hasBoat() == 1) {
                chr.sendPacket((PacketCreator.boatPacket(true)));
            } else {
                chr.sendPacket(PacketCreator.boatPacket(false));
            }
        }

        chr.receivePartyMemberHP();
        announcePlayerDiseases(chr.getClient());
    }

    private static void announcePlayerDiseases(final Client c) {
        Server.getInstance().registerAnnouncePlayerDiseases(c);
    }

    public Portal getRandomPlayerSpawnpoint() {
        List<Portal> spawnPoints = new ArrayList<>();
        for (Portal portal : portals.values()) {
            if (portal.getType() >= 0 && portal.getType() <= 1 && portal.getTargetMapId() == MapId.NONE) {
                spawnPoints.add(portal);
            }
        }
        Portal portal = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
        return portal != null ? portal : getPortal(0);
    }

    public Portal findClosestTeleportPortal(Point from) {
        Portal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (Portal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (portal.getType() == Portal.TELEPORT_PORTAL && distance < shortestDistance && portal.getTargetMapId() != MapId.NONE) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public Portal findClosestPlayerSpawnpoint(Point from) {
        Portal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (Portal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 1 && distance < shortestDistance && portal.getTargetMapId() == MapId.NONE) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public Portal findClosestPortal(Point from) {
        Portal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (Portal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public Portal findMarketPortal() {
        for (Portal portal : portals.values()) {
            String ptScript = portal.getScriptName();
            if (ptScript != null && ptScript.contains("market")) {
                return portal;
            }
        }
        return null;
    }

    /*
    public Collection<Portal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }
    */

    public void addPlayerPuppet(Character player) {
        for (Monster mm : this.getAllMonsters()) {
            mm.aggroAddPuppet(player);
        }
    }

    public void removePlayerPuppet(Character player) {
        for (Monster mm : this.getAllMonsters()) {
            mm.aggroRemovePuppet(player);
        }
    }

    public void removePlayer(Character chr) {
        Channel cserv = chr.getClient().getChannelServer();
        chr.unregisterChairBuff();

        Party party = chr.getParty();
        chrWLock.lock();
        try {
            if (party != null && party.getMemberById(chr.getId()) != null) {
                removePartyMemberInternal(chr, party.getId());
            }

            characters.remove(chr);
        } finally {
            chrWLock.unlock();
        }

        if (MiniDungeonInfo.isDungeonMap(mapid)) {
            MiniDungeon mmd = cserv.getMiniDungeon(mapid);
            if (mmd != null) {
                if (!mmd.unregisterPlayer(chr)) {
                    cserv.removeMiniDungeon(mapid);
                }
            }
        }

        removeMapObject(chr.getObjectId());
        if (!chr.isHidden()) {
            broadcastMessage(PacketCreator.removePlayerFromMap(chr.getId()));
        } else {
            broadcastGMMessage(PacketCreator.removePlayerFromMap(chr.getId()));
        }

        chr.leaveMap();

        for (Summon summon : new ArrayList<>(chr.getSummonsValues())) {
            if (summon.isStationary()) {
                chr.cancelEffectFromBuffStat(BuffStat.PUPPET);
            } else {
                removeMapObject(summon);
            }
        }

        if (chr.getDragon() != null) {
            removeMapObject(chr.getDragon());
            if (chr.isHidden()) {
                this.broadcastGMPacket(chr, PacketCreator.removeDragon(chr.getId()));
            } else {
                this.broadcastPacket(chr, PacketCreator.removeDragon(chr.getId()));
            }
        }
    }

    public void broadcastMessage(Packet packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastGMMessage(Packet packet) {
        broadcastGMMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    /**
     * Nonranged. Repeat to source according to parameter.
     *
     * @param source
     * @param packet
     * @param repeatToSource
     */
    public void broadcastMessage(Character source, Packet packet, boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    /**
     * Ranged and repeat according to parameters.
     *
     * @param source
     * @param packet
     * @param repeatToSource
     * @param ranged
     */
    public void broadcastMessage(Character source, Packet packet, boolean repeatToSource, boolean ranged) {
        broadcastMessage(repeatToSource ? null : source, packet, ranged ? getRangedDistance() : Double.POSITIVE_INFINITY, source.getPosition());
    }

    /**
     * Always ranged from Point.
     *
     * @param packet
     * @param rangedFrom
     */
    public void broadcastMessage(Packet packet, Point rangedFrom) {
        broadcastMessage(null, packet, getRangedDistance(), rangedFrom);
    }

    /**
     * Always ranged from point. Does not repeat to source.
     *
     * @param source
     * @param packet
     * @param rangedFrom
     */
    public void broadcastMessage(Character source, Packet packet, Point rangedFrom) {
        broadcastMessage(source, packet, getRangedDistance(), rangedFrom);
    }

    private void broadcastMessage(Character source, Packet packet, double rangeSq, Point rangedFrom) {
        chrRLock.lock();
        try {
            for (Character chr : characters) {
                if (chr != source) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.sendPacket(packet);
                        }
                    } else {
                        chr.sendPacket(packet);
                    }
                }
            }
        } finally {
            chrRLock.unlock();
        }
    }

    private void updateBossSpawn(Monster monster) {
        if (monster.hasBossHPBar()) {
            broadcastBossHpMessage(monster, monster.hashCode(), monster.makeBossHPBarPacket(), monster.getPosition());
        }
        if (monster.isBoss()) {
            if (unclaimOwnership() != null) {
                String mobName = MonsterInformationProvider.getInstance().getMobNameFromId(monster.getId());
                if (mobName != null) {
                    mobName = mobName.trim();
                    this.dropMessage(5, "This lawn has been taken siege by " + mobName + "'s forces and will be kept hold until their defeat.");
                }
            }
        }
    }

    public void broadcastBossHpMessage(Monster mm, int bossHash, Packet packet) {
        broadcastBossHpMessage(mm, bossHash, null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastBossHpMessage(Monster mm, int bossHash, Packet packet, Point rangedFrom) {
        broadcastBossHpMessage(mm, bossHash, null, packet, getRangedDistance(), rangedFrom);
    }

    private void broadcastBossHpMessage(Monster mm, int bossHash, Character source, Packet packet, double rangeSq, Point rangedFrom) {
        chrRLock.lock();
        try {
            for (Character chr : characters) {
                if (chr != source) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.getClient().announceBossHpBar(mm, bossHash, packet);
                        }
                    } else {
                        chr.getClient().announceBossHpBar(mm, bossHash, packet);
                    }
                }
            }
        } finally {
            chrRLock.unlock();
        }
    }

    private void broadcastItemDropMessage(MapItem mdrop, Point dropperPos, Point dropPos, byte mod, short delay,
                                          Point rangedFrom) {
        broadcastItemDropMessage(mdrop, dropperPos, dropPos, mod, delay, getRangedDistance(), rangedFrom);
    }

    private void broadcastItemDropMessage(MapItem mdrop, Point dropperPos, Point dropPos, byte mod, short delay) {
        broadcastItemDropMessage(mdrop, dropperPos, dropPos, mod, delay, Double.POSITIVE_INFINITY, null);
    }

    private void broadcastItemDropMessage(MapItem mdrop, Point dropperPos, Point dropPos, byte mod, short delay,
                                          double rangeSq, Point rangedFrom) {
        chrRLock.lock();
        try {
            for (Character chr : characters) {
                Packet packet = PacketCreator.dropItemFromMapObject(chr, mdrop, dropperPos, dropPos, mod, delay);

                // TODO: remove along with USE_MAXRANGE config
                if (rangeSq < Double.POSITIVE_INFINITY) {
                    if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                        chr.sendPacket(packet);
                    }
                } else {
                    chr.sendPacket(packet);
                }
            }
        } finally {
            chrRLock.unlock();
        }
    }

    public void broadcastSpawnPlayerMapObjectMessage(Character source, Character player, boolean enteringField) {
        broadcastSpawnPlayerMapObjectMessage(source, player, enteringField, false);
    }

    public void broadcastGMSpawnPlayerMapObjectMessage(Character source, Character player, boolean enteringField) {
        broadcastSpawnPlayerMapObjectMessage(source, player, enteringField, true);
    }

    private void broadcastSpawnPlayerMapObjectMessage(Character source, Character player, boolean enteringField, boolean gmBroadcast) {
        chrRLock.lock();
        try {
            if (gmBroadcast) {
                for (Character chr : characters) {
                    if (chr.isGM()) {
                        if (chr != source) {
                            chr.sendPacket(PacketCreator.spawnPlayerMapObject(chr.getClient(), player, enteringField));
                        }
                    }
                }
            } else {
                for (Character chr : characters) {
                    if (chr != source) {
                        chr.sendPacket(PacketCreator.spawnPlayerMapObject(chr.getClient(), player, enteringField));
                    }
                }
            }
        } finally {
            chrRLock.unlock();
        }
    }

    public void broadcastUpdateCharLookMessage(Character source, Character player) {
        chrRLock.lock();
        try {
            for (Character chr : characters) {
                if (chr != source) {
                    chr.sendPacket(PacketCreator.updateCharLook(chr.getClient(), player));
                }
            }
        } finally {
            chrRLock.unlock();
        }
    }

    public void dropMessage(int type, String message) {
        broadcastStringMessage(type, message);
    }

    public void broadcastStringMessage(int type, String message) {
        broadcastMessage(PacketCreator.serverNotice(type, message));
    }

    private static boolean isNonRangedType(MapObjectType type) {
        switch (type) {
            case NPC:
            case PLAYER:
            case HIRED_MERCHANT:
            case PLAYER_NPC:
            case DRAGON:
            case MIST:
            case KITE:
                return true;
            default:
                return false;
        }
    }

    private void sendObjectPlacement(Client c) {
        Character chr = c.getPlayer();
        Collection<MapObject> objects;

        objectRLock.lock();
        try {
            objects = new ArrayList<>(mapobjects.values());
        } finally {
            objectRLock.unlock();
        }

        for (MapObject o : objects) {
            if (isNonRangedType(o.getType())) {
                o.sendSpawnData(c);
            } else if (o.getType() == MapObjectType.SUMMON) {
                Summon summon = (Summon) o;
                if (summon.getOwner() == chr) {
                    if (chr.isSummonsEmpty() || !chr.containsSummon(summon)) {
                        objectWLock.lock();
                        try {
                            mapobjects.remove(o.getObjectId());
                        } finally {
                            objectWLock.unlock();
                        }

                        //continue;
                    }
                }
            }
        }

        if (chr != null) {
            for (MapObject o : getMapObjectsInRange(chr.getPosition(), getRangedDistance(), rangedMapobjectTypes)) {
                if (o.getType() == MapObjectType.REACTOR) {
                    if (((Reactor) o).isAlive()) {
                        o.sendSpawnData(chr.getClient());
                        chr.addVisibleMapObject(o);
                    }
                } else {
                    o.sendSpawnData(chr.getClient());
                    chr.addVisibleMapObject(o);

                    if (o.getType() == MapObjectType.MONSTER) {
                        ((Monster) o).aggroUpdateController();
                    }
                }
            }
        }
    }

    public List<MapObject> getMapObjectsInRange(Point from, double rangeSq, List<MapObjectType> types) {
        List<MapObject> ret = new LinkedList<>();
        objectRLock.lock();
        try {
            for (MapObject l : mapobjects.values()) {
                if (types.contains(l.getType())) {
                    if (from.distanceSq(l.getPosition()) <= rangeSq) {
                        ret.add(l);
                    }
                }
            }
            return ret;
        } finally {
            objectRLock.unlock();
        }
    }

    public List<MapObject> getMapObjectsInBox(Rectangle box, List<MapObjectType> types) {
        List<MapObject> ret = new LinkedList<>();
        objectRLock.lock();
        try {
            for (MapObject l : mapobjects.values()) {
                if (types.contains(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
            return ret;
        } finally {
            objectRLock.unlock();
        }
    }

    public void addPortal(Portal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public Portal getPortal(String portalname) {
        for (Portal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public Portal getPortal(int portalid) {
        return portals.get(portalid);
    }

    public void addMapleArea(Rectangle rec) {
        areas.add(rec);
    }

    public List<Rectangle> getAreas() {
        return new ArrayList<>(areas);
    }

    public Rectangle getArea(int index) {
        return areas.get(index);
    }

    public void setFootholds(FootholdTree footholds) {
        this.footholds = footholds;
    }

    public FootholdTree getFootholds() {
        return footholds;
    }

    public void setMapPointBoundings(int px, int py, int h, int w) {
        mapArea.setBounds(px, py, w, h);
    }

    public void setMapLineBoundings(int vrTop, int vrBottom, int vrLeft, int vrRight) {
        mapArea.setBounds(vrLeft, vrTop, vrRight - vrLeft, vrBottom - vrTop);
    }

    public MonsterAggroCoordinator getAggroCoordinator() {
        return aggroMonitor;
    }

    /**
     * it's threadsafe, gtfo :D
     *
     * @param monster
     * @param mobTime
     */
    public void addMonsterSpawn(Monster monster, int mobTime, int team) {
        Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        SpawnPoint sp = new SpawnPoint(monster, newpos, !monster.isMobile(), mobTime, mobInterval, team);
        monsterSpawn.add(sp);
        if (sp.shouldSpawn() || mobTime == -1) {// -1 does not respawn and should not either but force ONE spawn
            spawnMonster(sp.getMonster());
        }
    }

    public void addAllMonsterSpawn(Monster monster, int mobTime, int team) {
        Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        SpawnPoint sp = new SpawnPoint(monster, newpos, !monster.isMobile(), mobTime, mobInterval, team);
        allMonsterSpawn.add(sp);
    }

    public void removeMonsterSpawn(int mobId, int x, int y) {
        // assumption: spawn points identifies by tuple (lifeid, x, y)

        Point checkpos = calcPointBelow(new Point(x, y));
        checkpos.y -= 1;

        List<SpawnPoint> toRemove = new LinkedList<>();
        for (SpawnPoint sp : getMonsterSpawn()) {
            Point pos = sp.getPosition();
            if (sp.getMonsterId() == mobId && checkpos.equals(pos)) {
                toRemove.add(sp);
            }
        }

        if (!toRemove.isEmpty()) {
            synchronized (monsterSpawn) {
                for (SpawnPoint sp : toRemove) {
                    monsterSpawn.remove(sp);
                }
            }
        }
    }

    public void removeAllMonsterSpawn(int mobId, int x, int y) {
        // assumption: spawn points identifies by tuple (lifeid, x, y)

        Point checkpos = calcPointBelow(new Point(x, y));
        checkpos.y -= 1;

        List<SpawnPoint> toRemove = new LinkedList<>();
        for (SpawnPoint sp : getAllMonsterSpawn()) {
            Point pos = sp.getPosition();
            if (sp.getMonsterId() == mobId && checkpos.equals(pos)) {
                toRemove.add(sp);
            }
        }

        if (!toRemove.isEmpty()) {
            synchronized (allMonsterSpawn) {
                for (SpawnPoint sp : toRemove) {
                    allMonsterSpawn.remove(sp);
                }
            }
        }
    }

    public void reportMonsterSpawnPoints(Character chr) {
        chr.dropMessage(6, "Mob spawnpoints on map " + getId() + ", with available Mob SPs " + monsterSpawn.size() + ", used " + spawnedMonstersOnMap.get() + ":");
        for (SpawnPoint sp : getAllMonsterSpawn()) {
            chr.dropMessage(6, "  id: " + sp.getMonsterId() + " canSpawn: " + !sp.getDenySpawn() + " numSpawned: " + sp.getSpawned() + " x: " + sp.getPosition().getX() + " y: " + sp.getPosition().getY() + " time: " + sp.getMobTime() + " team: " + sp.getTeam());
        }
    }

    public Map<Integer, Character> getMapPlayers() {
        chrRLock.lock();
        try {
            Map<Integer, Character> mapChars = new HashMap<>(characters.size());

            for (Character chr : characters) {
                mapChars.put(chr.getId(), chr);
            }

            return mapChars;
        } finally {
            chrRLock.unlock();
        }
    }

    public Collection<Character> getCharacters() {
        chrRLock.lock();
        try {
            return Collections.unmodifiableCollection(this.characters);
        } finally {
            chrRLock.unlock();
        }
    }

    public Character getCharacterById(int id) {
        chrRLock.lock();
        try {
            for (Character chr : this.characters) {
                if (chr.getId() == id) {
                    return chr;
                }
            }
        } finally {
            chrRLock.unlock();
        }
        return null;
    }

    private static void updateMapObjectVisibility(Character chr, MapObject mo) {
        if (!chr.isMapObjectVisible(mo)) { // object entered view range
            if (mo.getType() == MapObjectType.SUMMON || mo.getPosition().distanceSq(chr.getPosition()) <= getRangedDistance()) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else if (mo.getType() != MapObjectType.SUMMON && mo.getPosition().distanceSq(chr.getPosition()) > getRangedDistance()) {
            chr.removeVisibleMapObject(mo);
            mo.sendDestroyData(chr.getClient());
        }
    }

    public void moveMonster(Monster monster, Point reportedPos) {
        monster.setPosition(reportedPos);
        for (Character chr : getAllPlayers()) {
            updateMapObjectVisibility(chr, monster);
        }
    }

    public void movePlayer(Character player, Point newPosition) {
        player.setPosition(newPosition);

        try {
            MapObject[] visibleObjects = player.getVisibleMapObjects();

            Map<Integer, MapObject> mapObjects = getCopyMapObjects();
            for (MapObject mo : visibleObjects) {
                if (mo != null) {
                    if (mapObjects.get(mo.getObjectId()) == mo) {
                        updateMapObjectVisibility(player, mo);
                    } else {
                        player.removeVisibleMapObject(mo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (MapObject mo : getMapObjectsInRange(player.getPosition(), getRangedDistance(), rangedMapobjectTypes)) {
            if (!player.isMapObjectVisible(mo)) {
                mo.sendSpawnData(player.getClient());
                player.addVisibleMapObject(mo);
            }
        }
    }

    public final void toggleEnvironment(final String ms) {
        Map<String, Integer> env = getEnvironment();

        if (env.containsKey(ms)) {
            moveEnvironment(ms, env.get(ms) == 1 ? 2 : 1);
        } else {
            moveEnvironment(ms, 1);
        }
    }

    public final void moveEnvironment(final String ms, final int type) {
        broadcastMessage(PacketCreator.environmentMove(ms, type));

        objectWLock.lock();
        try {
            environment.put(ms, type);
        } finally {
            objectWLock.unlock();
        }
    }

    public final Map<String, Integer> getEnvironment() {
        objectRLock.lock();
        try {
            return Collections.unmodifiableMap(environment);
        } finally {
            objectRLock.unlock();
        }
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setClock(boolean hasClock) {
        this.clock = hasClock;
    }

    public boolean hasClock() {
        return clock;
    }

    public void setTown(boolean isTown) {
        this.town = isTown;
    }

    public boolean isTown() {
        return town;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean mute) {
        isMuted = mute;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public void setEverlast(boolean everlast) {
        this.everlast = everlast;
    }

    public boolean getEverlast() {
        return everlast;
    }

    public int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }

    public void setMobCapacity(int capacity) {
        this.mobCapacity = capacity;
    }

    public void setBackgroundTypes(HashMap<Integer, Integer> backTypes) {
        backgroundTypes.putAll(backTypes);
    }

    // not really costly to keep generating imo
    public void sendNightEffect(Character chr) {
        for (Entry<Integer, Integer> types : backgroundTypes.entrySet()) {
            if (types.getValue() >= 3) { // 3 is a special number
                chr.sendPacket(PacketCreator.changeBackgroundEffect(true, types.getKey(), 0));
            }
        }
    }

    public void broadcastNightEffect() {
        chrRLock.lock();
        try {
            for (Character chr : this.characters) {
                sendNightEffect(chr);
            }
        } finally {
            chrRLock.unlock();
        }
    }

    public Character getCharacterByName(String name) {
        chrRLock.lock();
        try {
            for (Character chr : this.characters) {
                if (chr.getName().equalsIgnoreCase(name)) {
                    return chr;
                }
            }
        } finally {
            chrRLock.unlock();
        }
        return null;
    }

    public boolean makeDisappearItemFromMap(MapObject mapobj) {
        if (mapobj instanceof MapItem) {
            return makeDisappearItemFromMap((MapItem) mapobj);
        } else {
            return mapobj == null;  // no drop to make disappear...
        }
    }

    public boolean makeDisappearItemFromMap(MapItem mapitem) {
        if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
            mapitem.lockItem();
            try {
                if (mapitem.isPickedUp()) {
                    return true;
                }

                MapleMap.this.pickItemDrop(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem);
                return true;
            } finally {
                mapitem.unlockItem();
            }
        }

        return false;
    }

    // TODO: no reason to implement runnable - this is not intended to be submitted to another thread
    private class MobLootEntry implements Runnable {

        private final byte droptype;
        private final int mobpos;
        private final int chRate;
        private final Point pos;
        private final short delay;
        private final List<MonsterDropEntry> dropEntry;
        private final List<MonsterDropEntry> visibleQuestEntry;
        private final List<MonsterDropEntry> otherQuestEntry;
        private final List<MonsterGlobalDropEntry> globalEntry;
        private final Character chr;
        private final Monster mob;

        protected MobLootEntry(byte droptype, int mobpos, int chRate, Point pos, short delay,
                               List<MonsterDropEntry> dropEntry, List<MonsterDropEntry> visibleQuestEntry,
                               List<MonsterDropEntry> otherQuestEntry, List<MonsterGlobalDropEntry> globalEntry,
                               Character chr, Monster mob) {
            this.droptype = droptype;
            this.mobpos = mobpos;
            this.chRate = chRate;
            this.pos = pos;
            this.delay = delay;
            this.dropEntry = dropEntry;
            this.visibleQuestEntry = visibleQuestEntry;
            this.otherQuestEntry = otherQuestEntry;
            this.globalEntry = globalEntry;
            this.chr = chr;
            this.mob = mob;
        }

        @Override
        public void run() {
            byte d = 1;

            // Normal Drops
            d = dropItemsFromMonsterOnMap(dropEntry, pos, d, chRate, droptype, mobpos, chr, mob, delay);

            // Global Drops
            d = dropGlobalItemsFromMonsterOnMap(globalEntry, pos, d, droptype, mobpos, chr, mob, delay);

            // Quest Drops
            d = dropItemsFromMonsterOnMap(visibleQuestEntry, pos, d, chRate, droptype, mobpos, chr, mob, delay);
            dropItemsFromMonsterOnMap(otherQuestEntry, pos, d, chRate, droptype, mobpos, chr, mob, delay);
        }
    }

    private class ActivateItemReactor implements Runnable {

        private final MapItem mapitem;
        private final Reactor reactor;
        private final Client c;

        public ActivateItemReactor(MapItem mapitem, Reactor reactor, Client c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            reactor.hitLockReactor();
            try {
                if (reactor.getReactorType() == 100) {
                    if (reactor.getShouldCollect() == true && mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
                        mapitem.lockItem();
                        try {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            mapitem.setPickedUp(true);
                            unregisterItemDrop(mapitem);

                            reactor.setShouldCollect(false);
                            MapleMap.this.broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());

                            droppedItemCount.decrementAndGet();
                            MapleMap.this.removeMapObject(mapitem);

                            reactor.hitReactor(c);

                            if (reactor.getDelay() > 0) {
                                MapleMap reactorMap = reactor.getMap();

                                OverallService service = (OverallService) reactorMap.getChannelServer().getServiceAccess(ChannelServices.OVERALL);
                                service.registerOverallAction(reactorMap.getId(), () -> {
                                    reactor.lockReactor();
                                    try {
                                        reactor.resetReactorActions(0);
                                        reactor.setAlive(true);
                                        broadcastMessage(PacketCreator.triggerReactor(reactor, 0));
                                    } finally {
                                        reactor.unlockReactor();
                                    }
                                }, reactor.getDelay());
                            }
                        } finally {
                            mapitem.unlockItem();
                        }
                    }
                }
            } finally {
                reactor.hitUnlockReactor();
            }
        }
    }

    public void instanceMapFirstSpawn(int difficulty, boolean isPq) {
        for (SpawnPoint spawnPoint : getAllMonsterSpawn()) {
            if (spawnPoint.getMobTime() == -1) {   //just those allowed to be spawned only once
                spawnMonster(spawnPoint.getMonster());
            }
        }
    }

    public void instanceMapRespawn() {
        if (!allowSummons) {
            return;
        }

        final int numShouldSpawn = (short) ((monsterSpawn.size() - spawnedMonstersOnMap.get()));//Fking lol'd
        if (numShouldSpawn > 0) {
            List<SpawnPoint> randomSpawn = getMonsterSpawn();
            Collections.shuffle(randomSpawn);
            int spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                if (spawnPoint.shouldSpawn()) {
                    spawnMonster(spawnPoint.getMonster());
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        }
    }

    public void instanceMapForceRespawn() {
        if (!allowSummons) {
            return;
        }

        final int numShouldSpawn = (short) ((monsterSpawn.size() - spawnedMonstersOnMap.get()));//Fking lol'd
        if (numShouldSpawn > 0) {
            List<SpawnPoint> randomSpawn = getMonsterSpawn();
            Collections.shuffle(randomSpawn);
            int spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                if (spawnPoint.shouldForceSpawn()) {
                    spawnMonster(spawnPoint.getMonster());
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        }
    }

    public void closeMapSpawnPoints() {
        for (SpawnPoint spawnPoint : getMonsterSpawn()) {
            spawnPoint.setDenySpawn(true);
        }
    }

    public void restoreMapSpawnPoints() {
        for (SpawnPoint spawnPoint : getMonsterSpawn()) {
            spawnPoint.setDenySpawn(false);
        }
    }

    public void setAllowSpawnPointInBox(boolean allow, Rectangle box) {
        for (SpawnPoint sp : getMonsterSpawn()) {
            if (box.contains(sp.getPosition())) {
                sp.setDenySpawn(!allow);
            }
        }
    }

    public void setAllowSpawnPointInRange(boolean allow, Point from, double rangeSq) {
        for (SpawnPoint sp : getMonsterSpawn()) {
            if (from.distanceSq(sp.getPosition()) <= rangeSq) {
                sp.setDenySpawn(!allow);
            }
        }
    }

    public SpawnPoint findClosestSpawnpoint(Point from) {
        SpawnPoint closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (SpawnPoint sp : getMonsterSpawn()) {
            double distance = sp.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = sp;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    private static double getCurrentSpawnRate(int numPlayers) {
        return 0.70 + (0.05 * Math.min(6, numPlayers));
    }

    private int getNumShouldSpawn(int numPlayers) {
        /*
        System.out.println("----------------------------------");
        for (SpawnPoint spawnPoint : getMonsterSpawn()) {
            System.out.println("sp " + spawnPoint.getPosition().getX() + ", " + spawnPoint.getPosition().getY() + ": " + spawnPoint.getDenySpawn());
        }
        System.out.println("try " + monsterSpawn.size() + " - " + spawnedMonstersOnMap.get());
        System.out.println("----------------------------------");
        */

        if (YamlConfig.config.server.USE_ENABLE_FULL_RESPAWN) {
            return (monsterSpawn.size() - spawnedMonstersOnMap.get());
        }

        int maxNumShouldSpawn = (int) Math.ceil(getCurrentSpawnRate(numPlayers) * monsterSpawn.size());
        return maxNumShouldSpawn - spawnedMonstersOnMap.get();
    }

    public void respawn() {
        if (!allowSummons) {
            return;
        }

        int numPlayers;
        chrRLock.lock();
        try {
            numPlayers = characters.size();

            if (numPlayers == 0) {
                return;
            }
        } finally {
            chrRLock.unlock();
        }

        int numShouldSpawn = getNumShouldSpawn(numPlayers);
        if (numShouldSpawn > 0) {
            List<SpawnPoint> randomSpawn = new ArrayList<>(getMonsterSpawn());
            Collections.shuffle(randomSpawn);
            short spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                if (spawnPoint.shouldSpawn()) {
                    spawnMonster(spawnPoint.getMonster());
                    spawned++;

                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        }
    }

    public void mobMpRecovery() {
        for (Monster mob : this.getAllMonsters()) {
            if (mob.isAlive()) {
                mob.heal(0, mob.getLevel());
            }
        }
    }

    public final int getNumPlayersInArea(final int index) {
        return getNumPlayersInRect(getArea(index));
    }

    public final int getNumPlayersInRect(final Rectangle rect) {
        int ret = 0;

        chrRLock.lock();
        try {
            final Iterator<Character> ltr = characters.iterator();
            while (ltr.hasNext()) {
                if (rect.contains(ltr.next().getPosition())) {
                    ret++;
                }
            }
        } finally {
            chrRLock.unlock();
        }
        return ret;
    }

    public final int getNumPlayersItemsInArea(final int index) {
        return getNumPlayersItemsInRect(getArea(index));
    }

    public final int getNumPlayersItemsInRect(final Rectangle rect) {
        int retP = getNumPlayersInRect(rect);
        int retI = getMapObjectsInBox(rect, Arrays.asList(MapObjectType.ITEM)).size();

        return retP + retI;
    }

    private interface DelayedPacketCreation {

        void sendPackets(Client c);
    }

    private interface SpawnCondition {

        boolean canSpawn(Character chr);
    }

    public int getHPDec() {
        return decHP;
    }

    public void setHPDec(int delta) {
        decHP = delta;
    }

    public int getHPDecProtect() {
        return protectItem;
    }

    public void setHPDecProtect(int delta) {
        this.protectItem = delta;
    }

    public float getRecovery() {
        return recovery;
    }

    public void setRecovery(float recRate) {
        recovery = recRate;
    }

    private int hasBoat() {
        return !boat ? 0 : (docked ? 1 : 2);
    }

    public void setBoat(boolean hasBoat) {
        this.boat = hasBoat;
    }

    public void setDocked(boolean isDocked) {
        this.docked = isDocked;
    }

    public boolean getDocked() {
        return this.docked;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public int getSeats() {
        return seats;
    }

    public void broadcastGMMessage(Character source, Packet packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    private void broadcastGMMessage(Character source, Packet packet, double rangeSq, Point rangedFrom) {
        chrRLock.lock();
        try {
            for (Character chr : characters) {
                if (chr != source && chr.isGM()) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.sendPacket(packet);
                        }
                    } else {
                        chr.sendPacket(packet);
                    }
                }
            }
        } finally {
            chrRLock.unlock();
        }
    }

    public void broadcastNONGMMessage(Character source, Packet packet, boolean repeatToSource) {
        chrRLock.lock();
        try {
            for (Character chr : characters) {
                if (chr != source && !chr.isGM()) {
                    chr.sendPacket(packet);
                }
            }
        } finally {
            chrRLock.unlock();
        }
    }

    public OxQuiz getOx() {
        return ox;
    }

    public void setOx(OxQuiz set) {
        this.ox = set;
    }

    public void setOxQuiz(boolean b) {
        this.isOxQuiz = b;
    }

    public boolean isOxQuiz() {
        return isOxQuiz;
    }

    public void setOnUserEnter(String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public String getOnUserEnter() {
        return onUserEnter;
    }

    public void setOnFirstUserEnter(String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    public String getOnFirstUserEnter() {
        return onFirstUserEnter;
    }

    private boolean hasForcedEquip() {
        return fieldType == 81 || fieldType == 82;
    }

    public void setFieldType(int fieldType) {
        this.fieldType = fieldType;
    }

    public void clearDrops(Character player) {
        for (MapObject i : getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.ITEM))) {
            droppedItemCount.decrementAndGet();
            removeMapObject(i);
            this.broadcastMessage(PacketCreator.removeItemFromMap(i.getObjectId(), 0, player.getId()));
        }
    }

    public void clearDrops() {
        for (MapObject i : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.ITEM))) {
            droppedItemCount.decrementAndGet();
            removeMapObject(i);
            this.broadcastMessage(PacketCreator.removeItemFromMap(i.getObjectId(), 0, 0));
        }
    }

    public void setFieldLimit(int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public int getFieldLimit() {
        return fieldLimit;
    }

    public void allowSummonState(boolean b) {
        MapleMap.this.allowSummons = b;
    }

    public boolean getSummonState() {
        return MapleMap.this.allowSummons;
    }

    public void warpEveryone(int to) {
        List<Character> players = new ArrayList<>(getCharacters());

        for (Character chr : players) {
            chr.changeMap(to);
        }
    }

    public void warpEveryone(int to, int pto) {
        List<Character> players = new ArrayList<>(getCharacters());

        for (Character chr : players) {
            chr.changeMap(to, pto);
        }
    }

    // BEGIN EVENTS
    public void setSnowball(int team, Snowball ball) {
        switch (team) {
            case 0:
                this.snowball0 = ball;
                break;
            case 1:
                this.snowball1 = ball;
                break;
            default:
                break;
        }
    }

    public Snowball getSnowball(int team) {
        switch (team) {
            case 0:
                return snowball0;
            case 1:
                return snowball1;
            default:
                return null;
        }
    }

    private boolean specialEquip() {//Maybe I shouldn't use fieldType :\
        return fieldType == 4 || fieldType == 19;
    }

    public void setCoconut(Coconut nut) {
        this.coconut = nut;
    }

    public Coconut getCoconut() {
        return coconut;
    }

    public void warpOutByTeam(int team, int mapid) {
        List<Character> chars = new ArrayList<>(getCharacters());
        for (Character chr : chars) {
            if (chr != null) {
                if (chr.getTeam() == team) {
                    chr.changeMap(mapid);
                }
            }
        }
    }

    public void startEvent(final Character chr) {
        if (this.mapid == MapId.EVENT_COCONUT_HARVEST && getCoconut() == null) {
            setCoconut(new Coconut(this));
            coconut.startEvent();
        } else if (this.mapid == MapId.EVENT_PHYSICAL_FITNESS) {
            chr.setFitness(new Fitness(chr));
            chr.getFitness().startFitness();
        } else if (this.mapid == MapId.EVENT_OLA_OLA_1 || this.mapid == MapId.EVENT_OLA_OLA_2 ||
                this.mapid == MapId.EVENT_OLA_OLA_3 || this.mapid == MapId.EVENT_OLA_OLA_4) {
            chr.setOla(new Ola(chr));
            chr.getOla().startOla();
        } else if (this.mapid == MapId.EVENT_OX_QUIZ && getOx() == null) {
            setOx(new OxQuiz(this));
            getOx().sendQuestion();
            setOxQuiz(true);
        } else if (this.mapid == MapId.EVENT_SNOWBALL && getSnowball(chr.getTeam()) == null) {
            setSnowball(0, new Snowball(0, this));
            setSnowball(1, new Snowball(1, this));
            getSnowball(chr.getTeam()).startEvent();
        }
    }

    public boolean eventStarted() {
        return eventstarted;
    }

    public void startEvent() {
        this.eventstarted = true;
    }

    public void setEventStarted(boolean event) {
        this.eventstarted = event;
    }

    public String getEventNPC() {
        StringBuilder sb = new StringBuilder();
        sb.append("Talk to ");
        if (mapid == MapId.SOUTHPERRY) {
            sb.append("Paul!");
        } else if (mapid == MapId.LITH_HARBOUR) {
            sb.append("Jean!");
        } else if (mapid == MapId.ORBIS) {
            sb.append("Martin!");
        } else if (mapid == MapId.LUDIBRIUM) {
            sb.append("Tony!");
        } else {
            return null;
        }
        return sb.toString();
    }

    public boolean hasEventNPC() {
        return this.mapid == 60000 || this.mapid == MapId.LITH_HARBOUR || this.mapid == MapId.ORBIS || this.mapid == MapId.LUDIBRIUM;
    }

    public boolean isStartingEventMap() {
        return this.mapid == MapId.EVENT_PHYSICAL_FITNESS || this.mapid == MapId.EVENT_OX_QUIZ ||
                this.mapid == MapId.EVENT_FIND_THE_JEWEL || this.mapid == MapId.EVENT_OLA_OLA_0 || this.mapid == MapId.EVENT_OLA_OLA_1;
    }

    public boolean isEventMap() {
        return this.mapid >= MapId.EVENT_FIND_THE_JEWEL && this.mapid < MapId.EVENT_WINNER || this.mapid > MapId.EVENT_EXIT && this.mapid <= 109090000;
    }

    public void setTimeMob(int id, String msg) {
        timeMob = new Pair<>(id, msg);
    }

    public Pair<Integer, String> getTimeMob() {
        return timeMob;
    }

    public void toggleHiddenNPC(int id) {
        chrRLock.lock();
        objectRLock.lock();
        try {
            for (MapObject obj : mapobjects.values()) {
                if (obj.getType() == MapObjectType.NPC) {
                    NPC npc = (NPC) obj;
                    if (npc.getId() == id) {
                        npc.setHide(!npc.isHidden());
                        if (!npc.isHidden()) //Should only be hidden upon changing maps
                        {
                            broadcastMessage(PacketCreator.spawnNPC(npc));
                        }
                    }
                }
            }
        } finally {
            objectRLock.unlock();
            chrRLock.unlock();
        }
    }

    public void setMobInterval(short interval) {
        this.mobInterval = interval;
    }

    public short getMobInterval() {
        return mobInterval;
    }

    public void clearMapObjects() {
        clearDrops();
        killAllMonsters();
        resetReactors();
    }

    public final void resetFully() {
        resetMapObjects();
    }

    public void resetMapObjects() {
        resetMapObjects(1, false);
    }

    public void resetPQ() {
        resetPQ(1);
    }

    public void resetPQ(int difficulty) {
        resetMapObjects(difficulty, true);
    }

    public void resetMapObjects(int difficulty, boolean isPq) {
        clearMapObjects();

        restoreMapSpawnPoints();
        instanceMapFirstSpawn(difficulty, isPq);
    }

    public void broadcastShip(final boolean state) {
        broadcastMessage(PacketCreator.boatPacket(state));
        this.setDocked(state);
    }

    public void broadcastEnemyShip(final boolean state) {
        broadcastMessage(PacketCreator.crogBoatPacket(state));
        this.setDocked(state);
    }

    public boolean isHorntailDefeated() {   // all parts of dead horntail can be found here?
        for (int i = MobId.DEAD_HORNTAIL_MIN; i <= MobId.DEAD_HORNTAIL_MAX; i++) {
            if (getMonsterById(i) == null) {
                return false;
            }
        }

        return true;
    }

    public void spawnHorntailOnGroundBelow(final Point targetPoint) {   // ayy lmao
        Monster htIntro = LifeFactory.getMonster(MobId.SUMMON_HORNTAIL);
        spawnMonsterOnGroundBelow(htIntro, targetPoint);    // htintro spawn animation converting into horntail detected thanks to Arnah

        final Monster ht = LifeFactory.getMonster(MobId.HORNTAIL);
        ht.setParentMobOid(htIntro.getObjectId());
        ht.addListener(new MonsterListener() {
            @Override
            public void monsterKilled(int aniTime) {}

            @Override
            public void monsterDamaged(Character from, int trueDmg) {
                ht.addHp(trueDmg);
            }

            @Override
            public void monsterHealed(int trueHeal) {
                ht.addHp(-trueHeal);
            }
        });
        spawnMonsterOnGroundBelow(ht, targetPoint);

        for (int mobId = MobId.HORNTAIL_HEAD_A; mobId <= MobId.HORNTAIL_TAIL; mobId++) {
            Monster m = LifeFactory.getMonster(mobId);
            m.setParentMobOid(htIntro.getObjectId());

            m.addListener(new MonsterListener() {
                @Override
                public void monsterKilled(int aniTime) {}

                @Override
                public void monsterDamaged(Character from, int trueDmg) {
                    // thanks Halcyon for noticing HT not dropping loots due to propagated damage not registering attacker
                    ht.applyFakeDamage(from, trueDmg, true);
                }

                @Override
                public void monsterHealed(int trueHeal) {
                    ht.addHp(trueHeal);
                }
            });

            spawnMonsterOnGroundBelow(m, targetPoint);
        }
    }

    public boolean claimOwnership(Character chr) {
        if (mapOwner == null) {
            this.mapOwner = chr;
            chr.setOwnedMap(this);

            mapOwnerLastActivityTime = Server.getInstance().getCurrentTime();

            getChannelServer().registerOwnedMap(this);
            return true;
        } else {
            return chr == mapOwner;
        }
    }

    public Character unclaimOwnership() {
        Character lastOwner = this.mapOwner;
        return unclaimOwnership(lastOwner) ? lastOwner : null;
    }

    public boolean unclaimOwnership(Character chr) {
        if (chr != null && mapOwner == chr) {
            this.mapOwner = null;
            chr.setOwnedMap(null);

            mapOwnerLastActivityTime = Long.MAX_VALUE;

            getChannelServer().unregisterOwnedMap(this);
            return true;
        } else {
            return false;
        }
    }

    private void refreshOwnership() {
        mapOwnerLastActivityTime = Server.getInstance().getCurrentTime();
    }

    public boolean isOwnershipRestricted(Character chr) {
        Character owner = mapOwner;

        if (owner != null) {
            if (owner != chr && !owner.isPartyMember(chr)) {    // thanks Vcoc & BHB for suggesting the map ownership feature
                chr.showMapOwnershipInfo(owner);
                return true;
            } else {
                this.refreshOwnership();
            }
        }

        return false;
    }

    public void checkMapOwnerActivity() {
        long timeNow = Server.getInstance().getCurrentTime();
        if (timeNow - mapOwnerLastActivityTime > 60000) {
            if (unclaimOwnership() != null) {
                this.dropMessage(5, "This lawn is now free real estate.");
            }
        }
    }

    private final List<Point> takenSpawns = new LinkedList<>();
    private final List<GuardianSpawnPoint> guardianSpawns = new LinkedList<>();
    private final List<MCSkill> blueTeamBuffs = new ArrayList();
    private final List<MCSkill> redTeamBuffs = new ArrayList();
    private final List<Integer> skillIds = new ArrayList();
    private final List<Pair<Integer, Integer>> mobsToSpawn = new ArrayList();

    public List<MCSkill> getBlueTeamBuffs() {
        return blueTeamBuffs;
    }

    public List<MCSkill> getRedTeamBuffs() {
        return redTeamBuffs;
    }

    public void clearBuffList() {
        redTeamBuffs.clear();
        blueTeamBuffs.clear();
    }

    public List<MapObject> getAllPlayer() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.PLAYER));
    }

    public boolean isCPQMap() {
        switch (this.getId()) {
            case 980000101:
            case 980000201:
            case 980000301:
            case 980000401:
            case 980000501:
            case 980000601:
            case 980031100:
            case 980032100:
            case 980033100:
                return true;
        }
        return false;
    }

    public boolean isCPQMap2() {
        switch (this.getId()) {
            case 980031100:
            case 980032100:
            case 980033100:
                return true;
        }
        return false;
    }

    public boolean isCPQLobby() {
        switch (this.getId()) {
            case 980000100:
            case 980000200:
            case 980000300:
            case 980000400:
            case 980000500:
            case 980000600:
                return true;
        }
        return false;
    }

    public boolean isBlueCPQMap() {
        switch (this.getId()) {
            case 980000501:
            case 980000601:
            case 980031200:
            case 980032200:
            case 980033200:
                return true;
        }
        return false;
    }

    public boolean isPurpleCPQMap() {
        switch (this.getId()) {
            case 980000301:
            case 980000401:
            case 980031200:
            case 980032200:
            case 980033200:
                return true;
        }
        return false;
    }

    public Point getRandomSP(int team) {
        if (takenSpawns.size() > 0) {
            for (SpawnPoint sp : monsterSpawn) {
                for (Point pt : takenSpawns) {
                    if ((sp.getPosition().x == pt.x && sp.getPosition().y == pt.y) || (sp.getTeam() != team && !this.isBlueCPQMap())) {
                        continue;
                    } else {
                        takenSpawns.add(pt);
                        return sp.getPosition();
                    }
                }
            }
        } else {
            for (SpawnPoint sp : monsterSpawn) {
                if (sp.getTeam() == team || this.isBlueCPQMap()) {
                    takenSpawns.add(sp.getPosition());
                    return sp.getPosition();
                }
            }
        }
        return null;
    }

    public GuardianSpawnPoint getRandomGuardianSpawn(int team) {
        boolean alltaken = false;
        for (GuardianSpawnPoint a : this.guardianSpawns) {
            if (!a.isTaken()) {
                alltaken = false;
                break;
            }
        }
        if (alltaken) {
            return null;
        }
        if (this.guardianSpawns.size() > 0) {
            while (true) {
                for (GuardianSpawnPoint gsp : this.guardianSpawns) {
                    if (!gsp.isTaken() && Math.random() < 0.3 && (gsp.getTeam() == -1 || gsp.getTeam() == team)) {
                        return gsp;
                    }
                }
            }
        }
        return null;
    }

    public void addGuardianSpawnPoint(GuardianSpawnPoint a) {
        this.guardianSpawns.add(a);
    }

    public int spawnGuardian(int team, int num) {
        try {
            if (team == 0 && redTeamBuffs.size() >= 4 || team == 1 && blueTeamBuffs.size() >= 4) {
                return 2;
            }
            final MCSkill skill = CarnivalFactory.getInstance().getGuardian(num);
            if (team == 0 && redTeamBuffs.contains(skill)) {
                return 0;
            } else if (team == 1 && blueTeamBuffs.contains(skill)) {
                return 0;
            }
            GuardianSpawnPoint pt = this.getRandomGuardianSpawn(team);
            if (pt == null) {
                return -1;
            }
            int reactorID = 9980000 + team;
            Reactor reactor = new Reactor(ReactorFactory.getReactorS(reactorID), reactorID);
            pt.setTaken(true);
            reactor.setPosition(pt.getPosition());
            reactor.setName(team + "" + num); //lol
            reactor.resetReactorActions(0);
            this.spawnReactor(reactor);
            reactor.setGuardian(pt);
            this.buffMonsters(team, skill);
            getReactorByOid(reactor.getObjectId()).hitReactor(((Character) this.getAllPlayer().get(0)).getClient());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public void buffMonsters(int team, MCSkill skill) {
        if (skill == null) {
            return;
        }

        if (team == 0) {
            redTeamBuffs.add(skill);
        } else if (team == 1) {
            blueTeamBuffs.add(skill);
        }
        for (MapObject mmo : this.mapobjects.values()) {
            if (mmo.getType() == MapObjectType.MONSTER) {
                Monster mob = (Monster) mmo;
                if (mob.getTeam() == team) {
                    skill.getSkill().applyEffect(null, mob, false, null);
                }
            }
        }
    }

    public final List<Integer> getSkillIds() {
        return skillIds;
    }

    public final void addSkillId(int z) {
        this.skillIds.add(z);
    }

    public final void addMobSpawn(int mobId, int spendCP) {
        this.mobsToSpawn.add(new Pair<>(mobId, spendCP));
    }

    public final List<Pair<Integer, Integer>> getMobsToSpawn() {
        return mobsToSpawn;
    }

    public boolean isCPQWinnerMap() {
        switch (this.getId()) {
            case 980000103:
            case 980000203:
            case 980000303:
            case 980000403:
            case 980000503:
            case 980000603:
            case 980031300:
            case 980032300:
            case 980033300:
                return true;
        }
        return false;
    }

    public boolean isCPQLoserMap() {
        switch (this.getId()) {
            case 980000104:
            case 980000204:
            case 980000304:
            case 980000404:
            case 980000504:
            case 980000604:
            case 980031400:
            case 980032400:
            case 980033400:
                return true;
        }
        return false;
    }

    public void runCharacterStatUpdate() {
        if (!statUpdateRunnables.isEmpty()) {
            List<Runnable> toRun = new ArrayList<>(statUpdateRunnables);
            statUpdateRunnables.clear();

            for (Runnable r : toRun) {
                r.run();
            }
        }
    }

    public void registerCharacterStatUpdate(Runnable r) {
        statUpdateRunnables.add(r);
    }

    public void dispose() {
        for (Monster mm : this.getAllMonsters()) {
            mm.dispose();
        }

        clearMapObjects();

        event = null;
        footholds = null;
        portals.clear();
        mapEffect = null;

        chrWLock.lock();
        try {
            aggroMonitor.dispose();
            aggroMonitor = null;

            if (itemMonitor != null) {
                itemMonitor.cancel(false);
                itemMonitor = null;
            }

            if (expireItemsTask != null) {
                expireItemsTask.cancel(false);
                expireItemsTask = null;
            }

            if (characterStatUpdateTask != null) {
                characterStatUpdateTask.cancel(false);
                characterStatUpdateTask = null;
            }
        } finally {
            chrWLock.unlock();
        }
    }

    public int getMaxMobs() {
        return maxMobs;
    }

    public void setMaxMobs(int maxMobs) {
        this.maxMobs = maxMobs;
    }

    public int getMaxReactors() {
        return maxReactors;
    }

    public void setMaxReactors(int maxReactors) {
        this.maxReactors = maxReactors;
    }

    public int getDeathCP() {
        return deathCP;
    }

    public void setDeathCP(int deathCP) {
        this.deathCP = deathCP;
    }

    public int getTimeDefault() {
        return timeDefault;
    }

    public void setTimeDefault(int timeDefault) {
        this.timeDefault = timeDefault;
    }

    public int getTimeExpand() {
        return timeExpand;
    }

    public void setTimeExpand(int timeExpand) {
        this.timeExpand = timeExpand;
    }

}
