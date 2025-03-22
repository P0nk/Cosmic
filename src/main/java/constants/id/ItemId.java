package constants.id;

import java.util.stream.IntStream;

public class ItemId {
    // Misc
    public static final int PENDANT_OF_THE_SPIRIT = 1122017;
    public static final int HEART_SHAPED_CHOCOLATE = 5110000;
    public static final int HAPPY_BIRTHDAY = 2022153;
    public static final int FISHING_CHAIR = 3011000;
    public static final int MINI_GAME_BASE = 4080000;
    public static final int MATCH_CARDS = 4080100;
    public static final int MAGICAL_MITTEN = 1472063;
    public static final int RPS_CERTIFICATE_BASE = 4031332;
    public static final int GOLDEN_MAPLE_LEAF = 4000313;
    public static final int PERFECT_PITCH = 4310000;
    public static final int MAGIC_ROCK = 4006000;
    public static final int GOLDEN_CHICKEN_EFFECT = 4290000;
    public static final int BUMMER_EFFECT = 4290001;
    public static final int ARPQ_SHIELD = 2022269;
    public static final int ROARING_TIGER_MESSENGER = 5390006;

    public static boolean isExpIncrease(int itemId) {
        return itemId >= 2022450 && itemId <= 2022452;
    }

    public static boolean isRateCoupon(int itemId) {
        int itemType = itemId / 1000;
        return itemType == 5211 || itemType == 5360;
    }

    public static boolean isMonsterCard(int itemId) {
        int itemType = itemId / 10000;
        return itemType == 238;
    }

    public static boolean isPyramidBuff(int itemId) {
        return (itemId >= 2022585 && itemId <= 2022588) || (itemId >= 2022616 && itemId <= 2022617);
    }

    public static boolean isDojoBuff(int itemId) {
        return itemId >= 2022359 && itemId <= 2022421;
    }

    // Potion
    public static final int WHITE_POTION = 2000002;
    public static final int BLUE_POTION = 2000003;
    public static final int ORANGE_POTION = 2000001;
    public static final int MANA_ELIXIR = 2000006;

    // HP/MP recovery
    public static final int SORCERERS_POTION = 2022337;
    public static final int RUSSELLONS_PILLS = 2022198;

    // Environment
    public static final int RED_BEAN_PORRIDGE = 2022001;
    public static final int SOFT_WHITE_BUN = 2022186;
    public static final int AIR_BUBBLE = 2022040;

    // Chair
    public static final int RELAXER = 3010000;
    private static final int CHAIR_MIN = RELAXER;
    private static final int CHAIR_MAX = FISHING_CHAIR;

    public static boolean isChair(int itemId) {
        return itemId >= CHAIR_MIN && itemId <= CHAIR_MAX;
        // alt: return itemId / 10000 == 301;
    }

    // Throwing star
    public static final int SUBI_THROWING_STARS = 2070000;
    public static final int HWABI_THROWING_STARS = 2070007;
    private static final int ORANGE_THROWING_STARS = 2070013;
    public static final int BALANCED_FURY = 2070018;
    public static final int CRYSTAL_ILBI_THROWING_STARS = 2070016;

    public static IntStream allThrowingStarIds() {
        // excluding 2070015 - A Beginner Thief's Throwing Stars
        return IntStream.concat(
                IntStream.range(SUBI_THROWING_STARS, ORANGE_THROWING_STARS + 1),
                IntStream.of(CRYSTAL_ILBI_THROWING_STARS, BALANCED_FURY)
        );
    }

    // Bullet
    public static final int BULLET = 2330000;
    private static final int BULLET_MIN = BULLET;
    private static final int BULLET_MAX = 2330005;
    public static final int BLAZE_CAPSULE = 2331000;
    public static final int GLAZE_CAPSULE = 2332000;

    public static IntStream allBulletIds() {
        return IntStream.range(BULLET_MIN, BULLET_MAX + 1);
    }

    public static IntStream allBulletCapsuleIds() {
        return IntStream.of(BLAZE_CAPSULE, GLAZE_CAPSULE);
    }

