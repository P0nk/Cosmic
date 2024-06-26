package server.partyquest.pyramid;

public class PyramidCharacterStats {

    private final PyramidDifficulty difficulty;
    private int totalHits;
    private int totalMisses;
    private int totalCools;
    private PyramidRank rank = PyramidRank.C;
    private int skillUses;

    public PyramidCharacterStats(PyramidDifficulty difficulty) {
        this.difficulty = difficulty;
        this.totalHits = 0;
        this.totalMisses = 0;
        this.totalCools = 0;
        this.skillUses = 0;
    }

    public PyramidDifficulty getDifficulty() {
        return difficulty;
    }

    public PyramidRank getRank() {
        return rank;
    }

    public void setRank(PyramidRank rank) {
        this.rank = rank;
    }

    public void calculateRank() {
        if (rank.equals(PyramidRank.D)) {
            return;
        }

        int totalScore = (totalHits + totalCools - totalMisses);
        if (totalScore >= 3000) rank = PyramidRank.S;
        else if (totalScore >= 2000) rank = PyramidRank.A;
        else if (totalScore >= 1000) rank = PyramidRank.B;
        else rank = PyramidRank.C;
    }

    public int getTotalHits() {
        return totalHits;
    }

    public void addHits(int amount) {
        totalHits += amount;
    }

    public int getTotalMisses() {
        return totalMisses;
    }

    public void addMisses(int amount) {
        totalMisses += amount;
    }

    public int getTotalCools() {
        return totalCools;
    }

    public void addCools(int amount) {
        totalCools += amount;
    }

    public void addSkillUses(int amount) {
        skillUses += amount;
    }

    public boolean canUseSkill() {
        return skillUses < getMaxSkillUses();
    }

    public int getMaxSkillUses() {
        int total = totalHits + totalCools;
        return Math.min(total / 500, 6); // Capped at 6 skills per PQ
    }

    public int getAvailableSkillUses() {
        return getMaxSkillUses() - skillUses;
    }

    public int calculateExp() {
        // TODO: Are players are supposed to get more EXP if they are in a party?
        int exp = (totalHits * 20) + (totalCools * 100);
        if (rank.equals(PyramidRank.S)) exp += (5500 * difficulty.getMode());
        if (rank.equals(PyramidRank.A)) exp += (5000 * difficulty.getMode());
        if (rank.equals(PyramidRank.B)) exp += (4250 * difficulty.getMode());
        if (rank.equals(PyramidRank.C)) exp += (2000 * difficulty.getMode());
        if (rank.equals(PyramidRank.D)) exp /= 5;

        return exp;
    }

    public int getBlessingBuff() {
        int total = totalHits + totalCools;
        if (total >= 2000) return 2022588;
        else if (total >= 1000) return 2022587;
        else if (total >= 500) return 2022586;
        else if (total >= 250) return 2022585;
        else return 0;
    }
}
