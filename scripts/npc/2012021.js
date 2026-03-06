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

    var now = new Date();
    var minutes = now.getMinutes();

    // Boarding 0-2.5m = 0-2m
    // Flight: 2.5m - 12.5m 
    var msg = "Welcome to Orbis Station. We have flights departing for Leafre every 15 minutes.\r\n";

    if (em.getProperty("entry") == "true") {
        msg += "We are currently #bboarding#k for Leafre!\r\n";

        var nextHour = now.getHours();
        var currentCycleStart = Math.floor(minutes / 15) * 15;
        var takeoffMin = currentCycleStart + 2; // Floor to 2 mins past just for display

        var hrStr = nextHour < 10 ? "0" + nextHour : nextHour;
        var minStr = takeoffMin < 10 ? "0" + takeoffMin : takeoffMin;

        msg += "The Cabin will leave at approx #b#e" + hrStr + ":" + minStr + " and 30 secs#k#n.\r\n";
        action(1, 0, 0); // Boarding is open
    } else {
        msg += "The Cabin to Leafre is already travelling.\r\n";

        var nextHour = now.getHours();
        var nextMin = 0;
        if (minutes < 15) nextMin = 15;
        else if (minutes < 30) nextMin = 30;
        else if (minutes < 45) nextMin = 45;
        else {
            nextMin = 0;
            nextHour = (nextHour + 1) % 24;
        }
        var nextHour2 = nextHour;
        var nextMin2 = (nextMin + 15) % 60;
        if (nextMin2 == 0) nextHour2 = (nextHour2 + 1) % 24;

        var hrStr = nextHour < 10 ? "0" + nextHour : nextHour;
        var minStr = nextMin < 10 ? "0" + nextMin : nextMin;
        var hrStr2 = nextHour2 < 10 ? "0" + nextHour2 : nextHour2;
        var minStr2 = nextMin2 < 10 ? "0" + nextMin2 : nextMin2;

        msg += "The next boardings will be at #b#e" + hrStr + ":" + minStr + "#k#n and #b#e" + hrStr2 + ":" + minStr2 + "#k#n.\r\n";
        cm.sendOk(msg);
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
            cm.sendNext("Welcome to the Orbis Station. We have flights departing for Leafre every 15 minutes.\r\n(XX:00, XX:15, XX:30, XX:45)");
        } else if (status == 1) {
            cm.sendYesNo("The Cabin is currently boarding. The ride takes about 10 minutes. Would you like to head to the dock?");
        } else if (status == 2) {
            if (cm.haveItem(4031331)) { // Ticket to Leafre
                cm.gainItem(4031331, -1);
                cm.warp(200000132); // Orbis Docked Map

                // Calculate timer manually for late joiners
                var now = java.lang.System.currentTimeMillis();
                var cycleTime = now % 900000;
                var boardingEnd = 150000; // 2.5 mins
                var timeLeft = boardingEnd - cycleTime;
                if (timeLeft > 0) {
                    var PacketCreator = Java.type("tools.PacketCreator");
                    cm.sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
                }

                cm.dispose();
            } else {
                cm.sendOk("You don't have a ticket to Leafre. Please buy one from the ticket booth.");
                cm.dispose();
            }
        }
    }
}