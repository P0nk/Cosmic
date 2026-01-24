/*
    Door boss Spawner (Refactored)
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
    var bossMobid = 9400610;
    var bossMapid = 677000003;
    var bossMsg = "Amdusias has appeared!";

    var map = em.getChannelServer().getMapFactory().getMap(bossMapid);
    if (map.getMonsterById(bossMobid) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var boss = LifeFactory.getMonster(bossMobid);
    var bossPos = new java.awt.Point(467, 0);
    map.spawnMonsterOnGroundBelow(boss, bossPos);
    map.broadcastMessage(PacketCreator.serverNotice(6, bossMsg));

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