    // Starter
    public static final int BEGINNERS_GUIDE = 4161001;
    public static final int LEGENDS_GUIDE = 4161048;
    public static final int NOBLESSE_GUIDE = 4161047;

    // Warrior
    public static final int RED_HWARANG_SHIRT = 1040021;
    public static final int BLACK_MARTIAL_ARTS_PANTS = 1060016;
    public static final int MITHRIL_BATTLE_GRIEVES = 1072039;
    public static final int GLADIUS = 1302008;
    public static final int MITHRIL_POLE_ARM = 1442001;
    public static final int MITHRIL_MAUL = 1422001;
    public static final int FIREMANS_AXE = 1312005;
    public static final int DARK_ENGRIT = 1051010;

    // Bowman
    public static final int GREEN_HUNTERS_ARMOR = 1040067;
    public static final int GREEN_HUNTRESS_ARMOR = 1041054;
    public static final int GREEN_HUNTERS_PANTS = 1060056;
    public static final int GREEN_HUNTRESS_PANTS = 1061050;
    public static final int GREEN_HUNTER_BOOTS = 1072081;
    public static final int RYDEN = 1452005;
    public static final int MOUNTAIN_CROSSBOW = 1462000;

    // Magician
    public static final int BLUE_WIZARD_ROBE = 1050003;
    public static final int PURPLE_FAIRY_TOP = 1041041;
    public static final int PURPLE_FAIRY_SKIRT = 1061034;
    public static final int RED_MAGICSHOES = 1072075;
    public static final int MITHRIL_WAND = 1372003;
    public static final int CIRCLE_WINDED_STAFF = 1382017;

    // Thief
    public static final int DARK_BROWN_STEALER = 1040057;
    public static final int RED_STEAL = 1041047;
    public static final int DARK_BROWN_STEALER_PANTS = 1060043;
    public static final int RED_STEAL_PANTS = 1061043;
    public static final int BRONZE_CHAIN_BOOTS = 1072032;
    public static final int STEEL_GUARDS = 1472008;
    public static final int REEF_CLAW = 1332012;

    // Pirate
    public static final int BROWN_PAULIE_BOOTS = 1072294;
    public static final int PRIME_HANDS = 1482004;
    public static final int COLD_MIND = 1492004;
    public static final int BROWN_POLLARD = 1052107;

    // Three snails
    public static final int SNAIL_SHELL = 4000019;
    public static final int BLUE_SNAIL_SHELL = 4000000;
    public static final int RED_SNAIL_SHELL = 4000016;

    // Special scroll
    public static final int COLD_PROTECTION_SCROLl = 2041058;
    public static final int SPIKES_SCROLL = 2040727;
    public static final int VEGAS_SPELL_10 = 5610000;
    public static final int VEGAS_SPELL_60 = 5610001;
    public static final int CHAOS_SCROll_60 = 2049100;
    public static final int LIAR_TREE_SAP = 2049101;
    public static final int MAPLE_SYRUP = 2049102;
    public static final int WHITE_SCROLL = 2340000;
    public static final int CLEAN_SLATE_1 = 2049000;
    public static final int CLEAN_SLATE_3 = 2049001;
    public static final int CLEAN_SLATE_5 = 2049002;
    public static final int CLEAN_SLATE_20 = 2049003;
    public static final int RING_STR_100_SCROLL = 2041100;
    public static final int DRAGON_STONE_SCROLL = 2041200;
    public static final int BELT_STR_100_SCROLL = 2041300;

    // Cure debuff
    public static final int ALL_CURE_POTION = 2050004;
    public static final int EYEDROP = 2050001;
    public static final int TONIC = 2050002;
    public static final int HOLY_WATER = 2050003;
    private static final int DOJO_PARTY_ALL_CURE = 2022433;
    private static final int CARNIVAL_PARTY_ALL_CURE = 2022163;
    public static final int WHITE_ELIXIR = 2022544;

