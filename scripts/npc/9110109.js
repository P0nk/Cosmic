/*
    Sai - 9110109
    Function: Ninja Interaction
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
            cm.sendNext("Shh... I'm on a covert mission.");
        } else if (status == 1) {
            cm.sendNextPrev("The Kunoichis have overrun the lower levels. If you're heading that way, thin their numbers for me, will you?");
        } else if (status == 2) {
            cm.sendPrev("I'll be watching from the shadows.");
        } else if (status == 3) {
            cm.dispose();
        }
    }
}
