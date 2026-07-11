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
/*      Rulax Rebirth item exchanger
*/
var status = 0;

function start() {
    cm.sendNext("Adventurers like you would like to take a look at what i have to offer..");
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
    } else {
        status++;
        if (status == 1) {
            cm.sendSimple("The following sets are available for crafting:" +
            "\r\n#L0# #v4033446:# #k#l\r\n#L1# #v4033442:# #k#l\r\n#L2# #v4033450:# #k#l");
        } else if (status == 2) {
            if (selection == 0) {
                cm.sendSimple("Do you have \r\n#L0# 1 #v4032133:# and 1 #v4001094:# #k#l");

            } else if (selection == 1) {
                cm.sendSimple("Do you have \r\n#L1# 1 #v4021010:# and 1 #v4001693:# #k#l");

            } else if (selection == 2) {
                cm.sendSimple("Do you have \r\n#L2# 1 #v4000659:# and 1 #v4032922:# #k#l");

            }
        } else if (status == 3) {
            if (selection == 0) {
                if (cm.haveItem(4032133) && cm.haveItem(4001094)) {
                    cm.gainItem(4032133, -1);
                    cm.gainItem(4001094, -1);
                    cm.gainItem(4033446, 1);
                    cm.sendOk("Fair trade adventurer..");
                    return
                }
                cm.sendOk("You are lacking one of the items, you think this is a joke?!");
                cm.dispose();
            } 
            if (selection == 1) {
                if (cm.haveItem(4021010) && cm.haveItem(4001693)) {
                    cm.gainItem(4021010, -1);
                    cm.gainItem(4001693, -1);
                    cm.gainItem(4033442, 1);
                    cm.sendOk("Fair trade adventurer..");
                    return
                }
                cm.sendOk("You are lacking one of the items, you think this is a joke?!");
                cm.dispose();
            } 
            if (selection == 2) {
                if (cm.haveItem(4000659) && cm.haveItem(4032922)) {
                    cm.gainItem(4000659, -1);
                    cm.gainItem(4032922, -1);
                    cm.gainItem(4033450, 1);
                    cm.sendOk("Fair trade adventurer..");
                    return
                }
                cm.sendOk("You are lacking one of the items, you think this is a joke?!");
                cm.dispose();
            } 
            
        }
    }
}