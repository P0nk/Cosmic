var eventName = "AirPlane";
var toMap = 540010101;

function start(ms) {
    var em = ms.getClient().getEventManager(eventName);

    //is the player late to start the travel?
    if (em.getProperty("docked") == "false") {
        ms.getClient().getPlayer().warpAhead(toMap);
    } else {
        // Calculate time left for 00:00 - 10:00 boarding window
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 3600000;
        if (cycleTime < 600000) {
            var timeLeft = 600000 - cycleTime;
            var PacketCreator = Java.type("tools.PacketCreator");
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    }
}
