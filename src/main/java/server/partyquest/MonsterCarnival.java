package server.partyquest;

import client.Character;
import config.YamlConfig;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import server.TimerManager;
import server.maps.MapleMap;
import server.maps.Reactor;
import tools.PacketCreator;

import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Drago (Dragohe4rt)
 */
public class MonsterCarnival {
    private static final String ENTRY_TEXT = "You can select \"Summon Monsters\", \"Ability\", or \"Protector\" as your tactic during the Monster Carnival. Use Tab and F1 ~ F12 for quick access!";
    private static final String TIME_EXTENSION_TEXT = "The time has been extended.";

    public static final String FIND_ERROR_TEXT = "We could not find a group in this room.\r\nProbably the group was scrapped inside the room!";
    public static final String ENTER_LOBBY_TEXT = "You will now receive challenges from other groups. If you do not accept a challenge within 3 minutes, you will be taken out.";
    public static final String PICK_ROOM_TEXT = "Sign up for the Monster Festival!\r\n";
    public static final String LEADER_NOT_FOUND_TEXT = "Could not find the Leader.";
    public static final String FACING_CHALLENGE_TEXT = "The group is currently facing a challenge.";
    public static final String CANCELED_CHALLENGE_TEXT = "The group in the room canceled your challenge.";

    public static int D = 3;
    public static int C = 2;
    public static int B = 1;
    public static int A = 0;

    private Party p1, p2;
    private MapleMap map;
    private ScheduledFuture<?> timer, effectTimer, respawnTask;
    private long startTime = 0;
    private int summonsR = 0, summonsB = 0, room = 0;
    private Character leader1, leader2, team1, team2;
    private int redCP, blueCP, redTotalCP, blueTotalCP, redTimeupCP, blueTimeupCP;
    private boolean cpq1;

