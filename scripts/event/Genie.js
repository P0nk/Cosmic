/*
    Genie Transport (Orbis <-> Ariant) - Split Cycle
    Cycle: 20 Minutes (Real-Time)
    Phases:
    - 00:00 - 02:30: Orbis Boarding (2.5 mins)
    - 02:30 - 10:00: Ride to Ariant (7.5 mins)
    - 10:00 - 12:30: Ariant Boarding (2.5 mins)
    - 12:30 - 20:00: Ride to Orbis (7.5 mins)
*/

// Maps
var Orbis_Station;     // 200000100
var Orbis_Docked;      // 200000151
var Orbis_BTF;         // 200000152 (Boarding Room)
var Ariant_Docked;     // 260000100 (Station/Dock combined?)
var Ariant_BTF;        // 260000110

var Genie_to_Ariant;   // 200090400
var Genie_to_Orbis;    // 200090410

// Durations
var ORBIS_BOARDING_TIME = 150000; // 2.5 mins
var RIDE_TO_ARIANT_TIME = 450000; // 7.5 mins
var ARIANT_BOARDING_TIME = 150000; // 2.5 mins
var RIDE_TO_ORBIS_TIME = 450000;   // 7.5 mins

function init() {
    try {
        var mf = em.getChannelServer().getMapFactory();

        Orbis_Station = mf.getMap(200000100);
        Orbis_Docked = mf.getMap(200000151);
        Orbis_BTF = mf.getMap(200000152);

        Ariant_Docked = mf.getMap(260000100);
        Ariant_BTF = mf.getMap(260000110);

        Genie_to_Ariant = mf.getMap(200090400);
        Genie_to_Orbis = mf.getMap(200090410);

        // Initial sync
        syncEvent();
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Genie JS] Crash in init: " + e);
        e.printStackTrace();
    }
}

function syncEvent() {
    try {
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 1200000; // 20 min cycle

        if (cycleTime < 150000) {
            // 0 - 2.5 mins: Orbis Boarding
            Genie_to_Orbis.warpEveryone(Orbis_Station.getId());
            Genie_to_Ariant.warpEveryone(Ariant_Docked.getId());
            setupOrbisBoarding(150000 - cycleTime);
        } else if (cycleTime < 600000) {
            // 2.5 - 10.0 mins: Ride to Ariant
            Genie_to_Orbis.warpEveryone(Orbis_Station.getId());
            setupRideToAriant(600000 - cycleTime);
        } else if (cycleTime < 750000) {
            // 10.0 - 12.5 mins: Ariant Boarding
            Genie_to_Ariant.warpEveryone(Ariant_Docked.getId());
            Genie_to_Orbis.warpEveryone(Orbis_Station.getId());
            setupAriantBoarding(750000 - cycleTime);
        } else {
            // 12.5 - 20.0 mins: Ride to Orbis
            Genie_to_Ariant.warpEveryone(Ariant_Docked.getId());
            setupRideToOrbis(1200000 - cycleTime);
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Genie JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

// --- Scheduler Hooks ---
function runOrbisBoarding(eim) {
    setupOrbisBoarding(ORBIS_BOARDING_TIME);
}

function runRideToAriant(eim) {
    setupRideToAriant(RIDE_TO_ARIANT_TIME);
}

function runAriantBoarding(eim) {
    setupAriantBoarding(ARIANT_BOARDING_TIME);
}

function runRideToOrbis(eim) {
    setupRideToOrbis(RIDE_TO_ORBIS_TIME);
}

// --- Setup Logic ---
function setupOrbisBoarding(timeLeft) {
    // Arrival from Ariant cleanup
    Genie_to_Orbis.warpEveryone(Orbis_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "orbis");

    Orbis_Docked.setDocked(true);
    Ariant_Docked.setDocked(false);

    em.schedule("runRideToAriant", timeLeft);
    Orbis_Docked.broadcastShip(true);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    Orbis_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRideToAriant(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    Orbis_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Orbis_BTF.warpEveryone(Genie_to_Ariant.getId());

    Orbis_Docked.broadcastShip(false);

    em.schedule("runAriantBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Genie_to_Ariant.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupAriantBoarding(timeLeft) {
    // Arrival from Orbis cleanup
    Genie_to_Ariant.warpEveryone(Ariant_Docked.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "ariant");

    Ariant_Docked.setDocked(true);
    Orbis_Docked.setDocked(false);

    em.schedule("runRideToOrbis", timeLeft);
    Ariant_Docked.broadcastShip(true); // Ariant might not have ship animation

    var PacketCreator = Java.type("tools.PacketCreator");
    Ariant_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRideToOrbis(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    Ariant_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Ariant_BTF.warpEveryone(Genie_to_Orbis.getId());

    Ariant_Docked.broadcastShip(false);

    em.schedule("runOrbisBoarding", timeLeft);

    var PacketCreator = Java.type("tools.PacketCreator");
    Genie_to_Orbis.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

// Required Filler Functions
function cancelSchedule() { }
function dispose() { }
function monsterValue(eim, mobId) { return 0; }
function setup(level, lobbyid) { return em.newInstance("Genie"); }
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