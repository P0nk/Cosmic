/*
    Isma (2012006) - Orbis Station Master
*/

var status = -1;
var sel;

var destinations = ["Ellinia", "Ludibrium", "Leafre", "Mu Lung", "Ariant", "Ereve"];
var boatType = ["the ship", "the train", "the Cabin", "Hak", "the Genie", "the ship"];

function getNextSchedules(baseIntervals) {
    var now = new Date();
    var currentHour = now.getHours();
    var currentMin = now.getMinutes();

    var schedules = [];

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
    var message = "Welcome to Orbis Station! I am the Station Master here. Where would you like to travel to?\r\n";
    for (var i = 0; i < destinations.length; i++) {
        message += "\r\n#L" + i + "##bTake " + boatType[i] + " to " + destinations[i] + ".#l";
    }
    cm.sendSimple(message);
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    }
    status++;
    if (status == 0) {
        sel = selection;
        var schedules = [];
        var msg = "";

        if (sel == 0) { // Ellinia
            schedules = getNextSchedules([0]);
            // Orbis -> Ellinia takes off every hour, but boards at 30? Wait. No.
            // Orbis -> Ellinia boards at :30
            schedules = getNextSchedules([30]);
            msg = "Ah, the magical forests of Ellinia. " + boatType[sel] + " to Ellinia departs every hour. ";
        } else if (sel == 1) { // Ludi
            schedules = getNextSchedules([0, 15, 30, 45]);
            msg = "Ludibrium, the town of toys! " + boatType[sel] + " to Ludi departs 4 times an hour. ";
        } else if (sel == 2) { // Leafre
            schedules = getNextSchedules([0, 15, 30, 45]);
            msg = "Minar Forest... brave choice. " + boatType[sel] + " to Leafre departs 4 times an hour. ";
        } else if (sel == 4) { // Ariant
            schedules = getNextSchedules([0, 20, 40]);
            msg = "The sandy dunes of Ariant await! " + boatType[sel] + " to Ariant departs 3 times an hour. ";
        } else {
            msg = "I will send you to the platform for " + destinations[sel] + " now.";
        }

        if (schedules.length > 0) {
            msg += "The next boardings are at #b#e" + schedules[0] + "#k#n and #b#e" + schedules[1] + "#k#n.\r\n\r\nWould you like to head to the platform now?";
            cm.sendYesNo(msg);
        } else {
            cm.sendNext(msg);
        }
    } else if (status == 1) {
        cm.warp(200000110 + (sel * 10), "west00");
        cm.dispose();
    }
}