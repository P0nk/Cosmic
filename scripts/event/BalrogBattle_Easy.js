/*
    Balrog Battle Easy (Refactored)
*/

var isPq = true;
var minPlayers = 3, maxPlayers = 30;
var minLevel = 50, maxLevel = 255;
var entryMap = 105100400;
var exitMap = 105100100;
var recruitMap = 105100100;
var clearMap = 105100401;

var minMapId = 105100400;
var maxMapId = 105100401;

var minMobId = 8830007;
var maxMobId = 8830013;
var bossMobId = 8830010;

var eventTime = 60; // 60 minutes
var releaseClawTime = 1;
const maxLobbies = 1;

function init() {
    setEventRequirements();
}

function getMaxLobbies() {
    return maxLobbies;
}

function setEventRequirements() {
    var reqStr = "";
    reqStr += "\r\n    Number of players: ";
    reqStr += (maxPlayers - minPlayers >= 1) ? minPlayers + " ~ " + maxPlayers : minPlayers;
    reqStr += "\r\n    Level range: ";
    reqStr += (maxLevel - minLevel >= 1) ? minLevel + " ~ " + maxLevel : minLevel;
    reqStr += "\r\n    Time limit: " + eventTime + " minutes";

    em.setProperty("party", reqStr);
}

function setEventExclusives(eim) {
    eim.setExclusiveItems([]);
}

function setEventRewards(eim) {
    eim.setEventRewards(1, [], []);
    eim.setEventClearStageExp([]);
}

function getEligibleParty(party) {
    var eligible = [];
    var hasLeader = false;

    if (party.size() > 0) {
        var partyList = party.toArray();
        for (var i = 0; i < party.size(); i++) {
            var ch = partyList[i];
            if (ch.getMapId() == recruitMap && ch.getLevel() >= minLevel && ch.getLevel() <= maxLevel) {
                if (ch.isLeader()) hasLeader = true;
                eligible.push(ch);
            }
        }
    }

    if (!(hasLeader && eligible.length >= minPlayers && eligible.length <= maxPlayers)) {
        eligible = [];
    }
    return Java.to(eligible, Java.type('net.server.world.PartyCharacter[]'));
}

function setup(level, lobbyid) {
    var eim = em.newInstance("Balrog" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");

    eim.getInstanceMap(105100400).resetPQ(level);
    eim.getInstanceMap(105100401).resetPQ(level);
    eim.schedule("releaseLeftClaw", releaseClawTime * 60000);

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function afterSetup(eim) {
    spawnBalrog(eim);
}

function respawnStages(eim) {}

function releaseLeftClaw(eim) {
    eim.getInstanceMap(entryMap).killMonster(8830013);
}

function spawnBalrog(eim) {
    var mapObj = eim.getInstanceMap(entryMap);
    var spawnPoint = new java.awt.Point(412, 258);

    mapObj.spawnFakeMonsterOnGroundBelow(LifeFactory.getMonster(8830007), spawnPoint);
    mapObj.spawnMonsterOnGroundBelow(LifeFactory.getMonster(8830009), spawnPoint);
    mapObj.spawnMonsterOnGroundBelow(LifeFactory.getMonster(8830013), spawnPoint);
}

function spawnSealedBalrog(eim) {
    eim.getInstanceMap(entryMap).spawnMonsterOnGroundBelow(LifeFactory.getMonster(bossMobId), new java.awt.Point(412, 258));
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
        if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
            eim.unregisterPlayer(player);
            end(eim);
        } else {
            eim.unregisterPlayer(player);
        }
    }
}

function playerRevive(eim, player) {
    if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.unregisterPlayer(player);
    }
}

function playerDisconnected(eim, player) {
    if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.unregisterPlayer(player);
    }
}

function monsterValue(eim, mobId) {
    return 1;
}

function end(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        playerExit(eim, party.get(i));
    }
    eim.dispose();
}

function giveRandomEventReward(eim, player) {
    eim.giveEventReward(player);
}

function clearPQ(eim) {
    eim.stopEventTimer();
    eim.setEventCleared();
}

function isUnsealedBalrog(mob) {
    var balrogid = mob.getId() - 8830007;
    return balrogid >= 0 && balrogid <= 2;
}

function isBalrogBody(mob) {
    return mob.getId() == minMobId;
}

function monsterKilled(mob, eim) {
    if (isUnsealedBalrog(mob)) {
        var count = eim.getIntProperty("boss");

        if (count == 2) {
            eim.showClearEffect();
            eim.clearPQ();

            eim.dispatchRaiseQuestMobCount(bossMobId, entryMap);
            mob.getMap().broadcastBalrogVictory(eim.getLeader().getName());
        } else {
            if (count == 1) {
                var mapobj = eim.getInstanceMap(entryMap);
                mapobj.makeMonsterReal(mapobj.getMonsterById(8830007));
            }
            eim.setIntProperty("boss", count + 1);
        }

        if (isBalrogBody(mob)) {
            eim.schedule("spawnSealedBalrog", 10 * 1000);
        }
    }
}

// ---------- FILLER FUNCTIONS ----------
function playerUnregistered(eim, player) {}
function changedLeader(eim, leader) {}
function playerDead(eim, player) {}
function leftParty(eim, player) {}
function disbandParty(eim) {}
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}