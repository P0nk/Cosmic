/*
    Bamboo Warrior Spawner (Refactored)
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
    var mapObj = em.getChannelServer().getMapFactory().getMap(800020120);
    var mobObj = LifeFactory.getMonster(6090002); // Global LifeFactory

    if (mapObj.getMonsterById(6090002) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    // Use full path for Point since it isn't injected
    mapObj.spawnMonsterOnGroundBelow(mobObj, new java.awt.Point(560, 50));
    mapObj.broadcastMessage(PacketCreator.serverNotice(6, "From amongst the ruins shrouded by the mists, Bamboo Warrior appears."));
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