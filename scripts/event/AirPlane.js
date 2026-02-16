/*
    Airplane Event Script (Kerning <-> CBD)
    Cycle: 30 Minutes (Real-Time)
    Phases:
    - 00:00 - 05:00: Boarding (5 mins)
    - 05:00 - 30:00: Ride (25 mins)
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
var KC_Docked;         // 103000000 (Kerning City)
var KC_BTF;            // 540010100 (Boarding Room)
var CBD_Docked;        // 540010000 (CBD)
var CBD_BTF;           // 540010001 (Boarding Room)

var Plane_to_CBD;      // 540010101
var Plane_to_KC;       // 540010002

// Durations (30 min cycle)
var BOARDING_TIME = 300000;  // 5 mins
var RIDE_TIME = 1500000;     // 25 mins

function init() {
    try {
        console.log("[AirPlane JS] init() started.");
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
        console.error("[AirPlane JS] Crash in init: " + e);
        e.printStackTrace();
    }
}

function syncEvent() {
    try {
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 1800000; // 30 min cycle
        console.log("[AirPlane JS] Sync Event. Cycle Time: " + cycleTime);

        if (cycleTime < 300000) {
            // 0 - 5 mins: Boarding
            console.log("[AirPlane JS] Phase: Boarding (Sync)");
            // Ensure ride maps are empty
            Plane_to_CBD.warpEveryone(CBD_Docked.getId());
            Plane_to_KC.warpEveryone(KC_Docked.getId());
            setupBoarding(300000 - cycleTime);
        } else {
            // 5 - 30 mins: Ride
            console.log("[AirPlane JS] Phase: Ride (Sync)");
            // Warp waiting rooms to ride
            KC_BTF.warpEveryone(Plane_to_CBD.getId());
            CBD_BTF.warpEveryone(Plane_to_KC.getId());
            setupRide(1800000 - cycleTime);
        }
    } catch (e) {
        console.error("[AirPlane JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

// --- Scheduler Hooks ---
function runBoarding(eim) {
    setupBoarding(BOARDING_TIME);
}

function runRide(eim) {
    setupRide(RIDE_TIME);
}

// --- Setup Logic ---
function setupBoarding(timeLeft) {
    console.log("[AirPlane JS] Setup Boarding for " + timeLeft + "ms");

    // Cleanup previous ride
    Plane_to_CBD.warpEveryone(CBD_Docked.getId());
    Plane_to_KC.warpEveryone(KC_Docked.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");

    // Docked logic unknown for these maps, usually station NPCs check property
    // But setting maps to docked just in case
    // KC_Docked.setDocked(true); 
    // CBD_Docked.setDocked(true);

    em.schedule("runRide", timeLeft);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    KC_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    CBD_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRide(timeLeft) {
    console.log("[AirPlane JS] Setup Ride for " + timeLeft + "ms");

    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    // Warp Waiting Room -> Ride
    KC_BTF.warpEveryone(Plane_to_CBD.getId());
    CBD_BTF.warpEveryone(Plane_to_KC.getId());

    em.schedule("runBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Plane_to_CBD.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
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