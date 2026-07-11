package constants.id;

public class MobId {
    public static final int ARPQ_BOMB = 9300166;
    public static final int GIANT_CAKE = 9400606;
    public static final int TRANSPARENT_ITEM = 9300216;

    public static final int GREEN_MUSHROOM = 1110100;
    public static final int DEJECTED_GREEN_MUSHROOM = 1110130;
    public static final int GREEN_MUSHROOM_QUEST = 9101000;
    public static final int ZOMBIE_MUSHROOM = 2230101;
    public static final int ANNOYED_ZOMBIE_MUSHROOM = 2230131;
    public static final int ZOMBIE_MUSHROOM_QUEST = 9101001;
    public static final int GHOST_STUMP = 1140100;
    public static final int SMIRKING_GHOST_STUMP = 1140130;
    public static final int GHOST_STUMP_QUEST = 9101002;

    public static final int PAPULATUS_CLOCK = 8500001;
    public static final int HIGH_DARKSTAR = 8500003;
    public static final int LOW_DARKSTAR = 8500004;

    public static final int PIANUS_R = 8510000;
    public static final int BLOODY_BOOM = 8510100;

    public static final int PINK_BEAN = 8820001;

    public static final int ZAKUM_1 = 8800000;
    public static final int ZAKUM_2 = 8800001;
    public static final int ZAKUM_3 = 8800002;
    public static final int ZAKUM_ARM_1 = 8800003;
    public static final int ZAKUM_ARM_2 = 8800004;
    public static final int ZAKUM_ARM_3 = 8800005;
    public static final int ZAKUM_ARM_4 = 8800006;
    public static final int ZAKUM_ARM_5 = 8800007;
    public static final int ZAKUM_ARM_6 = 8800008;
    public static final int ZAKUM_ARM_7 = 8800009;
    public static final int ZAKUM_ARM_8 = 8800010;

    public static boolean isZakumArm(int mobId) {
        return mobId >= ZAKUM_ARM_1 && mobId <= ZAKUM_ARM_8;
    }

    public static final int HORNTAIL_PREHEAD_LEFT = 8810000;
    public static final int HORNTAIL_PREHEAD_RIGHT = 8810001;
    public static final int HORNTAIL_HEAD_A = 8810002;
    public static final int HORNTAIL_HEAD_B = 8810003;
    public static final int HORNTAIL_HEAD_C = 8810004;
    public static final int HORNTAIL_HAND_LEFT = 8810005;
    public static final int HORNTAIL_HAND_RIGHT = 8810006;
    public static final int HORNTAIL_WINGS = 8810007;
    public static final int HORNTAIL_LEGS = 8810008;
    public static final int HORNTAIL_TAIL = 8810009;
    public static final int DEAD_HORNTAIL_MIN = 8810010;
    public static final int DEAD_HORNTAIL_MAX = 8810017;
    public static final int HORNTAIL = 8810018;
    public static final int SUMMON_HORNTAIL = 8810026;

    public static boolean isDeadHorntailPart(int mobId) {
        return mobId >= DEAD_HORNTAIL_MIN && mobId <= DEAD_HORNTAIL_MAX;
    }

    public static final int SCARLION_STATUE = 9420546;
    public static final int SCARLION = 9420547;
    public static final int ANGRY_SCARLION = 9420548;
    public static final int FURIOUS_SCARLION = 9420549;
    public static final int TARGA_STATUE = 9420541;
    public static final int TARGA = 9420542;
    public static final int ANGRY_TARGA = 9420543;
    public static final int FURIOUS_TARGA = 9420544;

    // Catch mobs
    public static final int TAMABLE_HOG = 9300101;
    public static final int MUTANT_PIG = 8600003;
    public static final int GHOST = 9500197;
    public static final int ARPQ_SCORPION = 9300157;
    public static final int LOST_RUDOLPH = 9500320;
    public static final int KING_SLIME_DOJO = 9300187;
    public static final int FAUST_DOJO = 9300189;
    public static final int MUSHMOM_DOJO = 9300191;
    public static final int POISON_FLOWER = 9300175;
    public static final int P_JUNIOR = 9500336;

    // Friendly mobs
    public static final int WATCH_HOG = 9300102;
    public static final int MOON_BUNNY = 9300061;
    public static final int TYLUS = 9300093;
    public static final int JULIET = 9300137;
    public static final int ROMEO = 9300138;
    public static final int DELLI = 9300162;
    public static final int GIANT_SNOWMAN_LV1_EASY = 9400322;
    public static final int GIANT_SNOWMAN_LV1_MEDIUM = 9400327;
    public static final int GIANT_SNOWMAN_LV1_HARD = 9400332;
    public static final int GIANT_SNOWMAN_LV5_EASY = 9400326;
    public static final int GIANT_SNOWMAN_LV5_MEDIUM = 9400331;
    public static final int GIANT_SNOWMAN_LV5_HARD = 9400336;

    // Dojo
    private static final int DOJO_BOSS_MIN = 9300184;
    private static final int DOJO_BOSS_MAX = 9300215;

    public static boolean isDojoBoss(int mobId) {
        return mobId >= DOJO_BOSS_MIN && mobId <= DOJO_BOSS_MAX;
    }
}
