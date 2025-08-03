/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

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
import provider.wz.WZFiles;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MonsterInformationProvider {
    private static final Logger log = LoggerFactory.getLogger(MonsterInformationProvider.class);
    // Author : LightPepsi

    private static final MonsterInformationProvider instance = new MonsterInformationProvider();

    public static MonsterInformationProvider getInstance() {
        return instance;
    }

    private final Map<Pair<Integer, Integer>, Integer> mobAttackAnimationTime = new HashMap<>();
    private final Map<MobSkill, Integer> mobSkillAnimationTime = new HashMap<>();

    private final Map<Integer, Pair<Integer, Integer>> mobAttackInfo = new HashMap<>();

    private final Map<Integer, Boolean> mobBossCache = new HashMap<>();
    private final Map<Integer, String> mobNameCache = new HashMap<>();

    private MonsterInformationProvider() {
    }

    public final void setMobAttackAnimationTime(int monsterId, int attackPos, int animationTime) {
        mobAttackAnimationTime.put(new Pair<>(monsterId, attackPos), animationTime);
    }

    public final Integer getMobAttackAnimationTime(int monsterId, int attackPos) {
        Integer time = mobAttackAnimationTime.get(new Pair<>(monsterId, attackPos));
        return time == null ? 0 : time;
    }

    public final void setMobSkillAnimationTime(MobSkill skill, int animationTime) {
        mobSkillAnimationTime.put(skill, animationTime);
    }

    public final Integer getMobSkillAnimationTime(MobSkill skill) {
        Integer time = mobSkillAnimationTime.get(skill);
        return time == null ? 0 : time;
    }

    public final void setMobAttackInfo(int monsterId, int attackPos, int mpCon, int coolTime) {
        mobAttackInfo.put((monsterId << 3) + attackPos, new Pair<>(mpCon, coolTime));
    }

    public final Pair<Integer, Integer> getMobAttackInfo(int monsterId, int attackPos) {
        if (attackPos < 0 || attackPos > 7) {
            return null;
        }
        return mobAttackInfo.get((monsterId << 3) + attackPos);
    }

    public static ArrayList<Pair<Integer, String>> getMobsIDsFromName(String search) {
        DataProvider dataProvider = DataProviderFactory.getDataProvider(WZFiles.STRING);
        ArrayList<Pair<Integer, String>> retMobs = new ArrayList<>();
        Data data = dataProvider.getData("Mob.img");
        List<Pair<Integer, String>> mobPairList = new LinkedList<>();
        for (Data mobIdData : data.getChildren()) {
            int mobIdFromData = Integer.parseInt(mobIdData.getName());
            String mobNameFromData = DataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
            mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
        }
        for (Pair<Integer, String> mobPair : mobPairList) {
            if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                retMobs.add(mobPair);
            }
        }
        return retMobs;
    }

    public boolean isBoss(int id) {
        Boolean boss = mobBossCache.get(id);
        if (boss == null) {
            try {
                boss = LifeFactory.getMonster(id).isBoss();
            } catch (NullPointerException npe) {
                boss = false;
            } catch (Exception e) {   //nonexistant mob
                boss = false;

                log.warn("Non-existent mob id {}", id, e);
            }

            mobBossCache.put(id, boss);
        }

        return boss;
    }

    public String getMobNameFromId(int id) {
        String mobName = mobNameCache.get(id);
        if (mobName == null) {
            DataProvider dataProvider = DataProviderFactory.getDataProvider(WZFiles.STRING);
            Data mobData = dataProvider.getData("Mob.img");

            mobName = DataTool.getString(mobData.getChildByPath(id + "/name"), "");
            mobNameCache.put(id, mobName);
        }

        return mobName;
    }
}
