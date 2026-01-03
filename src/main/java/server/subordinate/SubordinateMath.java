package server.subordinate;

import client.inventory.Equip;
import tools.Randomizer;

public class SubordinateMath {

    /**
     * Calculates the new stats for a LEVEL UP PREVIEW.
     * @param eq The item being upgraded.
     * @param premium If true, uses one multiplier for all stats.
     * @param nxMultiplier If true, narrows the RNG range.
     * @param maxRate The cap for the random multiplier.
     * @return A StatsResult object containing the calculated values.
     */
    public static StatsResult simulateUpgrade(Equip eq, boolean premium, boolean nxMultiplier, double maxRate) {
        StatsResult result = new StatsResult();

        // 1. Generate Multipliers
        double[] mults = new double[6];
        double defMult;

        if (premium) {
            // Premium: Generate ONE main multiplier and apply to all
            double roll = generateMultiplier(nxMultiplier, maxRate);
            for (int i = 0; i < 6; i++) mults[i] = roll;
        } else {
            // Regular: Generate independent multipliers for each stat
            for (int i = 0; i < 6; i++) mults[i] = generateMultiplier(nxMultiplier, maxRate);
        }

        // Defense is always calculated separately
        defMult = 1.1 + (Randomizer.nextDouble() * 0.1);

        // 2. Calculate New Stats
        result.str  = (short) Math.floor(eq.getStr()  * mults[0]);
        result.dex  = (short) Math.floor(eq.getDex()  * mults[1]);
        result.int_ = (short) Math.floor(eq.getInt()  * mults[2]);
        result.luk  = (short) Math.floor(eq.getLuk()  * mults[3]);
        result.watk = (short) Math.floor(eq.getWatk() * mults[4]);
        result.matk = (short) Math.floor(eq.getMatk() * mults[5]);
        result.wdef = (short) Math.floor(eq.getWdef() * defMult);
        result.mdef = (short) Math.floor(eq.getMdef() * defMult);

        result.level = (byte) (eq.getItemLevel() + 1);
        result.hiddenLevel = (byte) (eq.getLevel() + 1);
        result.multipliers = mults;

        return result;
    }

    private static double generateMultiplier(boolean nxMultiplier, double maxRate) {
        if (nxMultiplier) {
            return 1.4 + (Randomizer.nextDouble() * 0.22);
        } else {
            return 1.4 + (Randomizer.nextDouble() * (maxRate - 1.4));
        }
    }

    public static class StatsResult {
        public short str, dex, int_, luk, watk, matk, wdef, mdef;
        public byte level, hiddenLevel;
        public double[] multipliers;
    }
}