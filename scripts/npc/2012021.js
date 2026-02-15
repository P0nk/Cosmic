/*
    Ramini (2012021) - Orbis Station
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
    var boardingEnd = 150000; // 2.5 mins

    if (cycleTime < boardingEnd) {
        action(1, 0, 0); // Boarding is open
    } else {
        var waitTime = 900000 - cycleTime; // Time until next 00/15/30/45
        var min = Math.ceil(waitTime / 60000);
        if (min <= 0) min = 1;

        cm.sendOk("The flight to Leafre is currently in progress. The next ship will arrive in approximately #b" + min + " minutes#k.\r\nThe travel duration is #b5 minutes#k.");
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
            cm.sendYesNo("Do you want to go to Leafre? The trip takes #b5 minutes#k. The ship is boarding right now, would you like to go?");
        } else if (status == 1) {
            if (cm.haveItem(4031331)) { // Ticket to Leafre
                cm.gainItem(4031331, -1);
                cm.warp(200000132); // Orbis Docked Map
                cm.dispose();
            } else {
                cm.sendOk("You don't have a ticket to Leafre. Please buy one from the ticket booth.");
                cm.dispose();
            }
        }
    }
}