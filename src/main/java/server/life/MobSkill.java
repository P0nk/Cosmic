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
import client.Disease;
import client.status.MonsterStatus;
import constants.id.MapId;
import constants.id.MobId;
import constants.skills.Bishop;
import net.server.services.task.channel.OverallService;
import net.server.services.type.ChannelServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.maps.MapObject;
import server.maps.MapObjectType;
import server.maps.MapleMap;
import server.maps.Mist;
import tools.Randomizer;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Danny (Leifde)
 */
public class MobSkill {
    private static final Logger log = LoggerFactory.getLogger(MobSkill.class);

    private final MobSkillId id;
    private final int mpCon;
    private final int spawnEffect;
    private final int hp;
    private final int x;
    private final int y;
    private final int count;
    private final long duration;
    private final long cooltime;
    private final float prop;
    private final Point lt;
    private final Point rb;
    private final int limit;
    private final List<Integer> toSummon;

    private MobSkill(MobSkillType type, int level, int mpCon, int spawnEffect, int hp, int x, int y, int count,
                     long duration, long cooltime, float prop, Point lt, Point rb, int limit, List<Integer> toSummon) {
        this.id = new MobSkillId(type, level);
        this.mpCon = mpCon;
        this.spawnEffect = spawnEffect;
        this.hp = hp;
        this.x = x;
        this.y = y;
        this.count = count;
        this.duration = duration;
        this.cooltime = cooltime;
        this.prop = prop;
        this.lt = lt;
        this.rb = rb;
        this.limit = limit;
        this.toSummon = toSummon;
    }

    static class Builder {
        private final MobSkillType type;
        private final int level;
        private int mpCon;
        private int spawnEffect;
        private int hp;
        private int x;
        private int y;
        private int count;
        private long duration;
        private long cooltime;
        private float prop;
        private Point lt;
        private Point rb;
        private int limit;
        private List<Integer> toSummon;

        public Builder(MobSkillType type, int level) {
            this.type = type;
            this.level = level;
        }

        public Builder mpCon(int mpCon) {
            this.mpCon = mpCon;
            return this;
        }

        public Builder spawnEffect(int spawnEffect) {
            this.spawnEffect = spawnEffect;
            return this;
        }

        public Builder hp(int hp) {
            this.hp = hp;
            return this;
        }

        public Builder x(int x) {
            this.x = x;
            return this;
        }

        public Builder y(int y) {
            this.y = y;
            return this;
        }

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder cooltime(long cooltime) {
            this.cooltime = cooltime;
            return this;
        }

        public Builder prop(float prop) {
            this.prop = prop;
            return this;
        }

        public Builder lt(Point lt) {
            this.lt = lt;
            return this;
        }

        public Builder rb(Point rb) {
            this.rb = rb;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder toSummon(List<Integer> toSummon) {
            this.toSummon = Collections.unmodifiableList(toSummon);
            return this;
        }

        public MobSkill build() {
            return new MobSkill(type, level, mpCon, spawnEffect, hp, x, y, count, duration, cooltime, prop, lt, rb,
                    limit, toSummon);
        }
    }

    public void applyDelayedEffect(final Character player, final Monster monster, final boolean skill, int animationTime) {
        Runnable toRun = () -> {
            if (monster.isAlive()) {
                applyEffect(player, monster, skill, null);
            }
        };

        OverallService service = (OverallService) monster.getMap().getChannelServer().getServiceAccess(ChannelServices.OVERALL);
        service.registerOverallAction(monster.getMap().getId(), toRun, animationTime);
    }

    public void applyEffect(Monster monster) {
        applyEffect(null, monster, false, Collections.emptyList());
    }

