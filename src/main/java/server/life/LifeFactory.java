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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.DataType;
import provider.wz.WZFiles;
import tools.Pair;
import tools.StringUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LifeFactory {
    private static final Logger log = LoggerFactory.getLogger(LifeFactory.class);
    private static final DataProvider data = DataProviderFactory.getDataProvider(WZFiles.MOB);
    private final static DataProvider stringDataWZ = DataProviderFactory.getDataProvider(WZFiles.STRING);
    private static final Data mobStringData = stringDataWZ.getData("Mob.img");
    private static final Data npcStringData = stringDataWZ.getData("Npc.img");
    private static final Map<Integer, MonsterStats> monsterStats = new HashMap<>();
    private static final Set<Integer> hpbarBosses = getHpBarBosses();

    private static Set<Integer> getHpBarBosses() {
        Set<Integer> ret = new HashSet<>();

        DataProvider uiDataWZ = DataProviderFactory.getDataProvider(WZFiles.UI);
        for (Data bossData : uiDataWZ.getData("UIWindow.img").getChildByPath("MobGage/Mob").getChildren()) {
            ret.add(Integer.valueOf(bossData.getName()));
        }

        return ret;
    }

    public static AbstractLoadedLife getLife(int id, String type) {
        if (type.equalsIgnoreCase("n")) {
            return getNPC(id);
        } else if (type.equalsIgnoreCase("m")) {
            return getMonster(id);
        } else {
            log.warn("Unknown Life type: {}", type);
            return null;
        }
    }

    private static class MobAttackInfoHolder {
        protected int attackPos;
        protected int mpCon;
        protected int coolTime;
        protected int animationTime;

        protected MobAttackInfoHolder(int attackPos, int mpCon, int coolTime, int animationTime) {
            this.attackPos = attackPos;
            this.mpCon = mpCon;
            this.coolTime = coolTime;
            this.animationTime = animationTime;
        }
    }

    private static void setMonsterAttackInfo(int mid, List<MobAttackInfoHolder> attackInfos) {
        if (!attackInfos.isEmpty()) {
            MonsterInformationProvider mi = MonsterInformationProvider.getInstance();

            for (MobAttackInfoHolder attackInfo : attackInfos) {
                mi.setMobAttackInfo(mid, attackInfo.attackPos, attackInfo.mpCon, attackInfo.coolTime);
                mi.setMobAttackAnimationTime(mid, attackInfo.attackPos, attackInfo.animationTime);
            }
        }
    }

    private static Pair<MonsterStats, List<MobAttackInfoHolder>> getMonsterStats(int mid) {
        Data monsterData = data.getData(StringUtil.getLeftPaddedStr(mid + ".img", '0', 11));
        if (monsterData == null) {
            return null;
        }
        Data monsterInfoData = monsterData.getChildByPath("info");

        List<MobAttackInfoHolder> attackInfos = new LinkedList<>();
        MonsterStats stats = new MonsterStats();

        int linkMid = DataTool.getIntConvert("link", monsterInfoData, 0);
        if (linkMid != 0) {
            Pair<MonsterStats, List<MobAttackInfoHolder>> linkStats = getMonsterStats(linkMid);
            if (linkStats == null) {
                return null;
            }

            // thanks resinate for noticing non-propagable infos such as revives getting retrieved
            attackInfos.addAll(linkStats.getRight());
        }

        stats.setHp(DataTool.getIntConvert("maxHP", monsterInfoData));
        stats.setFriendly(DataTool.getIntConvert("damagedByMob", monsterInfoData, stats.isFriendly() ? 1 : 0) == 1);
        stats.setPADamage(DataTool.getIntConvert("PADamage", monsterInfoData));
        stats.setPDDamage(DataTool.getIntConvert("PDDamage", monsterInfoData));
        stats.setMADamage(DataTool.getIntConvert("MADamage", monsterInfoData));
        stats.setMDDamage(DataTool.getIntConvert("MDDamage", monsterInfoData));
        stats.setMp(DataTool.getIntConvert("maxMP", monsterInfoData, stats.getMp()));
        stats.setExp(DataTool.getIntConvert("exp", monsterInfoData, stats.getExp()));
        stats.setLevel(DataTool.getIntConvert("level", monsterInfoData));
        stats.setRemoveAfter(DataTool.getIntConvert("removeAfter", monsterInfoData, stats.removeAfter()));
        stats.setBoss(DataTool.getIntConvert("boss", monsterInfoData, stats.isBoss() ? 1 : 0) > 0);
        stats.setExplosiveReward(DataTool.getIntConvert("explosiveReward", monsterInfoData, stats.isExplosiveReward() ? 1 : 0) > 0);
        stats.setFfaLoot(DataTool.getIntConvert("publicReward", monsterInfoData, stats.isFfaLoot() ? 1 : 0) > 0);
        stats.setUndead(DataTool.getIntConvert("undead", monsterInfoData, stats.isUndead() ? 1 : 0) > 0);
        stats.setName(DataTool.getString(mid + "/name", mobStringData, "MISSINGNO"));
        stats.setBuffToGive(DataTool.getIntConvert("buff", monsterInfoData, stats.getBuffToGive()));
        stats.setCP(DataTool.getIntConvert("getCP", monsterInfoData, stats.getCP()));
        stats.setRemoveOnMiss(DataTool.getIntConvert("removeOnMiss", monsterInfoData, stats.removeOnMiss() ? 1 : 0) > 0);
        stats.setCoolDamage(DataTool.getIntConvert("coolDamage", monsterInfoData, Integer.MAX_VALUE));
        stats.setCoolDamageProb(DataTool.getIntConvert("coolDamageProb", monsterInfoData, 0));

        Data special = monsterInfoData.getChildByPath("coolDamage");
        if (special != null) {
            int coolDmg = DataTool.getIntConvert("coolDamage", monsterInfoData);
            int coolProb = DataTool.getIntConvert("coolDamageProb", monsterInfoData, 0);
            stats.setCool(new Pair<>(coolDmg, coolProb));
        }
        special = monsterInfoData.getChildByPath("loseItem");
        if (special != null) {
            for (Data liData : special.getChildren()) {
                stats.addLoseItem(new loseItem(DataTool.getInt(liData.getChildByPath("id")), (byte) DataTool.getInt(liData.getChildByPath("prop")), (byte) DataTool.getInt(liData.getChildByPath("x"))));
            }
        }
        special = monsterInfoData.getChildByPath("selfDestruction");
        if (special != null) {
            stats.setSelfDestruction(new selfDestruction((byte) DataTool.getInt(special.getChildByPath("action")), DataTool.getIntConvert("removeAfter", special, -1), DataTool.getIntConvert("hp", special, -1)));
        }
        Data firstAttackData = monsterInfoData.getChildByPath("firstAttack");
        int firstAttack = 0;
        if (firstAttackData != null) {
            if (firstAttackData.getType() == DataType.FLOAT) {
                firstAttack = Math.round(DataTool.getFloat(firstAttackData));
            } else {
                firstAttack = DataTool.getInt(firstAttackData);
            }
        }
        stats.setFirstAttack(firstAttack > 0);
        stats.setDropPeriod(DataTool.getIntConvert("dropItemPeriod", monsterInfoData, stats.getDropPeriod() / 10000) * 10000);

        // thanks yuxaij, Riizade, Z1peR, Anesthetic for noticing some bosses crashing players due to missing requirements
        boolean hpbarBoss = stats.isBoss() && hpbarBosses.contains(mid);
        stats.setTagColor(hpbarBoss ? DataTool.getIntConvert("hpTagColor", monsterInfoData, 0) : 0);
        stats.setTagBgColor(hpbarBoss ? DataTool.getIntConvert("hpTagBgcolor", monsterInfoData, 0) : 0);

        for (Data idata : monsterData) {
            if (!idata.getName().equals("info")) {
                int delay = 0;
                for (Data pic : idata.getChildren()) {
                    delay += DataTool.getIntConvert("delay", pic, 0);
                }
                stats.setAnimationTime(idata.getName(), delay);
            }
        }
        Data reviveInfo = monsterInfoData.getChildByPath("revive");
        if (reviveInfo != null) {
            List<Integer> revives = new LinkedList<>();
            for (Data data_ : reviveInfo) {
                revives.add(DataTool.getInt(data_));
            }
            stats.setRevives(revives);
        }
        decodeElementalString(stats, DataTool.getString("elemAttr", monsterInfoData, ""));

        MonsterInformationProvider mi = MonsterInformationProvider.getInstance();
        Data monsterSkillInfoData = monsterInfoData.getChildByPath("skill");
        if (monsterSkillInfoData != null) {
            int i = 0;
            Set<MobSkillId> skills = new HashSet<>();
            while (monsterSkillInfoData.getChildByPath(Integer.toString(i)) != null) {
                int skillId = DataTool.getInt(i + "/skill", monsterSkillInfoData, 0);
                int skillLv = DataTool.getInt(i + "/level", monsterSkillInfoData, 0);
                MobSkillType type = MobSkillType.from(skillId).orElseThrow();
                skills.add(new MobSkillId(type, skillLv));

                Data monsterSkillData = monsterData.getChildByPath("skill" + (i + 1));
                if (monsterSkillData != null) {
                    int animationTime = 0;
                    for (Data effectEntry : monsterSkillData.getChildren()) {
                        animationTime += DataTool.getIntConvert("delay", effectEntry, 0);
                    }

                    MobSkill skill = MobSkillFactory.getMobSkillOrThrow(type, skillLv);
                    mi.setMobSkillAnimationTime(skill, animationTime);
                }

                i++;
            }
            stats.setSkills(skills);
        }

        int i = 0;
        Data monsterAttackData;
        while ((monsterAttackData = monsterData.getChildByPath("attack" + (i + 1))) != null) {
            int animationTime = 0;
            for (Data effectEntry : monsterAttackData.getChildren()) {
                animationTime += DataTool.getIntConvert("delay", effectEntry, 0);
            }

            int mpCon = DataTool.getIntConvert("info/conMP", monsterAttackData, 0);
            int coolTime = DataTool.getIntConvert("info/attackAfter", monsterAttackData, 0);
            attackInfos.add(new MobAttackInfoHolder(i, mpCon, coolTime, animationTime));
            i++;
        }

        Data banishData = monsterInfoData.getChildByPath("ban");
        if (banishData != null) {
            stats.setBanishInfo(new BanishInfo(DataTool.getString("banMsg", banishData), DataTool.getInt("banMap/0/field", banishData, -1), DataTool.getString("banMap/0/portal", banishData, "sp")));
        }

        int noFlip = DataTool.getInt("noFlip", monsterInfoData, 0);
        if (noFlip > 0) {
            Point origin = DataTool.getPoint("stand/0/origin", monsterData, null);
            if (origin != null) {
                stats.setFixedStance(origin.getX() < 1 ? 5 : 4);    // fixed left/right
            }
        }

        return new Pair<>(stats, attackInfos);
    }

    public static Monster getMonster(int mid) {
        try {
            MonsterStats stats = monsterStats.get(mid);
            if (stats == null) {
                Pair<MonsterStats, List<MobAttackInfoHolder>> mobStats = getMonsterStats(mid);
                stats = mobStats.getLeft();
                setMonsterAttackInfo(mid, mobStats.getRight());

                monsterStats.put(mid, stats);
            }
            return new Monster(mid, stats);
        } catch (NullPointerException npe) {
            log.error("[SEVERE] MOB {} failed to load.", mid, npe);
            return null;
        }
    }

    public static int getMonsterLevel(int mid) {
        try {
            MonsterStats stats = monsterStats.get(mid);
            if (stats == null) {
                Data monsterData = data.getData(StringUtil.getLeftPaddedStr(mid + ".img", '0', 11));
                if (monsterData == null) {
                    return -1;
                }
                Data monsterInfoData = monsterData.getChildByPath("info");
                return DataTool.getIntConvert("level", monsterInfoData);
            } else {
                return stats.getLevel();
            }
        } catch (NullPointerException npe) {
            log.error("[SEVERE] MOB {} failed to load.", mid, npe);
        }

        return -1;
    }

    private static void decodeElementalString(MonsterStats stats, String elemAttr) {
        for (int i = 0; i < elemAttr.length(); i += 2) {
            stats.setEffectiveness(Element.getFromChar(elemAttr.charAt(i)), ElementalEffectiveness.getByNumber(Integer.parseInt(String.valueOf(elemAttr.charAt(i + 1)))));
        }
    }

    public static NPC getNPC(int nid) {
        return new NPC(nid, new NPCStats(DataTool.getString(nid + "/name", npcStringData, "MISSINGNO")));
    }

    public static String getNPCDefaultTalk(int nid) {
        return DataTool.getString(nid + "/d0", npcStringData, "(...)");
    }

    public static class BanishInfo {

        private final int map;
        private final String portal;
        private final String msg;

        public BanishInfo(String msg, int map, String portal) {
            this.msg = msg;
            this.map = map;
            this.portal = portal;
        }

        public int getMap() {
            return map;
        }

        public String getPortal() {
            return portal;
        }

        public String getMsg() {
            return msg;
        }
    }

    public static class loseItem {

        private final int id;
        private final byte chance;
        private final byte x;

        public loseItem(int id, byte chance, byte x) {
            this.id = id;
            this.chance = chance;
            this.x = x;
        }

        public int getId() {
            return id;
        }

        public byte getChance() {
            return chance;
        }

        public byte getX() {
            return x;
        }
    }

    public static class selfDestruction {

        private final byte action;
        private final int removeAfter;
        private final int hp;

        public selfDestruction(byte action, int removeAfter, int hp) {
            this.action = action;
            this.removeAfter = removeAfter;
            this.hp = hp;
        }

        public int getHp() {
            return hp;
        }

        public byte getAction() {
            return action;
        }

        public int removeAfter() {
            return removeAfter;
        }
    }
}
