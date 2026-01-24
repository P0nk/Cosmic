/*
    Orbis PQ (Refactored)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 51, maxLevel = 255;
var entryMap = 920010000;
var exitMap = 920011200;
var recruitMap = 200080101;
var clearMap = 920011300;

var minMapId = 920010000;
var maxMapId = 920011300;
var eventTime = 45;

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
    var itemSet = [4001044, 4001045, 4001046, 4001047, 4001048, 4001049, 4001050, 4001051, 4001052, 4001053, 4001054, 4001055, 4001056, 4001057, 4001058, 4001059, 4001060, 4001061, 4001062, 4001063];
    eim.setExclusiveItems(itemSet);
}

function setEventRewards(eim) {
    eim.setEventRewards(1,
        [2040602, 2040802, 2040002, 2040402, 2040505, 2040502, 2040601, 2044501, 2044701, 2044601, 2041019, 2041016, 2041022, 2041013, 2041007, 2043301, 2040301, 2040801, 2040001, 2040004, 2040504, 2040501, 2040513, 2043101, 2044201, 2044401, 2040701, 2044301, 2043801, 2040401, 2043701, 2040803, 2000003, 2000002, 2000004, 2000006, 2000005, 2022000, 2001001, 2001002, 2022003, 2001000, 2020014, 2020015, 4003000, 1102015, 1102016, 1102017, 1102018, 1102021, 1102022, 1102023, 1102024, 1102084, 1102085, 1102086, 1032019, 1032020, 1032021, 1032014, 2070011, 4010003, 4010000, 4010006, 4010002, 4010005, 4010004, 4010001, 4020001, 4020002, 4020008, 4020007, 4020003, 4020000, 4020004, 4020005, 4020006, 2210000, 2210001, 2210002, 2070006, 2070005, 2070007, 2070004, 2061003, 2060003, 2060004, 2061004, 2100000, 2100001, 2100002, 2100003, 2100004, 2100005],
        [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 100, 100, 15, 80, 5, 25, 20, 20, 25, 20, 15, 10, 45, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 15, 15, 10, 15, 15, 15, 15, 15, 15, 10, 10, 15, 15, 15, 15, 15, 5, 5, 5, 1, 1, 1, 1, 2000, 2000, 2000, 2000, 1, 1, 1, 1, 1, 1]
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
    var eim = em.newInstance("Orbis" + lobbyid);
    eim.setProperty("level", level);

    for (var i = 0; i <= 8; i++) eim.setProperty("statusStg" + i, -1);
    eim.setProperty("statusStg2_c", 0);
    eim.setProperty("statusStg7_c", 0);
    eim.setProperty("statusStgBonus", 0);

    var maps = [920010000, 920010100, 920010200, 920010300, 920010400, 920010500, 920010600,
                920010601, 920010602, 920010603, 920010604, 920010700, 920010800, 920010900,
                920010910, 920010911, 920010912, 920010920, 920010921, 920010922, 920010930,
                920010931, 920010932, 920011000, 920011100, 920011200, 920011300];
    for (var m of maps) eim.getInstanceMap(m).resetPQ(level);

    respawnStages(eim);

    var d = new Date();
    eim.getInstanceMap(920010400).getReactorByName("music").setEventState(d.getDay());

    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function isTeamAllJobs(eim) {
    var eventJobs = eim.getEventPlayersJobs();
    var rangeJobs = parseInt('111110', 2);
    return ((eventJobs & rangeJobs) == rangeJobs);
}

function afterSetup(eim) {
    if (isTeamAllJobs(eim)) {
        var rnd = Math.floor(Math.random() * 4);
        eim.applyEventPlayersItemBuff(2022090 + rnd);
    }
}

function playerEntry(eim, player) {
    var map = eim.getMapInstance(entryMap);
    player.changeMap(map, map.getPortal(0));
    var text = "Hi, my name is Eak, the Chamberlain of the Goddess...";
    player.getAbstractPlayerInteraction().npcTalk(2013001, text);
}

function scheduledTimeout(eim) {
    if (eim.getIntProperty("statusStg8") == 1) {
        eim.warpEventTeam(920011300);
    } else {
        end(eim);
    }
}

function playerUnregistered(eim, player) {
    player.cancelEffect(2022090);
    player.cancelEffect(2022091);
    player.cancelEffect(2022092);
    player.cancelEffect(2022093);
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

// ---------- FILLER FUNCTIONS ----------
function respawnStages(eim) {}
function playerDead(eim, player) {}
function monsterKilled(mob, eim) {}
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}