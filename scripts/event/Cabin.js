/*
    Orbis <-> Leafre Travel Script (Cabin) - Real Time 15 Min Cycle
*/

// Polyfill: Map console.log to Java System.out
var console = {
    log: function (msg) {
        var System = Java.type("java.lang.System");
        System.out.println(msg);
    },
    error: function (msg) {
        var System = Java.type("java.lang.System");
        System.err.println(msg);
    }
};

// Maps
var Orbis_Station;     // 200000131
var Orbis_Docked;      // 200000132
var Leafre_Docked;     // 240000111
var Ride_To_Leafre;    // 200090200
var Ride_To_Orbis;     // 200090210
var Orbis_Arrival;     // 200000131
var Leafre_Arrival;    // 240000110

function init() {
    try {
        console.log("[Cabin JS] init() started.");
        var mf = em.getChannelServer().getMapFactory();

        Orbis_Docked = mf.getMap(200000132);
        Leafre_Docked = mf.getMap(240000111);
        Ride_To_Leafre = mf.getMap(200090200);
        Ride_To_Orbis = mf.getMap(200090210);
        Orbis_Arrival = mf.getMap(200000131);
        Leafre_Arrival = mf.getMap(240000110);

        if (Orbis_Docked == null || Leafre_Docked == null) {
            console.error("[Cabin JS] Maps failed to load!");
        }

        // Initial sync
        syncEvent();
    } catch (e) {
        console.error("[Cabin JS] Crash in init: " + e);
    }
}

function syncEvent() {
    try {
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 900000; // 15 min cycle
        console.log("[Cabin JS] Sync Event. Cycle Time: " + cycleTime);

        if (cycleTime < 150000) {
            // 0 - 2.5 mins: Orbis Boarding
            console.log("[Cabin JS] Phase: Orbis Boarding");
            Ride_To_Orbis.warpEveryone(Orbis_Arrival.getId());
            Ride_To_Leafre.warpEveryone(Leafre_Arrival.getId());
            runOrbisBoarding(150000 - cycleTime);
        } else if (cycleTime < 450000) {
            // 2.5 - 7.5 mins: Ride to Leafre
            console.log("[Cabin JS] Phase: Ride to Leafre");
            Ride_To_Orbis.warpEveryone(Orbis_Arrival.getId());
            runRideToLeafre(450000 - cycleTime);
        } else if (cycleTime < 600000) {
            // 7.5 - 10 mins: Leafre Boarding
            console.log("[Cabin JS] Phase: Leafre Boarding");
            Ride_To_Leafre.warpEveryone(Leafre_Arrival.getId());
            Ride_To_Orbis.warpEveryone(Orbis_Arrival.getId());
            runLeafreBoarding(600000 - cycleTime);
        } else {
            // 10 - 15 mins: Ride to Orbis
            console.log("[Cabin JS] Phase: Ride to Orbis");
            Ride_To_Leafre.warpEveryone(Leafre_Arrival.getId());
            runRideToOrbis(900000 - cycleTime);
        }
    } catch (e) {
        console.error("[Cabin JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

function runOrbisBoarding(timeLeft) {
    console.log("[Cabin JS] runOrbisBoarding schedule for " + timeLeft + "ms");
    // Arrival from Leafre cleanup
    Ride_To_Orbis.warpEveryone(Orbis_Arrival.getId());

    // Setup Orbis Boarding
    em.setProperty("location", "orbis");
    em.setProperty("docked", "true");
    em.setProperty("entry", "true");

    Orbis_Docked.setDocked(true);
    Leafre_Docked.setDocked(false);

    em.schedule("runRideToLeafre", timeLeft);
    Orbis_Docked.broadcastShip(true);

    // Timer
    var PacketCreator = Java.type("tools.PacketCreator");
    Orbis_Docked.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function runRideToLeafre(timeLeft) {
    console.log("[Cabin JS] runRideToLeafre schedule for " + timeLeft + "ms");
    // Close Orbis Boarding
    em.setProperty("entry", "false");
    em.setProperty("docked", "false");

    Orbis_Docked.setDocked(false);

    // Warp to Ride
    Orbis_Docked.warpEveryone(Ride_To_Leafre.getId());
    console.log("[Cabin JS] Warped everyone to Ride to Leafre (200090200)");

    em.schedule("runLeafreBoarding", timeLeft);
    Orbis_Docked.broadcastShip(false);

    // Timer
    var PacketCreator = Java.type("tools.PacketCreator");
    Ride_To_Leafre.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function runLeafreBoarding(timeLeft) {
    console.log("[Cabin JS] runLeafreBoarding schedule for " + timeLeft + "ms");
    // Arrival from Orbis cleanup
    Ride_To_Leafre.warpEveryone(Leafre_Arrival.getId());
    console.log("[Cabin JS] Warped everyone to Leafre Arrival (240000110)");

    // Setup Leafre Boarding
    em.setProperty("location", "leafre");
    em.setProperty("docked", "true");
    em.setProperty("entry", "true");

    Leafre_Docked.setDocked(true);
    Orbis_Docked.setDocked(false);

    em.schedule("runRideToOrbis", timeLeft);
    Leafre_Docked.broadcastShip(true);

    // Timer
    var PacketCreator = Java.type("tools.PacketCreator");
    Leafre_Docked.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function runRideToOrbis(timeLeft) {
    console.log("[Cabin JS] runRideToOrbis schedule for " + timeLeft + "ms");
    // Close Leafre Boarding
    em.setProperty("entry", "false");
    em.setProperty("docked", "false");

    Leafre_Docked.setDocked(false);

    // Warp to Ride
    Leafre_Docked.warpEveryone(Ride_To_Orbis.getId());
    console.log("[Cabin JS] Warped everyone to Ride to Orbis (200090210)");

    em.schedule("runOrbisBoarding", timeLeft);
    Leafre_Docked.broadcastShip(false);

    // Timer
    var PacketCreator = Java.type("tools.PacketCreator");
    Ride_To_Orbis.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

// Required Filler Functions
function cancelSchedule() { }
function dispose() { }
function monsterValue(eim, mobId) { return 0; }
function setup(level, lobbyid) { return em.newInstance("Cabin"); }
function playerEntry(eim, player) { }
function playerExit(eim, player) { }
function playerDisconnected(eim, player) { }
function playerRevive(eim, player) { return true; }
function scheduledTimeout(eim) { }
function changedMap(eim, player, mapid) { }
function playerDead(eim, player) { }
function playerUnregistered(eim, player) { }
function leftParty(eim, player) { }
function disbandParty(eim) { }
function clearPQ(eim) { }
function allMonstersDead(eim) { }
function friendlyKilled(mob, eim) { }
function monsterKilled(mob, eim) { }