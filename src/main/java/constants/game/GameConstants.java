package constants.game;

import client.Disease;
import client.Job;
import config.YamlConfig;
import constants.id.MapId;
import constants.skills.Aran;
import provider.*;
import provider.wz.WZFiles;
import server.maps.FieldLimit;
import server.maps.MapleMap;
import server.quest.Quest;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/*
 * @author kevintjuh93
 * @author Ronan
 */
public class GameConstants {
    public static String[] WORLD_NAMES = {"Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia", "Yellonde", "Demethos", "Galicia", "El Nido", "Zenith", "Arcenia", "Kastia", "Judis", "Plana", "Kalluna", "Stius", "Croa", "Medere"};
    public static final String[] stats = {"tuc", "reqLevel", "reqJob", "reqSTR", "reqDEX", "reqINT", "reqLUK", "reqPOP", "cash", "cursed", "success", "setItemID", "equipTradeBlock", "durability", "randOption", "randStat", "masterLevel", "reqSkillLevel", "elemDefault", "incRMAS", "incRMAF", "incRMAI", "incRMAL", "canLevel", "skill", "charmEXP"};
    public static final int[] CASH_DATA = new int[]{50200004, 50200069, 50200117, 50100008, 50000047};

    // Ronan's rates upgrade system
    private static final int[] DROP_RATE_GAIN =  {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};    //old {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
    private static final int[] MESO_RATE_GAIN = {4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};     //old {1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78, 91, 105};
    private static final int[] EXP_RATE_GAIN = {1, 1, 2, 2, 2, 3, 3, 4, 4, 5, 5, 5, 5, 5};    //fibonacci :3 {1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610}

    private static final int[] jobUpgradeBlob = {1, 20, 60, 110, 190};
    private static final int[] jobUpgradeSpUp = {0, 1, 2, 3, 6};
    private final static Map<Integer, String> jobNames = new HashMap<>();
    private final static NumberFormat nfFormatter = new DecimalFormat("#,###,###,###");
    private final static NumberFormat nfParser = NumberFormat.getInstance(YamlConfig.config.server.USE_UNITPRICE_WITH_COMMA ? Locale.FRANCE : Locale.UK);

    public static final Disease[] CPQ_DISEASES = {Disease.SLOW, Disease.SEDUCE, Disease.STUN, Disease.POISON,
            Disease.SEAL, Disease.DARKNESS, Disease.WEAKEN, Disease.CURSE};

    public static final int MAX_FIELD_MOB_DAMAGE = getMaxObstacleMobDamageFromWz() * 2;

    public static int getPlayerBonusDropRate(int slot) {
        return (DROP_RATE_GAIN[slot]);
    }

    public static int getPlayerBonusMesoRate(int slot) {
        return (MESO_RATE_GAIN[slot]);
    }

    public static int getPlayerBonusExpRate(int slot) {
        return (EXP_RATE_GAIN[slot]);
    }

    // "goto" command for players
    public static final Map<String, Integer> GOTO_TOWNS = new HashMap<>() {{
        put("southperry", MapId.SOUTHPERRY);
        put("amherst", MapId.AMHERST);
        put("henesys", MapId.HENESYS);
        put("ellinia", MapId.ELLINIA);
        put("perion", MapId.PERION);
        put("kerning", MapId.KERNING_CITY);
        put("lith", MapId.LITH_HARBOUR);
        put("sleepywood", MapId.SLEEPYWOOD);
        put("florina", MapId.FLORINA_BEACH);
        put("nautilus", MapId.NAUTILUS_HARBOR);
        put("ereve", MapId.EREVE);
        put("rien", MapId.RIEN);
        put("orbis", MapId.ORBIS);
        put("happy", MapId.HAPPYVILLE);
        put("elnath", MapId.EL_NATH);
        put("ludi", MapId.LUDIBRIUM);
        put("aqua", MapId.AQUARIUM);
        put("leafre", MapId.LEAFRE);
        put("mulung", MapId.MU_LUNG);
        put("herb", MapId.HERB_TOWN);
        put("omega", MapId.OMEGA_SECTOR);
        put("kft", MapId.KOREAN_FOLK_TOWN);
        put("ellin", MapId.ELLIN_FOREST);
        put("nlc", MapId.NEW_LEAF_CITY);
        put("showa", MapId.SHOWA_TOWN);
        put("shrine", MapId.MUSHROOM_SHRINE);
        put("ariant", MapId.ARIANT);
        put("magatia", MapId.MAGATIA);
        put("singapore", MapId.SINGAPORE);
        put("quay", MapId.BOAT_QUAY_TOWN);
        put("kampung", MapId.KAMPUNG_VILLAGE);
        put("amoria", MapId.AMORIA);
        put("temple", MapId.TEMPLE_OF_TIME);
        put("square", MapId.KERNING_SQUARE);
        put("neo", MapId.NEO_CITY);
        put("mushking", MapId.MUSHROOM_KINGDOM);
    }};

