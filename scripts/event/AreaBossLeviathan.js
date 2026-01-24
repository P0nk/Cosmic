/*
    Leviathan Spawner (Refactored)
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
    var leviathansCanyon = em.getChannelServer().getMapFactory().getMap(240040401);
    var leviathan = LifeFactory.getMonster(8220003);
    if (leviathansCanyon.getMonsterById(8220003) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var posX = Math.floor((Math.random() * 600) - 300);
    var posY = 1125;
    var spawnpoint = new java.awt.Point(posX, posY);
    leviathansCanyon.spawnMonsterOnGroundBelow(leviathan, spawnpoint);

    leviathansCanyon.broadcastMessage(PacketCreator.serverNotice(6, "Leviathan emerges from the canyon and the cold icy wind blows."));
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