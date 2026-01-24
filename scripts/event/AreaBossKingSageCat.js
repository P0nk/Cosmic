/*
    King Sage Cat Spawner (Refactored)
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
    var goblinForest2 = em.getChannelServer().getMapFactory().getMap(250010504);
    var kingSageCat = LifeFactory.getMonster(7220002);

    if (goblinForest2.getMonsterById(7220002) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }
    var posX = Math.floor((Math.random() * 1300) - 500);
    var posY = 540;
    var spawnpoint = new java.awt.Point(posX, posY);
    goblinForest2.spawnMonsterOnGroundBelow(kingSageCat, spawnpoint);

    goblinForest2.broadcastMessage(PacketCreator.serverNotice(6, "The ghostly air around here has become stronger. The unpleasant sound of a cat crying can be heard."));
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