/*
    Hyottoko - 9110104
    Function: Lore (Old Man in Mask)
*/

var status = -1;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
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
            cm.sendNext("Hehehe... the Shogun is watching.");
        } else if (status == 1) {
            cm.sendPrev("This castle is filled with traps. One wrong step, and... BAM! Hehehe!");
        } else if (status == 2) {
            cm.dispose();
        }
    }
}
