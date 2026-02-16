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
            cm.sendNext("Welcome to the Leafre Station. We have flights departing for Orbis every 15 minutes, starting at 7 minutes past the hour.\r\n(XX:07, XX:22, XX:37, XX:52)");
        } else if (status == 1) {
            cm.sendYesNo("The ship is currently boarding. The ride takes #b5 minutes#k. Would you like to head to the dock?");
        } else if (status == 2) {
            if (cm.haveItem(4031045)) { // Ticket to Orbis
                cm.gainItem(4031045, -1);
                cm.warp(240000111); // Leafre Docked Map

                // Calculate timer manually for late joiners
                var now = java.lang.System.currentTimeMillis();
                var cycleTime = now % 900000;
                var boardingEnd = 600000; // 10 mins (450,000 to 600,000)
                var timeLeft = boardingEnd - cycleTime;
                if (timeLeft > 0) {
                    var PacketCreator = Java.type("tools.PacketCreator");
                    cm.sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
                }

                cm.dispose();
            } else {
                cm.sendOk("Make sure you got a #bTicket to Orbis (Regular)#k to travel in this flight. Check your inventory.");
                cm.dispose();
            }
        }
    }
}