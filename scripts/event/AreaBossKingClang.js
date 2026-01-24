/*
    King Clang Spawner (Refactored)
*/

var hotSand;

function init() {
    hotSand = em.getChannelServer().getMapFactory().getMap(110040000);
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
    if (hotSand.getMonsterById(5220001) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var kingClang = LifeFactory.getMonster(5220001);
    var posX = Math.floor((Math.random() * 2400) - 1600);
    var posY = 140;
    var spawnpoint = new java.awt.Point(posX, posY);
    hotSand.spawnMonsterOnGroundBelow(kingClang, spawnpoint);

    hotSand.broadcastMessage(PacketCreator.serverNotice(6, "A strange turban shell has appeared on the beach."));
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