/*
    Hoshi - 9110113
    Function: Lore (Spinning Top)
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
            cm.sendNext("Look at it spin! It never stops!");
        } else if (status == 1) {
            cm.sendPrev("I've been practicing every day. One day I'll be the best top spinner in all of Zipangu!");
        } else if (status == 2) {
            cm.dispose();
        }
    }
}
