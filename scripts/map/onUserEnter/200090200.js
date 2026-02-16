/*
    Map: 200090200 (Ride to Leafre)
    Description: Displays countdown timer upon entry
*/

function start(ms) {
    try {

        var PacketCreator = Java.type("tools.PacketCreator");

        // Ride to Leafre Phase: 02:30 - 07:30 (150,000 - 450,000)
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 900000;
        var arrivalTime = 450000; // 07:30

        var timeLeft = arrivalTime - cycleTime;

        if (timeLeft > 0) {
            ms.getClient().sendPacket(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Map 200090200] Error: " + e);
    }
}