    // "goto" command for only-GMs
    public static final Map<String, Integer> GOTO_AREAS = new HashMap<>() {{
        put("gmmap", MapId.GM_MAP);
        put("excavation", MapId.EXCAVATION_SITE);
        put("mushmom", MapId.SOMEONE_ELSES_HOUSE);
        put("griffey", MapId.GRIFFEY_FOREST);
        put("manon", MapId.MANONS_FOREST);
        put("horseman", MapId.HOLLOWED_GROUND);
        put("balrog", MapId.CURSED_SANCTUARY);
        put("zakum", MapId.DOOR_TO_ZAKUM);
        put("papu", MapId.ORIGIN_OF_CLOCKTOWER);
        put("guild", MapId.GUILD_HQ);
        put("skelegon", MapId.DRAGON_NEST_LEFT_BEHIND);
        put("hpq", MapId.HENESYS_PARK);
        put("pianus", MapId.CAVE_OF_PIANUS);
        put("horntail", MapId.ENTRANCE_TO_HORNTAILS_CAVE);
        put("pinkbean", MapId.FORGOTTEN_TWILIGHT);
        put("keep", MapId.CRIMSONWOOD_KEEP);
        put("dojo", MapId.MU_LUNG_DOJO_HALL);
        put("bosspq", MapId.EXCLUSIVE_TRAINING_CENTER);
        put("fm", MapId.FM_ENTRANCE);
    }};

