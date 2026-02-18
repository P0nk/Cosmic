function start() {
    var em = cm.getEventManager("Trains");
    var now = new Date();
    var minutes = now.getMinutes();
    var seconds = now.getSeconds();
    var totalSeconds = minutes * 60 + seconds;
    var cycleTime = (totalSeconds % 900); // 15 mins in seconds

    // Orbis Boarding: 0 - 2.5 mins (0 - 150s)
    // Ride: 2.5 - 7.5 mins

    var msg = "";
    if (em.getProperty("entry") == "true") {
        msg = "We are currently #bboarding#k for Ludibrium!\r\n";
        var timeLeft = 150 - cycleTime;
        var minLeft = Math.ceil(timeLeft / 60);
        msg += "The train will leave in #b" + minLeft + " minutes#k.\r\n";
    } else {
        msg = "The train to Ludibrium is already travelling.\r\n";
        // Next boarding at next 0 or 15 or 30 or 45
        // If current is 5, next is 15.
        // If current is 20, next is 30.
        var nextBoarding = 15 - (minutes % 15);
        msg += "The next train will board in #b" + nextBoarding + " minutes#k.\r\n";
    }

    if (cm.haveItem(4031074)) {
        if (em.getProperty("entry") == "true") {
            cm.sendYesNo(msg + "Do you want to go to Ludibrium?");
        } else {
            cm.sendOk(msg + "Please be patient for the next one.");
            cm.dispose();
        }
    } else {
        cm.sendOk("Make sure you got a Ludibrium ticket to travel in this train. Check your inventory.");
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