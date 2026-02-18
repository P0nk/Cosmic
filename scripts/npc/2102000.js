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

function start() {
    var em = cm.getEventManager("Genie");
    var now = new Date();
    var minutes = now.getMinutes();
    var seconds = now.getSeconds();
    var totalSeconds = minutes * 60 + seconds;
    var cycleTime = (totalSeconds % 1200); // 20 mins

    // Ariant Boarding: 10 - 12.5 mins (600s - 750s)

    var msg = "";
    if (em.getProperty("entry") == "true") {
        msg = "We are currently #bboarding#k for Orbis!\r\n";
        var timeLeft = 750 - cycleTime;
        var minLeft = Math.ceil(timeLeft / 60);
        msg += "The genie will leave in #b" + minLeft + " minutes#k.\r\n";
    } else {
        msg = "The genie to Orbis is already travelling.\r\n";
        // Next boarding at 10, 30, 50
        // Boarding starts at 10.0 into cycle
        var minIntoCycle = (minutes + (seconds / 60)) % 20;
        var waitTime = 0;
        if (minIntoCycle < 10) {
            waitTime = 10 - minIntoCycle;
        } else {
            waitTime = (20 - minIntoCycle) + 10;
        }
        var waitMin = Math.ceil(waitTime);
        msg += "The next genie will board in #b" + waitMin + " minutes#k.\r\n";
    }

    if (cm.haveItem(4031045)) {
        if (em.getProperty("entry") == "true") {
            cm.sendYesNo(msg + "This will not be a short flight, so you need to take care of some things, I suggest you do that first before getting on board. Do you still wish to board the genie?");
        } else {
            cm.sendOk(msg + "I'm sorry, but you'll have to get on the next ride. The ride schedule is available through the guide at the ticketing booth.");
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