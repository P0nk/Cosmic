/*
    Scarga Battle (Refactored)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 30;
var minLevel = 100, maxLevel = 255;
var entryMap = 551030200;
var exitMap = 551030100;
var recruitMap = 551030100;
var clearMap = 551030100;

var minMapId = 551030200;
var maxMapId = 551030200;
var eventTime = 60;
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
    eim.setEventRewards(1,
        [1102145, 1102084, 1102085, 1102086, 1102087, 1052165, 1052166, 1052167, 1402013, 1332030, 1032030, 1032070, 4003000, 4000030, 4006000, 4006001, 4005000, 4005001, 4005002, 4005003, 4005004, 2022016, 2022263, 2022264, 2022015, 2022306, 2022307, 2022306, 2022113],
        [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 50, 50, 120, 120, 4, 4, 4, 4, 2, 125, 125, 125, 30, 30, 30, 30, 30]
    );
    eim.setEventClearStageExp([]);
    eim.setEventClearStageMeso([]);
}

function setup(channel) {
    var eim = em.newInstance("Scarga" + channel);
    eim.setProperty("canJoin", 1);
    eim.setProperty("defeatedBoss", 0);

    eim.getInstanceMap(551030200).resetPQ(1);

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

function isScarga(mob) {
    var mobid = mob.getId();
    return (mobid == 9420544) || (mobid == 9420549);
}

function monsterKilled(mob, eim) {
    if (isScarga(mob)) {
        var killed = eim.getIntProperty("defeatedBoss");
        if (killed == 1) {
            eim.showClearEffect();
            eim.clearPQ();
        }
        eim.setIntProperty("defeatedBoss", killed + 1);
    }
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function getEligibleParty(party) {return Java.to([], Java.type('net.server.world.PartyCharacter[]'));} // Not used for boss runs usually
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