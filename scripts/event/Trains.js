/*
    Trains (Refactored & Debug Mode)
*/

// Polyfill: Map console.log to Java System.out (Safer Version)
var console = {
    log: function(msg) {
        var System = Java.type("java.lang.System");
        System.out.println(msg);
    },
    error: function(msg) {
        var System = Java.type("java.lang.System");
        System.err.println(msg);
    }
};

var Orbis_btf;
var Train_to_Orbis;
var Orbis_docked;
var Ludibrium_btf;
var Train_to_Ludibrium;
var Ludibrium_docked;
var Orbis_Station;
var Ludibrium_Station;

var closeTime = 4 * 60 * 1000;
var beginTime = 5 * 60 * 1000;
var rideTime = 5 * 60 * 1000;

function init() {
    try {
//        console.log("[Trains JS] script init() triggered!"); // If you see this, JS is running.

        closeTime = em.getTransportationTime(closeTime);
        beginTime = em.getTransportationTime(beginTime);
        rideTime = em.getTransportationTime(rideTime);

//        console.log("[Trains JS] Transportation times set.");

        var mf = em.getChannelServer().getMapFactory();
        Orbis_btf = mf.getMap(200000122);
        Ludibrium_btf = mf.getMap(220000111);
        Train_to_Orbis = mf.getMap(200090110);
        Train_to_Ludibrium = mf.getMap(200090100);
        Orbis_docked = mf.getMap(200000121);
        Ludibrium_docked = mf.getMap(220000110);
        Orbis_Station = mf.getMap(200000100);
        Ludibrium_Station = mf.getMap(220000100);

        scheduleNew();
        console.log("[Trains JS] Initialization complete. Schedule started.");

    } catch (e) {
        console.error("[Trains JS] CRASH in init(): " + e);
    }
}

function scheduleNew() {
    try {
        em.setProperty("docked", "true");
        em.setProperty("entry", "true");
        Orbis_docked.setDocked(true);
        Ludibrium_docked.setDocked(true);

//        console.log("[Trains JS] Gates Open. Waiting for closeTime.");
        em.schedule("stopEntry", closeTime);
        em.schedule("takeoff", beginTime);
    } catch (e) {
        console.error("[Trains JS] Error in scheduleNew: " + e);
    }
}

function stopEntry() {
//    console.log("[Trains JS] Gates Closed.");
    em.setProperty("entry", "false");
}

function takeoff() {
//    console.log("[Trains JS] Takeoff!");
    em.setProperty("docked", "false");
    Orbis_docked.setDocked(false);
    Ludibrium_docked.setDocked(false);
    Orbis_btf.warpEveryone(Train_to_Ludibrium.getId());
    Ludibrium_btf.warpEveryone(Train_to_Orbis.getId());
    Orbis_docked.broadcastShip(false);
    Ludibrium_docked.broadcastShip(false);
    em.schedule("arrived", rideTime);
}

function arrived() {
//    console.log("[Trains JS] Arrived.");
    Train_to_Orbis.warpEveryone(Orbis_Station.getId(), 0);
    Train_to_Ludibrium.warpEveryone(Ludibrium_Station.getId(), 0);
    Orbis_docked.broadcastShip(true);
    Ludibrium_docked.broadcastShip(true);
    scheduleNew();
}

function cancelSchedule() {}
function dispose() {}