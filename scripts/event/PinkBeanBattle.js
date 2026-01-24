/*
    Pink Bean Battle (Refactored)
*/

var isPq = true;
var minPlayers = 1, maxPlayers = 30;
var minLevel = 120, maxLevel = 255;
var entryMap = 270050100;
var exitMap = 270050300;
var recruitMap = 270050000;
var clearMap = 270050300;

var minMapId = 270050100;
var maxMapId = 270050300;
var eventTime = 140;

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
    eim.setEventClearStageMeso([]);
}

function afterSetup(eim) {
    eim.dropMessage(5, "The first wave will start, prepare yourselves.");
    eim.removeMonsters(270050100);
    startWave(eim)
}

function setup(channel) {
    var eim = em.newInstance("PinkBean" + channel);
    eim.setProperty("canJoin", 1);
    eim.setProperty("defeatedBoss", 0);
    eim.setProperty("fallenPlayers", 0);
    eim.setProperty("stage", 1);
    eim.setProperty("channel", channel);

    var level = 1;
    eim.getInstanceMap(270050100).resetPQ(level);
    eim.getInstanceMap(270050200).resetPQ(level);
    eim.getInstanceMap(270050300).resetPQ(level);

    var mob = LifeFactory.getMonster(8820000);
    mob.disableDrops();
    eim.getInstanceMap(270050100).spawnMonsterOnGroundBelow(mob, new java.awt.Point(0, -42));

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
            end(eim);
        } else {
            eim.dropMessage(5, "[Expedition] " + player.getName() + " has left the expedition.");
            eim.unregisterPlayer(player);
        }
    }
}

function playerDead(eim, player) {
    var count = eim.getIntProperty("fallenPlayers") + 1;
    eim.setIntProperty("fallenPlayers", count);

    if (count == 5) {
        eim.dropMessage(5, "[Expedition] Too many players have fallen, Pink Bean is now deemed undefeatable; the expedition is over.");
        end(eim);
    } else if (count == 4) {
        eim.dropMessage(5, "[Expedition] Pink Bean is growing stronger than ever, last stand mode everyone!");
    } else if (count == 3) {
        eim.dropMessage(5, "[Expedition] Casualty count is starting to get out of control. Battle with care.");
    }
}

function playerRevive(eim, player) {
    return true;
}

function monsterRevive(eim, mob) {
    if (isPinkBean(mob)) {
        mob.enableDrops();
    }
}

function playerDisconnected(eim, player) {
    if (eim.isExpeditionTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        end(eim);
    } else {
        eim.dropMessage(5, "[Expedition] " + player.getName() + " has left the expedition.");
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

function isPinkBean(mob) {
    return mob.getId() == 8820001;
}

function isJrBoss(mob) {
    var mobid = mob.getId();
    return (mobid >= 8820002 && mobid <= 8820006);
}

function noJrBossesLeft(map) {
    return map.countMonster(8820002, 8820006) == 0;
}

function spawnJrBoss(mobObj, gotKilled) {
    var spawnid = gotKilled ? mobObj.getId() + 17 : mobObj.getId() - 17;
    if (!gotKilled) mobObj.getMap().killMonster(mobObj.getId());

    var mob = LifeFactory.getMonster(spawnid);
    mobObj.getMap().spawnMonsterOnGroundBelow(mob, mobObj.getPosition());
}

function monsterKilled(mob, eim) {
    if (isPinkBean(mob)) {
        eim.setIntProperty("defeatedBoss", 1);
        eim.showClearEffect(mob.getMap().getId());
        mob.getMap().killAllMonsters();
        eim.clearPQ();
        var ch = eim.getIntProperty("channel");
        mob.getMap().broadcastPinkBeanVictory(ch);
    } else if (isJrBoss(mob)) {
        if (noJrBossesLeft(mob.getMap())) {
            var stage = eim.getIntProperty("stage");
            if (stage == 5) {
                var itemObj = new Packages.client.inventory.Item(4001193, 0, 1);
                var mapObj = eim.getMapFactory().getMap(270050100);
                var reactObj = mapObj.getReactorById(2708000);
                var dropper = eim.getPlayers().get(0);
                mapObj.spawnItemDrop(dropper, dropper, itemObj, reactObj.getPosition(), true, true);
                eim.dropMessage(6, "With the last of its guardians fallen, Pink Bean loses its invulnerability. The real fight starts now!");
            } else {
                stage++;
                eim.setIntProperty("stage", stage);
                eim.dropMessage(5, "The next wave will start, prepare yourselves.");
                startWave(eim)
            }
        }
    }
}

function startWave(eim) {
    var mapObj = eim.getMapInstance(270050100);
    if (!mapObj) {
        eim.dropMessage(5, "Map not ready, retrying...");
        eim.schedule("startWave", 2000);
        return;
    }

    var stage = parseInt(eim.getProperty("stage"));
    var spawnPositions = [
        new java.awt.Point(5, -42),
        new java.awt.Point(5, -42),
        new java.awt.Point(5, -42),
        new java.awt.Point(5, -42),
        new java.awt.Point(5, -42)
    ];

    var spawnedMobs = [];
    for (var i = 1; i <= stage; i++) {
        var baseMobId = 8820019 + (i % 5);
        var baseMob = LifeFactory.getMonster(baseMobId);
        var spawnPos = spawnPositions[(i - 1) % spawnPositions.length];
        mapObj.spawnMonsterOnGroundBelow(baseMob, spawnPos);
        spawnedMobs.push(baseMob);
    }

    for (var i = 0; i < spawnedMobs.length; i++) {
        spawnJrBoss(spawnedMobs[i], false);
    }
}

// ---------- FILLER FUNCTIONS ----------
function changedLeader(eim, leader) {}
function leftParty(eim, player) {}
function disbandParty(eim) {}
function monsterValue(eim, mobId) { return 1; }
function playerUnregistered(eim, player) {}
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}