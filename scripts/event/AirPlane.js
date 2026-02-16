/*
    Airplane Event Script (Kerning <-> CBD) - Split Cycle
    Cycle: 30 Minutes (Real-Time)
    Phases:
    - 00:00 - 02:30: Kerning Boarding (2.5 mins)
    - 02:30 - 15:00: Ride to CBD (12.5 mins)
    - 15:00 - 17:30: CBD Boarding (2.5 mins)
    - 17:30 - 30:00: Ride to Kerning (12.5 mins)
*/

// Maps
var KC_Docked;         // 103000000 (Kerning City)
var KC_BTF;            // 540010100 (Boarding Room)
var CBD_Docked;        // 540010000 (CBD)
var CBD_BTF;           // 540010001 (Boarding Room)

var Plane_to_CBD;      // 540010101
var Plane_to_KC;       // 540010002

// Durations (30 min cycle)
var KC_BOARDING_TIME = 150000;   // 2.5 mins
var RIDE_TO_CBD_TIME = 750000;   // 12.5 mins
var CBD_BOARDING_TIME = 150000;  // 2.5 mins
var RIDE_TO_KC_TIME = 750000;    // 12.5 mins

function init() {
    try {
        var mf = em.getChannelServer().getMapFactory();

        KC_Docked = mf.getMap(103000000);
        KC_BTF = mf.getMap(540010100);

        CBD_Docked = mf.getMap(540010000);
        CBD_BTF = mf.getMap(540010001);

        Plane_to_CBD = mf.getMap(540010101);
        Plane_to_KC = mf.getMap(540010002);

        // Initial sync
        syncEvent();
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[AirPlane JS] Crash in init: " + e);
        e.printStackTrace();
    }
}

function syncEvent() {
    try {
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 1800000; // 30 min cycle

        if (cycleTime < 150000) {
            // 0 - 2.5 mins: Kerning Boarding
            Plane_to_KC.warpEveryone(KC_Docked.getId());
            Plane_to_CBD.warpEveryone(CBD_Docked.getId());
            setupKCBoarding(150000 - cycleTime);
        } else if (cycleTime < 900000) {
            // 2.5 - 15.0 mins: Ride to CBD
            Plane_to_KC.warpEveryone(KC_Docked.getId());
            setupRideToCBD(900000 - cycleTime);
        } else if (cycleTime < 1050000) {
            // 15.0 - 17.5 mins: CBD Boarding
            Plane_to_CBD.warpEveryone(CBD_Docked.getId());
            Plane_to_KC.warpEveryone(KC_Docked.getId());
            setupCBDBoarding(1050000 - cycleTime);
        } else {
            // 17.5 - 30.0 mins: Ride to Kerning
            Plane_to_CBD.warpEveryone(CBD_Docked.getId());
            setupRideToKC(1800000 - cycleTime);
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[AirPlane JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

// --- Scheduler Hooks ---
function runKCBoarding(eim) {
    setupKCBoarding(KC_BOARDING_TIME);
}

function runRideToCBD(eim) {
    setupRideToCBD(RIDE_TO_CBD_TIME);
}

function runCBDBoarding(eim) {
    setupCBDBoarding(CBD_BOARDING_TIME);
}

function runRideToKC(eim) {
    setupRideToKC(RIDE_TO_KC_TIME);
}

// --- Setup Logic ---
function setupKCBoarding(timeLeft) {
    // Arrival from CBD cleanup
    Plane_to_KC.warpEveryone(KC_Docked.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "kerning");

    // Docked logic if applicable
    // KC_Docked.setDocked(true);

    em.schedule("runRideToCBD", timeLeft);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    KC_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRideToCBD(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    // Warp Waiting Room -> Ride
    KC_BTF.warpEveryone(Plane_to_CBD.getId());

    em.schedule("runCBDBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Plane_to_CBD.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupCBDBoarding(timeLeft) {
    // Arrival from Kerning cleanup
    Plane_to_CBD.warpEveryone(CBD_Docked.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "cbd");

    // CBD_Docked.setDocked(true);

    em.schedule("runRideToKC", timeLeft);

    var PacketCreator = Java.type("tools.PacketCreator");
    CBD_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRideToKC(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    // Warp Waiting Room -> Ride
    CBD_BTF.warpEveryone(Plane_to_KC.getId());

    em.schedule("runKCBoarding", timeLeft);

    var PacketCreator = Java.type("tools.PacketCreator");
    Plane_to_KC.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

// Required Filler Functions
function cancelSchedule() { }
function dispose() { }
function monsterValue(eim, mobId) { return 0; }
function setup(level, lobbyid) { return em.newInstance("AirPlane"); }
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