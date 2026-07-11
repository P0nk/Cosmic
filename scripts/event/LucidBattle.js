
const ExpeditionType = Java.type("server.expeditions.ExpeditionType");

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 200, maxLevel = 255;
var entryMap = 450004750;
var exitMap = 450003600;
var recruitMap = 450003600;
var clearMap = 450003600;

var minMapId = 450004750;
var maxMapId = 450004750;

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

function setup(level, lobbyid) {
    var eim = em.newInstance("Hard Lucid" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");
    eim.setIntProperty("fallenPlayers", 0);

    eim.getInstanceMap(450004750).resetPQ(level);

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    
    // Schedule monster spawn every 3 minutes
    eim.schedule("spawnMonster", 60000); // 180000 milliseconds = 3 minutes
    
    return eim;
}

function spawnMonster(eim) {
    var map = eim.getMapInstance(entryMap);
    var monster = em.getMonster(8880183);
    map.spawnMonsterOnGroundBelow(monster, new java.awt.Point(1012, 48)); // Spawning at (1012, 48)
    
    // Reschedule the next spawn
    eim.schedule("spawnMonster", 60000); // 180000 milliseconds = 3 minutes
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
        eim.dropMessage(5, "[Expedition] Too many players have fallen, Lucid is now deemed undefeatable; the expedition is over.");
        end(eim);
    } else if (count == 9) {
        eim.dropMessage(6, "[Expedition] This is your last chance! Do NOT die!");
    } else if (count == 5) {
        eim.dropMessage(6, "[Expedition] Casualty count is starting to get out of control. Battle with care.");
    }
}

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
    eim.setEventCleared(ExpeditionType.LUCID);
}

function isLucid(mob) {
    var mobid = mob.getId();
    return mobid == 8880150;
}

function monsterKilled(mob, eim) {
    if (isLucid(mob)) {
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
