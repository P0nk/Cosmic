/*
    Sasuke - 9110110
    Function: Guard (Lore)
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
            cm.sendNext("Halt! Keep your eyes open. Suspicious characters have been spotted recently.");
        } else if (status == 1) {
            cm.sendPrev("Daisuke over there isn't feeling well, so I have to do double the work watching the entrance.");
        } else if (status == 2) {
            cm.dispose();
        }
    }
}
