/*
    Genie Transport (Orbis <-> Ariant)
    Cycle: 10 Minutes (Real-Time)
    Phases:
    - 00:00 - 05:00: Boarding
    - 05:00 - 10:00: Ride
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
var BOARDING_TIME = 300000; // 5 mins
var RIDE_TIME = 300000;     // 5 mins

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
        var cycleTime = now % 600000; // 10 min cycle

        if (cycleTime < 300000) {
            // 0 - 5 mins: Boarding
            // Ensure ride maps are empty
            Genie_to_Ariant.warpEveryone(Ariant_Docked.getId());
            Genie_to_Orbis.warpEveryone(Orbis_Station.getId());
            setupBoarding(300000 - cycleTime);
        } else {
            // 5 - 10 mins: Ride
            // Warp waiting rooms to ride
            Orbis_BTF.warpEveryone(Genie_to_Ariant.getId());
            Ariant_BTF.warpEveryone(Genie_to_Orbis.getId());
            setupRide(600000 - cycleTime);
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Genie JS] Crash in syncEvent: " + e);
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
    Genie_to_Ariant.warpEveryone(Ariant_Docked.getId());
    Genie_to_Orbis.warpEveryone(Orbis_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");

    Orbis_Docked.setDocked(true);
    Ariant_Docked.setDocked(true);

    em.schedule("runRide", timeLeft);
    Orbis_Docked.broadcastShip(true);
    Ariant_Docked.broadcastShip(true);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    Orbis_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    Ariant_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRide(timeLeft) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    Orbis_Docked.setDocked(false);
    Ariant_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Orbis_BTF.warpEveryone(Genie_to_Ariant.getId());
    Ariant_BTF.warpEveryone(Genie_to_Orbis.getId());

    Orbis_Docked.broadcastShip(false);
    Ariant_Docked.broadcastShip(false);

    em.schedule("runBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Genie_to_Ariant.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
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