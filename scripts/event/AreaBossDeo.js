/*
    Deo Spawner (Refactored)
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
    var royalCatthusDesert = em.getChannelServer().getMapFactory().getMap(260010201);

    if (royalCatthusDesert.getMonsterById(3220001) != null) {
        em.schedule("start", 3 * 60 * 60 * 1000);
        return;
    }

    var deo = LifeFactory.getMonster(3220001);
    royalCatthusDesert.spawnMonsterOnGroundBelow(deo, new java.awt.Point(645, 275));
    royalCatthusDesert.broadcastMessage(PacketCreator.serverNotice(6, "Deo slowly appeared out of the sand dust."));
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