    public static boolean isPartyAllCure(int itemId) {
        return itemId == DOJO_PARTY_ALL_CURE || itemId == CARNIVAL_PARTY_ALL_CURE;
    }

    // Special effect
    public static final int PHARAOHS_BLESSING_1 = 2022585;
    public static final int PHARAOHS_BLESSING_2 = 2022586;
    public static final int PHARAOHS_BLESSING_3 = 2022587;
    public static final int PHARAOHS_BLESSING_4 = 2022588;

    // Evolve pet
    public static final int DRAGON_PET = 5000028;
    public static final int ROBO_PET = 5000047;

    // Pet equip
    public static final int MESO_MAGNET = 1812000;
    public static final int ITEM_POUCH = 1812001;
    public static final int ITEM_IGNORE = 1812007;

    public static boolean isPet(int itemId) {
        return itemId / 1000 == 5000;
    }

    // Expirable pet
    public static final int PET_SNAIL = 5000054;

    // Permanent pet
    private static final int PERMA_PINK_BEAN = 5000060;
    private static final int PERMA_KINO = 5000100;
    private static final int PERMA_WHITE_TIGER = 5000101;
    private static final int PERMA_MINI_YETI = 5000102;

    public static int[] getPermaPets() {
        return new int[]{PERMA_PINK_BEAN, PERMA_KINO, PERMA_WHITE_TIGER, PERMA_MINI_YETI};
    }

    // Maker
    public static final int BASIC_MONSTER_CRYSTAL_1 = 4260000;
    public static final int BASIC_MONSTER_CRYSTAL_2 = 4260001;
    public static final int BASIC_MONSTER_CRYSTAL_3 = 4260002;
    public static final int INTERMEDIATE_MONSTER_CRYSTAL_1 = 4260003;
    public static final int INTERMEDIATE_MONSTER_CRYSTAL_2 = 4260004;
    public static final int INTERMEDIATE_MONSTER_CRYSTAL_3 = 4260005;
    public static final int ADVANCED_MONSTER_CRYSTAL_1 = 4260006;
    public static final int ADVANCED_MONSTER_CRYSTAL_2 = 4260007;
    public static final int ADVANCED_MONSTER_CRYSTAL_3 = 4260008;

    public static boolean isMonsterCrystal(int itemId) {
        return itemId / 10000 == 426;
    }

    // NPC weather (PQ)
    public static final int NPC_WEATHER_GROWLIE = 5120016; // Henesys PQ

    // Safety charm
    public static final int SAFETY_CHARM = 5130000;
    public static final int EASTER_BASKET = 4031283;
    public static final int EASTER_CHARM = 4140903;

    // Engagement box
    public static final int ENGAGEMENT_BOX_MOONSTONE = 2240000;
    public static final int ENGAGEMENT_BOX_STAR = 2240001;
    public static final int ENGAGEMENT_BOX_GOLDEN = 2240002;
    public static final int ENGAGEMENT_BOX_SILVER = 2240003;
    public static final int EMPTY_ENGAGEMENT_BOX_MOONSTONE = 4031357;
    public static final int ENGAGEMENT_RING_MOONSTONE = 4031358;
    public static final int EMPTY_ENGAGEMENT_BOX_STAR = 4031359;
    public static final int ENGAGEMENT_RING_STAR = 4031360;
    public static final int EMPTY_ENGAGEMENT_BOX_GOLDEN = 4031361;
    public static final int ENGAGEMENT_RING_GOLDEN = 4031362;
    public static final int EMPTY_ENGAGEMENT_BOX_SILVER = 4031363;
    public static final int ENGAGEMENT_RING_SILVER = 4031364;

    public static boolean isWeddingToken(int itemId) {
        return itemId >= ItemId.EMPTY_ENGAGEMENT_BOX_MOONSTONE && itemId <= ItemId.ENGAGEMENT_RING_SILVER;
    }

    // Wedding etc
    public static final int PARENTS_BLESSING = 4031373;
    public static final int OFFICIATORS_PERMISSION = 4031374;
    public static final int ONYX_CHEST_FOR_COUPLE = 4031424;

