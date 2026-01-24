/*
    Magatia PQ (Zenumist) (Refactored)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 4;
var minLevel = 71, maxLevel = 255;
var entryMap = 926100000;
var exitMap = 926100700;
var recruitMap = 261000011;
var clearMap = 926100700;

var minMapId = 926100000;
var maxMapId = 926100600;

var eventTime = 45; // 45 minutes
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
    eim.setExclusiveItems([4001130, 4001131, 4001132, 4001133, 4001134, 4001135]);
}

function setEventRewards(eim) {
    eim.setEventRewards(1,
        [2000003, 2000002, 2000004, 2000005, 2022003, 1032016, 1032015, 1032014, 2041212, 2041020, 2040502, 2041016, 2044701, 2040301, 2043201, 2040501, 2040704, 2044001, 2043701, 2040803, 1102026, 1102028, 1102029],
        [100, 100, 20, 10, 50, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
    );
    eim.setEventClearStageExp([0, 10000, 20000, 0, 20000, 20000, 0, 0]);
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
    var eim = em.newInstance("MagatiaZ" + lobbyid);
    eim.setProperty("level", level);
    eim.setIntProperty("isAlcadno", 0);

    var props = ["escortFail", "yuleteTimeout", "yuleteTalked", "yuletePassed", "npcShocked", "normalClear",
                 "statusStg1", "statusStg2", "statusStg3", "statusStg4", "statusStg5", "statusStg6", "statusStg7"];
    for (var p of props) eim.setIntProperty(p, 0);

    var maps = [926100000, 926100001, 926100100, 926100200, 926100201, 926100202, 926100203,
                926100300, 926100301, 926100302, 926100303, 926100304, 926100400, 926100401,
                926100500, 926100600, 926100700];
    for (var m of maps) eim.getInstanceMap(m).resetPQ(level);

    eim.getInstanceMap(926100201).shuffleReactors(2518000, 2612004);
    eim.getInstanceMap(926100202).shuffleReactors(2518000, 2612004);

    eim.spawnNpc(2112000, new java.awt.Point(252, 243), eim.getInstanceMap(926100203));
    eim.spawnNpc(2112000, new java.awt.Point(200, 100), eim.getInstanceMap(926100401));
    eim.spawnNpc(2112001, new java.awt.Point(200, 100), eim.getInstanceMap(926100500));
    eim.spawnNpc(2112018, new java.awt.Point(200, 100), eim.getInstanceMap(926100600));

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function shuffle(array) {
    var currentIndex = array.length, temporaryValue, randomIndex;
    while (0 !== currentIndex) {
        randomIndex = Math.floor(Math.random() * currentIndex);
        currentIndex -= 1;
        temporaryValue = array[currentIndex];
        array[currentIndex] = array[randomIndex];
        array[randomIndex] = temporaryValue;
    }
    return array;
}

function generateStg6Combo(eim) {
    var matrix = [];
    for (var i = 0; i < 4; i++) matrix.push([]);
    for (var j = 0; j < 10; j++) {
        var array = [0, 1, 2, 3];
        array = shuffle(array);
        for (var i = 0; i < 4; i++) matrix[i].push(array[i]);
    }
    for (var i = 0; i < 4; i++) {
        var comb = "";
        for (var j = 0; j < 10; j++) comb += matrix[i][j].toString();
        eim.setProperty("stage6_comb" + (i + 1), comb);
    }
}

function afterSetup(eim) {
    eim.setIntProperty("escortFail", 0);
    var books = [-1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 2, 3];
    shuffle(books);
    for (var i = 0; i < books.length; i++) eim.setIntProperty("stg1_b" + i, books[i]);
}

function respawnStages(eim) {
    eim.getMapInstance(926100100).instanceMapRespawn();
    eim.getMapInstance(926100200).instanceMapRespawn();

    if (!eim.isEventCleared()) {
        var mapobj = eim.getMapInstance(926100401);
        var mobcount = mapobj.countMonster(9300150);
        var mobobj;

        if (mobcount == 0) {
            mobobj = LifeFactory.getMonster(9300150);
            mapobj.spawnMonsterOnGroundBelow(mobobj, new java.awt.Point(-278, -126));
            mobobj = LifeFactory.getMonster(9300150);
            mapobj.spawnMonsterOnGroundBelow(mobobj, new java.awt.Point(-542, -126));
        } else if (mobcount == 1) {
            mobobj = LifeFactory.getMonster(9300150);
            mapobj.spawnMonsterOnGroundBelow(mobobj, new java.awt.Point(-542, -126));
        }
    }
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
    } else if (mapid == 926100203 && eim.getIntProperty("yuleteTimeout") == 0) {
        eim.setIntProperty("yuleteTimeout", 1);
        eim.schedule("yuleteAction", 10 * 1000);
    }
}

function yuleteAction(eim) {
    if (eim.getIntProperty("yuleteTalked") == 1) {
        eim.setIntProperty("yuletePassed", 1);
        eim.dropMessage(5, "Yulete: Ugh, you guys disgust me. All I desired was to make this nation the greatest alchemy powerhouse of the entire world. If they won't accept this, I will make it true by myself, at any costs!!!");
    } else {
        eim.dropMessage(5, "Yulete: Hahaha... Did you really think I was going to be so disprepared knowing that the Magatia societies' dogs would be coming in my pursuit after my actions? Fools!");
    }
    eim.setIntProperty("yuleteTalked", -1);

    var mapobj = eim.getMapInstance(926100203);
    var mob1 = 9300143, mob2 = 9300144;
    mapobj.destroyNPC(2112000);

    var mobobj1, mobobj2;
    var xPoints = [-455, 0, 360];
    for (var x of xPoints) {
        for (var i = 0; i < 5; i++) {
            mobobj1 = LifeFactory.getMonster(mob1);
            mobobj2 = LifeFactory.getMonster(mob2);
            mapobj.spawnMonsterOnGroundBelow(mobobj1, new java.awt.Point(x, 135));
            mapobj.spawnMonsterOnGroundBelow(mobobj2, new java.awt.Point(x, 135));
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

function giveRandomEventReward(eim, player) {
    eim.giveEventReward(player);
}

function clearPQ(eim) {
    eim.stopEventTimer();
    eim.setEventCleared();
}

function monsterKilled(mob, eim) {
    var map = mob.getMap();

    if (map.getId() == 926100001 && eim.getIntProperty("statusStg1") == 1) {
        if (map.countMonsters() == 0) {
            eim.showClearEffect();
            eim.giveEventPlayersStageReward(2);
            eim.setIntProperty("statusStg2", 1);
        }
    } else if (map.getId() == 926100203 && eim.getIntProperty("statusStg1") == 1) {
        if (map.countMonsters() == 0) {
            eim.showClearEffect();
            eim.giveEventPlayersStageReward(5);
            generateStg6Combo(eim);
            map.getReactorByName("rnj6_out").forceHitReactor(1);
        }
    } else if (mob.getId() == 9300139 || mob.getId() == 9300140) {
        eim.showClearEffect();
        eim.giveEventPlayersStageReward(7);
        eim.spawnNpc(2112006, new java.awt.Point(-370, -150), map);

        var gain = (eim.getIntProperty("escortFail") == 1) ? 90000 : ((mob.getId() == 9300139) ? 105000 : 140000);
        eim.giveEventPlayersExp(gain);
        map.killAllMonstersNotFriendly();

        if (mob.getId() == 9300139) eim.setIntProperty("normalClear", 1);
        eim.clearPQ();
    }
}

function friendlyKilled(mob, eim) {
    eim.setIntProperty("escortFail", 1);
}

// ---------- FILLER FUNCTIONS ----------
function playerUnregistered(eim, player) {}
function playerDead(eim, player) {}
function monsterValue(eim, mobId) { return 1; }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}