function start() {
    var em = cm.getEventManager("Trains");
    var now = new Date();
    var minutes = now.getMinutes();
    var seconds = now.getSeconds();

    // Ludi Boarding: 7.5 - 10 mins into the 15-min cycle
    // (We use exact server time checks rather than relative cycle time as much as possible for display)

    var msg = "All aboard the Ludibrium Express! Make sure your toys are stowed safely.\r\n";
    if (em.getProperty("entry") == "true") {
        msg += "We are currently #bboarding#k for Orbis!\r\n";

        // Find next :10, :25, :40, :55 takeoff time
        var nextHour = now.getHours();
        var nextMin = 10;
        if (minutes < 10) nextMin = 10;
        else if (minutes < 25) nextMin = 25;
        else if (minutes < 40) nextMin = 40;
        else if (minutes < 55) nextMin = 55;
        else {
            nextMin = 10;
            nextHour = (nextHour + 1) % 24;
        }

        var hrStr = nextHour < 10 ? "0" + nextHour : nextHour;
        var minStr = nextMin < 10 ? "0" + nextMin : nextMin;

        msg += "The train will leave at #b#e" + hrStr + ":" + minStr + "#k#n.\r\n";
    } else {
        msg += "The train to Orbis is already travelling.\r\n";

        // Find next :07, :22, :37, :52 boarding time
        var nextHour = now.getHours();
        var nextMin = 7;
        if (minutes < 7) nextMin = 7;
        else if (minutes < 22) nextMin = 22;
        else if (minutes < 37) nextMin = 37;
        else if (minutes < 52) nextMin = 52;
        else {
            nextMin = 7;
            nextHour = (nextHour + 1) % 24;
        }

        var hrStr = nextHour < 10 ? "0" + nextHour : nextHour;
        var minStr = nextMin < 10 ? "0" + nextMin : nextMin;

        msg += "The next train will board at #b#e" + hrStr + ":" + minStr + "#k#n.\r\n";
    }

    if (cm.haveItem(4031045)) {
        if (em.getProperty("entry") == "true") {
            cm.sendYesNo(msg + "Do you want to go to Orbis?");
        } else {
            cm.sendOk(msg + "Please be patient for the next one.");
            cm.dispose();
        }
    } else {
        cm.sendOk(msg + "\r\n#e(Make sure you've got an Orbis Ticket to travel on this train.)#n");
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