    public static final List<String> GAME_SONGS = new ArrayList<>(170) {{
        add("Jukebox/Congratulation");
        add("Bgm00/SleepyWood");
        add("Bgm00/FloralLife");
        add("Bgm00/GoPicnic");
        add("Bgm00/Nightmare");
        add("Bgm00/RestNPeace");
        add("Bgm01/AncientMove");
        add("Bgm01/MoonlightShadow");
        add("Bgm01/WhereTheBarlogFrom");
        add("Bgm01/CavaBien");
        add("Bgm01/HighlandStar");
        add("Bgm01/BadGuys");
        add("Bgm02/MissingYou");
        add("Bgm02/WhenTheMorningComes");
        add("Bgm02/EvilEyes");
        add("Bgm02/JungleBook");
        add("Bgm02/AboveTheTreetops");
        add("Bgm03/Subway");
        add("Bgm03/Elfwood");
        add("Bgm03/BlueSky");
        add("Bgm03/Beachway");
        add("Bgm03/SnowyVillage");
        add("Bgm04/PlayWithMe");
        add("Bgm04/WhiteChristmas");
        add("Bgm04/UponTheSky");
        add("Bgm04/ArabPirate");
        add("Bgm04/Shinin'Harbor");
        add("Bgm04/WarmRegard");
        add("Bgm05/WolfWood");
        add("Bgm05/DownToTheCave");
        add("Bgm05/AbandonedMine");
        add("Bgm05/MineQuest");
        add("Bgm05/HellGate");
        add("Bgm06/FinalFight");
        add("Bgm06/WelcomeToTheHell");
        add("Bgm06/ComeWithMe");
        add("Bgm06/FlyingInABlueDream");
        add("Bgm06/FantasticThinking");
        add("Bgm07/WaltzForWork");
        add("Bgm07/WhereverYouAre");
        add("Bgm07/FunnyTimeMaker");
        add("Bgm07/HighEnough");
        add("Bgm07/Fantasia");
        add("Bgm08/LetsMarch");
        add("Bgm08/ForTheGlory");
        add("Bgm08/FindingForest");
        add("Bgm08/LetsHuntAliens");
        add("Bgm08/PlotOfPixie");
        add("Bgm09/DarkShadow");
        add("Bgm09/TheyMenacingYou");
        add("Bgm09/FairyTale");
        add("Bgm09/FairyTalediffvers");
        add("Bgm09/TimeAttack");
        add("Bgm10/Timeless");
        add("Bgm10/TimelessB");
        add("Bgm10/BizarreTales");
        add("Bgm10/TheWayGrotesque");
        add("Bgm10/Eregos");
        add("Bgm11/BlueWorld");
        add("Bgm11/Aquarium");
        add("Bgm11/ShiningSea");
        add("Bgm11/DownTown");
        add("Bgm11/DarkMountain");
        add("Bgm12/AquaCave");
        add("Bgm12/DeepSee");
        add("Bgm12/WaterWay");
        add("Bgm12/AcientRemain");
        add("Bgm12/RuinCastle");
        add("Bgm12/Dispute");
        add("Bgm13/CokeTown");
        add("Bgm13/Leafre");
        add("Bgm13/Minar'sDream");
        add("Bgm13/AcientForest");
        add("Bgm13/TowerOfGoddess");
        add("Bgm14/DragonLoad");
        add("Bgm14/HonTale");
        add("Bgm14/CaveOfHontale");
        add("Bgm14/DragonNest");
        add("Bgm14/Ariant");
        add("Bgm14/HotDesert");
        add("Bgm15/MureungHill");
        add("Bgm15/MureungForest");
        add("Bgm15/WhiteHerb");
        add("Bgm15/Pirate");
        add("Bgm15/SunsetDesert");
        add("Bgm16/Duskofgod");
        add("Bgm16/FightingPinkBeen");
        add("Bgm16/Forgetfulness");
        add("Bgm16/Remembrance");
        add("Bgm16/Repentance");
        add("Bgm16/TimeTemple");
        add("Bgm17/MureungSchool1");
        add("Bgm17/MureungSchool2");
        add("Bgm17/MureungSchool3");
        add("Bgm17/MureungSchool4");
        add("Bgm18/BlackWing");
        add("Bgm18/DrillHall");
        add("Bgm18/QueensGarden");
        add("Bgm18/RaindropFlower");
        add("Bgm18/WolfAndSheep");
        add("Bgm19/BambooGym");
        add("Bgm19/CrystalCave");
        add("Bgm19/MushCatle");
        add("Bgm19/RienVillage");
        add("Bgm19/SnowDrop");
        add("Bgm20/GhostShip");
        add("Bgm20/NetsPiramid");
        add("Bgm20/UnderSubway");
        add("Bgm21/2021year");
        add("Bgm21/2099year");
        add("Bgm21/2215year");
        add("Bgm21/2230year");
        add("Bgm21/2503year");
        add("Bgm21/KerningSquare");
        add("Bgm21/KerningSquareField");
        add("Bgm21/KerningSquareSubway");
        add("Bgm21/TeraForest");
        add("BgmEvent/FunnyRabbit");
        add("BgmEvent/FunnyRabbitFaster");
        add("BgmEvent/wedding");
        add("BgmEvent/weddingDance");
        add("BgmEvent/wichTower");
        add("BgmGL/amoria");
        add("BgmGL/Amorianchallenge");
        add("BgmGL/chapel");
        add("BgmGL/cathedral");
        add("BgmGL/Courtyard");
        add("BgmGL/CrimsonwoodKeep");
        add("BgmGL/CrimsonwoodKeepInterior");
        add("BgmGL/GrandmastersGauntlet");
        add("BgmGL/HauntedHouse");
        add("BgmGL/NLChunt");
        add("BgmGL/NLCtown");
        add("BgmGL/NLCupbeat");
        add("BgmGL/PartyQuestGL");
        add("BgmGL/PhantomForest");
        add("BgmJp/Feeling");
        add("BgmJp/BizarreForest");
        add("BgmJp/Hana");
        add("BgmJp/Yume");
        add("BgmJp/Bathroom");
        add("BgmJp/BattleField");
        add("BgmJp/FirstStepMaster");
        add("BgmMY/Highland");
        add("BgmMY/KualaLumpur");
        add("BgmSG/BoatQuay_field");
        add("BgmSG/BoatQuay_town");
        add("BgmSG/CBD_field");
        add("BgmSG/CBD_town");
        add("BgmSG/Ghostship");
        add("BgmUI/ShopBgm");
        add("BgmUI/Title");
    }};

