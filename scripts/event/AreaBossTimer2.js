/*
    Timer2 Spawner (Refactored)
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
    var lostTime1 = em.getChannelServer().getMapFactory().getMap(220050000);
    var timer2 = LifeFactory.getMonster(5220003);

    if (lostTime1.getMonsterById(5220003) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var posX = Math.floor((Math.random() * 1400) - 1000);
    var posY = 1030;
    var spawnpoint = new java.awt.Point(posX, posY);
    lostTime1.spawnMonsterOnGroundBelow(timer2, spawnpoint);

    lostTime1.broadcastMessage(PacketCreator.serverNotice(6, "Tick-Tock Tick-Tock! Timer makes it's presence known."));
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