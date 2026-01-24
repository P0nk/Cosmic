/*
    Vs Von Leon Expedition Script (Refactored & Fixed)
*/

// ExpeditionType is injected globally. Do NOT redeclare it.

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 1, maxLevel = 255;
var entryMap = 211070100;
var exitMap = 211070000;
var recruitMap = 211070000;
var clearMap = 211070000;

var minMapId = 211070100;
var maxMapId = 211070101;
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
    var eim = em.newInstance("VonLeon" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");
    eim.getInstanceMap(entryMap).resetPQ(level);

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function afterSetup(eim) {
    var mapObj = eim.getInstanceMap(entryMap);
    mapObj.destroyNPC(2161008);
    eim.schedule("spawnVonLeon", 4000);
}

function spawnVonLeon(eim) {
    var mapObj = eim.getMapInstance(entryMap);
    var boss = LifeFactory.getMonster(8840000);
    eim.registerMonster(boss);
    mapObj.spawnMonsterOnGroundBelow(boss, new java.awt.Point(49, -181));
}

function playerEntry(eim, player) {
    player.changeMap(eim.getMapInstance(entryMap), eim.getMapInstance(entryMap).getPortal(0));
}

function scheduledTimeout(eim) {
    end(eim);
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, 0);
}

function changedMap(eim, player, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        eim.unregisterPlayer(player);
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

function isVonleon(mob) { return mob.getId() == 8840000; }

function monsterKilled(mob, eim) {
    if (isVonleon(mob)) {
        eim.showClearEffect();
        clearPQ(eim);

        var party = eim.getPlayers();
        for (var i = 0; i < party.size(); i++) {
            party.get(i).getClient().getAbstractPlayerInteraction().gainItem(4001126, 10);
        }

        var mapObj = eim.getMapInstance(entryMap);
        eim.spawnNpc(2161008, new java.awt.Point(49, -181), mapObj);
    }
}

// ---------- FILLER FUNCTIONS ----------
function respawnStages(eim) {}
function playerUnregistered(eim, player) {}
function playerLeft(eim, player) { if (!eim.isEventCleared()) playerExit(eim, player); }
function changedLeader(eim, leader) {}
function playerDead(eim, player) {}
function playerRevive(eim, player) { eim.unregisterPlayer(player); }
function playerDisconnected(eim, player) { eim.unregisterPlayer(player); }
function leftParty(eim, player) {}
function disbandParty(eim) {}
function monsterValue(eim, mobId) { return 1; }
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}