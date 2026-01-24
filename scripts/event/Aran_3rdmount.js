/*
    Event - Wolves' Mount Quest (Refactored)
*/

var entryMap = 914030000;
var exitMap = 140010210;

var minMapId = 914030000;
var maxMapId = 914030000;

var eventTime = 3; // 3 minutes
const maxLobbies = 7;

function getMaxLobbies() {
    return maxLobbies;
}

function init() {
    em.setProperty("noEntry", "false");
}

function setup(level, lobbyid) {
    var eim = em.newInstance("Aran_3rdmount_" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");
    return eim;
}

function playerEntry(eim, player) {
    var mapObj = eim.getInstanceMap(entryMap);

    mapObj.resetPQ(1);
    mapObj.instanceMapForceRespawn();
    mapObj.closeMapSpawnPoints();
    respawnStages(eim);

    player.changeMap(entryMap, 1);
    em.setProperty("noEntry", "true");

    player.sendPacket(PacketCreator.getClock(eventTime * 60));
    eim.startEventTimer(eventTime * 60000);
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    eim.dispose();
    em.setProperty("noEntry", "false");
}

function scheduledTimeout(eim) {
    var player = eim.getPlayers().get(0);
    playerExit(eim, player);
    player.changeMap(exitMap);
}

function playerDisconnected(eim, player) {
    playerExit(eim, player);
}

function changedMap(eim, chr, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        playerExit(eim, chr);
    }
}

function clearPQ(eim) {
    eim.stopEventTimer();
    eim.setEventCleared();

    var player = eim.getPlayers().get(0);
    eim.unregisterPlayer(player);
    player.changeMap(exitMap);

    eim.dispose();
    em.setProperty("noEntry", "false");
}

function monsterKilled(mob, eim) {
    if (eim.getInstanceMap(entryMap).countMonsters() == 0) {
        eim.showClearEffect();
    }
}

function friendlyKilled(mob, eim) {
    if (em.getProperty("noEntry") != "false") {
        var player = eim.getPlayers().get(0);
        playerExit(eim, player);
        player.changeMap(exitMap);
    }
}

function monsterValue(eim, mobId) {
    return 1;
}

// ---------- FILLER FUNCTIONS ----------
function respawnStages(eim) {}
function playerUnregistered(eim, player) {}
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose() {}
function disbandParty(eim, player) {}
function afterSetup(eim) {}
function changedLeader(eim, leader) {}
function leftParty(eim, player) {}