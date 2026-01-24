/*
    Horntail Battle (Refactored)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 30;
var minLevel = 100, maxLevel = 255;
var entryMap = 240060000;
var exitMap = 240050600;
var recruitMap = 240050400;
var clearMap = 240050600;

var minMapId = 240060000;
var maxMapId = 240060200;
var eventTime = 120;

const maxLobbies = 1;

function init() {
    setEventRequirements();
}

function getMaxLobbies() {
    return maxLobbies;
}

function setEventRequirements() {
    var reqStr = "";
    reqStr += "\r\n    Number of players: " + (maxPlayers - minPlayers >= 1 ? minPlayers + " ~ " + maxPlayers : minPlayers);
    reqStr += "\r\n    Level range: " + (maxLevel - minLevel >= 1 ? minLevel + " ~ " + maxLevel : minLevel);
    reqStr += "\r\n    Time limit: " + eventTime + " minutes";
    em.setProperty("party", reqStr);
}

function setEventExclusives(eim) {
    eim.setExclusiveItems([]);
}

function setEventRewards(eim) {
    eim.setEventRewards(1, [], []);
    eim.setEventClearStageExp([]);
    eim.setEventClearStageMeso([]);
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

function setup(channel) {
    var eim = em.newInstance("Horntail" + channel);
    eim.setProperty("killCount", 0);
    eim.setProperty("canJoin", 1);
    eim.setProperty("defeatedBoss", 0);
    eim.setProperty("defeatedHead", 0);

    var level = 1;
    eim.getInstanceMap(240060000).resetPQ(level);
    eim.getInstanceMap(240060100).resetPQ(level);
    eim.getInstanceMap(240060200).resetPQ(level);

    var map, mob;
    map = eim.getInstanceMap(240060000);
    mob = LifeFactory.getMonster(8810000);
    map.spawnMonsterOnGroundBelow(mob, new java.awt.Point(960, 120));

    map = eim.getInstanceMap(240060100);
    mob = LifeFactory.getMonster(8810001);
    map.spawnMonsterOnGroundBelow(mob, new java.awt.Point(-420, 120));

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

function scheduledTimeout(eim) {
    end(eim);
}

function changedMap(eim, player, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
            eim.unregisterPlayer(player);
            eim.dropMessage(5, "[Expedition] Either the leader has quit the expedition or there is no longer the minimum number of members required to continue it.");
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
        eim.dropMessage(5, "[Expedition] Either the leader has quit the expedition or there is no longer the minimum number of members required to continue it.");
        end(eim);
    } else {
        eim.dropMessage(5, "[Expedition] " + player.getName() + " has left the instance.");
        eim.unregisterPlayer(player);
    }
}

function playerDisconnected(eim, player) {
    if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        eim.dropMessage(5, "[Expedition] Either the leader has quit the expedition or there is no longer the minimum number of members required to continue it.");
        end(eim);
    } else {
        eim.dropMessage(5, "[Expedition] " + player.getName() + " has left the instance.");
        eim.unregisterPlayer(player);
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
}

function isHorntailHead(mob) {
    var mobid = mob.getId();
    return (mobid == 8810000 || mobid == 8810001);
}

function isHorntail(mob) {
    var mobid = mob.getId();
    return (mobid == 8810018);
}

function monsterKilled(mob, eim) {
    if (isHorntail(mob)) {
        eim.setIntProperty("defeatedBoss", 1);
        eim.showClearEffect(mob.getMap().getId());
        eim.increaseClearCount();
        eim.dispatchRaiseQuestMobCount(8810018, 240060200);
        mob.getMap().broadcastHorntailVictory();
    } else if (isHorntailHead(mob)) {
        var killed = eim.getIntProperty("defeatedHead");
        eim.setIntProperty("defeatedHead", killed + 1);
        eim.showClearEffect(mob.getMap().getId());
    }
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function changedLeader(eim, leader) {}
function playerDead(eim, player) {}
function leftParty(eim, player) {}
function disbandParty(eim) {}
function monsterValue(eim, mobId) { return 1; }
function playerUnregistered(eim, player) {}
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}