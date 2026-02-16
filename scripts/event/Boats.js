/*
    Boats Event Script (Orbis <-> Ellinia)
    Cycle: 15 Minutes (Real-Time)
    Phases:
    - 00:00 - 05:00: Boarding
    - 05:00 - 15:00: Ride (Invasion check at 08:00)
*/

// Maps
var Orbis_Station;     // 200000100
var Orbis_Docked;      // 200000111
var Orbis_BTF;         // 200000112 (Boarding Room)
var Ellinia_Station;   // 101000300
var Ellinia_Docked;    // 101000300
var Ellinia_BTF;       // 101000301 (Boarding Room)

var Boat_to_Orbis;     // 200090010
var Orbis_Boat_Cabin;  // 200090011
var Boat_to_Ellinia;   // 200090000
var Ellinia_Boat_Cabin;// 200090001

// Durations
var BOARDING_TIME = 300000; // 5 mins
var RIDE_TIME = 600000;     // 10 mins
var INVASION_DELAY = 180000;// 3 mins into ride

function init() {
    try {
        var mf = em.getChannelServer().getMapFactory();

        Orbis_Station = mf.getMap(200000100);
        Orbis_Docked = mf.getMap(200000111);
        Orbis_BTF = mf.getMap(200000112);

        Ellinia_Station = mf.getMap(101000300);
        Ellinia_Docked = mf.getMap(101000300);
        Ellinia_BTF = mf.getMap(101000301);

        Boat_to_Orbis = mf.getMap(200090010);
        Orbis_Boat_Cabin = mf.getMap(200090011);
        Boat_to_Ellinia = mf.getMap(200090000);
        Ellinia_Boat_Cabin = mf.getMap(200090001);

        // Initial sync
        syncEvent();
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Boats JS] Crash in init: " + e);
        e.printStackTrace();
    }
}

function syncEvent() {
    try {
        var now = java.lang.System.currentTimeMillis();
        var cycleTime = now % 900000; // 15 min cycle

        if (cycleTime < 300000) {
            // 0 - 5 mins: Boarding
            // Ensure ride maps are empty (warp to station)
            Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
            Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
            setupBoarding(300000 - cycleTime);
        } else {
            // 5 - 15 mins: Ride
            // Warp waiting rooms to ride
            Orbis_BTF.warpEveryone(Boat_to_Ellinia.getId());
            Ellinia_BTF.warpEveryone(Boat_to_Orbis.getId());
            setupRide(900000 - cycleTime, cycleTime);
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Boats JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

// --- Scheduler Hooks ---
function runBoarding(eim) {
    setupBoarding(BOARDING_TIME);
}

function runRide(eim) {
    setupRide(RIDE_TIME, 0); // 0 offset implies fresh start
}

function runInvasionCheck(eim) {
    checkInvasion();
}

// --- Setup Logic ---
function setupBoarding(timeLeft) {
    // Cleanup previous ride
    Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
    Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());
    Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
    Ellinia_Boat_Cabin.warpEveryone(Ellinia_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("haveBalrog", "false"); // Reset balrog status

    Orbis_Docked.setDocked(true);

    em.schedule("runRide", timeLeft);
    Orbis_Docked.broadcastShip(true);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    Orbis_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    Ellinia_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRide(timeLeft, offset) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");
    Orbis_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Orbis_BTF.warpEveryone(Boat_to_Ellinia.getId());
    Ellinia_BTF.warpEveryone(Boat_to_Orbis.getId());

    Orbis_Docked.broadcastShip(false);

    em.schedule("runBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Boat_to_Ellinia.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    Boat_to_Orbis.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    Orbis_Boat_Cabin.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    Ellinia_Boat_Cabin.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));

    // Invasion Logic
    var timeUntilInvasion = INVASION_DELAY - offset; // 3 mins - offset
    if (timeUntilInvasion > 0) {
        em.schedule("runInvasionCheck", timeUntilInvasion);
    }
}

function checkInvasion() {
    if (Math.random() < 0.4) {
        em.setProperty("haveBalrog", "true");

        var PacketCreator = Java.type("tools.PacketCreator");
        var music = PacketCreator.musicChange("Bgm04/ArabPirate");

        // Broadcast music and spawn
        Boat_to_Orbis.broadcastMessage(music);
        Boat_to_Ellinia.broadcastMessage(music);

        Boat_to_Orbis.broadcastEnemyShip(true);
        Boat_to_Ellinia.broadcastEnemyShip(true);

        spawnBalrogs();
    }
}

function spawnBalrogs() {
    var LifeFactory = Java.type("server.life.LifeFactory");
    var Point = Java.type("java.awt.Point");

    // Crimson Balrog (8150000)
    var map1 = Boat_to_Ellinia;
    var pos1 = new Point(-538, 143);
    map1.spawnMonsterOnGroundBelow(LifeFactory.getMonster(8150000), pos1);
    map1.spawnMonsterOnGroundBelow(LifeFactory.getMonster(8150000), pos1);

    var map2 = Boat_to_Orbis;
    var pos2 = new Point(339, 148);
    map2.spawnMonsterOnGroundBelow(LifeFactory.getMonster(8150000), pos2);
    map2.spawnMonsterOnGroundBelow(LifeFactory.getMonster(8150000), pos2);
}

// Required Filler Functions
function cancelSchedule() { }
function dispose() { }
function monsterValue(eim, mobId) { return 0; }
function setup(level, lobbyid) { return em.newInstance("Boats"); }
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