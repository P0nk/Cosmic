function start() {
    if (cm.haveItem(4031045)) {
        var em = cm.getEventManager("Cabin");
        if (em.getProperty("entry") == "true") {
            cm.sendYesNo("Do you wish to board the flight?");
        } else {
            var dockedTime = em.getProperty("dockedTime");
            var now = java.lang.System.currentTimeMillis();

            if (dockedTime != null) {
                var diff = now - dockedTime;
                var cycle = 10 * 60 * 1000;
                var remain = cycle - diff;
                var min = Math.ceil(remain / 60000);

                if (min <= 0) min = 1;

                cm.sendOk("The flight has not arrived yet. Come back in ~" + min + " minutes.");
            } else {
                cm.sendOk("The flight has not arrived yet. Come back soon.");
            }
            cm.dispose();
        }
    } else {
        cm.sendOk("Make sure you got an Orbis ticket to travel in this flight. Check your inventory.");
        cm.dispose();
    }
}

function action(mode, type, selection) {
    if (mode <= 0) {
        cm.sendOk("Okay, talk to me if you change your mind!");
        cm.dispose();
        return;
    }
    var em = cm.getEventManager("Cabin");
    if (em.getProperty("entry") == "true") {
        cm.warp(240000111);
        cm.gainItem(4031045, -1);
    } else {
        cm.sendOk("The flight has not arrived yet. Come back soon.");
    }
    cm.dispose();
}