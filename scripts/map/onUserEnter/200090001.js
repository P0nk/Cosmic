/*
    Map: 200090001 (Cabin to Ellinia)
    Description: Displays countdown timer upon entry
*/

function start(ms) {
    try {
        var PacketCreator = Java.type("tools.PacketCreator");

        // Boats Phase: Ride (05:00 - 15:00)
        // Arrival at 15:00 (900,000 ms)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 900000;
        var arrivalTime = 900000;

        var timeLeft = arrivalTime - cycleTime;

        if (timeLeft > 0) {
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Map 200090001] Error: " + e);
    }
}
