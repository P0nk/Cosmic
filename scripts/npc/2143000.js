/*  Author: Rulax
    NPC Name:       Informant (2143000)
    Map(s):         Knight District 2
    Description:    Hallowed Ground (Dream Key)
*/
var array = ["Hallowed Ground of Dawn", "Hallowed Ground of Blaze", "Hallowed Ground of Wind", "Hallowed Ground of Night", "Hallowed Ground of Thunder"];
function start() {
    status = -1;
    action(1, 0, 0);
}

function action (mode, type, selection) {
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
            let menuSel = "So you want to go to the Hallowed Ground? Oh yeah, we discovered a new Hallowed Ground. I hear the key to the Cygnus Garden can be found there. Please continue doing your best to bring peace to our world.\r\n#b\r\n";
            menuSel += generateSelectionMenu(array);
            cm.sendSimple(menuSel);
        } else if (status == 1) {
            var mapid = 0;

            switch (selection) {
                case 0:
                    mapid = 271030201;
                    break;
                case 1:
                    mapid = 271030202;
                    break;
                case 2:
                    mapid = 271030203;
                    break;
                case 3:
                    mapid = 271030204;
                    break;
                case 4:
                    mapid = 271030205;
                    break;
            }

            if (mapid > 0) {
                cm.warp(mapid, 1);
            }
            cm.dispose();
        }
    }
}

function generateSelectionMenu(array) {
    var menu = "";

    var len = Math.min(array.length);
    for (var i = 0; i < len; i++) {
        menu += "#L" + i + "#" + array[i] + "#l\r\n";
    }
    return menu;
}