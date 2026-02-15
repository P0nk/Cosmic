/*
    Orbis <-> Leafre Travel Script (Cabin)
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
var Orbis_Station;     // 200000131 (User Provided) // Station to verify ticket? Or is this just the map before docked?
// Actually script logic usually moves from Docked -> Ride -> Destination Station
// The NPC handles Station -> Docked

var Orbis_Docked;      // 200000132
var Leafre_Docked;     // 240000111

var Ride_To_Leafre;    // 200090200
var Ride_To_Orbis;     // 200090210

var Orbis_Arrival;     // 200000100 (Standard)
var Leafre_Arrival;    // 240000100 (Standard)

// Time Settings
var closeTime = 4 * 60 * 1000;    // Boarding closes at 4 mins
var beginTime = 5 * 60 * 1000;    // Ride starts at 5 mins
var rideTime = 5 * 60 * 1000;     // Ride lasts 5 mins (User Requested)

function init() {
    try {
        closeTime = em.getTransportationTime(closeTime);
        beginTime = em.getTransportationTime(beginTime);
        rideTime = em.getTransportationTime(rideTime);

        var mf = em.getChannelServer().getMapFactory();

        Orbis_Docked = mf.getMap(200000132);
        Leafre_Docked = mf.getMap(240000111);

        Ride_To_Leafre = mf.getMap(200090200);
        Ride_To_Orbis = mf.getMap(200090210);

        Orbis_Arrival = mf.getMap(200000100);
        Leafre_Arrival = mf.getMap(240000100);

        scheduleNew();
        console.log("[Cabin JS] Travel initialized.");
    } catch (e) {
        console.error("[Cabin JS] Error in init: " + e);
    }
}

function scheduleNew() {
    try {
        em.setProperty("docked", "true");
        em.setProperty("entry", "true");

        Orbis_Docked.setDocked(true);
        Leafre_Docked.setDocked(true);

        em.schedule("stopEntry", closeTime);
        em.schedule("takeoff", beginTime);
    } catch (e) {
        console.error("[Cabin JS] Error in scheduleNew: " + e);
    }
}

function stopEntry() {
    em.setProperty("entry", "false");
    // Could spawn boxes or clear objects if needed
}

function takeoff() {
    em.setProperty("docked", "false");

    Orbis_Docked.setDocked(false);
    Leafre_Docked.setDocked(false);

    // Warp from Docked to Ride Maps
    Orbis_Docked.warpEveryone(Ride_To_Leafre.getId());
    Leafre_Docked.warpEveryone(Ride_To_Orbis.getId());

    // Broadcast ship movement (optional visual)
    Orbis_Docked.broadcastShip(false);
    Leafre_Docked.broadcastShip(false);

    em.schedule("arrived", rideTime);
}

function arrived() {
    // Warp from Ride Maps to Destination Stations
    Ride_To_Leafre.warpEveryone(Leafre_Arrival.getId(), 0);
    Ride_To_Orbis.warpEveryone(Orbis_Arrival.getId(), 0);

    // Broadcast ship arrival
    Orbis_Docked.broadcastShip(true);
    Leafre_Docked.broadcastShip(true);

    scheduleNew();
}

// Required Filler Functions
function cancelSchedule() { }
function dispose() { }