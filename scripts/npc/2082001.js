/*
    Leafre Station - To Orbis Guide (2082001)
*/

function start() {
    status = -1;
    var em = cm.getEventManager("Cabin");

    if (em == null) {
        cm.sendOk("The event script is missing. Please report this to an admin.");
        cm.dispose();
        return;
    }

    if (em.getProperty("entry") == "false") {
        var dockedTime = em.getProperty("dockedTime");
        var now = java.lang.System.currentTimeMillis();
        var msg = "The flight to Orbis is currently in progress or preparing for takeoff.\r\n";

        if (dockedTime != null) {
            var diff = now - parseInt(dockedTime);
            var cycle = 10 * 60 * 1000;
            var remain = cycle - diff;
            var min = Math.ceil(remain / 60000);
            if (min <= 0) min = 1;
            msg += "The travel duration is **5 minutes**. The next ship will arrive in approximately **" + min + " minutes**.";
        } else {
            msg += "Please wait for the next ship to arrive.";
        }
        cm.sendOk(msg);
        cm.dispose();
    } else {
        action(1, 0, 0);
    }
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
            cm.sendYesNo("Do you want to go to Orbis? The trip takes **5 minutes**. The ship is boarding right now, would you like to go?");
        } else if (status == 1) {
            if (cm.haveItem(4031045)) { // Orbis Ticket
                var em = cm.getEventManager("Cabin");
                if (em.getProperty("entry") == "true") {
                    cm.gainItem(4031045, -1);
                    cm.warp(240000111); // Leafre Docked Map
                    cm.dispose();
                } else {
                    cm.sendOk("The ship has just left! You almost made it. Please wait for the next one.");
                    cm.dispose();
                }
            } else {
                cm.sendOk("Make sure you got an Orbis ticket to travel in this flight. Check your inventory.");
                cm.dispose();
            }
        }
    }
}