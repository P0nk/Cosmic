/*
    Tae Roon Spawner (Refactored)
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
    var territoryOfWanderingBear = em.getChannelServer().getMapFactory().getMap(250010304);
    var taeRoon = LifeFactory.getMonster(7220000);

    if (territoryOfWanderingBear.getMonsterById(7220000) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var posX = Math.floor((Math.random() * 700) - 800);
    var posY = 390;
    var spawnpoint = new java.awt.Point(posX, posY);
    territoryOfWanderingBear.spawnMonsterOnGroundBelow(taeRoon, spawnpoint);

    territoryOfWanderingBear.broadcastMessage(PacketCreator.serverNotice(6, "Tae Roon has appeared with a soft whistling sound."));
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