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
/**
 *@author Ronan
 *party3_r4pt
 */
/**
 * Reference: https://www.youtube.com/watch?v=XgjoE9jelfI&t=255s
 *
 * ----------------------------------------------------------------------------
 *
 * How to test quickly:
 * 1) Go to Map 200080101 [Orbis: The Unknown Tower] (Entrance to PQ)
 * 2) Create a party
 * 3) Talk to Wonky to start the PQ (must be started for getEventInstance() to work properly)
 * 4) Go to Map 920010700 [Hidden Street: Tower of Goddess <On the Way Up>] (which runs this script)
 * 5) Jump to the top, checking at least 1 correct and 1 incorrect portals in each row
 *
 * ----------------------------------------------------------------------------
 *
 * Map Layout (Portal Names)
 *      pt03
 * rp161    rp162   rp163   rp164 |
 * rp151    rp152   rp153   rp154 |- Block 3 | Left
 * rp141    rp142   rp143   rp144 |
 * rp131    rp132   rp133   rp134 |
 *      pt02
 * rp121    rp122   rp123   rp124 |
 * rp111    rp112   rp113   rp114 |- Block 2 | Right
 * rp101    rp102   rp103   rp104 |
 * rp091    rp092   rp093   rp094 |
 *      pt01
 * rp081    rp082   rp083   rp084 |
 * rp071    rp072   rp073   rp074 |- Block 1 | Left
 * rp061    rp062   rp063   rp064 |
 * rp051    rp052   rp053   rp054 |
 *      pt00
 * rp041    rp042   rp043   rp044 |
 * rp031    rp032   rp033   rp034 |- Block 0 | Right
 * rp021    rp022   rp023   rp024 |
 * rp011    rp012   rp013   rp014 |
 *      r6fail
 *
 * A successful portal (climb) takes the player 1 row higher,
 *     and places them at the 1st or 4th portal depending on the block (L or R above)
 *
 * A fail portal (fall) takes the player to the previous checkpoint - r6fail, pt00, pt01, pt02
 */

const zeroPad = (num, places) => String(num).padStart(places, '0')

function enter(pi) {
    var eim = pi.getEventInstance();

    // Set up secret combination if entering a portal for the first time
    if (eim.getProperty("stage6_comb") == null) {
        var comb = "0";

        for (var i = 0; i < 16; i++) {
            var r = Math.floor((Math.random() * 4)) + 1;
            comb += r.toString();
        }

        eim.setProperty("stage6_comb", comb);
    }

    var comb = eim.getProperty("stage6_comb");

    var name = pi.getPortal().getName().substring(2, 5);
    var portalId = parseInt(name, 10);

    var pRow = Math.floor(portalId / 10);
    var pCol = portalId % 10;
    var pBlock = ((pRow - 1) / 4) | 0; // cast to integer

    var destPortalName;
    var destSuccessColumn = "1";
    if (pBlock == 0 || pBlock == 2) {
        destSuccessColumn = "4";
    }

    if (pCol == parseInt(comb.substring(pRow, pRow + 1), 10)) {
        // Success - climb
        switch (pRow) {
            case 16: destPortalName = "pt03"; break;
            case 12: destPortalName = "pt02"; break;
            case  8: destPortalName = "pt01"; break;
            case  4: destPortalName = "pt00"; break;
            default: destPortalName = "rp" + zeroPad(pRow+1, 2) + destSuccessColumn;
        }
    } else {
        // Fail - fall
        switch (pBlock) {
            case 3: destPortalName = "pt02"; break;
            case 2: destPortalName = "pt01"; break;
            case 1: destPortalName = "pt00"; break;
            case 0: destPortalName = "r6fail"; break;
        }
    }

    pi.playPortalSound();
    pi.warp(pi.getMapId(), pi.getMap().getPortal(destPortalName).getId());
    return true;
}