/*
    Orbis <-> Leafre Travel Script (Cabin) - Real Time 15 Min Cycle
*/

// Maps
var Orbis_Station;     // 200000131
var Orbis_Docked;      // 200000132
var Leafre_Docked;     // 240000111
var Ride_To_Leafre;    // 200090200
var Ride_To_Orbis;     // 200090210
var Orbis_Arrival;     // 200000131
var Leafre_Arrival;    // 240000110

// Durations
var ORBIS_BOARDING_TIME = 150000; // 2.5 mins
var RIDE_TO_LEAFRE_TIME = 300000; // 5 mins
var LEAFRE_BOARDING_TIME = 150000; // 2.5 mins
var RIDE_TO_ORBIS_TIME = 300000; // 5 mins

function init() {
    try {
        var mf = em.getChannelServer().getMapFactory();

        Orbis_Docked = mf.getMap(200000132);
        Leafre_Docked = mf.getMap(240000111);
        Ride_To_Leafre = mf.getMap(200090200);
        Ride_To_Orbis = mf.getMap(200090210);
        Orbis_Arrival = mf.getMap(200000131);
        Leafre_Arrival = mf.getMap(240000110);

        if (Orbis_Docked == null || Leafre_Docked == null) {
            var System = Java.type("java.lang.System");
            System.err.println("[Cabin JS] Maps failed to load!");
        }

        // Initial sync
        syncEvent();
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Cabin JS] Crash in init: " + e);
    }
}

function syncEvent() {
    try {
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 900000; // 15 min cycle

        if (cycleTime < 150000) {
            // 0 - 2.5 mins: Orbis Boarding
            Ride_To_Orbis.warpEveryone(Orbis_Arrival.getId());
            Ride_To_Leafre.warpEveryone(Leafre_Arrival.getId());
            setupOrbisBoarding(150000 - cycleTime);
        } else if (cycleTime < 450000) {
            // 2.5 - 7.5 mins: Ride to Leafre
            Ride_To_Orbis.warpEveryone(Orbis_Arrival.getId());
            setupRideToLeafre(450000 - cycleTime);
        } else if (cycleTime < 600000) {
            // 7.5 - 10 mins: Leafre Boarding
            Ride_To_Leafre.warpEveryone(Leafre_Arrival.getId());
            Ride_To_Orbis.warpEveryone(Orbis_Arrival.getId());
            setupLeafreBoarding(600000 - cycleTime);
        } else {
            // 10 - 15 mins: Ride to Orbis
            Ride_To_Leafre.warpEveryone(Leafre_Arrival.getId());
            setupRideToOrbis(900000 - cycleTime);
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Cabin JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

// --- Scheduler Hooks (triggered by EventManager) ---
function runOrbisBoarding(eim) {
    setupOrbisBoarding(ORBIS_BOARDING_TIME);
}

function runRideToLeafre(eim) {
    setupRideToLeafre(RIDE_TO_LEAFRE_TIME);
}

function runLeafreBoarding(eim) {
    setupLeafreBoarding(LEAFRE_BOARDING_TIME);
}

function runRideToOrbis(eim) {
    setupRideToOrbis(RIDE_TO_ORBIS_TIME);
}

// --- Setup Logic ---
function setupOrbisBoarding(timeLeft) {
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

function setupRideToLeafre(timeLeft) {
    // Close Orbis Boarding
    em.setProperty("entry", "false");
    em.setProperty("docked", "false");

    Orbis_Docked.setDocked(false);

    // Warp to Ride
    Orbis_Docked.warpEveryone(Ride_To_Leafre.getId());

    em.schedule("runLeafreBoarding", timeLeft);
    Orbis_Docked.broadcastShip(false);

    // Timer
    var PacketCreator = Java.type("tools.PacketCreator");
    Ride_To_Leafre.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupLeafreBoarding(timeLeft) {
    // Arrival from Orbis cleanup
    Ride_To_Leafre.warpEveryone(Leafre_Arrival.getId());

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

function setupRideToOrbis(timeLeft) {
    // Close Leafre Boarding
    em.setProperty("entry", "false");
    em.setProperty("docked", "false");

    Leafre_Docked.setDocked(false);

    // Warp to Ride
    Leafre_Docked.warpEveryone(Ride_To_Orbis.getId());

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