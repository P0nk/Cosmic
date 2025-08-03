/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
        int max = mob != null && mob.isBoss() ? 1 : maxSpawnedMonsters;
        if (denySpawn || mobTime < 0 || spawnedMonsters.get() >= max) {
            return false;
        }
        return nextPossibleSpawn <= Server.getInstance().getCurrentTime();
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
            public void monsterHealed(int trueHeal) {}
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
