/*
    Trains Event Script (Orbis <-> Ludibrium)
    Cycle: 10 Minutes (Real-Time)
    Phases:
    - 00:00 - 05:00: Boarding
    - 05:00 - 10:00: Ride
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
var Orbis_Station;     // 200000100
var Orbis_Docked;      // 200000121
var Orbis_BTF;         // 200000122 (Boarding Room)
var Ludi_Station;      // 220000100
var Ludi_Docked;       // 220000110
var Ludi_BTF;          // 220000111

var Train_to_Ludi;     // 200090100
var Train_to_Orbis;    // 200090110

// Durations
var BOARDING_TIME = 300000; // 5 mins
var RIDE_TIME = 300000;     // 5 mins

function init() {
    try {
        console.log("[Trains JS] init() started.");
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
        console.error("[Trains JS] Crash in init: " + e);
        e.printStackTrace();
    }
}

function syncEvent() {
    try {
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 600000; // 10 min cycle
        console.log("[Trains JS] Sync Event. Cycle Time: " + cycleTime);

        if (cycleTime < 300000) {
            // 0 - 5 mins: Boarding
            console.log("[Trains JS] Phase: Boarding (Sync)");
            // Ensure ride maps are empty
            Train_to_Ludi.warpEveryone(Ludi_Station.getId());
            Train_to_Orbis.warpEveryone(Orbis_Station.getId());
            setupBoarding(300000 - cycleTime);
        } else {
            // 5 - 10 mins: Ride
            console.log("[Trains JS] Phase: Ride (Sync)");
            // Warp waiting rooms to ride
            Orbis_BTF.warpEveryone(Train_to_Ludi.getId());
            Ludi_BTF.warpEveryone(Train_to_Orbis.getId());
            setupRide(600000 - cycleTime);
        }
    } catch (e) {
        console.error("[Trains JS] Crash in syncEvent: " + e);
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
    console.log("[Trains JS] Setup Boarding for " + timeLeft + "ms");

    // Cleanup previous ride
    Train_to_Ludi.warpEveryone(Ludi_Station.getId());
    Train_to_Orbis.warpEveryone(Orbis_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");

    Orbis_Docked.setDocked(true);
    Ludi_Docked.setDocked(true);

    em.schedule("runRide", timeLeft);
    Orbis_Docked.broadcastShip(true);
    Ludi_Docked.broadcastShip(true);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    Orbis_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    Ludi_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRide(timeLeft) {
    console.log("[Trains JS] Setup Ride for " + timeLeft + "ms");

    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    Orbis_Docked.setDocked(false);
    Ludi_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Orbis_BTF.warpEveryone(Train_to_Ludi.getId());
    Ludi_BTF.warpEveryone(Train_to_Orbis.getId());

    Orbis_Docked.broadcastShip(false);
    Ludi_Docked.broadcastShip(false);

    em.schedule("runBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Train_to_Ludi.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
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