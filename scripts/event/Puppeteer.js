/*
    Puppeteer Event (Refactored)
*/

var minPlayers = 1;
var timeLimit = 1; // 10 minutes (variable mismatch in original, keeping logic)
var eventTimer = 1000 * 60 * timeLimit;
var exitMap = 105070300;
var eventMap = 910510000;

var minMapId = 910510000;
var maxMapId = 910510000;

function init() {}

function setup(difficulty, lobbyId) {
    var eim = em.newInstance("Puppeteer_" + lobbyId);
    eim.getInstanceMap(eventMap).resetFully();
    eim.getInstanceMap(eventMap).allowSummonState(false);
    respawn(eim);
    eim.startEventTimer(eventTimer);
    return eim;
}

function playerEntry(eim, player) {
    var cave = eim.getMapInstance(eventMap);
    player.changeMap(cave, 1);
}

function scheduledTimeout(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        playerExit(eim, party.get(i));
    }
    eim.dispose();
}

function playerRevive(eim, player) {
    player.respawn(eim, exitMap);
    return false;
}

function playerDisconnected(eim, player) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        if (party.get(i).equals(player)) {
            removePlayer(eim, player);
        } else {
            playerExit(eim, party.get(i));
        }
    }
    eim.dispose();
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(exitMap);
}

function changedMap(eim, chr, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        removePlayer(eim, chr);
        eim.stopEventTimer();
        eim.setEventCleared();
        eim.dispose();
    }
}

function removePlayer(eim, player) {
    eim.unregisterPlayer(player);
    player.getMap().removePlayer(player);
    player.setMap(exitMap);
}

// ---------- FILLER FUNCTIONS ----------
function monsterValue(eim, mobId) { return -1; }
function respawn(eim) {}
function afterSetup(eim) {}
function playerDead(eim, player) {}
function leftParty(eim, player) {}
function disbandParty(eim) {}
function playerUnregistered(eim, player) {}
function cancelSchedule() {}
function dispose() {}
function clearPQ(eim) {}
function monsterKilled(mob, eim) {}
function allMonstersDead(eim) {}
function changedLeader(eim, leader) {}