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

function getNextSchedules() {
    var now = new Date();
    var currentHour = now.getHours();
    var currentMin = now.getMinutes();

    var schedules = [];
    var baseIntervals = [10, 30, 50];

    // Find next 2 intervals
    var minOffset = 0;
    while (schedules.length < 2) {
        var checkHour = (currentHour + Math.floor((currentMin + minOffset) / 60)) % 24;
        var checkMin = (currentMin + minOffset) % 60;

        for (var i = 0; i < baseIntervals.length; i++) {
            if (checkMin <= baseIntervals[i] && schedules.length < 2) {
                var hrStr = checkHour < 10 ? "0" + checkHour : checkHour;
                var minStr = baseIntervals[i] < 10 ? "0" + baseIntervals[i] : baseIntervals[i];
                schedules.push(hrStr + ":" + minStr);
            }
        }
        minOffset += 60;
        if (schedules.length < 2) {
            currentMin = 0;
            minOffset = 60;
        }
    }
    return schedules;
}

function start() {
    var em = cm.getEventManager("Genie");
    var now = new Date();
    var minutes = now.getMinutes();
    var seconds = now.getSeconds();

    // Ariant Boarding: 10 - 12.5 mins into the 20-min cycle
    // (We use absolute time check)

    var msg = "The desert heat can be unforgiving, but our Magical Genies offer a swift breeze! I sell tickets for the Genie to Orbis.\r\n";
    if (em.getProperty("entry") == "true") {
        msg += "We are currently #bboarding#k for Orbis!\r\n";

        var nextHour = now.getHours();
        var nextMin = 12;
        if (minutes < 12) nextMin = 12;
        else if (minutes < 32) nextMin = 32;
        else if (minutes < 52) nextMin = 52;
        else {
            nextMin = 12;
            nextHour = (nextHour + 1) % 24;
        }

        var hrStr = nextHour < 10 ? "0" + nextHour : nextHour;
        var minStr = nextMin < 10 ? "0" + nextMin : nextMin;

        msg += "The genie will leave at approx #b#e" + hrStr + ":" + minStr + " and 30 secs#k#n.\r\n";
    } else {
        msg += "The genie to Orbis is already travelling.\r\n";
        var schedules = getNextSchedules();
        msg += "The next Genies board at #b#e" + schedules[0] + "#k#n and #b#e" + schedules[1] + "#k#n.\r\n";
    }

    if (cm.haveItem(4031045)) {
        if (em.getProperty("entry") == "true") {
            cm.sendYesNo(msg + "This will not be a short flight, so I suggest you take care of business first. Do you still wish to board the genie?");
        } else {
            cm.sendOk(msg + "I'm sorry, but you'll have to get on the next ride. Please check the schedule.");
            cm.dispose();
        }
    } else {
        cm.sendOk("Make sure you got an Ariant ticket to travel in this genie. Check your inventory.");
        cm.dispose();
    }
}

function action(mode, type, selection) {
    if (mode <= 0) {
        cm.sendOk("Okay, talk to me if you change your mind!");
        cm.dispose();
        return;
    }

    var em = cm.getEventManager("Genie");
    if (em.getProperty("entry") == "true") {
        cm.warp(260000110);
        cm.gainItem(4031045, -1);
    } else {
        cm.sendOk("This genie is getting ready for takeoff. I'm sorry, but you'll have to get on the next ride. The ride schedule is available through the guide at the ticketing booth.");
    }

    cm.dispose();
}