    public MonsterCarnival(Party p1, Party p2, int mapid, boolean cpq1, int room) {
        try {
            this.cpq1 = cpq1;
            this.room = room;
            this.p1 = p1;
            this.p2 = p2;
            Channel cs = Server.getInstance().getWorld(p2.getLeader().getWorld()).getChannel(p2.getLeader().getChannel());
            p1.setEnemy(p2);
            p2.setEnemy(p1);
            map = cs.getMapFactory().getDisposableMap(mapid);
            startTime = System.currentTimeMillis() + MINUTES.toMillis(10);
            int redPortal = 0;
            int bluePortal = 0;
            if (map.isPurpleCPQMap()) {
                redPortal = 2;
                bluePortal = 1;
            }
            for (PartyCharacter mpc : p1.getMembers()) {
                Character mc = mpc.getPlayer();
                if (mc != null) {
                    mc.setMonsterCarnival(this);
                    mc.setTeam(0);
                    mc.setFestivalPoints(0);
                    mc.forceChangeMap(map, map.getPortal(redPortal));
                    mc.dropMessage(6, ENTRY_TEXT);
                    if (p1.getLeader().getId() == mc.getId()) {
                        leader1 = mc;
                    }
                    team1 = mc;
                }
            }
            for (PartyCharacter mpc : p2.getMembers()) {
                Character mc = mpc.getPlayer();
                if (mc != null) {
                    mc.setMonsterCarnival(this);
                    mc.setTeam(1);
                    mc.setFestivalPoints(0);
                    mc.forceChangeMap(map, map.getPortal(bluePortal));
                    mc.dropMessage(6, ENTRY_TEXT);
                    if (p2.getLeader().getId() == mc.getId()) {
                        leader2 = mc;
                    }
                    team2 = mc;
                }
            }
            if (team1 == null || team2 == null) {
                for (PartyCharacter mpc : p1.getMembers()) {
                    Character chr = mpc.getPlayer();
                    if (chr != null) {
                        chr.dropMessage(5, "There was a problem. Please re-create a room.");
                    }
                }
                for (PartyCharacter mpc : p2.getMembers()) {
                    Character chr = mpc.getPlayer();
                    if (chr != null) {
                        chr.dropMessage(5, "There was a problem. Please re-create a room.");
                    }
                }
                return;
            }

            // thanks Atoot, Vcoc for noting double CPQ functional being sent to players in CPQ start

            timer = TimerManager.getInstance().schedule(() -> timeUp(), SECONDS.toMillis(map.getTimeDefault())); // thanks Atoot for noticing an irregular "event extended" issue here
            effectTimer = TimerManager.getInstance().schedule(() -> complete(), SECONDS.toMillis(map.getTimeDefault() - 10));
            respawnTask = TimerManager.getInstance().register(() -> respawn(), YamlConfig.config.server.RESPAWN_INTERVAL);

            cs.initMonsterCarnival(cpq1, room);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void respawn() {
        map.respawn();
    }

    public void playerDisconnected(int charid) {
        int team = -1;
        for (PartyCharacter mpc : leader1.getParty().getMembers()) {
            if (mpc.getId() == charid) {
                team = 0;
            }
        }
        for (PartyCharacter mpc : leader2.getParty().getMembers()) {
            if (mpc.getId() == charid) {
                team = 1;
            }
        }
        for (Character chrMap : map.getAllPlayers()) {
            if (team == -1) {
                team = 1;
            }
            String teamName = switch (team) {
                case 0 -> "Maple Red";
                case 1 -> "Maple Blue";
                default -> "";
            };
            chrMap.dropMessage(5, "%s left the Carnival of Monsters.".formatted(teamName));
        }
        earlyFinish();
    }

    private void earlyFinish() {
        dispose(true);
    }

    public void leftParty(int charid) {
        playerDisconnected(charid);
    }

    protected void dispose() {
        dispose(false);
    }

    public boolean canSummonR() {
        return summonsR < map.getMaxMobs();
    }

    public void summonR() {
        summonsR++;
    }

    public boolean canSummonB() {
        return summonsB < map.getMaxMobs();
    }

    public void summonB() {
        summonsB++;
    }

    public boolean canGuardianR() {
        int teamReactors = 0;
        for (Reactor react : map.getAllReactors()) {
            if (react.getName().substring(0, 1).contentEquals("0")) {
                teamReactors += 1;
            }
        }

        return teamReactors < map.getMaxReactors();
    }

    public boolean canGuardianB() {
        int teamReactors = 0;
        for (Reactor react : map.getAllReactors()) {
            if (react.getName().substring(0, 1).contentEquals("1")) {
                teamReactors += 1;
            }
        }

        return teamReactors < map.getMaxReactors();
    }

    protected void dispose(boolean warpout) {
        Channel cs = map.getChannelServer();
        MapleMap out;
        if (!cpq1) { // cpq2
            out = cs.getMapFactory().getMap(980030010);
        } else {
            out = cs.getMapFactory().getMap(980000010);
        }
        for (PartyCharacter mpc : leader1.getParty().getMembers()) {
            Character mc = mpc.getPlayer();
            if (mc != null) {
                mc.resetCP();
                mc.setTeam(-1);
                mc.setMonsterCarnival(null);
                if (warpout) {
                    mc.changeMap(out, out.getPortal(0));
                }
            }
        }
        for (PartyCharacter mpc : leader2.getParty().getMembers()) {
            Character mc = mpc.getPlayer();
            if (mc != null) {
                mc.resetCP();
                mc.setTeam(-1);
                mc.setMonsterCarnival(null);
                if (warpout) {
                    mc.changeMap(out, out.getPortal(0));
                }
            }
        }
        if (this.timer != null) {
            this.timer.cancel(true);
            this.timer = null;
        }
        if (this.effectTimer != null) {
            this.effectTimer.cancel(true);
            this.effectTimer = null;
        }
        if (this.respawnTask != null) {
            this.respawnTask.cancel(true);
            this.respawnTask = null;
        }
        redTotalCP = 0;
        blueTotalCP = 0;
        leader1.getParty().setEnemy(null);
        leader2.getParty().setEnemy(null);
        map.dispose();
        map = null;

        cs.finishMonsterCarnival(cpq1, room);
    }

    public void exit() {
        dispose();
    }

    public ScheduledFuture<?> getTimer() {
        return this.timer;
    }

    private void finish(int winningTeam) {
        try {
            Channel cs = map.getChannelServer();
            if (winningTeam == 0) {
                for (PartyCharacter mpc : leader1.getParty().getMembers()) {
                    Character mc = mpc.getPlayer();
                    if (mc != null) {
                        mc.gainFestivalPoints(this.redTotalCP);
                        mc.setMonsterCarnival(null);
                        if (cpq1) {
                            mc.changeMap(cs.getMapFactory().getMap(map.getId() + 2), cs.getMapFactory().getMap(map.getId() + 2).getPortal(0));
                        } else {
                            mc.changeMap(cs.getMapFactory().getMap(map.getId() + 200), cs.getMapFactory().getMap(map.getId() + 200).getPortal(0));
                        }
                        mc.setTeam(-1);
                        mc.dispelDebuffs();
                    }
                }
                for (PartyCharacter mpc : leader2.getParty().getMembers()) {
                    Character mc = mpc.getPlayer();
                    if (mc != null) {
                        mc.gainFestivalPoints(this.blueTotalCP);
                        mc.setMonsterCarnival(null);
                        if (cpq1) {
                            mc.changeMap(cs.getMapFactory().getMap(map.getId() + 3), cs.getMapFactory().getMap(map.getId() + 3).getPortal(0));
                        } else {
                            mc.changeMap(cs.getMapFactory().getMap(map.getId() + 300), cs.getMapFactory().getMap(map.getId() + 300).getPortal(0));
                        }
                        mc.setTeam(-1);
                        mc.dispelDebuffs();
                    }
                }
            } else if (winningTeam == 1) {
                for (PartyCharacter mpc : leader2.getParty().getMembers()) {
                    Character mc = mpc.getPlayer();
                    if (mc != null) {
                        mc.gainFestivalPoints(this.blueTotalCP);
                        mc.setMonsterCarnival(null);
                        if (cpq1) {
                            mc.changeMap(cs.getMapFactory().getMap(map.getId() + 2), cs.getMapFactory().getMap(map.getId() + 2).getPortal(0));
                        } else {
                            mc.changeMap(cs.getMapFactory().getMap(map.getId() + 200), cs.getMapFactory().getMap(map.getId() + 200).getPortal(0));
                        }
                        mc.setTeam(-1);
                        mc.dispelDebuffs();
                    }
                }
                for (PartyCharacter mpc : leader1.getParty().getMembers()) {
                    Character mc = mpc.getPlayer();
                    if (mc != null) {
                        mc.gainFestivalPoints(this.redTotalCP);
                        mc.setMonsterCarnival(null);
                        if (cpq1) {
                            mc.changeMap(cs.getMapFactory().getMap(map.getId() + 3), cs.getMapFactory().getMap(map.getId() + 3).getPortal(0));
                        } else {
                            mc.changeMap(cs.getMapFactory().getMap(map.getId() + 300), cs.getMapFactory().getMap(map.getId() + 300).getPortal(0));
                        }
                        mc.setTeam(-1);
                        mc.dispelDebuffs();
                    }
                }
            }
            dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void timeUp() {
        int cp1 = this.redTimeupCP;
        int cp2 = this.blueTimeupCP;
        if (cp1 == cp2) {
            extendTime();
            return;
        }
        if (cp1 > cp2) {
            finish(0);
        } else {
            finish(1);
        }
    }

    public long getTimeLeft() {
        return (startTime - System.currentTimeMillis());
    }

    public int getTimeLeftSeconds() {
        return (int) (getTimeLeft() / 1000);
    }

    private void extendTime() {
        for (Character chrMap : map.getAllPlayers()) {
            chrMap.dropMessage(5, TIME_EXTENSION_TEXT);
        }
        startTime = System.currentTimeMillis() + MINUTES.toMillis(3);

        map.broadcastMessage(PacketCreator.getClock((int) MINUTES.toSeconds(3)));

        timer = TimerManager.getInstance().schedule(() -> timeUp(), SECONDS.toMillis(map.getTimeExpand()));
        effectTimer = TimerManager.getInstance().schedule(() -> complete(), SECONDS.toMillis(map.getTimeExpand() - 10)); // thanks Vcoc for noticing a time set issue here
    }

    public void complete() {
        int cp1 = this.redTotalCP;
        int cp2 = this.blueTotalCP;

        this.redTimeupCP = cp1;
        this.blueTimeupCP = cp2;

        if (cp1 == cp2) {
            return;
        }
        boolean redWin = cp1 > cp2;
        int chnl = leader1.getClient().getChannel();
        int chnl1 = leader2.getClient().getChannel();
        if (chnl != chnl1) {
            throw new RuntimeException("Os lideres estao em canais diferentes.");
        }

        map.killAllMonsters();
        for (PartyCharacter mpc : leader1.getParty().getMembers()) {
            Character mc = mpc.getPlayer();
            if (mc != null) {
                if (redWin) {
                    mc.sendPacket(PacketCreator.showEffect("quest/carnival/win"));
                    mc.sendPacket(PacketCreator.playSound("MobCarnival/Win"));
                    mc.dispelDebuffs();
                } else {
                    mc.sendPacket(PacketCreator.showEffect("quest/carnival/lose"));
                    mc.sendPacket(PacketCreator.playSound("MobCarnival/Lose"));
                    mc.dispelDebuffs();
                }
            }
        }
        for (PartyCharacter mpc : leader2.getParty().getMembers()) {
            Character mc = mpc.getPlayer();
            if (mc != null) {
                if (!redWin) {
                    mc.sendPacket(PacketCreator.showEffect("quest/carnival/win"));
                    mc.sendPacket(PacketCreator.playSound("MobCarnival/Win"));
                    mc.dispelDebuffs();
                } else {
                    mc.sendPacket(PacketCreator.showEffect("quest/carnival/lose"));
                    mc.sendPacket(PacketCreator.playSound("MobCarnival/Lose"));
                    mc.dispelDebuffs();
                }
            }
        }
    }

    public Party getRed() {
        return p1;
    }

    public void setRed(Party p1) {
        this.p1 = p1;
    }

    public Party getBlue() {
        return p2;
    }

    public void setBlue(Party p2) {
        this.p2 = p2;
    }

    public Character getLeader1() {
        return leader1;
    }

    public void setLeader1(Character leader1) {
        this.leader1 = leader1;
    }

    public Character getLeader2() {
        return leader2;
    }

    public void setLeader2(Character leader2) {
        this.leader2 = leader2;
    }

    public Character getEnemyLeader(int team) {
        switch (team) {
            case 0:
                return leader2;
            case 1:
                return leader1;
        }
        return null;
    }

    public int getBlueCP() {
        return blueCP;
    }

    public void setBlueCP(int blueCP) {
        this.blueCP = blueCP;
    }

    public int getBlueTotalCP() {
        return blueTotalCP;
    }

    public void setBlueTotalCP(int blueTotalCP) {
        this.blueTotalCP = blueTotalCP;
    }

    public int getRedCP() {
        return redCP;
    }

    public void setRedCP(int redCP) {
        this.redCP = redCP;
    }

    public int getRedTotalCP() {
        return redTotalCP;
    }

    public void setRedTotalCP(int redTotalCP) {
        this.redTotalCP = redTotalCP;
    }

    public int getTotalCP(int team) {
        if (team == 0) {
            return redTotalCP;
        } else if (team == 1) {
            return blueTotalCP;
        } else {
            throw new RuntimeException("Equipe desconhecida");
        }
    }

    public void setTotalCP(int totalCP, int team) {
        if (team == 0) {
            this.redTotalCP = totalCP;
        } else if (team == 1) {
            this.blueTotalCP = totalCP;
        }
    }

    public int getCP(int team) {
        if (team == 0) {
            return redCP;
        } else if (team == 1) {
            return blueCP;
        } else {
            throw new RuntimeException("Equipe desconhecida" + team);
        }
    }

    public void setCP(int CP, int team) {
        if (team == 0) {
            this.redCP = CP;
        } else if (team == 1) {
            this.blueCP = CP;
        }
    }

    public int getRoom() {
        return this.room;
    }

    public MapleMap getEventMap() {
        return this.map;
    }
}
