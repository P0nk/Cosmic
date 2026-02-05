/*
    Perry (Ninja Castle) - 9110000
    Function: Teleport to Mushroom Shrine
*/

var status = -1;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) {
        status++;
    } else {
        status--;
    }

    if (status == 0) {
        cm.sendYesNo("Do you want to return to #bMushroom Shrine#k?");
    } else if (status == 1) {
        cm.warp(800000000, 0); // Warp to Mushroom Shrine
        cm.dispose();
    }
}
