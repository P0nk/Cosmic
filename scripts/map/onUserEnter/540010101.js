/*
    Map: 540010101 (Plane to CBD)
    Description: Displays countdown timer upon entry
*/

function start(ms) {
    try {
        var PacketCreator = Java.type("tools.PacketCreator");

        // Plane Phase: Ride to CBD (02:30 - 15:00)
        // Arrival at 15:00 (900,000 ms)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 1800000; // 30 min cycle
        var arrivalTime = 900000; // 15:00

        var timeLeft = arrivalTime - cycleTime;

        if (timeLeft > 0) {
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Map 540010101] Error: " + e);
    }
}
