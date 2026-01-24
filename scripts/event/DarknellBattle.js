/*
    Vs Darknell Battle (Refactored & Fixed)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 225, maxLevel = 256;
var entryMap = 450012210;
var exitMap = 450012200;
var recruitMap = 450012200;
var clearMap = 450012200;

var minMapId = 450012210;
var maxMapId = 450012210;

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
    var eim = em.newInstance("Darknell" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");

    eim.getInstanceMap(450012210).resetPQ(level);

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
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

function end(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        playerExit(eim, party.get(i));
    }
    eim.dispose();
}

function clearPQ(eim) {
    eim.stopEventTimer();
    eim.setEventCleared(ExpeditionType.DARKNELL);
}

function isDarknell(mob) {
    return mob.getId() == 8645009;
}

function monsterKilled(mob, eim) {
    if (isDarknell(mob)) {
        eim.showClearEffect();
        eim.clearPQ();
        var party = eim.getPlayers();
        for (var i = 0; i < party.size(); i++) {
            eim.getPlayers().get(i).getClient().getAbstractPlayerInteraction().gainItem(4001126, 30);
        }
    }
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function respawnStages(eim) {}
function playerUnregistered(eim, player) {}
function playerLeft(eim, player) { if (!eim.isEventCleared()) playerExit(eim, player); }
function changedMap(eim, player, mapid) { if (mapid < minMapId || mapid > maxMapId) { if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) { eim.unregisterPlayer(player); end(eim); } else { eim.unregisterPlayer(player); } } }
function changedLeader(eim, leader) {}
function playerDead(eim, player) {}
function playerRevive(eim, player) { if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) { eim.unregisterPlayer(player); end(eim); } else { eim.unregisterPlayer(player); } }
function playerDisconnected(eim, player) { if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) { eim.unregisterPlayer(player); end(eim); } else { eim.unregisterPlayer(player); } }
function leftParty(eim, player) {}
function disbandParty(eim) {}
function monsterValue(eim, mobId) { return 1; }
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}