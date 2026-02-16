/*
    Map: 200090400 (Genie to Ariant)
    Description: Displays countdown timer upon entry
*/

function start(ms) {
    try {
        var PacketCreator = Java.type("tools.PacketCreator");

        // Genie Phase: Ride (05:00 - 10:00)
        // Arrival at 10:00 (600,000 ms)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 600000; // 10 min cycle
        var arrivalTime = 600000; // 10:00

        var timeLeft = arrivalTime - cycleTime;

        if (timeLeft > 0) {
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Map 200090400] Error: " + e);
    }
}
