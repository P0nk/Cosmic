/*
    Boats Event Script (Orbis <-> Ellinia) - Split Cycle
    Cycle: 60 Minutes (Real-Time)
    Phases:
    - 00:00 - 10:00: Ellinia Boarding (10 mins)
    - 10:00 - 25:00: Ride to Orbis (15 mins)
    - 25:00 - 30:00: Docked at Orbis (Gap 5 mins)
    - 30:00 - 40:00: Orbis Boarding (10 mins)
    - 40:00 - 55:00: Ride to Ellinia (15 mins)
    - 55:00 - 60:00: Docked at Ellinia (Gap 5 mins)
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
var BOARDING_TIME = 600000; // 10 mins
var RIDE_TIME = 900000;    // 15 mins
var GAP_TIME = 300000;     // 5 mins
var INVASION_DELAY = 300000; // 5 mins into ride

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
        var cycleTime = now % 3600000; // 60 min cycle

        // 00:00 - 10:00: Ellinia Boarding
        if (cycleTime < 600000) {
            Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
            Ellinia_Boat_Cabin.warpEveryone(Ellinia_Station.getId());
            Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
            Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());

            setupElliniaBoarding(600000 - cycleTime);
        }
        // 10:00 - 25:00: Ride to Orbis
        else if (cycleTime < 1500000) {
            Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
            Ellinia_Boat_Cabin.warpEveryone(Ellinia_Station.getId());

            setupRideToOrbis(1500000 - cycleTime, cycleTime - 600000);
        }
        // 25:00 - 30:00: Gap at Orbis
        else if (cycleTime < 1800000) {
            Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
            Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());
            Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
            Ellinia_Boat_Cabin.warpEveryone(Ellinia_Station.getId());

            setupDockAtOrbis(1800000 - cycleTime);
        }
        // 30:00 - 40:00: Orbis Boarding
        else if (cycleTime < 2400000) {
            Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
            Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());

            setupOrbisBoarding(2400000 - cycleTime);
        }
        // 40:00 - 55:00: Ride to Ellinia
        else if (cycleTime < 3300000) {
            Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
            Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());

            setupRideToEllinia(3300000 - cycleTime, cycleTime - 2400000);
        }
        // 55:00 - 60:00: Gap at Ellinia
        else {
            Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
            Ellinia_Boat_Cabin.warpEveryone(Ellinia_Station.getId());
            Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
            Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());

            setupDockAtEllinia(3600000 - cycleTime);
        }

    } catch (e) {
        var System = Java.type("java.lang.System");
        System.err.println("[Boats JS] Crash in syncEvent: " + e);
        e.printStackTrace();
    }
}

// --- Scheduler Hooks ---
function runElliniaBoarding(eim) {
    setupElliniaBoarding(BOARDING_TIME);
}

function runRideToOrbis(eim) {
    setupRideToOrbis(RIDE_TIME, 0);
}

function runDockAtOrbis(eim) {
    setupDockAtOrbis(GAP_TIME);
}

function runOrbisBoarding(eim) {
    setupOrbisBoarding(BOARDING_TIME);
}

function runRideToEllinia(eim) {
    setupRideToEllinia(RIDE_TIME, 0);
}

function runDockAtEllinia(eim) {
    setupDockAtEllinia(GAP_TIME);
}

function runInvasionCheck(eim) {
    checkInvasion(eim);
}

// --- Setup Logic ---

// 00:00 - 10:00
function setupElliniaBoarding(timeLeft) {
    // Cleanup previous ride (from DockAtEllinia)
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

// 10:00 - 25:00
function setupRideToOrbis(timeLeft, offset) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    Ellinia_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Ellinia_BTF.warpEveryone(Boat_to_Orbis.getId());

    em.schedule("runDockAtOrbis", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Boat_to_Orbis.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    Orbis_Boat_Cabin.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));

    // Invasion Logic - Random delay between 1 to 5 minutes
    var randomDelay = Math.floor(Math.random() * 240000) + 60000;
    var timeUntilInvasion = randomDelay - offset;

    // Ensure we don't schedule an immediate invasion due to high offset
    if (timeUntilInvasion < 10000) {
        timeUntilInvasion = 10000;
    }

    // Ensure invasion happens within flight time
    if (timeUntilInvasion < timeLeft - 60000) {
        em.schedule("runInvasionCheck", timeUntilInvasion);
    }
}

// 25:00 - 30:00
function setupDockAtOrbis(timeLeft) {
    // Arrival from Ellinia cleanup
    Boat_to_Orbis.warpEveryone(Orbis_Station.getId());
    Orbis_Boat_Cabin.warpEveryone(Orbis_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "false"); // Not boarding yet
    em.setProperty("location", "orbis");

    Orbis_Docked.setDocked(true);

    em.schedule("runOrbisBoarding", timeLeft);
}

// 30:00 - 40:00
function setupOrbisBoarding(timeLeft) {
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

// 40:00 - 55:00
function setupRideToEllinia(timeLeft, offset) {
    em.setProperty("docked", "false");
    em.setProperty("entry", "false");

    Orbis_Docked.setDocked(false);

    // Warp Waiting Room -> Ride
    Orbis_BTF.warpEveryone(Boat_to_Ellinia.getId());

    Orbis_Docked.broadcastShip(false);

    em.schedule("runDockAtEllinia", timeLeft);

    // Timers for Ride Maps
    var PacketCreator = Java.type("tools.PacketCreator");
    Boat_to_Ellinia.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));
    Ellinia_Boat_Cabin.broadcastMessage(PacketCreator.getClock(Math.floor(timeLeft / 1000)));

    // Invasion Logic - Random delay between 1 to 5 minutes
    var randomDelay = Math.floor(Math.random() * 240000) + 60000;
    var timeUntilInvasion = randomDelay - offset;

    // Ensure we don't schedule an immediate invasion due to high offset
    if (timeUntilInvasion < 10000) {
        timeUntilInvasion = 10000;
    }

    // Ensure invasion happens within flight time
    if (timeUntilInvasion < timeLeft - 60000) {
        em.schedule("runInvasionCheck", timeUntilInvasion);
    }
}

// 55:00 - 60:00
function setupDockAtEllinia(timeLeft) {
    // Arrival from Orbis cleanup
    Boat_to_Ellinia.warpEveryone(Ellinia_Station.getId());
    Ellinia_Boat_Cabin.warpEveryone(Ellinia_Station.getId());

    em.setProperty("docked", "true");
    em.setProperty("entry", "false");
    em.setProperty("location", "ellinia");

    Ellinia_Docked.setDocked(true);

    em.schedule("runElliniaBoarding", timeLeft);
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