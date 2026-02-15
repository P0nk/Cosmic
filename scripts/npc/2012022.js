/*
    Palace (2012022) - Orbis Waiting Area Guide
*/

var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0) {
            var now = java.lang.System.currentTimeMillis();
            var cycleTime = now % 900000; // 15 min cycle
            var departureTime = 150000; // 2.5 mins

            var waitTime = 0;
            if (cycleTime < departureTime) {
                waitTime = departureTime - cycleTime;
            } else {
                // Should not happen if warped correctly, but handling login/glitch
                // Next departure is (15 - cycle) + 2.5
                waitTime = (900000 - cycleTime) + departureTime;
            }

            var min = Math.ceil(waitTime / 60000);
            if (min < 1) min = 1;

            var timeMsg = "The ship to Leafre will depart in approximately #b" + min + " minutes#k. ";
            cm.sendYesNo(timeMsg + "Do you wish to leave the waiting room?");
        } else if (status == 1) {
            cm.sendNext("Alright, see you next time. Take care.");
        } else if (status == 2) {
            cm.warp(200000131, 0); // Back to Orbis Station
            cm.dispose();
        }
    }
}
