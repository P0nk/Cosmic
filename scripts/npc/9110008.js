/*
    Perry (Mushroom Shrine) - 9110008
    Function: Teleport to Ninja Castle
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
        cm.sendYesNo("Do you want to travel to #bNinja Castle#k? It's a dangerous place, but the palanquin is ready.");
    } else if (status == 1) {
        cm.warp(800040000, 0); // Warp to Ninja Castle
        cm.dispose();
    }
}
