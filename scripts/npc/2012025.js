function start() {
    var em = cm.getEventManager("Genie");
    var now = new Date();
    var minutes = now.getMinutes();
    var seconds = now.getSeconds();
    var totalSeconds = minutes * 60 + seconds;
    var cycleTime = (totalSeconds % 1200); // 20 mins (1200s)

    // Orbis Boarding: 0 - 2.5 mins (0 - 150s)

    var msg = "";
    if (em.getProperty("entry") == "true") {
        msg = "We are currently #bboarding#k for Ariant!\r\n";
        var timeLeft = 150 - cycleTime;
        var minLeft = Math.ceil(timeLeft / 60);
        msg += "The genie will leave in #b" + minLeft + " minutes#k.\r\n";
    } else {
        msg = "The genie to Ariant is already travelling.\r\n";
        // Next boarding at next 0, 20, 40
        var nextBoarding = 20 - (minutes % 20);
        msg += "The next genie will board in #b" + nextBoarding + " minutes#k.\r\n";
    }

    if (cm.haveItem(4031576)) {
        if (em.getProperty("entry") == "true") {
            cm.sendYesNo(msg + "This will not be a short flight, so you need to take care of some things, I suggest you do that first before getting on board. Do you still wish to board the genie?");
        } else {
            cm.sendOk(msg + "I'm sorry, but you'll have to get on the next ride. The ride schedule is available through the guide at the ticketing booth.");
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