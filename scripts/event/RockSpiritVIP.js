/*
    Rock Spirit VIP (Refactored)
*/

var entryMap;
var exitMap;
var otherMap;

var minMapId = 103040410;
var maxMapId = 103040460;

var minPlayers = 1;
var fightTime = 30;
var timer = 1000 * 60 * fightTime;

function init() {
    exitMap = em.getChannelServer().getMapFactory().getMap(103040400);
    entryMap = em.getChannelServer().getMapFactory().getMap(103040440);
    otherMap = em.getChannelServer().getMapFactory().getMap(103040450);
}

function setup(level, lobbyid) {
    var eim = em.newInstance("RockSpiritVIP_" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");

    respawn(eim);
    eim.startEventTimer(timer);
    return eim;
}

function respawn(eim) {
    var map = eim.getMapInstance(entryMap.getId());
    var map2 = eim.getMapInstance(otherMap.getId());
    map.allowSummonState(true);
    map2.allowSummonState(true);
    map.instanceMapRespawn();
    map2.instanceMapRespawn();
    eim.schedule("respawn", 10000);
}

function playerEntry(eim, player) {
    var amplifierMap = eim.getMapInstance(entryMap.getId());
    player.changeMap(amplifierMap, 1);
    eim.schedule("timeOut", timer);
}

function playerRevive(eim, player) {
    player.respawn(eim, exitMap);
    return false;
}

function playerDisconnected(eim, player) {
    if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.unregisterPlayer(player);
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

function end(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        playerExit(eim, party.get(i));
    }
    eim.dispose();
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, exitMap.getPortal(0));
}

function timeOut(eim) {
    end(eim);
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function playerDead(eim, player) {}
function monsterValue(eim, mobId) { return -1; }
function leftParty(eim, player) {}
function disbandParty(eim) {}
function playerUnregistered(eim, player) {}
function cancelSchedule() {}
function dispose() {}
function clearPQ(eim) {}
function monsterKilled(mob, eim) {}
function allMonstersDead(eim) {}
function scheduledTimeout(eim) {}
function changedLeader(eim, leader) {}