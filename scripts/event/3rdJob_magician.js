/*
    3rd Job Event - Magician (Refactored)
**/

var entryMap = 108010200;
var exitMap = 100040106;

var minMapId = 108010200;
var maxMapId = 108010201;

var eventTime = 20; // 20 minutes
const maxLobbies = 7;

function getMaxLobbies() {
    return maxLobbies;
}

function init() {
    em.setProperty("noEntry", "false");
}

function setup(level, lobbyid) {
    var eim = em.newInstance("3rdJob_magician_" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");
    return eim;
}

function playerEntry(eim, player) {
    eim.getInstanceMap(maxMapId).resetPQ(1);

    player.changeMap(entryMap, 0);
    em.setProperty("noEntry", "true");

    // PacketCreator is auto-injected
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
    playerExit(eim, eim.getPlayers().get(0));
    player.changeMap(exitMap);
}

function playerDisconnected(eim, player) {
    playerExit(eim, player);
}

function clear(eim) {
    var player = eim.getPlayers().get(0);
    eim.unregisterPlayer(player);
    player.changeMap(exitMap);

    eim.dispose();
    em.setProperty("noEntry", "false");
}

function changedMap(eim, chr, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        playerExit(eim, chr);
    }
}

function monsterValue(eim, mobId) {
    return 1;
}

// ---------- FILLER FUNCTIONS ----------
function playerUnregistered(eim, player) {}
function monsterKilled(mob, eim) {}
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose() {}
function disbandParty(eim, player) {}
function afterSetup(eim) {}
function changedLeader(eim, leader) {}
function leftParty(eim, player) {}
function clearPQ(eim) {}