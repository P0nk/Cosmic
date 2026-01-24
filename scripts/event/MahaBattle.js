/*
    Vs Uncontrollable Maha (Refactored)
*/

var entryMap = 914020000;
var exitMap = 140000000;
var recruitMap = 140000000;
var clearMap = 140000000;

var minMapId = 914020000;
var maxMapId = 914020000;

var eventTime = 10; // 10 minutes
const maxLobbies = 7;

function init() {}

function setup(level, lobbyid) {
    var eim = em.newInstance("Maha" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");

    var mapObj = eim.getInstanceMap(entryMap);
    mapObj.resetPQ(level);
    mapObj.instanceMapForceRespawn();

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    return eim;
}

function playerEntry(eim, player) {
    var map = eim.getMapInstance(entryMap);
    player.changeMap(map, map.getPortal(0));
}

function scheduledTimeout(eim) {
    end(eim);
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, 0);
}

function playerLeft(eim, player) {
    if (!eim.isEventCleared()) {
        playerExit(eim, player);
    }
}

function changedMap(eim, player, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
            eim.unregisterPlayer(player);
            end(eim);
        } else {
            eim.unregisterPlayer(player);
        }
    }
}

function playerRevive(eim, player) {
    if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.unregisterPlayer(player);
    }
}

function playerDisconnected(eim, player) {
    if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.unregisterPlayer(player);
    }
}

function end(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        playerExit(eim, party.get(i));
    }
    eim.dispose();
}

function clearPQ(eim) {
    eim.stopEventTimer();
    eim.setEventCleared();
}

function isMaha(mob) {
    return mob.getId() == 9001014;
}

function monsterKilled(mob, eim) {
    if (isMaha(mob)) {
        eim.clearPQ();
    }
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function respawnStages(eim) {}
function playerUnregistered(eim, player) {}
function changedLeader(eim, leader) {}
function playerDead(eim, player) {}
function leftParty(eim, player) {}
function disbandParty(eim) {}
function monsterValue(eim, mobId) { return 1; }
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}