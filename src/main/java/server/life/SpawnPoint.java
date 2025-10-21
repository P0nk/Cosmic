package server.life;

import client.Character;
import net.server.Server;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;

public class SpawnPoint {
    private final int monster;
    private final int mobTime;
    private final int team;
    private final int fh;
    private final int f;
    private final Point pos;
    private long nextPossibleSpawn;
    private int mobInterval = 5000;
    private final AtomicInteger spawnedMonsters = new AtomicInteger(0);
    private final boolean immobile;
    private boolean denySpawn = false;

    // ✅ Added field to support multi-spawn per tick
    private int allowedSpawnCount = 1;

    public SpawnPoint(final Monster monster, Point pos, boolean immobile, int mobTime, int mobInterval, int team) {
        this.monster = monster.getId();
        this.pos = new Point(pos);
        this.mobTime = mobTime;
        this.team = team;
        this.fh = monster.getFh();
        this.f = monster.getF();
        this.immobile = immobile;
        this.mobInterval = mobInterval;
        this.nextPossibleSpawn = Server.getInstance().getCurrentTime();
    }

    public int getSpawned() {
        return spawnedMonsters.intValue();
    }

    public void setDenySpawn(boolean val) {
        denySpawn = val;
    }

    public boolean getDenySpawn() {
        return denySpawn;
    }

    public boolean shouldSpawn() {
        return shouldSpawn(1);
    }

    public boolean shouldSpawn(int maxSpawnedMonsters) {
        Monster mob = LifeFactory.getMonster(monster);
        int max = (mob != null && (mob.isBoss() || mob.getId() == 9001007)) ? 1 : maxSpawnedMonsters;

        if (denySpawn || mobTime < 0 || spawnedMonsters.get() >= max) {
            return false;
        }

        // Only allow spawn attempts if cooldown expired
        if (nextPossibleSpawn > Server.getInstance().getCurrentTime()) {
            return false;
        }

        // 💡 Double the spawn output per tick if possible
        int remaining = max - spawnedMonsters.get();
        int spawnNow = Math.min(2, remaining); // 2 mobs per tick max

        // Update next spawn timing *after* this double spawn
        nextPossibleSpawn = Server.getInstance().getCurrentTime() + mobInterval;

        // Store how many spawns are permitted this tick
        allowedSpawnCount = spawnNow;

        return true;
    }
    // Allows additional spawns in same tick without waiting for cooldown
    public boolean shouldSpawnExtra(int maxSpawnedMonsters) {
        Monster mob = LifeFactory.getMonster(monster);
        int max = (mob != null && (mob.isBoss() || mob.getId() == 9001007)) ? 1 : maxSpawnedMonsters;

        if (denySpawn || mobTime < 0 || spawnedMonsters.get() >= max) {
            return false;
        }

        // Skip cooldown check entirely for extra tick spawns
        int remaining = max - spawnedMonsters.get();
        return remaining > 0;
    }



    // ✅ Getter so MapleMap can read how many mobs to spawn this tick
    public int getAllowedSpawnCount() {
        return allowedSpawnCount;
    }

    public boolean shouldForceSpawn() {
        return mobTime >= 0 && spawnedMonsters.get() <= 0;
    }

    public Monster getMonster() {
        Monster mob = new Monster(LifeFactory.getMonster(monster));
        mob.setPosition(new Point(pos));
        mob.setTeam(team);
        mob.setFh(fh);
        mob.setF(f);
        spawnedMonsters.incrementAndGet();
        mob.addListener(new MonsterListener() {
            @Override
            public void monsterKilled(int aniTime) {
                nextPossibleSpawn = Server.getInstance().getCurrentTime();
                if (mobTime > 0) {
                    nextPossibleSpawn += SECONDS.toMillis(mobTime);
                } else {
                    nextPossibleSpawn += aniTime;
                }
                spawnedMonsters.decrementAndGet();
            }

            @Override
            public void monsterDamaged(Character from, int trueDmg) {}

            @Override
            public void monsterHealed(long trueHeal) {}
        });
        if (mobTime == 0) {
            nextPossibleSpawn = Server.getInstance().getCurrentTime() + mobInterval;
        }
        return mob;
    }

    public int getMonsterId() {
        return monster;
    }

    public Point getPosition() {
        return pos;
    }

    public final int getF() {
        return f;
    }

    public final int getFh() {
        return fh;
    }

    public int getMobTime() {
        return mobTime;
    }

    public int getTeam() {
        return team;
    }
}
