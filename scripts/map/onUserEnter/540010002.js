/*
    Map: 540010002 (Plane to KC)
    Description: Displays countdown timer upon entry
*/

function start(ms) {
    try {
        var PacketCreator = Java.type("tools.PacketCreator");

        // AirPlane Phase: Ride (05:00 - 30:00)
        // Arrival at 30:00 (1,800,000 ms)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 1800000; // 30 min cycle
        var arrivalTime = 1800000; // 30:00

        var timeLeft = arrivalTime - cycleTime;

        if (timeLeft > 0) {
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Map 540010002] Error: " + e);
    }
}
