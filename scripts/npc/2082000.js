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
// Corba - Leafre Ticket Vendor

var status = 0;
var cost = 30000;

function start() {
    status = -1;
    action(1, 0, 0);
}

function getNextSchedules() {
    var now = new Date();
    var currentHour = now.getHours();
    var currentMin = now.getMinutes();

    var schedules = [];
    var baseIntervals = [7, 22, 37, 52];

    // Find next 3 intervals
    var minOffset = 0;
    while (schedules.length < 3) {
        var checkHour = (currentHour + Math.floor((currentMin + minOffset) / 60)) % 24;
        var checkMin = (currentMin + minOffset) % 60;

        for (var i = 0; i < baseIntervals.length; i++) {
            if (checkMin <= baseIntervals[i] && schedules.length < 3) {
                var hrStr = checkHour < 10 ? "0" + checkHour : checkHour;
                var minStr = baseIntervals[i] < 10 ? "0" + baseIntervals[i] : baseIntervals[i];
                schedules.push(hrStr + ":" + minStr);
            }
        }
        minOffset += 60; // Jump to next hour to find more
        if (schedules.length < 3) { // Force re-eval for next hour
            currentMin = 0;
            minOffset = 60;
        }
    }

    return schedules;
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (status >= 0 && mode == 0) {
            cm.sendOk("See you next time.");
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;

        if (status == 0) {
            var schedules = getNextSchedules();
            var msg = "The skies above Minar Forest are infested with dragons, so our Cabins must fly swiftly and precisely. I sell tickets for the Cabin to Orbis.\r\n";
            msg += "\r\nWe board four times an hour. The next Cabins leave at #b#e" + schedules[0] + "#k#n, #b#e" + schedules[1] + "#k#n, and #b#e" + schedules[2] + "#k#n.\r\n";
            msg += "\r\nThe ride costs #b" + cost + " mesos#k. Are you sure you want to purchase a #b#t4031045##k?";
            cm.sendYesNo(msg);
        } else if (status == 1) {
            if (cm.getMeso() >= cost && cm.canHold(4031045)) {
                cm.gainItem(4031045, 1);
                cm.gainMeso(-cost);
                cm.dispose();
            } else {
                cm.sendOk("Are you sure you have enough mesos? Please also check if your ETC inventory is full.");
                cm.dispose();
            }
        }
    }
}
