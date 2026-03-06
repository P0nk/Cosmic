function start() {
    var now = new Date();
    var minutes = now.getMinutes();
    var msg = "Hello, I am Cherry from Ellinia.\r\n";

    // Ellinia Boarding: :00 - :10
    // Flight: :10 - :25

    if (minutes < 10) {
        msg += "We are currently #bboarding#k for Orbis!\r\nKeep a tight grip on your belongings and stay indoors if you hear the sirens! The skies can be treacherous.\r\n";
    } else {
        msg += "We are currently #rnot boarding#k.\r\n";

        var nextHour = now.getHours();
        var hrStr = nextHour < 10 ? "0" + nextHour : nextHour;
        var next2Hour = (nextHour + 1) % 24;
        var hrStr2 = next2Hour < 10 ? "0" + next2Hour : next2Hour;

        msg += "Next Boarding Time: #b#e" + hrStr2 + ":00#k#n\r\n";
        msg += "Next Take Off Time: #b#e" + hrStr2 + ":10#k#n\r\n";
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