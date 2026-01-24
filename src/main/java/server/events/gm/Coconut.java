package server.events.gm;

import client.Character;
import constants.id.MapId;
import server.TimerManager;
import server.maps.MapleMap;
import tools.PacketCreator;
import java.util.ArrayList;
import java.util.List;

public class Coconut extends Event {
    private final MapleMap map;
    private int mapleScore = 0;
    private int storyScore = 0;
    private int countBombing = 80;
    private int countFalling = 401;
    private int countStopped = 20;
    private final List<CoconutObject> coconuts = new ArrayList<>(); // Renamed class below

    public Coconut(MapleMap map) {
        super(1, 50);
        this.map = map;
    }

    public void startEvent() {
        map.startEvent();
        coconuts.clear();
        for (int i = 0; i < 506; i++) {
            coconuts.add(new CoconutObject(i));
        }
        map.broadcastMessage(PacketCreator.hitCoconut(true, 0, 0));
        setCoconutsHittable(true);
        map.broadcastMessage(PacketCreator.getClock(300));

        TimerManager.getInstance().schedule(() -> {
            if (map.getId() == MapId.EVENT_COCONUT_HARVEST) {
                if (mapleScore == storyScore) {
                    bonusTime();
                } else {
                    calculateResultAndWarp();
                }
            }
        }, 300 * 1000);
    }

    public void bonusTime() {
        map.broadcastMessage(PacketCreator.getClock(120));
        TimerManager.getInstance().schedule(this::calculateResultAndWarp, 120 * 1000);
    }

    private void calculateResultAndWarp() {
        boolean mapleWins = mapleScore > storyScore;
        boolean draw = mapleScore == storyScore;

        for (Character chr : map.getCharacters()) {
            int team = chr.getTeam();
            boolean isWinner = (team == 0 && mapleWins) || (team == 1 && !mapleWins && !draw);

            if (isWinner) {
                chr.sendPacket(PacketCreator.showEffect("event/coconut/victory"));
                chr.sendPacket(PacketCreator.playSound("Coconut/Victory"));
            } else {
                chr.sendPacket(PacketCreator.showEffect("event/coconut/lose"));
                chr.sendPacket(PacketCreator.playSound("Coconut/Failed"));
            }
        }
        warpOut(mapleWins, draw);
    }

    public void warpOut(boolean mapleWins, boolean draw) {
        setCoconutsHittable(false);
        TimerManager.getInstance().schedule(() -> {
            // Create copy to avoid ConcurrentModificationException during warp
            List<Character> chars = new ArrayList<>(map.getCharacters());

            for (Character chr : chars) {
                if (draw) {
                    chr.changeMap(MapId.EVENT_EXIT);
                } else if ((mapleWins && chr.getTeam() == 0) || (!mapleWins && chr.getTeam() == 1)) {
                    chr.changeMap(MapId.EVENT_WINNER);
                } else {
                    chr.changeMap(MapId.EVENT_EXIT);
                }
            }
            map.setCoconut(null);
        }, 12000);
    }

    public int getMapleScore() { return mapleScore; }
    public int getStoryScore() { return storyScore; }
    public void addMapleScore() { this.mapleScore++; }
    public void addStoryScore() { this.storyScore++; }

    public int getBombings() { return countBombing; }
    public void bombCoconut() { countBombing--; }

    public int getFalling() { return countFalling; }
    public void fallCoconut() { countFalling--; }

    public int getStopped() { return countStopped; }
    public void stopCoconut() { countStopped--; }

    public CoconutObject getCoconut(int id) {
        if (id >= 0 && id < coconuts.size()) {
            return coconuts.get(id);
        }
        return null;
    }

    public List<CoconutObject> getAllCoconuts() { return coconuts; }

    public void setCoconutsHittable(boolean hittable) {
        for (CoconutObject nut : coconuts) {
            nut.setHittable(hittable);
        }
    }
}