/*
    Centipede Spawner (Refactored)
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
    var herbGarden = em.getChannelServer().getMapFactory().getMap(251010102);

    if (herbGarden.getMonsterById(5220004) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    // LifeFactory and PacketCreator are injected globally now
    var gcent = LifeFactory.getMonster(5220004);
    herbGarden.spawnMonsterOnGroundBelow(gcent, new java.awt.Point(560, 50));
    herbGarden.broadcastMessage(PacketCreator.serverNotice(6, "From the mists surrounding the herb garden, the gargantuous Giant Centipede appears."));
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