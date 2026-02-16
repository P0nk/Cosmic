/*
    Map: 600010005 (Subway to NLC)
    Description: Displays countdown timer upon entry
*/

function start(ms) {
    try {
        var PacketCreator = Java.type("tools.PacketCreator");

        // Subway Phase: Ride to NLC (01:30 - 05:00)
        // Arrival at 05:00 (300,000 ms)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 600000; // 10 min cycle
        var arrivalTime = 300000; // 05:00

        var timeLeft = arrivalTime - cycleTime;

        if (timeLeft > 0) {
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Map 600010005] Error: " + e);
    }
}
