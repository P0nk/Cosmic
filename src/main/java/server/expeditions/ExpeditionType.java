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

package server.expeditions;

import config.YamlConfig;
import server.expeditions.ExpeditionBossLog.BossLogEntry;

/**
 * @author Alan (SharpAceX)
 */

public enum ExpeditionType {
    BALROG_EASY(3, 30, 50, 255, 5),
    BALROG_NORMAL(6, 30, 50, 255, 5),
    SCARGA(6, 30, 100, 255, 5),
    PAPULATUS(1, 30, 80, 255, 5),
    SHOWA(3, 30, 100, 255, 5),
    ZAKUM(6, 30, 50, 255, 5),
    HORNTAIL(6, 30, 100, 255, 5),
    CHAOS_ZAKUM(6, 30, 120, 255, 5),
    CHAOS_HORNTAIL(6, 30, 120, 255, 5),
    ARIANT(2, 7, 20, 255, 5),
    ARIANT1(2, 7, 20, 255, 5),
    ARIANT2(2, 7, 20, 255, 5),
    PINKBEAN(6, 30, 120, 255, 5),
    CWKPQ(6, 30, 90, 255, 5),   // CWKPQ min-level 90, found thanks to Cato
    VONLEON(6, 30, 120, 255, 5),
    CYGNUS(6, 30, 120, 255, 5),
    WILLSPIDER(6, 30, 200, 255, 5),
    VERUS(6, 30, 200, 255, 5),
    DARKNELL(6, 30, 200, 255, 5),
    KREXEL(6, 30, 120, 255, 5),
    CASTELLAN(6, 30, 120, 255, 5),
    LUCID(6, 30, 200, 255, 5),
    NORMALLOTUS(6, 30, 200, 255, 5),
    HARDLOTUS(6, 30, 190, 255, 5),
    EASYMAGNUS(6, 30, 150, 255, 5),
    NORMALMAGNUS(6, 30, 170, 255, 5),
    HARDMAGNUS(6, 30, 200, 255, 5),
    NWILLSPIDER(6, 30, 200, 255, 5),
    NLUCID(6, 30, 200, 255, 5),
    NVERUS(6, 30, 200, 255, 5),
    NDARKNELL(6, 30, 200, 255, 5),
    MAGNUS(6, 30, 200, 255, 5);

    public static BossLogEntry typeToEntry(ExpeditionType type) {

        if (type == ZAKUM)
            return BossLogEntry.ZAKUM;
        if (type == HORNTAIL)
            return BossLogEntry.HORNTAIL;
        if (type == PINKBEAN)
            return BossLogEntry.PINKBEAN;
        if (type == VONLEON)
            return BossLogEntry.VONLEON;
        if (type == CYGNUS)
            return BossLogEntry.CYGNUS;
        if (type == KREXEL)
            return BossLogEntry.KREXEL;
        if (type == CASTELLAN)
            return BossLogEntry.CASTELLAN;
        if (type == NLUCID)
            return BossLogEntry.NLUCID;
        if (type == NWILLSPIDER)
            return BossLogEntry.NWILLSPIDER;
        if (type == NDARKNELL)
            return BossLogEntry.NDARKNELL;
        if (type == NVERUS)
            return BossLogEntry.NVERUS;
        if (type == LUCID)
            return BossLogEntry.LUCID;
        if (type == WILLSPIDER)
            return BossLogEntry.WILLSPIDER;
        if (type == DARKNELL)
            return BossLogEntry.DARKNELL;
        if (type == VERUS)
            return BossLogEntry.VERUS;
        if (type == MAGNUS)
            return BossLogEntry.MAGNUS;
        if (type == SCARGA)
            return BossLogEntry.SCARGA;
        if (type == PAPULATUS)
            return BossLogEntry.PAPULATUS;
        return null;
    }
    private final int minSize;
    private final int maxSize;
    private final int minLevel;
    private final int maxLevel;
    private final int registrationMinutes;

    ExpeditionType(int minSize, int maxSize, int minLevel, int maxLevel, int minutes) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.registrationMinutes = minutes;
    }

    public int getMinSize() {
        return !YamlConfig.config.server.USE_ENABLE_SOLO_EXPEDITIONS ? minSize : 1;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getRegistrationMinutes() {
        return registrationMinutes;
    }
}
