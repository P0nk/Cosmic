/*
    Crimsonwood Keep PQ (Refactored)
*/

var isPq = true;
var minPlayers = 6, maxPlayers = 30;
var minLevel = 90, maxLevel = 255;
var entryMap = 610030100;
var exitMap = 610030020;
var recruitMap = 610030020;
var clearMap = 610030020;

var minMapId = 610030100;
var maxMapId = 610030800;

var eventTime = 2; // 2 minutes for first stage
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
    eim.setExclusiveItems([4001256, 4001257, 4001258, 4001259, 4001260]);
}

function setEventRewards(eim) {
    eim.setEventRewards(1, [], []);
    eim.setEventClearStageExp([2500, 8000, 18000, 25000, 30000, 40000]);
    eim.setEventClearStageMeso([500, 1000, 2000, 5000, 8000, 20000]);
}

function getNameFromList(index, array) {
    return array[index];
}

function generateMapReactors(map) {
    var jobReactors = [[0, 0, -1, -1, 0],
        [-1, 4, 3, 3, 3],
        [1, 3, 4, 2, 2],
        [2, -1, 0, 1, -1],
        [3, 2, 1, 0, -1],
        [4, 1, -1, 4, 1],
        [-1, 2, 4],
        [-1, -1]
    ];

    var rndIndex;
    var jobFound;
    while (true) {
        jobFound = {};
        rndIndex = [];

        for (var i = 0; i < jobReactors.length; i++) {
            var jobReactorSlot = jobReactors[i];
            var idx = Math.floor(Math.random() * jobReactorSlot.length);
            jobFound["" + jobReactorSlot[idx]] = 1;
            rndIndex.push(idx);
        }

        if (Object.keys(jobFound).length == 6) break;
    }

    var toDeploy = [];
    toDeploy.push(getNameFromList(rndIndex[0], ["4skill0a", "4skill0b", "4fake1c", "4fake1d", "4skill0e"]));
    toDeploy.push(getNameFromList(rndIndex[1], ["4fake0a", "4skill4b", "4skill3c", "4skill3d", "4skill3e"]));
    toDeploy.push(getNameFromList(rndIndex[2], ["4skill1a", "4skill3b", "4skill4c", "4skill2d", "4skill2e"]));
    toDeploy.push(getNameFromList(rndIndex[3], ["4skill2a", "4fake1b", "4skill0c", "4skill1d", "4fake1e"]));
    toDeploy.push(getNameFromList(rndIndex[4], ["4skill3a", "4skill2b", "4skill1c", "4skill0d", "4fake0e"]));
    toDeploy.push(getNameFromList(rndIndex[5], ["4skill4a", "4skill1b", "4fake0c", "4skill4d", "4skill1e"]));
    toDeploy.push(getNameFromList(rndIndex[6], ["4fake1a", "4skill2c", "4skill4e"]));
    toDeploy.push(getNameFromList(rndIndex[7], ["4fake0b", "4fake0d"]));

    var toRandomize = [];
    for (var i = 0; i < toDeploy.length; i++) {
        var react = map.getReactorByName(toDeploy[i]);
        react.setState(1);
        toRandomize.push(react);
    }

    map.shuffleReactors(toRandomize);
}

