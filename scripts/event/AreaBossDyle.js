/*
    Dyle Spawner (Refactored)
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
    var dangeroudCroko1 = em.getChannelServer().getMapFactory().getMap(107000300);
    if (dangeroudCroko1.getMonsterById(6220000) != null) {
        setupTask = em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var spawnpoint = new java.awt.Point(90, 119);
    dangeroudCroko1.spawnMonsterOnGroundBelow(LifeFactory.getMonster(6220000), spawnpoint);
    dangeroudCroko1.broadcastMessage(PacketCreator.serverNotice(6, "The huge crocodile Dyle has come out from the swamp."));
    setupTask = em.schedule("start", 3 * 60 * 60 * 1000);
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