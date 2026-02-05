/*
    Akai - 9110103
    Function: Lore (Lady on Bench)
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
            cm.sendNext("The cherry blossoms are beautiful this time of year, aren't they?");
        } else if (status == 1) {
            cm.sendNextPrev("But... have you noticed those strange men in black running around? They seem to be coming from the castle.");
        } else if (status == 2) {
            cm.sendPrev("I worry for the safety of our shrine. Please be careful if you head that way.");
        } else if (status == 3) {
            cm.dispose();
        }
    }
}
