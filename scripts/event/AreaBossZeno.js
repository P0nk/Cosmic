/*
    Zeno Spawner (Refactored)
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
    var graysPrairie = em.getChannelServer().getMapFactory().getMap(221040301);

    if (graysPrairie.getMonsterById(6220001) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var zeno = LifeFactory.getMonster(6220001);
    graysPrairie.spawnMonsterOnGroundBelow(zeno, new java.awt.Point(-4224, 776));
    graysPrairie.broadcastMessage(PacketCreator.serverNotice(6, "Zeno has appeared with a heavy sound of machinery."));
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