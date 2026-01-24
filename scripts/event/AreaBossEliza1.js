/*
    Eliza1 Spawner (Refactored)
*/

var setupTask;

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
    var eliza = LifeFactory.getMonster(8220000);
    var stairwayToTheSky2 = em.getChannelServer().getMapFactory().getMap(200010300);

    if (stairwayToTheSky2.getMonsterById(8220000) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var spawnpoint = new java.awt.Point(208, 83);
    stairwayToTheSky2.spawnMonsterOnGroundBelow(eliza, spawnpoint);
    stairwayToTheSky2.broadcastMessage(PacketCreator.serverNotice(6, "Eliza has appeared with a black whirlwind."));
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