    // Wedding ticket
    public static final int NORMAL_WEDDING_TICKET_CATHEDRAL = 5251000;
    public static final int NORMAL_WEDDING_TICKET_CHAPEL = 5251001;
    public static final int PREMIUM_WEDDING_TICKET_CHAPEL = 5251002;
    public static final int PREMIUM_WEDDING_TICKET_CATHEDRAL = 5251003;

    // Wedding reservation
    public static final int PREMIUM_CATHEDRAL_RESERVATION_RECEIPT = 4031375;
    public static final int PREMIUM_CHAPEL_RESERVATION_RECEIPT = 4031376;
    public static final int NORMAL_CATHEDRAL_RESERVATION_RECEIPT = 4031480;
    public static final int NORMAL_CHAPEL_RESERVATION_RECEIPT = 4031481;

    // Wedding invite
    public static final int INVITATION_CHAPEL = 4031377;
    public static final int INVITATION_CATHEDRAL = 4031395;
    public static final int RECEIVED_INVITATION_CHAPEL = 4031406;
    public static final int RECEIVED_INVITATION_CATHEDRAL = 4031407;

    public static final int CARAT_RING_BASE = 1112300; // Unsure about math on this and the following one
    public static final int CARAT_RING_BOX_BASE = 2240004;
    private static final int CARAT_RING_BOX_MAX = 2240015;

    public static final int ENGAGEMENT_BOX_MIN = ENGAGEMENT_BOX_MOONSTONE;
    public static final int ENGAGEMENT_BOX_MAX = CARAT_RING_BOX_MAX;

    // Wedding ring
    public static final int WEDDING_RING_MOONSTONE = 1112803;
    public static final int WEDDING_RING_STAR = 1112806;
    public static final int WEDDING_RING_GOLDEN = 1112807;
    public static final int WEDDING_RING_SILVER = 1112809;

    public static boolean isWeddingRing(int itemId) {
        return itemId == WEDDING_RING_MOONSTONE || itemId == WEDDING_RING_STAR ||
               itemId == WEDDING_RING_GOLDEN || itemId == WEDDING_RING_SILVER;
    }

    // Priority buff
    public static final int ROSE_SCENT = 2022631;
    public static final int FREESIA_SCENT = 2022632;
    public static final int LAVENDER_SCENT = 2022633;

    // Cash shop
    public static final int WHEEL_OF_FORTUNE = 5510000;
    public static final int CASH_SHOP_SURPRISE = 5222000;
    public static final int EXP_COUPON_2X_4H = 5211048;
    public static final int DROP_COUPON_2X_4H = 5360042;
    public static final int EXP_COUPON_3X_2H = 5211060;
    public static final int QUICK_DELIVERY_TICKET = 5330000;
    public static final int CHALKBOARD_1 = 5370000;
    public static final int CHALKBOARD_2 = 5370001;
    public static final int REMOTE_GACHAPON_TICKET = 5451000;
    public static final int AP_RESET = 5050000;
    public static final int NAME_CHANGE = 5400000;
    public static final int WORLD_TRANSFER = 5401000;
    public static final int MAPLE_LIFE_B = 5432000;
    public static final int VICIOUS_HAMMER = 5570000;

    public static final int NX_CARD_100 = 4031865;
    public static final int NX_CARD_250 = 4031866;

    public static boolean isNxCard(int itemId) {
        return itemId == NX_CARD_100 || itemId == NX_CARD_250;
    }

    public static boolean isCashPackage(int itemId) {
        return itemId / 10000 == 910;
    }

    // Face expression
    private static final int FACE_EXPRESSION_MIN = 5160000;
    private static final int FACE_EXPRESSION_MAX = 5160014;

    public static boolean isFaceExpression(int itemId) {
        return itemId >= FACE_EXPRESSION_MIN && itemId <= FACE_EXPRESSION_MAX;
    }

    // New Year card
    public static final int NEW_YEARS_CARD = 2160101;
    public static final int NEW_YEARS_CARD_SEND = 4300000;
    public static final int NEW_YEARS_CARD_RECEIVED = 4301000;