    // TODO: avoid output argument banishPlayersOutput
    public void applyEffect(Character player, Monster monster, boolean skill, List<Character> banishPlayersOutput) {
        Disease disease = null;
        Map<MonsterStatus, Integer> stats = new EnumMap<>(MonsterStatus.class);
        List<Integer> reflection = new ArrayList<>();
        MapleMap map = monster.getMap();
        switch(monster.getId()) {
            case 8840000:
                if (id.type() == MobSkillType.ATTACK_UP || id.type() == MobSkillType.ATTACK_UP_M) {
                    if (map.countMonster(8210006) > 0) {
                        monster.heal((monster.getMaxHp() / 20) * map.countMonster(8210006), 0);
                        map.killMonster(8210006);
                    }
                    if (map.countMonster(8210007) > 0) {
                        monster.heal((monster.getMaxHp() / 10) * map.countMonster(8210006), 0);
                        map.killMonster(8210007);
                    }
                }
                if (id.type() == MobSkillType.DEFENSE_UP || id.type() == MobSkillType.DEFENSE_UP_M) {
                    map.tpVL(monster);
                }
                break;
            case 8240099:
            case 8240098:
                if (id.level() == 7) {
                    Random r = new Random();
                    map.LotusSpawn(r.nextInt(0, 1), r.nextInt(10, 15));
                } else if (id.level() == 8) {
                    Random r = new Random();
                    map.LotusSpawn(r.nextInt(0, 1), r.nextInt(8, 10));
                } else if (id.level() == 11) {
                    Random r = new Random();
                    map.LotusSpawn(r.nextInt(1), r.nextInt(12, 15));
                } else if (id.level() == 65) {
                    map.LotusSpawn(2, 8);
                }
                break;
            case 8880000:
            case 8880002:
            case 8880010:
                if(id.level() == 13) {
                    map.spawnPillars(3);
                }
                break;
        }
        switch (id.type()) {
            case ATTACK_UP, ATTACK_UP_M, PAD -> stats.put(MonsterStatus.WEAPON_ATTACK_UP, x);
            case MAGIC_ATTACK_UP, MAGIC_ATTACK_UP_M, MAD -> stats.put(MonsterStatus.MAGIC_ATTACK_UP, x);
            case DEFENSE_UP, DEFENSE_UP_M, PDR -> stats.put(MonsterStatus.WEAPON_DEFENSE_UP, x);
            case MAGIC_DEFENSE_UP, MAGIC_DEFENSE_UP_M, MDR -> stats.put(MonsterStatus.MAGIC_DEFENSE_UP, x);
            case HEAL_M -> applyHealEffect(skill, monster);
            case SEAL -> disease = Disease.SEAL;
            case DARKNESS -> disease = Disease.DARKNESS;
            case WEAKNESS -> disease = Disease.WEAKEN;
            case STUN -> disease = Disease.STUN;
            case CURSE -> disease = Disease.CURSE;
            case POISON -> disease = Disease.POISON;
            case SLOW -> disease = Disease.SLOW;
            case DISPEL -> applyDispelEffect(skill, monster, player);
            case SEDUCE -> disease = Disease.SEDUCE;
            case BANISH -> applyBanishEffect(skill, monster, player, banishPlayersOutput);
            case AREA_POISON -> spawnMonsterMist(monster);
            case REVERSE_INPUT -> disease = Disease.CONFUSE;
            case UNDEAD -> disease = Disease.ZOMBIFY;
            case PHYSICAL_IMMUNE -> {
                if (makeChanceResult() && !monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
                    stats.put(MonsterStatus.WEAPON_IMMUNITY, x);
                }
            }
            case MAGIC_IMMUNE -> {
                if (makeChanceResult() && !monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) {
                    stats.put(MonsterStatus.MAGIC_IMMUNITY, x);
                }
            }
            case PHYSICAL_COUNTER -> {
                stats.put(MonsterStatus.WEAPON_REFLECT, 10);
                stats.put(MonsterStatus.WEAPON_IMMUNITY, 10);
                reflection.add(x);
            }
            case MAGIC_COUNTER -> {
                stats.put(MonsterStatus.MAGIC_REFLECT, 10);
                stats.put(MonsterStatus.MAGIC_IMMUNITY, 10);
                reflection.add(x);
            }
            case PHYSICAL_AND_MAGIC_COUNTER -> {
                stats.put(MonsterStatus.WEAPON_REFLECT, 10);
                stats.put(MonsterStatus.WEAPON_IMMUNITY, 10);
                stats.put(MonsterStatus.MAGIC_REFLECT, 10);
                stats.put(MonsterStatus.MAGIC_IMMUNITY, 10);
                reflection.add(x);
            }
            case ACC -> stats.put(MonsterStatus.ACC, x);
            case EVA -> stats.put(MonsterStatus.AVOID, x);
            case SPEED -> stats.put(MonsterStatus.SPEED, x);
            case SEAL_SKILL -> stats.put(MonsterStatus.SEAL_SKILL, x);
            case SUMMON -> summonMonsters(monster);
        }
        if (stats.size() > 0) {
            applyMonsterBuffs(stats, skill, monster, reflection);
        }
        if (disease != null) {
            applyDisease(disease, skill, monster, player);
        }
    }

    private void applyHealEffect(boolean skill, Monster monster) {
        if (lt != null && rb != null && skill) {
            List<MapObject> objects = getObjectsInRange(monster, MapObjectType.MONSTER);
            final int hps = (getX() / 1000) * (int) (950 + 1050 * Math.random());
            for (MapObject mons : objects) {
                ((Monster) mons).heal(hps, getY());
            }
        } else {
            monster.heal(getX(), getY());
        }
    }

    private void applyDispelEffect(boolean skill, Monster monster, Character player) {
        if (lt != null && rb != null && skill) {
            getPlayersInRange(monster).forEach(Character::dispel);
        } else {
            player.dispel();
        }
    }

    private void applyBanishEffect(boolean skill, Monster monster, Character player,
                                   List<Character> banishPlayersOutput) {
        if (lt != null && rb != null && skill) {
            banishPlayersOutput.addAll(getPlayersInRange(monster));
        } else {
            banishPlayersOutput.add(player);
        }
    }

    private void spawnMonsterMist(Monster monster) {
        Rectangle mistArea = calculateBoundingBox(monster.getPosition());
        var mist = new Mist(mistArea, monster, this);
        int mistDuration = x * 100;
        monster.getMap().spawnMist(mist, mistDuration, false, false, false, false);
    }

