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
/* Don Giovanni
	Kerning VIP Hair/Hair Color Change.

        GMS-like revised by Ronan -- contents found thanks to Mitsune (GamerBewbs), Waltzing, AyumiLove
*/
var status = 0;
var beauty = 0;
var faceprice = 1000000;
var facecolorprice = 1000000;
var mface_v = Array(
20000,20002,20003,20004,20005,20006,20007,20008,20009,20010,
20011,20012,20013,20014,20015,20016,20017,20018,20019,20020,
20021,20022,20023,20024,20025,20026,20027,20028,20029,20030, // Page 1
20000,20032,20033,20035,20036,20037,20038,20040,20041,20042,
20043,20044,20045,20046,20047,20048,20049,20050,20051,20052,
20053,20054,20055,20056,20057,20058,20059,20060,20061,20062, // Page 2
20000,20064,20065,20066,20067,20068,20069,20070,20071,20072,
20073,20074,20075,20076,20077,20078,20079,20080,20081,20082,
20083,20084,20085,20086,20087,20088,20091,20092,20093,21000, // Page 3
20000,21002,21003,21004,21005,21006,21007,21008,21009,21010,
21011,21012,21013,21014,21015,21016,21017,21018,21019,21020,
21021,21022,21023,21024,21025,21026,21027,21028,21029,21030, // Page 4
20000,21033,21034,21035,21036,21037,21038,21039,21040,21041,
21042,21043,21044,21045,21046,21047,21048,21049,21050,21052,
21053,21054,21055,21056,21057,21058,21059,21060,21061,21062, // Page 5
//20000,21064,21065,21066,21067,21068,21069,21070,21071,21072,
//21073,21074,21075,21076,21077,21078,21080,21081,21082,21083,
//21084,21085,21087,21088
);
var fface_v = mface_v;
var facenew = Array();

function pushIfItemExists(array, itemid) {
//    console.log(cm.getCosmeticItem(itemid))
//    console.log(!cm.isCosmeticEquipped(itemid))
    if ((itemid = cm.getCosmeticItem(itemid)) != -1 && !cm.isCosmeticEquipped(itemid)) {
        array.push(itemid);
    }
}

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode < 1)  // disposing issue with stylishs found thanks to Vcoc
    {
        cm.dispose();
    } else {
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0) {
            var msg = "You can't change your life, but you can change your face." +
                      "So what's your choice?\r\n"
            var choices = "#b#L0#I want the power to change my face.#l\r\n"+
                          "#b#L1#My eye color is not my identity.#l"
            cm.sendSimple(msg + choices);
        } else if (status == 1) {
            if (selection == 0) { // Style hair - Show the pages
                beauty = 1;
                var lines    = [];
                const perRow = 4;
                const max = Math.floor(mface_v.length / 30);
                for (var i=0; i <= max; i++) {
                    lines.push(
                    "#b#L" + i + "#" +
                    "Page: " + (i + 1) +
                    "#l"
                    );
                }
                const rows = [];
                for (let i = 0; i < lines.length; i += perRow) {
                  const chunk = lines.slice(i, i + perRow);
                  rows.push(chunk.join("   "));    // three spaces between each link
                }
                const grid = rows.join("\r\n");
                cm.sendSimple("So what face do you want? Here's my catalogue.\r\nEach face cost " + faceprice + " nx!\r\n" + grid);
            } else if (selection == 1) { // Color Hair
                status = 1;
                beauty = 2;
                action(1,0,0);
            }
        } else if (status == 2) {
            if (beauty == 1) {
                page = selection;
                facenew = hairSelection(page)
        //            console.log(facenew)
                cm.sendStyle("Control your face with the power of #b"
                + faceprice +"nx#k I'll change it for you. Choose the one to your liking~.", facenew);
            } else if (beauty == 2) {
                facecolor = Array();
                beauty = 2;
                var current = parseInt(cm.getPlayer().getFace() / 10) * 10;
                for (var i = 0; i < 9; i++) {
                    pushIfItemExists(facecolor, current + i);
                }
                cm.sendStyle("I can totally change your facecolor and make it look so good. Why don't you change it up a bit? With #b"+facecolorprice+"k nx#k I'll change it for you. Choose the one to your liking.", facecolor)
            }
        } else if (status == 3) {
            cm.dispose();
            if (beauty == 1) {
                if (cm.getCashShop().getCash(1) >= faceprice) {
                    cm.setFace(facenew[selection]);
                    cm.gainCash(-faceprice);
                    cm.sendOk("Enjoy your new and improved hairstyle!");
                } else {
                    cm.sendOk("Hmmm...it looks like you don't have enough NX...I'm afraid I can't give you a haircut without it. I'm sorry...");
                }
            } else if (beauty == 2) {
                if (cm.getCashShop().getCash(1) >= facecolorprice) {
                    cm.setFace(facecolor[selection]);
                    cm.gainCash(-facecolorprice);
                    cm.sendOk("Enjoy your new and improved hair color!");
                } else {
                    cm.sendOk("Hmmm...it looks like you don't have enough NX...I'm afraid I can't dye your hair without it. I'm sorry...");
                }
            }
        }
    }
}

function hairSelection(page) {
    facenew = Array();
    const start   = page * 30;
    const end     = Math.min(start + 30, mface_v.length);
    const hairMod = cm.getPlayer().getHair() % 10;
    for (let i = start; i < end; i++) {
//        console.log(mface_v[i])
        pushIfItemExists(facenew, mface_v[i] + hairMod);
    }
    return facenew;
}