var eventName = "Trains";
var toMap = 200090110;

function start(ms) {
    var em = ms.getClient().getEventManager(eventName);

    //is the player late to start the travel?
    if (em.getProperty("docked") == "false") {
        ms.getClient().getPlayer().warpAhead(toMap);
    } else {
        // Calculate time left for 07:30 - 10:00 Ludi boarding window (in 15m cycle)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 900000;
        if (cycleTime >= 450000 && cycleTime < 600000) {
            var timeLeft = 600000 - cycleTime;
            var PacketCreator = Java.type("tools.PacketCreator");
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    }
}