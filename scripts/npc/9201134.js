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
/* Aldol
 *
 * @Author Ronan
 */
const ExpeditionBossLog = Java.type("server.expeditions.ExpeditionBossLog");

var status;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        var eim = cm.getEventInstance();
        if (!eim.isEventCleared()) {
            if (status == 0) {
                cm.sendYesNo("If you leave now, you won't be able to return. \r\n\r\n" +
                cm.getEventInstance().sendDmgDealt(cm.getPlayer().getWorld()) +
                "Are you sure you want to leave?");
            } else if (status == 1) {
                cm.warp(551030100, 2);
                cm.dispose();
            }
        } else {
            if (status == 0) {
                cm.sendNext("You guys defeated both Scarlion and Targa! Wonderful! Take this memento as a prize for your bravery.\r\n\r\n" +
                cm.getEventInstance().sendDmgDealt(cm.getPlayer().getWorld()) +
                "Are you ready to go?");
            } else if (status == 1) {
                if (!eim.giveEventReward(cm.getPlayer())) {
                    cm.sendNext("Please make room on your inventory first!");
                } else {
                    cm.getClient().getWorldServer().removeUnclaimed(ExpeditionBossLog.BossLogEntry.SCARGA, cm.getPlayer().getId());
                    cm.warp(551030100, 2);
                }

                cm.dispose();
            }
        }
    }
}