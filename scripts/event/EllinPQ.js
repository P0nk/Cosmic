/*
    Ellin PQ (Refactored)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 44, maxLevel = 255;
var entryMap = 930000000;
var exitMap = 930000800;
var recruitMap = 300030100;
var clearMap = 930000800;

var minMapId = 930000000;
var maxMapId = 930000800;

var eventTime = 30; // 30 minutes
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
    reqStr += "\r\n    For #radventurers only#k.";
    reqStr += "\r\n    Time limit: " + eventTime + " minutes";
    em.setProperty("party", reqStr);
}

function setEventExclusives(eim) {
    eim.setExclusiveItems([4001162, 4001163, 4001169, 2270004]);
}

function setEventRewards(eim) {}

function getEligibleParty(party) {
    var eligible = [];
    var hasLeader = false;

    if (party.size() > 0) {
        var partyList = party.toArray();
        for (var i = 0; i < party.size(); i++) {
            var ch = partyList[i];
            if (ch.getMapId() == recruitMap && ch.getLevel() >= minLevel && ch.getLevel() <= maxLevel && Math.floor(ch.getJob().getId() / 1000) == 0) {
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
    var eim = em.newInstance("Ellin" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("statusStg4", 0);

    eim.getInstanceMap(930000000).resetPQ(level);
    eim.getInstanceMap(930000100).resetPQ(level);
    eim.getInstanceMap(930000200).resetPQ(level);
    eim.getInstanceMap(930000300).resetPQ(level);
    eim.getInstanceMap(930000400).resetPQ(level);
    var map = eim.getInstanceMap(930000500);
    map.resetPQ(level);
    map.shuffleReactors();
    eim.getInstanceMap(930000600).resetPQ(level);
    eim.getInstanceMap(930000700).resetPQ(level);

    respawnStg2(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function respawnStg2(eim) {
    if (!eim.getMapInstance(930000200).getPlayers().isEmpty()) {
        eim.getMapInstance(930000200).instanceMapRespawn();
    }
    eim.schedule("respawnStg2", 4 * 1000);
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
        end(eim);
    } else {
        playerExit(eim, player);
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

function isPoisonGolem(mob) {
    return (mob.getId() == 9300182);
}

function monsterKilled(mob, eim, hasKiller) {
    var map = mob.getMap();

    if (isPoisonGolem(mob)) {
        eim.showClearEffect(map.getId());
        eim.clearPQ();
    } else if (map.countMonsters() == 0) {
        var stage = ((map.getId() % 1000) / 100);
        if (stage == 1 || (stage == 4 && !hasKiller)) {
            eim.showClearEffect(map.getId());
        }
    }
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function playerUnregistered(eim, player) {}
function playerDead(eim, player) {}
function monsterValue(eim, mobId) { return 1; }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}