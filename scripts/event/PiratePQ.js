/*
    Pirate PQ (Refactored)
*/

var isPq = true;
var isGrindMode = false;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 55, maxLevel = 255;
var entryMap = 925100000;
var exitMap = 925100700;
var recruitMap = 251010404;
var clearMap = 925100600;

var minMapId = 925100000;
var maxMapId = 925100500;
var eventTime = 4;
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
    eim.setExclusiveItems([4001117, 4001120, 4001121, 4001122]);
}

function setEventRewards(eim) {}

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
    var eim = em.newInstance("Pirate" + lobbyid);
    eim.setProperty("level", level);

    var props = ["stage2", "stage2a", "stage3a", "stage2b", "stage3b", "stage4", "stage5", "openedChests", "openedBoxes"];
    for (var p of props) eim.setProperty(p, "0");
    eim.setProperty("curStage", "1");
    eim.setProperty("grindMode", isGrindMode ? "1" : "0");

    eim.getInstanceMap(925100000).resetPQ(level);
    eim.getInstanceMap(925100000).shuffleReactors();
    eim.getInstanceMap(925100100).resetPQ(level);

    // Stage 200 Setup
    var map = eim.getInstanceMap(925100200);
    map.resetPQ(level);
    map.shuffleReactors();
    for (var i = 0; i < 5; i++) {
        var mobs = [9300124, 9300125, 9300124, 9300125];
        var points = [new java.awt.Point(430, 75), new java.awt.Point(1600, 75), new java.awt.Point(430, 238), new java.awt.Point(1600, 238)];
        for (var j=0; j<4; j++) {
            var mob = em.getMonster(mobs[j]);
            eim.registerMonster(mob);
            mob.changeDifficulty(level, isPq);
            map.spawnMonsterOnGroundBelow(mob, points[j]);
        }
    }

    // Stage 201 Setup
    map = eim.getInstanceMap(925100201);
    map.resetPQ(level);
    for (var i = 0; i < 10; i++) {
        var mob = em.getMonster(9300112);
        var mob2 = em.getMonster(9300113);
        eim.registerMonster(mob);
        eim.registerMonster(mob2);
        mob.changeDifficulty(level, isPq);
        mob2.changeDifficulty(level, isPq);
        map.spawnMonsterOnGroundBelow(mob, new java.awt.Point(0, 238));
        map.spawnMonsterOnGroundBelow(mob2, new java.awt.Point(1700, 238));
    }

    eim.getInstanceMap(925100202).resetPQ(level);

    // Stage 300 Setup
    map = eim.getInstanceMap(925100300);
    map.resetPQ(level);
    map.shuffleReactors();
    for (var i = 0; i < 5; i++) {
        var mobs = [9300124, 9300125, 9300124, 9300125];
        var points = [new java.awt.Point(430, 75), new java.awt.Point(1600, 75), new java.awt.Point(430, 238), new java.awt.Point(1600, 238)];
        for (var j=0; j<4; j++) {
            var mob = em.getMonster(mobs[j]);
            eim.registerMonster(mob);
            mob.changeDifficulty(level, isPq);
            map.spawnMonsterOnGroundBelow(mob, points[j]);
        }
    }

    // Stage 301 Setup
    map = eim.getInstanceMap(925100301);
    map.resetPQ(level);
    for (var i = 0; i < 10; i++) {
        var mob = em.getMonster(9300112);
        var mob2 = em.getMonster(9300113);
        eim.registerMonster(mob);
        eim.registerMonster(mob2);
        mob.changeDifficulty(level, isPq);
        mob2.changeDifficulty(level, isPq);
        map.spawnMonsterOnGroundBelow(mob, new java.awt.Point(0, 238));
        map.spawnMonsterOnGroundBelow(mob2, new java.awt.Point(1700, 238));
    }

    eim.getInstanceMap(925100302).resetPQ(level);
    eim.getInstanceMap(925100400).resetPQ(level);
    eim.getInstanceMap(925100500).resetPQ(level);

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function respawnStages(eim) {
    var stg = eim.getIntProperty("stage2");
    if (stg < 3) {
        eim.getMapInstance(925100100).spawnAllMonsterIdFromMapSpawnList(9300114 + stg, eim.getIntProperty("level"), true);
    }
    eim.getMapInstance(925100400).instanceMapRespawn();
    eim.schedule("respawnStages", 10 * 1000);
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

function changedMapInside(eim, mapid) {
    var stage = eim.getIntProperty("curStage");
    if (stage == 1 && mapid == 925100100) { eim.restartEventTimer(6 * 60 * 1000); eim.setIntProperty("curStage", 2); }
    else if (stage == 2 && mapid == 925100200) { eim.restartEventTimer(6 * 60 * 1000); eim.setIntProperty("curStage", 3); }
    else if (stage == 3 && mapid == 925100300) { eim.restartEventTimer(6 * 60 * 1000); eim.setIntProperty("curStage", 4); }
    else if (stage == 4 && mapid == 925100400) { eim.restartEventTimer(6 * 60 * 1000); eim.setIntProperty("curStage", 5); }
    else if (stage == 5 && mapid == 925100500) { eim.restartEventTimer(8 * 60 * 1000); eim.setIntProperty("curStage", 6); }
}

function changedMap(eim, player, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
            eim.unregisterPlayer(player);
            end(eim);
        } else {
            eim.unregisterPlayer(player);
        }
    } else {
        changedMapInside(eim, mapid);
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
    eim.giveEventPlayersExpTier([55,70,80,95,120], -1);
    eim.giveEventPlayersCash(2000);
    eim.warpEventTeam(925100600);
}

function monsterKilled(mob, eim) {
    var map = mob.getMap();

    if (isLordPirate(mob)) {
        map.broadcastStringMessage(5, "As Lord Pirate dies, Wu Yang is released!");
        eim.spawnNpc(2094001, new java.awt.Point(777, 140), mob.getMap());
    }

    if (map.countMonsters() == 0) {
        var stage = ((map.getId() % 1000) / 100) + 1;
        if ((stage == 1 || stage == 3 || stage == 4) && passedGrindMode(map, eim)) {
            eim.showClearEffect(map.getId());
        } else if (stage == 5) {
            if (map.getReactorByName("sMob1").getState() >= 1 && map.getReactorByName("sMob2").getState() >= 1 && map.getReactorByName("sMob3").getState() >= 1 && map.getReactorByName("sMob4").getState() >= 1) {
                eim.showClearEffect(map.getId());
            }
        }
    }
}

function isLordPirate(mob) {
    var mobid = mob.getId();
    return (mobid == 9300105) || (mobid == 9300106) || (mobid == 9300107) || (mobid == 9300119);
}

function passedGrindMode(map, eim) {
    if (eim.getIntProperty("grindMode") == 0) return true;
    return eim.activatedAllReactorsOnMap(map, 2511000, 2517999);
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function playerUnregistered(eim, player) {}
function playerDead(eim, player) {}
function monsterValue(eim, mobId) { return 1; }
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}