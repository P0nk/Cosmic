/* 
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any otheer version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; witout even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.


 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import client.autoban.AutobanManager;
import client.creator.CharacterFactoryRecipe;
import client.inventory.Equip;
import client.inventory.Equip.StatUpgrade;
import client.inventory.Inventory;
import client.inventory.InventoryProof;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.ModifyInventory;
import client.inventory.Pet;
import client.inventory.PetDataFactory;
import client.inventory.WeaponType;
import client.inventory.manipulator.CashIdGenerator;
import client.inventory.manipulator.InventoryManipulator;
import client.keybind.KeyBinding;
import client.keybind.QuickslotBinding;
import client.newyear.NewYearCardRecord;
import client.processor.action.PetAutopotProcessor;
import client.processor.npc.FredrickProcessor;
import config.YamlConfig;
import constants.game.ExpTable;
import constants.game.GameConstants;
import constants.id.ItemId;
import constants.id.MapId;
import constants.id.MobId;
import constants.inventory.ItemConstants;
import constants.skills.Aran;
import constants.skills.Beginner;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.Buccaneer;
import constants.skills.Corsair;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.Evan;
import constants.skills.FPArchMage;
import constants.skills.Hermit;
import constants.skills.Hero;
import constants.skills.ILArchMage;
import constants.skills.Legend;
import constants.skills.Magician;
import constants.skills.Marauder;
import constants.skills.Marksman;
import constants.skills.NightLord;
import constants.skills.Noblesse;
import constants.skills.Paladin;
import constants.skills.Priest;
import constants.skills.Ranger;
import constants.skills.Shadower;
import constants.skills.Sniper;
import constants.skills.ThunderBreaker;
import constants.skills.Warrior;
import model.CharacterIdentity;
import net.netty.GameViolationException;
import net.packet.Packet;
import net.server.PlayerBuffValueHolder;
import net.server.PlayerCoolDownValueHolder;
import net.server.Server;
import net.server.coordinator.world.InviteCoordinator;
import net.server.guild.Alliance;
import net.server.guild.Guild;
import net.server.guild.GuildCharacter;
import net.server.guild.GuildPackets;
import net.server.world.Messenger;
import net.server.world.MessengerCharacter;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import net.server.world.PartyOperation;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventInstanceManager;
import scripting.item.ItemScriptManager;
import server.CashShop;
import server.ExpLogger;
import server.ExpLogger.ExpLogRecord;
import server.ItemInformationProvider;
import server.ItemInformationProvider.ScriptedItem;
import server.Marriage;
import server.StatEffect;
import server.Storage;
import server.ThreadManager;
import server.TimerManager;
import server.Trade;
import server.events.Events;
import server.events.RescueGaga;
import server.events.gm.Fitness;
import server.events.gm.Ola;
import server.life.BanishInfo;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.MobSkillType;
import server.life.Monster;
import server.life.PlayerNPC;
import server.maps.AbstractAnimatedMapObject;
import server.maps.Door;
import server.maps.DoorObject;
import server.maps.Dragon;
import server.maps.FieldLimit;
import server.maps.HiredMerchant;
import server.maps.MapEffect;
import server.maps.MapItem;
import server.maps.MapManager;
import server.maps.MapObject;
import server.maps.MapObjectType;
import server.maps.MapleMap;
import server.maps.MiniGame;
import server.maps.MiniGame.MiniGameResult;
import server.maps.PlayerShop;
import server.maps.PlayerShopItem;
import server.maps.Portal;
import server.maps.SavedLocation;
import server.maps.SavedLocationType;
import server.maps.Summon;
import server.minigame.RockPaperScissor;
import server.partyquest.AriantColiseum;
import server.partyquest.MonsterCarnival;
import server.partyquest.MonsterCarnivalParty;
import server.partyquest.PartyQuest;
import server.quest.Quest;
import server.shop.Shop;
import tools.DatabaseConnection;
import tools.LongTool;
import tools.PacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.packets.WeddingPackets;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Character extends AbstractCharacterObject {
    private static final Logger log = LoggerFactory.getLogger(Character.class);
    private static final String LEVEL_200 = "[Congrats] %s has reached Level %d! Congratulate %s on such an amazing achievement!";
    private static final String[] BLOCKED_NAMES = {"admin", "owner", "moderator", "intern", "donor", "administrator", "FREDRICK", "help", "helper", "alert", "notice", "maplestory", "fuck", "wizet", "fucking", "negro", "fuk", "fuc", "penis", "pussy", "asshole", "gay",
            "nigger", "homo", "suck", "cum", "shit", "shitty", "condom", "security", "official", "rape", "nigga", "sex", "tit", "boner", "orgy", "clit", "asshole", "fatass", "bitch", "support", "gamemaster", "cock", "gaay", "gm",
            "operate", "master", "sysop", "party", "GameMaster", "community", "message", "event", "test", "meso", "Scania", "yata", "AsiaSoft", "henesys"};

    private int world;
    private int accountid, id, level;
    private int rank, rankMove, jobRank, jobRankMove;
    private int gender, hair, face;
    private int fame;
    private int initialSpawnPoint;
    private int mapid;
    private int currentPage, currentType = 0, currentTab = 1;
    private int itemEffect;
    private int guildid, guildRank, allianceRank;
    private int messengerposition = 4;
    private int slots = 0;
    private int energybar;
    private int gmLevel;
    private int ci = 0;
    private FamilyEntry familyEntry;
    private int familyId;
    private int bookCover;
    private int battleshipHp = 0;
    private int mesosTraded = 0;
    private int possibleReports = 10;
    private int ariantPoints, dojoPoints, vanquisherStage, dojoStage, dojoEnergy, vanquisherKills;
    private int expRate = 1, mesoRate = 1, dropRate = 1, expCoupon = 1, mesoCoupon = 1, dropCoupon = 1;
    private int omokwins, omokties, omoklosses, matchcardwins, matchcardties, matchcardlosses;
    private int owlSearch;
    private long lastfametime, lastUsedCashItem, lastExpression = 0, lastHealed, lastDeathtime, jailExpiration = -1;
    private transient int localstr, localdex, localluk, localint_, localmagic, localwatk;
    private transient int equipmaxhp, equipmaxmp, equipstr, equipdex, equipluk, equipint_, equipmagic, equipwatk, localchairhp, localchairmp;
    private int localchairrate;
    private boolean hidden, equipchanged = true, berserk, hasMerchant, hasSandboxItem = false, whiteChat = false, canRecvPartySearchInvite = true;
    private boolean equippedMesoMagnet = false, equippedItemPouch = false, equippedPetItemIgnore = false;
    private boolean usedSafetyCharm = false;
    private float autopotHpAlert, autopotMpAlert;
    private int linkedLevel = 0;
    private String linkedName = null;
    private boolean finishedDojoTutorial;
    private volatile boolean usedStorage = false; // TODO: fully move this into Storage where usage can be fully controlled.
    private String name;
    private String chalktext;
    private String commandtext;
    private String dataString;
    private String search = null;
    private final AtomicBoolean mapTransitioning = new AtomicBoolean(true);  // player client is currently trying to change maps or log in the game map
    private final AtomicBoolean awayFromWorld = new AtomicBoolean(true);  // player is online, but on cash shop or mts
    private final AtomicInteger exp = new AtomicInteger();
    private final AtomicInteger gachaexp = new AtomicInteger();
    private final AtomicInteger meso = new AtomicInteger();
    private final AtomicInteger chair = new AtomicInteger(-1);
    private long totalExpGained = 0;
    private int merchantmeso;
    private BuddyList buddylist;
    private EventInstanceManager eventInstance = null;
    private HiredMerchant hiredMerchant = null;
    private Client client;
    private GuildCharacter mgc = null;
    private PartyCharacter mpc = null;
    private Inventory[] inventory;
    private Job job = Job.BEGINNER;
    private Messenger messenger = null;
    private MiniGame miniGame;
    private RockPaperScissor rps;
    private Mount maplemount;
    private Party party;
    private final Pet[] pets = new Pet[3];
    private PlayerShop playerShop = null;
    private Shop shop = null;
    private SkinColor skinColor = SkinColor.NORMAL;
    private Storage storage = null;
    private Trade trade = null;
    private MonsterBook monsterbook;
    private CashShop cashshop;
    private final Set<NewYearCardRecord> newyears = new LinkedHashSet<>();
    private final SavedLocation[] savedLocations;
    private final SkillMacro[] skillMacros = new SkillMacro[5];
    private List<Integer> lastmonthfameids;
    private final List<WeakReference<MapleMap>> lastVisitedMaps = new LinkedList<>();
    private WeakReference<MapleMap> ownedMap = new WeakReference<>(null);
    private final Map<Short, QuestStatus> quests;
    private final Set<Monster> controlled = new LinkedHashSet<>();
    private final Map<Integer, String> entered = new LinkedHashMap<>();
    private final Set<MapObject> visibleMapObjects = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<Skill, SkillEntry> skills = new LinkedHashMap<>();
    private final Map<Integer, Integer> activeCoupons = new LinkedHashMap<>();
    private final Map<Integer, Integer> activeCouponRates = new LinkedHashMap<>();
    private final EnumMap<BuffStat, BuffStatValueHolder> effects = new EnumMap<>(BuffStat.class);
    private final Map<BuffStat, Byte> buffEffectsCount = new LinkedHashMap<>();
    private final Map<Disease, Long> diseaseExpires = new LinkedHashMap<>();
    private final Map<Integer, Map<BuffStat, BuffStatValueHolder>> buffEffects = new LinkedHashMap<>(); // non-overriding buffs thanks to Ronan
    private final Map<Integer, Long> buffExpires = new LinkedHashMap<>();
    private final Map<Integer, KeyBinding> keymap = new LinkedHashMap<>();
    private final Map<Integer, Summon> summons = new LinkedHashMap<>();
    private final Map<Integer, CooldownValueHolder> coolDowns = new LinkedHashMap<>();
    private final EnumMap<Disease, Pair<DiseaseValueHolder, MobSkill>> diseases = new EnumMap<>(Disease.class);
    private byte[] m_aQuickslotLoaded;
    private QuickslotBinding m_pQuickslotKeyMapped;
    private Door pdoor = null;
    private Map<Quest, Long> questExpirations = new LinkedHashMap<>();
    private ScheduledFuture<?> dragonBloodSchedule;
    private ScheduledFuture<?> hpDecreaseTask;
    private ScheduledFuture<?> beholderHealingSchedule, beholderBuffSchedule, berserkSchedule;
    private ScheduledFuture<?> skillCooldownTask = null;
    private ScheduledFuture<?> buffExpireTask = null;
    private ScheduledFuture<?> itemExpireTask = null;
    private ScheduledFuture<?> diseaseExpireTask = null;
    private ScheduledFuture<?> questExpireTask = null;
    private ScheduledFuture<?> recoveryTask = null;
    private ScheduledFuture<?> extraRecoveryTask = null;
    private ScheduledFuture<?> chairRecoveryTask = null;
    private ScheduledFuture<?> pendantOfSpirit = null; //1122017
    private ScheduledFuture<?> cpqSchedule = null;
    private final Lock chrLock = new ReentrantLock(true);
    private final Lock evtLock = new ReentrantLock(true);
    private final Lock petLock = new ReentrantLock(true);
    private final Lock prtLock = new ReentrantLock();
    private final Lock cpnLock = new ReentrantLock();
    private final Map<Integer, Set<Integer>> excluded = new LinkedHashMap<>();
    private final Set<Integer> excludedItems = new LinkedHashSet<>();
    private final Set<Integer> disabledPartySearchInvites = new LinkedHashSet<>();
    private static final String[] ariantroomleader = new String[3];
    private static final int[] ariantroomslot = new int[3];
    private long portaldelay = 0, lastcombo = 0;
    private short combocounter = 0;
    private final List<String> blockedPortals = new ArrayList<>();
    private final Map<Short, String> area_info = new LinkedHashMap<>();
    private AutobanManager autoban;
    private volatile boolean isBanned = false;
    private boolean blockCashShop = false;
    private boolean allowExpGain = true;
    private byte pendantExp = 0, lastmobcount = 0, doorSlot = -1;
    private final List<Integer> trockmaps = new ArrayList<>();
    private final List<Integer> viptrockmaps = new ArrayList<>();
    private Map<String, Events> events = new LinkedHashMap<>();
    private PartyQuest partyQuest = null;
    private final List<Pair<DelayedQuestUpdate, Object[]>> npcUpdateQuests = new LinkedList<>();
    private Dragon dragon = null;
    private Ring marriageRing;
    private int marriageItemid = -1;
    private int partnerId = -1;
    private final List<Ring> crushRings = new ArrayList<>();
    private final List<Ring> friendshipRings = new ArrayList<>();
    private boolean loggedIn = false;
    private boolean useCS;  //chaos scroll upon crafting item.
    private long npcCd;
    private int newWarpMap = -1;
    private boolean canWarpMap = true;  //only one "warp" must be used per call, and this will define the right one.
    private int canWarpCounter = 0;     //counts how many times "inner warps" have been called.
    private byte extraHpRec = 0, extraMpRec = 0;
    private short extraRecInterval;
    private int targetHpBarHash = 0;
    private long targetHpBarTime = 0;
    private long nextWarningTime = 0;
    private long lastExpGainTime;
    private boolean pendingNameChange; //only used to change name on logout, not to be relied upon elsewhere
    private long loginTime;
    private boolean chasing = false;

    private Character() {
        super.setListener(new AbstractCharacterListener() {
            @Override
            public void onHpChanged(int oldHp) {
                hpChangeAction(oldHp);
            }

            @Override
            public void onHpmpPoolUpdate() {
                List<Pair<Stat, Integer>> hpmpupdate = recalcLocalStats();
                for (Pair<Stat, Integer> p : hpmpupdate) {
                    statUpdates.put(p.getLeft(), p.getRight());
                }

                if (hp > localmaxhp) {
                    setHp(localmaxhp);
                    statUpdates.put(Stat.HP, hp);
                }

                if (mp > localmaxmp) {
                    setMp(localmaxmp);
                    statUpdates.put(Stat.MP, mp);
                }
            }

            @Override
            public void onStatUpdate() {
                recalcLocalStats();
            }

            @Override
            public void onAnnounceStatPoolUpdate() {
                List<Pair<Stat, Integer>> statup = new ArrayList<>(8);
                for (Map.Entry<Stat, Integer> s : statUpdates.entrySet()) {
                    statup.add(new Pair<>(s.getKey(), s.getValue()));
                }

                sendPacket(PacketCreator.updatePlayerStats(statup, true, Character.this));
            }
        });

        useCS = false;

        setStance(0);
        inventory = new Inventory[InventoryType.values().length];
        savedLocations = new SavedLocation[SavedLocationType.values().length];

        for (InventoryType type : InventoryType.values()) {
            byte b = 24;
            if (type == InventoryType.CASH) {
                b = 96;
            }
            inventory[type.ordinal()] = new Inventory(this, type, b);
        }
        inventory[InventoryType.CANHOLD.ordinal()] = new InventoryProof(this);

        for (int i = 0; i < SavedLocationType.values().length; i++) {
            savedLocations[i] = null;
        }
        quests = new LinkedHashMap<>();
        setPosition(new Point(0, 0));
    }

    private static Job getJobStyleInternal(int jobid, byte opt) {
        int jobtype = jobid / 100;

        if (jobtype == Job.WARRIOR.getId() / 100 || jobtype == Job.DAWNWARRIOR1.getId() / 100 || jobtype == Job.ARAN1.getId() / 100) {
            return (Job.WARRIOR);
        } else if (jobtype == Job.MAGICIAN.getId() / 100 || jobtype == Job.BLAZEWIZARD1.getId() / 100 || jobtype == Job.EVAN1.getId() / 100) {
            return (Job.MAGICIAN);
        } else if (jobtype == Job.BOWMAN.getId() / 100 || jobtype == Job.WINDARCHER1.getId() / 100) {
            if (jobid / 10 == Job.CROSSBOWMAN.getId() / 10) {
                return (Job.CROSSBOWMAN);
            } else {
                return (Job.BOWMAN);
            }
        } else if (jobtype == Job.THIEF.getId() / 100 || jobtype == Job.NIGHTWALKER1.getId() / 100) {
            return (Job.THIEF);
        } else if (jobtype == Job.PIRATE.getId() / 100 || jobtype == Job.THUNDERBREAKER1.getId() / 100) {
            if (opt == (byte) 0x80) {
                return (Job.BRAWLER);
            } else {
                return (Job.GUNSLINGER);
            }
        }

        return (Job.BEGINNER);
    }

    public Job getJobStyle(byte opt) {
        return getJobStyleInternal(this.getJob().getId(), opt);
    }

    public Job getJobStyle() {
        return getJobStyle((byte) ((this.getStr() > this.getDex()) ? 0x80 : 0x40));
    }

    public static Character getDefault(Client c) {
        Character ret = new Character();
        ret.client = c;
        ret.setGMLevel(0);
        ret.hp = 50;
        ret.setMaxHp(50);
        ret.mp = 5;
        ret.setMaxMp(5);
        ret.str = 12;
        ret.dex = 5;
        ret.int_ = 4;
        ret.luk = 4;
        ret.map = null;
        ret.job = Job.BEGINNER;
        ret.level = 1;
        ret.accountid = c.getAccID();
        ret.buddylist = new BuddyList(20);
        ret.maplemount = null;
        ret.getInventory(InventoryType.EQUIP).setSlotLimit(24);
        ret.getInventory(InventoryType.USE).setSlotLimit(24);
        ret.getInventory(InventoryType.SETUP).setSlotLimit(24);
        ret.getInventory(InventoryType.ETC).setSlotLimit(24);

        // Select a keybinding method
        int[] selectedKey;
        int[] selectedType;
        int[] selectedAction;

        if (YamlConfig.config.server.USE_CUSTOM_KEYSET) {
            selectedKey = GameConstants.getCustomKey(true);
            selectedType = GameConstants.getCustomType(true);
            selectedAction = GameConstants.getCustomAction(true);
        } else {
            selectedKey = GameConstants.getCustomKey(false);
            selectedType = GameConstants.getCustomType(false);
            selectedAction = GameConstants.getCustomAction(false);
        }

        for (int i = 0; i < selectedKey.length; i++) {
            ret.keymap.put(selectedKey[i], new KeyBinding(selectedType[i], selectedAction[i]));
        }


        //to fix the map 0 lol
        for (int i = 0; i < 5; i++) {
            ret.trockmaps.add(MapId.NONE);
        }
        for (int i = 0; i < 10; i++) {
            ret.viptrockmaps.add(MapId.NONE);
        }

        return ret;
    }

    public boolean isLoggedinWorld() {
        return this.isLoggedin() && !this.isAwayFromWorld();
    }

    public boolean isAwayFromWorld() {
        return awayFromWorld.get();
    }

    public void setEnteredChannelWorld() {
        awayFromWorld.set(false);
        client.getChannelServer().removePlayerAway(id);

        if (canRecvPartySearchInvite) {
            this.getWorldServer().getPartySearchCoordinator().attachPlayer(this);
        }
    }

    public void setAwayFromChannelWorld() {
        setAwayFromChannelWorld(false);
    }

    public void setDisconnectedFromChannelWorld() {
        setAwayFromChannelWorld(true);
    }

    private void setAwayFromChannelWorld(boolean disconnect) {
        awayFromWorld.set(true);

        if (!disconnect) {
            client.getChannelServer().insertPlayerAway(id);
        } else {
            client.getChannelServer().removePlayerAway(id);
        }
    }

    public void updatePartySearchAvailability(boolean psearchAvailable) {
        if (psearchAvailable) {
            if (canRecvPartySearchInvite && getParty() == null) {
                this.getWorldServer().getPartySearchCoordinator().attachPlayer(this);
            }
        } else {
            if (canRecvPartySearchInvite) {
                this.getWorldServer().getPartySearchCoordinator().detachPlayer(this);
            }
        }
    }

    public boolean toggleRecvPartySearchInvite() {
        canRecvPartySearchInvite = !canRecvPartySearchInvite;

        if (canRecvPartySearchInvite) {
            updatePartySearchAvailability(getParty() == null);
        } else {
            this.getWorldServer().getPartySearchCoordinator().detachPlayer(this);
        }

        return canRecvPartySearchInvite;
    }

    public boolean isRecvPartySearchInviteEnabled() {
        return canRecvPartySearchInvite;
    }

    public void resetPartySearchInvite(int fromLeaderid) {
        disabledPartySearchInvites.remove(fromLeaderid);
    }

    public void disablePartySearchInvite(int fromLeaderid) {
        disabledPartySearchInvites.add(fromLeaderid);
    }

    public boolean hasDisabledPartySearchInvite(int fromLeaderid) {
        return disabledPartySearchInvites.contains(fromLeaderid);
    }

    public boolean getCS() {
        return useCS;
    }

    public void setCS(boolean cs) {
        useCS = cs;
    }

    public long getNpcCooldown() {
        return npcCd;
    }

    public void setNpcCooldown(long d) {
        npcCd = d;
    }

    public void setOwlSearch(int id) {
        owlSearch = id;
    }

    public int getOwlSearch() {
        return owlSearch;
    }

    public void addCooldown(int skillId, long startTime, long length) {
        effLock.lock();
        chrLock.lock();
        try {
            this.coolDowns.put(Integer.valueOf(skillId), new CooldownValueHolder(skillId, startTime, length));
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void addCrushRing(Ring r) {
        crushRings.add(r);
    }

    public Ring getRingById(int id) {
        for (Ring ring : getCrushRings()) {
            if (ring.getRingId() == id) {
                return ring;
            }
        }
        for (Ring ring : getFriendshipRings()) {
            if (ring.getRingId() == id) {
                return ring;
            }
        }

        if (marriageRing != null) {
            if (marriageRing.getRingId() == id) {
                return marriageRing;
            }
        }

        return null;
    }

    public int getMarriageItemId() {
        return marriageItemid;
    }

    public void setMarriageItemId(int itemid) {
        marriageItemid = itemid;
    }

    public int getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(int partnerid) {
        partnerId = partnerid;
    }

    public int getRelationshipId() {
        return getWorldServer().getRelationshipId(id);
    }

    public boolean isMarried() {
        return marriageRing != null && partnerId > 0;
    }

    public boolean hasJustMarried() {
        EventInstanceManager eim = getEventInstance();
        if (eim != null) {
            String prop = eim.getProperty("groomId");

            if (prop != null) {
                return (Integer.parseInt(prop) == id || eim.getIntProperty("brideId") == id) &&
                        (mapid == MapId.CHAPEL_WEDDING_ALTAR || mapid == MapId.CATHEDRAL_WEDDING_ALTAR);
            }
        }

        return false;
    }

    public int addDojoPointsByMap(int mapid) {
        int pts = 0;
        if (dojoPoints < 17000) {
            pts = 1 + ((mapid - 1) / 100 % 100) / 6;
            if (!MapId.isPartyDojo(this.getMapId())) {
                pts++;
            }
            this.dojoPoints += pts;
        }
        return pts;
    }

    public void addFame(int famechange) {
        this.fame += famechange;
    }

    public void addFriendshipRing(Ring r) {
        friendshipRings.add(r);
    }

    public void addMarriageRing(Ring r) {
        marriageRing = r;
    }

    public void addMesosTraded(int gain) {
        this.mesosTraded += gain;
    }

    public void addPet(Pet pet) {
        petLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (pets[i] == null) {
                    pets[i] = pet;
                    return;
                }
            }
        } finally {
            petLock.unlock();
        }
    }

    public void addSummon(int id, Summon summon) {
        summons.put(id, summon);

        if (summon.isPuppet()) {
            map.addPlayerPuppet(this);
        }
    }

    public void addVisibleMapObject(MapObject mo) {
        visibleMapObjects.add(mo);
    }

    public int calculateMaxBaseDamage(int watk, WeaponType weapon) {
        int mainstat, secondarystat;
        if (getJob().isA(Job.THIEF) && weapon == WeaponType.DAGGER_OTHER) {
            weapon = WeaponType.DAGGER_THIEVES;
        }

        if (weapon == WeaponType.BOW || weapon == WeaponType.CROSSBOW || weapon == WeaponType.GUN) {
            mainstat = localdex;
            secondarystat = localstr;
        } else if (weapon == WeaponType.CLAW || weapon == WeaponType.DAGGER_THIEVES) {
            mainstat = localluk;
            secondarystat = localdex + localstr;
        } else {
            mainstat = localstr;
            secondarystat = localdex;
        }
        return (int) Math.ceil(((weapon.getMaxDamageMultiplier() * mainstat + secondarystat) / 100.0) * watk);
    }

    public int calculateMaxBaseDamage(int watk) {
        int maxbasedamage;
        Item weapon_item = getInventory(InventoryType.EQUIPPED).getItem((short) -11);
        if (weapon_item != null) {
            maxbasedamage = calculateMaxBaseDamage(watk, ItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId()));
        } else {
            if (job.isA(Job.PIRATE) || job.isA(Job.THUNDERBREAKER1)) {
                double weapMulti = 3;
                if (job.getId() % 100 != 0) {
                    weapMulti = 4.2;
                }

                int attack = (int) Math.min(Math.floor((2 * getLevel() + 31) / 3), 31);
                maxbasedamage = (int) Math.ceil((localstr * weapMulti + localdex) * attack / 100.0);
            } else {
                maxbasedamage = 1;
            }
        }
        return maxbasedamage;
    }

    public int calculateMaxBaseMagicDamage(int matk) {
        int maxbasedamage = matk;
        int totalint = getTotalInt();

        if (totalint > 2000) {
            maxbasedamage -= 2000;
            maxbasedamage += (int) ((0.09033024267 * totalint) + 3823.8038);
        } else {
            maxbasedamage -= totalint;

            if (totalint > 1700) {
                maxbasedamage += (int) (0.1996049769 * Math.pow(totalint, 1.300631341));
            } else {
                maxbasedamage += (int) (0.1996049769 * Math.pow(totalint, 1.290631341));
            }
        }

        return (maxbasedamage * 107) / 100;
    }

    public void setCombo(short count) {
        if (count < combocounter) {
            cancelEffectFromBuffStat(BuffStat.ARAN_COMBO);
        }
        combocounter = (short) Math.min(30000, count);
        if (count > 0) {
            sendPacket(PacketCreator.showCombo(combocounter));
        }
    }

    public void setLastCombo(long time) {
        lastcombo = time;
    }

    public short getCombo() {
        return combocounter;
    }

    public long getLastCombo() {
        return lastcombo;
    }

    public int getLastMobCount() { //Used for skills that have mobCount at 1. (a/b)
        return lastmobcount;
    }

    public void setLastMobCount(byte count) {
        lastmobcount = count;
    }

    public boolean cannotEnterCashShop() {
        return blockCashShop;
    }

    public void toggleBlockCashShop() {
        blockCashShop = !blockCashShop;
    }

    public void toggleExpGain() {
        allowExpGain = !allowExpGain;
    }

    public void setClient(Client c) {
        this.client = c;
    }

    public void newClient(Client c) {
        this.loggedIn = true;
        c.setAccountName(this.client.getAccountName());//No null's for accountName
        this.setClient(c);
        this.map = c.getChannelServer().getMapFactory().getMap(getMapId());
        Portal portal = map.findClosestPlayerSpawnpoint(getPosition());
        if (portal == null) {
            portal = map.getPortal(0);
        }
        this.setPosition(portal.getPosition());
        this.initialSpawnPoint = portal.getId();
    }

    public String getMedalText() {
        String medal = "";
        final Item medalItem = getInventory(InventoryType.EQUIPPED).getItem((short) -49);
        if (medalItem != null) {
            medal = "<" + ItemInformationProvider.getInstance().getName(medalItem.getItemId()) + "> ";
        }
        return medal;
    }

    public void Hide(boolean hide, boolean login) {
        if (isGM() && hide != this.hidden) {
            if (!hide) {
                this.hidden = false;
                sendPacket(PacketCreator.getGMEffect(0x10, (byte) 0));
                List<BuffStat> dsstat = Collections.singletonList(BuffStat.DARKSIGHT);
                getMap().broadcastGMMessage(this, PacketCreator.cancelForeignBuff(id, dsstat), false);
                getMap().broadcastSpawnPlayerMapObjectMessage(this, this, false);

                for (Summon ms : this.getSummonsValues()) {
                    getMap().broadcastNONGMMessage(this, PacketCreator.spawnSummon(ms, false), false);
                }

                for (MapObject mo : this.getMap().getMonsters()) {
                    Monster m = (Monster) mo;
                    m.aggroUpdateController();
                }
            } else {
                this.hidden = true;
                sendPacket(PacketCreator.getGMEffect(0x10, (byte) 1));
                if (!login) {
                    getMap().broadcastNONGMMessage(this, PacketCreator.removePlayerFromMap(getId()), false);
                }
                List<Pair<BuffStat, Integer>> ldsstat = Collections.singletonList(new Pair<BuffStat, Integer>(BuffStat.DARKSIGHT, 0));
                getMap().broadcastGMMessage(this, PacketCreator.giveForeignBuff(id, ldsstat), false);
                this.releaseControlledMonsters();
            }
            sendPacket(PacketCreator.enableActions());
        }
    }

    public void Hide(boolean hide) {
        Hide(hide, false);
    }

    public void toggleHide(boolean login) {
        Hide(!hidden);
    }

    public void cancelMagicDoor() {
        List<BuffStatValueHolder> mbsvhList = getAllStatups();
        for (BuffStatValueHolder mbsvh : mbsvhList) {
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    private void cancelPlayerBuffs(List<BuffStat> buffstats) {
        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
            updateLocalStats();
            sendPacket(PacketCreator.cancelBuff(buffstats));
            if (buffstats.size() > 0) {
                getMap().broadcastMessage(this, PacketCreator.cancelForeignBuff(getId(), buffstats), false);
            }
        }
    }

    public static boolean canCreateChar(String name) {
        String lname = name.toLowerCase();
        for (String nameTest : BLOCKED_NAMES) {
            if (lname.contains(nameTest)) {
                return false;
            }
        }
        return getIdByName(name) < 0 && Pattern.compile("[a-zA-Z0-9]{3,12}").matcher(name).matches();
    }

    public boolean canDoor() {
        Door door = getPlayerDoor();
        return door == null || (door.isActive() && door.getElapsedDeployTime() > 5000);
    }

    public void setHasSandboxItem() {
        hasSandboxItem = true;
    }

    public void removeSandboxItems() {  // sandbox idea thanks to Morty
        if (!hasSandboxItem) {
            return;
        }

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (InventoryType invType : InventoryType.values()) {
            Inventory inv = this.getInventory(invType);

            inv.lockInventory();
            try {
                for (Item item : new ArrayList<>(inv.list())) {
                    if (InventoryManipulator.isSandboxItem(item)) {
                        InventoryManipulator.removeFromSlot(client, invType, item.getPosition(), item.getQuantity(), false);
                        dropMessage(5, "[" + ii.getName(item.getItemId()) + "] has passed its trial conditions and will be removed from your inventory.");
                    }
                }
            } finally {
                inv.unlockInventory();
            }
        }

        hasSandboxItem = false;
    }

    public FameStatus canGiveFame(Character from) {
        if (this.isGM()) {
            return FameStatus.OK;
        } else if (lastfametime >= System.currentTimeMillis() - 3600000 * 24) {
            return FameStatus.NOT_TODAY;
        } else if (lastmonthfameids.contains(Integer.valueOf(from.getId()))) {
            return FameStatus.NOT_THIS_MONTH;
        } else {
            return FameStatus.OK;
        }
    }

    public void changeCI(int type) {
        this.ci = type;
    }

    public void setMasteries(int jobId) {
        int[] skills = new int[4];
        for (int i = 0; i > skills.length; i++) {
            skills[i] = 0; //that initialization meng
        }
        if (jobId == 112) {
            skills[0] = Hero.ACHILLES;
            skills[1] = Hero.MONSTER_MAGNET;
            skills[2] = Hero.BRANDISH;
        } else if (jobId == 122) {
            skills[0] = Paladin.ACHILLES;
            skills[1] = Paladin.MONSTER_MAGNET;
            skills[2] = Paladin.BLAST;
        } else if (jobId == 132) {
            skills[0] = DarkKnight.BEHOLDER;
            skills[1] = DarkKnight.ACHILLES;
            skills[2] = DarkKnight.MONSTER_MAGNET;
        } else if (jobId == 212) {
            skills[0] = FPArchMage.BIG_BANG;
            skills[1] = FPArchMage.MANA_REFLECTION;
            skills[2] = FPArchMage.PARALYZE;
        } else if (jobId == 222) {
            skills[0] = ILArchMage.BIG_BANG;
            skills[1] = ILArchMage.MANA_REFLECTION;
            skills[2] = ILArchMage.CHAIN_LIGHTNING;
        } else if (jobId == 232) {
            skills[0] = Bishop.BIG_BANG;
            skills[1] = Bishop.MANA_REFLECTION;
            skills[2] = Bishop.HOLY_SHIELD;
        } else if (jobId == 312) {
            skills[0] = Bowmaster.BOW_EXPERT;
            skills[1] = Bowmaster.HAMSTRING;
            skills[2] = Bowmaster.SHARP_EYES;
        } else if (jobId == 322) {
            skills[0] = Marksman.MARKSMAN_BOOST;
            skills[1] = Marksman.BLIND;
            skills[2] = Marksman.SHARP_EYES;
        } else if (jobId == 412) {
            skills[0] = NightLord.SHADOW_STARS;
            skills[1] = NightLord.SHADOW_SHIFTER;
            skills[2] = NightLord.VENOMOUS_STAR;
        } else if (jobId == 422) {
            skills[0] = Shadower.SHADOW_SHIFTER;
            skills[1] = Shadower.VENOMOUS_STAB;
            skills[2] = Shadower.BOOMERANG_STEP;
        } else if (jobId == 512) {
            skills[0] = Buccaneer.BARRAGE;
            skills[1] = Buccaneer.ENERGY_ORB;
            skills[2] = Buccaneer.SPEED_INFUSION;
            skills[3] = Buccaneer.DRAGON_STRIKE;
        } else if (jobId == 522) {
            skills[0] = Corsair.ELEMENTAL_BOOST;
            skills[1] = Corsair.BULLSEYE;
            skills[2] = Corsair.WRATH_OF_THE_OCTOPI;
            skills[3] = Corsair.RAPID_FIRE;
        } else if (jobId == 2112) {
            skills[0] = Aran.OVER_SWING;
            skills[1] = Aran.HIGH_MASTERY;
            skills[2] = Aran.FREEZE_STANDING;
        } else if (jobId == 2217) {
            skills[0] = Evan.MAPLE_WARRIOR;
            skills[1] = Evan.ILLUSION;
        } else if (jobId == 2218) {
            skills[0] = Evan.BLESSING_OF_THE_ONYX;
            skills[1] = Evan.BLAZE;
        }
        for (Integer skillId : skills) {
            if (skillId != 0) {
                Skill skill = SkillFactory.getSkill(skillId);
                final int skilllevel = getSkillLevel(skill);
                if (skilllevel > 0) {
                    continue;
                }

                changeSkillLevel(skill, (byte) 0, 10, -1);
            }
        }
    }

    private void broadcastChangeJob() {
        for (Character chr : map.getAllPlayers()) {
            Client chrC = chr.getClient();

            if (chrC != null) {     // propagate new job 3rd-person effects (FJ, Aran 1st strike, etc)
                this.sendDestroyData(chrC);
                this.sendSpawnData(chrC);
            }
        }

        TimerManager.getInstance().schedule(new Runnable() {    // need to delay to ensure clientside has finished reloading character data
            @Override
            public void run() {
                Character thisChr = Character.this;
                MapleMap map = thisChr.getMap();

                if (map != null) {
                    map.broadcastMessage(thisChr, PacketCreator.showForeignEffect(thisChr.getId(), 8), false);
                }
            }
        }, 777);
    }

    public synchronized void changeJob(Job newJob) {
        if (newJob == null) {
            return;//the fuck you doing idiot!
        }

        if (canRecvPartySearchInvite && getParty() == null) {
            this.updatePartySearchAvailability(false);
            this.job = newJob;
            this.updatePartySearchAvailability(true);
        } else {
            this.job = newJob;
        }

        int spGain = 1;
        if (GameConstants.hasSPTable(newJob)) {
            spGain += 2;
        } else {
            if (newJob.getId() % 10 == 2) {
                spGain += 2;
            }

            if (YamlConfig.config.server.USE_ENFORCE_JOB_SP_RANGE) {
                spGain = getChangedJobSp(newJob);
            }
        }

        if (spGain > 0) {
            gainSp(spGain, GameConstants.getSkillBook(newJob.getId()), true);
        }

        // thanks xinyifly for finding out missing AP awards (AP Reset can be used as a compass)
        if (newJob.getId() % 100 >= 1) {
            if (this.isCygnus()) {
                gainAp(7, true);
            } else {
                if (YamlConfig.config.server.USE_STARTING_AP_4 || newJob.getId() % 10 >= 1) {
                    gainAp(5, true);
                }
            }
        } else {    // thanks Periwinks for noticing an AP shortage from lower levels
            if (YamlConfig.config.server.USE_STARTING_AP_4 && newJob.getId() % 1000 >= 1) {
                gainAp(4, true);
            }
        }

        if (!isGM()) {
            for (byte i = 1; i < 5; i++) {
                gainSlots(i, 4, true);
            }
        }

        int addhp = 0, addmp = 0;
        int job_ = job.getId() % 1000; // lame temp "fix"
        if (job_ == 100) {                      // 1st warrior
            addhp += Randomizer.rand(200, 250);
        } else if (job_ == 200) {               // 1st mage
            addmp += Randomizer.rand(100, 150);
        } else if (job_ % 100 == 0) {           // 1st others
            addhp += Randomizer.rand(100, 150);
            addmp += Randomizer.rand(25, 50);
        } else if (job_ > 0 && job_ < 200) {    // 2nd~4th warrior
            addhp += Randomizer.rand(300, 350);
        } else if (job_ < 300) {                // 2nd~4th mage
            addmp += Randomizer.rand(450, 500);
        } else if (job_ > 0) {                  // 2nd~4th others
            addhp += Randomizer.rand(300, 350);
            addmp += Randomizer.rand(150, 200);
        }
        
        /*
        //aran perks?
        int newJobId = newJob.getId();
        if(newJobId == 2100) {          // become aran1
            addhp += 275;
            addmp += 15;
        } else if(newJobId == 2110) {   // become aran2
            addmp += 275;
        } else if(newJobId == 2111) {   // become aran3
            addhp += 275;
            addmp += 275;
        }
        */

        effLock.lock();
        statWlock.lock();
        try {
            addMaxMPMaxHP(addhp, addmp, true);
            recalcLocalStats();

            List<Pair<Stat, Integer>> statup = new ArrayList<>(7);
            statup.add(new Pair<>(Stat.HP, hp));
            statup.add(new Pair<>(Stat.MP, mp));
            statup.add(new Pair<>(Stat.MAXHP, clientmaxhp));
            statup.add(new Pair<>(Stat.MAXMP, clientmaxmp));
            statup.add(new Pair<>(Stat.AVAILABLEAP, remainingAp));
            statup.add(new Pair<>(Stat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job.getId())]));
            statup.add(new Pair<>(Stat.JOB, job.getId()));
            sendPacket(PacketCreator.updatePlayerStats(statup, true, this));
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }

        setMPC(new PartyCharacter(this));
        silentPartyUpdate();

        if (dragon != null) {
            getMap().broadcastMessage(PacketCreator.removeDragon(dragon.getObjectId()));
            dragon = null;
        }

        if (this.guildid > 0) {
            getGuild().broadcast(PacketCreator.jobMessage(0, job.getId(), name), this.getId());
        }
        Family family = getFamily();
        if (family != null) {
            family.broadcast(PacketCreator.jobMessage(1, job.getId(), name), this.getId());
        }
        setMasteries(this.job.getId());
        guildUpdate();

        broadcastChangeJob();

        if (GameConstants.hasSPTable(newJob) && newJob.getId() != 2001) {
            if (getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
                cancelBuffStats(BuffStat.MONSTER_RIDING);
            }
            createDragon();
        }

        if (YamlConfig.config.server.USE_ANNOUNCE_CHANGEJOB) {
            if (!this.isGM()) {
                broadcastAcquaintances(6, "[" + GameConstants.ordinal(GameConstants.getJobBranch(newJob)) + " Job] " + name + " has just become a " + GameConstants.getJobName(this.job.getId()) + ".");    // thanks Vcoc for noticing job name appearing in uppercase here
            }
        }
    }

    public void broadcastAcquaintances(int type, String message) {
        broadcastAcquaintances(PacketCreator.serverNotice(type, message));
    }

    public void broadcastAcquaintances(Packet packet) {
        buddylist.broadcast(packet, getWorldServer().getPlayerStorage());
        Family family = getFamily();
        if (family != null) {
            family.broadcast(packet, id);
        }

        Guild guild = getGuild();
        if (guild != null) {
            guild.broadcast(packet, id);
        }
        
        /*
        if(partnerid > 0) {
            partner.sendPacket(packet); not yet implemented
        }
        */
        sendPacket(packet);
    }

    public void changeKeybinding(int key, KeyBinding keybinding) {
        if (keybinding.getType() != 0) {
            keymap.put(Integer.valueOf(key), keybinding);
        } else {
            keymap.remove(Integer.valueOf(key));
        }
    }

    public void changeQuickslotKeybinding(byte[] aQuickslotKeyMapped) {
        this.m_pQuickslotKeyMapped = new QuickslotBinding(aQuickslotKeyMapped);
    }

    public QuickslotBinding getQuickslotBinding() {
        return this.m_pQuickslotKeyMapped;
    }

    public void broadcastStance(int newStance) {
        setStance(newStance);
        broadcastStance();
    }

    public void broadcastStance() {
        map.broadcastMessage(this, PacketCreator.movePlayer(id, this.getIdleMovement(), AbstractAnimatedMapObject.IDLE_MOVEMENT_PACKET_LENGTH), false);
    }

    public MapleMap getWarpMap(int map) {
        MapleMap warpMap;
        EventInstanceManager eim = getEventInstance();
        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else if (this.getMonsterCarnival() != null && this.getMonsterCarnival().getEventMap().getId() == map) {
            warpMap = this.getMonsterCarnival().getEventMap();
        } else {
            warpMap = client.getChannelServer().getMapFactory().getMap(map);
        }
        return warpMap;
    }

    // for use ONLY inside OnUserEnter map scripts that requires a player to change map while still moving between maps.
    public void warpAhead(int map) {
        newWarpMap = map;
    }

    private void eventChangedMap(int map) {
        EventInstanceManager eim = getEventInstance();
        if (eim != null) {
            eim.changedMap(this, map);
        }
    }

    private void eventAfterChangedMap(int map) {
        EventInstanceManager eim = getEventInstance();
        if (eim != null) {
            eim.afterChangedMap(this, map);
        }
    }

    public void changeMapBanish(BanishInfo banishInfo) {
        if (banishInfo.msg() != null) {
            dropMessage(5, banishInfo.msg());
        }

        MapleMap map_ = getWarpMap(mapid);
        Portal portal_ = map_.getPortal(banishInfo.portal());
        changeMap(map_, portal_ != null ? portal_ : map_.getRandomPlayerSpawnpoint());
    }

    public void changeMap(int map) {
        MapleMap warpMap;
        EventInstanceManager eim = getEventInstance();

        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else {
            warpMap = client.getChannelServer().getMapFactory().getMap(map);
        }

        changeMap(warpMap, warpMap.getRandomPlayerSpawnpoint());
    }

    public void changeMap(int map, int portal) {
        MapleMap warpMap;
        EventInstanceManager eim = getEventInstance();

        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else {
            warpMap = client.getChannelServer().getMapFactory().getMap(map);
        }

        changeMap(warpMap, warpMap.getPortal(portal));
    }

    public void changeMap(int map, String portal) {
        MapleMap warpMap;
        EventInstanceManager eim = getEventInstance();

        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else {
            warpMap = client.getChannelServer().getMapFactory().getMap(map);
        }

        changeMap(warpMap, warpMap.getPortal(portal));
    }

    public void changeMap(int map, Portal portal) {
        MapleMap warpMap;
        EventInstanceManager eim = getEventInstance();

        if (eim != null) {
            warpMap = eim.getMapInstance(map);
        } else {
            warpMap = client.getChannelServer().getMapFactory().getMap(map);
        }

        changeMap(warpMap, portal);
    }

    public void changeMap(MapleMap to) {
        changeMap(to, 0);
    }

    public void changeMap(MapleMap to, int portal) {
        changeMap(to, to.getPortal(portal));
    }

    public void changeMap(final MapleMap target, Portal pto) {
        canWarpCounter++;

        eventChangedMap(target.getId());    // player can be dropped from an event here, hence the new warping target.
        MapleMap to = getWarpMap(target.getId());
        if (pto == null) {
            pto = to.getPortal(0);
        }
        changeMapInternal(to, pto.getPosition(), PacketCreator.getWarpToMap(to, pto.getId(), this));
        canWarpMap = false;

        canWarpCounter--;
        if (canWarpCounter == 0) {
            canWarpMap = true;
        }

        eventAfterChangedMap(this.getMapId());
    }

    // Only used when entering Magic Door (Priest skill).
    public void changeMap(final MapleMap target, final Point pos) {
        canWarpCounter++;

        eventChangedMap(target.getId());
        MapleMap to = getWarpMap(target.getId());
        changeMapInternal(to, pos, PacketCreator.getWarpToMap(to, 0x80, pos, this));
        canWarpMap = false;

        canWarpCounter--;
        if (canWarpCounter == 0) {
            canWarpMap = true;
        }

        eventAfterChangedMap(this.getMapId());
    }

    public void forceChangeMap(final MapleMap target, Portal pto) {
        // will actually enter the map given as parameter, regardless of being an eventmap or whatnot

        canWarpCounter++;
        eventChangedMap(MapId.NONE);

        EventInstanceManager mapEim = target.getEventInstance();
        if (mapEim != null) {
            EventInstanceManager playerEim = this.getEventInstance();
            if (playerEim != null) {
                playerEim.exitPlayer(this);
                if (playerEim.getPlayerCount() == 0) {
                    playerEim.dispose();
                }
            }

            // thanks Thora for finding an issue with players not being actually warped into the target event map (rather sent to the event starting map)
            mapEim.registerPlayer(this, false);
        }

        MapleMap to = target; // warps directly to the target intead of the target's map id, this allows GMs to patrol players inside instances.
        if (pto == null) {
            pto = to.getPortal(0);
        }
        changeMapInternal(to, pto.getPosition(), PacketCreator.getWarpToMap(to, pto.getId(), this));
        canWarpMap = false;

        canWarpCounter--;
        if (canWarpCounter == 0) {
            canWarpMap = true;
        }

        eventAfterChangedMap(this.getMapId());
    }

    private boolean buffMapProtection() {
        int thisMapid = mapid;
        int returnMapid = client.getChannelServer().getMapFactory().getMap(thisMapid).getReturnMapId();

        effLock.lock();
        chrLock.lock();
        try {
            for (Entry<BuffStat, BuffStatValueHolder> mbs : effects.entrySet()) {
                if (mbs.getKey() == BuffStat.MAP_PROTECTION) {
                    byte value = (byte) mbs.getValue().value;

                    if (value == 1 && ((returnMapid == MapId.EL_NATH && thisMapid != MapId.ORBIS_TOWER_BOTTOM) || returnMapid == MapId.INTERNET_CAFE)) {
                        return true;        //protection from cold
                    } else {
                        return value == 2 && (returnMapid == MapId.AQUARIUM || thisMapid == MapId.ORBIS_TOWER_BOTTOM);        //breathing underwater
                    }
                }
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }

        for (Item it : this.getInventory(InventoryType.EQUIPPED).list()) {
            if ((it.getFlag() & ItemConstants.COLD) == ItemConstants.COLD &&
                    ((returnMapid == MapId.EL_NATH && thisMapid != MapId.ORBIS_TOWER_BOTTOM) || returnMapid == MapId.INTERNET_CAFE)) {
                return true;        //protection from cold
            }
        }

        return false;
    }

    public List<Integer> getLastVisitedMapids() {
        List<Integer> lastVisited = new ArrayList<>(5);

        petLock.lock();
        try {
            for (WeakReference<MapleMap> lv : lastVisitedMaps) {
                MapleMap lvm = lv.get();

                if (lvm != null) {
                    lastVisited.add(lvm.getId());
                }
            }
        } finally {
            petLock.unlock();
        }

        return lastVisited;
    }

    public void partyOperationUpdate(Party party, List<Character> exPartyMembers) {
        List<WeakReference<MapleMap>> mapids;

        petLock.lock();
        try {
            mapids = new LinkedList<>(lastVisitedMaps);
        } finally {
            petLock.unlock();
        }

        List<Character> partyMembers = new LinkedList<>();
        for (Character mc : (exPartyMembers != null) ? exPartyMembers : this.getPartyMembersOnline()) {
            if (mc.isLoggedinWorld()) {
                partyMembers.add(mc);
            }
        }

        Character partyLeaver = null;
        if (exPartyMembers != null) {
            partyMembers.remove(this);
            partyLeaver = this;
        }

        MapleMap map = this.getMap();
        List<MapItem> partyItems = null;

        int partyId = exPartyMembers != null ? -1 : this.getPartyId();
        for (WeakReference<MapleMap> mapRef : mapids) {
            MapleMap mapObj = mapRef.get();

            if (mapObj != null) {
                List<MapItem> partyMapItems = mapObj.updatePlayerItemDropsToParty(partyId, id, partyMembers, partyLeaver);
                if (map.hashCode() == mapObj.hashCode()) {
                    partyItems = partyMapItems;
                }
            }
        }

        if (partyItems != null && exPartyMembers == null) {
            map.updatePartyItemDropsToNewcomer(this, partyItems);
        }

        updatePartyTownDoors(party, this, partyLeaver, partyMembers);
    }

    private static void addPartyPlayerDoor(Character target) {
        Door targetDoor = target.getPlayerDoor();
        if (targetDoor != null) {
            target.applyPartyDoor(targetDoor, true);
        }
    }

    private static void removePartyPlayerDoor(Party party, Character target) {
        target.removePartyDoor(party);
    }

    private static void updatePartyTownDoors(Party party, Character target, Character partyLeaver, List<Character> partyMembers) {
        if (partyLeaver != null) {
            removePartyPlayerDoor(party, target);
        } else {
            addPartyPlayerDoor(target);
        }

        Map<Integer, Door> partyDoors = null;
        if (!partyMembers.isEmpty()) {
            partyDoors = party.getDoors();

            for (Character pchr : partyMembers) {
                Door door = partyDoors.get(pchr.getId());
                if (door != null) {
                    door.updateDoorPortal(pchr);
                }
            }

            for (Door door : partyDoors.values()) {
                for (Character pchar : partyMembers) {
                    DoorObject mdo = door.getTownDoor();
                    mdo.sendDestroyData(pchar.getClient(), true);
                    pchar.removeVisibleMapObject(mdo);
                }
            }

            if (partyLeaver != null) {
                Collection<Door> leaverDoors = partyLeaver.getDoors();
                for (Door door : leaverDoors) {
                    for (Character pchar : partyMembers) {
                        DoorObject mdo = door.getTownDoor();
                        mdo.sendDestroyData(pchar.getClient(), true);
                        pchar.removeVisibleMapObject(mdo);
                    }
                }
            }

            List<Integer> histMembers = party.getMembersSortedByHistory();
            for (Integer chrid : histMembers) {
                Door door = partyDoors.get(chrid);

                if (door != null) {
                    for (Character pchar : partyMembers) {
                        DoorObject mdo = door.getTownDoor();
                        mdo.sendSpawnData(pchar.getClient());
                        pchar.addVisibleMapObject(mdo);
                    }
                }
            }
        }

        if (partyLeaver != null) {
            Collection<Door> leaverDoors = partyLeaver.getDoors();

            if (partyDoors != null) {
                for (Door door : partyDoors.values()) {
                    DoorObject mdo = door.getTownDoor();
                    mdo.sendDestroyData(partyLeaver.getClient(), true);
                    partyLeaver.removeVisibleMapObject(mdo);
                }
            }

            for (Door door : leaverDoors) {
                DoorObject mdo = door.getTownDoor();
                mdo.sendDestroyData(partyLeaver.getClient(), true);
                partyLeaver.removeVisibleMapObject(mdo);
            }

            for (Door door : leaverDoors) {
                door.updateDoorPortal(partyLeaver);

                DoorObject mdo = door.getTownDoor();
                mdo.sendSpawnData(partyLeaver.getClient());
                partyLeaver.addVisibleMapObject(mdo);
            }
        }
    }

    private Integer getVisitedMapIndex(MapleMap map) {
        int idx = 0;

        for (WeakReference<MapleMap> mapRef : lastVisitedMaps) {
            if (map.equals(mapRef.get())) {
                return idx;
            }

            idx++;
        }

        return -1;
    }

    public void visitMap(MapleMap map) {
        petLock.lock();
        try {
            int idx = getVisitedMapIndex(map);

            if (idx == -1) {
                if (lastVisitedMaps.size() == YamlConfig.config.server.MAP_VISITED_SIZE) {
                    lastVisitedMaps.remove(0);
                }
            } else {
                WeakReference<MapleMap> mapRef = lastVisitedMaps.remove(idx);
                lastVisitedMaps.add(mapRef);
                return;
            }

            lastVisitedMaps.add(new WeakReference<>(map));
        } finally {
            petLock.unlock();
        }
    }

    public void setOwnedMap(MapleMap map) {
        ownedMap = new WeakReference<>(map);
    }

    public MapleMap getOwnedMap() {
        return ownedMap.get();
    }

    public void notifyMapTransferToPartner(int mapid) {
        if (partnerId > 0) {
            final Character partner = getWorldServer().getPlayerStorage().getCharacterById(partnerId);
            if (partner != null && !partner.isAwayFromWorld()) {
                partner.sendPacket(WeddingPackets.OnNotifyWeddingPartnerTransfer(id, mapid));
            }
        }
    }

    public void removeIncomingInvites() {
        InviteCoordinator.removePlayerIncomingInvites(id);
    }

    private void changeMapInternal(final MapleMap to, final Point pos, Packet warpPacket) {
        if (!canWarpMap) {
            return;
        }

        this.mapTransitioning.set(true);

        this.unregisterChairBuff();
        Trade.cancelTrade(this, Trade.TradeResult.UNSUCCESSFUL_ANOTHER_MAP);
        this.closePlayerInteractions();

        Party e = null;
        if (this.getParty() != null && this.getParty().getEnemy() != null) {
            e = this.getParty().getEnemy();
        }
        final Party k = e;

        sendPacket(warpPacket);
        map.removePlayer(this);
        if (client.getChannelServer().getPlayerStorage().getCharacterById(id) == null) {
            log.warn("Chr {} got stuck when moving to map {}", getName(), map.getId());
            return;
        }

        map = to;
        setPosition(pos);
        map.addPlayer(this);
        visitMap(map);

        prtLock.lock();
        try {
            if (party != null) {
                mpc.setMapId(to.getId());
                sendPacket(PacketCreator.updateParty(client.getChannel(), party, PartyOperation.SILENT_UPDATE, null));
                updatePartyMemberHPInternal();
            }
        } finally {
            prtLock.unlock();
        }
        if (Character.this.getParty() != null) {
            Character.this.getParty().setEnemy(k);
        }
        silentPartyUpdateInternal(getParty());  // EIM script calls inside

        notifyMapTransferToPartner(map.getId());

        //alas, new map has been specified when a warping was being processed...
        if (newWarpMap != -1) {
            canWarpMap = true;

            int temp = newWarpMap;
            newWarpMap = -1;
            changeMap(temp);
        } else {
            // if this event map has a gate already opened, render it
            EventInstanceManager eim = getEventInstance();
            if (eim != null) {
                eim.recoverOpenedGate(this, map.getId());
            }

            // if this map has obstacle components moving, make it do so for this client
            sendPacket(PacketCreator.environmentMoveList(map.getEnvironment().entrySet()));
        }
    }

    public boolean isChangingMaps() {
        return this.mapTransitioning.get();
    }

    public void setMapTransitionComplete() {
        this.mapTransitioning.set(false);
    }

    public void changePage(int page) {
        this.currentPage = page;
    }

    public void changeSkillLevel(Skill skill, byte newLevel, int newMasterlevel, long expiration) {
        if (newLevel > -1) {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
            if (!GameConstants.isHiddenSkills(skill.getId())) {
                sendPacket(PacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, expiration));
            }
        } else {
            skills.remove(skill);
            sendPacket(PacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, -1)); //Shouldn't use expiration anymore :)
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM skills WHERE skillid = ? AND characterid = ?")) {
                ps.setInt(1, skill.getId());
                ps.setInt(2, id);
                ps.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void changeTab(int tab) {
        this.currentTab = tab;
    }

    public void changeType(int type) {
        this.currentType = type;
    }

    public void checkBerserk(final boolean isHidden) {
        if (berserkSchedule != null) {
            berserkSchedule.cancel(false);
        }
        final Character chr = this;
        if (job.equals(Job.DARKKNIGHT)) {
            Skill BerserkX = SkillFactory.getSkill(DarkKnight.BERSERK);
            final int skilllevel = getSkillLevel(BerserkX);
            if (skilllevel > 0) {
                berserk = chr.getHp() * 100 / chr.getCurrentMaxHp() < BerserkX.getEffect(skilllevel).getX();
                berserkSchedule = TimerManager.getInstance().register(new Runnable() {
                    @Override
                    public void run() {
                        if (awayFromWorld.get()) {
                            return;
                        }

                        sendPacket(PacketCreator.showOwnBerserk(skilllevel, berserk));
                        if (!isHidden) {
                            getMap().broadcastMessage(Character.this, PacketCreator.showBerserk(getId(), skilllevel, berserk), false);
                        } else {
                            getMap().broadcastGMMessage(Character.this, PacketCreator.showBerserk(getId(), skilllevel, berserk), false);
                        }
                    }
                }, 5000, 3000);
            }
        }
    }

    public void checkMessenger() {
        if (messenger != null && messengerposition < 4 && messengerposition > -1) {
            World worldz = getWorldServer();
            worldz.silentJoinMessenger(messenger.getId(), new MessengerCharacter(this, messengerposition), messengerposition);
            worldz.updateMessenger(getMessenger().getId(), name, client.getChannel());
        }
    }

    public void controlMonster(Monster monster) {
        if (cpnLock.tryLock()) {
            try {
                controlled.add(monster);
            } finally {
                cpnLock.unlock();
            }
        }
    }

    public void stopControllingMonster(Monster monster) {
        if (cpnLock.tryLock()) {
            try {
                controlled.remove(monster);
            } finally {
                cpnLock.unlock();
            }
        }
    }

    public int getNumControlledMonsters() {
        cpnLock.lock();
        try {
            return controlled.size();
        } finally {
            cpnLock.unlock();
        }
    }

    public Collection<Monster> getControlledMonsters() {
        cpnLock.lock();
        try {
            return new ArrayList<>(controlled);
        } finally {
            cpnLock.unlock();
        }
    }

    public void releaseControlledMonsters() {
        Collection<Monster> controlledMonsters;

        cpnLock.lock();
        try {
            controlledMonsters = new ArrayList<>(controlled);
            controlled.clear();
        } finally {
            cpnLock.unlock();
        }

        for (Monster monster : controlledMonsters) {
            monster.aggroRedirectController();
        }
    }

    public boolean applyConsumeOnPickup(final int itemId) {
        if (itemId / 1000000 == 2) {
            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            if (ii.isConsumeOnPickup(itemId)) {
                if (ItemConstants.isPartyItem(itemId)) {
                    List<Character> partyMembers = this.getPartyMembersOnSameMap();
                    if (!ItemId.isPartyAllCure(itemId)) {
                        StatEffect mse = ii.getItemEffect(itemId);
                        if (!partyMembers.isEmpty()) {
                            for (Character mc : partyMembers) {
                                if (mc.isAlive()) {
                                    mse.applyTo(mc);
                                }
                            }
                        } else if (this.isAlive()) {
                            mse.applyTo(this);
                        }
                    } else {
                        if (!partyMembers.isEmpty()) {
                            for (Character mc : partyMembers) {
                                mc.dispelDebuffs();
                            }
                        } else {
                            this.dispelDebuffs();
                        }
                    }
                } else {
                    ii.getItemEffect(itemId).applyTo(this);
                }

                if (ItemId.isMonsterCard(itemId)) {
                    monsterbook.addCard(itemId, client);
                }
                return true;
            }
        }
        return false;
    }

    public final void pickupItem(MapObject ob) {
        pickupItem(ob, -1);
    }

    public final void pickupItem(MapObject ob, int petIndex) {     // yes, one picks the MapObject, not the MapItem
        if (ob == null) {                                               // pet index refers to the one picking up the item
            return;
        }

        if (ob instanceof MapItem mapitem) {
            if (System.currentTimeMillis() - mapitem.getDropTime() < 400 || !mapitem.canBePickedBy(this)) {
                sendPacket(PacketCreator.enableActions());
                return;
            }

            List<Character> mpcs = new LinkedList<>();
            if (mapitem.getMeso() > 0 && !mapitem.isPickedUp()) {
                mpcs = getPartyMembersOnSameMap();
            }

            ScriptedItem itemScript = null;
            mapitem.lockItem();
            try {
                if (mapitem.isPickedUp()) {
                    sendPacket(PacketCreator.showItemUnavailable());
                    sendPacket(PacketCreator.enableActions());
                    return;
                }

                boolean isPet = petIndex > -1;
                final Packet pickupPacket = PacketCreator.removeItemFromMap(mapitem.getObjectId(), (isPet) ? 5 : 2, this.getId(), isPet, petIndex);

                Item mItem = mapitem.getItem();
                boolean hasSpaceInventory = true;
                ItemInformationProvider ii = ItemInformationProvider.getInstance();
                if (ItemId.isNxCard(mapitem.getItemId()) || mapitem.getMeso() > 0 || ii.isConsumeOnPickup(mapitem.getItemId()) || (hasSpaceInventory = InventoryManipulator.checkSpace(client, mapitem.getItemId(), mItem.getQuantity(), mItem.getOwner()))) {
                    int mapId = this.getMapId();

                    if ((MapId.isSelfLootableOnly(mapId))) {//happyville trees and guild PQ
                        if (!mapitem.isPlayerDrop() || mapitem.getDropper().getObjectId() == client.getPlayer().getObjectId()) {
                            if (mapitem.getMeso() > 0) {
                                if (!mpcs.isEmpty()) {
                                    int mesosamm = mapitem.getMeso() / mpcs.size();
                                    for (Character partymem : mpcs) {
                                        if (partymem.isLoggedinWorld()) {
                                            partymem.gainMeso(mesosamm, true, true, false);
                                        }
                                    }
                                } else {
                                    this.gainMeso(mapitem.getMeso(), true, true, false);
                                }

                                this.getMap().pickItemDrop(pickupPacket, mapitem);
                            } else if (ItemId.isNxCard(mapitem.getItemId())) {
                                // Add NX to account, show effect and make item disappear
                                int nxGain = mapitem.getItemId() == ItemId.NX_CARD_100 ? 100 : 250;
                                this.getCashShop().gainCash(1, nxGain);

                                if (YamlConfig.config.server.USE_ANNOUNCE_NX_COUPON_LOOT) {
                                    showHint("You have earned #e#b" + nxGain + " NX#k#n. (" + this.getCashShop().getCash(CashShop.NX_CREDIT) + " NX)", 300);
                                }

                                this.getMap().pickItemDrop(pickupPacket, mapitem);
                            } else if (InventoryManipulator.addFromDrop(client, mItem, true)) {
                                this.getMap().pickItemDrop(pickupPacket, mapitem);
                            } else {
                                sendPacket(PacketCreator.enableActions());
                                return;
                            }
                        } else {
                            sendPacket(PacketCreator.showItemUnavailable());
                            sendPacket(PacketCreator.enableActions());
                            return;
                        }
                        sendPacket(PacketCreator.enableActions());
                        return;
                    }

                    if (!this.needQuestItem(mapitem.getQuest(), mapitem.getItemId())) {
                        sendPacket(PacketCreator.showItemUnavailable());
                        sendPacket(PacketCreator.enableActions());
                        return;
                    }

                    if (mapitem.getMeso() > 0) {
                        if (!mpcs.isEmpty()) {
                            int mesosamm = mapitem.getMeso() / mpcs.size();
                            for (Character partymem : mpcs) {
                                if (partymem.isLoggedinWorld()) {
                                    partymem.gainMeso(mesosamm, true, true, false);
                                }
                            }
                        } else {
                            this.gainMeso(mapitem.getMeso(), true, true, false);
                        }
                    } else if (mItem.getItemId() / 10000 == 243) {
                        ScriptedItem info = ii.getScriptedItemInfo(mItem.getItemId());
                        if (info != null && info.runOnPickup()) {
                            itemScript = info;
                        } else {
                            if (!InventoryManipulator.addFromDrop(client, mItem, true)) {
                                sendPacket(PacketCreator.enableActions());
                                return;
                            }
                        }
                    } else if (ItemId.isNxCard(mapitem.getItemId())) {
                        // Add NX to account, show effect and make item disappear
                        int nxGain = mapitem.getItemId() == ItemId.NX_CARD_100 ? 100 : 250;
                        this.getCashShop().gainCash(1, nxGain);

                        if (YamlConfig.config.server.USE_ANNOUNCE_NX_COUPON_LOOT) {
                            showHint("You have earned #e#b" + nxGain + " NX#k#n. (" + this.getCashShop().getCash(CashShop.NX_CREDIT) + " NX)", 300);
                        }
                    } else if (applyConsumeOnPickup(mItem.getItemId())) {
                    } else if (InventoryManipulator.addFromDrop(client, mItem, true)) {
                        if (mItem.getItemId() == ItemId.ARPQ_SPIRIT_JEWEL) {
                            updateAriantScore();
                        }
                    } else {
                        sendPacket(PacketCreator.enableActions());
                        return;
                    }

                    this.getMap().pickItemDrop(pickupPacket, mapitem);
                } else if (!hasSpaceInventory) {
                    sendPacket(PacketCreator.getInventoryFull());
                    sendPacket(PacketCreator.getShowInventoryFull());
                }
            } finally {
                mapitem.unlockItem();
            }

            if (itemScript != null) {
                ItemScriptManager ism = ItemScriptManager.getInstance();
                ism.runItemScript(client, itemScript);
            }
        }
        sendPacket(PacketCreator.enableActions());
    }

    public int countItem(int itemid) {
        return inventory[ItemConstants.getInventoryType(itemid).ordinal()].countById(itemid);
    }

    public boolean canHold(int itemid) {
        return canHold(itemid, 1);
    }

    public boolean canHold(int itemid, int quantity) {
        return client.getAbstractPlayerInteraction().canHold(itemid, quantity);
    }

    public boolean canHoldUniques(List<Integer> itemids) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (Integer itemid : itemids) {
            if (ii.isPickupRestricted(itemid) && this.haveItem(itemid)) {
                return false;
            }
        }

        return true;
    }

    public boolean isRidingBattleship() {
        Integer bv = getBuffedValue(BuffStat.MONSTER_RIDING);
        return bv != null && bv.equals(Corsair.BATTLE_SHIP);
    }

    public void announceBattleshipHp() {
        sendPacket(PacketCreator.skillCooldown(5221999, battleshipHp));
    }

    public void decreaseBattleshipHp(int decrease) {
        this.battleshipHp -= decrease;
        if (battleshipHp <= 0) {
            Skill battleship = SkillFactory.getSkill(Corsair.BATTLE_SHIP);
            int cooldown = battleship.getEffect(getSkillLevel(battleship)).getCooldown();
            sendPacket(PacketCreator.skillCooldown(Corsair.BATTLE_SHIP, cooldown));
            addCooldown(Corsair.BATTLE_SHIP, Server.getInstance().getCurrentTime(), SECONDS.toMillis(cooldown));
            removeCooldown(5221999);
            cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
        } else {
            announceBattleshipHp();
            addCooldown(5221999, 0, Long.MAX_VALUE);
        }
    }

    public void decreaseReports() {
        this.possibleReports--;
    }

    public void deleteGuild(int guildId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?")) {
                ps.setInt(1, guildId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void nextPendingRequest(Client c) {
        CharacterIdentity pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.sendPacket(PacketCreator.requestBuddylistAdd(pendingBuddyRequest.id(), c.getPlayer().getId(), pendingBuddyRequest.name()));
        }
    }

    private void notifyRemoteChannel(Client c, int remoteChannel, int otherCid, BuddyList.BuddyOperation operation) {
        Character player = c.getPlayer();
        if (remoteChannel != -1) {
            c.getWorldServer().buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation);
        }
    }

    public void deleteBuddy(int otherCid) {
        BuddyList bl = getBuddylist();

        if (bl.containsVisible(otherCid)) {
            notifyRemoteChannel(client, getWorldServer().find(otherCid), otherCid, BuddyList.BuddyOperation.DELETED);
        }
        bl.remove(otherCid);
        sendPacket(PacketCreator.updateBuddylist(getBuddylist().getBuddies()));
        nextPendingRequest(client);
    }

    public static boolean deleteCharFromDB(Character player, int senderAccId) {
        int cid = player.getId();
        if (!Server.getInstance().haveCharacterEntry(senderAccId, cid)) {    // thanks zera (EpiphanyMS) for pointing a critical exploit with non-authed character deletion request
            return false;
        }

        final int accId = senderAccId;
        int world = 0;
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT world FROM characters WHERE id = ?")) {
                ps.setInt(1, cid);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        world = rs.getInt("world");
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT buddyid FROM buddies WHERE characterid = ?")) {
                ps.setInt(1, cid);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int buddyid = rs.getInt("buddyid");
                        Character buddy = Server.getInstance().getWorld(world).getPlayerStorage().getCharacterById(buddyid);

                        if (buddy != null) {
                            buddy.deleteBuddy(cid);
                        }
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM buddies WHERE characterid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT threadid FROM bbs_threads WHERE postercid = ?")) {
                ps.setInt(1, cid);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int threadId = rs.getInt("threadid");

                        try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM bbs_replies WHERE threadid = ?")) {
                            ps2.setInt(1, threadId);
                            ps2.executeUpdate();
                        }
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM bbs_threads WHERE postercid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT id, guildid, guildrank, name, allianceRank FROM characters WHERE id = ? AND accountid = ?")) {
                ps.setInt(1, cid);
                ps.setInt(2, accId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt("guildid") > 0) {
                        Server.getInstance().deleteGuildCharacter(new GuildCharacter(player, cid, 0, rs.getString("name"), (byte) -1, (byte) -1, 0, rs.getInt("guildrank"), rs.getInt("guildid"), false, rs.getInt("allianceRank")));
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM wishlists WHERE charid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM cooldowns WHERE charid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM playerdiseases WHERE charid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM area_info WHERE charid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM monsterbook WHERE charid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM characters WHERE id = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM family_character WHERE cid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM famelog WHERE characterid_to = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT inventoryitemid, petid FROM inventoryitems WHERE characterid = ?")) {
                ps.setInt(1, cid);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int inventoryitemid = rs.getInt("inventoryitemid");

                        try (PreparedStatement ps2 = con.prepareStatement("SELECT ringid FROM inventoryequipment WHERE inventoryitemid = ?")) {
                            ps2.setInt(1, inventoryitemid);

                            try (ResultSet rs2 = ps2.executeQuery()) {
                                while (rs2.next()) {
                                    final int ringid = rs2.getInt("ringid");

                                    if (ringid > -1) {
                                        try (PreparedStatement ps3 = con.prepareStatement("DELETE FROM rings WHERE id = ?")) {
                                            ps3.setInt(1, ringid);
                                            ps3.executeUpdate();
                                        }

                                        CashIdGenerator.freeCashId(ringid);
                                    }
                                }
                            }
                        }

                        try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM inventoryequipment WHERE inventoryitemid = ?")) {
                            ps2.setInt(1, inventoryitemid);
                            ps2.executeUpdate();
                        }

                        final int petid = rs.getInt("petid");
                        if (!rs.wasNull()) {
                            try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM pets WHERE petid = ?")) {
                                ps2.setInt(1, petid);
                                ps2.executeUpdate();
                            }
                            CashIdGenerator.freeCashId(petid);
                        }
                    }
                }
            }

            deleteQuestProgressWhereCharacterId(con, cid);
            FredrickProcessor.removeFredrickLog(cid);   // thanks maple006 for pointing out the player's Fredrick items are not being deleted at character deletion

            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM mts_cart WHERE cid = ?")) {
                ps.setInt(1, cid);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final int mtsid = rs.getInt("id");

                        try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM mts_items WHERE id = ?")) {
                            ps2.setInt(1, mtsid);
                            ps2.executeUpdate();
                        }
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM mts_cart WHERE cid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }

            String[] toDel = {"famelog", "inventoryitems", "keymap", "queststatus", "savedlocations", "trocklocations", "skillmacros", "skills", "eventstats", "server_queue"};
            for (String s : toDel) {
                Character.deleteWhereCharacterId(con, "DELETE FROM `" + s + "` WHERE characterid = ?", cid);
            }

            Server.getInstance().deleteCharacterEntry(accId, cid);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void deleteQuestProgressWhereCharacterId(Connection con, int cid) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM medalmaps WHERE characterid = ?")) {
            ps.setInt(1, cid);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = con.prepareStatement("DELETE FROM questprogress WHERE characterid = ?")) {
            ps.setInt(1, cid);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = con.prepareStatement("DELETE FROM queststatus WHERE characterid = ?")) {
            ps.setInt(1, cid);
            ps.executeUpdate();
        }
    }

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static void deleteWhereCharacterId(Connection con, String sql, int cid) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cid);
            ps.executeUpdate();
        }
    }

    private void stopChairTask() {
        chrLock.lock();
        try {
            if (chairRecoveryTask != null) {
                chairRecoveryTask.cancel(false);
                chairRecoveryTask = null;
            }
        } finally {
            chrLock.unlock();
        }
    }

    private static Pair<Integer, Pair<Integer, Integer>> getChairTaskIntervalRate(int maxhp, int maxmp) {
        float toHeal = Math.max(maxhp, maxmp);
        float maxDuration = SECONDS.toMillis(YamlConfig.config.server.CHAIR_EXTRA_HEAL_MAX_DELAY);

        int rate = 0;
        int minRegen = 1, maxRegen = (256 * YamlConfig.config.server.CHAIR_EXTRA_HEAL_MULTIPLIER) - 1, midRegen = 1;
        while (minRegen < maxRegen) {
            midRegen = (int) ((minRegen + maxRegen) * 0.94);

            float procs = toHeal / midRegen;
            float newRate = maxDuration / procs;
            rate = (int) newRate;

            if (newRate < 420) {
                minRegen = (int) (1.2 * midRegen);
            } else if (newRate > 5000) {
                maxRegen = (int) (0.8 * midRegen);
            } else {
                break;
            }
        }

        float procs = maxDuration / rate;
        int hpRegen, mpRegen;
        if (maxhp > maxmp) {
            hpRegen = midRegen;
            mpRegen = (int) Math.ceil(maxmp / procs);
        } else {
            hpRegen = (int) Math.ceil(maxhp / procs);
            mpRegen = midRegen;
        }

        return new Pair<>(rate, new Pair<>(hpRegen, mpRegen));
    }

    private void updateChairHealStats() {
        statRlock.lock();
        try {
            if (localchairrate != -1) {
                return;
            }
        } finally {
            statRlock.unlock();
        }

        effLock.lock();
        statWlock.lock();
        try {
            Pair<Integer, Pair<Integer, Integer>> p = getChairTaskIntervalRate(localmaxhp, localmaxmp);

            localchairrate = p.getLeft();
            localchairhp = p.getRight().getLeft();
            localchairmp = p.getRight().getRight();
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }
    }

    private void startChairTask() {
        if (chair.get() < 0) {
            return;
        }

        int healInterval;
        effLock.lock();
        try {
            updateChairHealStats();
            healInterval = localchairrate;
        } finally {
            effLock.unlock();
        }

        chrLock.lock();
        try {
            if (chairRecoveryTask != null) {
                stopChairTask();
            }

            chairRecoveryTask = TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    updateChairHealStats();
                    final int healHP = localchairhp;
                    final int healMP = localchairmp;

                    if (Character.this.getHp() < localmaxhp) {
                        byte recHP = (byte) (healHP / YamlConfig.config.server.CHAIR_EXTRA_HEAL_MULTIPLIER);

                        sendPacket(PacketCreator.showOwnRecovery(recHP));
                        getMap().broadcastMessage(Character.this, PacketCreator.showRecovery(id, recHP), false);
                    } else if (Character.this.getMp() >= localmaxmp) {
                        stopChairTask();    // optimizing schedule management when player is already with full pool.
                    }

                    addMPHP(healHP, healMP);
                }
            }, healInterval, healInterval);
        } finally {
            chrLock.unlock();
        }
    }

    private void stopExtraTask() {
        chrLock.lock();
        try {
            if (extraRecoveryTask != null) {
                extraRecoveryTask.cancel(false);
                extraRecoveryTask = null;
            }
        } finally {
            chrLock.unlock();
        }
    }

    private void startExtraTask(final byte healHP, final byte healMP, final short healInterval) {
        chrLock.lock();
        try {
            startExtraTaskInternal(healHP, healMP, healInterval);
        } finally {
            chrLock.unlock();
        }
    }

    private void startExtraTaskInternal(final byte healHP, final byte healMP, final short healInterval) {
        extraRecInterval = healInterval;

        extraRecoveryTask = TimerManager.getInstance().register(new Runnable() {
            @Override
            public void run() {
                if (getBuffSource(BuffStat.HPREC) == -1 && getBuffSource(BuffStat.MPREC) == -1) {
                    stopExtraTask();
                    return;
                }

                if (Character.this.getHp() < localmaxhp) {
                    if (healHP > 0) {
                        sendPacket(PacketCreator.showOwnRecovery(healHP));
                        getMap().broadcastMessage(Character.this, PacketCreator.showRecovery(id, healHP), false);
                    }
                }

                addMPHP(healHP, healMP);
            }
        }, healInterval, healInterval);
    }

    public void disbandGuild() {
        if (guildid < 1 || guildRank != 1) {
            return;
        }
        try {
            Server.getInstance().disbandGuild(guildid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dispel() {
        if (!(YamlConfig.config.server.USE_UNDISPEL_HOLY_SHIELD && this.hasActiveBuff(Bishop.HOLY_SHIELD))) {
            List<BuffStatValueHolder> mbsvhList = getAllStatups();
            for (BuffStatValueHolder mbsvh : mbsvhList) {
                if (mbsvh.effect.isSkill()) {
                    if (mbsvh.effect.getBuffSourceId() != Aran.COMBO_ABILITY) { // check discovered thanks to Croosade dev team
                        cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                    }
                }
            }
        }
    }

    public final boolean hasDisease(final Disease dis) {
        chrLock.lock();
        try {
            return diseases.containsKey(dis);
        } finally {
            chrLock.unlock();
        }
    }

    public final int getDiseasesSize() {
        chrLock.lock();
        try {
            return diseases.size();
        } finally {
            chrLock.unlock();
        }
    }

    public Map<Disease, Pair<Long, MobSkill>> getAllDiseases() {
        chrLock.lock();
        try {
            long curtime = Server.getInstance().getCurrentTime();
            Map<Disease, Pair<Long, MobSkill>> ret = new LinkedHashMap<>();

            for (Entry<Disease, Long> de : diseaseExpires.entrySet()) {
                Pair<DiseaseValueHolder, MobSkill> dee = diseases.get(de.getKey());
                DiseaseValueHolder mdvh = dee.getLeft();

                ret.put(de.getKey(), new Pair<>(mdvh.length - (curtime - mdvh.startTime), dee.getRight()));
            }

            return ret;
        } finally {
            chrLock.unlock();
        }
    }

    public void silentApplyDiseases(Map<Disease, Pair<Long, MobSkill>> diseaseMap) {
        chrLock.lock();
        try {
            long curTime = Server.getInstance().getCurrentTime();

            for (Entry<Disease, Pair<Long, MobSkill>> di : diseaseMap.entrySet()) {
                long expTime = curTime + di.getValue().getLeft();

                diseaseExpires.put(di.getKey(), expTime);
                diseases.put(di.getKey(), new Pair<>(new DiseaseValueHolder(curTime, di.getValue().getLeft()), di.getValue().getRight()));
            }
        } finally {
            chrLock.unlock();
        }
    }

    public void announceDiseases() {
        Set<Entry<Disease, Pair<DiseaseValueHolder, MobSkill>>> chrDiseases;

        chrLock.lock();
        try {
            // Poison damage visibility and diseases status visibility, extended through map transitions thanks to Ronan
            if (!this.isLoggedinWorld()) {
                return;
            }

            chrDiseases = new LinkedHashSet<>(diseases.entrySet());
        } finally {
            chrLock.unlock();
        }

        for (Entry<Disease, Pair<DiseaseValueHolder, MobSkill>> di : chrDiseases) {
            Disease disease = di.getKey();
            MobSkill skill = di.getValue().getRight();
            final List<Pair<Disease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, Integer.valueOf(skill.getX())));

            if (disease != Disease.SLOW) {
                map.broadcastMessage(PacketCreator.giveForeignDebuff(id, debuff, skill));
            } else {
                map.broadcastMessage(PacketCreator.giveForeignSlowDebuff(id, debuff, skill));
            }
        }
    }

    public void collectDiseases() {
        for (Character chr : map.getAllPlayers()) {
            int cid = chr.getId();

            for (Entry<Disease, Pair<Long, MobSkill>> di : chr.getAllDiseases().entrySet()) {
                Disease disease = di.getKey();
                MobSkill skill = di.getValue().getRight();
                final List<Pair<Disease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, Integer.valueOf(skill.getX())));

                if (disease != Disease.SLOW) {
                    this.sendPacket(PacketCreator.giveForeignDebuff(cid, debuff, skill));
                } else {
                    this.sendPacket(PacketCreator.giveForeignSlowDebuff(cid, debuff, skill));
                }
            }
        }
    }

    public void giveDebuff(final Disease disease, MobSkill skill) {
        if (!hasDisease(disease) && getDiseasesSize() < 2) {
            if (!(disease == Disease.SEDUCE || disease == Disease.STUN)) {
                if (hasActiveBuff(Bishop.HOLY_SHIELD)) {
                    return;
                }
            }

            chrLock.lock();
            try {
                long curTime = Server.getInstance().getCurrentTime();
                diseaseExpires.put(disease, curTime + skill.getDuration());
                diseases.put(disease, new Pair<>(new DiseaseValueHolder(curTime, skill.getDuration()), skill));
            } finally {
                chrLock.unlock();
            }

            if (disease == Disease.SEDUCE && chair.get() < 0) {
                sitChair(-1);
            }

            final List<Pair<Disease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, Integer.valueOf(skill.getX())));
            sendPacket(PacketCreator.giveDebuff(debuff, skill));

            if (disease != Disease.SLOW) {
                map.broadcastMessage(this, PacketCreator.giveForeignDebuff(id, debuff, skill), false);
            } else {
                map.broadcastMessage(this, PacketCreator.giveForeignSlowDebuff(id, debuff, skill), false);
            }
        }
    }

    public void dispelDebuff(Disease debuff) {
        if (hasDisease(debuff)) {
            long mask = debuff.getValue();
            sendPacket(PacketCreator.cancelDebuff(mask));

            if (debuff != Disease.SLOW) {
                map.broadcastMessage(this, PacketCreator.cancelForeignDebuff(id, mask), false);
            } else {
                map.broadcastMessage(this, PacketCreator.cancelForeignSlowDebuff(id), false);
            }

            chrLock.lock();
            try {
                diseases.remove(debuff);
                diseaseExpires.remove(debuff);
            } finally {
                chrLock.unlock();
            }
        }
    }

    public void dispelDebuffs() {
        dispelDebuff(Disease.CURSE);
        dispelDebuff(Disease.DARKNESS);
        dispelDebuff(Disease.POISON);
        dispelDebuff(Disease.SEAL);
        dispelDebuff(Disease.WEAKEN);
        dispelDebuff(Disease.SLOW);    // thanks Conrad for noticing ZOMBIFY isn't dispellable
    }

    public void purgeDebuffs() {
        dispelDebuff(Disease.SEDUCE);
        dispelDebuff(Disease.ZOMBIFY);
        dispelDebuff(Disease.CONFUSE);
        dispelDebuffs();
    }

    public void cancelAllDebuffs() {
        chrLock.lock();
        try {
            diseases.clear();
            diseaseExpires.clear();
        } finally {
            chrLock.unlock();
        }
    }

    public void dispelSkill(int skillid) {
        List<BuffStatValueHolder> allBuffs = getAllStatups();
        for (BuffStatValueHolder mbsvh : allBuffs) {
            if (skillid == 0) {
                if (mbsvh.effect.isSkill() && (mbsvh.effect.getSourceId() % 10000000 == 1004 || dispelSkills(mbsvh.effect.getSourceId()))) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            } else if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    private static boolean dispelSkills(int skillid) {
        switch (skillid) {
            case DarkKnight.BEHOLDER:
            case FPArchMage.ELQUINES:
            case ILArchMage.IFRIT:
            case Priest.SUMMON_DRAGON:
            case Bishop.BAHAMUT:
            case Ranger.PUPPET:
            case Ranger.SILVER_HAWK:
            case Sniper.PUPPET:
            case Sniper.GOLDEN_EAGLE:
            case Hermit.SHADOW_PARTNER:
                return true;
            default:
                return false;
        }
    }

    public void changeFaceExpression(int emote) {
        long timeNow = Server.getInstance().getCurrentTime();
        // Client allows changing every 2 seconds. Give it a little bit of overhead for packet delays.
        if (timeNow - lastExpression > 1500) {
            lastExpression = timeNow;
            getMap().broadcastMessage(this, PacketCreator.facialExpression(this, emote), false);
        }
    }

    public void doHurtHp() {
        if (!(this.getInventory(InventoryType.EQUIPPED).findById(getMap().getHPDecProtect()) != null || buffMapProtection())) {
            addHP(-getMap().getHPDec());
        }
    }

    public void dropMessage(String message) {
        dropMessage(0, message);
    }

    public void dropMessage(int type, String message) {
        sendPacket(PacketCreator.serverNotice(type, message));
    }

    public void enteredScript(String script, int mapid) {
        if (!entered.containsKey(mapid)) {
            entered.put(mapid, script);
        }
    }

    public void equipChanged() {
        getMap().broadcastUpdateCharLookMessage(this, this);
        equipchanged = true;
        updateLocalStats();
        if (getMessenger() != null) {
            getWorldServer().updateMessenger(getMessenger(), getName(), getWorld(), client.getChannel());
        }
    }

    public void cancelDiseaseExpireTask() {
        if (diseaseExpireTask != null) {
            diseaseExpireTask.cancel(false);
            diseaseExpireTask = null;
        }
    }

    public void diseaseExpireTask() {
        if (diseaseExpireTask == null) {
            diseaseExpireTask = TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    Set<Disease> toExpire = new LinkedHashSet<>();

                    chrLock.lock();
                    try {
                        long curTime = Server.getInstance().getCurrentTime();

                        for (Entry<Disease, Long> de : diseaseExpires.entrySet()) {
                            if (de.getValue() < curTime) {
                                toExpire.add(de.getKey());
                            }
                        }
                    } finally {
                        chrLock.unlock();
                    }

                    for (Disease d : toExpire) {
                        dispelDebuff(d);
                    }
                }
            }, 1500);
        }
    }

    public void cancelBuffExpireTask() {
        if (buffExpireTask != null) {
            buffExpireTask.cancel(false);
            buffExpireTask = null;
        }
    }

    public void buffExpireTask() {
        if (buffExpireTask == null) {
            buffExpireTask = TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    Set<Entry<Integer, Long>> es;
                    List<BuffStatValueHolder> toCancel = new ArrayList<>();

                    effLock.lock();
                    chrLock.lock();
                    try {
                        es = new LinkedHashSet<>(buffExpires.entrySet());

                        long curTime = Server.getInstance().getCurrentTime();
                        for (Entry<Integer, Long> bel : es) {
                            if (curTime >= bel.getValue()) {
                                toCancel.add(buffEffects.get(bel.getKey()).entrySet().iterator().next().getValue());    //rofl
                            }
                        }
                    } finally {
                        chrLock.unlock();
                        effLock.unlock();
                    }

                    for (BuffStatValueHolder mbsvh : toCancel) {
                        cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                    }
                }
            }, 1500);
        }
    }

    public void cancelSkillCooldownTask() {
        if (skillCooldownTask != null) {
            skillCooldownTask.cancel(false);
            skillCooldownTask = null;
        }
    }

    public void skillCooldownTask() {
        if (skillCooldownTask == null) {
            skillCooldownTask = TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    Set<Entry<Integer, CooldownValueHolder>> es;

                    effLock.lock();
                    chrLock.lock();
                    try {
                        es = new LinkedHashSet<>(coolDowns.entrySet());
                    } finally {
                        chrLock.unlock();
                        effLock.unlock();
                    }

                    long curTime = Server.getInstance().getCurrentTime();
                    for (Entry<Integer, CooldownValueHolder> bel : es) {
                        CooldownValueHolder mcdvh = bel.getValue();
                        if (curTime >= mcdvh.startTime + mcdvh.length) {
                            removeCooldown(mcdvh.skillId);
                            sendPacket(PacketCreator.skillCooldown(mcdvh.skillId, 0));
                        }
                    }
                }
            }, 1500);
        }
    }

    public void cancelExpirationTask() {
        if (itemExpireTask != null) {
            itemExpireTask.cancel(false);
            itemExpireTask = null;
        }
    }

    public void expirationTask() {
        if (itemExpireTask == null) {
            itemExpireTask = TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    boolean deletedCoupon = false;

                    long expiration, currenttime = System.currentTimeMillis();
                    Set<Skill> keys = getSkills().keySet();
                    for (Iterator<Skill> i = keys.iterator(); i.hasNext(); ) {
                        Skill key = i.next();
                        SkillEntry skill = getSkills().get(key);
                        if (skill.expiration != -1 && skill.expiration < currenttime) {
                            changeSkillLevel(key, (byte) -1, 0, -1);
                        }
                    }

                    List<Item> toberemove = new ArrayList<>();
                    for (Inventory inv : inventory) {
                        for (Item item : inv.list()) {
                            expiration = item.getExpiration();

                            if (expiration != -1 && (expiration < currenttime) && ((item.getFlag() & ItemConstants.LOCK) == ItemConstants.LOCK)) {
                                short lock = item.getFlag();
                                lock &= ~(ItemConstants.LOCK);
                                item.setFlag(lock); //Probably need a check, else people can make expiring items into permanent items...
                                item.setExpiration(-1);
                                forceUpdateItem(item);   //TEST :3
                            } else if (expiration != -1 && expiration < currenttime) {
                                if (!ItemConstants.isPet(item.getItemId())) {
                                    sendPacket(PacketCreator.itemExpired(item.getItemId()));
                                    toberemove.add(item);
                                    if (ItemConstants.isRateCoupon(item.getItemId())) {
                                        deletedCoupon = true;
                                    }
                                } else {
                                    Pet pet = item.getPet();   // thanks Lame for noticing pets not getting despawned after expiration time
                                    if (pet != null) {
                                        unequipPet(pet, true);
                                    }

                                    if (ItemConstants.isExpirablePet(item.getItemId())) {
                                        sendPacket(PacketCreator.itemExpired(item.getItemId()));
                                        toberemove.add(item);
                                    } else {
                                        item.setExpiration(-1);
                                        forceUpdateItem(item);
                                    }
                                }
                            }
                        }

                        if (!toberemove.isEmpty()) {
                            for (Item item : toberemove) {
                                InventoryManipulator.removeFromSlot(client, inv.getType(), item.getPosition(), item.getQuantity(), true);
                            }

                            ItemInformationProvider ii = ItemInformationProvider.getInstance();
                            for (Item item : toberemove) {
                                List<Integer> toadd = new ArrayList<>();
                                Pair<Integer, String> replace = ii.getReplaceOnExpire(item.getItemId());
                                if (replace.left > 0) {
                                    toadd.add(replace.left);
                                    if (!replace.right.isEmpty()) {
                                        dropMessage(replace.right);
                                    }
                                }
                                for (Integer itemid : toadd) {
                                    InventoryManipulator.addById(client, itemid, (short) 1);
                                }
                            }

                            toberemove.clear();
                        }

                        if (deletedCoupon) {
                            updateCouponRates();
                        }
                    }
                }
            }, 60000);
        }
    }

    public enum FameStatus {

        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public void forceUpdateItem(Item item) {
        final List<ModifyInventory> mods = new LinkedList<>();
        mods.add(new ModifyInventory(3, item));
        mods.add(new ModifyInventory(0, item));
        sendPacket(PacketCreator.modifyInventory(true, mods));
    }

    public void gainGachaExp() {
        int expgain = 0;
        long currentgexp = gachaexp.get();
        if ((currentgexp + exp.get()) >= ExpTable.getExpNeededForLevel(level)) {
            expgain += ExpTable.getExpNeededForLevel(level) - exp.get();

            int nextneed = ExpTable.getExpNeededForLevel(level + 1);
            if (currentgexp - expgain >= nextneed) {
                expgain += nextneed;
            }

            this.gachaexp.set((int) (currentgexp - expgain));
        } else {
            expgain = this.gachaexp.getAndSet(0);
        }
        gainExp(expgain, false, true);
        updateSingleStat(Stat.GACHAEXP, this.gachaexp.get());
    }

    public void addGachaExp(int gain) {
        updateSingleStat(Stat.GACHAEXP, gachaexp.addAndGet(gain));
    }

    public void gainExp(int gain) {
        gainExp(gain, true, true);
    }

    public void gainExp(int gain, boolean show, boolean inChat) {
        gainExp(gain, show, inChat, true);
    }

    public void gainExp(int gain, boolean show, boolean inChat, boolean white) {
        gainExp(gain, 0, show, inChat, white);
    }

    public void gainExp(int gain, int party, boolean show, boolean inChat, boolean white) {
        if (hasDisease(Disease.CURSE)) {
            gain *= 0.5;
            party *= 0.5;
        }

        if (gain < 0) {
            gain = Integer.MAX_VALUE;   // integer overflow, heh.
        }

        if (party < 0) {
            party = Integer.MAX_VALUE;  // integer overflow, heh.
        }

        int equip = (int) Math.min((long) (gain / 10) * pendantExp, Integer.MAX_VALUE);

        gainExpInternal(gain, equip, party, show, inChat, white);
    }

    public void loseExp(int loss, boolean show, boolean inChat) {
        loseExp(loss, show, inChat, true);
    }

    public void loseExp(int loss, boolean show, boolean inChat, boolean white) {
        gainExpInternal(-loss, 0, 0, show, inChat, white);
    }

    private void announceExpGain(long gain, int equip, int party, boolean inChat, boolean white) {
        gain = Math.min(gain, Integer.MAX_VALUE);
        if (gain == 0) {
            if (party == 0) {
                return;
            }

            gain = party;
            party = 0;
            white = false;
        }

        sendPacket(PacketCreator.getShowExpGain((int) gain, equip, party, inChat, white));
    }

    private synchronized void gainExpInternal(long gain, int equip, int party, boolean show, boolean inChat, boolean white) {   // need of method synchonization here detected thanks to MedicOP
        long total = Math.max(gain + equip + party, -exp.get());

        if (level < getMaxLevel() && (allowExpGain || this.getEventInstance() != null)) {
            long leftover = 0;
            long nextExp = exp.get() + total;

            if (nextExp > (long) Integer.MAX_VALUE) {
                total = Integer.MAX_VALUE - exp.get();
                leftover = nextExp - Integer.MAX_VALUE;
            }
            updateSingleStat(Stat.EXP, exp.addAndGet((int) total));
            totalExpGained += total;
            if (show) {
                announceExpGain(gain, equip, party, inChat, white);
            }
            while (exp.get() >= ExpTable.getExpNeededForLevel(level)) {
                levelUp(true);
                if (level == getMaxLevel()) {
                    setExp(0);
                    updateSingleStat(Stat.EXP, 0);
                    break;
                }
            }

            if (leftover > 0) {
                gainExpInternal(leftover, equip, party, false, inChat, white);
            } else {
                lastExpGainTime = System.currentTimeMillis();

                if (YamlConfig.config.server.USE_EXP_GAIN_LOG) {
                    ExpLogRecord expLogRecord = new ExpLogger.ExpLogRecord(
                        getWorldServer().getExpRate(),
                        expCoupon,
                        totalExpGained,
                        exp.get(),
                        new Timestamp(lastExpGainTime),
                        id
                    );
                    ExpLogger.putExpLogRecord(expLogRecord);
                }

                totalExpGained = 0;
            }
        }
    }

    private Pair<Integer, Integer> applyFame(int delta) {
        petLock.lock();
        try {
            int newFame = fame + delta;
            if (newFame < -30000) {
                delta = -(30000 + fame);
            } else if (newFame > 30000) {
                delta = 30000 - fame;
            }

            fame += delta;
            return new Pair<>(fame, delta);
        } finally {
            petLock.unlock();
        }
    }

    public void gainFame(int delta) {
        gainFame(delta, null, 0);
    }

    public boolean gainFame(int delta, Character fromPlayer, int mode) {
        Pair<Integer, Integer> fameRes = applyFame(delta);
        delta = fameRes.getRight();
        if (delta != 0) {
            int thisFame = fameRes.getLeft();
            updateSingleStat(Stat.FAME, thisFame);

            if (fromPlayer != null) {
                fromPlayer.sendPacket(PacketCreator.giveFameResponse(mode, getName(), thisFame));
                sendPacket(PacketCreator.receiveFame(mode, fromPlayer.getName()));
            } else {
                sendPacket(PacketCreator.getShowFameGain(delta));
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean canHoldMeso(int gain) {  // thanks lucasziron for pointing out a need to check space availability for mesos on player transactions
        long nextMeso = (long) meso.get() + gain;
        return nextMeso <= Integer.MAX_VALUE;
    }

    public void gainMeso(int gain) {
        gainMeso(gain, true, false, true);
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false, false);
    }

    public void gainMeso(int gain, boolean show, boolean enableActions, boolean inChat) {
        long nextMeso;
        petLock.lock();
        try {
            nextMeso = (long) meso.get() + gain;  // thanks Thora for pointing integer overflow here
            if (nextMeso > Integer.MAX_VALUE) {
                gain -= (nextMeso - Integer.MAX_VALUE);
            } else if (nextMeso < 0) {
                gain = -meso.get();
            }
            nextMeso = meso.addAndGet(gain);
        } finally {
            petLock.unlock();
        }

        if (gain != 0) {
            updateSingleStat(Stat.MESO, (int) nextMeso, enableActions);
            if (show) {
                sendPacket(PacketCreator.getShowMesoGain(gain, inChat));
            }
        } else {
            sendPacket(PacketCreator.enableActions());
        }
    }

    public void genericGuildMessage(int code) {
        this.sendPacket(GuildPackets.genericGuildMessage((byte) code));
    }

    public int getAccountID() {
        return accountid;
    }

    public List<PlayerCoolDownValueHolder> getAllCooldowns() {
        List<PlayerCoolDownValueHolder> ret = new ArrayList<>();

        effLock.lock();
        chrLock.lock();
        try {
            for (CooldownValueHolder mcdvh : coolDowns.values()) {
                ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }

        return ret;
    }

    public int getAllianceRank() {
        return allianceRank;
    }

    public static String getAriantRoomLeaderName(int room) {
        return ariantroomleader[room];
    }

    public static int getAriantSlotsRoom(int room) {
        return ariantroomslot[room];
    }

    public void updateAriantScore() {
        updateAriantScore(0);
    }

    public void updateAriantScore(int dropQty) {
        AriantColiseum arena = this.getAriantColiseum();
        if (arena != null) {
            arena.updateAriantScore(this, countItem(ItemId.ARPQ_SPIRIT_JEWEL));

            if (dropQty > 0) {
                arena.addLostShards(dropQty);
            }
        }
    }

    public int getBattleshipHp() {
        return battleshipHp;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public static Map<String, String> getCharacterFromDatabase(String name) {
        Map<String, String> character = new LinkedHashMap<>();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT `id`, `accountid`, `name` FROM `characters` WHERE `name` = ?")) {
            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    character.put(rs.getMetaData().getColumnLabel(i), rs.getString(i));
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return character;
    }

    public Long getBuffedStarttime(BuffStat effect) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return null;
            }
            return Long.valueOf(mbsvh.startTime);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public Integer getBuffedValue(BuffStat effect) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return null;
            }
            return Integer.valueOf(mbsvh.value);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public int getBuffSource(BuffStat stat) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(stat);
            if (mbsvh == null) {
                return -1;
            }
            return mbsvh.effect.getSourceId();
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public StatEffect getBuffEffect(BuffStat stat) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(stat);
            if (mbsvh == null) {
                return null;
            } else {
                return mbsvh.effect;
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public Set<Integer> getAvailableBuffs() {
        effLock.lock();
        chrLock.lock();
        try {
            return new LinkedHashSet<>(buffEffects.keySet());
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    private List<BuffStatValueHolder> getAllStatups() {
        effLock.lock();
        chrLock.lock();
        try {
            List<BuffStatValueHolder> ret = new ArrayList<>();
            for (Map<BuffStat, BuffStatValueHolder> bel : buffEffects.values()) {
                for (BuffStatValueHolder mbsvh : bel.values()) {
                    ret.add(mbsvh);
                }
            }
            return ret;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {  // buff values will be stored in an arbitrary order
        effLock.lock();
        chrLock.lock();
        try {
            long curtime = Server.getInstance().getCurrentTime();

            Map<Integer, PlayerBuffValueHolder> ret = new LinkedHashMap<>();
            for (Map<BuffStat, BuffStatValueHolder> bel : buffEffects.values()) {
                for (BuffStatValueHolder mbsvh : bel.values()) {
                    int srcid = mbsvh.effect.getBuffSourceId();
                    if (!ret.containsKey(srcid)) {
                        ret.put(srcid, new PlayerBuffValueHolder((int) (curtime - mbsvh.startTime), mbsvh.effect));
                    }
                }
            }
            return new ArrayList<>(ret.values());
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public List<Pair<BuffStat, Integer>> getAllActiveStatups() {
        effLock.lock();
        chrLock.lock();
        try {
            List<Pair<BuffStat, Integer>> ret = new ArrayList<>();
            for (BuffStat mbs : effects.keySet()) {
                BuffStatValueHolder mbsvh = effects.get(mbs);
                ret.add(new Pair<>(mbs, mbsvh.value));
            }
            return ret;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public boolean hasBuffFromSourceid(int sourceid) {
        effLock.lock();
        chrLock.lock();
        try {
            return buffEffects.containsKey(sourceid);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public boolean hasActiveBuff(int sourceid) {
        LinkedList<BuffStatValueHolder> allBuffs;

        effLock.lock();
        chrLock.lock();
        try {
            allBuffs = new LinkedList<>(effects.values());
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }

        for (BuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getBuffSourceId() == sourceid) {
                return true;
            }
        }
        return false;
    }

    private List<Pair<BuffStat, Integer>> getActiveStatupsFromSourceid(int sourceid) { // already under effLock & chrLock
        List<Pair<BuffStat, Integer>> ret = new ArrayList<>();
        List<Pair<BuffStat, Integer>> singletonStatups = new ArrayList<>();
        for (Entry<BuffStat, BuffStatValueHolder> bel : buffEffects.get(sourceid).entrySet()) {
            BuffStat mbs = bel.getKey();
            BuffStatValueHolder mbsvh = effects.get(bel.getKey());

            Pair<BuffStat, Integer> p;
            if (mbsvh != null) {
                p = new Pair<>(mbs, mbsvh.value);
            } else {
                p = new Pair<>(mbs, 0);
            }

            if (!isSingletonStatup(mbs)) {   // thanks resinate, Daddy Egg for pointing out morph issues when updating it along with other statups
                ret.add(p);
            } else {
                singletonStatups.add(p);
            }
        }

        Collections.sort(ret, new Comparator<Pair<BuffStat, Integer>>() {
            @Override
            public int compare(Pair<BuffStat, Integer> p1, Pair<BuffStat, Integer> p2) {
                return p1.getLeft().compareTo(p2.getLeft());
            }
        });

        if (!singletonStatups.isEmpty()) {
            Collections.sort(singletonStatups, new Comparator<Pair<BuffStat, Integer>>() {
                @Override
                public int compare(Pair<BuffStat, Integer> p1, Pair<BuffStat, Integer> p2) {
                    return p1.getLeft().compareTo(p2.getLeft());
                }
            });

            ret.addAll(singletonStatups);
        }

        return ret;
    }

    private void addItemEffectHolder(Integer sourceid, long expirationtime, Map<BuffStat, BuffStatValueHolder> statups) {
        buffEffects.put(sourceid, statups);
        buffExpires.put(sourceid, expirationtime);
    }

    private boolean removeEffectFromItemEffectHolder(Integer sourceid, BuffStat buffStat) {
        Map<BuffStat, BuffStatValueHolder> lbe = buffEffects.get(sourceid);

        if (lbe.remove(buffStat) != null) {
            buffEffectsCount.put(buffStat, (byte) (buffEffectsCount.get(buffStat) - 1));

            if (lbe.isEmpty()) {
                buffEffects.remove(sourceid);
                buffExpires.remove(sourceid);
            }

            return true;
        }

        return false;
    }

    private void removeItemEffectHolder(Integer sourceid) {
        Map<BuffStat, BuffStatValueHolder> be = buffEffects.remove(sourceid);
        if (be != null) {
            for (Entry<BuffStat, BuffStatValueHolder> bei : be.entrySet()) {
                buffEffectsCount.put(bei.getKey(), (byte) (buffEffectsCount.get(bei.getKey()) - 1));
            }
        }

        buffExpires.remove(sourceid);
    }

    private void dropWorstEffectFromItemEffectHolder(BuffStat mbs) {
        Integer min = Integer.MAX_VALUE;
        Integer srcid = -1;
        for (Entry<Integer, Map<BuffStat, BuffStatValueHolder>> bpl : buffEffects.entrySet()) {
            BuffStatValueHolder mbsvh = bpl.getValue().get(mbs);
            if (mbsvh != null) {
                if (mbsvh.value < min) {
                    min = mbsvh.value;
                    srcid = bpl.getKey();
                }
            }
        }

        removeEffectFromItemEffectHolder(srcid, mbs);
    }

    private BuffStatValueHolder fetchBestEffectFromItemEffectHolder(BuffStat mbs) {
        Pair<Integer, Integer> max = new Pair<>(Integer.MIN_VALUE, 0);
        BuffStatValueHolder mbsvh = null;
        for (Entry<Integer, Map<BuffStat, BuffStatValueHolder>> bpl : buffEffects.entrySet()) {
            BuffStatValueHolder mbsvhi = bpl.getValue().get(mbs);
            if (mbsvhi != null) {
                if (!mbsvhi.effect.isActive(this)) {
                    continue;
                }

                if (mbsvhi.value > max.left) {
                    max = new Pair<>(mbsvhi.value, mbsvhi.effect.getStatups().size());
                    mbsvh = mbsvhi;
                } else if (mbsvhi.value == max.left && mbsvhi.effect.getStatups().size() > max.right) {
                    max = new Pair<>(mbsvhi.value, mbsvhi.effect.getStatups().size());
                    mbsvh = mbsvhi;
                }
            }
        }

        if (mbsvh != null) {
            effects.put(mbs, mbsvh);
        }
        return mbsvh;
    }

    private void extractBuffValue(int sourceid, BuffStat stat) {
        chrLock.lock();
        try {
            removeEffectFromItemEffectHolder(sourceid, stat);
        } finally {
            chrLock.unlock();
        }
    }

    public void debugListAllBuffs() {
        effLock.lock();
        chrLock.lock();
        try {
            log.debug("-------------------");
            log.debug("CACHED BUFF COUNT: {}", buffEffectsCount.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining(", "))
            );

            log.debug("-------------------");
            log.debug("CACHED BUFFS: {}", buffEffects.entrySet().stream()
                    .map(entry -> entry.getKey() + ": (" + entry.getValue().entrySet().stream()
                            .map(innerEntry -> innerEntry.getKey().name() + innerEntry.getValue().value)
                            .collect(Collectors.joining(", ")) + ")")
                    .collect(Collectors.joining(", "))
            );

            log.debug("-------------------");
            log.debug("IN ACTION: {}", effects.entrySet().stream()
                    .map(entry -> entry.getKey().name() + " -> " + ItemInformationProvider.getInstance().getName(entry.getValue().effect.getSourceId()))
                    .collect(Collectors.joining(", "))
            );
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void debugListAllBuffsCount() {
        effLock.lock();
        chrLock.lock();
        try {
            log.debug("ALL BUFFS COUNT: {}", buffEffectsCount.entrySet().stream()
                    .map(entry -> entry.getKey().name() + " -> " + entry.getValue())
                    .collect(Collectors.joining(", "))
            );
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void cancelAllBuffs(boolean softcancel) {
        if (softcancel) {
            effLock.lock();
            chrLock.lock();
            try {
                cancelEffectFromBuffStat(BuffStat.SUMMON);
                cancelEffectFromBuffStat(BuffStat.PUPPET);
                cancelEffectFromBuffStat(BuffStat.COMBO);

                effects.clear();

                for (Integer srcid : new ArrayList<>(buffEffects.keySet())) {
                    removeItemEffectHolder(srcid);
                }
            } finally {
                chrLock.unlock();
                effLock.unlock();
            }
        } else {
            Map<StatEffect, Long> mseBuffs = new LinkedHashMap<>();

            effLock.lock();
            chrLock.lock();
            try {
                for (Entry<Integer, Map<BuffStat, BuffStatValueHolder>> bpl : buffEffects.entrySet()) {
                    for (Entry<BuffStat, BuffStatValueHolder> mbse : bpl.getValue().entrySet()) {
                        mseBuffs.put(mbse.getValue().effect, mbse.getValue().startTime);
                    }
                }
            } finally {
                chrLock.unlock();
                effLock.unlock();
            }

            for (Entry<StatEffect, Long> mse : mseBuffs.entrySet()) {
                cancelEffect(mse.getKey(), false, mse.getValue());
            }
        }
    }

    private void dropBuffStats(List<Pair<BuffStat, BuffStatValueHolder>> effectsToCancel) {
        for (Pair<BuffStat, BuffStatValueHolder> cancelEffectCancelTasks : effectsToCancel) {
            //boolean nestedCancel = false;

            chrLock.lock();
            try {
                /*
                if (buffExpires.get(cancelEffectCancelTasks.getRight().effect.getBuffSourceId()) != null) {
                    nestedCancel = true;
                }*/

                if (cancelEffectCancelTasks.getRight().bestApplied) {
                    fetchBestEffectFromItemEffectHolder(cancelEffectCancelTasks.getLeft());
                }
            } finally {
                chrLock.unlock();
            }

            /*
            if (nestedCancel) {
                this.cancelEffect(cancelEffectCancelTasks.getRight().effect, false, -1, false);
            }*/
        }
    }

    private List<Pair<BuffStat, BuffStatValueHolder>> deregisterBuffStats(Map<BuffStat, BuffStatValueHolder> stats) {
        chrLock.lock();
        try {
            List<Pair<BuffStat, BuffStatValueHolder>> effectsToCancel = new ArrayList<>(stats.size());
            for (Entry<BuffStat, BuffStatValueHolder> stat : stats.entrySet()) {
                int sourceid = stat.getValue().effect.getBuffSourceId();

                if (!buffEffects.containsKey(sourceid)) {
                    buffExpires.remove(sourceid);
                }

                BuffStat mbs = stat.getKey();
                effectsToCancel.add(new Pair<>(mbs, stat.getValue()));

                BuffStatValueHolder mbsvh = effects.get(mbs);
                if (mbsvh != null && mbsvh.effect.getBuffSourceId() == sourceid) {
                    mbsvh.bestApplied = true;
                    effects.remove(mbs);

                    if (mbs == BuffStat.RECOVERY) {
                        if (recoveryTask != null) {
                            recoveryTask.cancel(false);
                            recoveryTask = null;
                        }
                    } else if (mbs == BuffStat.SUMMON || mbs == BuffStat.PUPPET) {
                        int summonId = mbsvh.effect.getSourceId();

                        Summon summon = summons.get(summonId);
                        if (summon != null) {
                            getMap().broadcastMessage(PacketCreator.removeSummon(summon, true), summon.getPosition());
                            getMap().removeMapObject(summon);
                            removeVisibleMapObject(summon);

                            summons.remove(summonId);
                            if (summon.isPuppet()) {
                                map.removePlayerPuppet(this);
                            } else if (summon.getSkill() == DarkKnight.BEHOLDER) {
                                if (beholderHealingSchedule != null) {
                                    beholderHealingSchedule.cancel(false);
                                    beholderHealingSchedule = null;
                                }
                                if (beholderBuffSchedule != null) {
                                    beholderBuffSchedule.cancel(false);
                                    beholderBuffSchedule = null;
                                }
                            }
                        }
                    } else if (mbs == BuffStat.DRAGONBLOOD) {
                        dragonBloodSchedule.cancel(false);
                        dragonBloodSchedule = null;
                    } else if (mbs == BuffStat.HPREC || mbs == BuffStat.MPREC) {
                        if (mbs == BuffStat.HPREC) {
                            extraHpRec = 0;
                        } else {
                            extraMpRec = 0;
                        }

                        if (extraRecoveryTask != null) {
                            extraRecoveryTask.cancel(false);
                            extraRecoveryTask = null;
                        }

                        if (extraHpRec != 0 || extraMpRec != 0) {
                            startExtraTaskInternal(extraHpRec, extraMpRec, extraRecInterval);
                        }
                    }
                }
            }

            return effectsToCancel;
        } finally {
            chrLock.unlock();
        }
    }

    public void cancelEffect(int itemId) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        cancelEffect(ii.getItemEffect(itemId), false, -1);
    }

    public boolean cancelEffect(StatEffect effect, boolean overwrite, long startTime) {
        boolean ret;

        prtLock.lock();
        effLock.lock();
        try {
            ret = cancelEffect(effect, overwrite, startTime, true);
        } finally {
            effLock.unlock();
            prtLock.unlock();
        }

        if (effect.isMagicDoor() && ret) {
            prtLock.lock();
            effLock.lock();
            try {
                if (!hasBuffFromSourceid(Priest.MYSTIC_DOOR)) {
                    Door.attemptRemoveDoor(this);
                }
            } finally {
                effLock.unlock();
                prtLock.unlock();
            }
        }

        return ret;
    }

    private static StatEffect getEffectFromBuffSource(Map<BuffStat, BuffStatValueHolder> buffSource) {
        try {
            return buffSource.entrySet().iterator().next().getValue().effect;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isUpdatingEffect(Set<StatEffect> activeEffects, StatEffect mse) {
        if (mse == null) {
            return false;
        }

        // thanks xinyifly for noticing "Speed Infusion" crashing game when updating buffs during map transition
        boolean active = mse.isActive(this);
        if (active) {
            return !activeEffects.contains(mse);
        } else {
            return activeEffects.contains(mse);
        }
    }

    public void updateActiveEffects() {
        effLock.lock();     // thanks davidlafriniere, maple006, RedHat for pointing a deadlock occurring here
        try {
            Set<BuffStat> updatedBuffs = new LinkedHashSet<>();
            Set<StatEffect> activeEffects = new LinkedHashSet<>();

            for (BuffStatValueHolder mse : effects.values()) {
                activeEffects.add(mse.effect);
            }

            for (Map<BuffStat, BuffStatValueHolder> buff : buffEffects.values()) {
                StatEffect mse = getEffectFromBuffSource(buff);
                if (isUpdatingEffect(activeEffects, mse)) {
                    for (Pair<BuffStat, Integer> p : mse.getStatups()) {
                        updatedBuffs.add(p.getLeft());
                    }
                }
            }

            for (BuffStat mbs : updatedBuffs) {
                effects.remove(mbs);
            }

            updateEffects(updatedBuffs);
        } finally {
            effLock.unlock();
        }
    }

    private void updateEffects(Set<BuffStat> removedStats) {
        effLock.lock();
        chrLock.lock();
        try {
            Set<BuffStat> retrievedStats = new LinkedHashSet<>();

            for (BuffStat mbs : removedStats) {
                fetchBestEffectFromItemEffectHolder(mbs);

                BuffStatValueHolder mbsvh = effects.get(mbs);
                if (mbsvh != null) {
                    for (Pair<BuffStat, Integer> statup : mbsvh.effect.getStatups()) {
                        retrievedStats.add(statup.getLeft());
                    }
                }
            }

            propagateBuffEffectUpdates(new LinkedHashMap<Integer, Pair<StatEffect, Long>>(), retrievedStats, removedStats);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    private boolean cancelEffect(StatEffect effect, boolean overwrite, long startTime, boolean firstCancel) {
        Set<BuffStat> removedStats = new LinkedHashSet<>();
        dropBuffStats(cancelEffectInternal(effect, overwrite, startTime, removedStats));
        updateLocalStats();
        updateEffects(removedStats);

        return !removedStats.isEmpty();
    }

    private List<Pair<BuffStat, BuffStatValueHolder>> cancelEffectInternal(StatEffect effect, boolean overwrite, long startTime, Set<BuffStat> removedStats) {
        Map<BuffStat, BuffStatValueHolder> buffstats = null;
        BuffStat ombs;
        if (!overwrite) {   // is removing the source effect, meaning every effect from this srcid is being purged
            buffstats = extractCurrentBuffStats(effect);
        } else if ((ombs = getSingletonStatupFromEffect(effect)) != null) {   // removing all effects of a buff having non-shareable buff stat.
            BuffStatValueHolder mbsvh = effects.get(ombs);
            if (mbsvh != null) {
                buffstats = extractCurrentBuffStats(mbsvh.effect);
            }
        }

        if (buffstats == null) {            // all else, is dropping ALL current statups that uses same stats as the given effect
            buffstats = extractLeastRelevantStatEffectsIfFull(effect);
        }

        if (effect.isMapChair()) {
            stopChairTask();
        }

        List<Pair<BuffStat, BuffStatValueHolder>> toCancel = deregisterBuffStats(buffstats);
        if (effect.isMonsterRiding()) {
            this.getClient().getWorldServer().unregisterMountHunger(this);
            this.getMount().setActive(false);
        }

        if (!overwrite) {
            removedStats.addAll(buffstats.keySet());
        }

        return toCancel;
    }

    public void cancelEffectFromBuffStat(BuffStat stat) {
        BuffStatValueHolder effect;

        effLock.lock();
        chrLock.lock();
        try {
            effect = effects.get(stat);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
        if (effect != null) {
            cancelEffect(effect.effect, false, -1);
        }
    }

    public void cancelBuffStats(BuffStat stat) {
        effLock.lock();
        try {
            List<Pair<Integer, BuffStatValueHolder>> cancelList = new LinkedList<>();

            chrLock.lock();
            try {
                for (Entry<Integer, Map<BuffStat, BuffStatValueHolder>> bel : this.buffEffects.entrySet()) {
                    BuffStatValueHolder beli = bel.getValue().get(stat);
                    if (beli != null) {
                        cancelList.add(new Pair<>(bel.getKey(), beli));
                    }
                }
            } finally {
                chrLock.unlock();
            }

            Map<BuffStat, BuffStatValueHolder> buffStatList = new LinkedHashMap<>();
            for (Pair<Integer, BuffStatValueHolder> p : cancelList) {
                buffStatList.put(stat, p.getRight());
                extractBuffValue(p.getLeft(), stat);
                dropBuffStats(deregisterBuffStats(buffStatList));
            }
        } finally {
            effLock.unlock();
        }

        cancelPlayerBuffs(Arrays.asList(stat));
    }

    private Map<BuffStat, BuffStatValueHolder> extractCurrentBuffStats(StatEffect effect) {
        chrLock.lock();
        try {
            Map<BuffStat, BuffStatValueHolder> stats = new LinkedHashMap<>();
            Map<BuffStat, BuffStatValueHolder> buffList = buffEffects.remove(effect.getBuffSourceId());

            if (buffList != null) {
                for (Entry<BuffStat, BuffStatValueHolder> stateffect : buffList.entrySet()) {
                    stats.put(stateffect.getKey(), stateffect.getValue());
                    buffEffectsCount.put(stateffect.getKey(), (byte) (buffEffectsCount.get(stateffect.getKey()) - 1));
                }
            }

            return stats;
        } finally {
            chrLock.unlock();
        }
    }

    private Map<BuffStat, BuffStatValueHolder> extractLeastRelevantStatEffectsIfFull(StatEffect effect) {
        Map<BuffStat, BuffStatValueHolder> extractedStatBuffs = new LinkedHashMap<>();

        chrLock.lock();
        try {
            Map<BuffStat, Byte> stats = new LinkedHashMap<>();
            Map<BuffStat, BuffStatValueHolder> minStatBuffs = new LinkedHashMap<>();

            for (Entry<Integer, Map<BuffStat, BuffStatValueHolder>> mbsvhi : buffEffects.entrySet()) {
                for (Entry<BuffStat, BuffStatValueHolder> mbsvhe : mbsvhi.getValue().entrySet()) {
                    BuffStat mbs = mbsvhe.getKey();
                    Byte b = stats.get(mbs);

                    if (b != null) {
                        stats.put(mbs, (byte) (b + 1));
                        if (mbsvhe.getValue().value < minStatBuffs.get(mbs).value) {
                            minStatBuffs.put(mbs, mbsvhe.getValue());
                        }
                    } else {
                        stats.put(mbs, (byte) 1);
                        minStatBuffs.put(mbs, mbsvhe.getValue());
                    }
                }
            }

            Set<BuffStat> effectStatups = new LinkedHashSet<>();
            for (Pair<BuffStat, Integer> efstat : effect.getStatups()) {
                effectStatups.add(efstat.getLeft());
            }

            for (Entry<BuffStat, Byte> it : stats.entrySet()) {
                boolean uniqueBuff = isSingletonStatup(it.getKey());

                if (it.getValue() >= (!uniqueBuff ? YamlConfig.config.server.MAX_MONITORED_BUFFSTATS : 1) && effectStatups.contains(it.getKey())) {
                    BuffStatValueHolder mbsvh = minStatBuffs.get(it.getKey());

                    Map<BuffStat, BuffStatValueHolder> lpbe = buffEffects.get(mbsvh.effect.getBuffSourceId());
                    lpbe.remove(it.getKey());
                    buffEffectsCount.put(it.getKey(), (byte) (buffEffectsCount.get(it.getKey()) - 1));

                    if (lpbe.isEmpty()) {
                        buffEffects.remove(mbsvh.effect.getBuffSourceId());
                    }
                    extractedStatBuffs.put(it.getKey(), mbsvh);
                }
            }
        } finally {
            chrLock.unlock();
        }

        return extractedStatBuffs;
    }

    private void cancelInactiveBuffStats(Set<BuffStat> retrievedStats, Set<BuffStat> removedStats) {
        List<BuffStat> inactiveStats = new LinkedList<>();
        for (BuffStat mbs : removedStats) {
            if (!retrievedStats.contains(mbs)) {
                inactiveStats.add(mbs);
            }
        }

        if (!inactiveStats.isEmpty()) {
            sendPacket(PacketCreator.cancelBuff(inactiveStats));
            getMap().broadcastMessage(this, PacketCreator.cancelForeignBuff(getId(), inactiveStats), false);
        }
    }

    private static Map<StatEffect, Integer> topologicalSortLeafStatCount(Map<BuffStat, Stack<StatEffect>> buffStack) {
        Map<StatEffect, Integer> leafBuffCount = new LinkedHashMap<>();

        for (Entry<BuffStat, Stack<StatEffect>> e : buffStack.entrySet()) {
            Stack<StatEffect> mseStack = e.getValue();
            if (mseStack.isEmpty()) {
                continue;
            }

            StatEffect mse = mseStack.peek();
            Integer count = leafBuffCount.get(mse);
            if (count == null) {
                leafBuffCount.put(mse, 1);
            } else {
                leafBuffCount.put(mse, count + 1);
            }
        }

        return leafBuffCount;
    }

    private static List<StatEffect> topologicalSortRemoveLeafStats(Map<StatEffect, Set<BuffStat>> stackedBuffStats, Map<BuffStat, Stack<StatEffect>> buffStack, Map<StatEffect, Integer> leafStatCount) {
        List<StatEffect> clearedStatEffects = new LinkedList<>();
        Set<BuffStat> clearedStats = new LinkedHashSet<>();

        for (Entry<StatEffect, Integer> e : leafStatCount.entrySet()) {
            StatEffect mse = e.getKey();

            if (stackedBuffStats.get(mse).size() <= e.getValue()) {
                clearedStatEffects.add(mse);

                for (BuffStat mbs : stackedBuffStats.get(mse)) {
                    clearedStats.add(mbs);
                }
            }
        }

        for (BuffStat mbs : clearedStats) {
            StatEffect mse = buffStack.get(mbs).pop();
            stackedBuffStats.get(mse).remove(mbs);
        }

        return clearedStatEffects;
    }

    private static void topologicalSortRebaseLeafStats(Map<StatEffect, Set<BuffStat>> stackedBuffStats, Map<BuffStat, Stack<StatEffect>> buffStack) {
        for (Entry<BuffStat, Stack<StatEffect>> e : buffStack.entrySet()) {
            Stack<StatEffect> mseStack = e.getValue();

            if (!mseStack.isEmpty()) {
                StatEffect mse = mseStack.pop();
                stackedBuffStats.get(mse).remove(e.getKey());
            }
        }
    }

    private static List<StatEffect> topologicalSortEffects(Map<BuffStat, List<Pair<StatEffect, Integer>>> buffEffects) {
        Map<StatEffect, Set<BuffStat>> stackedBuffStats = new LinkedHashMap<>();
        Map<BuffStat, Stack<StatEffect>> buffStack = new LinkedHashMap<>();

        for (Entry<BuffStat, List<Pair<StatEffect, Integer>>> e : buffEffects.entrySet()) {
            BuffStat mbs = e.getKey();

            Stack<StatEffect> mbsStack = new Stack<>();
            buffStack.put(mbs, mbsStack);

            for (Pair<StatEffect, Integer> emse : e.getValue()) {
                StatEffect mse = emse.getLeft();
                mbsStack.push(mse);

                Set<BuffStat> mbsStats = stackedBuffStats.get(mse);
                if (mbsStats == null) {
                    mbsStats = new LinkedHashSet<>();
                    stackedBuffStats.put(mse, mbsStats);
                }

                mbsStats.add(mbs);
            }
        }

        List<StatEffect> buffList = new LinkedList<>();
        while (true) {
            Map<StatEffect, Integer> leafStatCount = topologicalSortLeafStatCount(buffStack);
            if (leafStatCount.isEmpty()) {
                break;
            }

            List<StatEffect> clearedNodes = topologicalSortRemoveLeafStats(stackedBuffStats, buffStack, leafStatCount);
            if (clearedNodes.isEmpty()) {
                topologicalSortRebaseLeafStats(stackedBuffStats, buffStack);
            } else {
                buffList.addAll(clearedNodes);
            }
        }

        return buffList;
    }

    private static List<StatEffect> sortEffectsList(Map<StatEffect, Integer> updateEffectsList) {
        Map<BuffStat, List<Pair<StatEffect, Integer>>> buffEffects = new LinkedHashMap<>();

        for (Entry<StatEffect, Integer> p : updateEffectsList.entrySet()) {
            StatEffect mse = p.getKey();

            for (Pair<BuffStat, Integer> statup : mse.getStatups()) {
                BuffStat stat = statup.getLeft();

                List<Pair<StatEffect, Integer>> statBuffs = buffEffects.get(stat);
                if (statBuffs == null) {
                    statBuffs = new ArrayList<>();
                    buffEffects.put(stat, statBuffs);
                }

                statBuffs.add(new Pair<>(mse, statup.getRight()));
            }
        }

        Comparator cmp = new Comparator<Pair<StatEffect, Integer>>() {
            @Override
            public int compare(Pair<StatEffect, Integer> o1, Pair<StatEffect, Integer> o2) {
                return o2.getRight().compareTo(o1.getRight());
            }
        };

        for (Entry<BuffStat, List<Pair<StatEffect, Integer>>> statBuffs : buffEffects.entrySet()) {
            Collections.sort(statBuffs.getValue(), cmp);
        }

        return topologicalSortEffects(buffEffects);
    }

    private List<Pair<Integer, Pair<StatEffect, Long>>> propagatePriorityBuffEffectUpdates(Set<BuffStat> retrievedStats) {
        List<Pair<Integer, Pair<StatEffect, Long>>> priorityUpdateEffects = new LinkedList<>();
        Map<BuffStatValueHolder, StatEffect> yokeStats = new LinkedHashMap<>();

        // priority buffsources: override buffstats for the client to perceive those as "currently buffed"
        Set<BuffStatValueHolder> mbsvhList = new LinkedHashSet<>();
        for (BuffStatValueHolder mbsvh : getAllStatups()) {
            mbsvhList.add(mbsvh);
        }

        for (BuffStatValueHolder mbsvh : mbsvhList) {
            StatEffect mse = mbsvh.effect;
            int buffSourceId = mse.getBuffSourceId();
            if (isPriorityBuffSourceid(buffSourceId) && !hasActiveBuff(buffSourceId)) {
                for (Pair<BuffStat, Integer> ps : mse.getStatups()) {
                    BuffStat mbs = ps.getLeft();
                    if (retrievedStats.contains(mbs)) {
                        BuffStatValueHolder mbsvhe = effects.get(mbs);

                        // this shouldn't even be null...
                        //if (mbsvh != null) {
                        yokeStats.put(mbsvh, mbsvhe.effect);
                        //}
                    }
                }
            }
        }

        for (Entry<BuffStatValueHolder, StatEffect> e : yokeStats.entrySet()) {
            BuffStatValueHolder mbsvhPriority = e.getKey();
            StatEffect mseActive = e.getValue();

            priorityUpdateEffects.add(new Pair<>(mseActive.getBuffSourceId(), new Pair<>(mbsvhPriority.effect, mbsvhPriority.startTime)));
        }

        return priorityUpdateEffects;
    }

    private void propagateBuffEffectUpdates(Map<Integer, Pair<StatEffect, Long>> retrievedEffects, Set<BuffStat> retrievedStats, Set<BuffStat> removedStats) {
        cancelInactiveBuffStats(retrievedStats, removedStats);
        if (retrievedStats.isEmpty()) {
            return;
        }

        Map<BuffStat, Pair<Integer, StatEffect>> maxBuffValue = new LinkedHashMap<>();
        for (BuffStat mbs : retrievedStats) {
            BuffStatValueHolder mbsvh = effects.get(mbs);
            if (mbsvh != null) {
                retrievedEffects.put(mbsvh.effect.getBuffSourceId(), new Pair<>(mbsvh.effect, mbsvh.startTime));
            }

            maxBuffValue.put(mbs, new Pair<>(Integer.MIN_VALUE, null));
        }

        Map<StatEffect, Integer> updateEffects = new LinkedHashMap<>();

        List<StatEffect> recalcMseList = new LinkedList<>();
        for (Entry<Integer, Pair<StatEffect, Long>> re : retrievedEffects.entrySet()) {
            recalcMseList.add(re.getValue().getLeft());
        }

        boolean mageJob = this.getJobStyle() == Job.MAGICIAN;
        do {
            List<StatEffect> mseList = recalcMseList;
            recalcMseList = new LinkedList<>();

            for (StatEffect mse : mseList) {
                int maxEffectiveStatup = Integer.MIN_VALUE;
                for (Pair<BuffStat, Integer> st : mse.getStatups()) {
                    BuffStat mbs = st.getLeft();

                    boolean relevantStatup = true;
                    if (mbs == BuffStat.WATK) {  // not relevant for mages
                        if (mageJob) {
                            relevantStatup = false;
                        }
                    } else if (mbs == BuffStat.MATK) { // not relevant for non-mages
                        if (!mageJob) {
                            relevantStatup = false;
                        }
                    }

                    Pair<Integer, StatEffect> mbv = maxBuffValue.get(mbs);
                    if (mbv == null) {
                        continue;
                    }

                    if (mbv.getLeft() < st.getRight()) {
                        StatEffect msbe = mbv.getRight();
                        if (msbe != null) {
                            recalcMseList.add(msbe);
                        }

                        maxBuffValue.put(mbs, new Pair<>(st.getRight(), mse));

                        if (relevantStatup) {
                            if (maxEffectiveStatup < st.getRight()) {
                                maxEffectiveStatup = st.getRight();
                            }
                        }
                    }
                }

                updateEffects.put(mse, maxEffectiveStatup);
            }
        } while (!recalcMseList.isEmpty());

        List<StatEffect> updateEffectsList = sortEffectsList(updateEffects);

        List<Pair<Integer, Pair<StatEffect, Long>>> toUpdateEffects = new LinkedList<>();
        for (StatEffect mse : updateEffectsList) {
            toUpdateEffects.add(new Pair<>(mse.getBuffSourceId(), retrievedEffects.get(mse.getBuffSourceId())));
        }

        List<Pair<BuffStat, Integer>> activeStatups = new LinkedList<>();
        for (Pair<Integer, Pair<StatEffect, Long>> lmse : toUpdateEffects) {
            Pair<StatEffect, Long> msel = lmse.getRight();

            for (Pair<BuffStat, Integer> statup : getActiveStatupsFromSourceid(lmse.getLeft())) {
                activeStatups.add(statup);
            }

            msel.getLeft().updateBuffEffect(this, activeStatups, msel.getRight());
            activeStatups.clear();
        }

        List<Pair<Integer, Pair<StatEffect, Long>>> priorityEffects = propagatePriorityBuffEffectUpdates(retrievedStats);
        for (Pair<Integer, Pair<StatEffect, Long>> lmse : priorityEffects) {
            Pair<StatEffect, Long> msel = lmse.getRight();

            for (Pair<BuffStat, Integer> statup : getActiveStatupsFromSourceid(lmse.getLeft())) {
                activeStatups.add(statup);
            }

            msel.getLeft().updateBuffEffect(this, activeStatups, msel.getRight());
            activeStatups.clear();
        }

        if (this.isRidingBattleship()) {
            List<Pair<BuffStat, Integer>> statups = new ArrayList<>(1);
            statups.add(new Pair<>(BuffStat.MONSTER_RIDING, 0));
            this.sendPacket(PacketCreator.giveBuff(ItemId.BATTLESHIP, 5221006, statups));
            this.announceBattleshipHp();
        }
    }

    private static BuffStat getSingletonStatupFromEffect(StatEffect mse) {
        for (Pair<BuffStat, Integer> mbs : mse.getStatups()) {
            if (isSingletonStatup(mbs.getLeft())) {
                return mbs.getLeft();
            }
        }

        return null;
    }

    private static boolean isSingletonStatup(BuffStat mbs) {
        switch (mbs) {           //HPREC and MPREC are supposed to be singleton
            case COUPON_EXP1:
            case COUPON_EXP2:
            case COUPON_EXP3:
            case COUPON_EXP4:
            case COUPON_DRP1:
            case COUPON_DRP2:
            case COUPON_DRP3:
            case MESO_UP_BY_ITEM:
            case ITEM_UP_BY_ITEM:
            case RESPECT_PIMMUNE:
            case RESPECT_MIMMUNE:
            case DEFENSE_ATT:
            case DEFENSE_STATE:
            case WATK:
            case WDEF:
            case MATK:
            case MDEF:
            case ACC:
            case AVOID:
            case SPEED:
            case JUMP:
                return false;

            default:
                return true;
        }
    }

    private static boolean isPriorityBuffSourceid(int sourceid) {
        switch (sourceid) {
            case -ItemId.ROSE_SCENT:
            case -ItemId.FREESIA_SCENT:
            case -ItemId.LAVENDER_SCENT:
                return true;

            default:
                return false;
        }
    }

    private void addItemEffectHolderCount(BuffStat stat) {
        Byte val = buffEffectsCount.get(stat);
        if (val != null) {
            val = (byte) (val + 1);
        } else {
            val = (byte) 1;
        }

        buffEffectsCount.put(stat, val);
    }

    public void registerEffect(StatEffect effect, long starttime, long expirationtime, boolean isSilent) {
        if (effect.isDragonBlood()) {
            prepareDragonBlood(effect);
        } else if (effect.isBerserk()) {
            checkBerserk(isHidden());
        } else if (effect.isBeholder()) {
            final int beholder = DarkKnight.BEHOLDER;
            if (beholderHealingSchedule != null) {
                beholderHealingSchedule.cancel(false);
            }
            if (beholderBuffSchedule != null) {
                beholderBuffSchedule.cancel(false);
            }
            Skill bHealing = SkillFactory.getSkill(DarkKnight.AURA_OF_BEHOLDER);
            int bHealingLvl = getSkillLevel(bHealing);
            if (bHealingLvl > 0) {
                final StatEffect healEffect = bHealing.getEffect(bHealingLvl);
                int healInterval = (int) SECONDS.toMillis(healEffect.getX());
                beholderHealingSchedule = TimerManager.getInstance().register(new Runnable() {
                    @Override
                    public void run() {
                        if (awayFromWorld.get()) {
                            return;
                        }

                        addHP(healEffect.getHp());
                        sendPacket(PacketCreator.showOwnBuffEffect(beholder, 2));
                        getMap().broadcastMessage(Character.this, PacketCreator.summonSkill(getId(), beholder, 5), true);
                        getMap().broadcastMessage(Character.this, PacketCreator.showOwnBuffEffect(beholder, 2), false);
                    }
                }, healInterval, healInterval);
            }
            Skill bBuff = SkillFactory.getSkill(DarkKnight.HEX_OF_BEHOLDER);
            if (getSkillLevel(bBuff) > 0) {
                final StatEffect buffEffect = bBuff.getEffect(getSkillLevel(bBuff));
                int buffInterval = (int) SECONDS.toMillis(buffEffect.getX());
                beholderBuffSchedule = TimerManager.getInstance().register(new Runnable() {
                    @Override
                    public void run() {
                        if (awayFromWorld.get()) {
                            return;
                        }

                        buffEffect.applyTo(Character.this);
                        sendPacket(PacketCreator.showOwnBuffEffect(beholder, 2));
                        getMap().broadcastMessage(Character.this, PacketCreator.summonSkill(getId(), beholder, (int) (Math.random() * 3) + 6), true);
                        getMap().broadcastMessage(Character.this, PacketCreator.showBuffEffect(getId(), beholder, 2), false);
                    }
                }, buffInterval, buffInterval);
            }
        } else if (effect.isRecovery()) {
            int healInterval = (YamlConfig.config.server.USE_ULTRA_RECOVERY) ? 2000 : 5000;
            final byte heal = (byte) effect.getX();

            chrLock.lock();
            try {
                if (recoveryTask != null) {
                    recoveryTask.cancel(false);
                }

                recoveryTask = TimerManager.getInstance().register(new Runnable() {
                    @Override
                    public void run() {
                        if (getBuffSource(BuffStat.RECOVERY) == -1) {
                            chrLock.lock();
                            try {
                                if (recoveryTask != null) {
                                    recoveryTask.cancel(false);
                                    recoveryTask = null;
                                }
                            } finally {
                                chrLock.unlock();
                            }

                            return;
                        }

                        addHP(heal);
                        sendPacket(PacketCreator.showOwnRecovery(heal));
                        getMap().broadcastMessage(Character.this, PacketCreator.showRecovery(id, heal), false);
                    }
                }, healInterval, healInterval);
            } finally {
                chrLock.unlock();
            }
        } else if (effect.getHpRRate() > 0 || effect.getMpRRate() > 0) {
            if (effect.getHpRRate() > 0) {
                extraHpRec = effect.getHpR();
                extraRecInterval = effect.getHpRRate();
            }

            if (effect.getMpRRate() > 0) {
                extraMpRec = effect.getMpR();
                extraRecInterval = effect.getMpRRate();
            }

            chrLock.lock();
            try {
                stopExtraTask();
                startExtraTask(extraHpRec, extraMpRec, extraRecInterval);   // HP & MP sharing the same task holder
            } finally {
                chrLock.unlock();
            }

        } else if (effect.isMapChair()) {
            startChairTask();
        }

        prtLock.lock();
        effLock.lock();
        chrLock.lock();
        try {
            Integer sourceid = effect.getBuffSourceId();
            Map<BuffStat, BuffStatValueHolder> toDeploy;
            Map<BuffStat, BuffStatValueHolder> appliedStatups = new LinkedHashMap<>();

            for (Pair<BuffStat, Integer> ps : effect.getStatups()) {
                appliedStatups.put(ps.getLeft(), new BuffStatValueHolder(effect, starttime, ps.getRight()));
            }

            boolean active = effect.isActive(this);
            if (YamlConfig.config.server.USE_BUFF_MOST_SIGNIFICANT) {
                toDeploy = new LinkedHashMap<>();
                Map<Integer, Pair<StatEffect, Long>> retrievedEffects = new LinkedHashMap<>();
                Set<BuffStat> retrievedStats = new LinkedHashSet<>();
                for (Entry<BuffStat, BuffStatValueHolder> statup : appliedStatups.entrySet()) {
                    BuffStatValueHolder mbsvh = effects.get(statup.getKey());
                    BuffStatValueHolder statMbsvh = statup.getValue();

                    if (active) {
                        if (mbsvh == null || mbsvh.value < statMbsvh.value || (mbsvh.value == statMbsvh.value && mbsvh.effect.getStatups().size() <= statMbsvh.effect.getStatups().size())) {
                            toDeploy.put(statup.getKey(), statMbsvh);
                        } else {
                            if (!isSingletonStatup(statup.getKey())) {
                                for (Pair<BuffStat, Integer> mbs : mbsvh.effect.getStatups()) {
                                    retrievedStats.add(mbs.getLeft());
                                }
                            }
                        }
                    }

                    addItemEffectHolderCount(statup.getKey());
                }

                // should also propagate update from buffs shared with priority sourceids
                Set<BuffStat> updated = appliedStatups.keySet();
                for (BuffStatValueHolder mbsvh : this.getAllStatups()) {
                    if (isPriorityBuffSourceid(mbsvh.effect.getBuffSourceId())) {
                        for (Pair<BuffStat, Integer> p : mbsvh.effect.getStatups()) {
                            if (updated.contains(p.getLeft())) {
                                retrievedStats.add(p.getLeft());
                            }
                        }
                    }
                }

                if (!isSilent) {
                    addItemEffectHolder(sourceid, expirationtime, appliedStatups);
                    for (Entry<BuffStat, BuffStatValueHolder> statup : toDeploy.entrySet()) {
                        effects.put(statup.getKey(), statup.getValue());
                    }

                    if (active) {
                        retrievedEffects.put(sourceid, new Pair<>(effect, starttime));
                    }

                    propagateBuffEffectUpdates(retrievedEffects, retrievedStats, new LinkedHashSet<BuffStat>());
                }
            } else {
                for (Entry<BuffStat, BuffStatValueHolder> statup : appliedStatups.entrySet()) {
                    addItemEffectHolderCount(statup.getKey());
                }

                toDeploy = (active ? appliedStatups : new LinkedHashMap<BuffStat, BuffStatValueHolder>());
            }

            addItemEffectHolder(sourceid, expirationtime, appliedStatups);
            for (Entry<BuffStat, BuffStatValueHolder> statup : toDeploy.entrySet()) {
                effects.put(statup.getKey(), statup.getValue());
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
            prtLock.unlock();
        }

        updateLocalStats();
    }

    private static int getJobMapChair(Job job) {
        switch (job.getId() / 1000) {
            case 0:
                return Beginner.MAP_CHAIR;
            case 1:
                return Noblesse.MAP_CHAIR;
            default:
                return Legend.MAP_CHAIR;
        }
    }

    public boolean unregisterChairBuff() {
        if (!YamlConfig.config.server.USE_CHAIR_EXTRAHEAL) {
            return false;
        }

        int skillId = getJobMapChair(job);
        int skillLv = getSkillLevel(skillId);
        if (skillLv > 0) {
            StatEffect mapChairSkill = SkillFactory.getSkill(skillId).getEffect(skillLv);
            return cancelEffect(mapChairSkill, false, -1);
        }

        return false;
    }

    public boolean registerChairBuff() {
        if (!YamlConfig.config.server.USE_CHAIR_EXTRAHEAL) {
            return false;
        }

        int skillId = getJobMapChair(job);
        int skillLv = getSkillLevel(skillId);
        if (skillLv > 0) {
            StatEffect mapChairSkill = SkillFactory.getSkill(skillId).getEffect(skillLv);
            mapChairSkill.applyTo(this);
            return true;
        }

        return false;
    }

    public int getChair() {
        return chair.get();
    }

    public String getChalkboard() {
        return this.chalktext;
    }

    public Client getClient() {
        return client;
    }

    public AbstractPlayerInteraction getAbstractPlayerInteraction() {
        return client.getAbstractPlayerInteraction();
    }

    public List<QuestStatus> getQuests() {
        synchronized (quests) {
            return new ArrayList<>(quests.values());
        }
    }

    public final List<QuestStatus> getCompletedQuests() {
        List<QuestStatus> ret = new LinkedList<>();
        for (QuestStatus qs : getQuests()) {
            if (qs.getStatus().equals(QuestStatus.Status.COMPLETED)) {
                ret.add(qs);
            }
        }

        return Collections.unmodifiableList(ret);
    }

    public List<Ring> getCrushRings() {
        Collections.sort(crushRings);
        return crushRings;
    }

    public int getCurrentCI() {
        return ci;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getCurrentTab() {
        return currentTab;
    }

    public int getCurrentType() {
        return currentType;
    }

    public int getDojoEnergy() {
        return dojoEnergy;
    }

    public int getDojoPoints() {
        return dojoPoints;
    }

    public int getDojoStage() {
        return dojoStage;
    }

    public Collection<Door> getDoors() {
        prtLock.lock();
        try {
            return (party != null ? Collections.unmodifiableCollection(party.getDoors().values()) : (pdoor != null ? Collections.singleton(pdoor) : new LinkedHashSet<Door>()));
        } finally {
            prtLock.unlock();
        }
    }

    public Door getPlayerDoor() {
        prtLock.lock();
        try {
            return pdoor;
        } finally {
            prtLock.unlock();
        }
    }

    public Door getMainTownDoor() {
        for (Door door : getDoors()) {
            if (door.getTownPortal().getId() == 0x80) {
                return door;
            }
        }

        return null;
    }

    public void applyPartyDoor(Door door, boolean partyUpdate) {
        Party chrParty;
        prtLock.lock();
        try {
            if (!partyUpdate) {
                pdoor = door;
            }

            chrParty = getParty();
            if (chrParty != null) {
                chrParty.addDoor(id, door);
            }
        } finally {
            prtLock.unlock();
        }

        silentPartyUpdateInternal(chrParty);
    }

    public Door removePartyDoor(boolean partyUpdate) {
        Door ret = null;
        Party chrParty;

        prtLock.lock();
        try {
            chrParty = getParty();
            if (chrParty != null) {
                chrParty.removeDoor(id);
            }

            if (!partyUpdate) {
                ret = pdoor;
                pdoor = null;
            }
        } finally {
            prtLock.unlock();
        }

        silentPartyUpdateInternal(chrParty);
        return ret;
    }

    private void removePartyDoor(Party formerParty) {    // player is no longer registered at this party
        formerParty.removeDoor(id);
    }

    public int getEnergyBar() {
        return energybar;
    }

    public EventInstanceManager getEventInstance() {
        evtLock.lock();
        try {
            return eventInstance;
        } finally {
            evtLock.unlock();
        }
    }

    public Marriage getMarriageInstance() {
        EventInstanceManager eim = getEventInstance();

        if (eim != null || !(eim instanceof Marriage)) {
            return (Marriage) eim;
        } else {
            return null;
        }
    }

    public void resetExcluded(int petId) {
        chrLock.lock();
        try {
            Set<Integer> petExclude = excluded.get(petId);

            if (petExclude != null) {
                petExclude.clear();
            } else {
                excluded.put(petId, new LinkedHashSet<Integer>());
            }
        } finally {
            chrLock.unlock();
        }
    }

    public void addExcluded(int petId, int x) {
        chrLock.lock();
        try {
            excluded.get(petId).add(x);
        } finally {
            chrLock.unlock();
        }
    }

    public void commitExcludedItems() {
        Map<Integer, Set<Integer>> petExcluded = this.getExcluded();

        chrLock.lock();
        try {
            excludedItems.clear();
        } finally {
            chrLock.unlock();
        }

        for (Map.Entry<Integer, Set<Integer>> pe : petExcluded.entrySet()) {
            byte petIndex = this.getPetIndex(pe.getKey());
            if (petIndex < 0) {
                continue;
            }

            Set<Integer> exclItems = pe.getValue();
            if (!exclItems.isEmpty()) {
                sendPacket(PacketCreator.loadExceptionList(this.getId(), pe.getKey(), petIndex, new ArrayList<>(exclItems)));

                chrLock.lock();
                try {
                    for (Integer itemid : exclItems) {
                        excludedItems.add(itemid);
                    }
                } finally {
                    chrLock.unlock();
                }
            }
        }
    }

    public void exportExcludedItems(Client c) {
        Map<Integer, Set<Integer>> petExcluded = this.getExcluded();
        for (Map.Entry<Integer, Set<Integer>> pe : petExcluded.entrySet()) {
            byte petIndex = this.getPetIndex(pe.getKey());
            if (petIndex < 0) {
                continue;
            }

            Set<Integer> exclItems = pe.getValue();
            if (!exclItems.isEmpty()) {
                c.sendPacket(PacketCreator.loadExceptionList(this.getId(), pe.getKey(), petIndex, new ArrayList<>(exclItems)));
            }
        }
    }

    public Map<Integer, Set<Integer>> getExcluded() {
        chrLock.lock();
        try {
            return Collections.unmodifiableMap(excluded);
        } finally {
            chrLock.unlock();
        }
    }

    public Set<Integer> getExcludedItems() {
        chrLock.lock();
        try {
            return Collections.unmodifiableSet(excludedItems);
        } finally {
            chrLock.unlock();
        }
    }

    public int getExp() {
        return exp.get();
    }

    public int getGachaExp() {
        return gachaexp.get();
    }

    public boolean hasNoviceExpRate() {
        return YamlConfig.config.server.USE_ENFORCE_NOVICE_EXPRATE && isBeginnerJob() && level < 11;
    }

    public int getExpRate() {
        if (hasNoviceExpRate()) {   // base exp rate 1x for early levels idea thanks to Vcoc
            return 1;
        }

        return expRate;
    }

    public int getCouponExpRate() {
        return expCoupon;
    }

    public int getRawExpRate() {
        return expRate / (expCoupon * getWorldServer().getExpRate());
    }

    public int getDropRate() {
        return dropRate;
    }

    public int getCouponDropRate() {
        return dropCoupon;
    }

    public int getRawDropRate() {
        return dropRate / (dropCoupon * getWorldServer().getDropRate());
    }

    public int getBossDropRate() {
        World w = getWorldServer();
        return (dropRate / w.getDropRate()) * w.getBossDropRate();
    }

    public int getMesoRate() {
        return mesoRate;
    }

    public int getCouponMesoRate() {
        return mesoCoupon;
    }

    public int getRawMesoRate() {
        return mesoRate / (mesoCoupon * getWorldServer().getMesoRate());
    }

    public int getQuestExpRate() {
        if (hasNoviceExpRate()) {
            return 1;
        }

        World w = getWorldServer();
        return w.getExpRate() * w.getQuestRate();
    }

    public int getQuestMesoRate() {
        World w = getWorldServer();
        return w.getMesoRate() * w.getQuestRate();
    }

    public float getCardRate(int itemid) {
        float rate = 100.0f;

        if (itemid == 0) {
            StatEffect mseMeso = getBuffEffect(BuffStat.MESO_UP_BY_ITEM);
            if (mseMeso != null) {
                rate += mseMeso.getCardRate(mapid, itemid);
            }
        } else {
            StatEffect mseItem = getBuffEffect(BuffStat.ITEM_UP_BY_ITEM);
            if (mseItem != null) {
                rate += mseItem.getCardRate(mapid, itemid);
            }
        }

        return rate / 100;
    }

    public int getFace() {
        return face;
    }

    public int getFame() {
        return fame;
    }

    public Family getFamily() {
        if (familyEntry != null) {
            return familyEntry.getFamily();
        } else {
            return null;
        }
    }

    public FamilyEntry getFamilyEntry() {
        return familyEntry;
    }

    public void setFamilyEntry(FamilyEntry entry) {
        if (entry != null) {
            setFamilyId(entry.getFamily().getID());
        }
        this.familyEntry = entry;
    }

    public int getFamilyId() {
        return familyId;
    }

    public boolean getFinishedDojoTutorial() {
        return finishedDojoTutorial;
    }

    public void setUsedStorage() {
        usedStorage = true;
    }

    public void resetUsedStorage() {
        usedStorage = false;
    }

    public boolean usedStorage() {
        return usedStorage;
    }

    public List<Ring> getFriendshipRings() {
        Collections.sort(friendshipRings);
        return friendshipRings;
    }

    public int getGender() {
        return gender;
    }

    public boolean isMale() {
        return getGender() == 0;
    }

    public Guild getGuild() {
        try {
            return Server.getInstance().getGuild(getGuildId(), getWorld(), this);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public Alliance getAlliance() {
        if (mgc != null) {
            try {
                return Server.getInstance().getAlliance(getGuild().getAllianceId());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }

    public int getGuildId() {
        return guildid;
    }

    public int getGuildRank() {
        return guildRank;
    }

    public int getHair() {
        return hair;
    }

    public HiredMerchant getHiredMerchant() {
        return hiredMerchant;
    }

    public int getId() {
        return id;
    }

    public static int getAccountIdByName(String name) {
        final int id;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return -1;
                }
                id = rs.getInt("accountid");
            }
            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getIdByName(String name) {
        final int id;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id FROM characters WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return -1;
                }
                id = rs.getInt("id");
            }
            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String getNameById(int id) {
        final String name;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                name = rs.getString("name");
            }
            return name;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public Inventory getInventory(InventoryType type) {
        return inventory[type.ordinal()];
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public boolean haveItemWithId(int itemid, boolean checkEquipped) {
        return (inventory[ItemConstants.getInventoryType(itemid).ordinal()].findById(itemid) != null)
                || (checkEquipped && inventory[InventoryType.EQUIPPED.ordinal()].findById(itemid) != null);
    }

    public boolean haveItemEquipped(int itemid) {
        return (inventory[InventoryType.EQUIPPED.ordinal()].findById(itemid) != null);
    }

    public boolean haveWeddingRing() {
        int[] rings = {ItemId.WEDDING_RING_STAR, ItemId.WEDDING_RING_MOONSTONE, ItemId.WEDDING_RING_GOLDEN, ItemId.WEDDING_RING_SILVER};

        for (int ringid : rings) {
            if (haveItemWithId(ringid, true)) {
                return true;
            }
        }

        return false;
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int count = inventory[ItemConstants.getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            count += inventory[InventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return count;
    }

    public int getCleanItemQuantity(int itemid, boolean checkEquipped) {
        int count = inventory[ItemConstants.getInventoryType(itemid).ordinal()].countNotOwnedById(itemid);
        if (checkEquipped) {
            count += inventory[InventoryType.EQUIPPED.ordinal()].countNotOwnedById(itemid);
        }
        return count;
    }

    public Job getJob() {
        return job;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    public int getJobType() {
        return job.getId() / 1000;
    }

    public Map<Integer, KeyBinding> getKeymap() {
        return Collections.unmodifiableMap(keymap);
    }

    public long getLastHealed() {
        return lastHealed;
    }

    public long getLastUsedCashItem() {
        return lastUsedCashItem;
    }

    public int getLevel() {
        return level;
    }

    public int getFh() {
        Point pos = this.getPosition();
        pos.y -= 6;

        if (map.getFootholds().findBelow(pos) == null) {
            return 0;
        } else {
            return map.getFootholds().findBelow(pos).getY1();
        }
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public Ring getMarriageRing() {
        return partnerId > 0 ? marriageRing : null;
    }

    public int getMasterLevel(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }

    public int getMasterLevel(Skill skill) {
        if (skills.get(skill) == null) {
            return 0;
        }
        return skills.get(skill).masterlevel;
    }

    public int getTotalStr() {
        return localstr;
    }

    public int getTotalDex() {
        return localdex;
    }

    public int getTotalInt() {
        return localint_;
    }

    public int getTotalLuk() {
        return localluk;
    }

    public int getTotalMagic() {
        return localmagic;
    }

    public int getTotalWatk() {
        return localwatk;
    }

    public int getMaxClassLevel() {
        return isCygnus() ? 120 : 200;
    }

    public int getMaxLevel() {
        if (!YamlConfig.config.server.USE_ENFORCE_JOB_LEVEL_RANGE || isGmJob()) {
            return getMaxClassLevel();
        }

        return GameConstants.getJobMaxLevel(job);
    }

    public int getMeso() {
        return meso.get();
    }

    public int getMerchantMeso() {
        return merchantmeso;
    }

    public int getMerchantNetMeso() {
        int elapsedDays = 0;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT `timestamp` FROM `fredstorage` WHERE `cid` = ?")) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    elapsedDays = FredrickProcessor.timestampElapsedDays(rs.getTimestamp(1), System.currentTimeMillis());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (elapsedDays > 100) {
            elapsedDays = 100;
        }

        long netMeso = merchantmeso; // negative mesos issues found thanks to Flash, Vcoc
        netMeso = (netMeso * (100 - elapsedDays)) / 100;
        return (int) netMeso;
    }

    public int getMesosTraded() {
        return mesosTraded;
    }

    public int getMessengerPosition() {
        return messengerposition;
    }

    public GuildCharacter getMGC() {
        return mgc;
    }

    public void setMGC(GuildCharacter mgc) {
        this.mgc = mgc;
    }

    public PartyCharacter getMPC() {
        if (mpc == null) {
            mpc = new PartyCharacter(this);
        }
        return mpc;
    }

    public void setMPC(PartyCharacter mpc) {
        this.mpc = mpc;
    }

    public int getTargetHpBarHash() {
        return this.targetHpBarHash;
    }

    public void setTargetHpBarHash(int mobHash) {
        this.targetHpBarHash = mobHash;
    }

    public long getTargetHpBarTime() {
        return this.targetHpBarTime;
    }

    public void setTargetHpBarTime(long timeNow) {
        this.targetHpBarTime = timeNow;
    }

    public void setPlayerAggro(int mobHash) {
        setTargetHpBarHash(mobHash);
        setTargetHpBarTime(System.currentTimeMillis());
    }

    public void resetPlayerAggro() {
        if (getWorldServer().unregisterDisabledServerMessage(id)) {
            client.announceServerMessage();
        }

        setTargetHpBarHash(0);
        setTargetHpBarTime(0);
    }

    public MiniGame getMiniGame() {
        return miniGame;
    }

    public int getMiniGamePoints(MiniGameResult type, boolean omok) {
        if (omok) {
            switch (type) {
                case WIN:
                    return omokwins;
                case LOSS:
                    return omoklosses;
                default:
                    return omokties;
            }
        } else {
            switch (type) {
                case WIN:
                    return matchcardwins;
                case LOSS:
                    return matchcardlosses;
                default:
                    return matchcardties;
            }
        }
    }

    public MonsterBook getMonsterBook() {
        return monsterbook;
    }

    public int getMonsterBookCover() {
        return bookCover;
    }

    public Mount getMount() {
        return maplemount;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public String getName() {
        return name;
    }

    public int getNextEmptyPetIndex() {
        petLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (pets[i] == null) {
                    return i;
                }
            }
            return 3;
        } finally {
            petLock.unlock();
        }
    }

    public int getNoPets() {
        petLock.lock();
        try {
            int ret = 0;
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    ret++;
                }
            }
            return ret;
        } finally {
            petLock.unlock();
        }
    }

    public Party getParty() {
        prtLock.lock();
        try {
            return party;
        } finally {
            prtLock.unlock();
        }
    }

    public int getPartyId() {
        prtLock.lock();
        try {
            return (party != null ? party.getId() : -1);
        } finally {
            prtLock.unlock();
        }
    }

    public List<Character> getPartyMembersOnline() {
        List<Character> list = new LinkedList<>();

        prtLock.lock();
        try {
            if (party != null) {
                for (PartyCharacter mpc : party.getMembers()) {
                    Character mc = mpc.getPlayer();
                    if (mc != null) {
                        list.add(mc);
                    }
                }
            }
        } finally {
            prtLock.unlock();
        }

        return list;
    }

    public List<Character> getPartyMembersOnSameMap() {
        List<Character> list = new LinkedList<>();
        int thisMapHash = this.getMap().hashCode();

        prtLock.lock();
        try {
            if (party != null) {
                for (PartyCharacter mpc : party.getMembers()) {
                    Character chr = mpc.getPlayer();
                    if (chr != null) {
                        MapleMap chrMap = chr.getMap();
                        if (chrMap != null && chrMap.hashCode() == thisMapHash && chr.isLoggedinWorld()) {
                            list.add(chr);
                        }
                    }
                }
            }
        } finally {
            prtLock.unlock();
        }

        return list;
    }

    public boolean isPartyMember(Character chr) {
        return isPartyMember(chr.getId());
    }

    public boolean isPartyMember(int cid) {
        prtLock.lock();
        try {
            if (party != null) {
                return party.getMemberById(cid) != null;
            }
        } finally {
            prtLock.unlock();
        }

        return false;
    }

    public PlayerShop getPlayerShop() {
        return playerShop;
    }

    public RockPaperScissor getRPS() { // thanks inhyuk for suggesting RPS addition
        return rps;
    }

    public void setGMLevel(int level) {
        this.gmLevel = Math.min(level, 6);
        this.gmLevel = Math.max(level, 0);

        whiteChat = gmLevel >= 4;   // thanks ozanrijen for suggesting default white chat
    }

    public void closePartySearchInteractions() {
        this.getWorldServer().getPartySearchCoordinator().unregisterPartyLeader(this);
        if (canRecvPartySearchInvite) {
            this.getWorldServer().getPartySearchCoordinator().detachPlayer(this);
        }
    }

    public void closePlayerInteractions() {
        closeNpcShop();
        closeTrade();
        closePlayerShop();
        closeMiniGame(true);
        closeRPS();
        closeHiredMerchant(false);
        closePlayerMessenger();

        client.closePlayerScriptInteractions();
        resetPlayerAggro();
    }

    public void closeNpcShop() {
        setShop(null);
    }

    public void closeTrade() {
        Trade.cancelTrade(this, Trade.TradeResult.PARTNER_CANCEL);
    }

    public void closePlayerShop() {
        PlayerShop mps = this.getPlayerShop();
        if (mps == null) {
            return;
        }

        if (mps.isOwner(this)) {
            mps.setOpen(false);
            getWorldServer().unregisterPlayerShop(mps);

            for (PlayerShopItem mpsi : mps.getItems()) {
                if (mpsi.getBundles() >= 2) {
                    Item iItem = mpsi.getItem().copy();
                    iItem.setQuantity((short) (mpsi.getBundles() * iItem.getQuantity()));
                    InventoryManipulator.addFromDrop(this.getClient(), iItem, false);
                } else if (mpsi.isExist()) {
                    InventoryManipulator.addFromDrop(this.getClient(), mpsi.getItem(), true);
                }
            }
            mps.closeShop();
        } else {
            mps.removeVisitor(this);
        }
        this.setPlayerShop(null);
    }

    public void closeMiniGame(boolean forceClose) {
        MiniGame game = this.getMiniGame();
        if (game == null) {
            return;
        }

        if (game.isOwner(this)) {
            game.closeRoom(forceClose);
        } else {
            game.removeVisitor(forceClose, this);
        }
    }

    public void closeHiredMerchant(boolean closeMerchant) {
        HiredMerchant merchant = this.getHiredMerchant();
        if (merchant == null) {
            return;
        }

        if (closeMerchant) {
            if (merchant.isOwner(this) && merchant.getItems().isEmpty()) {
                merchant.forceClose();
            } else {
                merchant.removeVisitor(this);
                this.setHiredMerchant(null);
            }
        } else {
            if (merchant.isOwner(this)) {
                merchant.setOpen(true);
            } else {
                merchant.removeVisitor(this);
            }
            try {
                merchant.saveItems(false);
            } catch (SQLException e) {
                log.error("Error while saving {}'s Hired Merchant items.", name, e);
            }
        }
    }

    public void closePlayerMessenger() {
        Messenger m = this.getMessenger();
        if (m == null) {
            return;
        }

        World w = getWorldServer();
        MessengerCharacter messengerplayer = new MessengerCharacter(this, this.getMessengerPosition());

        w.leaveMessenger(m.getId(), messengerplayer);
        this.setMessenger(null);
        this.setMessengerPosition(4);
    }

    public Pet[] getPets() {
        petLock.lock();
        try {
            return Arrays.copyOf(pets, pets.length);
        } finally {
            petLock.unlock();
        }
    }

    public Pet getPet(int index) {
        if (index < 0) {
            return null;
        }

        petLock.lock();
        try {
            return pets[index];
        } finally {
            petLock.unlock();
        }
    }

    public byte getPetIndex(int petId) {
        petLock.lock();
        try {
            for (byte i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    if (pets[i].getUniqueId() == petId) {
                        return i;
                    }
                }
            }
            return -1;
        } finally {
            petLock.unlock();
        }
    }

    public byte getPetIndex(Pet pet) {
        petLock.lock();
        try {
            for (byte i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    if (pets[i].getUniqueId() == pet.getUniqueId()) {
                        return i;
                    }
                }
            }
            return -1;
        } finally {
            petLock.unlock();
        }
    }

    public int getPossibleReports() {
        return possibleReports;
    }

    public final byte getQuestStatus(final int quest) {
        synchronized (quests) {
            QuestStatus mqs = quests.get((short) quest);
            if (mqs != null) {
                return (byte) mqs.getStatus().getId();
            } else {
                return 0;
            }
        }
    }

    public QuestStatus getQuest(final int quest) {
        return getQuest(Quest.getInstance(quest));
    }

    public QuestStatus getQuest(Quest quest) {
        synchronized (quests) {
            short questid = quest.getId();
            QuestStatus qs = quests.get(questid);
            if (qs == null) {
                qs = new QuestStatus(quest, QuestStatus.Status.NOT_STARTED);
                quests.put(questid, qs);
            }
            return qs;
        }
    }

    //---- \/ \/ \/ \/ \/ \/ \/  NOT TESTED  \/ \/ \/ \/ \/ \/ \/ \/ \/ ----

    public final void setQuestAdd(final Quest quest, final byte status, final String customData) {
        synchronized (quests) {
            if (!quests.containsKey(quest.getId())) {
                final QuestStatus stat = new QuestStatus(quest, QuestStatus.Status.getById(status));
                stat.setCustomData(customData);
                quests.put(quest.getId(), stat);
            }
        }
    }

    public final QuestStatus getQuestNAdd(final Quest quest) {
        synchronized (quests) {
            if (!quests.containsKey(quest.getId())) {
                final QuestStatus status = new QuestStatus(quest, QuestStatus.Status.NOT_STARTED);
                quests.put(quest.getId(), status);
                return status;
            }
            return quests.get(quest.getId());
        }
    }

    public final QuestStatus getQuestNoAdd(final Quest quest) {
        synchronized (quests) {
            return quests.get(quest.getId());
        }
    }

    public final QuestStatus getQuestRemove(final Quest quest) {
        synchronized (quests) {
            return quests.remove(quest.getId());
        }
    }

    //---- /\ /\ /\ /\ /\ /\ /\  NOT TESTED  /\ /\ /\ /\ /\ /\ /\ /\ /\ ----

    public boolean needQuestItem(int questid, int itemid) {
        if (questid <= 0) { //For non quest items :3
            return true;
        }

        int amountNeeded, questStatus = this.getQuestStatus(questid);
        if (questStatus == 0) {
            amountNeeded = Quest.getInstance(questid).getStartItemAmountNeeded(itemid);
            if (amountNeeded == Integer.MIN_VALUE) {
                return false;
            }
        } else if (questStatus != 1) {
            return false;
        } else {
            amountNeeded = Quest.getInstance(questid).getCompleteItemAmountNeeded(itemid);
            if (amountNeeded == Integer.MAX_VALUE) {
                return true;
            }
        }

        return getInventory(ItemConstants.getInventoryType(itemid)).countById(itemid) < amountNeeded;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public SavedLocation[] getSavedLocations() {
        return Arrays.copyOf(savedLocations, savedLocations.length);
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = null;
    }

    public int peekSavedLocation(String type) {
        SavedLocation sl = savedLocations[SavedLocationType.fromString(type).ordinal()];
        if (sl == null) {
            return -1;
        }
        return sl.getMapId();
    }

    public int getSavedLocation(String type) {
        int m = peekSavedLocation(type);
        clearSavedLocation(SavedLocationType.fromString(type));

        return m;
    }

    public String getSearch() {
        return search;
    }

    public Shop getShop() {
        return shop;
    }

    public Map<Skill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public int getSkillLevel(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return 0;
        }
        return ret.skillevel;
    }

    public byte getSkillLevel(Skill skill) {
        if (skills.get(skill) == null) {
            return 0;
        }
        return skills.get(skill).skillevel;
    }

    public long getSkillExpiration(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return -1;
        }
        return ret.expiration;
    }

    public long getSkillExpiration(Skill skill) {
        if (skills.get(skill) == null) {
            return -1;
        }
        return skills.get(skill).expiration;
    }

    public SkinColor getSkinColor() {
        return skinColor;
    }

    public int getSlot() {
        return slots;
    }

    public final List<QuestStatus> getStartedQuests() {
        List<QuestStatus> ret = new LinkedList<>();
        for (QuestStatus qs : getQuests()) {
            if (qs.getStatus().equals(QuestStatus.Status.STARTED)) {
                ret.add(qs);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public StatEffect getStatForBuff(BuffStat effect) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return null;
            }
            return mbsvh.effect;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public Storage getStorage() {
        return storage;
    }

    public Collection<Summon> getSummonsValues() {
        return summons.values();
    }

    public void clearSummons() {
        summons.clear();
    }

    public Summon getSummonByKey(int id) {
        return summons.get(id);
    }

    public boolean isSummonsEmpty() {
        return summons.isEmpty();
    }

    public boolean containsSummon(Summon summon) {
        return summons.containsValue(summon);
    }

    public Trade getTrade() {
        return trade;
    }

    public int getVanquisherKills() {
        return vanquisherKills;
    }

    public int getVanquisherStage() {
        return vanquisherStage;
    }

    public MapObject[] getVisibleMapObjects() {
        return visibleMapObjects.toArray(new MapObject[visibleMapObjects.size()]);
    }

    public int getWorld() {
        return world;
    }

    public World getWorldServer() {
        return Server.getInstance().getWorld(world);
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        if (skillid == 5221999) {
            this.battleshipHp = (int) length;
            addCooldown(skillid, 0, length);
        } else {
            long timeNow = Server.getInstance().getCurrentTime();
            int time = (int) ((length + starttime) - timeNow);
            addCooldown(skillid, timeNow, time);
        }
    }

    public int gmLevel() {
        return gmLevel;
    }

    private void guildUpdate() {
        mgc.setLevel(level);
        mgc.setJobId(job.getId());

        if (this.guildid < 1) {
            return;
        }

        try {
            Server.getInstance().memberLevelJobUpdate(this.mgc);
            //Server.getInstance().getGuild(guildid, world, mgc).gainGP(40);
            int allianceId = getGuild().getAllianceId();
            if (allianceId > 0) {
                Server.getInstance().allianceMessage(allianceId, GuildPackets.updateAllianceJobLevel(this), getId(), -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleEnergyChargeGain() { // to get here energychargelevel has to be > 0
        Skill energycharge = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.ENERGY_CHARGE) : SkillFactory.getSkill(Marauder.ENERGY_CHARGE);
        StatEffect ceffect;
        ceffect = energycharge.getEffect(getSkillLevel(energycharge));
        TimerManager tMan = TimerManager.getInstance();
        if (energybar < 10000) {
            energybar += 102;
            if (energybar > 10000) {
                energybar = 10000;
            }
            List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.ENERGY_CHARGE, energybar));
            setBuffedValue(BuffStat.ENERGY_CHARGE, energybar);
            sendPacket(PacketCreator.giveBuff(energybar, 0, stat));
            sendPacket(PacketCreator.showOwnBuffEffect(energycharge.getId(), 2));
            getMap().broadcastPacket(this, PacketCreator.showBuffEffect(id, energycharge.getId(), 2));
            getMap().broadcastPacket(this, PacketCreator.giveForeignPirateBuff(id, energycharge.getId(),
                    ceffect.getDuration(), stat));
        }
        if (energybar >= 10000 && energybar < 11000) {
            energybar = 15000;
            final Character chr = this;
            tMan.schedule(new Runnable() {
                @Override
                public void run() {
                    energybar = 0;
                    List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.ENERGY_CHARGE, energybar));
                    setBuffedValue(BuffStat.ENERGY_CHARGE, energybar);
                    sendPacket(PacketCreator.giveBuff(energybar, 0, stat));
                    getMap().broadcastPacket(chr, PacketCreator.cancelForeignFirstDebuff(id, ((long) 1) << 50));
                }
            }, ceffect.getDuration());
        }
    }

    public void handleOrbconsume() {
        int skillid = isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
        Skill combo = SkillFactory.getSkill(skillid);
        List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.COMBO, 1));
        setBuffedValue(BuffStat.COMBO, 1);
        sendPacket(PacketCreator.giveBuff(skillid, combo.getEffect(getSkillLevel(combo)).getDuration() + (int) ((getBuffedStarttime(BuffStat.COMBO) - System.currentTimeMillis())), stat));
        getMap().broadcastMessage(this, PacketCreator.giveForeignBuff(getId(), stat), false);
    }

    public boolean hasEntered(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEntered(String script, int mapId) {
        String e = entered.get(mapId);
        return script.equals(e);
    }

    public void hasGivenFame(Character to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(Integer.valueOf(to.getId()));
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)")) {
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasMerchant() {
        return hasMerchant;
    }

    public boolean haveItem(int itemid) {
        return getItemQuantity(itemid, ItemConstants.isEquipment(itemid)) > 0;
    }

    public boolean haveCleanItem(int itemid) {
        return getCleanItemQuantity(itemid, ItemConstants.isEquipment(itemid)) > 0;
    }

    public boolean hasEmptySlot(int itemId) {
        return getInventory(ItemConstants.getInventoryType(itemId)).getNextFreeSlot() > -1;
    }

    public boolean hasEmptySlot(byte invType) {
        return getInventory(InventoryType.getByType(invType)).getNextFreeSlot() > -1;
    }

    public void increaseGuildCapacity() {
        int cost = Guild.getIncreaseGuildCost(getGuild().getCapacity());

        if (getMeso() < cost) {
            dropMessage(1, "You don't have enough mesos.");
            return;
        }

        if (Server.getInstance().increaseGuildCapacity(guildid)) {
            gainMeso(-cost, true, false, true);
        } else {
            dropMessage(1, "Your guild already reached the maximum capacity of players.");
        }
    }

    private static String getTimeRemaining(long timeLeft) {
        int seconds = (int) Math.floor(timeLeft / SECONDS.toMillis(1)) % 60;
        int minutes = (int) Math.floor(timeLeft / MINUTES.toMillis(1)) % 60;

        return (minutes > 0 ? (String.format("%02d", minutes) + " minutes, ") : "") + String.format("%02d", seconds) + " seconds";
    }

    public boolean isBuffFrom(BuffStat stat, Skill skill) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(stat);
            if (mbsvh == null) {
                return false;
            }
            return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public boolean isGmJob() {
        int jn = job.getJobNiche();
        return jn >= 8 && jn <= 9;
    }

    public boolean isCygnus() {
        return getJobType() == 1;
    }

    public boolean isAran() {
        return job.getId() >= 2000 && job.getId() <= 2112;
    }

    public boolean isBeginnerJob() {
        return (job.getId() == 0 || job.getId() == 1000 || job.getId() == 2000);
    }

    public boolean isGM() {
        return gmLevel > 1;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isMapObjectVisible(MapObject mo) {
        return visibleMapObjects.contains(mo);
    }

    public boolean isPartyLeader() {
        prtLock.lock();
        try {
            Party party = getParty();
            return party != null && party.getLeaderId() == getId();
        } finally {
            prtLock.unlock();
        }
    }

    public boolean isGuildLeader() {    // true on guild master or jr. master
        return guildid > 0 && guildRank < 3;
    }

    public boolean attemptCatchFish(int baitLevel) {
        return YamlConfig.config.server.USE_FISHING_SYSTEM && MapId.isFishingArea(mapid) &&
                this.getPosition().getY() > 0 &&
                ItemConstants.isFishingChair(chair.get()) &&
                this.getWorldServer().registerFisherPlayer(this, baitLevel);
    }

    public void leaveMap() {
        releaseControlledMonsters();
        visibleMapObjects.clear();
        setChair(-1);
        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(false);
        }

        AriantColiseum arena = this.getAriantColiseum();
        if (arena != null) {
            arena.leaveArena(this);
        }
    }

    private int getChangedJobSp(Job newJob) {
        int curSp = getUsedSp(newJob) + getJobRemainingSp(newJob);
        int spGain = 0;
        int expectedSp = getJobLevelSp(level - 10, newJob, GameConstants.getJobBranch(newJob));
        if (curSp < expectedSp) {
            spGain += (expectedSp - curSp);
        }

        return getSpGain(spGain, curSp, newJob);
    }

    private int getUsedSp(Job job) {
        int jobId = job.getId();
        int spUsed = 0;

        for (Entry<Skill, SkillEntry> s : this.getSkills().entrySet()) {
            Skill skill = s.getKey();
            if (GameConstants.isInJobTree(skill.getId(), jobId) && !skill.isBeginnerSkill()) {
                spUsed += s.getValue().skillevel;
            }
        }

        return spUsed;
    }

    private int getJobLevelSp(int level, Job job, int jobBranch) {
        if (getJobStyleInternal(job.getId(), (byte) 0x40) == Job.MAGICIAN) {
            level += 2;  // starts earlier, level 8
        }

        return 3 * level + GameConstants.getChangeJobSpUpgrade(jobBranch);
    }

    private int getJobMaxSp(Job job) {
        int jobBranch = GameConstants.getJobBranch(job);
        int jobRange = GameConstants.getJobUpgradeLevelRange(jobBranch);
        return getJobLevelSp(jobRange, job, jobBranch);
    }

    private int getJobRemainingSp(Job job) {
        int skillBook = GameConstants.getSkillBook(job.getId());

        int ret = 0;
        for (int i = 0; i <= skillBook; i++) {
            ret += this.getRemainingSp(i);
        }

        return ret;
    }

    private int getSpGain(int spGain, Job job) {
        int curSp = getUsedSp(job) + getJobRemainingSp(job);
        return getSpGain(spGain, curSp, job);
    }

    private int getSpGain(int spGain, int curSp, Job job) {
        int maxSp = getJobMaxSp(job);

        spGain = Math.min(spGain, maxSp - curSp);
        int jobBranch = GameConstants.getJobBranch(job);
        return spGain;
    }

    private void levelUpGainSp() {
        if (GameConstants.getJobBranch(job) == 0) {
            return;
        }

        int spGain = 3;
        if (YamlConfig.config.server.USE_ENFORCE_JOB_SP_RANGE && !GameConstants.hasSPTable(job)) {
            spGain = getSpGain(spGain, job);
        }

        if (spGain > 0) {
            gainSp(spGain, GameConstants.getSkillBook(job.getId()), true);
        }
    }

    public synchronized void levelUp(boolean takeexp) {
        Skill improvingMaxHP = null;
        Skill improvingMaxMP = null;
        int improvingMaxHPLevel = 0;
        int improvingMaxMPLevel = 0;

        boolean isBeginner = isBeginnerJob();
        if (YamlConfig.config.server.USE_AUTOASSIGN_STARTERS_AP && isBeginner && level < 11) {
            effLock.lock();
            statWlock.lock();
            try {
                gainAp(5, true);

                int str = 0, dex = 0;
                if (level < 6) {
                    str += 5;
                } else {
                    str += 4;
                    dex += 1;
                }

                assignStrDexIntLuk(str, dex, 0, 0);
            } finally {
                statWlock.unlock();
                effLock.unlock();
            }
        } else {
            int remainingAp = 5;

            if (isCygnus()) {
                if (level > 10) {
                    if (level <= 17) {
                        remainingAp += 2;
                    } else if (level < 77) {
                        remainingAp++;
                    }
                }
            }

            gainAp(remainingAp, true);
        }

        int addhp = 0, addmp = 0;
        if (isBeginner) {
            addhp += Randomizer.rand(12, 16);
            addmp += Randomizer.rand(10, 12);
        } else if (job.isA(Job.WARRIOR) || job.isA(Job.DAWNWARRIOR1)) {
            improvingMaxHP = isCygnus() ? SkillFactory.getSkill(DawnWarrior.MAX_HP_INCREASE) : SkillFactory.getSkill(Warrior.IMPROVED_MAXHP);
            if (job.isA(Job.CRUSADER)) {
                improvingMaxMP = SkillFactory.getSkill(1210000);
            } else if (job.isA(Job.DAWNWARRIOR2)) {
                improvingMaxMP = SkillFactory.getSkill(11110000);
            }
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            addhp += Randomizer.rand(24, 28);
            addmp += Randomizer.rand(4, 6);
        } else if (job.isA(Job.MAGICIAN) || job.isA(Job.BLAZEWIZARD1)) {
            improvingMaxMP = isCygnus() ? SkillFactory.getSkill(BlazeWizard.INCREASING_MAX_MP) : SkillFactory.getSkill(Magician.IMPROVED_MAX_MP_INCREASE);
            improvingMaxMPLevel = getSkillLevel(improvingMaxMP);
            addhp += Randomizer.rand(10, 14);
            addmp += Randomizer.rand(22, 24);
        } else if (job.isA(Job.BOWMAN) || job.isA(Job.THIEF) || (job.getId() > 1299 && job.getId() < 1500)) {
            addhp += Randomizer.rand(20, 24);
            addmp += Randomizer.rand(14, 16);
        } else if (job.isA(Job.GM)) {
            addhp += 30000;
            addmp += 30000;
        } else if (job.isA(Job.PIRATE) || job.isA(Job.THUNDERBREAKER1)) {
            improvingMaxHP = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.IMPROVE_MAX_HP) : SkillFactory.getSkill(Brawler.IMPROVE_MAX_HP);
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            addhp += Randomizer.rand(22, 28);
            addmp += Randomizer.rand(18, 23);
        } else if (job.isA(Job.ARAN1)) {
            addhp += Randomizer.rand(44, 48);
            int aids = Randomizer.rand(4, 8);
            addmp += aids + Math.floor(aids * 0.1);
        }
        if (improvingMaxHPLevel > 0 && (job.isA(Job.WARRIOR) || job.isA(Job.PIRATE) || job.isA(Job.DAWNWARRIOR1) || job.isA(Job.THUNDERBREAKER1))) {
            addhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
        }
        if (improvingMaxMPLevel > 0 && (job.isA(Job.MAGICIAN) || job.isA(Job.CRUSADER) || job.isA(Job.BLAZEWIZARD1))) {
            addmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
        }

        if (YamlConfig.config.server.USE_RANDOMIZE_HPMP_GAIN) {
            if (getJobStyle() == Job.MAGICIAN) {
                addmp += localint_ / 20;
            } else {
                addmp += localint_ / 10;
            }
        }

        addMaxMPMaxHP(addhp, addmp, true);

        if (takeexp) {
            exp.addAndGet(-ExpTable.getExpNeededForLevel(level));
            if (exp.get() < 0) {
                exp.set(0);
            }
        }

        level++;
        if (level >= getMaxClassLevel()) {
            exp.set(0);

            int maxClassLevel = getMaxClassLevel();
            if (level == maxClassLevel) {
                if (!this.isGM()) {
                    if (YamlConfig.config.server.PLAYERNPC_AUTODEPLOY) {
                        ThreadManager.getInstance().newTask(new Runnable() {
                            @Override
                            public void run() {
                                PlayerNPC.spawnPlayerNPC(GameConstants.getHallOfFameMapid(job), Character.this);
                            }
                        });
                    }

                    final String names = (getMedalText() + name);
                    getWorldServer().broadcastPacket(PacketCreator.serverNotice(6, String.format(LEVEL_200, names, maxClassLevel, names)));
                }
            }

            level = maxClassLevel; //To prevent levels past the maximum
        }

        levelUpGainSp();

        effLock.lock();
        statWlock.lock();
        try {
            recalcLocalStats();
            changeHpMp(localmaxhp, localmaxmp, true);

            List<Pair<Stat, Integer>> statup = new ArrayList<>(10);
            statup.add(new Pair<>(Stat.AVAILABLEAP, remainingAp));
            statup.add(new Pair<>(Stat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job.getId())]));
            statup.add(new Pair<>(Stat.HP, hp));
            statup.add(new Pair<>(Stat.MP, mp));
            statup.add(new Pair<>(Stat.EXP, exp.get()));
            statup.add(new Pair<>(Stat.LEVEL, level));
            statup.add(new Pair<>(Stat.MAXHP, clientmaxhp));
            statup.add(new Pair<>(Stat.MAXMP, clientmaxmp));
            statup.add(new Pair<>(Stat.STR, str));
            statup.add(new Pair<>(Stat.DEX, dex));

            sendPacket(PacketCreator.updatePlayerStats(statup, true, this));
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }

        getMap().broadcastMessage(this, PacketCreator.showForeignEffect(getId(), 0), false);
        setMPC(new PartyCharacter(this));
        silentPartyUpdate();

        if (this.guildid > 0) {
            getGuild().broadcast(PacketCreator.levelUpMessage(2, level, name), this.getId());
        }

        if (level % 20 == 0) {
            if (YamlConfig.config.server.USE_ADD_RATES_BY_LEVEL == true) { //For the rate upgrade
                revertLastPlayerRates();
                setPlayerRates();
                this.yellowMessage("You managed to get level " + level + "! Getting experience and items seems a little easier now, huh?");
            }
        }

        if (YamlConfig.config.server.USE_PERFECT_PITCH && level >= 30) {
            //milestones?
            if (InventoryManipulator.checkSpace(client, ItemId.PERFECT_PITCH, (short) 1, "")) {
                InventoryManipulator.addById(client, ItemId.PERFECT_PITCH, (short) 1, "", -1);
            }
        } else if (level == 10) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (leaveParty()) {
                        showHint("You have reached #blevel 10#k, therefore you must leave your #rstarter party#k.");
                    }
                }
            };

            ThreadManager.getInstance().newTask(r);
        }

        guildUpdate();

        FamilyEntry familyEntry = getFamilyEntry();
        if (familyEntry != null) {
            familyEntry.giveReputationToSenior(YamlConfig.config.server.FAMILY_REP_PER_LEVELUP, true);
            FamilyEntry senior = familyEntry.getSenior();
            if (senior != null) { //only send the message to direct senior
                Character seniorChr = senior.getChr();
                if (seniorChr != null) {
                    seniorChr.sendPacket(PacketCreator.levelUpMessage(1, level, getName()));
                }
            }
        }
    }

    public boolean leaveParty() {
        Party party;
        boolean partyLeader;

        prtLock.lock();
        try {
            party = getParty();
            partyLeader = isPartyLeader();
        } finally {
            prtLock.unlock();
        }

        if (party != null) {
            if (partyLeader) {
                party.assignNewLeader(client);
            }
            Party.leaveParty(party, client);

            return true;
        } else {
            return false;
        }
    }
    public void setPlayerRates() {
        this.expRate *= GameConstants.getPlayerBonusExpRate(this.level / 20);
        this.mesoRate *= GameConstants.getPlayerBonusMesoRate(this.level / 20);
        this.dropRate *= GameConstants.getPlayerBonusDropRate(this.level / 20);
    }

    public void revertLastPlayerRates() {
        this.expRate /= GameConstants.getPlayerBonusExpRate((this.level - 1) / 20);
        this.mesoRate /= GameConstants.getPlayerBonusMesoRate((this.level - 1) / 20);
        this.dropRate /= GameConstants.getPlayerBonusDropRate((this.level - 1) / 20);
    }

    public void revertPlayerRates() {
        this.expRate /= GameConstants.getPlayerBonusExpRate(this.level / 20);
        this.mesoRate /= GameConstants.getPlayerBonusMesoRate(this.level / 20);
        this.dropRate /= GameConstants.getPlayerBonusDropRate(this.level / 20);
    }

    public void setWorldRates() {
        World worldz = getWorldServer();
        this.expRate *= worldz.getExpRate();
        this.mesoRate *= worldz.getMesoRate();
        this.dropRate *= worldz.getDropRate();
    }

    public void revertWorldRates() {
        World worldz = getWorldServer();
        this.expRate /= worldz.getExpRate();
        this.mesoRate /= worldz.getMesoRate();
        this.dropRate /= worldz.getDropRate();
    }

    private void setCouponRates() {
        List<Integer> couponEffects;

        Collection<Item> cashItems = this.getInventory(InventoryType.CASH).list();
        chrLock.lock();
        try {
            setActiveCoupons(cashItems);
            couponEffects = activateCouponsEffects();
        } finally {
            chrLock.unlock();
        }

        for (Integer couponId : couponEffects) {
            commitBuffCoupon(couponId);
        }
    }

    private void revertCouponRates() {
        revertCouponsEffects();
    }

    public void updateCouponRates() {
        Inventory cashInv = this.getInventory(InventoryType.CASH);
        if (cashInv == null) {
            return;
        }

        effLock.lock();
        chrLock.lock();
        cashInv.lockInventory();
        try {
            revertCouponRates();
            setCouponRates();
        } finally {
            cashInv.unlockInventory();
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void resetPlayerRates() {
        expRate = 1;
        mesoRate = 1;
        dropRate = 1;

        expCoupon = 1;
        mesoCoupon = 1;
        dropCoupon = 1;
    }

    private int getCouponMultiplier(int couponId) {
        return activeCouponRates.get(couponId);
    }

    private void setExpCouponRate(int couponId, int couponQty) {
        this.expCoupon *= (getCouponMultiplier(couponId) * couponQty);
    }

    private void setDropCouponRate(int couponId, int couponQty) {
        this.dropCoupon *= (getCouponMultiplier(couponId) * couponQty);
        this.mesoCoupon *= (getCouponMultiplier(couponId) * couponQty);
    }

    private void revertCouponsEffects() {
        dispelBuffCoupons();

        this.expRate /= this.expCoupon;
        this.dropRate /= this.dropCoupon;
        this.mesoRate /= this.mesoCoupon;

        this.expCoupon = 1;
        this.dropCoupon = 1;
        this.mesoCoupon = 1;
    }

    private List<Integer> activateCouponsEffects() {
        List<Integer> toCommitEffect = new LinkedList<>();

        if (YamlConfig.config.server.USE_STACK_COUPON_RATES) {
            for (Entry<Integer, Integer> coupon : activeCoupons.entrySet()) {
                int couponId = coupon.getKey();
                int couponQty = coupon.getValue();

                toCommitEffect.add(couponId);

                if (ItemConstants.isExpCoupon(couponId)) {
                    setExpCouponRate(couponId, couponQty);
                } else {
                    setDropCouponRate(couponId, couponQty);
                }
            }
        } else {
            int maxExpRate = 1, maxDropRate = 1, maxExpCouponId = -1, maxDropCouponId = -1;

            for (Entry<Integer, Integer> coupon : activeCoupons.entrySet()) {
                int couponId = coupon.getKey();

                if (ItemConstants.isExpCoupon(couponId)) {
                    if (maxExpRate < getCouponMultiplier(couponId)) {
                        maxExpCouponId = couponId;
                        maxExpRate = getCouponMultiplier(couponId);
                    }
                } else {
                    if (maxDropRate < getCouponMultiplier(couponId)) {
                        maxDropCouponId = couponId;
                        maxDropRate = getCouponMultiplier(couponId);
                    }
                }
            }

            if (maxExpCouponId > -1) {
                toCommitEffect.add(maxExpCouponId);
            }
            if (maxDropCouponId > -1) {
                toCommitEffect.add(maxDropCouponId);
            }

            this.expCoupon = maxExpRate;
            this.dropCoupon = maxDropRate;
            this.mesoCoupon = maxDropRate;
        }

        this.expRate *= this.expCoupon;
        this.dropRate *= this.dropCoupon;
        this.mesoRate *= this.mesoCoupon;

        return toCommitEffect;
    }

    private void setActiveCoupons(Collection<Item> cashItems) {
        activeCoupons.clear();
        activeCouponRates.clear();

        Map<Integer, Integer> coupons = Server.getInstance().getCouponRates();
        List<Integer> active = Server.getInstance().getActiveCoupons();

        for (Item it : cashItems) {
            if (ItemConstants.isRateCoupon(it.getItemId()) && active.contains(it.getItemId())) {
                Integer count = activeCoupons.get(it.getItemId());

                if (count != null) {
                    activeCoupons.put(it.getItemId(), count + 1);
                } else {
                    activeCoupons.put(it.getItemId(), 1);
                    activeCouponRates.put(it.getItemId(), coupons.get(it.getItemId()));
                }
            }
        }
    }

    private void commitBuffCoupon(int couponid) {
        if (!isLoggedin() || getCashShop().isOpened()) {
            return;
        }

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        StatEffect mse = ii.getItemEffect(couponid);
        mse.applyTo(this);
    }

    public void dispelBuffCoupons() {
        List<BuffStatValueHolder> allBuffs = getAllStatups();

        for (BuffStatValueHolder mbsvh : allBuffs) {
            if (ItemConstants.isRateCoupon(mbsvh.effect.getSourceId())) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public Set<Integer> getActiveCoupons() {
        chrLock.lock();
        try {
            return Collections.unmodifiableSet(activeCoupons.keySet());
        } finally {
            chrLock.unlock();
        }
    }

    public void addPlayerRing(Ring ring) {
        int ringItemId = ring.getItemId();
        if (ItemId.isWeddingRing(ringItemId)) {
            this.addMarriageRing(ring);
        } else if (ring.getItemId() > 1112012) {
            this.addFriendshipRing(ring);
        } else {
            this.addCrushRing(ring);
        }
    }

    public static Character loadCharacterViewFromDB(ResultSet rs, List<Item> equipped) {
        Character ret = new Character();

        try {
            ret.accountid = rs.getInt("accountid");
            ret.id = rs.getInt("id");
            ret.name = rs.getString("name");
            ret.gender = rs.getInt("gender");
            ret.skinColor = SkinColor.getById(rs.getInt("skincolor"));
            ret.face = rs.getInt("face");
            ret.hair = rs.getInt("hair");

            // skipping pets, probably unneeded here

            ret.level = rs.getInt("level");
            ret.job = Job.getById(rs.getInt("job"));
            ret.str = rs.getInt("str");
            ret.dex = rs.getInt("dex");
            ret.int_ = rs.getInt("int");
            ret.luk = rs.getInt("luk");
            ret.hp = rs.getInt("hp");
            ret.setMaxHp(rs.getInt("maxhp"));
            ret.mp = rs.getInt("mp");
            ret.setMaxMp(rs.getInt("maxmp"));
            ret.remainingAp = rs.getInt("ap");
            ret.loadCharSkillPoints(rs.getString("sp").split(","));
            ret.exp.set(rs.getInt("exp"));
            ret.fame = rs.getInt("fame");
            ret.gachaexp.set(rs.getInt("gachaexp"));
            ret.mapid = rs.getInt("map");
            ret.initialSpawnPoint = rs.getInt("spawnpoint");
            ret.setGMLevel(rs.getInt("gm"));
            ret.world = rs.getByte("world");
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");

            if (equipped != null) {  // players can have no equipped items at all, ofc
                Inventory inv = ret.inventory[InventoryType.EQUIPPED.ordinal()];
                for (Item item : equipped) {
                    inv.addItemFromDB(item);
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return ret;
    }

    public Character createCharacterView() {
        Character ret = new Character();

        ret.accountid = this.getAccountID();
        ret.id = this.getId();
        ret.name = this.getName();
        ret.gender = this.getGender();
        ret.skinColor = this.getSkinColor();
        ret.face = this.getFace();
        ret.hair = this.getHair();

        // skipping pets, probably unneeded here

        ret.level = this.getLevel();
        ret.job = this.getJob();
        ret.str = this.getStr();
        ret.dex = this.getDex();
        ret.int_ = this.getInt();
        ret.luk = this.getLuk();
        ret.hp = this.getHp();
        ret.setMaxHp(this.getMaxHp());
        ret.mp = this.getMp();
        ret.setMaxMp(this.getMaxMp());
        ret.remainingAp = this.getRemainingAp();
        ret.setRemainingSp(this.getRemainingSps());
        ret.exp.set(this.getExp());
        ret.fame = this.getFame();
        ret.gachaexp.set(this.getGachaExp());
        ret.mapid = this.getMapId();
        ret.initialSpawnPoint = this.getInitialSpawnpoint();

        ret.inventory[InventoryType.EQUIPPED.ordinal()] = this.getInventory(InventoryType.EQUIPPED);

        ret.setGMLevel(this.gmLevel());
        ret.world = this.getWorld();
        ret.rank = this.getRank();
        ret.rankMove = this.getRankMove();
        ret.jobRank = this.getJobRank();
        ret.jobRankMove = this.getJobRankMove();

        return ret;
    }

    private void loadCharSkillPoints(String[] skillPoints) {
        int[] sps = new int[skillPoints.length];
        for (int i = 0; i < skillPoints.length; i++) {
            sps[i] = Integer.parseInt(skillPoints[i]);
        }

        setRemainingSp(sps);
    }

    public int getRemainingSp() {
        return getRemainingSp(job.getId()); //default
    }

    public void updateRemainingSp(int remainingSp) {
        updateRemainingSp(remainingSp, GameConstants.getSkillBook(job.getId()));
    }

    @Deprecated
    public static Character loadCharFromDB(int chrId, Client client, boolean channelServer) throws SQLException {
        return loadCharFromDB(chrId, client, channelServer, new MonsterBook(Collections.emptyList()));
    }

    public static Character loadCharFromDB(final int chrId, Client client, boolean channelServer,
                                           MonsterBook monsterBook) throws SQLException {
        Character ret = new Character();
        ret.client = client;
        ret.id = chrId;

        try (Connection con = DatabaseConnection.getConnection()) {
            final int mountexp;
            final int mountlevel;
            final int mounttiredness;
            final World wserv;

            // Character info
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?")) {
                ps.setInt(1, chrId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new RuntimeException("Loading char failed (not found)");
                    }

                    ret.name = rs.getString("name");
                    ret.level = rs.getInt("level");
                    ret.fame = rs.getInt("fame");
                    ret.str = rs.getInt("str");
                    ret.dex = rs.getInt("dex");
                    ret.int_ = rs.getInt("int");
                    ret.luk = rs.getInt("luk");
                    ret.exp.set(rs.getInt("exp"));
                    ret.gachaexp.set(rs.getInt("gachaexp"));
                    ret.hp = rs.getInt("hp");
                    ret.setMaxHp(rs.getInt("maxhp"));
                    ret.mp = rs.getInt("mp");
                    ret.setMaxMp(rs.getInt("maxmp"));
                    ret.hpMpApUsed = rs.getInt("hpMpUsed");
                    ret.hasMerchant = rs.getInt("HasMerchant") == 1;
                    ret.remainingAp = rs.getInt("ap");
                    ret.loadCharSkillPoints(rs.getString("sp").split(","));
                    ret.meso.set(rs.getInt("meso"));
                    ret.merchantmeso = rs.getInt("MerchantMesos");
                    ret.setGMLevel(rs.getInt("gm"));
                    ret.skinColor = SkinColor.getById(rs.getInt("skincolor"));
                    ret.gender = rs.getInt("gender");
                    ret.job = Job.getById(rs.getInt("job"));
                    ret.finishedDojoTutorial = rs.getInt("finishedDojoTutorial") == 1;
                    ret.vanquisherKills = rs.getInt("vanquisherKills");
                    ret.omokwins = rs.getInt("omokwins");
                    ret.omoklosses = rs.getInt("omoklosses");
                    ret.omokties = rs.getInt("omokties");
                    ret.matchcardwins = rs.getInt("matchcardwins");
                    ret.matchcardlosses = rs.getInt("matchcardlosses");
                    ret.matchcardties = rs.getInt("matchcardties");
                    ret.hair = rs.getInt("hair");
                    ret.face = rs.getInt("face");
                    ret.accountid = rs.getInt("accountid");
                    ret.mapid = rs.getInt("map");
                    ret.jailExpiration = rs.getLong("jailexpire");
                    ret.initialSpawnPoint = rs.getInt("spawnpoint");
                    ret.world = rs.getByte("world");
                    ret.rank = rs.getInt("rank");
                    ret.rankMove = rs.getInt("rankMove");
                    ret.jobRank = rs.getInt("jobRank");
                    ret.jobRankMove = rs.getInt("jobRankMove");
                    mountexp = rs.getInt("mountexp");
                    mountlevel = rs.getInt("mountlevel");
                    mounttiredness = rs.getInt("mounttiredness");
                    ret.guildid = rs.getInt("guildid");
                    ret.guildRank = rs.getInt("guildrank");
                    ret.allianceRank = rs.getInt("allianceRank");
                    ret.familyId = rs.getInt("familyId");
                    ret.bookCover = rs.getInt("monsterbookcover");
                    ret.monsterbook = monsterBook;
                    ret.vanquisherStage = rs.getInt("vanquisherStage");
                    ret.ariantPoints = rs.getInt("ariantPoints");
                    ret.dojoPoints = rs.getInt("dojoPoints");
                    ret.dojoStage = rs.getInt("lastDojoStage");
                    ret.dataString = rs.getString("dataString");
                    ret.mgc = new GuildCharacter(ret);
                    int buddyCapacity = rs.getInt("buddyCapacity");
                    ret.buddylist = new BuddyList(buddyCapacity);
                    ret.lastExpGainTime = rs.getTimestamp("lastExpGainTime").getTime();
                    ret.canRecvPartySearchInvite = rs.getBoolean("partySearch");

                    wserv = Server.getInstance().getWorld(ret.world);

                    ret.getInventory(InventoryType.EQUIP).setSlotLimit(rs.getByte("equipslots"));
                    ret.getInventory(InventoryType.USE).setSlotLimit(rs.getByte("useslots"));
                    ret.getInventory(InventoryType.SETUP).setSlotLimit(rs.getByte("setupslots"));
                    ret.getInventory(InventoryType.ETC).setSlotLimit(rs.getByte("etcslots"));

                    short sandboxCheck = 0x0;
                    for (Pair<Item, InventoryType> item : ItemFactory.INVENTORY.loadItems(ret.id, !channelServer)) {
                        sandboxCheck |= item.getLeft().getFlag();

                        ret.getInventory(item.getRight()).addItemFromDB(item.getLeft());
                        Item itemz = item.getLeft();
                        if (itemz.getPetId() > -1) {
                            Pet pet = itemz.getPet();
                            if (pet != null && pet.isSummoned()) {
                                ret.addPet(pet);
                            }
                            continue;
                        }

                        InventoryType mit = item.getRight();
                        if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                            Equip equip = (Equip) item.getLeft();
                            if (equip.getRingId() > -1) {
                                Ring ring = Ring.loadFromDb(equip.getRingId());
                                if (item.getRight().equals(InventoryType.EQUIPPED)) {
                                    ring.equip();
                                }

                                ret.addPlayerRing(ring);
                            }
                        }
                    }

                    if ((sandboxCheck & ItemConstants.SANDBOX) == ItemConstants.SANDBOX) {
                        ret.setHasSandboxItem();
                    }

                    ret.partnerId = rs.getInt("partnerId");
                    ret.marriageItemid = rs.getInt("marriageItemId");
                    if (ret.marriageItemid > 0 && ret.partnerId <= 0) {
                        ret.marriageItemid = -1;
                    } else if (ret.partnerId > 0 && wserv.getRelationshipId(ret.id) <= 0) {
                        ret.marriageItemid = -1;
                        ret.partnerId = -1;
                    }

                    NewYearCardRecord.loadPlayerNewYearCards(ret);

                    //PreparedStatement ps2, ps3;
                    //ResultSet rs2, rs3;

                    // Items excluded from pet loot
                    try (PreparedStatement psPet = con.prepareStatement("SELECT petid FROM inventoryitems WHERE characterid = ? AND petid > -1")) {
                        psPet.setInt(1, chrId);

                        try (ResultSet rsPet = psPet.executeQuery()) {
                            while (rsPet.next()) {
                                final int petId = rsPet.getInt("petid");

                                try (PreparedStatement psItem = con.prepareStatement("SELECT itemid FROM petignores WHERE petid = ?")) {
                                    psItem.setInt(1, petId);

                                    ret.resetExcluded(petId);

                                    try (ResultSet rsItem = psItem.executeQuery()) {
                                        while (rsItem.next()) {
                                            ret.addExcluded(petId, rsItem.getInt("itemid"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ret.commitExcludedItems();


                    if (channelServer) {
                        MapManager mapManager = client.getChannelServer().getMapFactory();
                        ret.map = mapManager.getMap(ret.mapid);

                        if (ret.map == null) {
                            ret.map = mapManager.getMap(MapId.HENESYS);
                        }
                        Portal portal = ret.map.getPortal(ret.initialSpawnPoint);
                        if (portal == null) {
                            portal = ret.map.getPortal(0);
                            ret.initialSpawnPoint = 0;
                        }
                        ret.setPosition(portal.getPosition());
                        int partyid = rs.getInt("party");
                        Party party = wserv.getParty(partyid);
                        if (party != null) {
                            ret.mpc = party.getMemberById(ret.id);
                            if (ret.mpc != null) {
                                ret.mpc = new PartyCharacter(ret);
                                ret.party = party;
                            }
                        }
                        int messengerid = rs.getInt("messengerid");
                        int position = rs.getInt("messengerposition");
                        if (messengerid > 0 && position < 4 && position > -1) {
                            Messenger messenger = wserv.getMessenger(messengerid);
                            if (messenger != null) {
                                ret.messenger = messenger;
                                ret.messengerposition = position;
                            }
                        }
                        ret.loggedIn = true;
                    }
                }
            }

            // Teleport rocks
            try (PreparedStatement ps = con.prepareStatement("SELECT mapid,vip FROM trocklocations WHERE characterid = ? LIMIT 15")) {
                ps.setInt(1, chrId);

                try (ResultSet rs = ps.executeQuery()) {
                    byte vip = 0;
                    byte reg = 0;
                    while (rs.next()) {
                        if (rs.getInt("vip") == 1) {
                            ret.viptrockmaps.add(rs.getInt("mapid"));
                            vip++;
                        } else {
                            ret.trockmaps.add(rs.getInt("mapid"));
                            reg++;
                        }
                    }
                    while (vip < 10) {
                        ret.viptrockmaps.add(MapId.NONE);
                        vip++;
                    }
                    while (reg < 5) {
                        ret.trockmaps.add(MapId.NONE);
                        reg++;
                    }
                }
            }

            // Account info
            try (PreparedStatement ps = con.prepareStatement("SELECT name, characterslots, language FROM accounts WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, ret.accountid);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Client retClient = ret.getClient();

                        retClient.setAccountName(rs.getString("name"));
                        retClient.setCharacterSlots(rs.getByte("characterslots"));
                    }
                }
            }

            // Area info
            try (PreparedStatement ps = con.prepareStatement("SELECT `area`,`info` FROM area_info WHERE charid = ?")) {
                ps.setInt(1, ret.id);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ret.area_info.put(rs.getShort("area"), rs.getString("info"));
                    }
                }
            }

            // Event stats
            try (PreparedStatement ps = con.prepareStatement("SELECT `name`,`info` FROM eventstats WHERE characterid = ?")) {
                ps.setInt(1, ret.id);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("name");
                        if (rs.getString("name").contentEquals("rescueGaga")) {
                            ret.events.put(name, new RescueGaga(rs.getInt("info")));
                        }
                    }
                }
            }

            ret.cashshop = new CashShop(ret.accountid, ret.id, ret.getJobType());
            ret.autoban = new AutobanManager(ret);

            // Blessing of the Fairy
            try (PreparedStatement ps = con.prepareStatement("SELECT name, level FROM characters WHERE accountid = ? AND id != ? ORDER BY level DESC limit 1")) {
                ps.setInt(1, ret.accountid);
                ps.setInt(2, chrId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ret.linkedName = rs.getString("name");
                        ret.linkedLevel = rs.getInt("level");
                    }
                }
            }

            if (channelServer) {
                final Map<Integer, QuestStatus> loadedQuestStatus = new LinkedHashMap<>();

                // Quest status
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?")) {
                    ps.setInt(1, chrId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Quest q = Quest.getInstance(rs.getShort("quest"));
                            QuestStatus status = new QuestStatus(q, QuestStatus.Status.getById(rs.getInt("status")));
                            long cTime = rs.getLong("time");
                            if (cTime > -1) {
                                status.setCompletionTime(SECONDS.toMillis(cTime));
                            }

                            long eTime = rs.getLong("expires");
                            if (eTime > 0) {
                                status.setExpirationTime(eTime);
                            }

                            status.setForfeited(rs.getInt("forfeited"));
                            status.setCompleted(rs.getInt("completed"));
                            ret.quests.put(q.getId(), status);
                            loadedQuestStatus.put(rs.getInt("queststatusid"), status);
                        }
                    }
                }

                // Quest progress
                // opportunity for improvement on questprogress/medalmaps calls to DB
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM questprogress WHERE characterid = ?")) {
                    ps.setInt(1, chrId);
                    try (ResultSet rsProgress = ps.executeQuery()) {
                        while (rsProgress.next()) {
                            QuestStatus status = loadedQuestStatus.get(rsProgress.getInt("queststatusid"));
                            if (status != null) {
                                status.setProgress(rsProgress.getInt("progressid"), rsProgress.getString("progress"));
                            }
                        }
                    }
                }

                // Medal map visit progress
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM medalmaps WHERE characterid = ?")) {
                    ps.setInt(1, chrId);
                    try (ResultSet rsMedalMaps = ps.executeQuery()) {
                        while (rsMedalMaps.next()) {
                            QuestStatus status = loadedQuestStatus.get(rsMedalMaps.getInt("queststatusid"));
                            if (status != null) {
                                status.addMedalMap(rsMedalMaps.getInt("mapid"));
                            }
                        }
                    }
                }

                loadedQuestStatus.clear();

                // Skills
                try (PreparedStatement ps = con.prepareStatement("SELECT skillid,skilllevel,masterlevel,expiration FROM skills WHERE characterid = ?")) {
                    ps.setInt(1, chrId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Skill pSkill = SkillFactory.getSkill(rs.getInt("skillid"));
                            if (pSkill != null) { // edit reported by Shavit (=＾● ⋏ ●＾=), thanks Zein for noticing an NPE here
                                ret.skills.put(pSkill, new SkillEntry(rs.getByte("skilllevel"), rs.getInt("masterlevel"), rs.getLong("expiration")));
                            }
                        }
                    }
                }

                // Cooldowns (load)
                try (PreparedStatement ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM cooldowns WHERE charid = ?")) {
                    ps.setInt(1, ret.getId());

                    try (ResultSet rs = ps.executeQuery()) {
                        long curTime = Server.getInstance().getCurrentTime();
                        while (rs.next()) {
                            final int skillid = rs.getInt("SkillID");
                            final long length = rs.getLong("length"), startTime = rs.getLong("StartTime");
                            if (skillid != 5221999 && (length + startTime < curTime)) {
                                continue;
                            }
                            ret.giveCoolDowns(skillid, startTime, length);
                        }
                    }
                }

                // Cooldowns (delete)
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM cooldowns WHERE charid = ?")) {
                    ps.setInt(1, ret.getId());
                    ps.executeUpdate();
                }

                // Debuffs (load)
                Map<Disease, Pair<Long, MobSkill>> loadedDiseases = new LinkedHashMap<>();
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM playerdiseases WHERE charid = ?")) {
                    ps.setInt(1, ret.getId());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            final Disease disease = Disease.ordinal(rs.getInt("disease"));
                            if (disease == Disease.NULL) {
                                continue;
                            }

                            final int skillid = rs.getInt("mobskillid");
                            final int skilllv = rs.getInt("mobskilllv");
                            final long length = rs.getInt("length");

                            MobSkillType type = MobSkillType.from(skillid).orElseThrow();
                            MobSkill ms = MobSkillFactory.getMobSkillOrThrow(type, skilllv);
                            loadedDiseases.put(disease, new Pair<>(length, ms));
                        }
                    }
                }

                // Debuffs (delete)
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM playerdiseases WHERE charid = ?")) {
                    ps.setInt(1, ret.getId());
                    ps.executeUpdate();
                }

                if (!loadedDiseases.isEmpty()) {
                    Server.getInstance().getPlayerBuffStorage().addDiseasesToStorage(ret.id, loadedDiseases);
                }

                // Skill macros
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?")) {
                    ps.setInt(1, chrId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int position = rs.getInt("position");
                            SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                            ret.skillMacros[position] = macro;
                        }
                    }
                }

                // Key config
                try (PreparedStatement ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?")) {
                    ps.setInt(1, chrId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int key = rs.getInt("key");
                            int type = rs.getInt("type");
                            int action = rs.getInt("action");
                            ret.keymap.put(key, new KeyBinding(type, action));
                        }
                    }
                }

                // Saved locations
                try (PreparedStatement ps = con.prepareStatement("SELECT `locationtype`,`map`,`portal` FROM savedlocations WHERE characterid = ?")) {
                    ps.setInt(1, chrId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ret.savedLocations[SavedLocationType.valueOf(rs.getString("locationtype")).ordinal()] = new SavedLocation(rs.getInt("map"), rs.getInt("portal"));
                        }
                    }
                }

                // Fame history
                try (PreparedStatement ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30")) {
                    ps.setInt(1, chrId);

                    try (ResultSet rs = ps.executeQuery()) {
                        ret.lastfametime = 0;
                        ret.lastmonthfameids = new ArrayList<>(31);
                        while (rs.next()) {
                            ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                            ret.lastmonthfameids.add(rs.getInt("characterid_to"));
                        }
                    }
                }
                
                ret.buddylist.loadFromDb(chrId);
                ret.storage = wserv.getAccountStorage(ret.accountid);

                /* Double-check storage incase player is first time on server
                 * The storage won't exist so nothing to load
                 */
                if(ret.storage == null) {
                    wserv.loadAccountStorage(ret.accountid);
                    ret.storage = wserv.getAccountStorage(ret.accountid);
                }
                
                int startHp = ret.hp, startMp = ret.mp;
                ret.reapplyLocalStats();
                ret.changeHpMp(startHp, startMp, true);
                //ret.resetBattleshipHp();
            }

            final int mountid = ret.getJobType() * 10000000 + 1004;
            if (ret.getInventory(InventoryType.EQUIPPED).getItem((short) -18) != null) {
                ret.maplemount = new Mount(ret, ret.getInventory(InventoryType.EQUIPPED).getItem((short) -18).getItemId(), mountid);
            } else {
                ret.maplemount = new Mount(ret, 0, mountid);
            }
            ret.maplemount.setExp(mountexp);
            ret.maplemount.setLevel(mountlevel);
            ret.maplemount.setTiredness(mounttiredness);
            ret.maplemount.setActive(false);

            // Quickslot key config
            try (final PreparedStatement pSelectQuickslotKeyMapped = con.prepareStatement("SELECT keymap FROM quickslotkeymapped WHERE accountid = ?;")) {
                pSelectQuickslotKeyMapped.setInt(1, ret.getAccountID());

                try (final ResultSet pResultSet = pSelectQuickslotKeyMapped.executeQuery()) {
                    if (pResultSet.next()) {
                        ret.m_aQuickslotLoaded = LongTool.LongToBytes(pResultSet.getLong(1));
                        ret.m_pQuickslotKeyMapped = new QuickslotBinding(ret.m_aQuickslotLoaded);
                    }
                }
            }

            return ret;
        } catch (SQLException | RuntimeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void reloadQuestExpirations() {
        for (QuestStatus mqs : getStartedQuests()) {
            if (mqs.getExpirationTime() > 0) {
                questTimeLimit2(mqs.getQuest(), mqs.getExpirationTime());
            }
        }
    }

    public static String makeMapleReadable(String in) {
        String i = in.replace('I', 'i');
        i = i.replace('l', 'L');
        i = i.replace("rn", "Rn");
        i = i.replace("vv", "Vv");
        i = i.replace("VV", "Vv");

        return i;
    }

    private static class BuffStatValueHolder {

        public StatEffect effect;
        public long startTime;
        public int value;
        public boolean bestApplied;

        public BuffStatValueHolder(StatEffect effect, long startTime, int value) {
            super();
            this.effect = effect;
            this.startTime = startTime;
            this.value = value;
            this.bestApplied = false;
        }
    }

    public static class CooldownValueHolder {

        public int skillId;
        public long startTime, length;

        public CooldownValueHolder(int skillId, long startTime, long length) {
            super();
            this.skillId = skillId;
            this.startTime = startTime;
            this.length = length;
        }
    }

    public void message(String m) {
        dropMessage(5, m);
    }

    public void yellowMessage(String m) {
        sendPacket(PacketCreator.sendYellowTip(m));
    }

    public void raiseQuestMobCount(int id) {
        // It seems nexon uses monsters that don't exist in the WZ (except string) to merge multiple mobs together for these 3 monsters.
        // We also want to run mobKilled for both since there are some quest that don't use the updated ID...
        if (id == MobId.GREEN_MUSHROOM || id == MobId.DEJECTED_GREEN_MUSHROOM) {
            raiseQuestMobCount(MobId.GREEN_MUSHROOM_QUEST);
        } else if (id == MobId.ZOMBIE_MUSHROOM || id == MobId.ANNOYED_ZOMBIE_MUSHROOM) {
            raiseQuestMobCount(MobId.ZOMBIE_MUSHROOM_QUEST);
        } else if (id == MobId.GHOST_STUMP || id == MobId.SMIRKING_GHOST_STUMP) {
            raiseQuestMobCount(MobId.GHOST_STUMP_QUEST);
        }

        int lastQuestProcessed = 0;
        try {
            synchronized (quests) {
                for (QuestStatus qs : getQuests()) {
                    lastQuestProcessed = qs.getQuest().getId();
                    if (qs.getStatus() == QuestStatus.Status.COMPLETED || qs.getQuest().canComplete(this, null)) {
                        continue;
                    }

                    if (qs.progress(id)) {
                        announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, false);
                        if (qs.getInfoNumber() > 0) {
                            announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Character.mobKilled. chrId {}, last quest processed: {}", this.id, lastQuestProcessed, e);
        }
    }

    public Mount mount(int id, int skillid) {
        Mount mount = maplemount;
        mount.setItemId(id);
        mount.setSkillId(skillid);
        return mount;
    }

    private void playerDead() {
        if (this.getMap().isCPQMap()) {
            int losing = getMap().getDeathCP();
            if (getCP() < losing) {
                losing = getCP();
            }
            getMap().broadcastMessage(PacketCreator.playerDiedMessage(getName(), losing, getTeam()));
            gainCP(-losing);
            return;
        }

        cancelAllBuffs(false);
        dispelDebuffs();
        lastDeathtime = Server.getInstance().getCurrentTime();

        EventInstanceManager eim = getEventInstance();
        if (eim != null) {
            eim.playerKilled(this);
        }
        int[] charmID = {ItemId.SAFETY_CHARM, ItemId.EASTER_BASKET, ItemId.EASTER_CHARM};
        int possesed = 0;
        int i;
        for (i = 0; i < charmID.length; i++) {
            int quantity = getItemQuantity(charmID[i], false);
            if (possesed == 0 && quantity > 0) {
                possesed = quantity;
                break;
            }
        }
        if (possesed > 0 && !MapId.isDojo(getMapId())) {
            message("You have used a safety charm, so your EXP points have not been decreased.");
            InventoryManipulator.removeById(client, ItemConstants.getInventoryType(charmID[i]), charmID[i], 1, true, false);
            usedSafetyCharm = true;
        } else if (getJob() != Job.BEGINNER) { //Hmm...
            if (!FieldLimit.NO_EXP_DECREASE.check(getMap().getFieldLimit())) {  // thanks Conrad for noticing missing FieldLimit check
                int XPdummy = ExpTable.getExpNeededForLevel(getLevel());

                if (getMap().isTown()) {    // thanks MindLove, SIayerMonkey, HaItsNotOver for noting players only lose 1% on town maps
                    XPdummy /= 100;
                } else {
                    if (getLuk() < 50) {    // thanks Taiketo, Quit, Fishanelli for noting player EXP loss are fixed, 50-LUK threshold
                        XPdummy /= 10;
                    } else {
                        XPdummy /= 20;
                    }
                }

                int curExp = getExp();
                if (curExp > XPdummy) {
                    loseExp(XPdummy, false, false);
                } else {
                    loseExp(curExp, false, false);
                }
            }
        }

        if (getBuffedValue(BuffStat.MORPH) != null) {
            cancelEffectFromBuffStat(BuffStat.MORPH);
        }

        if (getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
            cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
        }

        unsitChairInternal();
        sendPacket(PacketCreator.enableActions());
    }

    private void unsitChairInternal() {
        int chairid = chair.get();
        if (chairid >= 0) {
            if (ItemConstants.isFishingChair(chairid)) {
                this.getWorldServer().unregisterFisherPlayer(this);
            }

            setChair(-1);
            if (unregisterChairBuff()) {
                getMap().broadcastMessage(this, PacketCreator.cancelForeignChairSkillEffect(this.getId()), false);
            }

            getMap().broadcastMessage(this, PacketCreator.showChair(this.getId(), 0), false);
        }

        sendPacket(PacketCreator.cancelChair(-1));
    }

    public void sitChair(int itemId) {
        if (this.isLoggedinWorld()) {
            if (itemId >= 1000000) {    // sit on item chair
                if (chair.get() < 0) {
                    setChair(itemId);
                    getMap().broadcastMessage(this, PacketCreator.showChair(this.getId(), itemId), false);
                }
                sendPacket(PacketCreator.enableActions());
            } else if (itemId >= 0) {    // sit on map chair
                if (chair.get() < 0) {
                    setChair(itemId);
                    if (registerChairBuff()) {
                        getMap().broadcastMessage(this, PacketCreator.giveForeignChairSkillEffect(this.getId()), false);
                    }
                    sendPacket(PacketCreator.cancelChair(itemId));
                }
            } else {    // stand up
                unsitChairInternal();
            }
        }
    }

    private void setChair(int chair) {
        this.chair.set(chair);
    }

    public void respawn(int returnMap) {
        respawn(null, returnMap);    // unspecified EIM, don't force EIM unregister in this case
    }

    public void respawn(EventInstanceManager eim, int returnMap) {
        if (eim != null) {
            eim.unregisterPlayer(this);    // some event scripts uses this...
        }
        changeMap(returnMap);

        cancelAllBuffs(false);  // thanks Oblivium91 for finding out players still could revive in area and take damage before returning to town

        if (usedSafetyCharm) {  // thanks kvmba for noticing safety charm not providing 30% HP/MP
            addMPHP((int) Math.ceil(this.getClientMaxHp() * 0.3), (int) Math.ceil(this.getClientMaxMp() * 0.3));
        } else {
            updateHp(50);
        }

        setStance(0);
    }

    private void prepareDragonBlood(final StatEffect bloodEffect) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        dragonBloodSchedule = TimerManager.getInstance().register(new Runnable() {
            @Override
            public void run() {
                if (awayFromWorld.get()) {
                    return;
                }

                addHP(-bloodEffect.getX());
                sendPacket(PacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
                getMap().broadcastMessage(Character.this, PacketCreator.showBuffEffect(getId(), bloodEffect.getSourceId(), 5), false);
            }
        }, 4000, 4000);
    }

    private void recalcEquipStats() {
        if (equipchanged) {
            equipmaxhp = 0;
            equipmaxmp = 0;
            equipdex = 0;
            equipint_ = 0;
            equipstr = 0;
            equipluk = 0;
            equipmagic = 0;
            equipwatk = 0;
            //equipspeed = 0;
            //equipjump = 0;

            for (Item item : getInventory(InventoryType.EQUIPPED)) {
                Equip equip = (Equip) item;
                equipmaxhp += equip.getHp();
                equipmaxmp += equip.getMp();
                equipdex += equip.getDex();
                equipint_ += equip.getInt();
                equipstr += equip.getStr();
                equipluk += equip.getLuk();
                equipmagic += equip.getMatk() + equip.getInt();
                equipwatk += equip.getWatk();
                //equipspeed += equip.getSpeed();
                //equipjump += equip.getJump();
            }

            equipchanged = false;
        }

        localmaxhp += equipmaxhp;
        localmaxmp += equipmaxmp;
        localdex += equipdex;
        localint_ += equipint_;
        localstr += equipstr;
        localluk += equipluk;
        localmagic += equipmagic;
        localwatk += equipwatk;
    }

    private void reapplyLocalStats() {
        effLock.lock();
        chrLock.lock();
        statWlock.lock();
        try {
            localmaxhp = getMaxHp();
            localmaxmp = getMaxMp();
            localdex = getDex();
            localint_ = getInt();
            localstr = getStr();
            localluk = getLuk();
            localmagic = localint_;
            localwatk = 0;
            localchairrate = -1;

            recalcEquipStats();

            localmagic = Math.min(localmagic, 2000);

            Integer hbhp = getBuffedValue(BuffStat.HYPERBODYHP);
            if (hbhp != null) {
                localmaxhp += (hbhp.doubleValue() / 100) * localmaxhp;
            }
            Integer hbmp = getBuffedValue(BuffStat.HYPERBODYMP);
            if (hbmp != null) {
                localmaxmp += (hbmp.doubleValue() / 100) * localmaxmp;
            }

            localmaxhp = Math.min(30000, localmaxhp);
            localmaxmp = Math.min(30000, localmaxmp);

            StatEffect combo = getBuffEffect(BuffStat.ARAN_COMBO);
            if (combo != null) {
                localwatk += combo.getX();
            }

            if (energybar == 15000) {
                Skill energycharge = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.ENERGY_CHARGE) : SkillFactory.getSkill(Marauder.ENERGY_CHARGE);
                StatEffect ceffect = energycharge.getEffect(getSkillLevel(energycharge));
                localwatk += ceffect.getWatk();
            }

            Integer mwarr = getBuffedValue(BuffStat.MAPLE_WARRIOR);
            if (mwarr != null) {
                localstr += getStr() * mwarr / 100;
                localdex += getDex() * mwarr / 100;
                localint_ += getInt() * mwarr / 100;
                localluk += getLuk() * mwarr / 100;
            }
            if (job.isA(Job.BOWMAN)) {
                Skill expert = null;
                if (job.isA(Job.MARKSMAN)) {
                    expert = SkillFactory.getSkill(3220004);
                } else if (job.isA(Job.BOWMASTER)) {
                    expert = SkillFactory.getSkill(3120005);
                }
                if (expert != null) {
                    int boostLevel = getSkillLevel(expert);
                    if (boostLevel > 0) {
                        localwatk += expert.getEffect(boostLevel).getX();
                    }
                }
            }

            Integer watkbuff = getBuffedValue(BuffStat.WATK);
            if (watkbuff != null) {
                localwatk += watkbuff.intValue();
            }
            Integer matkbuff = getBuffedValue(BuffStat.MATK);
            if (matkbuff != null) {
                localmagic += matkbuff.intValue();
            }

            /*
            Integer speedbuff = getBuffedValue(BuffStat.SPEED);
            if (speedbuff != null) {
                localspeed += speedbuff.intValue();
            }
            Integer jumpbuff = getBuffedValue(BuffStat.JUMP);
            if (jumpbuff != null) {
                localjump += jumpbuff.intValue();
            }
            */

            Integer blessing = getSkillLevel(10000000 * getJobType() + 12);
            if (blessing > 0) {
                localwatk += blessing;
                localmagic += blessing * 2;
            }

            if (job.isA(Job.THIEF) || job.isA(Job.BOWMAN) || job.isA(Job.PIRATE) || job.isA(Job.NIGHTWALKER1) || job.isA(Job.WINDARCHER1)) {
                Item weapon_item = getInventory(InventoryType.EQUIPPED).getItem((short) -11);
                if (weapon_item != null) {
                    ItemInformationProvider ii = ItemInformationProvider.getInstance();
                    WeaponType weapon = ii.getWeaponType(weapon_item.getItemId());
                    boolean bow = weapon == WeaponType.BOW;
                    boolean crossbow = weapon == WeaponType.CROSSBOW;
                    boolean claw = weapon == WeaponType.CLAW;
                    boolean gun = weapon == WeaponType.GUN;
                    if (bow || crossbow || claw || gun) {
                        // Also calc stars into this.
                        Inventory inv = getInventory(InventoryType.USE);
                        for (short i = 1; i <= inv.getSlotLimit(); i++) {
                            Item item = inv.getItem(i);
                            if (item != null) {
                                if ((claw && ItemConstants.isThrowingStar(item.getItemId())) || (gun && ItemConstants.isBullet(item.getItemId())) || (bow && ItemConstants.isArrowForBow(item.getItemId())) || (crossbow && ItemConstants.isArrowForCrossBow(item.getItemId()))) {
                                    if (item.getQuantity() > 0) {
                                        // Finally there!
                                        localwatk += ii.getWatkForProjectile(item.getItemId());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                // Add throwing stars to dmg.
            }
        } finally {
            statWlock.unlock();
            chrLock.unlock();
            effLock.unlock();
        }
    }

    private List<Pair<Stat, Integer>> recalcLocalStats() {
        effLock.lock();
        chrLock.lock();
        statWlock.lock();
        try {
            List<Pair<Stat, Integer>> hpmpupdate = new ArrayList<>(2);
            int oldlocalmaxhp = localmaxhp;
            int oldlocalmaxmp = localmaxmp;

            reapplyLocalStats();

            if (YamlConfig.config.server.USE_FIXED_RATIO_HPMP_UPDATE) {
                if (localmaxhp != oldlocalmaxhp) {
                    Pair<Stat, Integer> hpUpdate;

                    if (transienthp == Float.NEGATIVE_INFINITY) {
                        hpUpdate = calcHpRatioUpdate(localmaxhp, oldlocalmaxhp);
                    } else {
                        hpUpdate = calcHpRatioTransient();
                    }

                    hpmpupdate.add(hpUpdate);
                }

                if (localmaxmp != oldlocalmaxmp) {
                    Pair<Stat, Integer> mpUpdate;

                    if (transientmp == Float.NEGATIVE_INFINITY) {
                        mpUpdate = calcMpRatioUpdate(localmaxmp, oldlocalmaxmp);
                    } else {
                        mpUpdate = calcMpRatioTransient();
                    }

                    hpmpupdate.add(mpUpdate);
                }
            }

            return hpmpupdate;
        } finally {
            statWlock.unlock();
            chrLock.unlock();
            effLock.unlock();
        }
    }

    private void updateLocalStats() {
        prtLock.lock();
        effLock.lock();
        statWlock.lock();
        try {
            int oldmaxhp = localmaxhp;
            List<Pair<Stat, Integer>> hpmpupdate = recalcLocalStats();
            enforceMaxHpMp();

            if (!hpmpupdate.isEmpty()) {
                sendPacket(PacketCreator.updatePlayerStats(hpmpupdate, true, this));
            }

            if (oldmaxhp != localmaxhp) {   // thanks Wh1SK3Y (Suwaidy) for pointing out a deadlock occuring related to party members HP
                updatePartyMemberHP();
            }
        } finally {
            statWlock.unlock();
            effLock.unlock();
            prtLock.unlock();
        }
    }

    public void receivePartyMemberHP() {
        prtLock.lock();
        try {
            if (party != null) {
                for (Character partychar : this.getPartyMembersOnSameMap()) {
                    sendPacket(PacketCreator.updatePartyMemberHP(partychar.getId(), partychar.getHp(), partychar.getCurrentMaxHp()));
                }
            }
        } finally {
            prtLock.unlock();
        }
    }

    public void removeAllCooldownsExcept(int id, boolean packet) {
        effLock.lock();
        chrLock.lock();
        try {
            ArrayList<CooldownValueHolder> list = new ArrayList<>(coolDowns.values());
            for (CooldownValueHolder mcvh : list) {
                if (mcvh.skillId != id) {
                    coolDowns.remove(mcvh.skillId);
                    if (packet) {
                        sendPacket(PacketCreator.skillCooldown(mcvh.skillId, 0));
                    }
                }
            }
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public static void removeAriantRoom(int room) {
        ariantroomleader[room] = "";
        ariantroomslot[room] = 0;
    }

    public void removeCooldown(int skillId) {
        effLock.lock();
        chrLock.lock();
        try {
            this.coolDowns.remove(skillId);
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void removePet(Pet pet, boolean shift_left) {
        petLock.lock();
        try {
            int slot = -1;
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    if (pets[i].getUniqueId() == pet.getUniqueId()) {
                        pets[i] = null;
                        slot = i;
                        break;
                    }
                }
            }
            if (shift_left) {
                if (slot > -1) {
                    for (int i = slot; i < 3; i++) {
                        if (i != 2) {
                            pets[i] = pets[i + 1];
                        } else {
                            pets[i] = null;
                        }
                    }
                }
            }
        } finally {
            petLock.unlock();
        }
    }

    public void removeVisibleMapObject(MapObject mo) {
        visibleMapObjects.remove(mo);
    }

    public synchronized void resetStats() {
        if (!YamlConfig.config.server.USE_AUTOASSIGN_STARTERS_AP) {
            return;
        }

        effLock.lock();
        statWlock.lock();
        try {
            int tap = remainingAp + str + dex + int_ + luk, tsp = 1;
            int tstr = 4, tdex = 4, tint = 4, tluk = 4;

            switch (job.getId()) {
                case 100:
                case 1100:
                case 2100:
                    tstr = 35;
                    tsp += ((getLevel() - 10) * 3);
                    break;
                case 200:
                case 1200:
                    tint = 20;
                    tsp += ((getLevel() - 8) * 3);
                    break;
                case 300:
                case 1300:
                case 400:
                case 1400:
                    tdex = 25;
                    tsp += ((getLevel() - 10) * 3);
                    break;
                case 500:
                case 1500:
                    tdex = 20;
                    tsp += ((getLevel() - 10) * 3);
                    break;
            }

            tap -= tstr;
            tap -= tdex;
            tap -= tint;
            tap -= tluk;

            if (tap >= 0) {
                updateStrDexIntLukSp(tstr, tdex, tint, tluk, tap, tsp, GameConstants.getSkillBook(job.getId()));
            } else {
                log.warn("Chr {} tried to have its stats reset without enough AP available");
            }
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }
    }

    public void resetBattleshipHp() {
        int bshipLevel = Math.max(getLevel() - 120, 0);  // thanks alex12 for noticing battleship HP issues for low-level players
        this.battleshipHp = 400 * getSkillLevel(SkillFactory.getSkill(Corsair.BATTLE_SHIP)) + (bshipLevel * 200);
    }

    public void resetEnteredScript() {
        entered.remove(map.getId());
    }

    public void resetEnteredScript(int mapId) {
        entered.remove(mapId);
    }

    public void resetEnteredScript(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                entered.remove(mapId);
            }
        }
    }

    public void saveGuildStatus() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ?, allianceRank = ? WHERE id = ?")) {
            ps.setInt(1, guildid);
            ps.setInt(2, guildRank);
            ps.setInt(3, allianceRank);
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public void saveLocationOnWarp() {  // suggestion to remember the map before warp command thanks to Lei
        Portal closest = map.findClosestPortal(getPosition());
        int curMapid = getMapId();

        for (int i = 0; i < savedLocations.length; i++) {
            if (savedLocations[i] == null) {
                savedLocations[i] = new SavedLocation(curMapid, closest != null ? closest.getId() : 0);
            }
        }
    }

    public void saveLocation(String type) {
        Portal closest = map.findClosestPortal(getPosition());
        savedLocations[SavedLocationType.fromString(type).ordinal()] = new SavedLocation(getMapId(), closest != null ? closest.getId() : 0);
    }

    public final boolean insertNewChar(CharacterFactoryRecipe recipe) {
        str = recipe.getStr();
        dex = recipe.getDex();
        int_ = recipe.getInt();
        luk = recipe.getLuk();
        setMaxHp(recipe.getMaxHp());
        setMaxMp(recipe.getMaxMp());
        hp = maxhp;
        mp = maxmp;
        level = recipe.getLevel();
        remainingAp = recipe.getRemainingAp();
        remainingSp[GameConstants.getSkillBook(job.getId())] = recipe.getRemainingSp();
        mapid = recipe.getMap();
        meso.set(recipe.getMeso());

        List<Pair<Skill, Integer>> startingSkills = recipe.getStartingSkillLevel();
        for (Pair<Skill, Integer> skEntry : startingSkills) {
            Skill skill = skEntry.getLeft();
            this.changeSkillLevel(skill, skEntry.getRight().byteValue(), skill.getMaxLevel(), -1);
        }

        List<Pair<Item, InventoryType>> itemsWithType = recipe.getStartingItems();
        for (Pair<Item, InventoryType> itEntry : itemsWithType) {
            this.getInventory(itEntry.getRight()).addItem(itEntry.getLeft());
        }

        this.events.put("rescueGaga", new RescueGaga(0));


        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            try {
                // Character info
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO characters (str, dex, luk, `int`, gm, skincolor, gender, job, hair, face, map, meso, spawnpoint, accountid, name, world, hp, mp, maxhp, maxmp, level, ap, sp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, str);
                    ps.setInt(2, dex);
                    ps.setInt(3, luk);
                    ps.setInt(4, int_);
                    ps.setInt(5, gmLevel);
                    ps.setInt(6, skinColor.getId());
                    ps.setInt(7, gender);
                    ps.setInt(8, getJob().getId());
                    ps.setInt(9, hair);
                    ps.setInt(10, face);
                    ps.setInt(11, mapid);
                    ps.setInt(12, Math.abs(meso.get()));
                    ps.setInt(13, 0);
                    ps.setInt(14, accountid);
                    ps.setString(15, name);
                    ps.setInt(16, world);
                    ps.setInt(17, hp);
                    ps.setInt(18, mp);
                    ps.setInt(19, maxhp);
                    ps.setInt(20, maxmp);
                    ps.setInt(21, level);
                    ps.setInt(22, remainingAp);

                    StringBuilder sps = new StringBuilder();
                    for (int j : remainingSp) {
                        sps.append(j);
                        sps.append(",");
                    }
                    String sp = sps.toString();
                    ps.setString(23, sp.substring(0, sp.length() - 1));

                    int updateRows = ps.executeUpdate();
                    if (updateRows < 1) {
                        log.error("Error trying to insert chr {}", name);
                        return false;
                    }

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            this.id = rs.getInt(1);
                        } else {
                            log.error("Inserting chr {} failed", name);
                            return false;
                        }
                    }
                }

                // Select a keybinding method
                int[] selectedKey;
                int[] selectedType;
                int[] selectedAction;

                if (YamlConfig.config.server.USE_CUSTOM_KEYSET) {
                    selectedKey = GameConstants.getCustomKey(true);
                    selectedType = GameConstants.getCustomType(true);
                    selectedAction = GameConstants.getCustomAction(true);
                } else {
                    selectedKey = GameConstants.getCustomKey(false);
                    selectedType = GameConstants.getCustomType(false);
                    selectedAction = GameConstants.getCustomAction(false);
                }

                // Key config
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, id);
                    for (int i = 0; i < selectedKey.length; i++) {
                        ps.setInt(2, selectedKey[i]);
                        ps.setInt(3, selectedType[i]);
                        ps.setInt(4, selectedAction[i]);
                        ps.executeUpdate();
                    }
                }

                // No quickslots, or no change.
                boolean bQuickslotEquals = this.m_pQuickslotKeyMapped == null || (this.m_aQuickslotLoaded != null && Arrays.equals(this.m_pQuickslotKeyMapped.GetKeybindings(), this.m_aQuickslotLoaded));
                if (!bQuickslotEquals) {
                    long nQuickslotKeymapped = LongTool.BytesToLong(this.m_pQuickslotKeyMapped.GetKeybindings());

                    // Quickslot key config
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO quickslotkeymapped (accountid, keymap) VALUES (?, ?) ON DUPLICATE KEY UPDATE keymap = ?;")) {
                        ps.setInt(1, this.getAccountID());
                        ps.setLong(2, nQuickslotKeymapped);
                        ps.setLong(3, nQuickslotKeymapped);
                        ps.executeUpdate();
                    }
                }

                itemsWithType = new ArrayList<>();
                for (Inventory iv : inventory) {
                    for (Item item : iv.list()) {
                        itemsWithType.add(new Pair<>(item, iv.getType()));
                    }
                }

                ItemFactory.INVENTORY.saveItems(itemsWithType, id, con);

                if (!skills.isEmpty()) {
                    // Skills
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)")) {
                        ps.setInt(1, id);
                        for (Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                            ps.setInt(2, skill.getKey().getId());
                            ps.setInt(3, skill.getValue().skillevel);
                            ps.setInt(4, skill.getValue().masterlevel);
                            ps.setLong(5, skill.getValue().expiration);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                
                con.commit();
                return true;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                con.setAutoCommit(true);
            }
        } catch (Throwable t) {
            log.error("Error creating chr {}, level: {}, job: {}", name, level, job.getId(), t);
        }

        return false;
    }

    public CharacterStats getCharacterStats() {
        CharacterStats.CharacterStatsBuilder builder = CharacterStats.builder()
                .id(id)
                .level(level)
                .fame(fame)
                .gmLevel(gmLevel)
                .skin(skinColor.getId())
                .gender(gender)
                .job(job.getId())
                .hair(hair)
                .face(face)
                .meso(meso.get())
                .hpMpApUsed(hpMpApUsed)
                .mapId(getSaveMap())
                .spawnPortal(getSaveSpawnPortal())
                .buddyCapacity(buddylist.getCapacity())
                .monsterBookCover(bookCover)
                .dojoVanquisherStage(vanquisherStage)
                .dojoPoints(dojoPoints)
                .dojoStage(dojoStage)
                .dojoTutorialComplete(finishedDojoTutorial)
                .dojoVanquisherKills(vanquisherKills)
                .matchCardWins(matchcardwins)
                .matchCardLosses(matchcardlosses)
                .matchCardTies(matchcardties)
                .omokWins(omokwins)
                .omokLosses(omoklosses)
                .omokTies(omokties)
                .dataString(dataString)
                .jailExpiration(jailExpiration)
                .lastExpGainTime(lastExpGainTime)
                .ariantPoints(ariantPoints)
                .canRecvPartySearchInvite(canRecvPartySearchInvite)
                .equipSlots(getSlots(InventoryType.EQUIP.getType()))
                .useSlots(getSlots(InventoryType.USE.getType()))
                .setupSlots(getSlots(InventoryType.SETUP.getType()))
                .etcSlots(getSlots(InventoryType.ETC.getType()));

        Party party = getParty();
        if (party != null) {
            builder.party(party.getId());
        }

        if (partnerId > 0) {
            builder.partnerId(partnerId);
        }

        if (marriageItemid > 0) {
            builder.marriageItemId(marriageItemid);
        }

        effLock.lock();
        statWlock.lock();
        try {
            builder.str(str)
                    .dex(dex)
                    .int_(int_)
                    .luk(luk)
                    .exp(Math.abs(exp.get()))
                    .gachaExp(Math.abs(gachaexp.get()))
                    .hp(hp)
                    .mp(mp)
                    .maxHp(maxhp)
                    .maxMp(maxmp)
                    .ap(remainingAp)
                    .sp(remainingSp[0]);
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }


        if (messenger != null) {
            builder.messenger(messenger.getId())
                    .messengerPosition(messengerposition);
        }

        if (maplemount != null) {
            builder.mountLevel(maplemount.getLevel())
                    .mountExp(maplemount.getExp())
                    .mountTiredness(maplemount.getTiredness());
        }

        return builder.build();
    }

    private int getSaveMap() {
        if (map == null || (cashshop != null && cashshop.isOpened())) {
            return mapid;
        }

        if (map.getForcedReturnId() != MapId.NONE) {
            return map.getForcedReturnId();
        }

        if (getHp() < 1) {
            return map.getReturnMapId();
        }

        return map.getId();
    }

    private int getSaveSpawnPortal() {
        if (map == null || map.getId() == MapId.CRIMSONWOOD_VALLEY_1 || map.getId() == MapId.CRIMSONWOOD_VALLEY_2) { // TODO: clean up. Shouldn't hardcode these maps.
            return 0;
        }

        Portal closest = map.findClosestPlayerSpawnpoint(getPosition());
        if (closest == null) {
            return 0;
        }

        return closest.getId();
    }

    public void sendPolice(String text) {
        final String message = getName() + " received this - " + text;
        log.info(message);
        if (Server.getInstance().isGmOnline(this.getWorld())) { //Alert and log if a GM is online
            Server.getInstance().broadcastGMMessage(this.getWorld(), PacketCreator.sendYellowTip(message));
        } else { //Auto DC and log if no GM is online
            throw new GameViolationException(text);
        }
    }

    public void sendKeymap() {
        sendPacket(PacketCreator.getKeymap(keymap));
    }

    public void sendMacros() {
        // Always send the macro packet to fix a client side bug when switching characters.
        sendPacket(PacketCreator.getMacros(skillMacros));
    }

    public SkillMacro[] getMacros() {
        return skillMacros;
    }

    public static void setAriantRoomLeader(int room, String charname) {
        ariantroomleader[room] = charname;
    }

    public static void setAriantSlotRoom(int room, int slot) {
        ariantroomslot[room] = slot;
    }

    public void setBattleshipHp(int battleshipHp) {
        this.battleshipHp = battleshipHp;
    }

    public void setBuddyCapacity(int capacity) {
        buddylist.setCapacity(capacity);
        sendPacket(PacketCreator.updateBuddyCapacity(capacity));
    }

    public void setBuffedValue(BuffStat effect, int value) {
        effLock.lock();
        chrLock.lock();
        try {
            BuffStatValueHolder mbsvh = effects.get(effect);
            if (mbsvh == null) {
                return;
            }
            mbsvh.value = value;
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
    }

    public void setDojoEnergy(int x) {
        this.dojoEnergy = Math.min(x, 10000);
    }

    public void setDojoPoints(int x) {
        this.dojoPoints = x;
    }

    public void setDojoStage(int x) {
        this.dojoStage = x;
    }

    public void setEnergyBar(int set) {
        energybar = set;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        evtLock.lock();
        try {
            this.eventInstance = eventInstance;
        } finally {
            evtLock.unlock();
        }
    }

    public void setExp(int amount) {
        this.exp.set(amount);
    }

    public void setGachaExp(int amount) {
        this.gachaexp.set(amount);
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setFamilyId(int familyId) {
        this.familyId = familyId;
    }

    public void setFinishedDojoTutorial() {
        this.finishedDojoTutorial = true;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public void setGM(int level) {
        this.gmLevel = level;
    }

    public void setGuildId(int _id) {
        guildid = _id;
    }

    public void setGuildRank(int _rank) {
        guildRank = _rank;
    }

    public void setAllianceRank(int _rank) {
        allianceRank = _rank;
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public void setHasMerchant(boolean set) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE characters SET HasMerchant = ? WHERE id = ?")) {
            ps.setInt(1, set ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        hasMerchant = set;
    }

    public void addMerchantMesos(int add) {
        final int newAmount = (int) Math.min((long) merchantmeso + add, Integer.MAX_VALUE);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE characters SET MerchantMesos = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, newAmount);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        merchantmeso = newAmount;
    }

    public void setMerchantMeso(int set) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE characters SET MerchantMesos = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, set);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        merchantmeso = set;
    }

    public synchronized void withdrawMerchantMesos() {
        int merchantMeso = this.getMerchantNetMeso();
        int playerMeso = this.getMeso();

        if (merchantMeso > 0) {
            int possible = Integer.MAX_VALUE - playerMeso;

            if (possible > 0) {
                if (possible < merchantMeso) {
                    this.gainMeso(possible, false);
                    this.setMerchantMeso(merchantMeso - possible);
                } else {
                    this.gainMeso(merchantMeso, false);
                    this.setMerchantMeso(0);
                }
            }
        } else {
            int nextMeso = playerMeso + merchantMeso;

            if (nextMeso < 0) {
                this.gainMeso(-playerMeso, false);
                this.setMerchantMeso(merchantMeso + playerMeso);
            } else {
                this.gainMeso(merchantMeso, false);
                this.setMerchantMeso(0);
            }
        }
    }

    public void setHiredMerchant(HiredMerchant merchant) {
        this.hiredMerchant = merchant;
    }

    private void hpChangeAction(int oldHp) {
        boolean playerDied = false;
        if (hp <= 0) {
            if (oldHp > hp) {
                playerDied = true;
            }
        }

        final boolean chrDied = playerDied;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                updatePartyMemberHP();    // thanks BHB (BHB88) for detecting a deadlock case within player stats.

                if (chrDied) {
                    playerDead();
                } else {
                    checkBerserk(isHidden());
                }
            }
        };
        if (map != null) {
            map.registerCharacterStatUpdate(r);
        }
    }

    private Pair<Stat, Integer> calcHpRatioUpdate(int newHp, int oldHp) {
        int delta = newHp - oldHp;
        this.hp = calcHpRatioUpdate(hp, oldHp, delta);

        hpChangeAction(Short.MIN_VALUE);
        return new Pair<>(Stat.HP, hp);
    }

    private Pair<Stat, Integer> calcMpRatioUpdate(int newMp, int oldMp) {
        int delta = newMp - oldMp;
        this.mp = calcMpRatioUpdate(mp, oldMp, delta);
        return new Pair<>(Stat.MP, mp);
    }

    private static int calcTransientRatio(float transientpoint) {
        int ret = (int) transientpoint;
        return !(ret <= 0 && transientpoint > 0.0f) ? ret : 1;
    }

    private Pair<Stat, Integer> calcHpRatioTransient() {
        this.hp = calcTransientRatio(transienthp * localmaxhp);

        hpChangeAction(Short.MIN_VALUE);
        return new Pair<>(Stat.HP, hp);
    }

    private Pair<Stat, Integer> calcMpRatioTransient() {
        this.mp = calcTransientRatio(transientmp * localmaxmp);
        return new Pair<>(Stat.MP, mp);
    }

    private int calcHpRatioUpdate(int curpoint, int maxpoint, int diffpoint) {
        int curMax = maxpoint;
        int nextMax = Math.min(30000, maxpoint + diffpoint);

        float temp = curpoint * nextMax;
        int ret = (int) Math.ceil(temp / curMax);

        transienthp = (maxpoint > nextMax) ? ((float) curpoint) / maxpoint : ((float) ret) / nextMax;
        return ret;
    }

    private int calcMpRatioUpdate(int curpoint, int maxpoint, int diffpoint) {
        int curMax = maxpoint;
        int nextMax = Math.min(30000, maxpoint + diffpoint);

        float temp = curpoint * nextMax;
        int ret = (int) Math.ceil(temp / curMax);

        transientmp = (maxpoint > nextMax) ? ((float) curpoint) / maxpoint : ((float) ret) / nextMax;
        return ret;
    }

    public boolean applyHpMpChange(int hpCon, int hpchange, int mpchange) {
        boolean zombify = hasDisease(Disease.ZOMBIFY);

        effLock.lock();
        statWlock.lock();
        try {
            int nextHp = hp + hpchange, nextMp = mp + mpchange;
            boolean cannotApplyHp = hpchange != 0 && nextHp <= 0 && (!zombify || hpCon > 0);
            boolean cannotApplyMp = mpchange != 0 && nextMp < 0;

            if (cannotApplyHp || cannotApplyMp) {
                if (!isGM()) {
                    return false;
                }

                if (cannotApplyHp) {
                    nextHp = 1;
                }
            }

            updateHpMp(nextHp, nextMp);
        } finally {
            statWlock.unlock();
            effLock.unlock();
        }

        // autopot on HPMP deplete... thanks shavit for finding out D. Roar doesn't trigger autopot request
        if (hpchange < 0) {
            KeyBinding autohpPot = this.getKeymap().get(91);
            if (autohpPot != null) {
                int autohpItemid = autohpPot.getAction();
                float autohpAlert = this.getAutopotHpAlert();
                if (((float) this.getHp()) / this.getCurrentMaxHp() <= autohpAlert) { // try within user settings... thanks Lame, Optimist, Stealth2800
                    Item autohpItem = this.getInventory(InventoryType.USE).findById(autohpItemid);
                    if (autohpItem != null) {
                        this.setAutopotHpAlert(0.9f * autohpAlert);
                        PetAutopotProcessor.runAutopotAction(client, autohpItem.getPosition(), autohpItemid);
                    }
                }
            }
        }

        if (mpchange < 0) {
            KeyBinding autompPot = this.getKeymap().get(92);
            if (autompPot != null) {
                int autompItemid = autompPot.getAction();
                float autompAlert = this.getAutopotMpAlert();
                if (((float) this.getMp()) / this.getCurrentMaxMp() <= autompAlert) {
                    Item autompItem = this.getInventory(InventoryType.USE).findById(autompItemid);
                    if (autompItem != null) {
                        this.setAutopotMpAlert(0.9f * autompAlert); // autoMP would stick to using pots at every depletion in some cases... thanks Rohenn
                        PetAutopotProcessor.runAutopotAction(client, autompItem.getPosition(), autompItemid);
                    }
                }
            }
        }

        return true;
    }

    public void setInventory(InventoryType type, Inventory inv) {
        inventory[type.ordinal()] = inv;
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public void setLastHealed(long time) {
        this.lastHealed = time;
    }

    public void setLastUsedCashItem(long time) {
        this.lastUsedCashItem = time;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public void setMessenger(Messenger messenger) {
        this.messenger = messenger;
    }

    public void setMessengerPosition(int position) {
        this.messengerposition = position;
    }

    public void setMiniGame(MiniGame miniGame) {
        this.miniGame = miniGame;
    }

    public void setMiniGamePoints(Character visitor, int winnerslot, boolean omok) {
        if (omok) {
            if (winnerslot == 1) {
                this.omokwins++;
                visitor.omoklosses++;
            } else if (winnerslot == 2) {
                visitor.omokwins++;
                this.omoklosses++;
            } else {
                this.omokties++;
                visitor.omokties++;
            }
        } else {
            if (winnerslot == 1) {
                this.matchcardwins++;
                visitor.matchcardlosses++;
            } else if (winnerslot == 2) {
                visitor.matchcardwins++;
                this.matchcardlosses++;
            } else {
                this.matchcardties++;
                visitor.matchcardties++;
            }
        }
    }

    public void setMonsterBookCover(int bookCover) {
        this.bookCover = bookCover;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRPS(RockPaperScissor rps) {
        this.rps = rps;
    }

    public void closeRPS() {
        RockPaperScissor rps = this.rps;
        if (rps != null) {
            rps.dispose(client);
            setRPS(null);
        }
    }

    public int getDoorSlot() {
        if (doorSlot != -1) {
            return doorSlot;
        }
        return fetchDoorSlot();
    }

    public int fetchDoorSlot() {
        prtLock.lock();
        try {
            doorSlot = (party == null) ? 0 : party.getPartyDoor(this.getId());
            return doorSlot;
        } finally {
            prtLock.unlock();
        }
    }

    public void setParty(Party p) {
        prtLock.lock();
        try {
            if (p == null) {
                this.mpc = null;
                doorSlot = -1;

                party = null;
            } else {
                party = p;
            }
        } finally {
            prtLock.unlock();
        }
    }

    public void setPlayerShop(PlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public void setSearch(String find) {
        search = find;
    }

    public void setSkinColor(SkinColor skinColor) {
        this.skinColor = skinColor;
    }

    public byte getSlots(int type) {
        return type == InventoryType.CASH.getType() ? 96 : inventory[type].getSlotLimit();
    }

    public boolean canGainSlots(int type, int slots) {
        slots += inventory[type].getSlotLimit();
        return slots <= 96;
    }

    public boolean gainSlots(int type, int slots, boolean update) {
        Integer updatedLimit = gainSlotsInternal(type, slots);
        if (updatedLimit == null) {
            return false;
        }

        if (update) {
            sendPacket(PacketCreator.updateInventorySlotLimit(type, updatedLimit));
        }
        return true;
    }

    private Integer gainSlotsInternal(int type, int slots) {
        inventory[type].lockInventory();
        try {
            if (!canGainSlots(type, slots)) {
                return null;
            }

            int newLimit = inventory[type].getSlotLimit() + slots;
            inventory[type].setSlotLimit(newLimit);
            return newLimit;
        } finally {
            inventory[type].unlockInventory();
        }
    }

    public int sellAllItemsFromName(byte invTypeId, String name) {
        //player decides from which inventory items should be sold.
        InventoryType type = InventoryType.getByType(invTypeId);

        Inventory inv = getInventory(type);
        inv.lockInventory();
        try {
            Item it = inv.findByName(name);
            if (it == null) {
                return (-1);
            }

            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            return (sellAllItemsFromPosition(ii, type, it.getPosition()));
        } finally {
            inv.unlockInventory();
        }
    }

    public int sellAllItemsFromPosition(ItemInformationProvider ii, InventoryType type, short pos) {
        int mesoGain = 0;

        Inventory inv = getInventory(type);
        inv.lockInventory();
        try {
            for (short i = pos; i <= inv.getSlotLimit(); i++) {
                if (inv.getItem(i) == null) {
                    continue;
                }
                mesoGain += standaloneSell(getClient(), ii, type, i, inv.getItem(i).getQuantity());
            }
        } finally {
            inv.unlockInventory();
        }

        return (mesoGain);
    }

    private int standaloneSell(Client c, ItemInformationProvider ii, InventoryType type, short slot, short quantity) {
        if (quantity == 0xFFFF || quantity == 0) {
            quantity = 1;
        }

        Inventory inv = getInventory(type);
        inv.lockInventory();
        try {
            Item item = inv.getItem(slot);
            if (item == null) { //Basic check
                return (0);
            }

            int itemid = item.getItemId();
            if (ItemConstants.isRechargeable(itemid)) {
                quantity = item.getQuantity();
            } else if (ItemId.isWeddingToken(itemid) || ItemId.isWeddingRing(itemid)) {
                return (0);
            }

            if (quantity < 0) {
                return (0);
            }
            short iQuant = item.getQuantity();
            if (iQuant == 0xFFFF) {
                iQuant = 1;
            }

            if (quantity <= iQuant && iQuant > 0) {
                InventoryManipulator.removeFromSlot(c, type, (byte) slot, quantity, false);
                int recvMesos = ii.getPrice(itemid, quantity);
                if (recvMesos > 0) {
                    gainMeso(recvMesos, false);
                    return (recvMesos);
                }
            }

            return (0);
        } finally {
            inv.unlockInventory();
        }
    }

    private static boolean hasMergeFlag(Item item) {
        return (item.getFlag() & ItemConstants.MERGE_UNTRADEABLE) == ItemConstants.MERGE_UNTRADEABLE;
    }

    private static void setMergeFlag(Item item) {
        short flag = item.getFlag();
        flag |= ItemConstants.MERGE_UNTRADEABLE;
        flag |= ItemConstants.UNTRADEABLE;
        item.setFlag(flag);
    }

    private List<Equip> getUpgradeableEquipped() {
        List<Equip> list = new LinkedList<>();

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (Item item : getInventory(InventoryType.EQUIPPED)) {
            if (ii.isUpgradeable(item.getItemId())) {
                list.add((Equip) item);
            }
        }

        return list;
    }

    private static List<Equip> getEquipsWithStat(List<Pair<Equip, Map<StatUpgrade, Short>>> equipped, StatUpgrade stat) {
        List<Equip> equippedWithStat = new LinkedList<>();

        for (Pair<Equip, Map<StatUpgrade, Short>> eq : equipped) {
            if (eq.getRight().containsKey(stat)) {
                equippedWithStat.add(eq.getLeft());
            }
        }

        return equippedWithStat;
    }

    public boolean mergeAllItemsFromName(String name) {
        InventoryType type = InventoryType.EQUIP;

        Inventory inv = getInventory(type);
        inv.lockInventory();
        try {
            Item it = inv.findByName(name);
            if (it == null) {
                return false;
            }

            Map<StatUpgrade, Float> statups = new LinkedHashMap<>();
            mergeAllItemsFromPosition(statups, it.getPosition());

            List<Pair<Equip, Map<StatUpgrade, Short>>> upgradeableEquipped = new LinkedList<>();
            Map<Equip, List<Pair<StatUpgrade, Integer>>> equipUpgrades = new LinkedHashMap<>();
            for (Equip eq : getUpgradeableEquipped()) {
                upgradeableEquipped.add(new Pair<>(eq, eq.getStats()));
                equipUpgrades.put(eq, new LinkedList<Pair<StatUpgrade, Integer>>());
            }

            /*
            for (Entry<StatUpgrade, Float> es : statups.entrySet()) {
                System.out.println(es);
            }
            */

            for (Entry<StatUpgrade, Float> e : statups.entrySet()) {
                Double ev = Math.sqrt(e.getValue());

                Set<Equip> extraEquipped = new LinkedHashSet<>(equipUpgrades.keySet());
                List<Equip> statEquipped = getEquipsWithStat(upgradeableEquipped, e.getKey());
                float extraRate = (float) (0.2 * Math.random());

                if (!statEquipped.isEmpty()) {
                    float statRate = 1.0f - extraRate;

                    int statup = (int) Math.ceil((ev * statRate) / statEquipped.size());
                    for (Equip statEq : statEquipped) {
                        equipUpgrades.get(statEq).add(new Pair<>(e.getKey(), statup));
                        extraEquipped.remove(statEq);
                    }
                }

                if (!extraEquipped.isEmpty()) {
                    int statup = (int) Math.round((ev * extraRate) / extraEquipped.size());
                    if (statup > 0) {
                        for (Equip extraEq : extraEquipped) {
                            equipUpgrades.get(extraEq).add(new Pair<>(e.getKey(), statup));
                        }
                    }
                }
            }

            dropMessage(6, "EQUIPMENT MERGE operation results:");
            for (Entry<Equip, List<Pair<StatUpgrade, Integer>>> eqpUpg : equipUpgrades.entrySet()) {
                List<Pair<StatUpgrade, Integer>> eqpStatups = eqpUpg.getValue();
                if (!eqpStatups.isEmpty()) {
                    Equip eqp = eqpUpg.getKey();
                    setMergeFlag(eqp);

                    String showStr = " '" + ItemInformationProvider.getInstance().getName(eqp.getItemId()) + "': ";
                    String upgdStr = eqp.gainStats(eqpStatups).getLeft();

                    this.forceUpdateItem(eqp);

                    showStr += upgdStr;
                    dropMessage(6, showStr);
                }
            }

            return true;
        } finally {
            inv.unlockInventory();
        }
    }

    public void mergeAllItemsFromPosition(Map<StatUpgrade, Float> statups, short pos) {
        Inventory inv = getInventory(InventoryType.EQUIP);
        inv.lockInventory();
        try {
            for (short i = pos; i <= inv.getSlotLimit(); i++) {
                standaloneMerge(statups, getClient(), InventoryType.EQUIP, i, inv.getItem(i));
            }
        } finally {
            inv.unlockInventory();
        }
    }

    private void standaloneMerge(Map<StatUpgrade, Float> statups, Client c, InventoryType type, short slot, Item item) {
        short quantity;
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        if (item == null || (quantity = item.getQuantity()) < 1 || ii.isCash(item.getItemId()) || !ii.isUpgradeable(item.getItemId()) || hasMergeFlag(item)) {
            return;
        }

        Equip e = (Equip) item;
        for (Entry<StatUpgrade, Short> s : e.getStats().entrySet()) {
            Float newVal = statups.get(s.getKey());

            float incVal = s.getValue().floatValue();
            switch (s.getKey()) {
                case incPAD:
                case incMAD:
                case incPDD:
                case incMDD:
                    incVal = (float) Math.log(incVal);
                    break;
            }

            if (newVal != null) {
                newVal += incVal;
            } else {
                newVal = incVal;
            }

            statups.put(s.getKey(), newVal);
        }

        InventoryManipulator.removeFromSlot(c, type, (byte) slot, quantity, false);
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public void setSlot(int slotid) {
        slots = slotid;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public void setVanquisherKills(int x) {
        this.vanquisherKills = x;
    }

    public void setVanquisherStage(int x) {
        this.vanquisherStage = x;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void shiftPetsRight() {
        petLock.lock();
        try {
            if (pets[2] == null) {
                pets[2] = pets[1];
                pets[1] = pets[0];
                pets[0] = null;
            }
        } finally {
            petLock.unlock();
        }
    }

    private long getDojoTimeLeft() {
        return client.getChannelServer().getDojoFinishTime(map.getId()) - Server.getInstance().getCurrentTime();
    }

    public void showDojoClock() {
        if (GameConstants.isDojoBossArea(map.getId())) {
            sendPacket(PacketCreator.getClock((int) (getDojoTimeLeft() / 1000)));
        }
    }

    public void showUnderleveledInfo(Monster mob) {
        long curTime = Server.getInstance().getCurrentTime();
        if (nextWarningTime < curTime) {
            nextWarningTime = curTime + MINUTES.toMillis(1);   // show underlevel info again after 1 minute

            showHint("You have gained #rno experience#k from defeating #e#b" + mob.getName() + "#k#n (lv. #b" + mob.getLevel() + "#k)! Take note you must have around the same level as the mob to start earning EXP from it.");
        }
    }

    public void showMapOwnershipInfo(Character mapOwner) {
        long curTime = Server.getInstance().getCurrentTime();
        if (nextWarningTime < curTime) {
            nextWarningTime = curTime + MINUTES.toMillis(1); // show underlevel info again after 1 minute

            String medal = "";
            Item medalItem = mapOwner.getInventory(InventoryType.EQUIPPED).getItem((short) -49);
            if (medalItem != null) {
                medal = "<" + ItemInformationProvider.getInstance().getName(medalItem.getItemId()) + "> ";
            }

            List<String> strLines = new LinkedList<>();
            strLines.add("");
            strLines.add("");
            strLines.add("");
            strLines.add(this.getClient().getChannelServer().getServerMessage().isEmpty() ? 0 : 1, "Get off my lawn!!");

            this.sendPacket(PacketCreator.getAvatarMega(mapOwner, medal, this.getClient().getChannel(), ItemId.ROARING_TIGER_MESSENGER, strLines, true));
        }
    }

    public void showHint(String msg) {
        showHint(msg, 500);
    }

    public void showHint(String msg, int length) {
        client.announceHint(msg, length);
    }

    public void silentGiveBuffs(List<Pair<Long, PlayerBuffValueHolder>> buffs) {
        for (Pair<Long, PlayerBuffValueHolder> mbsv : buffs) {
            PlayerBuffValueHolder mbsvh = mbsv.getRight();
            mbsvh.effect.silentApplyBuff(this, mbsv.getLeft());
        }
    }

    public void silentPartyUpdate() {
        silentPartyUpdateInternal(getParty());
    }

    private void silentPartyUpdateInternal(Party chrParty) {
        if (chrParty != null) {
            getWorldServer().updateParty(chrParty.getId(), PartyOperation.SILENT_UPDATE, getMPC());
        }
    }

    public static class SkillEntry {

        public int masterlevel;
        public byte skillevel;
        public long expiration;

        public SkillEntry(byte skillevel, int masterlevel, long expiration) {
            this.skillevel = skillevel;
            this.masterlevel = masterlevel;
            this.expiration = expiration;
        }

        @Override
        public String toString() {
            return skillevel + ":" + masterlevel;
        }
    }

    public boolean skillIsCooling(int skillId) {
        effLock.lock();
        chrLock.lock();
        try {
            return coolDowns.containsKey(Integer.valueOf(skillId));
        } finally {
            chrLock.unlock();
            effLock.unlock();
        }
    }

    public void runFullnessSchedule(int petSlot) {
        Pet pet = getPet(petSlot);
        if (pet == null) {
            return;
        }

        int newFullness = pet.getFullness() - PetDataFactory.getHunger(pet.getItemId());
        if (newFullness <= 5) {
            pet.setFullness(15);
            pet.saveToDb();
            unequipPet(pet, true);
            dropMessage(6, "Your pet grew hungry! Treat it some pet food to keep it healthy!");
        } else {
            pet.setFullness(newFullness);
            pet.saveToDb();
            Item petz = getInventory(InventoryType.CASH).getItem(pet.getPosition());
            if (petz != null) {
                forceUpdateItem(petz);
            }
        }
    }

    public boolean runTirednessSchedule() {
        if (maplemount != null) {
            int tiredness = maplemount.incrementAndGetTiredness();

            this.getMap().broadcastMessage(PacketCreator.updateMount(this.getId(), maplemount, false));
            if (tiredness > 99) {
                maplemount.setTiredness(99);
                this.dispelSkill(this.getJobType() * 10000000 + 1004);
                this.dropMessage(6, "Your mount grew tired! Treat it some revitalizer before riding it again!");
                return false;
            }
        }

        return true;
    }

    public void startMapEffect(String msg, int itemId) {
        startMapEffect(msg, itemId, 30000);
    }

    public void startMapEffect(String msg, int itemId, int duration) {
        final MapEffect mapEffect = new MapEffect(msg, itemId);
        sendPacket(mapEffect.makeStartData());
        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                sendPacket(mapEffect.makeDestroyData());
            }
        }, duration);
    }

    public void unequipAllPets() {
        for (int i = 0; i < 3; i++) {
            Pet pet = getPet(i);
            if (pet != null) {
                unequipPet(pet, true);
            }
        }
    }

    public void unequipPet(Pet pet, boolean shift_left) {
        unequipPet(pet, shift_left, false);
    }

    public void unequipPet(Pet pet, boolean shift_left, boolean hunger) {
        byte petIdx = this.getPetIndex(pet);
        Pet chrPet = this.getPet(petIdx);

        if (chrPet != null) {
            chrPet.setSummoned(false);
            chrPet.saveToDb();
        }

        this.getClient().getWorldServer().unregisterPetHunger(this, petIdx);
        getMap().broadcastMessage(this, PacketCreator.showPet(this, pet, true, hunger), true);

        removePet(pet, shift_left);
        commitExcludedItems();

        sendPacket(PacketCreator.petStatUpdate(this));
        sendPacket(PacketCreator.enableActions());
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
    }

    public void updatePartyMemberHP() {
        prtLock.lock();
        try {
            updatePartyMemberHPInternal();
        } finally {
            prtLock.unlock();
        }
    }

    private void updatePartyMemberHPInternal() {
        if (party != null) {
            int curmaxhp = getCurrentMaxHp();
            int curhp = getHp();
            for (Character partychar : this.getPartyMembersOnSameMap()) {
                partychar.sendPacket(PacketCreator.updatePartyMemberHP(getId(), curhp, curmaxhp));
            }
        }
    }

    public void setQuestProgress(int id, int infoNumber, String progress) {
        Quest q = Quest.getInstance(id);
        QuestStatus qs = getQuest(q);

        if (qs.getInfoNumber() == infoNumber && infoNumber > 0) {
            Quest iq = Quest.getInstance(infoNumber);
            QuestStatus iqs = getQuest(iq);
            iqs.setProgress(0, progress);
        } else {
            qs.setProgress(infoNumber, progress);   // quest progress is thoroughly a string match, infoNumber is actually another questid
        }

        announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, false);
        if (qs.getInfoNumber() > 0) {
            announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, true);
        }
    }

    public enum DelayedQuestUpdate {    // quest updates allow player actions during NPC talk...
        UPDATE, FORFEIT, COMPLETE, INFO
    }

    private void announceUpdateQuestInternal(Character chr, Pair<DelayedQuestUpdate, Object[]> questUpdate) {
        Object[] objs = questUpdate.getRight();

        switch (questUpdate.getLeft()) {
            case UPDATE:
                sendPacket(PacketCreator.updateQuest(chr, (QuestStatus) objs[0], (Boolean) objs[1]));
                break;

            case FORFEIT:
                sendPacket(PacketCreator.forfeitQuest((Short) objs[0]));
                break;

            case COMPLETE:
                sendPacket(PacketCreator.completeQuest((Short) objs[0], (Long) objs[1]));
                break;

            case INFO:
                QuestStatus qs = (QuestStatus) objs[0];
                sendPacket(PacketCreator.updateQuestInfo(qs.getQuest().getId(), qs.getNpc()));
                break;
        }
    }

    public void announceUpdateQuest(DelayedQuestUpdate questUpdateType, Object... params) {
        Pair<DelayedQuestUpdate, Object[]> p = new Pair<>(questUpdateType, params);
        Client c = this.getClient();
        if (c.getQM() != null || c.getCM() != null) {
            synchronized (npcUpdateQuests) {
                npcUpdateQuests.add(p);
            }
        } else {
            announceUpdateQuestInternal(this, p);
        }
    }

    public void flushDelayedUpdateQuests() {
        List<Pair<DelayedQuestUpdate, Object[]>> qmQuestUpdateList;

        synchronized (npcUpdateQuests) {
            qmQuestUpdateList = new ArrayList<>(npcUpdateQuests);
            npcUpdateQuests.clear();
        }

        for (Pair<DelayedQuestUpdate, Object[]> q : qmQuestUpdateList) {
            announceUpdateQuestInternal(this, q);
        }
    }

    public void updateQuestStatus(QuestStatus qs) {
        synchronized (quests) {
            quests.put(qs.getQuestID(), qs);
        }
        if (qs.getStatus().equals(QuestStatus.Status.STARTED)) {
            announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, false);
            if (qs.getInfoNumber() > 0) {
                announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, true);
            }
            announceUpdateQuest(DelayedQuestUpdate.INFO, qs);
        } else if (qs.getStatus().equals(QuestStatus.Status.COMPLETED)) {
            Quest mquest = qs.getQuest();
            short questid = mquest.getId();
            qs.setCompleted(qs.getCompleted() + 1);   // Jayd's idea - count quest completed

            announceUpdateQuest(DelayedQuestUpdate.COMPLETE, questid, qs.getCompletionTime());
            //announceUpdateQuest(DelayedQuestUpdate.INFO, qs); // happens after giving rewards, for non-next quests only
        } else if (qs.getStatus().equals(QuestStatus.Status.NOT_STARTED)) {
            announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, false);
            if (qs.getInfoNumber() > 0) {
                announceUpdateQuest(DelayedQuestUpdate.UPDATE, qs, true);
            }
            // reminder: do not reset quest progress of infoNumbers, some quests cannot backtrack
        }
    }

    private void expireQuest(Quest quest) {
        if (quest.forfeit(this)) {
            sendPacket(PacketCreator.questExpire(quest.getId()));
        }
    }

    public void cancelQuestExpirationTask() {
        evtLock.lock();
        try {
            if (questExpireTask != null) {
                questExpireTask.cancel(false);
                questExpireTask = null;
            }
        } finally {
            evtLock.unlock();
        }
    }

    public void forfeitExpirableQuests() {
        evtLock.lock();
        try {
            for (Quest quest : questExpirations.keySet()) {
                quest.forfeit(this);
            }

            questExpirations.clear();
        } finally {
            evtLock.unlock();
        }
    }

    public void questExpirationTask() {
        evtLock.lock();
        try {
            if (!questExpirations.isEmpty()) {
                if (questExpireTask == null) {
                    questExpireTask = TimerManager.getInstance().register(new Runnable() {
                        @Override
                        public void run() {
                            runQuestExpireTask();
                        }
                    }, SECONDS.toMillis(10));
                }
            }
        } finally {
            evtLock.unlock();
        }
    }

    private void runQuestExpireTask() {
        evtLock.lock();
        try {
            long timeNow = Server.getInstance().getCurrentTime();
            List<Quest> expireList = new LinkedList<>();

            for (Entry<Quest, Long> qe : questExpirations.entrySet()) {
                if (qe.getValue() <= timeNow) {
                    expireList.add(qe.getKey());
                }
            }

            if (!expireList.isEmpty()) {
                for (Quest quest : expireList) {
                    expireQuest(quest);
                    questExpirations.remove(quest);
                }

                if (questExpirations.isEmpty()) {
                    questExpireTask.cancel(false);
                    questExpireTask = null;
                }
            }
        } finally {
            evtLock.unlock();
        }
    }

    private void registerQuestExpire(Quest quest, long time) {
        evtLock.lock();
        try {
            if (questExpireTask == null) {
                questExpireTask = TimerManager.getInstance().register(new Runnable() {
                    @Override
                    public void run() {
                        runQuestExpireTask();
                    }
                }, SECONDS.toMillis(10));
            }

            questExpirations.put(quest, Server.getInstance().getCurrentTime() + time);
        } finally {
            evtLock.unlock();
        }
    }

    public void questTimeLimit(final Quest quest, int seconds) {
        registerQuestExpire(quest, SECONDS.toMillis(seconds));
        sendPacket(PacketCreator.addQuestTimeLimit(quest.getId(), (int) SECONDS.toMillis(seconds)));
    }

    public void questTimeLimit2(final Quest quest, long expires) {
        long timeLeft = expires - System.currentTimeMillis();

        if (timeLeft <= 0) {
            expireQuest(quest);
        } else {
            registerQuestExpire(quest, timeLeft);
        }
    }

    public void updateSingleStat(Stat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }

    private void updateSingleStat(Stat stat, int newval, boolean itemReaction) {
        sendPacket(PacketCreator.updatePlayerStats(Collections.singletonList(new Pair<>(stat, Integer.valueOf(newval))), itemReaction, this));
    }

    public void sendPacket(Packet packet) {
        client.sendPacket(packet);
    }

    @Override
    public int getObjectId() {
        return getId();
    }

    @Override
    public MapObjectType getType() {
        return MapObjectType.PLAYER;
    }

    @Override
    public void sendDestroyData(Client client) {
        client.sendPacket(PacketCreator.removePlayerFromMap(this.getObjectId()));
    }

    @Override
    public void sendSpawnData(Client client) {
        if (!this.isHidden() || client.getPlayer().gmLevel() > 1) {
            client.sendPacket(PacketCreator.spawnPlayerMapObject(client, this, false));

            if (buffEffects.containsKey(getJobMapChair(job))) { // mustn't effLock, chrLock sendSpawnData
                client.sendPacket(PacketCreator.giveForeignChairSkillEffect(id));
            }
        }

        if (this.isHidden()) {
            List<Pair<BuffStat, Integer>> dsstat = Collections.singletonList(new Pair<>(BuffStat.DARKSIGHT, 0));
            getMap().broadcastGMMessage(this, PacketCreator.giveForeignBuff(getId(), dsstat), false);
        }
    }

    @Override
    public void setObjectId(int id) {}

    @Override
    public String toString() {
        return name;
    }

    public int getLinkedLevel() {
        return linkedLevel;
    }

    public String getLinkedName() {
        return linkedName;
    }

    public CashShop getCashShop() {
        return cashshop;
    }

    public Set<NewYearCardRecord> getNewYearRecords() {
        return newyears;
    }

    public Set<NewYearCardRecord> getReceivedNewYearRecords() {
        Set<NewYearCardRecord> received = new LinkedHashSet<>();

        for (NewYearCardRecord nyc : newyears) {
            if (nyc.isReceiverCardReceived()) {
                received.add(nyc);
            }
        }

        return received;
    }

    public NewYearCardRecord getNewYearRecord(int cardid) {
        for (NewYearCardRecord nyc : newyears) {
            if (nyc.getId() == cardid) {
                return nyc;
            }
        }

        return null;
    }

    public void addNewYearRecord(NewYearCardRecord newyear) {
        newyears.add(newyear);
    }

    public void removeNewYearRecord(NewYearCardRecord newyear) {
        newyears.remove(newyear);
    }

    public void portalDelay(long delay) {
        this.portaldelay = System.currentTimeMillis() + delay;
    }

    public long portalDelay() {
        return portaldelay;
    }

    public void blockPortal(String scriptName) {
        if (!blockedPortals.contains(scriptName) && scriptName != null) {
            blockedPortals.add(scriptName);
            sendPacket(PacketCreator.enableActions());
        }
    }

    public void unblockPortal(String scriptName) {
        if (blockedPortals.contains(scriptName) && scriptName != null) {
            blockedPortals.remove(scriptName);
        }
    }

    public List<String> getBlockedPortals() {
        return blockedPortals;
    }

    public boolean containsAreaInfo(int area, String info) {
        Short area_ = Short.valueOf((short) area);
        if (area_info.containsKey(area_)) {
            return area_info.get(area_).contains(info);
        }
        return false;
    }

    public void updateAreaInfo(int area, String info) {
        area_info.put(Short.valueOf((short) area), info);
        sendPacket(PacketCreator.updateAreaInfo(area, info));
    }

    public String getAreaInfo(int area) {
        return area_info.get(Short.valueOf((short) area));
    }

    public Map<Short, String> getAreaInfos() {
        return area_info;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned() {
        isBanned = true;
    }

    public List<Integer> getTrockMaps() {
        return trockmaps;
    }

    public List<Integer> getVipTrockMaps() {
        return viptrockmaps;
    }

    public int getTrockSize() {
        int ret = trockmaps.indexOf(MapId.NONE);
        if (ret == -1) {
            ret = 5;
        }

        return ret;
    }

    public void deleteFromTrocks(int map) {
        trockmaps.remove(Integer.valueOf(map));
        while (trockmaps.size() < 10) {
            trockmaps.add(MapId.NONE);
        }
    }

    public void addTrockMap() {
        int index = trockmaps.indexOf(MapId.NONE);
        if (index != -1) {
            trockmaps.set(index, getMapId());
        }
    }

    public boolean isTrockMap(int id) {
        int index = trockmaps.indexOf(id);
        return index != -1;
    }

    public int getVipTrockSize() {
        int ret = viptrockmaps.indexOf(MapId.NONE);

        if (ret == -1) {
            ret = 10;
        }

        return ret;
    }

    public void deleteFromVipTrocks(int map) {
        viptrockmaps.remove(Integer.valueOf(map));
        while (viptrockmaps.size() < 10) {
            viptrockmaps.add(MapId.NONE);
        }
    }

    public void addVipTrockMap() {
        int index = viptrockmaps.indexOf(MapId.NONE);
        if (index != -1) {
            viptrockmaps.set(index, getMapId());
        }
    }

    public boolean isVipTrockMap(int id) {
        int index = viptrockmaps.indexOf(id);
        return index != -1;
    }

    public AutobanManager getAutobanManager() {
        return autoban;
    }

    public void equippedItem(Equip equip) {
        int itemid = equip.getItemId();

        if (itemid == ItemId.PENDANT_OF_THE_SPIRIT) {
            this.equipPendantOfSpirit();
        } else if (itemid == ItemId.MESO_MAGNET) {
            equippedMesoMagnet = true;
        } else if (itemid == ItemId.ITEM_POUCH) {
            equippedItemPouch = true;
        } else if (itemid == ItemId.ITEM_IGNORE) {
            equippedPetItemIgnore = true;
        }
    }

    public void unequippedItem(Equip equip) {
        int itemid = equip.getItemId();

        if (itemid == ItemId.PENDANT_OF_THE_SPIRIT) {
            this.unequipPendantOfSpirit();
        } else if (itemid == ItemId.MESO_MAGNET) {
            equippedMesoMagnet = false;
        } else if (itemid == ItemId.ITEM_POUCH) {
            equippedItemPouch = false;
        } else if (itemid == ItemId.ITEM_IGNORE) {
            equippedPetItemIgnore = false;
        }
    }

    public boolean isEquippedMesoMagnet() {
        return equippedMesoMagnet;
    }

    public boolean isEquippedItemPouch() {
        return equippedItemPouch;
    }

    public boolean isEquippedPetItemIgnore() {
        return equippedPetItemIgnore;
    }

    private void equipPendantOfSpirit() {
        if (pendantOfSpirit == null) {
            pendantOfSpirit = TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    if (pendantExp < 3) {
                        pendantExp++;
                        message("Pendant of the Spirit has been equipped for " + pendantExp + " hour(s), you will now receive " + pendantExp + "0% bonus exp.");
                    } else {
                        pendantOfSpirit.cancel(false);
                    }
                }
            }, 3600000); //1 hour
        }
    }

    private void unequipPendantOfSpirit() {
        if (pendantOfSpirit != null) {
            pendantOfSpirit.cancel(false);
            pendantOfSpirit = null;
        }
        pendantExp = 0;
    }

    private Collection<Item> getUpgradeableEquipList() {
        Collection<Item> fullList = getInventory(InventoryType.EQUIPPED).list();
        if (YamlConfig.config.server.USE_EQUIPMNT_LVLUP_CASH) {
            return fullList;
        }

        Collection<Item> eqpList = new LinkedHashSet<>();
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (Item it : fullList) {
            if (!ii.isCash(it.getItemId())) {
                eqpList.add(it);
            }
        }

        return eqpList;
    }

    public void increaseEquipExp(int expGain) {
        if (allowExpGain) {     // thanks Vcoc for suggesting equip EXP gain conditionally
            if (expGain < 0) {
                expGain = Integer.MAX_VALUE;
            }

            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            for (Item item : getUpgradeableEquipList()) {
                Equip nEquip = (Equip) item;
                String itemName = ii.getName(nEquip.getItemId());
                if (itemName == null) {
                    continue;
                }

                nEquip.gainItemExp(client, expGain);
            }
        }
    }

    public void showAllEquipFeatures() {
        String showMsg = "";

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (Item item : getInventory(InventoryType.EQUIPPED).list()) {
            Equip nEquip = (Equip) item;
            String itemName = ii.getName(nEquip.getItemId());
            if (itemName == null) {
                continue;
            }

            showMsg += nEquip.showEquipFeatures(client);
        }

        if (!showMsg.isEmpty()) {
            this.showHint("#ePLAYER EQUIPMENTS:#n\r\n\r\n" + showMsg, 400);
        }
    }

    public void broadcastMarriageMessage() {
        Guild guild = this.getGuild();
        if (guild != null) {
            guild.broadcast(PacketCreator.marriageMessage(0, name));
        }

        Family family = this.getFamily();
        if (family != null) {
            family.broadcast(PacketCreator.marriageMessage(1, name));
        }
    }

    public Map<String, Events> getEvents() {
        return events;
    }

    public PartyQuest getPartyQuest() {
        return partyQuest;
    }

    public void setPartyQuest(PartyQuest pq) {
        this.partyQuest = pq;
    }

    public void setCpqTimer(ScheduledFuture timer) {
        this.cpqSchedule = timer;
    }

    public void clearCpqTimer() {
        if (cpqSchedule != null) {
            cpqSchedule.cancel(true);
        }
        cpqSchedule = null;
    }

    public final void empty(final boolean remove) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(true);
        }
        dragonBloodSchedule = null;

        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(true);
        }
        hpDecreaseTask = null;

        if (beholderHealingSchedule != null) {
            beholderHealingSchedule.cancel(true);
        }
        beholderHealingSchedule = null;

        if (beholderBuffSchedule != null) {
            beholderBuffSchedule.cancel(true);
        }
        beholderBuffSchedule = null;

        if (berserkSchedule != null) {
            berserkSchedule.cancel(true);
        }
        berserkSchedule = null;

        unregisterChairBuff();
        cancelBuffExpireTask();
        cancelDiseaseExpireTask();
        cancelSkillCooldownTask();
        cancelExpirationTask();

        if (questExpireTask != null) {
            questExpireTask.cancel(true);
        }
        questExpireTask = null;

        if (recoveryTask != null) {
            recoveryTask.cancel(true);
        }
        recoveryTask = null;

        if (extraRecoveryTask != null) {
            extraRecoveryTask.cancel(true);
        }
        extraRecoveryTask = null;

        // already done on unregisterChairBuff
        /* if (chairRecoveryTask != null) { chairRecoveryTask.cancel(true); }
        chairRecoveryTask = null; */

        if (pendantOfSpirit != null) {
            pendantOfSpirit.cancel(true);
        }
        pendantOfSpirit = null;

        clearCpqTimer();

        evtLock.lock();
        try {
            if (questExpireTask != null) {
                questExpireTask.cancel(false);
                questExpireTask = null;

                questExpirations.clear();
                questExpirations = null;
            }
        } finally {
            evtLock.unlock();
        }

        if (maplemount != null) {
            maplemount.empty();
            maplemount = null;
        }
        if (remove) {
            partyQuest = null;
            events = null;
            mpc = null;
            mgc = null;
            party = null;
            FamilyEntry familyEntry = getFamilyEntry();
            if (familyEntry != null) {
                familyEntry.setCharacter(null);
                setFamilyEntry(null);
            }

            getWorldServer().registerTimedMapObject(new Runnable() {
                @Override
                public void run() {
                    client = null;  // clients still triggers handlers a few times after disconnecting
                    map = null;
                    setListener(null);

                    // thanks Shavit for noticing a memory leak with inventories holding owner object
                    for (int i = 0; i < inventory.length; i++) {
                        inventory[i].dispose();
                    }
                    inventory = null;
                }
            }, MINUTES.toMillis(5));
        }
    }

    public void logOff() {
        this.loggedIn = false;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE characters SET lastLogoutTime=? WHERE id=?")) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setLoginTime(long time) {
        this.loginTime = time;
    }

    public long getLoginTime() {
        return loginTime;
    }

    public long getLoggedInTime() {
        return System.currentTimeMillis() - loginTime;
    }

    public boolean isLoggedin() {
        return loggedIn;
    }

    public void setMapId(int mapid) {
        this.mapid = mapid;
    }

    public boolean getWhiteChat() {
        return isGM() && whiteChat;
    }

    public void toggleWhiteChat() {
        whiteChat = !whiteChat;
    }

    // These need to be renamed, but I am too lazy right now to go through the scripts and rename them...
    public String getPartyQuestItems() {
        return dataString;
    }

    public boolean gotPartyQuestItem(String partyquestchar) {
        return dataString.contains(partyquestchar);
    }

    public void removePartyQuestItem(String letter) {
        if (gotPartyQuestItem(letter)) {
            dataString = dataString.substring(0, dataString.indexOf(letter)) + dataString.substring(dataString.indexOf(letter) + letter.length());
        }
    }

    public void setPartyQuestItemObtained(String partyquestchar) {
        if (!dataString.contains(partyquestchar)) {
            this.dataString += partyquestchar;
        }
    }

    public void createDragon() {
        dragon = new Dragon(this);
    }

    public Dragon getDragon() {
        return dragon;
    }

    public void setDragon(Dragon dragon) {
        this.dragon = dragon;
    }

    public void setAutopotHpAlert(float hpPortion) {
        autopotHpAlert = hpPortion;
    }

    public float getAutopotHpAlert() {
        return autopotHpAlert;
    }

    public void setAutopotMpAlert(float mpPortion) {
        autopotMpAlert = mpPortion;
    }

    public float getAutopotMpAlert() {
        return autopotMpAlert;
    }

    public long getJailExpirationTimeLeft() {
        return jailExpiration - System.currentTimeMillis();
    }

    private void setFutureJailExpiration(long time) {
        jailExpiration = System.currentTimeMillis() + time;
    }

    public void addJailExpirationTime(long time) {
        long timeLeft = getJailExpirationTimeLeft();

        if (timeLeft <= 0) {
            setFutureJailExpiration(time);
        } else {
            setFutureJailExpiration(timeLeft + time);
        }
    }

    public void removeJailExpirationTime() {
        jailExpiration = 0;
    }

    public boolean registerNameChange(String newName) {
        try (Connection con = DatabaseConnection.getConnection()) {
            //check for pending name change
            long currentTimeMillis = System.currentTimeMillis();
            try (PreparedStatement ps = con.prepareStatement("SELECT completionTime FROM namechanges WHERE characterid=?")) { //double check, just in case
                ps.setInt(1, getId());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp completedTimestamp = rs.getTimestamp("completionTime");
                        if (completedTimestamp == null) {
                            return false; //pending
                        } else if (completedTimestamp.getTime() + YamlConfig.config.server.NAME_CHANGE_COOLDOWN > currentTimeMillis) {
                            return false;
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("Failed to register name change for chr {}", getName(), e);
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement("INSERT INTO namechanges (characterid, old, new) VALUES (?, ?, ?)")) {
                ps.setInt(1, getId());
                ps.setString(2, getName());
                ps.setString(3, newName);
                ps.executeUpdate();
                this.pendingNameChange = true;
                return true;
            } catch (SQLException e) {
                log.error("Failed to register name change for chr {}", getName(), e);
            }
        } catch (SQLException e) {
            log.error("Failed to get DB connection while registering name change", e);
        }
        return false;
    }

    public boolean cancelPendingNameChange() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM namechanges WHERE characterid=? AND completionTime IS NULL")) {
            ps.setInt(1, getId());
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                pendingNameChange = false;
            }
            return affectedRows > 0; //rows affected
        } catch (SQLException e) {
            log.error("Failed to cancel name change for chr {}", getName(), e);
            return false;
        }
    }

    public void doPendingNameChange() { //called on logout
        if (!pendingNameChange) {
            return;
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            int nameChangeId = -1;
            String newName = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM namechanges WHERE characterid = ? AND completionTime IS NULL")) {
                ps.setInt(1, getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return;
                    }
                    nameChangeId = rs.getInt("id");
                    newName = rs.getString("new");
                }
            } catch (SQLException e) {
                log.error("Failed to retrieve pending name changes for chr {}", this.name, e);
            }

            con.setAutoCommit(false);
            boolean success = doNameChange(con, getId(), getName(), newName, nameChangeId);
            if (!success) {
                con.rollback();
            } else {
                log.info("Name change applied: from {} to {}", this.name, newName);
            }
            con.setAutoCommit(true);
        } catch (SQLException e) {
            log.error("Failed to get DB connection for pending chr name change", e);
        }
    }

    public static void doNameChange(int characterId, String oldName, String newName, int nameChangeId) { //Don't do this while player is online
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            boolean success = doNameChange(con, characterId, oldName, newName, nameChangeId);
            if (!success) {
                con.rollback();
            }
            con.setAutoCommit(true);
        } catch (SQLException e) {
            log.error("Failed to get DB connection for chr name change", e);
        }
    }

    public static boolean doNameChange(Connection con, int characterId, String oldName, String newName, int nameChangeId) {
        try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET name = ? WHERE id = ?")) {
            ps.setString(1, newName);
            ps.setInt(2, characterId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to perform chr name change in database for chrId {}", characterId, e);
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("UPDATE rings SET partnername = ? WHERE partnername = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update rings during chr name change for chrId {}", characterId, e);
            return false;
        }

        /*try (PreparedStatement ps = con.prepareStatement("UPDATE playernpcs SET name = ? WHERE name = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("UPDATE gifts SET `from` = ? WHERE `from` = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }
        try (PreparedStatement ps = con.prepareStatement("UPDATE dueypackages SET SenderName = ? WHERE SenderName = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("UPDATE dueypackages SET SenderName = ? WHERE SenderName = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("UPDATE inventoryitems SET owner = ? WHERE owner = ?")) { //GMS doesn't do this
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("UPDATE mts_items SET owner = ? WHERE owner = ?")) { //GMS doesn't do this
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("UPDATE newyear SET sendername = ? WHERE sendername = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("UPDATE newyear SET receivername = ? WHERE receivername = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("UPDATE notes SET `to` = ? WHERE `to` = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("UPDATE notes SET `from` = ? WHERE `from` = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("UPDATE nxcode SET retriever = ? WHERE retriever = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        } catch(SQLException e) { 
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Character ID : " + characterId);
            return false;
        }*/

        if (nameChangeId != -1) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE namechanges SET completionTime = ? WHERE id = ?")) {
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ps.setInt(2, nameChangeId);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.error("Failed to save chr name change for chrId {}", nameChangeId, e);
                return false;
            }
        }
        return true;
    }

    public int checkWorldTransferEligibility() {
        if (getLevel() < 20) {
            return 2;
        } else if (client.wasRecentlyBanned()) {
            return 3;
        } else if (isMarried()) {
            return 4;
        } else if (getGuildRank() < 2) {
            return 5;
        } else if (getFamily() != null) {
            return 8;
        } else {
            return 0;
        }
    }

    public static String checkWorldTransferEligibility(Connection con, int characterId, int oldWorld, int newWorld) {
        if (!YamlConfig.config.server.ALLOW_CASHSHOP_WORLD_TRANSFER) {
            return "World transfers disabled.";
        }
        int accountId = -1;
        try (PreparedStatement ps = con.prepareStatement("SELECT accountid, level, guildid, guildrank, partnerId, familyId FROM characters WHERE id = ?")) {
            ps.setInt(1, characterId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return "Character does not exist.";
            }
            accountId = rs.getInt("accountid");
            if (rs.getInt("level") < 20) {
                return "Character is under level 20.";
            }
            if (rs.getInt("familyId") != -1) {
                return "Character is in family.";
            }
            if (rs.getInt("partnerId") != 0) {
                return "Character is married.";
            }
            if (rs.getInt("guildid") != 0 && rs.getInt("guildrank") < 2) {
                return "Character is the leader of a guild.";
            }
        } catch (SQLException e) {
            log.error("Change character name", e);
            return "SQL Error";
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT tempban FROM accounts WHERE id = ?")) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return "Account does not exist.";
            }
            LocalDateTime tempban = rs.getTimestamp("tempban").toLocalDateTime();
            if (!tempban.equals(DefaultDates.getTempban())) {
                return "Account has been banned.";
            }
        } catch (SQLException e) {
            log.error("Change character name", e);
            return "SQL Error";
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) AS rowcount FROM characters WHERE accountid = ? AND world = ?")) {
            ps.setInt(1, accountId);
            ps.setInt(2, newWorld);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return "SQL Error";
            }
            if (rs.getInt("rowcount") >= 3) {
                return "Too many characters on destination world.";
            }
        } catch (SQLException e) {
            log.error("Change character name", e);
            return "SQL Error";
        }
        return null;
    }

    public boolean registerWorldTransfer(int newWorld) {
        try (Connection con = DatabaseConnection.getConnection()) {
            //check for pending world transfer
            long currentTimeMillis = System.currentTimeMillis();
            try (PreparedStatement ps = con.prepareStatement("SELECT completionTime FROM worldtransfers WHERE characterid=?")) { //double check, just in case
                ps.setInt(1, getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Timestamp completedTimestamp = rs.getTimestamp("completionTime");
                    if (completedTimestamp == null) {
                        return false; //pending
                    } else if (completedTimestamp.getTime() + YamlConfig.config.server.WORLD_TRANSFER_COOLDOWN > currentTimeMillis) {
                        return false;
                    }
                }
            } catch (SQLException e) {
                log.error("Failed to register world transfer for chr {}", getName(), e);
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement("INSERT INTO worldtransfers (characterid, `from`, `to`) VALUES (?, ?, ?)")) {
                ps.setInt(1, getId());
                ps.setInt(2, getWorld());
                ps.setInt(3, newWorld);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                log.error("Failed to register world transfer for chr {}", getName(), e);
            }
        } catch (SQLException e) {
            log.error("Failed to get DB connection while registering world transfer", e);
        }
        return false;
    }

    public boolean cancelPendingWorldTranfer() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM worldtransfers WHERE characterid=? AND completionTime IS NULL")) {
            ps.setInt(1, getId());
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0; //rows affected
        } catch (SQLException e) {
            log.error("Failed to cancel pending world transfer for chr {}", getName(), e);
            return false;
        }
    }

    public static boolean doWorldTransfer(Connection con, int characterId, int oldWorld, int newWorld, int worldTransferId) {
        int mesos = 0;
        try (PreparedStatement ps = con.prepareStatement("SELECT meso FROM characters WHERE id = ?")) {
            ps.setInt(1, characterId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                log.warn("Character data invalid for world transfer? chrId {}", characterId);
                return false;
            }
            mesos = rs.getInt("meso");
        } catch (SQLException e) {
            log.error("Failed to do world transfer for chrId {}", characterId, e);
            return false;
        }
        try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET world = ?, meso = ?, guildid = ?, guildrank = ? WHERE id = ?")) {
            ps.setInt(1, newWorld);
            ps.setInt(2, Math.min(mesos, 1000000)); // might want a limit in "YamlConfig.config.server" for this
            ps.setInt(3, 0);
            ps.setInt(4, 5);
            ps.setInt(5, characterId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update chrId {} during world transfer", characterId, e);
            return false;
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM buddies WHERE characterid = ? OR buddyid = ?")) {
            ps.setInt(1, characterId);
            ps.setInt(2, characterId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete buddies for chrId {} during world transfer", characterId, e);
            return false;
        }
        if (worldTransferId != -1) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE worldtransfers SET completionTime = ? WHERE id = ?")) {
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ps.setInt(2, worldTransferId);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.error("Failed to update world transfer for chrId {}", characterId, e);
                return false;
            }
        }
        return true;
    }

    public String getLastCommandMessage() {
        return this.commandtext;
    }

    public void setLastCommandMessage(String text) {
        this.commandtext = text;
    }

    //EVENTS
    private byte team = 0;
    private Fitness fitness;
    private Ola ola;
    private long snowballattack;

    public byte getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = (byte) team;
    }

    public Ola getOla() {
        return ola;
    }

    public void setOla(Ola ola) {
        this.ola = ola;
    }

    public Fitness getFitness() {
        return fitness;
    }

    public void setFitness(Fitness fit) {
        this.fitness = fit;
    }

    public long getLastSnowballAttack() {
        return snowballattack;
    }

    public void setLastSnowballAttack(long time) {
        this.snowballattack = time;
    }

    // MCPQ

    public AriantColiseum ariantColiseum;
    private MonsterCarnival monsterCarnival;
    private MonsterCarnivalParty monsterCarnivalParty = null;

    private int cp = 0;
    private int totCP = 0;
    private int FestivalPoints;
    private boolean challenged = false;
    public short totalCP, availableCP;

    public void gainFestivalPoints(int gain) {
        this.FestivalPoints += gain;
    }

    public int getFestivalPoints() {
        return this.FestivalPoints;
    }

    public void setFestivalPoints(int pontos) {
        this.FestivalPoints = pontos;
    }

    public int getCP() {
        return cp;
    }

    public void addCP(int ammount) {
        totalCP += ammount;
        availableCP += ammount;
    }

    public void useCP(int ammount) {
        availableCP -= ammount;
    }

    public void gainCP(int gain) {
        if (this.getMonsterCarnival() != null) {
            if (gain > 0) {
                this.setTotalCP(this.getTotalCP() + gain);
            }
            this.setCP(this.getCP() + gain);
            if (this.getParty() != null) {
                this.getMonsterCarnival().setCP(this.getMonsterCarnival().getCP(team) + gain, team);
                if (gain > 0) {
                    this.getMonsterCarnival().setTotalCP(this.getMonsterCarnival().getTotalCP(team) + gain, team);
                }
            }
            if (this.getCP() > this.getTotalCP()) {
                this.setTotalCP(this.getCP());
            }
            sendPacket(PacketCreator.CPUpdate(false, this.getCP(), this.getTotalCP(), getTeam()));
            if (this.getParty() != null && getTeam() != -1) {
                this.getMap().broadcastMessage(PacketCreator.CPUpdate(true, this.getMonsterCarnival().getCP(team), this.getMonsterCarnival().getTotalCP(team), getTeam()));
            } else {
            }
        }
    }

    public void setTotalCP(int a) {
        this.totCP = a;
    }

    public void setCP(int a) {
        this.cp = a;
    }

    public int getTotalCP() {
        return totCP;
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public void resetCP() {
        this.cp = 0;
        this.totCP = 0;
        this.monsterCarnival = null;
    }

    public MonsterCarnival getMonsterCarnival() {
        return monsterCarnival;
    }

    public void setMonsterCarnival(MonsterCarnival monsterCarnival) {
        this.monsterCarnival = monsterCarnival;
    }

    public AriantColiseum getAriantColiseum() {
        return ariantColiseum;
    }

    public void setAriantColiseum(AriantColiseum ariantColiseum) {
        this.ariantColiseum = ariantColiseum;
    }

    public MonsterCarnivalParty getMonsterCarnivalParty() {
        return this.monsterCarnivalParty;
    }

    public void setMonsterCarnivalParty(MonsterCarnivalParty mcp) {
        this.monsterCarnivalParty = mcp;
    }

    public boolean isChallenged() {
        return challenged;
    }

    public void setChallenged(boolean challenged) {
        this.challenged = challenged;
    }

    public void gainAriantPoints(int points) {
        this.ariantPoints += points;
    }

    public int getAriantPoints() {
        return this.ariantPoints;
    }

    public boolean isChasing() {
        return chasing;
    }

    public void setChasing(boolean chasing) {
        this.chasing = chasing;
    }
}