    // MapleStory default keyset
    private static final int[] DEFAULT_KEY = {18, 65, 2, 23, 3, 4, 5, 6, 16, 17, 19, 25, 26, 27, 31, 34, 35, 37, 38, 40, 43, 44, 45, 46, 50, 56, 59, 60, 61, 62, 63, 64, 57, 48, 29, 7, 24, 33, 41, 39};
    private static final int[] DEFAULT_TYPE = {4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 5, 6, 6, 6, 6, 6, 6, 5, 4, 5, 4, 4, 4, 4, 4};
    private static final int[] DEFAULT_ACTION = {0, 106, 10, 1, 12, 13, 18, 24, 8, 5, 4, 19, 14, 15, 2, 17, 11, 3, 20, 16, 9, 50, 51, 6, 7, 53, 100, 101, 102, 103, 104, 105, 54, 22, 52, 21, 25, 26, 23, 27};

    // HeavenMS custom keyset
    private static final int[] CUSTOM_KEY = {2, 3, 4, 5, 31, 56, 59, 32, 42, 6, 17, 29, 30, 41, 50, 60, 61, 62, 63, 64, 65, 16, 7, 9, 13, 8};
    private static final int[] CUSTOM_TYPE = {4, 4, 4, 4, 5, 5, 6, 5, 5, 4, 4, 4, 5, 4, 4, 6, 6, 6, 6, 6, 6, 4, 4, 4, 4, 4};
    private static final int[] CUSTOM_ACTION = {1, 0, 3, 2, 53, 54, 100, 52, 51, 19, 5, 9, 50, 7, 22, 101, 102, 103, 104, 105, 106, 8, 17, 26, 20, 4};

    public static int[] getCustomKey(boolean customKeyset) {
        return (customKeyset ? CUSTOM_KEY : DEFAULT_KEY);
    }

    public static int[] getCustomType(boolean customKeyset) {
        return (customKeyset ? CUSTOM_TYPE : DEFAULT_TYPE);
    }

    public static int[] getCustomAction(boolean customKeyset) {
        return (customKeyset ? CUSTOM_ACTION : DEFAULT_ACTION);
    }

    private static final int[] mobHpVal = {0, 15, 20, 25, 35, 50, 65, 80, 95, 110, 125, 150, 175, 200, 225, 250, 275, 300, 325, 350,
            375, 405, 435, 465, 495, 525, 580, 650, 720, 790, 900, 990, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800,
            1900, 2000, 2100, 2200, 2300, 2400, 2520, 2640, 2760, 2880, 3000, 3200, 3400, 3600, 3800, 4000, 4300, 4600, 4900, 5200,
            5500, 5900, 6300, 6700, 7100, 7500, 8000, 8500, 9000, 9500, 10000, 11000, 12000, 13000, 14000, 15000, 17000, 19000, 21000, 23000,
            25000, 27000, 29000, 31000, 33000, 35000, 37000, 39000, 41000, 43000, 45000, 47000, 49000, 51000, 53000, 55000, 57000, 59000, 61000, 63000,
            65000, 67000, 69000, 71000, 73000, 75000, 77000, 79000, 81000, 83000, 85000, 89000, 91000, 93000, 95000, 97000, 99000, 101000, 103000,
            105000, 107000, 109000, 111000, 113000, 115000, 118000, 120000, 125000, 130000, 135000, 140000, 145000, 150000, 155000, 160000, 165000, 170000, 175000, 180000,
            185000, 190000, 195000, 200000, 205000, 210000, 215000, 220000, 225000, 230000, 235000, 240000, 250000, 260000, 270000, 280000, 290000, 300000, 310000, 320000,
            330000, 340000, 350000, 360000, 370000, 380000, 390000, 400000, 410000, 420000, 430000, 440000, 450000, 460000, 470000, 480000, 490000, 500000, 510000, 520000,
            530000, 550000, 570000, 590000, 610000, 630000, 650000, 670000, 690000, 710000, 730000, 750000, 770000, 790000, 810000, 830000, 850000, 870000, 890000, 910000};

