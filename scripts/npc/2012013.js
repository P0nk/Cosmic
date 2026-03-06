function start() {
    var em = cm.getEventManager("Trains");
    var now = new Date();
    var minutes = now.getMinutes();

    var msg = "Boarding for the train to Ludibrium begins at the top of the hour, and every 15 minutes thereafter.\r\n";
    if (em.getProperty("entry") == "true") {
        msg += "We are currently #bboarding#k for Ludibrium!\r\n";

        var nextHour = now.getHours();
        var nextMin = 0;
        if (minutes < 15) {
            nextMin = 15;
            // The wait room is configured manually by Trains.js to stay open 0-2.5m and close. Then the train flies.
            // But wait, Trains.js cycle is:
            // 0:00 -> Boarding Orbis
            // 2:30 -> Fly to Ludi
            // 7:30 -> Boarding Ludi
            // 10:00 -> Fly to Orbis
        }

        // Because boarding is 0 -> 2.5 min relative to the 15m cycle
        // That means the flight takes off at :02.5, :17.5, :32.5, :47.5
        var currentCycleStart = Math.floor(minutes / 15) * 15;
        var takeoffMin = currentCycleStart + 2; // Floor to 2 mins past just for display
        var hrStr = nextHour < 10 ? "0" + nextHour : nextHour;
        var minStr = takeoffMin < 10 ? "0" + takeoffMin : takeoffMin;

        msg += "The train will leave approximately at #b#e" + hrStr + ":" + minStr + " and 30 secs#k#n.\r\n";
    } else {
        msg += "The train to Ludibrium is already travelling.\r\n";

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

        msg += "The next two boardings are at #b#e" + hrStr + ":" + minStr + "#k#n and #b#e" + hrStr2 + ":" + minStr2 + "#k#n.\r\n";
    }

    if (cm.haveItem(4031074)) {
        if (em.getProperty("entry") == "true") {
            cm.sendYesNo(msg + "Do you want to go to Ludibrium?");
        } else {
            cm.sendOk(msg + "Please be patient for the next one.");
            cm.dispose();
        }
    } else {
        cm.sendOk(msg + "\r\n#e(Make sure you've got a Ludibrium Ticket to travel on this train.)#n");
        cm.dispose();
    }
}

function action(mode, type, selection) {
    if (mode <= 0) {
        cm.sendOk("Okay, talk to me if you change your mind!");
        cm.dispose();
        return;
    }
    var em = cm.getEventManager("Trains");
    if (em.getProperty("entry") == "true") {
        cm.warp(200000122);
        cm.gainItem(4031074, -1);
        cm.dispose();
    } else {
        cm.sendOk("The train to Ludibrium is ready to take off, please be patient for the next one.");
        cm.dispose();
    }
}