function setup(channel) {
    var eim = em.newInstance("CWKPQ" + channel);

    // Initialize all properties
    var props = ["glpq1", "glpq2", "glpq3", "glpq3_p", "glpq4", "glpq5", "glpq5_room", "glpq6",
                 "glpq_f0", "glpq_f1", "glpq_f2", "glpq_f3", "glpq_f4", "glpq_f5", "glpq_f6", "glpq_f7", "glpq_s"];
    eim.setProperty("current_instance", "0");
    for (var p of props) eim.setProperty(p, "0");

    var level = 1;
    var maps = [610030100, 610030200, 610030300, 610030400, 610030500, 610030510, 610030520,
                610030521, 610030522, 610030530, 610030540, 610030550, 610030600, 610030700, 610030800];

    for (var m of maps) eim.getInstanceMap(m).resetPQ(level);

    generateMapReactors(eim.getInstanceMap(610030400));
    eim.getInstanceMap(610030550).shuffleReactors();

    // Add Environments
    var a = ["a", "b", "c", "d", "e", "f", "g", "h", "i"];
    var map = eim.getInstanceMap(610030400);
    for (var x = 0; x < a.length; x++) {
        for (var y = 1; y <= 7; y++) {
            if (x == 1 || x == 3 || x == 4 || x == 6 || x == 8) {
                if (y != 2 && y != 4 && y != 5 && y != 7) map.moveEnvironment(a[x] + "" + y, 1);
            } else {
                map.moveEnvironment(a[x] + "" + y, 1);
            }
        }
    }

    // Spawn Monsters
    var pos_x = [944, 401, 28, -332, -855];
    var pos_y = [-204, -384, -504, -384, -204];
    var mobMap = eim.getInstanceMap(610030540);
    for (var z = 0; z < pos_x.length; z++) {
        var mob = em.getMonster(9400594);
        eim.registerMonster(mob);
        mobMap.spawnMonsterOnGroundBelow(mob, new java.awt.Point(pos_x[z], pos_y[z]));
    }

    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);

    eim.schedule("spawnGuardians", 60000);
    return eim;
}

function playerEntry(eim, player) {
    eim.dropMessage(5, "[Expedition] " + player.getName() + " has entered the map.");
    var map = eim.getMapInstance(610030100 + (eim.getIntProperty("current_instance") * 100));
    player.changeMap(map, map.getPortal(0));
}

function spawnGuardians(eim) {
    var map = eim.getMapInstance(610030100);
    if (map.countPlayers() <= 0) return;

    map.broadcastStringMessage(5, "The Master Guardians have detected you.");
    for (var i = 0; i < 20; i++) {
        var mob = eim.getMonster(9400594);
        eim.registerMonster(mob);
        map.spawnMonsterOnGroundBelow(mob, new java.awt.Point(1000, 336));
    }
}

function scheduledTimeout(eim) {
    end(eim);
}

function changedMap(eim, player, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
            eim.unregisterPlayer(player);
            eim.dropMessage(5, "[Expedition] Either the leader has quit the expedition or there is no longer the minimum number of members required to continue it.");
            end(eim);
        } else {
            if (!player.isGM()) {
                eim.dropMessage(5, "[Expedition] " + player.getName() + " has left the instance.");
                eim.unregisterPlayer(player);
            }
        }
    } else {
        var instance = eim.getIntProperty("current_instance");
        if (mapid == 610030200 && instance == 0) { eim.restartEventTimer(600000); eim.setIntProperty("current_instance", 1); }
        else if (mapid == 610030300 && instance == 1) { eim.restartEventTimer(600000); eim.setIntProperty("current_instance", 2); }
        else if (mapid == 610030400 && instance == 2) { eim.restartEventTimer(600000); eim.setIntProperty("current_instance", 3); }
        else if (mapid == 610030500 && instance == 3) { eim.restartEventTimer(1200000); eim.setIntProperty("current_instance", 4); }
        else if (mapid == 610030600 && instance == 4) { eim.restartEventTimer(3600000); eim.setIntProperty("current_instance", 5); }
        else if (mapid == 610030800 && instance == 5) { eim.restartEventTimer(60000); eim.setIntProperty("current_instance", 6); }
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

function playerRevive(eim, player) {
    if (eim.isEventTeamLackingNow(true, minPlayers, player)) {
        eim.unregisterPlayer(player);
        eim.dropMessage(5, "[Expedition] Team requirements failed.");
        end(eim);
    } else {
        eim.dropMessage(5, "[Expedition] " + player.getName() + " has left the instance.");
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

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function changedLeader(eim, leader) {}
function playerDead(eim, player) {}
function leftParty(eim, player) {}
function disbandParty(eim) {}
function monsterValue(eim, mobId) { return 1; }
function playerUnregistered(eim, player) {}
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function clearPQ(eim) { eim.stopEventTimer(); eim.setEventCleared(); }
function monsterKilled(mob, eim) {}
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}