    // Popular owl items
    private static final int WORK_GLOVES = 1082002;
    private static final int STEELY_THROWING_KNIVES = 2070005;
    private static final int ILBI_THROWING_STARS = 2070006;
    private static final int OWL_BALL_MASK = 1022047;
    private static final int PINK_ADVENTURER_CAPE = 1102041;
    private static final int CLAW_30_SCROLL = 2044705;
    private static final int HELMET_60_ACC_SCROLL = 2040017;
    private static final int MAPLE_SHIELD = 1092030;
    private static final int GLOVES_ATT_60_SCROLL = 2040804;

    public static int[] getOwlItems() {
        return new int[]{WORK_GLOVES, STEELY_THROWING_KNIVES, ILBI_THROWING_STARS, OWL_BALL_MASK, PINK_ADVENTURER_CAPE,
                CLAW_30_SCROLL, WHITE_SCROLL, HELMET_60_ACC_SCROLL, MAPLE_SHIELD, GLOVES_ATT_60_SCROLL};
    }

    // Henesys PQ
    public static final int GREEN_PRIMROSE_SEED = 4001095;
    public static final int PURPLE_PRIMROSE_SEED = 4001096;
    public static final int PINK_PRIMROSE_SEED = 4001097;
    public static final int BROWN_PRIMROSE_SEED = 4001098;
    public static final int YELLOW_PRIMROSE_SEED = 4001099;
    public static final int BLUE_PRIMROSE_SEED = 4001100;
    public static final int MOON_BUNNYS_RICE_CAKE = 4001101;

    // Catch mobs items
    public static final int PHEROMONE_PERFUME = 2270000;
    public static final int POUCH = 2270001;
    public static final int GHOST_SACK = 4031830;
    public static final int ARPQ_ELEMENT_ROCK = 2270002;
    public static final int ARPQ_SPIRIT_JEWEL = 4031868;
    public static final int MAGIC_CANE = 2270003;
    public static final int TAMED_RUDOLPH = 4031887;
    public static final int TRANSPARENT_MARBLE_1 = 2270005;
    public static final int MONSTER_MARBLE_1 = 2109001;
    public static final int TRANSPARENT_MARBLE_2 = 2270006;
    public static final int MONSTER_MARBLE_2 = 2109002;
    public static final int TRANSPARENT_MARBLE_3 = 2270007;
    public static final int MONSTER_MARBLE_3 = 2109003;
    public static final int EPQ_PURIFICATION_MARBLE = 2270004;
    public static final int EPQ_MONSTER_MARBLE = 4001169;
    public static final int FISH_NET = 2270008;
    public static final int FISH_NET_WITH_A_CATCH = 2022323;

    // Mount
    public static final int BATTLESHIP = 1932000;

    // Explorer mount
    public static final int HOG = 1902000;
    private static final int SILVER_MANE = 1902001;
    private static final int RED_DRACO = 1902002;
    private static final int EXPLORER_SADDLE = 1912000;

    public static boolean isExplorerMount(int itemId) {
        return itemId >= HOG && itemId <= RED_DRACO || itemId == EXPLORER_SADDLE;
    }

    // Cygnus mount
    private static final int MIMIANA = 1902005;
    private static final int MIMIO = 1902006;
    private static final int SHINJOU = 1902007;
    private static final int CYGNUS_SADDLE = 1912005;

    public static boolean isCygnusMount(int itemId) {
        return itemId >= MIMIANA && itemId <= SHINJOU || itemId == CYGNUS_SADDLE;
    }

    // Dev equips
    public static final int GREEN_HEADBAND = 1002067;
    public static final int TIMELESS_NIBLEHEIM = 1402046;
    public static final int BLUE_KORBEN = 1082140;
    public static final int MITHRIL_PLATINE_PANTS = 1060091;
    public static final int BLUE_CARZEN_BOOTS = 1072154;
    public static final int MITHRIL_PLATINE = 1040103;
}
