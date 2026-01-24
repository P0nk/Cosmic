/*
    Holiday PQ 3 (Refactored)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 6;
var minLevel = 41, maxLevel = 255;
var entryMap = 889100021;
var exitMap = 889100022;
var recruitMap = 889100020;
var clearMap = 889100022;

var minMapId = 889100021;
var maxMapId = 889100021;
var eventTime = 25;

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
    eim.setExclusiveItems([4032094, 4032095]);
}

function setEventRewards(eim) {
    eim.setEventRewards(3, [1302080, 1002033, 2022153, 2022042, 2020006, 2020009, 2020016, 2020024, 4010006, 4010007, 4020004, 4020005, 4003002], [1, 1, 1, 5, 20, 15, 10, 10, 2, 4, 4, 4, 1]);
    eim.setEventRewards(2, [1302080, 1002033, 2012005, 2012006, 2020002, 2020025, 2020026, 4010003, 4010004, 4010005, 4020002, 4020003, 4020007], [1, 1, 15, 15, 15, 10, 10, 3, 3, 3, 3, 3, 3]);
    eim.setEventRewards(1, [1002033, 2012005, 2012006, 2020002, 2022006, 2022002, 4010000, 4010001, 4010002, 4020000, 4020001, 4020006], [1, 15, 15, 10, 5, 5, 2, 2, 2, 2, 2, 2]);
    eim.setEventClearStageExp([210, 620, 500, 1400, 950, 2200]);
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
    var eim = em.newInstance("Holiday3_" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("stage", "0");
    eim.setProperty("statusStg1", "-1");
    eim.setProperty("missingDrops", "0");
    eim.setProperty("snowmanLevel", "0");
    eim.setProperty("snowmanStep", "0");
    eim.setProperty("spawnedBoss", "0");

    var mapobj = eim.getInstanceMap(entryMap);
    mapobj.resetPQ(level);
    mapobj.allowSummonState(false);

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function respawnStages(eim) {
    eim.getInstanceMap(entryMap).instanceMapRespawn();
    eim.schedule("respawnStages", 10 * 1000);
}

function snowmanHeal(eim) {
    var difficulty = eim.getIntProperty("level");
    var snowman = eim.getInstanceMap(entryMap).getMonsterById(9400316 + (5 * difficulty) + 5);
    snowman.heal(200 + 200 * difficulty, 0);
    eim.schedule("snowmanHeal", 10 * 1000);
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

function clearPQ(eim) {
    eim.stopEventTimer();
    eim.setEventCleared();
    eim.applyEventPlayersItemBuff(2022438);
}

function isScrooge(mob) {
    var mobid = mob.getId();
    return mobid >= 9400319 && mobid <= 9400321;
}

function monsterKilled(mob, eim) {
    try {
        if (eim.isEventCleared()) {
            return;
        } else if (isScrooge(mob)) {
            eim.giveEventPlayersStageReward(2 * eim.getIntProperty("level"));
            eim.showClearEffect();
            eim.clearPQ();
            return;
        }

        var rnd = Math.random();
        var forceDrop = false;
        if (rnd >= 0.42) {
            var miss = eim.getIntProperty("missingDrops");
            if (miss < 5) {
                eim.setIntProperty("missingDrops", miss + 1);
                return;
            }
            forceDrop = true;
        }

        var mapObj = mob.getMap();
        var itemObj = new Packages.client.inventory.Item((forceDrop || Math.random() < 0.77) ? 4032094 : 4032095, 0, 1);
        var dropper = eim.getPlayers().get(0);

        mapObj.spawnItemDrop(mob, dropper, itemObj, mob.getPosition(), true, false);
        eim.setIntProperty("missingDrops", 0);
    } catch (err) {}
}

function friendlyKilled(mob, eim) {
    eim.setIntProperty("snowmanStep", 0);
    var snowmanLevel = eim.getIntProperty("snowmanLevel");

    if (snowmanLevel <= 1) {
        end(eim);
    } else {
        eim.setIntProperty("snowmanLevel", snowmanLevel - 1);
    }
}

function snowmanEvolve(eim, curLevel) {
    var mapobj = eim.getInstanceMap(entryMap);
    var difficulty = eim.getIntProperty("level");
    var snowman = mapobj.getMonsterById(9400317 + (5 * difficulty) + (curLevel - 1));

    eim.setIntProperty("snowmanLevel", curLevel + 2);
    mapobj.killMonster(snowman, null, false, 2);

    var newSnowman = LifeFactory.getMonster(9400317 + (5 * difficulty) + curLevel);
    mapobj.spawnMonsterOnGroundBelow(newSnowman, new java.awt.Point(-180, 15));

    if (curLevel >= 4) {
        mapobj.allowSummonState(false);
        mapobj.killAllMonstersNotFriendly();
        mapobj.setReactorState();
        eim.giveEventPlayersStageReward(2 * difficulty - 1);
        eim.showClearEffect();
    }
}

function snowmanSnack(eim) {
    if (eim.getIntProperty("snowmanLevel") >= 5) return;

    var step = eim.getIntProperty("snowmanStep");
    var snowmanLevel = eim.getIntProperty("snowmanLevel");

    if (step >= 2 + (eim.getIntProperty("level") * snowmanLevel)) {
        step = 0;
        snowmanEvolve(eim, snowmanLevel);
    } else {
        var mapobj = eim.getInstanceMap(entryMap);
        var difficulty = eim.getIntProperty("level");
        var snowman = mapobj.getMonsterById(9400316 + (5 * difficulty) + snowmanLevel);

        snowman.heal(200 + (200 * snowmanLevel), 0);
        step += 1;
    }
    eim.setIntProperty("snowmanStep", step);
}

function snowmanSnackFake(eim) {
    if (eim.getIntProperty("snowmanLevel") >= 5) return;

    var step = eim.getIntProperty("snowmanStep");
    if (step > 0) eim.setIntProperty("snowmanStep", step - 1);
    eim.dropMessage(5, "The snowman absorbed a Fake Snow Vigor!");
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