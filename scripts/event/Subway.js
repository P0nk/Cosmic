/*
    Subway Event Script (Kerning <-> NLC)
    Cycle: 10 Minutes (Real-Time)
    Phases:
    - 00:00 - 05:00: Boarding
    - 05:00 - 10:00: Ride
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
var BOARDING_TIME = 300000; // 5 mins
var RIDE_TIME = 300000;     // 5 mins

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

        if (cycleTime < 300000) {
            // 0 - 5 mins: Boarding
            // Ensure ride maps are empty
            Subway_to_NLC.warpEveryone(NLC_Station.getId());
            Subway_to_KC.warpEveryone(KC_Station.getId());
            setupBoarding(300000 - cycleTime);
        } else {
            // 5 - 10 mins: Ride
            // Warp waiting rooms to ride
            KC_Waiting.warpEveryone(Subway_to_NLC.getId());
            NLC_Waiting.warpEveryone(Subway_to_KC.getId());
            setupRide(600000 - cycleTime);
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Subway JS] Crash in syncEvent: " + e);
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
    // Cleanup previous ride
    Subway_to_NLC.warpEveryone(NLC_Station.getId());
    Subway_to_KC.warpEveryone(KC_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");

    KC_Docked.setDocked(true);
    NLC_Docked.setDocked(true);

    em.schedule("runRide", timeLeft);
    // KC_Docked.broadcastShip(true); // Subways might have gates/clocks?
    // NLC_Docked.broadcastShip(true);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    KC_Waiting.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    NLC_Waiting.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRide(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    KC_Docked.setDocked(false);
    NLC_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    KC_Waiting.warpEveryone(Subway_to_NLC.getId());
    NLC_Waiting.warpEveryone(Subway_to_KC.getId());

    // KC_Docked.broadcastShip(false);
    // NLC_Docked.broadcastShip(false);

    em.schedule("runBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Subway_to_NLC.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
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