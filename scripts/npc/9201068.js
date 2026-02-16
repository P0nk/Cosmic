status = -1;
close = false;
oldSelection = -1;
var em;

function start() {
    em = cm.getEventManager("Subway");

    // Time Logic
    var cal = java.util.Calendar.getInstance();
    var hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
    var min = cal.get(java.util.Calendar.MINUTE);
    var nowStr = (hour < 10 ? "0" + hour : hour) + ":" + (min < 10 ? "0" + min : min);

    // Wait Time Logic (10 min cycle: 00:00-01:30 Board, 01:30-10:00 Wait)
    // Boarding Phase: 0 - 90,000 (1.5 mins)
    // Departure at x1.5 mins (1:30, 11:30, 21:30...)

    var nowMs = java.lang.System.currentTimeMillis();
    var cycle = nowMs % 600000; // 10 min
    var waitMs = 0;

    if (cycle < 90000) { // Boarding Phase (0 - 1.5 mins)
        waitMs = 90000 - cycle;
    } else { // Ride Phase (1.5 - 10 mins) -> Next boarding finishes at next cycle's 1.5 min mark
        waitMs = (600000 - cycle) + 90000;
    }
    var waitMin = Math.ceil(waitMs / 60000);

    var statusText = "";
    if (cycle < 90000) {
        statusText = "\r\n#gSTATUS: Can board now#k";
    } else {
        statusText = "\r\n#rSTATUS: Train not arrived#k";
    }

    var text = "Here's the ticket reader.\r\nCurrent Time: #b" + nowStr + "#k\r\nThe next train leaves in: #b" + waitMin + " minutes#k" + statusText;
    var hasTicket = false;
    if (cm.haveItem(4031713) && cm.getPlayer().getMapId() == 600010001) {
        text += "\r\n#b#L0##t4031713#";
        hasTicket = true;
    }
    if (!hasTicket) {
        cm.sendOk("It seems you don't have a ticket! You can buy one from Bell.");
        cm.dispose();
    } else {
        cm.sendSimple(text);
    }
}

function action(mode, type, selection) {
    status++;
    if (mode != 1) {
        if (mode == 0) {
            cm.sendNext("You must have some business to take care of here, right?");
        }
        cm.dispose();
        return;
    }
    if (status == 0) {
        if (selection == 0) {
            if (em.getProperty("entry") == "true") {
                cm.sendYesNo("It looks like there's plenty of room for this ride. Please have your ticket ready so I can let you in. The ride will be long, but you'll get to your destination just fine. What do you think? Do you wants to get on this ride?");
            } else {
                cm.sendNext("We will begin boarding 1 minute before the takeoff. Please be patient and wait for a few minutes. Be aware that the subway will take off right on time, and we stop receiving tickets 1 minute before that, so please make sure to be here on time.");
                cm.dispose();
            }
        }
        oldSelection = selection;
    } else if (status == 1) {
        if (oldSelection == 0 && cm.haveItem(4031713)) {
            if (em.getProperty("entry") == "true") {
                cm.gainItem(4031713, -1);
                cm.warp(600010002);
            } else {
                cm.sendNext("We will begin boarding 1 minute before the takeoff. Please be patient and wait for a few minutes. Be aware that the subway will take off right on time, and we stop receiving tickets 1 minute before that, so please make sure to be here on time.");
            }
        } else {
            cm.sendNext("Sorry, you need a ticket to enter!");
        }

        cm.dispose();
    }
}