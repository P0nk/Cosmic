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
            var em = cm.getEventManager("Cabin");
            var timeMsg = "";

            if (em != null && em.getProperty("dockedTime") != null) {
                var dockedTime = parseInt(em.getProperty("dockedTime"));
                var now = java.lang.System.currentTimeMillis();
                var departureTime = dockedTime + (5 * 60 * 1000); // 5 mins after docking
                var diff = departureTime - now;
                var min = Math.ceil(diff / 60000);

                if (min < 1) min = 1;

                timeMsg = "The ship to Leafre will depart in approximately " + min + " minutes. ";
            } else {
                timeMsg = "The ship is preparing for departure. ";
            }

            cm.sendYesNo(timeMsg + "Do you wish to leave the waiting room?");
        } else if (status == 1) {
            cm.sendNext("Alright, see you next time. Take care.");
        } else if (status == 2) {
            cm.warp(200000131, 0); // Back to Orbis Station
            cm.dispose();
        }
    }
}
