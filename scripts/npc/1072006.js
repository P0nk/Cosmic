/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
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

/**
 -- Odin JavaScript --------------------------------------------------------------------------------
 Bowman Job Instructor - Ant Tunnel For Bowman (108000100)
 ---------------------------------------------------------------------------------------------------
 **/

var status;
var completed = false;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && type > 0) {
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0) {
            // Check for marbles (provided by Outer NPC for reborns)
            if (cm.haveItem(4031013, 30)) {
                completed = true;
                cm.sendNext("You're a true hero! Take this and Athena will acknowledge you.");
            } else if (cm.getPlayer().getReborns() > 0 && cm.getJob().getId() == 300) {
                completed = true;
                cm.sendNext("I trained you before! I know you can pass this test easily. I will let you pass now but dont tell my colleague!");
            } else {
                completed = false;
                cm.sendSimple("You will have to collect me #b30 #t4031013##k. Good luck. \r\n#b#L1#I would like to leave#l");
            }
        } else if (status == 1) {
            if (completed) {
                cm.removeAll(4031013); // Remove Marbles

                // [REBIRTH OPTIMIZATION]
                // Only update quests if first time. Reborns skip to avoid DB errors.
                if (cm.getPlayer().getReborns() == 0) {
                    cm.completeQuest(100001);
                    cm.startQuest(100002);
                }

                // Always give Proof of Hero
                if (!cm.haveItem(4031012)) {
                    cm.gainItem(4031012, 1);
                }
            }

            // Warp back to Warning Street (The Road to the Dungeon)
            cm.warp(106010000, 9);
            cm.dispose();
        }
    }
}