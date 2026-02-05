/*
    Daisuke - 9110111
    Function: Sick Guard (Lore)
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
            cm.sendNext("Ugh... my stomach...");
        } else if (status == 1) {
            cm.sendPrev("I think I ate some bad mushroom tempura. Please... don't tell Sasuke I'm slacking off.");
        } else if (status == 2) {
            cm.dispose();
        }
    }
}