    public static String getJobName(int jobid) {
        String name = jobNames.get(jobid);

        if (name == null) {
            Job job = Job.getById(jobid);

            if (job != null) {
                name = job.name().toLowerCase();
                name = name.replaceAll("[*0-9]", "");
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
            } else {
                name = "";
            }

            jobNames.put(jobid, name);
        }

        return name;
    }

    public static int getJobUpgradeLevelRange(int jobbranch) {
        return jobUpgradeBlob[jobbranch];
    }

    public static int getChangeJobSpUpgrade(int jobbranch) {
        return jobUpgradeSpUp[jobbranch];
    }

    public static boolean isHallOfFameMap(int mapid) {
        switch (mapid) {
            case MapId.HALL_OF_WARRIORS:     // warrior
            case MapId.HALL_OF_MAGICIANS:     // magician
            case MapId.HALL_OF_BOWMEN:     // bowman
            case MapId.HALL_OF_THIEVES:     // thief
            case MapId.NAUTILUS_TRAINING_ROOM:     // pirate
            case MapId.KNIGHTS_CHAMBER:     // cygnus
            case MapId.KNIGHTS_CHAMBER_LARGE:     // other cygnus
            case MapId.KNIGHTS_CHAMBER_2:     // cygnus 2nd floor
            case MapId.KNIGHTS_CHAMBER_3:     // cygnus 3rd floor (beginners)
            case MapId.PALACE_OF_THE_MASTER:     // aran
                return true;

            default:
                return false;
        }
    }

    public static boolean isPodiumHallOfFameMap(int mapid) {
        switch (mapid) {
            case MapId.HALL_OF_WARRIORS:
            case MapId.HALL_OF_MAGICIANS:     // magician
            case MapId.HALL_OF_BOWMEN:     // bowman
            case MapId.HALL_OF_THIEVES:     // thief
            case MapId.NAUTILUS_TRAINING_ROOM:     // pirate
                return true;

            default:
                return false;
        }
    }

    public static byte getHallOfFameBranch(Job job, int mapid) {
        if (!isHallOfFameMap(mapid)) {
            return (byte) (26 + 4 * (mapid / 100000000));   // custom, 400 pnpcs available per continent
        }

        if (job.isA(Job.WARRIOR)) {
            return 10;
        } else if (job.isA(Job.MAGICIAN)) {
            return 11;
        } else if (job.isA(Job.BOWMAN)) {
            return 12;
        } else if (job.isA(Job.THIEF)) {
            return 13;
        } else if (job.isA(Job.PIRATE)) {
            return 14;
        } else if (job.isA(Job.DAWNWARRIOR1)) {
            return 15;
        } else if (job.isA(Job.BLAZEWIZARD1)) {
            return 16;
        } else if (job.isA(Job.WINDARCHER1)) {
            return 17;
        } else if (job.isA(Job.NIGHTWALKER1)) {
            return 18;
        } else if (job.isA(Job.THUNDERBREAKER1)) {
            return 19;
        } else if (job.isA(Job.ARAN1)) {
            return 20;
        } else if (job.isA(Job.EVAN1)) {
            return 21;
        } else if (job.isA(Job.BEGINNER)) {
            return 22;
        } else if (job.isA(Job.NOBLESSE)) {
            return 23;
        } else if (job.isA(Job.LEGEND)) {
            return 24;
        } else {
            return 25;
        }
    }

    public static int getOverallJobRankByScriptId(int scriptId) {
        int branch = (scriptId / 100) % 100;

        if (branch < 26) {
            return (scriptId % 100) + 1;
        } else {
            return ((scriptId - 2600) % 400) + 1;
        }
    }

    public static boolean canPnpcBranchUseScriptId(byte branch, int scriptId) {
        scriptId /= 100;
        scriptId %= 100;

        if (branch < 26) {
            return branch == scriptId;
        } else {
            return scriptId >= branch && scriptId < branch + 4;
        }
    }

