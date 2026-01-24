/*
    Mano Spawner (Refactored)
*/

function init() {
    scheduleNew();
}

function scheduleNew() {
    setupTask = em.schedule("start", 0);
}

function cancelSchedule() {
    if (setupTask != null) {
        setupTask.cancel(true);
    }
}

function start() {
    var thicketAroundTheBeach3 = em.getChannelServer().getMapFactory().getMap(104000400);
    var mano = LifeFactory.getMonster(2220000);
    if (thicketAroundTheBeach3.getMonsterById(2220000) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var spawnpoint = new java.awt.Point(279, -496);
    thicketAroundTheBeach3.spawnMonsterOnGroundBelow(mano, spawnpoint);

    thicketAroundTheBeach3.broadcastMessage(PacketCreator.serverNotice(6, "A cool breeze was felt when Mano appeared."));
    em.schedule("start", 3 * 60 * 60 * 1000);
}

// ---------- FILLER FUNCTIONS ----------
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