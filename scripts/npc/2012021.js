/*
    Orbis Station - To Leafre Guide (2012021) - Ramini
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
        var msg = "The flight to Leafre is currently in progress or preparing for takeoff.\r\n";

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
            cm.sendYesNo("Do you want to go to Leafre? The trip takes **5 minutes**. The ship is boarding right now, would you like to go?");
        } else if (status == 1) {
            if (cm.haveItem(4031331)) { // Ticket to Leafre
                var em = cm.getEventManager("Cabin");
                if (em.getProperty("entry") == "true") {
                    cm.gainItem(4031331, -1);
                    cm.warp(200000132); // Orbis Docked Map
                    cm.dispose();
                } else {
                    cm.sendOk("The ship has just left! You almost made it. Please wait for the next one.");
                    cm.dispose();
                }
            } else {
                cm.sendOk("You don't have a ticket to Leafre. Please buy one from the ticket booth.");
                cm.dispose();
            }
        }
    }
}