    public static int getHallOfFameMapid(Job job) {
        int jobid = job.getId();

        if (isCygnus(jobid)) {
            return MapId.KNIGHTS_CHAMBER;
        } else if (isAran(jobid)) {
            return MapId.PALACE_OF_THE_MASTER;
        } else {
            if (job.isA(Job.WARRIOR)) {
                return MapId.HALL_OF_WARRIORS;
            } else if (job.isA(Job.MAGICIAN)) {
                return MapId.HALL_OF_MAGICIANS;
            } else if (job.isA(Job.BOWMAN)) {
                return MapId.HALL_OF_BOWMEN;
            } else if (job.isA(Job.THIEF)) {
                return MapId.HALL_OF_THIEVES;
            } else if (job.isA(Job.PIRATE)) {
                return MapId.NAUTILUS_TRAINING_ROOM;
            } else {
                return MapId.KNIGHTS_CHAMBER_2;   // beginner explorers are allotted with the Cygnus, available map lul
            }
        }
    }

    public static int getJobBranch(Job job) {
        int jobid = job.getId();

        if (jobid % 1000 == 0) {
            return 0;
        } else if (jobid % 100 == 0) {
            return 1;
        } else {
            return 2 + (jobid % 10);
        }
    }

    public static int getJobMaxLevel(Job job) {
        int jobBranch = getJobBranch(job);

        switch (jobBranch) {
            case 0:
                return 10;   // beginner

            case 1:
                return 30;   // 1st job

            case 2:
                return 70;   // 2nd job

            case 3:
                return 120;   // 3rd job

            default:
                return (job.getId() / 1000 == 1) ? 120 : 255;   // 4th job: cygnus is 120, rest is 200
        }
    }

    public static int getSkillBook(final int job) {
        if (job >= 2210 && job <= 2218) {
            return job - 2209;
        }
        return 0;
    }

    public static boolean isAranSkills(final int skill) {
        return Aran.FULL_SWING == skill || Aran.OVER_SWING == skill || Aran.COMBO_TEMPEST == skill || Aran.COMBO_FENRIR == skill || Aran.COMBO_DRAIN == skill
                || Aran.HIDDEN_FULL_DOUBLE == skill || Aran.HIDDEN_FULL_TRIPLE == skill || Aran.HIDDEN_OVER_DOUBLE == skill || Aran.HIDDEN_OVER_TRIPLE == skill
                || Aran.COMBO_SMASH == skill || Aran.DOUBLE_SWING == skill || Aran.TRIPLE_SWING == skill;
    }

    public static boolean isHiddenSkills(final int skill) {
        return Aran.HIDDEN_FULL_DOUBLE == skill || Aran.HIDDEN_FULL_TRIPLE == skill || Aran.HIDDEN_OVER_DOUBLE == skill || Aran.HIDDEN_OVER_TRIPLE == skill;
    }

    public static boolean isCygnus(final int job) {
        return job / 1000 == 1;
    }

    public static boolean isAran(final int job) {
        return job == 2000 || (job >= 2100 && job <= 2112);
    }

    private static boolean isInBranchJobTree(int skillJobId, int jobId, int branchType) {
        int branch = (int) (Math.pow(10, branchType));

        int skillBranch = (skillJobId / branch) * branch;
        int jobBranch = (jobId / branch) * branch;

        return skillBranch == jobBranch;
    }

    private static boolean hasDivergedBranchJobTree(int skillJobId, int jobId, int branchType) {
        int branch = (int) (Math.pow(10, branchType));

        int skillBranch = skillJobId / branch;
        int jobBranch = jobId / branch;

        return skillBranch != jobBranch && skillBranch % 10 != 0;
    }

    public static boolean isInJobTree(int skillId, int jobId) {
        int skillJob = skillId / 10000;

        if (!isInBranchJobTree(skillJob, jobId, 0)) {
            for (int i = 1; i <= 3; i++) {
                if (hasDivergedBranchJobTree(skillJob, jobId, i)) {
                    return false;
                }
                if (isInBranchJobTree(skillJob, jobId, i)) {
                    return (skillJob <= jobId);
                }
            }
        } else {
            return (skillJob <= jobId);
        }

        return false;
    }

