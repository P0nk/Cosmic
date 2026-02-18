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
 Shalon - Ticketing Usher
 -- By ---------------------------------------------------------------------------------------------
 Whoever written this script
 -- Version Info -----------------------------------------------------------------------------------
 1.0 - First Version by Whoever written this script
 2.0 - Second Version by Jayd
 ---------------------------------------------------------------------------------------------------
 **/

status = -1;
oldSelection = -1;

function start() {
    var now = new Date();
    var minutes = now.getMinutes();
    var msg = "Hello, I am Shalon from Singapore Airport.\r\n";

    // CBD Boarding: :30 - :40
    // Flight: :40 - :55

    if (minutes >= 30 && minutes < 40) {
        msg += "We are currently #bboarding#k for Kerning City!\r\nThe plane will take off in " + (40 - minutes) + " mins.\r\n";
    } else {
        msg += "We are currently #rnot boarding#k.\r\n";
        var nextFlightIn = 0;
        if (minutes < 40) {
            nextFlightIn = 40 - minutes; // Before 40, flight is at 40
        } else {
            nextFlightIn = (60 - minutes) + 40; // After 40, flight is next hour :40
        }
        msg += "The next flight will be in #b" + nextFlightIn + " mins#k.\r\n";
    }

    msg += "Do you want to go to Kerning City?";
    cm.sendSimple(msg + "\r\n#b#L0#I would like to buy a plane ticket to Kerning City\r\n#b#L1#Let me go in to the departure point.");
}

function action(mode, type, selection) {
    status++;
    if (mode <= 0) {
        oldSelection = -1;
        cm.dispose();
    }

    if (status == 0) {
        if (selection == 0) {
            cm.sendYesNo("The ticket will cost you 5,000 mesos. Will you purchase the ticket?");
        } else if (selection == 1) {
            cm.sendYesNo("Would you like to go in now? You will lose your ticket once you go in! Thank you for choosing Wizet Airline.");
        }
        oldSelection = selection;
    } else if (status == 1) {
        if (oldSelection == 0) {
            if (cm.getPlayer().getMeso() > 4999 && !cm.getPlayer().haveItem(4031732)) {
                if (cm.getPlayer().canHold(4031732, 1)) {
                    cm.gainMeso(-5000);
                    cm.gainItem(4031732);
                    cm.sendOk("Thank you for choosing Wizet Airline! Enjoy your flight!");
                    cm.dispose();
                } else {
                    cm.sendOk("You don't have a free slot on your ETC inventory for the ticket, please make a room beforehand.");
                    cm.dispose();
                }
            } else {
                cm.sendOk("You do not have enough mesos or you've already purchased a ticket.");
                cm.dispose();
            }
        } else if (oldSelection == 1) {
            if (cm.itemQuantity(4031732) > 0) {
                var em = cm.getEventManager("AirPlane");
                if (em.getProperty("entry") == "true" && em.getProperty("location") == "cbd") {
                    cm.warp(540010001);
                    cm.gainItem(4031732, -1);
                } else {
                    if (em.getProperty("entry") == "true" && em.getProperty("location") == "kerning") {
                        cm.sendOk("The plane is currently boarding at Kerning City. Please wait for it to arrive.");
                    } else {
                        cm.sendOk("Sorry the plane has taken off, please wait a few minutes.");
                    }
                }
            } else {
                cm.sendOk("You need a #b#t4031732##k to get on the plane!");
            }
        }
        cm.dispose();
    }
}