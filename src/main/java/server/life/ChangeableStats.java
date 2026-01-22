/*
 * Corrected ChangeableStats for High-Level Mobs (>200)
 */
package server.life;

public class ChangeableStats extends OverrideMonsterStats {

    public int watk, matk, wdef, mdef, level;

    public ChangeableStats(MonsterStats stats, OverrideMonsterStats ostats) {
        hp = ostats.getHp();
        exp = ostats.getExp();
        mp = ostats.getMp();
        watk = stats.getPADamage();
        matk = stats.getMADamage();
        wdef = stats.getPDDamage();
        mdef = stats.getMDDamage();
        level = stats.getLevel();
    }

    public ChangeableStats(MonsterStats stats, int newLevel, boolean pqMob) {
        // Safety: Prevent division by zero
        double oldLevel = stats.getLevel() <= 0 ? 1 : stats.getLevel();

        // Calculate modifier
        final double mod = (double) newLevel / oldLevel;

        // Calculate HP/Exp Ratio (to scale EXP properly without looking up a table)
        double hpRatio = (double) stats.getHp() / (double) stats.getExp();
        if (Double.isNaN(hpRatio) || Double.isInfinite(hpRatio)) hpRatio = 1.0;

        final double pqMod = (pqMob ? 1.5 : 1.0);

        // 1. HP Calculation (Pure Math, No Lookup)
        long newHp = Math.round(stats.getHp() * mod * pqMod);
        hp = (int) Math.min(newHp, Integer.MAX_VALUE);

        // 2. EXP Calculation (Based on Ratio)
        if (stats.getExp() > 0) {
            exp = (int) Math.min(Math.round((hp / hpRatio) * pqMod), Integer.MAX_VALUE);
        } else {
            exp = 0;
        }

        // 3. Scale other stats
        mp = (int) Math.min(Math.round(stats.getMp() * mod * pqMod), Integer.MAX_VALUE);
        watk = (int) Math.min(Math.round(stats.getPADamage() * mod), Integer.MAX_VALUE);
        matk = (int) Math.min(Math.round(stats.getMADamage() * mod), Integer.MAX_VALUE);

        // Cap defense to prevent "Miss GodMode" if stats get too high
        int pDefCap = stats.isBoss() ? 30 : 20;
        int mDefCap = stats.isBoss() ? 30 : 20;

        wdef = (int) Math.min(pDefCap, Math.round(stats.getPDDamage() * mod));
        mdef = (int) Math.min(mDefCap, Math.round(stats.getMDDamage() * mod));

        level = newLevel;
    }

    public ChangeableStats(MonsterStats stats, float statModifier, boolean pqMob) {
        this(stats, (int) (statModifier * stats.getLevel()), pqMob);
    }
}