    public static boolean isPqSkill(final int skill) {
        return (skill >= 20000014 && skill <= 20000018) || skill == 10000013 || skill == 20001013 || (skill % 10000000 >= 1009 && skill % 10000000 <= 1011) || skill % 10000000 == 1020;
    }

    public static boolean bannedBindSkills(final int skill) {
        return isAranSkills(skill) || isPqSkill(skill);
    }

    public static boolean isGMSkills(final int skill) {
        return skill >= 9001000 && skill <= 9101008 || skill >= 8001000 && skill <= 8001001;
    }

    public static boolean isFreeMarketRoom(int mapid) {
        return mapid / 1000000 == 910 && mapid > MapId.FM_ENTRANCE; // FM rooms subset, thanks to shavitush (shavit)
    }

    public static boolean isMerchantLocked(MapleMap map) {
        if (FieldLimit.CANNOTMIGRATE.check(map.getFieldLimit())) {   // maps that cannot access cash shop cannot access merchants too (except FM rooms).
            return true;
        }

        return map.getId() == MapId.FM_ENTRANCE;
    }

    public static boolean isDojoBossArea(int mapid) {
        return MapId.isDojo(mapid) && (((mapid / 100) % 100) % 6) > 0;
    }

    public static boolean isAriantColiseumLobby(int mapid) {
        int mapbranch = mapid / 1000;
        return mapbranch == 980010 && mapid % 10 == 0;
    }

    public static boolean isAriantColiseumArena(int mapid) {
        int mapbranch = mapid / 1000;
        return mapbranch == 980010 && mapid % 10 == 1;
    }

    public static boolean isPqSkillMap(int mapid) {
        return MapId.isDojo(mapid) || MapId.isNettsPyramid(mapid);
    }

    public static boolean isFinisherSkill(int skillId) {
        return skillId > 1111002 && skillId < 1111007 || skillId == 11111002 || skillId == 11111003;
    }

    public static boolean isMedalQuest(short questid) {
        return Quest.getInstance(questid).getMedalRequirement() != -1;
    }

    public static boolean hasSPTable(Job job) {
        switch (job) {
            case EVAN:
            case EVAN1:
            case EVAN2:
            case EVAN3:
            case EVAN4:
            case EVAN5:
            case EVAN6:
            case EVAN7:
            case EVAN8:
            case EVAN9:
            case EVAN10:
                return true;
            default:
                return false;
        }
    }

    public static int getMonsterHP(final int level) {
        if (level < 0 || level >= mobHpVal.length) {
            return Integer.MAX_VALUE;
        }
        return mobHpVal[level];
    }

    public static String ordinal(int i) {
        String[] sufixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";

            default:
                return i + sufixes[i % 10];
        }
    }

    public synchronized static String numberWithCommas(int i) {
        if (!YamlConfig.config.server.USE_DISPLAY_NUMBERS_WITH_COMMA) {
            return nfFormatter.format(i);   // will display number on whatever locale is currently assigned on NumberFormat
        } else {
            return NumberFormat.getNumberInstance(Locale.UK).format(i);
        }
    }

    public synchronized static Number parseNumber(String value) {
        try {
            return nfParser.parse(value);
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

    private static int getMaxObstacleMobDamageFromWz() {
        DataProvider mapSource = DataProviderFactory.getDataProvider(WZFiles.MAP);
        int maxMobDmg = 0;

        DataDirectoryEntry root = mapSource.getRoot();
        for (DataDirectoryEntry objData : root.getSubdirectories()) {
            if (!objData.getName().contentEquals("Obj")) {
                continue;
            }

            for (DataFileEntry obj : objData.getFiles()) {
                for (Data l0 : mapSource.getData(objData.getName() + "/" + obj.getName()).getChildren()) {
                    for (Data l1 : l0.getChildren()) {
                        for (Data l2 : l1.getChildren()) {
                            int objDmg = DataTool.getIntConvert("s1/mobdamage", l2, 0);
                            if (maxMobDmg < objDmg) {
                                maxMobDmg = objDmg;
                            }
                        }
                    }
                }
            }
        }

        return maxMobDmg;
    }

}
