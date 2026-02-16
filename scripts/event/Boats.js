/*
    Boats Event Script (Orbis <-> Ellinia) - Split Cycle
    Cycle: 20 Minutes (Real-Time)
    Phases:
    - 00:00 - 02:00: Orbis Boarding (2 mins)
    - 02:00 - 10:00: Ride to Ellinia (8 mins)
    - 10:00 - 12:00: Ellinia Boarding (2 mins)
    - 12:00 - 20:00: Ride to Orbis (8 mins)
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
var ORBIS_BOARDING_TIME = 120000; // 2 mins
var RIDE_TO_ELLINIA_TIME = 480000; // 8 mins
var ELLINIA_BOARDING_TIME = 120000; // 2 mins
var RIDE_TO_ORBIS_TIME = 480000;   // 8 mins
var INVASION_DELAY = 180000; // 3 mins into ride

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
        var cycleTime = now % 1200000; // 20 min cycle

        if (cycleTime < 120000) {
            // 0 - 2.0 mins: Orbis Boarding
            Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
            Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());
            Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
            Ellinia_Boat_Cabin.warpEveryone(Ellinia_Station.getId());
            setupOrbisBoarding(120000 - cycleTime);
        } else if (cycleTime < 600000) {
            // 2.0 - 10.0 mins: Ride to Ellinia
            Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
            Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());
            setupRideToEllinia(600000 - cycleTime, cycleTime - 120000);
        } else if (cycleTime < 720000) {
            // 10.0 - 12.0 mins: Ellinia Boarding
            Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
            Ellinia_Boat_Cabin.warpEveryone(Ellinia_Station.getId());
            Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
            Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());
            setupElliniaBoarding(720000 - cycleTime);
        } else {
            // 12.0 - 20.0 mins: Ride to Orbis
            Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
            Ellinia_Boat_Cabin.warpEveryone(Ellinia_Station.getId());
            setupRideToOrbis(1200000 - cycleTime, cycleTime - 720000);
        }
    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Boats JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

// --- Scheduler Hooks ---
function runOrbisBoarding(eim) {
    setupOrbisBoarding(ORBIS_BOARDING_TIME);
}

function runRideToEllinia(eim) {
    setupRideToEllinia(RIDE_TO_ELLINIA_TIME, 0);
}

function runElliniaBoarding(eim) {
    setupElliniaBoarding(ELLINIA_BOARDING_TIME);
}

function runRideToOrbis(eim) {
    setupRideToOrbis(RIDE_TO_ORBIS_TIME, 0);
}

function runInvasionCheck(eim) {
    checkInvasion(eim);
}

// --- Setup Logic ---
function setupOrbisBoarding(timeLeft) {
    // Cleanup previous ride
    Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
    Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "orbis");
    em.setProperty("haveBalrog", "false");

    Orbis_Docked.setDocked(true);
    Ellinia_Docked.setDocked(false);

    em.schedule("runRideToEllinia", timeLeft);
    Orbis_Docked.broadcastShip(true);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    Orbis_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRideToEllinia(timeLeft, offset) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    Orbis_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Orbis_BTF.warpEveryone(Boat_to_Ellinia.getId());

    Orbis_Docked.broadcastShip(false);

    em.schedule("runElliniaBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Boat_to_Ellinia.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    Ellinia_Boat_Cabin.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));

    // Invasion Logic
    var timeUntilInvasion = INVASION_DELAY - offset;
    if (timeUntilInvasion > 0) {
        em.schedule("runInvasionCheck", timeUntilInvasion);
    }
}

function setupElliniaBoarding(timeLeft) {
    // Cleanup previous ride
    Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
    Ellinia_Boat_Cabin.warpEveryone(Ellinia_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "true");
    em.setProperty("location", "ellinia");
    em.setProperty("haveBalrog", "false");

    Ellinia_Docked.setDocked(true);
    Orbis_Docked.setDocked(false);

    em.schedule("runRideToOrbis", timeLeft);

    // Timers for Boarding Rooms
    var PacketCreator = Java.type("tools.PacketCreator");
    Ellinia_BTF.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
}

function setupRideToOrbis(timeLeft, offset) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    Ellinia_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Ellinia_BTF.warpEveryone(Boat_to_Orbis.getId());

    em.schedule("runOrbisBoarding", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Boat_to_Orbis.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    Orbis_Boat_Cabin.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));

    // Invasion Logic
    var timeUntilInvasion = INVASION_DELAY - offset;
    if (timeUntilInvasion > 0) {
        em.schedule("runInvasionCheck", timeUntilInvasion);
    }
}

function checkInvasion(eim) {
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