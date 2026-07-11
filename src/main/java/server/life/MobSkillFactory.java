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

import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Danny (Leifde)
 */
public class MobSkillFactory {
    private static final Map<String, MobSkill> mobSkills = new HashMap<>();
    private static final DataProvider dataSource = DataProviderFactory.getDataProvider(WZFiles.SKILL);
    private static final Data skillRoot = dataSource.getData("MobSkill.img");
    private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private static final Lock readLock = readWriteLock.readLock();
    private static final Lock writeLock = readWriteLock.writeLock();

    public static MobSkill getMobSkillOrThrow(MobSkillType type, int level) {
        return getMobSkill(type, level).orElseThrow(
                () -> new IllegalArgumentException("No MobSkill exists for type %s, level %d".formatted(type, level))
        );
    }

    public static Optional<MobSkill> getMobSkill(final MobSkillType type, final int level) {
        readLock.lock();
        try {
            MobSkill ms = mobSkills.get(createKey(type, level));
            if (ms != null) {
                return Optional.of(ms);
            }
        } finally {
            readLock.unlock();
        }

        return loadMobSkill(type, level);
    }

    private static Optional<MobSkill> loadMobSkill(final MobSkillType type, final int level) {
        writeLock.lock();
        try {
            MobSkill existingMs = mobSkills.get(createKey(type, level));
            if (existingMs != null) {
                return Optional.of(existingMs);
            }

            Data skillData = skillRoot.getChildByPath("%d/level/%d".formatted(type.getId(), level));
            if (skillData == null) {
                return Optional.empty();
            }

            int mpCon = DataTool.getInt("mpCon", skillData, 0);
            List<Integer> toSummon = new ArrayList<>();
            for (int i = 0; i > -1; i++) {
                if (skillData.getChildByPath(String.valueOf(i)) == null) {
                    break;
                }
                toSummon.add(DataTool.getInt(skillData.getChildByPath(String.valueOf(i)), 0));
            }
            int effect = DataTool.getInt("summonEffect", skillData, 0);
            int hp = DataTool.getInt("hp", skillData, 100);
            int x = DataTool.getInt("x", skillData, 1);
            int y = DataTool.getInt("y", skillData, 1);
            int count = DataTool.getInt("count", skillData, 1);
            long duration = SECONDS.toMillis(DataTool.getInt("time", skillData, 0));
            long cooltime = SECONDS.toMillis(DataTool.getInt("interval", skillData, 0));
            int iprop = DataTool.getInt("prop", skillData, 100);
            float prop = iprop / 100;
            int limit = DataTool.getInt("limit", skillData, 0);

            Data ltData = skillData.getChildByPath("lt");
            Data rbData = skillData.getChildByPath("rb");
            Point lt = null;
            Point rb = null;
            if (ltData != null && rbData != null) {
                lt = (Point) ltData.getData();
                rb = (Point) rbData.getData();
            }

            MobSkill loadedMobSkill = new MobSkill.Builder(type, level)
                    .mpCon(mpCon)
                    .toSummon(toSummon)
                    .cooltime(cooltime)
                    .duration(duration)
                    .hp(hp)
                    .x(x)
                    .y(y)
                    .count(count)
                    .prop(prop)
                    .limit(limit)
                    .lt(lt)
                    .rb(rb)
                    .build();

            mobSkills.put(createKey(type, level), loadedMobSkill);
            return Optional.of(loadedMobSkill);
        } finally {
            writeLock.unlock();
        }
    }

    private static String createKey(MobSkillType type, int skillLevel) {
        return type.getId() + "" + skillLevel;
    }
}
