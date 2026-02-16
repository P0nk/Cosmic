/*
    Trains Event Script (Orbis <-> Ludibrium) - Split Cycle
    Cycle: 15 Minutes (Real-Time)
    Phases:
    - 00:00 - 02:30: Orbis Boarding (2.5 mins)
    - 02:30 - 07:30: Ride to Ludi (5 mins)
    - 07:30 - 10:00: Ludi Boarding (2.5 mins)
    - 10:00 - 15:00: Ride to Orbis (5 mins)
*/

// Maps
var Orbis_Station;     // 200000100
var Orbis_Docked;      // 200000121
var Orbis_BTF;         // 200000122 (Boarding Room)
var Ludi_Station;      // 220000100
var Ludi_Docked;       // 220000110
var Ludi_BTF;          // 220000111

var Train_to_Ludi;     // 200090100
var Train_to_Orbis;    // 200090110

// Durations
var ORBIS_BOARDING_TIME = 150000; // 2.5 mins
var RIDE_TO_LUDI_TIME = 300000;   // 5 mins
var LUDI_BOARDING_TIME = 150000;  // 2.5 mins
var RIDE_TO_ORBIS_TIME = 300000;  // 5 mins

function init() {
    try {
        var mf = em.getChannelServer().getMapFactory();

        Orbis_Station = mf.getMap(200000100);
        Orbis_Docked = mf.getMap(200000121);
        Orbis_BTF = mf.getMap(200000122);

        Ludi_Station = mf.getMap(220000100);
        Ludi_Docked = mf.getMap(220000110);
        Ludi_BTF = mf.getMap(220000111);

        Train_to_Ludi = mf.getMap(200090100);
        Train_to_Orbis = mf.getMap(200090110);

        // Initial sync
        syncEvent();
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Trains JS] Crash in init: " + e);
        e.printStackTrace();
    }
}

function syncEvent() {
    try {
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 900000; // 15 min cycle

        if (cycleTime < 150000) {
            // 0 - 2.5 mins: Orbis Boarding
            Train_to_Orbis.warpEveryone(Orbis_Station.getId());
            Train_to_Ludi.warpEveryone(Ludi_Station.getId());
            setupOrbisBoarding(150000 - cycleTime);
        } else if (cycleTime < 450000) {
            // 2.5 - 7.5 mins: Ride to Ludi
            Train_to_Orbis.warpEveryone(Orbis_Station.getId());
            setupRideToLudi(450000 - cycleTime);
        } else if (cycleTime < 600000) {
            // 7.5 - 10 mins: Ludi Boarding
            Train_to_Ludi.warpEveryone(Ludi_Station.getId());
            Train_to_Orbis.warpEveryone(Orbis_Station.getId());
            setupLudiBoarding(600000 - cycleTime);
        } else {
            // 10 - 15 mins: Ride to Orbis
            Train_to_Ludi.warpEveryone(Ludi_Station.getId());
            setupRideToOrbis(900000 - cycleTime);
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Trains JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

// --- Scheduler Hooks ---
function runOrbisBoarding(eim) {
    setupOrbisBoarding(ORBIS_BOARDING_TIME);
}

function runRideToLudi(eim) {
    setupRideToLudi(RIDE_TO_LUDI_TIME);
}

function runLudiBoarding(eim) {
    setupLudiBoarding(LUDI_BOARDING_TIME);
}

function runRideToOrbis(eim) {
    setupRideToOrbis(RIDE_TO_ORBIS_TIME);
}

// --- Setup Logic ---
function setupOrbisBoarding(timeLeft) {
    // Arrival from Ludi cleanup
    Train_to_Orbis.warpEveryone(Orbis_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "orbis"); // Optional helper prop

    Orbis_Docked.setDocked(true);
    Ludi_Docked.setDocked(false);

    em.schedule("runRideToLudi", timeLeft);
    Orbis_Docked.broadcastShip(true);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    Orbis_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRideToLudi(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    Orbis_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Orbis_BTF.warpEveryone(Train_to_Ludi.getId());

    Orbis_Docked.broadcastShip(false);

    em.schedule("runLudiBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Train_to_Ludi.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupLudiBoarding(timeLeft) {
    // Arrival from Orbis cleanup
    Train_to_Ludi.warpEveryone(Ludi_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "ludi");

    Ludi_Docked.setDocked(true);
    Orbis_Docked.setDocked(false);

    em.schedule("runRideToOrbis", timeLeft);
    Ludi_Docked.broadcastShip(true);

    var PacketCreator = Java.type("tools.PacketCreator");
    Ludi_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRideToOrbis(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    Ludi_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Ludi_BTF.warpEveryone(Train_to_Orbis.getId());

    Ludi_Docked.broadcastShip(false);

    em.schedule("runOrbisBoarding", timeLeft);

    var PacketCreator = Java.type("tools.PacketCreator");
    Train_to_Orbis.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

// Required Filler Functions
function cancelSchedule() { }
function dispose() { }
function monsterValue(eim, mobId) { return 0; }
function setup(level, lobbyid) { return em.newInstance("Trains"); }
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