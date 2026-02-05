/*
    Nobuo - 9110114
    Function: Lore (Lost Dog)
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
            cm.sendNext("Have you seen a small dog running around?");
        } else if (status == 1) {
            cm.sendNextPrev("He ran through the castle gates! It's too dangerous for me to go in there.");
        } else if (status == 2) {
            cm.sendPrev("If you see him, please... scare him back out to safety!");
        } else if (status == 3) {
            cm.dispose();
        }
    }
}
