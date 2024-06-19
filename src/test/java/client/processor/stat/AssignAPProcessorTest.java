package client.processor.stat;

import client.Job;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class AssignAPProcessorTest {

    @Test
    void getMinHp() {
        int max_level = 200;
        int cygnus_max_level = 120;

        BiFunction<Job,Integer,Integer> f = AssignAPProcessor::getMinHp;

        assertAll(
                // Beginners
                () -> assertEquals(2438, f.apply(Job.BEGINNER, max_level)),
                () -> assertEquals(1478, f.apply(Job.NOBLESSE, cygnus_max_level)),

                // Warrior (Explorer)
                () -> assertEquals(4918, f.apply(Job.WARRIOR, max_level)),

                () -> assertEquals(5218, f.apply(Job.FIGHTER, max_level)),
                () -> assertEquals(5218, f.apply(Job.CRUSADER, max_level)),
                () -> assertEquals(5218, f.apply(Job.HERO, max_level)),

                () -> assertEquals(4918, f.apply(Job.PAGE, max_level)),
                () -> assertEquals(4918, f.apply(Job.WHITEKNIGHT, max_level)),
                () -> assertEquals(4918, f.apply(Job.PALADIN, max_level)),

                () -> assertEquals(4918, f.apply(Job.SPEARMAN, max_level)),
                () -> assertEquals(4918, f.apply(Job.DRAGONKNIGHT, max_level)),
                () -> assertEquals(4918, f.apply(Job.DARKKNIGHT, max_level)),

                // Warrior (Cygnus)
                () -> assertEquals(2998, f.apply(Job.DAWNWARRIOR1, cygnus_max_level)),
                () -> assertEquals(3298, f.apply(Job.DAWNWARRIOR2, cygnus_max_level)),
                () -> assertEquals(3298, f.apply(Job.DAWNWARRIOR3, cygnus_max_level)),
                () -> assertEquals(3298, f.apply(Job.DAWNWARRIOR4, cygnus_max_level)),

                // Warrior (Aran)
                () -> assertEquals(4918, f.apply(Job.ARAN1, max_level)),
                () -> assertEquals(5218, f.apply(Job.ARAN2, max_level)),
                () -> assertEquals(5218, f.apply(Job.ARAN3, max_level)),
                () -> assertEquals(5218, f.apply(Job.ARAN4, max_level)),

                // Magician (Explorer)
                () -> assertEquals(2054, f.apply(Job.MAGICIAN, max_level)),

                () -> assertEquals(2054, f.apply(Job.FP_WIZARD, max_level)),
                () -> assertEquals(2054, f.apply(Job.FP_MAGE, max_level)),
                () -> assertEquals(2054, f.apply(Job.FP_ARCHMAGE, max_level)),

                () -> assertEquals(2054, f.apply(Job.IL_WIZARD, max_level)),
                () -> assertEquals(2054, f.apply(Job.IL_MAGE, max_level)),
                () -> assertEquals(2054, f.apply(Job.IL_ARCHMAGE, max_level)),

                () -> assertEquals(2054, f.apply(Job.CLERIC, max_level)),
                () -> assertEquals(2054, f.apply(Job.PRIEST, max_level)),
                () -> assertEquals(2054, f.apply(Job.BISHOP, max_level)),

                // Magician (Cygnus)
                () -> assertEquals(1254, f.apply(Job.BLAZEWIZARD1, cygnus_max_level)),
                () -> assertEquals(1254, f.apply(Job.BLAZEWIZARD2, cygnus_max_level)),
                () -> assertEquals(1254, f.apply(Job.BLAZEWIZARD3, cygnus_max_level)),
                () -> assertEquals(1254, f.apply(Job.BLAZEWIZARD4, cygnus_max_level)),

                // Bowman (Explorer)
                () -> assertEquals(4058, f.apply(Job.BOWMAN, max_level)),

                () -> assertEquals(4358, f.apply(Job.HUNTER, max_level)),
                () -> assertEquals(4358, f.apply(Job.RANGER, max_level)),
                () -> assertEquals(4358, f.apply(Job.BOWMASTER, max_level)),

                () -> assertEquals(4358, f.apply(Job.CROSSBOWMAN, max_level)),
                () -> assertEquals(4358, f.apply(Job.SNIPER, max_level)),
                () -> assertEquals(4358, f.apply(Job.MARKSMAN, max_level)),

                // Bowman (Cygnus)
                () -> assertEquals(2458, f.apply(Job.WINDARCHER1, cygnus_max_level)),
                () -> assertEquals(2758, f.apply(Job.WINDARCHER2, cygnus_max_level)),
                () -> assertEquals(2758, f.apply(Job.WINDARCHER3, cygnus_max_level)),
                () -> assertEquals(2758, f.apply(Job.WINDARCHER4, cygnus_max_level)),

                // Thief (Explorer)
                () -> assertEquals(4058, f.apply(Job.THIEF, max_level)),

                () -> assertEquals(4358, f.apply(Job.ASSASSIN, max_level)),
                () -> assertEquals(4358, f.apply(Job.HERMIT, max_level)),
                () -> assertEquals(4358, f.apply(Job.NIGHTLORD, max_level)),

                () -> assertEquals(4358, f.apply(Job.BANDIT, max_level)),
                () -> assertEquals(4358, f.apply(Job.CHIEFBANDIT, max_level)),
                () -> assertEquals(4358, f.apply(Job.SHADOWER, max_level)),

                // Thief (Cygnus)
                () -> assertEquals(2458, f.apply(Job.NIGHTWALKER1, cygnus_max_level)),
                () -> assertEquals(2758, f.apply(Job.NIGHTWALKER2, cygnus_max_level)),
                () -> assertEquals(2758, f.apply(Job.NIGHTWALKER3, cygnus_max_level)),
                () -> assertEquals(2758, f.apply(Job.NIGHTWALKER4, cygnus_max_level)),

                // Pirate (Explorer)
                () -> assertEquals(4438, f.apply(Job.PIRATE, max_level)),

                () -> assertEquals(4738, f.apply(Job.BRAWLER, max_level)),
                () -> assertEquals(4738, f.apply(Job.MARAUDER, max_level)),
                () -> assertEquals(4738, f.apply(Job.BUCCANEER, max_level)),

                () -> assertEquals(4738, f.apply(Job.GUNSLINGER, max_level)),
                () -> assertEquals(4738, f.apply(Job.OUTLAW, max_level)),
                () -> assertEquals(4738, f.apply(Job.CORSAIR, max_level)),

                // Pirate (Cygnus)
                () -> assertEquals(2678, f.apply(Job.THUNDERBREAKER1, cygnus_max_level)),
                () -> assertEquals(2978, f.apply(Job.THUNDERBREAKER2, cygnus_max_level)),
                () -> assertEquals(2978, f.apply(Job.THUNDERBREAKER3, cygnus_max_level)),
                () -> assertEquals(2978, f.apply(Job.THUNDERBREAKER4, cygnus_max_level))
        );
    }

    @Test
    void getMinMp() {
        int max_level = 200;
        int cygnus_max_level = 120;

        BiFunction<Job,Integer,Integer> f = AssignAPProcessor::getMinMp;

        assertAll(
                // Beginners
                () -> assertEquals(1995, f.apply(Job.BEGINNER, max_level)),
                () -> assertEquals(1195, f.apply(Job.NOBLESSE, cygnus_max_level)),

                // Warrior (Explorer)
                () -> assertEquals(855, f.apply(Job.WARRIOR, max_level)),

                () -> assertEquals(855, f.apply(Job.FIGHTER, max_level)),
                () -> assertEquals(855, f.apply(Job.CRUSADER, max_level)),
                () -> assertEquals(855, f.apply(Job.HERO, max_level)),

                () -> assertEquals(955, f.apply(Job.PAGE, max_level)),
                () -> assertEquals(955, f.apply(Job.WHITEKNIGHT, max_level)),
                () -> assertEquals(955, f.apply(Job.PALADIN, max_level)),

                () -> assertEquals(955, f.apply(Job.SPEARMAN, max_level)),
                () -> assertEquals(955, f.apply(Job.DRAGONKNIGHT, max_level)),
                () -> assertEquals(955, f.apply(Job.DARKKNIGHT, max_level)),

                // Warrior (Cygnus)
                () -> assertEquals(535, f.apply(Job.DAWNWARRIOR1, cygnus_max_level)),
                () -> assertEquals(535, f.apply(Job.DAWNWARRIOR2, cygnus_max_level)),
                () -> assertEquals(535, f.apply(Job.DAWNWARRIOR3, cygnus_max_level)),
                () -> assertEquals(535, f.apply(Job.DAWNWARRIOR4, cygnus_max_level)),

                // Warrior (Aran)
                () -> assertEquals(855, f.apply(Job.ARAN1, max_level)),
                () -> assertEquals(855, f.apply(Job.ARAN2, max_level)),
                () -> assertEquals(855, f.apply(Job.ARAN3, max_level)),
                () -> assertEquals(855, f.apply(Job.ARAN4, max_level)),

                // Magician (Explorer)
                () -> assertEquals(4399, f.apply(Job.MAGICIAN, max_level)),

                () -> assertEquals(4849, f.apply(Job.FP_WIZARD, max_level)),
                () -> assertEquals(4849, f.apply(Job.FP_MAGE, max_level)),
                () -> assertEquals(4849, f.apply(Job.FP_ARCHMAGE, max_level)),

                () -> assertEquals(4849, f.apply(Job.IL_WIZARD, max_level)),
                () -> assertEquals(4849, f.apply(Job.IL_MAGE, max_level)),
                () -> assertEquals(4849, f.apply(Job.IL_ARCHMAGE, max_level)),

                () -> assertEquals(4849, f.apply(Job.CLERIC, max_level)),
                () -> assertEquals(4849, f.apply(Job.PRIEST, max_level)),
                () -> assertEquals(4849, f.apply(Job.BISHOP, max_level)),

                // Magician (Cygnus)
                () -> assertEquals(2639, f.apply(Job.BLAZEWIZARD1, cygnus_max_level)),
                () -> assertEquals(3089, f.apply(Job.BLAZEWIZARD2, cygnus_max_level)),
                () -> assertEquals(3089, f.apply(Job.BLAZEWIZARD3, cygnus_max_level)),
                () -> assertEquals(3089, f.apply(Job.BLAZEWIZARD4, cygnus_max_level)),

                // Bowman (Explorer)
                () -> assertEquals(2785, f.apply(Job.BOWMAN, max_level)),

                () -> assertEquals(2935, f.apply(Job.HUNTER, max_level)),
                () -> assertEquals(2935, f.apply(Job.RANGER, max_level)),
                () -> assertEquals(2935, f.apply(Job.BOWMASTER, max_level)),

                () -> assertEquals(2935, f.apply(Job.CROSSBOWMAN, max_level)),
                () -> assertEquals(2935, f.apply(Job.SNIPER, max_level)),
                () -> assertEquals(2935, f.apply(Job.MARKSMAN, max_level)),

                // Bowman (Cygnus)
                () -> assertEquals(1665, f.apply(Job.WINDARCHER1, cygnus_max_level)),
                () -> assertEquals(1815, f.apply(Job.WINDARCHER2, cygnus_max_level)),
                () -> assertEquals(1815, f.apply(Job.WINDARCHER3, cygnus_max_level)),
                () -> assertEquals(1815, f.apply(Job.WINDARCHER4, cygnus_max_level)),

                // Thief (Explorer)
                () -> assertEquals(2785, f.apply(Job.THIEF, max_level)),

                () -> assertEquals(2935, f.apply(Job.ASSASSIN, max_level)),
                () -> assertEquals(2935, f.apply(Job.HERMIT, max_level)),
                () -> assertEquals(2935, f.apply(Job.NIGHTLORD, max_level)),

                () -> assertEquals(2935, f.apply(Job.BANDIT, max_level)),
                () -> assertEquals(2935, f.apply(Job.CHIEFBANDIT, max_level)),
                () -> assertEquals(2935, f.apply(Job.SHADOWER, max_level)),

                // Thief (Cygnus)
                () -> assertEquals(1665, f.apply(Job.NIGHTWALKER1, cygnus_max_level)),
                () -> assertEquals(1815, f.apply(Job.NIGHTWALKER2, cygnus_max_level)),
                () -> assertEquals(1815, f.apply(Job.NIGHTWALKER3, cygnus_max_level)),
                () -> assertEquals(1815, f.apply(Job.NIGHTWALKER4, cygnus_max_level)),

                // Pirate (Explorer)
                () -> assertEquals(3545, f.apply(Job.PIRATE, max_level)),

                () -> assertEquals(3695, f.apply(Job.BRAWLER, max_level)),
                () -> assertEquals(3695, f.apply(Job.MARAUDER, max_level)),
                () -> assertEquals(3695, f.apply(Job.BUCCANEER, max_level)),

                () -> assertEquals(3695, f.apply(Job.GUNSLINGER, max_level)),
                () -> assertEquals(3695, f.apply(Job.OUTLAW, max_level)),
                () -> assertEquals(3695, f.apply(Job.CORSAIR, max_level)),

                // Pirate (Cygnus)
                () -> assertEquals(2105, f.apply(Job.THUNDERBREAKER1, cygnus_max_level)),
                () -> assertEquals(2255, f.apply(Job.THUNDERBREAKER2, cygnus_max_level)),
                () -> assertEquals(2255, f.apply(Job.THUNDERBREAKER3, cygnus_max_level)),
                () -> assertEquals(2255, f.apply(Job.THUNDERBREAKER4, cygnus_max_level))
        );
    }
}