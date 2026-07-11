package client;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Saloon {

    private String loadout_name;
    private int face;
    private int hair;
    private int skin;

    // Constructor
    public Saloon(String loadout_name, int hair, int face, int skin) {
        this.loadout_name = loadout_name;
        this.hair = hair;
        this.face = face;
        this.skin = skin;
    }

    // Getters and Setters
    public int getFace() { return face; }
    public void setFace(int face) { this.face = face; }

    public int getHair() { return hair; }
    public void setHair(int hair) { this.hair = hair; }

    public int getSkin() { return skin; }
    public void setSkin(int skin) { this.skin = skin; }

    public String getLoadoutName() {
        return loadout_name;
    }

    public void setLoadoutName(String loadout_name) {
        this.loadout_name = loadout_name;
    }

}
