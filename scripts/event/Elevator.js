/*
    Elevator (Refactored & Fixed)
*/

var beginTime = 60 * 1000;
var rideTime = 60 * 1000;

function init() {
    beginTime = em.getTransportationTime(beginTime);
    rideTime = em.getTransportationTime(rideTime);

    em.getChannelServer().getMapFactory().getMap(222020100).resetReactors();
    em.getChannelServer().getMapFactory().getMap(222020200).resetReactors();

    scheduleNew();
}

function scheduleNew() {
    em.setProperty("goingUp", "false");
    em.setProperty("goingDown", "true");

    em.getChannelServer().getMapFactory().getMap(222020100).resetReactors();
    em.getChannelServer().getMapFactory().getMap(222020200).setReactorState();
    em.schedule("goingUpNow", beginTime);
}

function goUp() {
    em.schedule("goingUpNow", beginTime);
}

function goDown() {
    em.schedule("goingDownNow", beginTime);
}

function goingUpNow() {
    em.getChannelServer().getMapFactory().getMap(222020110).warpEveryone(222020111);
    em.setProperty("goingUp", "true");
    em.schedule("isUpNow", rideTime);

    em.getChannelServer().getMapFactory().getMap(222020100).setReactorState();
}

function goingDownNow() {
    em.getChannelServer().getMapFactory().getMap(222020210).warpEveryone(222020211);
    em.setProperty("goingDown", "true");
    em.schedule("isDownNow", rideTime);

    em.getChannelServer().getMapFactory().getMap(222020200).setReactorState();
}

function isUpNow() {
    em.setProperty("goingDown", "false");
    em.getChannelServer().getMapFactory().getMap(222020200).resetReactors();
    em.getChannelServer().getMapFactory().getMap(222020111).warpEveryone(222020200, 0);

    goDown();
}

function isDownNow() {
    em.setProperty("goingUp", "false");
    em.getChannelServer().getMapFactory().getMap(222020100).resetReactors();
    em.getChannelServer().getMapFactory().getMap(222020211).warpEveryone(222020100, 4);

    goUp();
}

// ---------- FILLER FUNCTIONS ----------
function cancelSchedule() {}
function dispose() {}
function setup(eim, leaderid) {}
function monsterValue(eim, mobid) {return 0;}
function disbandParty(eim, player) {}
function playerDisconnected(eim, player) {}
function playerEntry(eim, player) {}
function monsterKilled(mob, eim) {}
function scheduledTimeout(eim) {}
function afterSetup(eim) {}
function changedLeader(eim, leader) {}
function playerExit(eim, player) {}
function leftParty(eim, player) {}
function clearPQ(eim) {}
function allMonstersDead(eim) {}
function playerUnregistered(eim, player) {}