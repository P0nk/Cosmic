/*
    Subway Event Script (Kerning <-> NLC) - Split Cycle
    Cycle: 10 Minutes (Real-Time)
    Phases:
    - 00:00 - 01:30: Kerning Boarding (1.5 mins)
    - 01:30 - 05:00: Ride to NLC (3.5 mins)
    - 05:00 - 06:30: NLC Boarding (1.5 mins)
    - 06:30 - 10:00: Ride to Kerning (3.5 mins)
*/

// Maps
var KC_Station;        // 103000100
var KC_Docked;         // 103000100 (Station is docked?)
var KC_Waiting;        // 600010004 (Boarding Room)
var NLC_Station;       // 600010001
var NLC_Docked;        // 600010001
var NLC_Waiting;       // 600010002

var Subway_to_NLC;     // 600010005
var Subway_to_KC;      // 600010003

// Durations
var KC_BOARDING_TIME = 90000;   // 1.5 mins
var RIDE_TO_NLC_TIME = 210000;  // 3.5 mins
var NLC_BOARDING_TIME = 90000;  // 1.5 mins
var RIDE_TO_KC_TIME = 210000;   // 3.5 mins

function init() {
    try {
        var mf = em.getChannelServer().getMapFactory();

        KC_Station = mf.getMap(103000100);
        KC_Docked = mf.getMap(103000100);
        KC_Waiting = mf.getMap(600010004);

        NLC_Station = mf.getMap(600010001);
        NLC_Docked = mf.getMap(600010001);
        NLC_Waiting = mf.getMap(600010002);

        Subway_to_NLC = mf.getMap(600010005);
        Subway_to_KC = mf.getMap(600010003);

        // Initial sync
        syncEvent();
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Subway JS] Crash in init: " + e);
        e.printStackTrace();
    }
}

function syncEvent() {
    try {
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 600000; // 10 min cycle

        if (cycleTime < 90000) {
            // 0 - 1.5 mins: Kerning Boarding
            Subway_to_KC.warpEveryone(KC_Station.getId());
            Subway_to_NLC.warpEveryone(NLC_Station.getId());
            setupKCBoarding(90000 - cycleTime);
        } else if (cycleTime < 300000) {
            // 1.5 - 5.0 mins: Ride to NLC
            Subway_to_KC.warpEveryone(KC_Station.getId());
            setupRideToNLC(300000 - cycleTime);
        } else if (cycleTime < 390000) {
            // 5.0 - 6.5 mins: NLC Boarding
            Subway_to_NLC.warpEveryone(NLC_Station.getId());
            Subway_to_KC.warpEveryone(KC_Station.getId());
            setupNLCBoarding(390000 - cycleTime);
        } else {
            // 6.5 - 10.0 mins: Ride to Kerning
            Subway_to_NLC.warpEveryone(NLC_Station.getId());
            setupRideToKC(600000 - cycleTime);
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Subway JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

// --- Scheduler Hooks ---
function runKCBoarding(eim) {
    setupKCBoarding(KC_BOARDING_TIME);
}

function runRideToNLC(eim) {
    setupRideToNLC(RIDE_TO_NLC_TIME);
}

function runNLCBoarding(eim) {
    setupNLCBoarding(NLC_BOARDING_TIME);
}

function runRideToKC(eim) {
    setupRideToKC(RIDE_TO_KC_TIME);
}

// --- Setup Logic ---
function setupKCBoarding(timeLeft) {
    // Arrival from NLC cleanup
    Subway_to_KC.warpEveryone(KC_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "kerning");

    KC_Docked.setDocked(true);
    NLC_Docked.setDocked(false);

    em.schedule("runRideToNLC", timeLeft);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    KC_Waiting.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRideToNLC(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    KC_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    KC_Waiting.warpEveryone(Subway_to_NLC.getId());

    em.schedule("runNLCBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Subway_to_NLC.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupNLCBoarding(timeLeft) {
    // Arrival from Kerning cleanup
    Subway_to_NLC.warpEveryone(NLC_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "nlc");

    NLC_Docked.setDocked(true);
    KC_Docked.setDocked(false);

    em.schedule("runRideToKC", timeLeft);

    var PacketCreator = Java.type("tools.PacketCreator");
    NLC_Waiting.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRideToKC(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    NLC_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    NLC_Waiting.warpEveryone(Subway_to_KC.getId());

    em.schedule("runKCBoarding", timeLeft);

    var PacketCreator = Java.type("tools.PacketCreator");
    Subway_to_KC.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

// Required Filler Functions
function cancelSchedule() { }
function dispose() { }
function monsterValue(eim, mobId) { return 0; }
function setup(level, lobbyid) { return em.newInstance("Subway"); }
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