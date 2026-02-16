var status = 0;
var ticketSelection = -1;
var text = "Here's the ticket reader.";
var hasTicket = false;
var NLC = false;
var em;

function start() {
    cm.sendSimple("Pick your destination.\n\r\n#L0##bKerning Square Shopping Center#l\n\n\r\n#L1#Enter Contruction Site#l\r\n#L2#New Leaf City#l");
}

function action(mode, type, selection) {
    em = cm.getEventManager("Subway");

    if (mode == -1) {
        cm.dispose();
        return;
    } else if (mode == 0) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        if (selection == 0) {
            var em = cm.getEventManager("KerningTrain");
            if (!em.startInstance(cm.getPlayer())) {
                cm.sendOk("The passenger wagon is already full. Try again a bit later.");
            }

            cm.dispose();

        } else if (selection == 1) {
            if (cm.haveItem(4031036) || cm.haveItem(4031037) || cm.haveItem(4031038)) {
                text += " You will be brought in immediately. Which ticket you would like to use?#b";
                for (var i = 0; i < 3; i++) {
                    if (cm.haveItem(4031036 + i)) {
                        text += "\r\n#b#L" + (i + 1) + "##t" + (4031036 + i) + "#";
                    }
                }
                cm.sendSimple(text);
                hasTicket = true;
            } else {
                cm.sendOk("It seems as though you don't have a ticket!");
                cm.dispose();

            }
        } else if (selection == 2) {
            // Time Logic
            var cal = java.util.Calendar.getInstance();
            var hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            var min = cal.get(java.util.Calendar.MINUTE);
            var nowStr = (hour < 10 ? "0" + hour : hour) + ":" + (min < 10 ? "0" + min : min);

            // Wait Time Logic (10 min cycle: 00:00-01:30 Board, 01:30-10:00 Wait)
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

            if (!cm.haveItem(4031711) && cm.getPlayer().getMapId() == 103000100) {
                cm.sendOk("It seems you don't have a ticket! You can buy one from Bell.");
                cm.dispose();
                return;
            }
            if (em.getProperty("entry") == "true") {
                cm.sendYesNo("It looks like there's plenty of room for this ride. Please have your ticket ready so I can let you in. The ride will be long, but you'll get to your destination just fine. What do you think? Do you want to get on this ride?\r\nCurrent Time: #b" + nowStr + "#k\r\nThe next train leaves in: #b" + waitMin + " minutes#k" + statusText);
            } else {
                cm.sendNext("We will begin boarding 1 minute before the takeoff. Please be patient and wait for a few minutes. Be aware that the subway will take off right on time, and we stop receiving tickets 1 minute before that, so please make sure to be here on time.\r\nCurrent Time: #b" + nowStr + "#k\r\nThe next train leaves in: #b" + waitMin + " minutes#k" + statusText);
                cm.dispose();

            }
        }
    } else if (status == 2) {
        if (hasTicket) {
            ticketSelection = selection;
            if (ticketSelection > -1) {
                cm.gainItem(4031035 + ticketSelection, -1);
                cm.warp(103000897 + (ticketSelection * 3), "st00");  // thanks IxianMace for noticing a few scripts having misplaced warp SP's
                hasTicket = false;
                cm.dispose();
                return;
            }
        }

        if (cm.haveItem(4031711)) {
            if (em.getProperty("entry") == "false") {
                cm.sendNext("We will begin boarding 1 minute before the takeoff. Please be patient and wait for a few minutes. Be aware that the subway will take off right on time, and we stop receiving tickets 1 minute before that, so please make sure to be here on time.");
            } else {
                cm.gainItem(4031711, -1);
                cm.warp(600010004);
            }

            cm.dispose();

        }
    }
}