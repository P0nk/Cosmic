/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * @author: Ronan
 * @event: Vs Von Leon
 */

const ExpeditionType = Java.type("server.expeditions.ExpeditionType");

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 200, maxLevel = 255;
var entryMap = 450012211;
var exitMap = 450012600;
var recruitMap = 450012600;
var clearMap = 450012600;

var minMapId = 450012211;
var maxMapId = 450012211;

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
var mobsToSpawn = [9995009]; // Mob IDs to spawn

function setup(level, lobbyid) {
    var eim = em.newInstance("Normal Darknell" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");
    eim.setIntProperty("fallenPlayers", 0);

    eim.getInstanceMap(450012211).resetPQ(level);

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
    var spawnX = 57; // Example position, adjust as needed
    var spawnY = 29;
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

function playerDead(eim, player) {
    var count = eim.getIntProperty("fallenPlayers");
    count = count + 1;

    eim.setIntProperty("fallenPlayers", count);

    if (count == 10) {
        eim.dropMessage(5, "[Expedition] Too many players have fallen, Darknell is now deemed undefeatable; the expedition is over.");
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
    eim.setEventCleared(ExpeditionType.NDARKNELL);
}

function isDarknell(mob) {
    var mobid = mob.getId();
    return mobid == 9995009;
}

function monsterKilled(mob, eim) {
    if (isDarknell(mob)) {
        eim.showClearEffect();
        eim.clearPQ();
        party = eim.getPlayers()
        for (var i = 0; i < party.size(); i++)
        eim.getPlayers().get(i).getClient().getAbstractPlayerInteraction().gainItem(4001126, 300);
       // mob.getMap().broadcastDarknellVictory();
    }
}

function allMonstersDead(eim) {}

function cancelSchedule() {}

function dispose(eim) {}