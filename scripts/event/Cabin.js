/*
    Orbis <-> Leafre Travel Script (Cabin) - Real Time 15 Min Cycle
*/

// Maps
var Orbis_Station;     // 200000131
var Orbis_Docked;      // 200000132
var Leafre_Docked;     // 240000111
var Ride_To_Leafre;    // 200090200
var Ride_To_Orbis;     // 200090210
var Orbis_Arrival;     // 200000100 (Default Landing)

// System & Time
var t_OrbisBoarding = 150000; // 2.5 mins (0-150s)
var t_RideToLeafre = 300000;  // 5.0 mins (150-450s)
var t_LeafreBoarding = 150000;// 2.5 mins (450-600s)
var t_RideToOrbis = 300000;   // 5.0 mins (600-900s)

function init() {
    var mf = em.getChannelServer().getMapFactory();

    Orbis_Docked = mf.getMap(200000132);
    Leafre_Docked = mf.getMap(240000111);
    Ride_To_Leafre = mf.getMap(200090200);
    Ride_To_Orbis = mf.getMap(200090210);
    Orbis_Arrival = mf.getMap(200000131);
    Leafre_Arrival = mf.getMap(240000110);

    // Initial sync
    syncEvent();
}

function syncEvent() {
    var now = java.lang.System.currentTimeMillis();
    var cycleTime = now % 900000; // 15 min cycle

    if (cycleTime < 150000) {
        // 0 - 2.5 mins: Orbis Boarding
        runOrbisBoarding(150000 - cycleTime);
    } else if (cycleTime < 450000) {
        // 2.5 - 7.5 mins: Ride to Leafre
        runRideToLeafre(450000 - cycleTime);
    } else if (cycleTime < 600000) {
        // 7.5 - 10 mins: Leafre Boarding
        runLeafreBoarding(600000 - cycleTime);
    } else {
        // 10 - 15 mins: Ride to Orbis
        runRideToOrbis(900000 - cycleTime);
    }
}

function runOrbisBoarding(timeLeft) {
    // Arrival from Leafre
    Ride_To_Orbis.warpEveryone(Orbis_Arrival.getId(), 0);

    // Setup Orbis Boarding
    em.setProperty("location", "orbis");
    em.setProperty("docked", "true");
    em.setProperty("entry", "true");

    Orbis_Docked.setDocked(true);
    Leafre_Docked.setDocked(false); // Ensure Leafre closed

    em.schedule("runRideToLeafre", timeLeft);

    // Broadcast arrival/docking?
    Orbis_Docked.broadcastShip(true);
}

function runRideToLeafre(timeLeft) {
    // Close Orbis Boarding
    em.setProperty("entry", "false");
    em.setProperty("docked", "false");

    Orbis_Docked.setDocked(false);

    // Warp to Ride
    Orbis_Docked.warpEveryone(Ride_To_Leafre.getId());

    em.schedule("runLeafreBoarding", timeLeft);

    Orbis_Docked.broadcastShip(false);
}

function runLeafreBoarding(timeLeft) {
    // Arrival from Orbis
    Ride_To_Leafre.warpEveryone(Leafre_Arrival.getId(), 0);

    // Setup Leafre Boarding
    em.setProperty("location", "leafre");
    em.setProperty("docked", "true");
    em.setProperty("entry", "true");

    Leafre_Docked.setDocked(true);
    Orbis_Docked.setDocked(false);

    em.schedule("runRideToOrbis", timeLeft);

    Leafre_Docked.broadcastShip(true);
}

function runRideToOrbis(timeLeft) {
    // Close Leafre Boarding
    em.setProperty("entry", "false");
    em.setProperty("docked", "false");

    Leafre_Docked.setDocked(false);

    // Warp to Ride
    Leafre_Docked.warpEveryone(Ride_To_Orbis.getId());

    em.schedule("runOrbisBoarding", timeLeft);

    Leafre_Docked.broadcastShip(false);
}

// Required Filler Functions
function cancelSchedule() { }
function dispose() { }