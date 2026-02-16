/*
    Map: 200090210 (Ride to Orbis)
    Description: Displays countdown timer upon entry
*/

function start(ms) {
    try {

        var PacketCreator = Java.type("tools.PacketCreator");

        // Ride to Orbis Phase: 10:00 - 15:00 (600,000 - 900,000)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 900000;
        var arrivalTime = 900000; // 15:00

        var timeLeft = arrivalTime - cycleTime;

        if (timeLeft > 0) {
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Map 200090210] Error: " + e);
    }
}
