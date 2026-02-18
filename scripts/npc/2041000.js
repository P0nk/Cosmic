function start() {
    var em = cm.getEventManager("Trains");
    var now = new Date();
    var minutes = now.getMinutes();
    var seconds = now.getSeconds();
    var totalSeconds = minutes * 60 + seconds;
    var cycleTime = (totalSeconds % 900); // 15 mins

    // Ludi Boarding: 7.5 - 10 mins (450s - 600s)

    var msg = "";
    if (em.getProperty("entry") == "true") {
        msg = "We are currently #bboarding#k for Orbis!\r\n";
        // Boarding ends at 600s check relative to cycle
        var timeLeft = 600 - cycleTime;
        var minLeft = Math.ceil(timeLeft / 60);
        msg += "The train will leave in #b" + minLeft + " minutes#k.\r\n";
    } else {
        msg = "The train to Orbis is already travelling.\r\n";
        // Next boarding starts at 7.5 mins into next cycle? No, 7.5 mins into CURRENT cycle if < 7.5 
        // If minutes % 15 is < 7.5, wait is 7.5 - current
        // If minutes % 15 is > 10, wait is (15-current) + 7.5

        var minIntoCycle = (minutes + (seconds / 60)) % 15;
        var waitTime = 0;
        if (minIntoCycle < 7.5) {
            waitTime = 7.5 - minIntoCycle;
        } else {
            waitTime = (15 - minIntoCycle) + 7.5;
        }
        var waitMin = Math.ceil(waitTime);
        msg += "The next train will board in #b" + waitMin + " minutes#k.\r\n";
    }

    if (cm.haveItem(4031045)) {
        if (em.getProperty("entry") == "true") {
            cm.sendYesNo(msg + "Do you want to go to Orbis?");
        } else {
            cm.sendOk(msg + "Please be patient for the next one.");
            cm.dispose();
        }
    } else {
        cm.sendOk("Make sure you got a Orbis ticket to travel in this train. Check your inventory.");
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
        cm.warp(220000111);
        cm.gainItem(4031045, -1);
        cm.dispose();
    } else {
        cm.sendOk("The train to Orbis is ready to take off, please be patient for the next one.");
        cm.dispose();
    }
}