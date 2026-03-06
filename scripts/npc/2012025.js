function getNextSchedules() {
    var now = new Date();
    var currentHour = now.getHours();
    var currentMin = now.getMinutes();

    var schedules = [];
    var baseIntervals = [0, 20, 40];

    // Find next 2 intervals
    var minOffset = 0;
    while (schedules.length < 2) {
        var checkHour = (currentHour + Math.floor((currentMin + minOffset) / 60)) % 24;
        var checkMin = (currentMin + minOffset) % 60;

        for (var i = 0; i < baseIntervals.length; i++) {
            if (checkMin <= baseIntervals[i] && schedules.length < 2) {
                var hrStr = checkHour < 10 ? "0" + checkHour : checkHour;
                var minStr = baseIntervals[i] < 10 ? "0" + baseIntervals[i] : baseIntervals[i];
                schedules.push(hrStr + ":" + minStr);
            }
        }
        minOffset += 60;
        if (schedules.length < 2) {
            currentMin = 0;
            minOffset = 60;
        }
    }
    return schedules;
}

function start() {
    var em = cm.getEventManager("Genie");
    var now = new Date();
    var minutes = now.getMinutes();

    var msg = "The Genie to the sandy dunes of Ariant is ready.\r\n";
    if (em.getProperty("entry") == "true") {
        msg += "We are currently #bboarding#k for Ariant!\r\n";

        var nextHour = now.getHours();
        var nextMin = 2;
        if (minutes < 2) nextMin = 2;
        else if (minutes < 22) nextMin = 22;
        else if (minutes < 42) nextMin = 42;
        else {
            nextMin = 2;
            nextHour = (nextHour + 1) % 24;
        }
        var hrStr = nextHour < 10 ? "0" + nextHour : nextHour;
        var minStr = nextMin < 10 ? "0" + nextMin : nextMin;

        msg += "The genie will leave at approx #b#e" + hrStr + ":" + minStr + " and 30 secs#k#n.\r\n";
    } else {
        msg += "The genie to Ariant is already travelling.\r\n";
        var schedules = getNextSchedules();
        msg += "Our next boardings are #b#e" + schedules[0] + "#k#n and #b#e" + schedules[1] + "#k#n.\r\n";
    }

    if (cm.haveItem(4031576)) {
        if (em.getProperty("entry") == "true") {
            cm.sendYesNo(msg + "This will not be a short flight. Do you still wish to board the genie?");
        } else {
            cm.sendOk(msg + "I'm sorry, but you'll have to get on the next ride. Please check the schedule.");
            cm.dispose();
        }
    } else {
        cm.sendOk("Make sure you got an Ariant ticket to travel in this genie. Check your inventory.");
        cm.dispose();
    }
}

function action(mode, type, selection) {
    if (mode <= 0) {
        cm.sendOk("Okay, talk to me if you change your mind!");
        cm.dispose();
        return;
    }

    var em = cm.getEventManager("Genie");
    if (em.getProperty("entry") == "true") {
        cm.warp(200000152);
        cm.gainItem(4031576, -1);
    } else {
        cm.sendOk("This genie is getting ready for takeoff. I'm sorry, but you'll have to get on the next ride. The ride schedule is available through the guide at the ticketing booth.");
    }

    cm.dispose();
}