/*
    Nine Tailed Fox Spawner (Refactored)
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
    var moonRidge = em.getChannelServer().getMapFactory().getMap(222010310);
    var nineTailedFox = LifeFactory.getMonster(7220001);
    if (moonRidge.getMonsterById(7220001) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }
    var posX = Math.floor((Math.random() * 1300) - 800);
    var posY = 33;
    var spawnpoint = new java.awt.Point(posX, posY);
    moonRidge.spawnMonsterOnGroundBelow(nineTailedFox, spawnpoint);

    moonRidge.broadcastMessage(PacketCreator.serverNotice(6, "As the moon light dims, a long fox cry can be heard and the presence of the old fox can be felt"));
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