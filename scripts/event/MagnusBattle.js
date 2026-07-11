const ExpeditionType = Java.type("server.expeditions.ExpeditionType");

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 200, maxLevel = 256;
var entryMap = 401060100;
var exitMap = 401053002;
var recruitMap = 401053002;
var clearMap = 401053002;

var minMapId = 401060100;
var maxMapId = 401060100;

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

    evLevel = 1;    //Rewards at clear PQ
    itemSet = [];
    itemQty = [];
    eim.setEventRewards(evLevel, itemSet, itemQty);

    expStages = [];    //bonus exp given on CLEAR stage signal
    eim.setEventClearStageExp(expStages);
}

function getEligibleParty(party) {      //selects, from the given party, the team that is allowed to attempt this event
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

var spawnTimes = [5000]; // Times to spawn mobs (in milliseconds)
var mobsToSpawn = [8880002]; // Mob IDs to spawn

function setup(level, lobbyid) {
    var eim = em.newInstance("Magnus" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");
    eim.setIntProperty("fallenPlayers", 0);

    eim.getInstanceMap(450010101).resetPQ(level);

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);

    // Schedule the spawnMob function to run at 1, 2, and 3 minutes
    eim.schedule("spawnMob1", spawnTimes[0]);

    return eim;
}

function spawnMob1(eim) {
    var map = eim.getMapInstance(entryMap); // Replace entryMap with the map you are working with
    var spawnX = 2592; // Example position, adjust as needed
    var spawnY = -1355;
    map.spawnMonsterOnGroundBelow(em.getMonster(mobsToSpawn[0]), new java.awt.Point(spawnX, spawnY));
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

function playerDead(eim, player) {}

function playerRevive(eim, player) { // player presses ok on the death pop up.
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
    eim.setEventCleared(ExpeditionType.MAGNUS);
}

function isMagnus(mob) {
    var mobid = mob.getId();
    return mobid == 8880002;
}

function monsterKilled(mob, eim) {
    if (isMagnus(mob)) {
        eim.showClearEffect();
        eim.clearPQ();
        party = eim.getPlayers()
        for (var i = 0; i < party.size(); i++)
        eim.getPlayers().get(i).getClient().getAbstractPlayerInteraction().gainItem(4001126, 300);
       // mob.getMap().broadcastVerusVictory();
    }
}

function allMonstersDead(eim) {}

function cancelSchedule() {}

function dispose(eim) {}