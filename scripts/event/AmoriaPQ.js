/*
    Amoria PQ (Refactored)
    @author: Ronan
*/

var isPq = true;
var onlyMarriedPlayers = true;
var minPlayers = 4, maxPlayers = 6;
var minLevel = 40, maxLevel = 255;
var entryMap = 670010200;
var exitMap = 670011000;
var recruitMap = 670010100;
var clearMap = 670010800;

var minMapId = 670010200;
var maxMapId = 670010800;

var eventTime = 75; // 75 minutes
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
    reqStr += "\r\n    At least 1 of both genders";
    if (onlyMarriedPlayers) reqStr += "\r\n    All married";
    reqStr += "\r\n    Time limit: " + eventTime + " minutes";

    em.setProperty("party", reqStr);
}

function setEventExclusives(eim) {
    var itemSet = [4031594, 4031595, 4031596, 4031597];
    eim.setExclusiveItems(itemSet);
}

function setEventRewards(eim) {
    eim.setEventRewards(1, [], []); // Level 1 rewards
    eim.setEventClearStageExp([2000, 4000, 6000, 8000, 9000, 11000]);
}

function getEligibleParty(party) {
    var eligible = [];
    var hasLeader = false, hasNotMarried = false;
    var mask = 0;

    if (party.size() > 0) {
        var partyList = party.toArray();
        for (var i = 0; i < party.size(); i++) {
            var ch = partyList[i];
            if (ch.getMapId() == recruitMap && ch.getLevel() >= minLevel && ch.getLevel() <= maxLevel) {
                if (ch.isLeader()) hasLeader = true;
                if (!ch.getPlayer().isMarried()) hasNotMarried = true;
                eligible.push(ch);
                mask |= (1 << ch.getPlayer().getGender());
            }
        }
    }

    if (!(hasLeader && eligible.length >= minPlayers && eligible.length <= maxPlayers && mask == 3)) {
        eligible = [];
    }
    if (onlyMarriedPlayers && hasNotMarried) {
        eligible = [];
    }
    // Return specific Java array type required by EventManager
    return Java.to(eligible, Java.type('net.server.world.PartyCharacter[]'));
}

function setup(level, lobbyid) {
    var eim = em.newInstance("Amoria" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("marriedGroup", 0);
    eim.setProperty("missCount", 0);
    eim.setProperty("statusStg1", -1);
    eim.setProperty("statusStg2", -1);
    eim.setProperty("statusStg3", -1);
    eim.setProperty("statusStg4", -1);
    eim.setProperty("statusStg5", -1);
    eim.setProperty("statusStg6", -1);
    eim.setProperty("statusStgBonus", 0);

    // Reset all maps
    var maps = [670010200, 670010300, 670010301, 670010302, 670010400, 670010500, 670010600, 670010700, 670010750, 670010800];
    for (var i = 0; i < maps.length; i++) {
        eim.getInstanceMap(maps[i]).resetPQ(level);
    }

    eim.getInstanceMap(670010200).toggleDrops();
    eim.getInstanceMap(670010300).toggleDrops();
    eim.getInstanceMap(670010301).toggleDrops();
    eim.getInstanceMap(670010302).toggleDrops();

    eim.getInstanceMap(670010200).instanceMapForceRespawn();
    eim.getInstanceMap(670010500).instanceMapForceRespawn();

    eim.getInstanceMap(670010750).shuffleReactors();
    eim.getInstanceMap(670010800).shuffleReactors();

    // Spawn Boss (Using Global LifeFactory)
    var mapObj = eim.getInstanceMap(670010700);
    var mobObj = LifeFactory.getMonster(9400536);
    mapObj.spawnMonsterOnGroundBelow(mobObj, new java.awt.Point(942, 478));

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);

    return eim;
}

function isTeamAllCouple(eim) {
    var eventPlayers = eim.getPlayers();
    for (var iterator = eventPlayers.iterator(); iterator.hasNext();) {
        var chr = iterator.next();
        var pid = chr.getPartnerId();
        if (pid <= 0 || eim.getPlayerById(pid) == null) {
            return false;
        }
    }
    return true;
}

function afterSetup(eim) {
    if (isTeamAllCouple(eim)) {
        eim.setIntProperty("marriedGroup", 1);
    }
}

function playerEntry(eim, player) {
    var map = eim.getMapInstance(entryMap);
    player.changeMap(map, map.getPortal(0));
}

function scheduledTimeout(eim) {
    if (eim.getIntProperty("statusStg6") == 1) {
        eim.warpEventTeam(exitMap);
    } else {
        end(eim);
    }
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

function changedLeader(eim, leader) {
    var mapid = leader.getMapId();
    if (!eim.isEventCleared() && (mapid < minMapId || mapid > maxMapId)) {
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

function leftParty(eim, player) {
    if (eim.isEventTeamLackingNow(false, minPlayers, player)) {
        end(eim);
    } else {
        playerLeft(eim, player);
    }
}

function disbandParty(eim) {
    if (!eim.isEventCleared()) {
        end(eim);
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

function dispose(eim) {}

// ---------- FILLER FUNCTIONS ----------
function respawnStages(eim) {}
function playerUnregistered(eim, player) {}
function playerDead(eim, player) {}
function monsterKilled(mob, eim) {}
function allMonstersDead(eim) {}
function cancelSchedule() {}