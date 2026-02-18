function start() {
    var now = new Date();
    var minutes = now.getMinutes();
    var msg = "Hello, I am Cherry from Ellinia.\r\n";

    // Ellinia Boarding: :00 - :10
    // Flight: :10 - :25

    if (minutes < 10) {
        msg += "We are currently #bboarding#k for Orbis!\r\nThe ship will take off at the top of the hour (approx " + (10 - minutes) + " mins).\r\n";
    } else {
        msg += "We are currently #rnot boarding#k.\r\n";
        msg += "Next Boarding Time: #bXX:00#k\r\n";
        msg += "Next Take Off Time: #bXX:10#k\r\n";
    }

    msg += "Do you want to go to Orbis?";

    if (cm.haveItem(4031045)) {
        cm.sendSimple(msg + "\r\n#b#L1#Let me go in to the departure point.");
    } else {
        cm.sendOk(msg + "\r\n\r\n#e(You need an Orbis Ticket to travel.)");
        cm.dispose();
    }
}

function action(mode, type, selection) {
    if (mode <= 0) {
        cm.sendOk("Okay, talk to me if you change your mind!");
        cm.dispose();
        return;
    }
    var em = cm.getEventManager("Boats");
    if (em.getProperty("entry") == "true" && em.getProperty("location") == "ellinia") {
        cm.warp(101000301);
        cm.gainItem(4031045, -1);
        cm.dispose();
    } else {
        if (em.getProperty("entry") == "true" && em.getProperty("location") == "orbis") {
            cm.sendOk("The boat is currently boarding at Orbis. Please wait for it to arrive.");
        } else {
            cm.sendOk("The boat to Orbis is already travelling, please be patient for the next one.");
        }
        cm.dispose();
    }
}	