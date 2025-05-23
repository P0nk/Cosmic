package constants.id;

public class MapId {
    // Special
    public static final int NONE = 999999999;
    public static final int GM_MAP = 180000000;
    public static final int JAIL = 300000012; // "Cellar: Camp Conference Room"
    public static final int DEVELOPERS_HQ = 777777777;

    // Misc
    public static final int ORBIS_TOWER_BOTTOM = 200082300;
    public static final int INTERNET_CAFE = 193000000;
    public static final int CRIMSONWOOD_VALLEY_1 = 610020000;
    public static final int CRIMSONWOOD_VALLEY_2 = 610020001;
    public static final int HENESYS_PQ = 910010000;
    public static final int ORIGIN_OF_CLOCKTOWER = 220080001;
    public static final int CAVE_OF_PIANUS = 230040420;
    public static final int GUILD_HQ = 200000301;
    public static final int FM_ENTRANCE = 910000000;

    // Beginner
    public static final int MUSHROOM_TOWN = 10000;

    // Town
    public static final int SOUTHPERRY = 2000000;
    public static final int AMHERST = 1000000;
    public static final int HENESYS = 100000000;
    public static final int ELLINIA = 101000000;
    public static final int PERION = 102000000;
    public static final int KERNING_CITY = 103000000;
    public static final int LITH_HARBOUR = 104000000;
    public static final int SLEEPYWOOD = 105040300;
    public static final int MUSHROOM_KINGDOM = 106020000;
    public static final int FLORINA_BEACH = 110000000;
    public static final int EREVE = 130000000;
    public static final int KERNING_SQUARE = 103040000;
    public static final int RIEN = 140000000;
    public static final int ORBIS = 200000000;
    public static final int EL_NATH = 211000000;
    public static final int LUDIBRIUM = 220000000;
    public static final int AQUARIUM = 230000000;
    public static final int LEAFRE = 240000000;
    public static final int NEO_CITY = 240070000;
    public static final int MU_LUNG = 250000000;
    public static final int HERB_TOWN = 251000000;
    public static final int OMEGA_SECTOR = 221000000;
    public static final int KOREAN_FOLK_TOWN = 222000000;
    public static final int ARIANT = 260000000;
    public static final int MAGATIA = 261000000;
    public static final int TEMPLE_OF_TIME = 270000100;
    public static final int ELLIN_FOREST = 300000000;
    public static final int SINGAPORE = 540000000;
    public static final int BOAT_QUAY_TOWN = 541000000;
    public static final int KAMPUNG_VILLAGE = 551000000;
    public static final int NEW_LEAF_CITY = 600000000;
    public static final int MUSHROOM_SHRINE = 800000000;
    public static final int SHOWA_TOWN = 801000000;
    public static final int NAUTILUS_HARBOR = 120000000;
    public static final int HAPPYVILLE = 209000000;

    public static final int SHOWA_SPA_M = 809000101;
    public static final int SHOWA_SPA_F = 809000201;

    private static final int MAPLE_ISLAND_MIN = 0;
    private static final int MAPLE_ISLAND_MAX = 2000001;

    public static boolean isMapleIsland(int mapId) {
        return mapId >= MAPLE_ISLAND_MIN && mapId <= MAPLE_ISLAND_MAX;
    }

    // Travel
    // There are 10 of each of these travel maps in the files
    public static final int FROM_LITH_TO_RIEN = 200090060;
    public static final int FROM_RIEN_TO_LITH = 200090070;
    public static final int DANGEROUS_FOREST = 140020300; // Rien docks
    public static final int FROM_ELLINIA_TO_EREVE = 200090030;
    public static final int SKY_FERRY = 130000210; // Ereve platform
    public static final int FROM_EREVE_TO_ELLINIA = 200090031;
    public static final int ELLINIA_SKY_FERRY = 101000400;
    public static final int FROM_EREVE_TO_ORBIS = 200090021;
    public static final int ORBIS_STATION = 200000161;
    public static final int FROM_ORBIS_TO_EREVE = 200090020;

    // Aran
    public static final int ARAN_TUTORIAL_START = 914000000;
    public static final int ARAN_TUTORIAL_MAX = 914000500;
    public static final int ARAN_INTRO = 140090000;
    private static final int BURNING_FOREST_1 = 914000200;
    private static final int BURNING_FOREST_2 = 914000210;
    private static final int BURNING_FOREST_3 = 914000220;

    // Aran tutorial
    public static boolean isGodlyStatMap(int mapId) {
        return mapId == BURNING_FOREST_1 || mapId == BURNING_FOREST_2 || mapId == BURNING_FOREST_3;
    }

    // Aran intro
    public static final int ARAN_TUTO_1 = 914090010;
    public static final int ARAN_TUTO_2 = 914090011;
    public static final int ARAN_TUTO_3 = 914090012;
    public static final int ARAN_TUTO_4 = 914090013;
    public static final int ARAN_POLEARM = 914090100;
    public static final int ARAN_MAHA = 914090200; // Black screen when warped to

