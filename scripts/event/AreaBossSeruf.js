/*
    Seruf Spawner (Refactored)
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
    var theSeaweedTower = em.getChannelServer().getMapFactory().getMap(230020100);
    var seruf = LifeFactory.getMonster(4220001);

    if (theSeaweedTower.getMonsterById(4220001) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var posX = Math.floor((Math.random() * 2300) - 1500);
    var posY = 520;
    var spawnpoint = new java.awt.Point(posX, posY);
    theSeaweedTower.spawnMonsterOnGroundBelow(seruf, spawnpoint);

    theSeaweedTower.broadcastMessage(PacketCreator.serverNotice(6, "A strange shell has appeared from a grove of seaweed"));
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