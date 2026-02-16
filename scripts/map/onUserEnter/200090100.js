/*
    Map: 200090100 (Train to Ludibrium)
    Description: Displays countdown timer upon entry
*/

function start(ms) {
    try {
        var PacketCreator = Java.type("tools.PacketCreator");

        // Train Phase: Ride to Ludi (02:30 - 07:30)
        // Arrival at 07:30 (450,000 ms)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 900000; // 15 min cycle
        var arrivalTime = 450000; // 07:30

        var timeLeft = arrivalTime - cycleTime;

        if (timeLeft > 0) {
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Map 200090100] Error: " + e);
    }
}
