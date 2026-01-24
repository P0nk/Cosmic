/*
    Treasure PQ (Refactored)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 140, maxLevel = 255;
var entryMap = 674030000;
var exitMap = 674030100;
var recruitMap = 674030100;
var clearMap = 674030300;

var minMapId = 674030000;
var maxMapId = 674030300;

var eventTime = 45;
var bonusTime = 10;
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
    eim.setExclusiveItems([4032118]);
}

function setEventRewards(eim) {
    eim.setEventRewards(1, [], []);
    eim.setEventClearStageExp([60000, 100000]);
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
    var eim = em.newInstance("Treasure" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("statusStg1", "0");
    eim.getInstanceMap(674030000).shuffleReactors();

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function respawnStages(eim) {
    eim.getMapInstance(674030000).instanceMapRespawn();
    eim.schedule("respawnStages", 15 * 1000);
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

function changedMap(eim, player, mapid) {
    if (mapid < minMapId || mapid > maxMapId || mapid == 674030100) {
        if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
            eim.unregisterPlayer(player);
            end(eim);
        } else {
            eim.unregisterPlayer(player);
        }
    }
}

function changedLeader(eim, leader) {
    var mapid = leader.getMapId();
    if (!eim.isEventCleared() && (mapid < minMapId || mapid > maxMapId || mapid == 674030100)) {
        end(eim);
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

function warpBonus(eim) {
    eim.startEventTimer(bonusTime * 60000);
    eim.warpEventTeam(674030300);
}

function clearPQ(eim) {
    eim.stopEventTimer();
    eim.setEventCleared();
    eim.schedule("warpBonus", 10 * 1000);
}

function isMV(mob) {
    return mob.getId() == 9400589;
}

function monsterKilled(mob, eim) {
    if (isMV(mob)) {
        eim.showClearEffect();
        eim.giveEventPlayersStageReward(2);
        eim.clearPQ();
    }
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function playerUnregistered(eim, player) {}
function playerLeft(eim, player) { if (!eim.isEventCleared()) playerExit(eim, player); }
function playerDead(eim, player) {}
function leftParty(eim, player) { if (eim.isEventTeamLackingNow(false, minPlayers, player)) end(eim); else playerLeft(eim, player); }
function disbandParty(eim) { if (!eim.isEventCleared()) end(eim); }
function monsterValue(eim, mobId) { return 1; }
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}