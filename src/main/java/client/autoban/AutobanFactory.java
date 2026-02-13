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

package client.autoban;

import client.Character;
import config.YamlConfig;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.PacketCreator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author kevintjuh93
 */
public enum AutobanFactory {
    MOB_COUNT,
    GENERAL,
    FIX_DAMAGE,
    DAMAGE_HACK(15, MINUTES.toMillis(1)),
    DISTANCE_HACK(10, MINUTES.toMillis(2)),
    PORTAL_DISTANCE(5, SECONDS.toMillis(30)),
    PACKET_EDIT,
    ACC_HACK,
    CREATION_GENERATOR,
    HIGH_HP_HEALING,
    FAST_HP_HEALING(15),
    FAST_MP_HEALING(20, SECONDS.toMillis(30)),
    GACHA_EXP,
    TUBI(20, SECONDS.toMillis(15)),
    SHORT_ITEM_VAC,
    ITEM_VAC,
    FAST_ITEM_PICKUP(5, SECONDS.toMillis(30)),
    FAST_ATTACK(10, SECONDS.toMillis(30)),
    MPCON(25, SECONDS.toMillis(30));

    private static final Logger log = LoggerFactory.getLogger(AutobanFactory.class);
    private static final Set<Integer> ignoredChrIds = new HashSet<>();

    private final int points;
    private final long expiretime;

    AutobanFactory() {
        this(1, -1);
    }

    AutobanFactory(int points) {
        this.points = points;
        this.expiretime = -1;
    }

    AutobanFactory(int points, long expire) {
        this.points = points;
        this.expiretime = expire;
    }

    public int getMaximum() {
        return points;
    }

    public long getExpire() {
        return expiretime;
    }

    public void alert(Character chr, String reason) {
        if (YamlConfig.config.server.USE_AUTOBAN) {
            if (chr != null && isIgnored(chr.getId())) {
                return;
            }
            Server.getInstance().broadcastGMMessage((chr != null ? chr.getWorld() : 0), PacketCreator.sendYellowTip((chr != null ? Character.makeMapleReadable(chr.getName()) : "") + " caused " + this.name() + " " + reason));
        }
        if (YamlConfig.config.server.USE_AUTOBAN_LOG) {
            final String chrName = chr != null ? Character.makeMapleReadable(chr.getName()) : "";
            log.info("Autoban alert - chr {} caused {}-{}", chrName, this.name(), reason);
        }
    }

    /**
     * Toggle ignored status for a character id.
     * An ignored character will not trigger GM alerts.
     *
     * @return new status. true if the chrId is now ignored, otherwise false.
     */
    public static boolean toggleIgnored(int chrId) {
        if (ignoredChrIds.contains(chrId)) {
            ignoredChrIds.remove(chrId);
            return false;
        } else {
            ignoredChrIds.add(chrId);
            return true;
        }
    }

    private static boolean isIgnored(int chrId) {
        return ignoredChrIds.contains(chrId);
    }

    public static Collection<Integer> getIgnoredChrIds() {
        return ignoredChrIds;
    }
}
