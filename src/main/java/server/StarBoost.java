package server;

import config.YamlConfig;

public class StarBoost {
    private int watk;
    private int matk;
    private int str;
    private int dex;
    private int luk;
    private int intStat;

    public StarBoost(int watk, int matk, int str, int dex, int luk, int intStat) {
        this.watk = watk;
        this.matk = matk;
        this.str = str;
        this.dex = dex;
        this.luk = luk;
        this.intStat = intStat;
    }

    // Getters
    public int getWatk() { return watk; }
    public int getMatk() { return matk; }
    public int getStr() { return str; }
    public int getDex() { return dex; }
    public int getLuk() { return luk; }
    public int getInt() { return intStat; }

    // Setters
    public void setWatk(int watk) { this.watk = watk; }
    public void setMatk(int matk) { this.matk = matk; }
    public void setStr(int str) { this.str = str; }
    public void setDex(int dex) { this.dex = dex; }
    public void setLuk(int luk) { this.luk = luk; }
    public void setInt(int intStat) { this.intStat = intStat; }

    // Operations
    public StarBoost add(StarBoost other) {
        return new StarBoost(
                this.watk + other.watk,
                this.matk + other.matk,
                this.str + other.str,
                this.dex + other.dex,
                this.luk + other.luk,
                this.intStat + other.intStat
        );
    }

    public StarBoost subtract(StarBoost other) {
        return new StarBoost(
                this.watk - other.watk,
                this.matk - other.matk,
                this.str - other.str,
                this.dex - other.dex,
                this.luk - other.luk,
                this.intStat - other.intStat
        );
    }

    public StarBoost multiply(int scalar) {
        return new StarBoost(
                this.watk * scalar,
                this.matk * scalar,
                this.str * scalar,
                this.dex * scalar,
                this.luk * scalar,
                this.intStat * scalar
        );
    }

    public StarBoost divide(int scalar) {
        if (scalar == 0) {
            throw new IllegalArgumentException("Cannot divide by zero.");
        }
        return new StarBoost(
                this.watk / scalar,
                this.matk / scalar,
                this.str / scalar,
                this.dex / scalar,
                this.luk / scalar,
                this.intStat / scalar
        );
    }

    public StarBoost boostPercentage() {
        double boostPercentage = YamlConfig.config.server.STARFORCE_BOOST_PERCENTAGE;
        double boostPercentage2 = YamlConfig.config.server.STARFORCE_BOOST_PERCENTAGE2;

        return new StarBoost(
                (int) Math.ceil(this.watk * boostPercentage),
                (int) Math.ceil(this.matk * boostPercentage),
                (int) Math.ceil(this.str * boostPercentage2),
                (int) Math.ceil(this.dex * boostPercentage2),
                (int) Math.ceil(this.luk * boostPercentage2),
                (int) Math.ceil(this.intStat * boostPercentage2)
        );
    }

    @Override
    public String toString() {
        return "StatBoost{" +
                "watk=" + watk +
                ", matk=" + matk +
                ", str=" + str +
                ", dex=" + dex +
                ", luk=" + luk +
                ", intStat=" + intStat +
                '}';
    }
}
