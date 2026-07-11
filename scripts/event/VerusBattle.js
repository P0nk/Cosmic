const ExpeditionType = Java.type("server.expeditions.ExpeditionType");

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 200, maxLevel = 255;
var entryMap = 450010100;
var exitMap = 450011660;
var recruitMap = 450011660;
var clearMap = 450011660;

var minMapId = 450010100;
var maxMapId = 450010100;

var eventTime = 120;     // 45 minutes

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
    if (maxPlayers - minPlayers >= 1) {
        reqStr += minPlayers + " ~ " + maxPlayers;
    } else {
        reqStr += minPlayers;
    }

    reqStr += "\r\n    Level range: ";
    if (maxLevel - minLevel >= 1) {
        reqStr += minLevel + " ~ " + maxLevel;
    } else {
        reqStr += minLevel;
    }

    reqStr += "\r\n    Time limit: ";
    reqStr += eventTime + " minutes";

    em.setProperty("party", reqStr);
}

function setEventExclusives(eim) {
    var itemSet = [];
    eim.setExclusiveItems(itemSet);
}

function setEventRewards(eim) {
    var itemSet, itemQty, evLevel, expStages;

    evLevel = 1;    // Rewards at clear PQ
    itemSet = [];
    itemQty = [];
    eim.setEventRewards(evLevel, itemSet, itemQty);

    expStages = [];    // Bonus exp given on CLEAR stage signal
    eim.setEventClearStageExp(expStages);
}

function getEligibleParty(party) {      // Selects, from the given party, the team that is allowed to attempt this event
    var eligible = [];
    var hasLeader = false;

    if (party.size() > 0) {
        var partyList = party.toArray();

        for (var i = 0; i < party.size(); i++) {
            var ch = partyList[i];

            if (ch.getMapId() == recruitMap && ch.getLevel() >= minLevel && ch.getLevel() <= maxLevel) {
                if (ch.isLeader()) {
                    hasLeader = true;
                }
                eligible.push(ch);
            }
        }
    }

    if (!(hasLeader && eligible.length >= minPlayers && eligible.length <= maxPlayers)) {
        eligible = [];
    }
    return Java.to(eligible, Java.type('net.server.world.PartyCharacter[]'));
}

var spawnIntervalFirst = 4 * 60 * 1000; // 4 minutes in milliseconds
var spawnIntervalSecond = 8 * 60 * 1000; // 8 minutes in milliseconds
var spawnIntervalThird = 1 * 60 * 1000; // 4 minutes in milliseconds for new mob
var mobsToSpawn = [8880531]; // Mob IDs to spawn
var entryMap = 450010100; // Map ID to spawn mobs
var spawnedMobs = []; // List to keep track of spawned mobs

function setup(level, lobbyid) {
    var eim = em.newInstance("Hard Verus Hilla" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");
    eim.setIntProperty("fallenPlayers", 0);

    eim.getInstanceMap(entryMap).resetPQ(level);

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    eim.schedule("spawnFirstMob", spawnIntervalFirst);
    eim.schedule("spawnThirdMob", spawnIntervalThird);
    return eim;
}

function spawnFirstMob(eim) {
    var map = eim.getMapInstance(entryMap);
    var mobId = mobsToSpawn[0];
    var spawnLocation = new java.awt.Point(-714, 266);

    if (map != null) {
        var mob = em.getMonster(mobId);
        map.spawnMonsterOnGroundBelow(mob, spawnLocation);
        spawnedMobs.push(mob);
    }

    // Schedule the next spawnSecondMob function
    eim.schedule("spawnSecondMob", spawnIntervalSecond);
}

function spawnSecondMob(eim) {
    var map = eim.getMapInstance(entryMap);
    var mobId = mobsToSpawn[0];
    var spawnLocation = new java.awt.Point(592, 266);

    if (map != null) {
        var mob = em.getMonster(mobId);
        map.spawnMonsterOnGroundBelow(mob, spawnLocation);
    }
}

function spawnThirdMob(eim) {
    var map = eim.getMapInstance(entryMap);
    var mobId = 8880455; // New mob ID
    var spawnLocation = new java.awt.Point(-105, 266); // Updated spawn location

    if (map != null) {
        var mob = em.getMonster(mobId);
        map.spawnMonsterOnGroundBelow(mob, spawnLocation);
    }

    // Reschedule the next spawnThirdMob function
    eim.schedule("spawnThirdMob", spawnIntervalThird);
}

function afterSetup(eim) {}
function respawnStages(eim) {}

function playerEntry(eim, player) {
    var map = eim.getMapInstance(entryMap);
    player.changeMap(map, map.getPortal(0));
}

function scheduledTimeout(eim) {
    end(eim);
}

function playerUnregistered(eim, player) {}

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
        if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
            eim.unregisterPlayer(player);
            end(eim);
        } else {
            eim.unregisterPlayer(player);
        }
    }
}

function changedLeader(eim, leader) {}

function playerDead(eim, player) {
    var count = eim.getIntProperty("fallenPlayers");
    count = count + 1;

    eim.setIntProperty("fallenPlayers", count);

    if (count == 10) {
        eim.dropMessage(5, "[Expedition] Too many players have fallen, Verus is now deemed undefeatable; the expedition is over.");
        end(eim);
    } else if (count == 9) {
        eim.dropMessage(6, "[Expedition] This is your last chance! Do NOT die!");
    } else if (count == 5) {
        eim.dropMessage(6, "[Expedition] Casualty count is starting to get out of control. Battle with care.");
    }
}

function playerRevive(eim, player) { // Player presses ok on the death pop up.
    if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.unregisterPlayer(player);
    }
}

function playerDisconnected(eim, player) {
    if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.unregisterPlayer(player);
    }
}

function leftParty(eim, player) {}

function disbandParty(eim) {}

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
    eim.setEventCleared(ExpeditionType.VERUS);
}

function isVerus(mob) {
    var mobid = mob.getId();
    return mobid == 8880415;
}

function monsterKilled(mob, eim) {
    if (isVerus(mob)) {
        eim.showClearEffect();
        eim.clearPQ();
        party = eim.getPlayers()
        for (var i = 0; i < party.size(); i++)
        eim.getPlayers().get(i).getClient().getAbstractPlayerInteraction().gainItem(4001126, 300);
        mob.getMap().killAllMonsters();
    }
}

function allMonstersDead(eim) {}

function cancelSchedule() {}

function dispose(eim) {}
