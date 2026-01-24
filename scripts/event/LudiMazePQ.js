/*
    Ludibrium Maze PQ (Refactored)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 51, maxLevel = 255;
var entryMap = 809050000;
var exitMap = 809050017;
var recruitMap = 220000000;
var clearMap = 809050016;

var minMapId = 809050000;
var maxMapId = 809050016;

var eventTime = 15; // 15 minutes
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
    eim.setExclusiveItems([4001106]);
}

function setEventRewards(eim) {
    eim.setEventRewards(1,
        [1442017, 1322025, 1032013, 1302016, 1072263, 1032043, 2000005, 2000004, 2001001, 2001002, 2020008, 2020010, 2030008, 2030010, 2030009, 2022000, 2001000, 2022019, 2020007, 2020006, 2020009, 2000006, 2040601, 2040605, 2040602, 2041027, 2041028, 2041004, 2041029, 2041017, 2041020, 2040008, 2040001, 2040009, 2040002, 2040504, 2040511, 2040505, 2040501, 2040904, 2040901, 2040905, 2040902, 2040404, 2040401, 2040405, 2040402],
        [1, 1, 1, 1, 1, 1, 5, 5, 5, 20, 20, 20, 20, 20, 50, 50, 50, 100, 100, 100, 100, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
    );
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
    var eim = em.newInstance("LudiMaze" + lobbyid);
    eim.setProperty("level", level);

    for (var i = 809050000; i <= 809050016; i++) {
        eim.getInstanceMap(i).resetPQ(level);
        eim.getInstanceMap(i).shuffleReactors();
    }

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function playerEntry(eim, player) {
    var rand = Math.floor(Math.random() * 15);
    var map = eim.getMapInstance(entryMap + rand);
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
    eim.warpEventTeam(809050016);
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function respawnStages(eim) {}
function playerUnregistered(eim, player) {}
function playerDead(eim, player) {}
function monsterValue(eim, mobId) { return 1; }
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function monsterKilled(mob, eim) {}
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}