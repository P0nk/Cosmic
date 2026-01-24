/*
    Stumpy Spawner (Refactored)
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
    var eastRockyMountain5 = em.getChannelServer().getMapFactory().getMap(101030404);
    var stumpy = LifeFactory.getMonster(3220000);

    if (eastRockyMountain5.getMonsterById(3220000) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var posX = Math.floor((Math.random() * 800) + 400);
    var posY = 1280;
    var spawnpoint = new java.awt.Point(posX, posY);
    eastRockyMountain5.spawnMonsterOnGroundBelow(stumpy, spawnpoint);

    eastRockyMountain5.broadcastMessage(PacketCreator.serverNotice(6, "Stumpy has appeared with a stumping sound that rings the Stone Mountain."));
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