    // Starting map
    public static final int STARTING_MAP_NOBLESSE = 130030000;

    // Cygnus intro
    // These are the actual maps
    private static final int CYGNUS_INTRO_LOCATION_MIN = 913040000;
    private static final int CYGNUS_INTRO_LOCATION_MAX = 913040006;

    public static boolean isCygnusIntro(int mapId) {
        return mapId >= CYGNUS_INTRO_LOCATION_MIN && mapId <= CYGNUS_INTRO_LOCATION_MAX;
    }

    // Cygnus intro video
    public static final int CYGNUS_INTRO_LEAD = 913040100;
    public static final int CYGNUS_INTRO_WARRIOR = 913040101;
    public static final int CYGNUS_INTRO_BOWMAN = 913040102;
    public static final int CYGNUS_INTRO_MAGE = 913040103;
    public static final int CYGNUS_INTRO_PIRATE = 913040104;
    public static final int CYGNUS_INTRO_THIEF = 913040105;
    public static final int CYGNUS_INTRO_CONCLUSION = 913040106;

    // Event
    public static final int EVENT_COCONUT_HARVEST = 109080000;
    public static final int EVENT_OX_QUIZ = 109020001;
    public static final int EVENT_PHYSICAL_FITNESS = 109040000;
    public static final int EVENT_OLA_OLA_0 = 109030001;
    public static final int EVENT_OLA_OLA_1 = 109030101;
    public static final int EVENT_OLA_OLA_2 = 109030201;
    public static final int EVENT_OLA_OLA_3 = 109030301;
    public static final int EVENT_OLA_OLA_4 = 109030401;
    public static final int EVENT_SNOWBALL = 109060000;
    public static final int EVENT_FIND_THE_JEWEL = 109010000;
    public static final int FITNESS_EVENT_LAST = 109040004;
    public static final int OLA_EVENT_LAST_1 = 109030003;
    public static final int OLA_EVENT_LAST_2 = 109030103;
    public static final int WITCH_TOWER_ENTRANCE = 980040000;
    public static final int EVENT_WINNER = 109050000;
    public static final int EVENT_EXIT = 109050001;
    public static final int EVENT_SNOWBALL_ENTRANCE = 109060001;

    private static final int PHYSICAL_FITNESS_MIN = EVENT_PHYSICAL_FITNESS;
    private static final int PHYSICAL_FITNESS_MAX = FITNESS_EVENT_LAST;

    public static boolean isPhysicalFitness(int mapId) {
        return mapId >= PHYSICAL_FITNESS_MIN && mapId <= PHYSICAL_FITNESS_MAX;
    }

    private static final int OLA_OLA_MIN = EVENT_OLA_OLA_0;
    private static final int OLA_OLA_MAX = 109030403; // OLA_OLA_4 level 3

    public static boolean isOlaOla(int mapId) {
        return mapId >= OLA_OLA_MIN && mapId <= OLA_OLA_MAX;
    }

    // Self lootable maps
    private static final int HAPPYVILLE_TREE_MIN = 209000001;
    private static final int HAPPYVILLE_TREE_MAX = 209000015;
    private static final int GPQ_FOUNTAIN_MIN = 990000500;
    private static final int GPQ_FOUNTAIN_MAX = 990000502;

    public static boolean isSelfLootableOnly(int mapId) {
        return (mapId >= HAPPYVILLE_TREE_MIN && mapId <= HAPPYVILLE_TREE_MAX) ||
                (mapId >= GPQ_FOUNTAIN_MIN && mapId <= GPQ_FOUNTAIN_MAX);
    }

    // Dojo
    public static final int DOJO_SOLO_BASE = 925020000;
    public static final int DOJO_PARTY_BASE = 925030000;
    public static final int DOJO_EXIT = 925020002;
    private static final int DOJO_MIN = DOJO_SOLO_BASE;
    private static final int DOJO_MAX = 925033804;
    private static final int DOJO_PARTY_MIN = 925030100;
    public static final int DOJO_PARTY_MAX = DOJO_MAX;

    public static boolean isDojo(int mapId) {
        return mapId >= DOJO_MIN && mapId <= DOJO_MAX;
    }

    public static boolean isPartyDojo(int mapId) {
        return mapId >= DOJO_PARTY_MIN && mapId <= DOJO_PARTY_MAX;
    }

