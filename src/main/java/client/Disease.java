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
package client;

import constants.game.GameConstants;
import server.life.MobSkillType;

import java.util.Arrays;

public enum Disease {
    NULL(0x0),
    SLOW(0x1, MobSkillType.SLOW),
    SEDUCE(0x80, MobSkillType.SEDUCE),
    FISHABLE(0x100),
    ZOMBIFY(0x4000),
    STOP_POTION(0x100000L, MobSkillType.STOP_POTION),
    CONFUSE(0x80000, MobSkillType.REVERSE_INPUT),
    STUN(0x2000000000000L, MobSkillType.STUN),
    POISON(0x4000000000000L, MobSkillType.POISON),
    SEAL(0x8000000000000L, MobSkillType.SEAL),
    DARKNESS(0x10000000000000L, MobSkillType.DARKNESS),
    WEAKEN(0x4000000000000000L, MobSkillType.WEAKNESS),
    CURSE(0x8000000000000000L, MobSkillType.CURSE);

    private final long i;
    private final MobSkillType mobSkillType;

    Disease(long i) {
        this(i, null);
    }

    Disease(long i, MobSkillType skill) {
        this.i = i;
        this.mobSkillType = skill;
    }

    public long getValue() {
        return i;
    }

    public boolean isFirst() {
        return false;
    }

    public MobSkillType getMobSkillType() {
        return mobSkillType;
    }

    public static Disease ordinal(int ord) {
        try {
            return Disease.values()[ord];
        } catch (IndexOutOfBoundsException io) {
            return NULL;
        }
    }

    public static final Disease getRandom() {
        Disease[] diseases = GameConstants.CPQ_DISEASES;
        return diseases[(int) (Math.random() * diseases.length)];
    }

    public static final Disease getBySkill(MobSkillType skill) {
        if (skill == null) {
            return null;
        }
        return Arrays.stream(Disease.values())
                .filter(d -> d.mobSkillType == skill)
                .findAny()
                .orElse(null);
    }

}