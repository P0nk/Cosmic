/*
    Fuji - 9110108
    Function: Lore (Falconer)
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
            cm.sendNext("Fujihawk sees everything.");
        } else if (status == 1) {
            cm.sendPrev("The path ahead is treacherous. The Master of this castle waits at the very top.");
        } else if (status == 2) {
            cm.dispose();
        }
    }
}
