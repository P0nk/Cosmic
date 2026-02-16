/*
    Map: 200090011 (Cabin to Orbis)
    Description: Displays countdown timer upon entry
*/

function start(ms) {
    try {
        var PacketCreator = Java.type("tools.PacketCreator");

        // Boat Phase: Ride to Orbis (12:00 - 20:00)
        // Arrival at 20:00 (1,200,000 ms)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 1200000; // 20 min cycle
        var arrivalTime = 1200000; // 20:00

        var timeLeft = arrivalTime - cycleTime;

        if (timeLeft > 0) {
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Map 200090011] Error: " + e);
    }
}
