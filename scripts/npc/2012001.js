function start() {
    var now = new Date();
    var minutes = now.getMinutes();
    var msg = "Hello, I am the ticketing usher from Orbis.\r\n";

    // Orbis Boarding: :30 - :40
    // Flight: :40 - :55

    if (minutes >= 30 && minutes < 40) {
        msg += "We are currently #bboarding#k for Ellinia!\r\nThe ship will take off at " + (40 - minutes) + " mins past the hour.\r\n";
    } else {
        msg += "We are currently #rnot boarding#k.\r\n";
        msg += "Next Boarding Time: #bXX:30#k\r\n";
        msg += "Next Take Off Time: #bXX:40#k\r\n";
    }

    msg += "Do you want to go to Ellinia?";

    if (cm.haveItem(4031047)) {
        cm.sendSimple(msg + "\r\n#b#L1#Let me go in to the departure point.");
    } else {
        cm.sendOk(msg + "\r\n\r\n#e(You need an Ellinia Ticket to travel.)");
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
    if (em.getProperty("entry") == "true" && em.getProperty("location") == "orbis") {
        cm.warp(200000112);
        cm.gainItem(4031047, -1);
        cm.dispose();
    } else {
        if (em.getProperty("entry") == "true" && em.getProperty("location") == "ellinia") {
            cm.sendOk("The boat is currently boarding at Ellinia. Please wait for it to arrive.");
        } else {
            cm.sendOk("The boat to Ellinia is already travelling, please be patient for the next one.");
        }
        cm.dispose();
    }
}