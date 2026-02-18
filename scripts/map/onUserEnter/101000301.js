var eventName = "Boats";
var toMap = 200090010;

function start(ms) {
    var em = ms.getClient().getEventManager(eventName);

    //is the player late to start the travel?
    if (em.getProperty("docked") == "false") {
        ms.getClient().getPlayer().warpAhead(toMap);
    } else {
        // Calculate time left for 10:00 - 12:00 boarding window (in 20m cycle)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 1200000;
        if (cycleTime >= 600000 && cycleTime < 720000) {
            var timeLeft = 720000 - cycleTime;
            var PacketCreator = Java.type("tools.PacketCreator");
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    }
}
