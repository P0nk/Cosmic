/*
    Zakum Battle (Refactored & Fixed)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 30;
var minLevel = 50, maxLevel = 255;
var entryMap = 280030000;
var exitMap = 211042400;
var recruitMap = 211042400;
var clearMap = 211042400;

var minMapId = 280030000;
var maxMapId = 280030000;
var eventTime = 120;
const maxLobbies = 1;

function init() { setEventRequirements(); }
function getMaxLobbies() { return maxLobbies; }

function setEventRequirements() {
    var reqStr = "";
    reqStr += "\r\n    Number of players: " + minPlayers + " ~ " + maxPlayers;
    reqStr += "\r\n    Level range: " + minLevel + " ~ " + maxLevel;
    reqStr += "\r\n    Time limit: " + eventTime + " minutes";
    em.setProperty("party", reqStr);
}

function setEventExclusives(eim) { eim.setExclusiveItems([]); }

function setEventRewards(eim) {
    eim.setEventRewards(1, [], []);
    eim.setEventClearStageExp([]);
    eim.setEventClearStageMeso([]);
}

function afterSetup(eim) {
    updateGateState(1);
}

function setup(channel) {
    var eim = em.newInstance("Zakum" + channel);
    eim.setProperty("canJoin", 1);
    eim.setProperty("defeatedBoss", 0);
    eim.getInstanceMap(280030000).resetPQ(1);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function playerEntry(eim, player) {
    eim.dropMessage(5, "[Expedition] " + player.getName() + " has entered the map.");
    var map = eim.getMapInstance(entryMap);
    player.changeMap(map, map.getPortal(0));
}

function scheduledTimeout(eim) { end(eim); }

function changedMap(eim, player, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
            eim.unregisterPlayer(player);
            eim.dropMessage(5, "[Expedition] Team disbanded.");
            end(eim);
        } else {
            if (!player.isGM()) {
                eim.dropMessage(5, "[Expedition] " + player.getName() + " has left the instance.");
                eim.unregisterPlayer(player);
            }
        }
    }
}

function playerRevive(eim, player) {
    if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.dropMessage(5, "[Expedition] " + player.getName() + " has left the instance.");
        eim.unregisterPlayer(player);
    }
}

function playerDisconnected(eim, player) {
    if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.dropMessage(5, "[Expedition] " + player.getName() + " has left the instance.");
        eim.unregisterPlayer(player);
    }
}

function playerUnregistered(eim, player) {
    if (eim.isEventCleared()) {
        em.completeQuest(player, 100200, 2030010);
    }
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, 0);
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
    updateGateState(0);
}

function isZakum(mob) { return mob.getId() == 8800002; }

function monsterKilled(mob, eim) {
    if (isZakum(mob)) {
        eim.setIntProperty("defeatedBoss", 1);
        eim.showClearEffect(mob.getMap().getId());
        clearPQ(eim);
        mob.getMap().broadcastZakumVictory();
    }
}

function updateGateState(newState) {
    em.getChannelServer().getMapFactory().getMap(211042300).getReactorById(2118002).forceHitReactor(newState);
}

function dispose(eim) {
    if (!eim.isEventCleared()) updateGateState(0);
}

// ---------- FILLER FUNCTIONS ----------
function changedLeader(eim, leader) {}
function playerDead(eim, player) {}
function leftParty(eim, player) {}
function disbandParty(eim) {}
function monsterValue(eim, mobId) { return 1; }
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function allMonstersDead(eim) {}
function cancelSchedule() {}