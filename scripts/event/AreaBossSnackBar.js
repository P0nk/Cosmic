/*
    Snack Bar Spawner (Refactored)
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
    var snackBarMap = em.getChannelServer().getMapFactory().getMap(105090310);

    if (snackBarMap.getMonsterById(8220008) != null || snackBarMap.getMonsterById(8220009) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var setPos = [[-626, -604], [735, -600]];
    var rndPos = setPos[Math.floor(Math.random() * setPos.length)];

    var snackBar = LifeFactory.getMonster(8220008);
    snackBarMap.spawnMonsterOnGroundBelow(snackBar, new java.awt.Point(rndPos[0], rndPos[1]));
    snackBarMap.broadcastMessage(PacketCreator.serverNotice(6, "Slowly, a suspicious food stand opens up on a strangely remote place."));
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