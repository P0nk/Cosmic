/*
    Cafe PQ 1 (Refactored)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 21, maxLevel = 255;
var entryMap = 190000000;
var exitMap = 193000000;
var recruitMap = 193000000;

var minMapId = 190000000;
var maxMapId = 190000002;
var eventMaps = [190000000, 190000001, 190000002];

var eventTime = 45;
var couponsNeeded = 400;
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
    eim.setExclusiveItems([4001007]);
}

function setEventRewards(eim) {
    eim.setEventRewards(1, [4001014], [1]);
    eim.setEventClearStageExp([20000]);
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
    var eim = em.newInstance("Lan1_" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("stage", "0");
    eim.setIntProperty("couponsNeeded", couponsNeeded);

    for (var i = 0; i < eventMaps.length; i++) {
        var mapObj = eim.getInstanceMap(eventMaps[i]);
        mapObj.resetPQ(level);
        mapObj.toggleDrops();
        mapObj.instanceMapForceRespawn();
    }

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function respawnStages(eim) {
    for (var i = 0; i < eventMaps.length; i++) {
        eim.getInstanceMap(eventMaps[i]).instanceMapRespawn();
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
    eim.giveEventPlayersStageReward(1);

    for (var i = 0; i < eventMaps.length; i++) {
        eim.getInstanceMap(eventMaps[i]).killAllMonstersNotFriendly();
        eim.showClearEffect(eventMaps[i]);
    }
}

function getDroppedQuantity(mob) {
    if (mob.getLevel() > 65) return 5;
    if (mob.getLevel() > 40) return 2;
    return 1;
}

function monsterKilled(mob, eim) {
    try {
        if (eim.isEventCleared()) return;

        var mapObj = mob.getMap();
        var itemObj = new client.inventory.Item(4001007, 0, getDroppedQuantity(mob)); // Using full path if Item isn't injected, or if injected just 'new Item'
        // If Item is not injected globally, use: var itemObj = new Packages.client.inventory.Item(...)
        // But since you removed Item import, I assume you want it clean.
        // If 'Item' is not in your EventGlobals, you might need to add it or use fully qualified name.
        // Assuming 'Item' is NOT injected:
        // var itemObj = new Packages.client.inventory.Item(...)

        var dropper = eim.getPlayers().get(0);
        mapObj.spawnItemDrop(mob, dropper, itemObj, mob.getPosition(), true, false);
    } catch (err) {}
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function playerUnregistered(eim, player) {}
function playerLeft(eim, player) { if (!eim.isEventCleared()) playerExit(eim, player); }
function changedMap(eim, player, mapid) { if (mapid < minMapId || mapid > maxMapId) { if (eim.isEventTeamLackingNow(true, minPlayers, player)) { eim.unregisterPlayer(player); end(eim); } else { eim.unregisterPlayer(player); } } }
function changedLeader(eim, leader) { if (!eim.isEventCleared() && (leader.getMapId() < minMapId || leader.getMapId() > maxMapId)) end(eim); }
function playerDead(eim, player) {}
function playerRevive(eim, player) { if (eim.isEventTeamLackingNow(true, minPlayers, player)) { eim.unregisterPlayer(player); end(eim); } else { eim.unregisterPlayer(player); } }
function playerDisconnected(eim, player) { if (eim.isEventTeamLackingNow(true, minPlayers, player)) { eim.unregisterPlayer(player); end(eim); } else { eim.unregisterPlayer(player); } }
function leftParty(eim, player) { if (eim.isEventTeamLackingNow(false, minPlayers, player)) { end(eim); } else { playerLeft(eim, player); } }
function disbandParty(eim) { if (!eim.isEventCleared()) end(eim); }
function monsterValue(eim, mobId) { return 1; }
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}