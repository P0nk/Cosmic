/*
    Tommie (2082001) - Leafre Station
*/

function start() {
    status = -1;
    var em = cm.getEventManager("Cabin");

    if (em == null) {
        cm.sendOk("The event script is missing. Please report this to an admin.");
        cm.dispose();
        return;
    }

    // Check strict 15-min cycle time
    var now = java.lang.System.currentTimeMillis();
    var cycleTime = now % 900000; // 0 to 900,000 ms (15 mins)
    var boardingStart = 450000; // 7.5 mins
    var boardingEnd = 600000; // 10 mins

    if (cycleTime >= boardingStart && cycleTime < boardingEnd) {
        action(1, 0, 0); // Boarding is open
    } else {
        var waitTime = 0;
        if (cycleTime < boardingStart) {
            waitTime = boardingStart - cycleTime;
        } else {
            // Already left (e.g. 11 mins), next is (15-11) + 7.5
            waitTime = (900000 - cycleTime) + boardingStart;
        }

        var min = Math.ceil(waitTime / 60000);
        if (min <= 0) min = 1;

        cm.sendOk("The flight to Orbis is currently in progress. The next ship will arrive in approximately #b" + min + " minutes#k.\r\nThe travel duration is #b5 minutes#k.");
        cm.dispose();
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
            cm.sendYesNo("Do you want to go to Orbis? The trip takes #b5 minutes#k. The ship is boarding right now, would you like to go?");
        } else if (status == 1) {
            if (cm.haveItem(4031045)) { // Ticket to Orbis
                cm.gainItem(4031045, -1);
                cm.warp(240000111); // Leafre Docked Map
                cm.dispose();
            } else {
                cm.sendOk("Make sure you got a #bTicket to Orbis (Regular)#k to travel in this flight. Check your inventory.");
                cm.dispose();
            }
        }
    }
}