package config;

public class WorldConfig {
    public int flag = 0;
    public String server_message = "Welcome!";
    public String event_message = "";
    public String why_am_i_recommended = "";
    public int channels = 1;
    public double exp_rate = 1.0;
    public double meso_rate = 1.0;
    public double drop_rate = 1.0;
    public double boss_drop_rate = 1.0;
    public int quest_rate = 1;
    public int travel_rate = 1;
    public int fishing_rate = 1;
    public float mob_rate = 1;
    public int max_mob_per_spawnpoint = 1;
    public boolean use_progressive_exp = true;
    public int mob_per_spawntick = 1; // default 1 tick spawn per spawn point
    public java.util.List<Double> exp_rate_gain = null;
    public java.util.List<Integer> meso_rate_gain = null;
    public java.util.List<Integer> drop_rate_gain = null;
}