    // Mini dungeon
    public static final int ANT_TUNNEL_2 = 105050100;
    public static final int CAVE_OF_MUSHROOMS_BASE = 105050101;
    public static final int ZAKUM_ALTAR = 280030100;
    public static final int SLEEPY_DUNGEON_4 = 105040304;
    public static final int GOLEMS_CASTLE_RUINS_BASE = 105040320;
    public static final int SAHEL_2 = 260020600;
    public static final int HILL_OF_SANDSTORMS_BASE = 260020630;
    public static final int RAIN_FOREST_EAST_OF_HENESYS = 100020000;
    public static final int HENESYS_PIG_FARM_BASE = 100020100;
    public static final int COLD_CRADLE = 105090311;
    public static final int DRAKES_BLUE_CAVE_BASE = 105090320;
    public static final int EOS_TOWER_76TH_TO_90TH_FLOOR = 221023400;
    public static final int DRUMMER_BUNNYS_LAIR_BASE = 221023401;
    public static final int BATTLEFIELD_OF_FIRE_AND_WATER = 240020500;
    public static final int ROUND_TABLE_OF_KENTAURUS_BASE = 240020512;
    public static final int RESTORING_MEMORY_BASE = 240040800;
    public static final int DESTROYED_DRAGON_NEST = 240040520;
    public static final int NEWT_SECURED_ZONE_BASE = 240040900;
    public static final int RED_NOSE_PIRATE_DEN_2 = 251010402;
    public static final int PILLAGE_OF_TREASURE_ISLAND_BASE = 251010410;
    public static final int LAB_AREA_C1 = 261020300;
    public static final int CRITICAL_ERROR_BASE = 261020301;
    public static final int FANTASY_THEME_PARK_3 = 551030000;
    public static final int LONGEST_RIDE_ON_BYEBYE_STATION = 551030001;

    // Boss rush
    private static final int BOSS_RUSH_MIN = 970030100;
    private static final int BOSS_RUSH_MAX = 970042711;

    public static boolean isBossRush(int mapId) {
        return mapId >= BOSS_RUSH_MIN && mapId <= BOSS_RUSH_MAX;
    }

    // ARPQ
    public static final int ARPQ_LOBBY = 980010000;
    public static final int ARPQ_ARENA_1 = 980010101;
    public static final int ARPQ_ARENA_2 = 980010201;
    public static final int ARPQ_ARENA_3 = 980010301;
    public static final int ARPQ_KINGS_ROOM = 980010010;

    // Nett's pyramid
    public static final int NETTS_PYRAMID = 926010001;
    public static final int NETTS_PYRAMID_SOLO_BASE = 926010100;
    public static final int NETTS_PYRAMID_PARTY_BASE = 926020100;
    private static final int NETTS_PYRAMID_MIN = NETTS_PYRAMID_SOLO_BASE;
    private static final int NETTS_PYRAMID_MAX = 926023500;

    public static boolean isNettsPyramid(int mapId) {
        return mapId >= NETTS_PYRAMID_MIN && mapId <= NETTS_PYRAMID_MAX;
    }

    // Fishing
    private static final int ON_THE_WAY_TO_THE_HARBOR = 120010000;
    private static final int PIER_ON_THE_BEACH = 251000100;
    private static final int PEACEFUL_SHIP = 541010110;

    public static boolean isFishingArea(int mapId) {
        return mapId == ON_THE_WAY_TO_THE_HARBOR || mapId == PIER_ON_THE_BEACH || mapId == PEACEFUL_SHIP;
    }

    // Wedding
    public static final int AMORIA = 680000000;
    public static final int CHAPEL_WEDDING_ALTAR = 680000110;
    public static final int CATHEDRAL_WEDDING_ALTAR = 680000210;
    public static final int WEDDING_PHOTO = 680000300;
    public static final int WEDDING_EXIT = 680000500;

    // Statue
    public static final int HALL_OF_WARRIORS = 102000004; // Explorer
    public static final int HALL_OF_MAGICIANS = 101000004;
    public static final int HALL_OF_BOWMEN = 100000204;
    public static final int HALL_OF_THIEVES = 103000008;
    public static final int NAUTILUS_TRAINING_ROOM = 120000105;
    public static final int KNIGHTS_CHAMBER = 130000100; // Cygnus
    public static final int KNIGHTS_CHAMBER_2 = 130000110;
    public static final int KNIGHTS_CHAMBER_3 = 130000120;
    public static final int KNIGHTS_CHAMBER_LARGE = 130000101;
    public static final int PALACE_OF_THE_MASTER = 140010110; // Aran

    // gm-goto
    public static final int EXCAVATION_SITE = 990000000;
    public static final int SOMEONE_ELSES_HOUSE = 100000005;
    public static final int GRIFFEY_FOREST = 240020101;
    public static final int MANONS_FOREST = 240020401;
    public static final int HOLLOWED_GROUND = 682000001;
    public static final int CURSED_SANCTUARY = 105090900;
    public static final int DOOR_TO_ZAKUM = 211042300;
    public static final int DRAGON_NEST_LEFT_BEHIND = 240040511;
    public static final int HENESYS_PARK = 100000200;
    public static final int ENTRANCE_TO_HORNTAILS_CAVE = 240050400;
    public static final int FORGOTTEN_TWILIGHT = 270050000;
    public static final int CRIMSONWOOD_KEEP = 610020006;
    public static final int MU_LUNG_DOJO_HALL = 925020001;
    public static final int EXCLUSIVE_TRAINING_CENTER = 970030000;
}
