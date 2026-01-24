/*
    Faust1 Spawner (Refactored)
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
    var theForestOfEvil1 = em.getChannelServer().getMapFactory().getMap(100040105);
    if (theForestOfEvil1.getMonsterById(5220002) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var faust1 = LifeFactory.getMonster(5220002);
    var spawnpoint = new java.awt.Point(456, 278);
    theForestOfEvil1.spawnMonsterOnGroundBelow(faust1, spawnpoint);
    theForestOfEvil1.broadcastMessage(PacketCreator.serverNotice(6, "Faust appeared amidst the blue fog."));
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