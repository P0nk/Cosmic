/*
    Airplane Event Script (Kerning <-> CBD) - Split Cycle
    Cycle: 60 Minutes (Real-Time)
    Phases:
    - 00:00 - 10:00: Kerning Boarding (10 mins)
    - 10:00 - 25:00: Ride to CBD (15 mins)
    - 25:00 - 30:00: Docked at CBD (Gap 5 mins)
    - 30:00 - 40:00: CBD Boarding (10 mins)
    - 40:00 - 55:00: Ride to Kerning (15 mins)
    - 55:00 - 60:00: Docked at Kerning (Gap 5 mins)
*/

// Maps
var KC_Docked;         // 103000000 (Kerning City)
var KC_BTF;            // 540010100 (Boarding Room)
var CBD_Docked;        // 540010000 (CBD)
var CBD_BTF;           // 540010001 (Boarding Room)

var Plane_to_CBD;      // 540010101
var Plane_to_KC;       // 540010002

// Durations (60 min cycle)
var BOARDING_TIME = 600000;      // 10 mins
var RIDE_TIME = 900000;          // 15 mins
var GAP_TIME = 300000;           // 5 mins

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
        var cycleTime = now % 3600000; // 60 min cycle

        // 00:00 - 10:00: Kerning Boarding
        if (cycleTime < 600000) {
            Plane_to_KC.warpEveryone(KC_Docked.getId());
            Plane_to_CBD.warpEveryone(CBD_Docked.getId());
            setupKCBoarding(600000 - cycleTime);
        }
        // 10:00 - 25:00: Ride to CBD
        else if (cycleTime < 1500000) {
            Plane_to_KC.warpEveryone(KC_Docked.getId());
            setupRideToCBD(1500000 - cycleTime);
        }
        // 25:00 - 30:00: Gap at CBD
        else if (cycleTime < 1800000) {
            Plane_to_CBD.warpEveryone(CBD_Docked.getId());
            Plane_to_KC.warpEveryone(KC_Docked.getId());
            setupDockAtCBD(1800000 - cycleTime);
        }
        // 30:00 - 40:00: CBD Boarding
        else if (cycleTime < 2400000) {
            Plane_to_CBD.warpEveryone(CBD_Docked.getId());
            setupCBDBoarding(2400000 - cycleTime);
        }
        // 40:00 - 55:00: Ride to Kerning
        else if (cycleTime < 3300000) {
            Plane_to_CBD.warpEveryone(CBD_Docked.getId());
            setupRideToKC(3300000 - cycleTime);
        }
        // 55:00 - 60:00: Gap at Kerning
        else {
            Plane_to_KC.warpEveryone(KC_Docked.getId());
            Plane_to_CBD.warpEveryone(CBD_Docked.getId());
            setupDockAtKC(3600000 - cycleTime);
        }

    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[AirPlane JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

// --- Scheduler Hooks ---
function runKCBoarding(eim) {
    setupKCBoarding(BOARDING_TIME);
}

function runRideToCBD(eim) {
    setupRideToCBD(RIDE_TIME);
}

function runDockAtCBD(eim) {
    setupDockAtCBD(GAP_TIME);
}

function runCBDBoarding(eim) {
    setupCBDBoarding(BOARDING_TIME);
}

function runRideToKC(eim) {
    setupRideToKC(RIDE_TIME);
}

function runDockAtKC(eim) {
    setupDockAtKC(GAP_TIME);
}

// --- Setup Logic ---

// 00:00 - 10:00
function setupKCBoarding(timeLeft) {
    // Start of cycle loop from DockAtKC
    Plane_to_KC.warpEveryone(KC_Docked.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "kerning");

    em.schedule("runRideToCBD", timeLeft);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    KC_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

// 10:00 - 25:00
function setupRideToCBD(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false"); // Close entry

    // Warp Waiting Room -> Ride
    KC_BTF.warpEveryone(Plane_to_CBD.getId());

    em.schedule("runDockAtCBD", timeLeft); // Next phase is GAP

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Plane_to_CBD.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

// 25:00 - 30:00
function setupDockAtCBD(timeLeft) {
    // Arrival from Kerning cleanup
    Plane_to_CBD.warpEveryone(CBD_Docked.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "false"); // Not boarding yet
    em.setProperty("location", "cbd");

    em.schedule("runCBDBoarding", timeLeft);
}

// 30:00 - 40:00
function setupCBDBoarding(timeLeft) {
    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "cbd");

    em.schedule("runRideToKC", timeLeft);

    var PacketCreator = Java.type("tools.PacketCreator");
    CBD_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

// 40:00 - 55:00
function setupRideToKC(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    // Warp Waiting Room -> Ride
    CBD_BTF.warpEveryone(Plane_to_KC.getId());

    em.schedule("runDockAtKC", timeLeft);

    var PacketCreator = Java.type("tools.PacketCreator");
    Plane_to_KC.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

// 55:00 - 60:00
function setupDockAtKC(timeLeft) {
    // Arrival from CBD cleanup
    Plane_to_KC.warpEveryone(KC_Docked.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "false");
    em.setProperty("location", "kerning");

    em.schedule("runKCBoarding", timeLeft);
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