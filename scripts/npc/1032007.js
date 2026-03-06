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
// Cherry (Ellinia Ticket Vendor)

var status = 0;
var cost = 5000;

function start() {
    status = -1;
    action(1, 0, 0);
}

function getNextSchedules() {
    var now = new Date();
    var currentHour = now.getHours();
    var currentMin = now.getMinutes();

    var schedules = [];
    var calcHour1 = currentHour;

    // Boarding stops at XX:10
    if (currentMin >= 10) {
        calcHour1 = (currentHour + 1) % 24;
    }

    var calcHour2 = (calcHour1 + 1) % 24;

    var hrStr1 = calcHour1 < 10 ? "0" + calcHour1 : calcHour1;
    var hrStr2 = calcHour2 < 10 ? "0" + calcHour2 : calcHour2;

    schedules.push(hrStr1 + ":00");
    schedules.push(hrStr2 + ":00");

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
            var msg = "Welcome to the magic-infused town of Ellinia! I sell tickets for the flight to Orbis.\r\n";
            msg += "\r\nThe boat to Orbis departs every hour, on the hour. Due to recent Crimson Balrog sightings, boarding is strictly limited to the first 10 minutes.\r\n";
            msg += "\r\nOur next available departures are #b#e" + schedules[0] + "#k#n and #b#e" + schedules[1] + "#k#n. ";
            msg += "The ride costs #b" + cost + " mesos#k. Are you sure you want to purchase a #b#t4031045##k?";
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
