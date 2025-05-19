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
/* NPC Base
	Map Name (Map ID)
	Extra NPC info.
 */

var ticketCount = 0;
var status;
var ticketId = 5220000;
var mapName = ["Henesys", "Ellinia", "Perion", "Kerning City", "Sleepywood", "Mushroom Shrine", "Showa Spa (M)", "Showa Spa (F)", "Ludibrium", "New Leaf City", "El Nath", "Nautilus"];
var curMapName = "";

function start() {
    status = -1;
    curMapName = mapName[(cm.getNpc() != 9100117 && cm.getNpc() != 9100109) ? (cm.getNpc() - 9100100) : cm.getNpc() == 9100109 ? 9 : 11];

    action(1, 0, 0);
}


function action(mode, type, selection) {
    if (mode < 0) {
        cm.dispose();
    } else {
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0 && mode == 1) {
            if (cm.haveItem(ticketId)) {
                cm.sendGetText("How many Gachapon tickets would you like to use?", "1");
            } else {
                cm.sendSimple("Welcome to the " + curMapName + " Gachapon. How may I help you?\r\n\r\n#L0#What is Gachapon?#l\r\n#L1#Where can you buy Gachapon tickets?#l");
            }

        } else if (status == 1 && cm.haveItem(ticketId)) {
            // Parse and validate input
            var ticketCount = parseInt(cm.getText());

            if (isNaN(ticketCount) || ticketCount <= 0) {
                cm.sendOk("Please enter a valid number of tickets.");
                cm.dispose();
                return;
            }

            if (!cm.haveItem(ticketId, ticketCount)) {
                cm.sendOk("You don't have enough Gachapon tickets.");
                cm.dispose();
                return;
            }

            if (cm.hasEnoughSlotsForGachapon(ticketCount)) {
                cm.doGachaponWithDelay(ticketCount);
                cm.dispose();
            } else {
                cm.sendOk("You need at least " + ticketCount + " free slots in each of your #rEQUIP, USE, SET-UP,#k and #rETC#k inventories to use Gachapon.");
                cm.dispose();
            }

        } else if (status == 1) {
            if (selection == 0) {
                cm.sendNext("Play Gachapon to earn rare scrolls, equipment, chairs, mastery books, and other cool items! All you need is a #bGachapon Ticket#k to be the winner of a random mix of items.");
            } else {
                cm.sendNext("Gachapon Tickets are available in the #rCash Shop#k and can be purchased using NX or Maple Points. Click on the red SHOP at the lower right hand corner of the screen to visit the #rCash Shop#k where you can purchase tickets.");
            }
        } else if (status == 2) {
            cm.sendNextPrev("You'll find a variety of items from the " + curMapName + " Gachapon, but you'll most likely find items and scrolls related to " + curMapName + ".");
        } else {
            cm.dispose();
        }
    }
}