    private void summonMonsters(Monster monster) {
        int skillLimit = this.limit;
        MapleMap map = monster.getMap();

        if (MapId.isDojo(map.getId())) {  // spawns in dojo should be unlimited
            skillLimit = Integer.MAX_VALUE;
        }

        if (map.getSpawnedMonstersOnMap() < 80) {
            List<Integer> summons = new ArrayList<>(toSummon);
            int summonLimit = monster.countAvailableMobSummons(summons.size(), skillLimit);
            if (summonLimit >= 1) {
                boolean bossRushMap = MapId.isBossRush(map.getId());

                Collections.shuffle(summons);
                for (Integer mobId : summons.subList(0, summonLimit)) {
                    Monster toSpawn = LifeFactory.getMonster(mobId);
                    if (toSpawn != null) {
                        if (bossRushMap) {
                            toSpawn.disableDrops();  // no littering on BRPQ pls
                        }
                        toSpawn.setPosition(monster.getPosition());
                        int ypos, xpos;
                        xpos = (int) monster.getPosition().getX();
                        ypos = (int) monster.getPosition().getY();
                        switch (mobId) {
                            case MobId.HIGH_DARKSTAR: // Pap bomb high
                                toSpawn.setFh((int) Math.ceil(Math.random() * 19.0));
                                ypos = -590;
                                break;
                            case MobId.LOW_DARKSTAR: // Pap bomb
                                xpos = (int) (monster.getPosition().getX() + Randomizer.nextInt(1000) - 500);
                                if (ypos != -590) {
                                    ypos = (int) monster.getPosition().getY();
                                }
                                break;
                            case MobId.BLOODY_BOOM: //Pianus bomb
                                if (Math.ceil(Math.random() * 5) == 1) {
                                    ypos = 78;
                                    xpos = Randomizer.nextInt(5) + (Randomizer.nextInt(2) == 1 ? 180 : 0);
                                } else {
                                    xpos = (int) (monster.getPosition().getX() + Randomizer.nextInt(1000) - 500);
                                }
                                break;
                        }
                        switch (map.getId()) {
                            case MapId.ORIGIN_OF_CLOCKTOWER: //Pap map
                                if (xpos < -890) {
                                    xpos = (int) (Math.ceil(Math.random() * 150) - 890);
                                } else if (xpos > 230) {
                                    xpos = (int) (230 - Math.ceil(Math.random() * 150));
                                }
                                break;
                            case MapId.CAVE_OF_PIANUS: // Pianus map
                                if (xpos < -239) {
                                    xpos = (int) (Math.ceil(Math.random() * 150) - 239);
                                } else if (xpos > 371) {
                                    xpos = (int) (371 - Math.ceil(Math.random() * 150));
                                }
                                break;
                        }
                        toSpawn.setPosition(new Point(xpos, ypos));
                        if (toSpawn.getId() == MobId.LOW_DARKSTAR) {
                            map.spawnFakeMonster(toSpawn);
                        } else {
                            map.spawnMonsterWithEffect(toSpawn, spawnEffect, toSpawn.getPosition());
                        }
                        monster.addSummonedMob(toSpawn);
                    }
                }
            }
        }
    }

    private void applyMonsterBuffs(Map<MonsterStatus, Integer> stats, boolean skill, Monster monster, List<Integer> reflection) {
        if (lt != null && rb != null && skill) {
            for (MapObject mons : getObjectsInRange(monster, MapObjectType.MONSTER)) {
                ((Monster) mons).applyMonsterBuff(stats, getX(), getDuration(), this, reflection);
            }
        } else {
            monster.applyMonsterBuff(stats, getX(), getDuration(), this, reflection);
        }
    }

    private void applyDisease(Disease disease, boolean skill, Monster monster, Character player) {
        if (lt != null && rb != null && skill) {
            int i = 0;
            for (Character character : getPlayersInRange(monster)) {
                if (!character.hasActiveBuff(Bishop.HOLY_SHIELD)) {
                    if (disease.equals(Disease.SEDUCE)) {
                        if (i < count) {
                            character.giveDebuff(Disease.SEDUCE, this);
                            i++;
                        }
                    } else {
                        character.giveDebuff(disease, this);
                    }
                }
            }
        } else {
            player.giveDebuff(disease, this);
        }
    }

    private List<Character> getPlayersInRange(Monster monster) {
        return monster.getMap().getPlayersInRange(calculateBoundingBox(monster.getPosition()));
    }

    public MobSkillId getId() {
        return id;
    }

    public MobSkillType getType() {
        return id.type();
    }

    public int getMpCon() {
        return mpCon;
    }

    public int getHP() {
        return hp;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public long getDuration() {
        return duration;
    }

    public long getCoolTime() {
        return cooltime;
    }

    public boolean makeChanceResult() {
        return prop == 1.0 || Math.random() < prop;
    }

    private Rectangle calculateBoundingBox(Point posFrom) {
        Point mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
        Point myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        Rectangle bounds = new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
        return bounds;
    }

    private List<MapObject> getObjectsInRange(Monster monster, MapObjectType objectType) {
        return monster.getMap().getMapObjectsInBox(calculateBoundingBox(monster.getPosition()), Collections.singletonList(objectType));
    }
}
