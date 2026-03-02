var eventName = "Boats";
var toMap = 200090010;

function start(ms) {
    var em = ms.getClient().getEventManager(eventName);

    //is the player late to start the travel?
    if (em.getProperty("docked") == "false") {
        ms.getClient().getPlayer().warpAhead(toMap);
    } else {
        // Calculate time left for 30:00 - 40:00 Orbis boarding window (in 60m cycle)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 3600000;
        if (cycleTime >= 1800000 && cycleTime < 2400000) {
            var timeLeft = 2400000 - cycleTime;
            var PacketCreator = Java.type("tools.PacketCreator");
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    }
}
