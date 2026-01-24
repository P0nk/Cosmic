/*
    3rd Job Event - Kenta's Mount Quest (Refactored)
**/

var entryMap = 923010000;
var exitMap = 923010100;

var minMapId = 923010000;
var maxMapId = 923010000;

var eventMaps = [923010000];
var eventTime = 5; // 5 minutes
const maxLobbies = 7;

function getMaxLobbies() {
    return maxLobbies;
}

function init() {
    em.setProperty("noEntry", "false");
}

function checkHogHealth(eim) {
    var watchHog = eim.getInstanceMap(923010000).getMonsterById(9300102);
    if (watchHog != null) {
        var hp = watchHog.getHp();
        var oldHp = eim.getIntProperty("whog_hp");

        if (oldHp - hp > 1000) {
            eim.dropMessage(6, "Please protect the pig from the aliens!");
        }
        eim.setIntProperty("whog_hp", hp);
    }
}

function respawnStages(eim) {
    var i;
    for (i = 0; i < eventMaps.length; i++) {
        eim.getInstanceMap(eventMaps[i]).instanceMapRespawn();
    }
    checkHogHealth(eim);

    eim.schedule("respawnStages", 10 * 1000);
}

function setup(level, lobbyid) {
    var eim = em.newInstance("3rdJob_mount_" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");
    eim.setProperty("whog_hp", "0");
    return eim;
}

function playerEntry(eim, player) {
    var mapObj = eim.getInstanceMap(entryMap);

    mapObj.resetPQ(1);
    mapObj.instanceMapForceRespawn();
    respawnStages(eim);

    player.changeMap(entryMap, 0);
    em.setProperty("noEntry", "true");

    // PacketCreator is auto-injected
    player.sendPacket(PacketCreator.getClock(eventTime * 60));
    eim.startEventTimer(eventTime * 60000);
}

function playerExit(eim, player) {
    var api = player.getAbstractPlayerInteraction();
    api.removeAll(4031507);
    api.removeAll(4031508);

    eim.unregisterPlayer(player);
    eim.dispose();
    em.setProperty("noEntry", "false");
}

function scheduledTimeout(eim) {
    var player = eim.getPlayers().get(0);
    playerExit(eim, player);
    player.changeMap(exitMap);
}

function playerDisconnected(eim, player) {
    playerExit(eim, player);
}

function changedMap(eim, chr, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        playerExit(eim, chr);
    }
}

function clearPQ(eim) {
    eim.stopEventTimer();
    eim.setEventCleared();

    var player = eim.getPlayers().get(0);
    eim.unregisterPlayer(player);
    player.changeMap(exitMap);

    eim.dispose();
    em.setProperty("noEntry", "false");
}

function friendlyKilled(mob, eim) {
    if (em.getProperty("noEntry") != "false") {
        var player = eim.getPlayers().get(0);
        playerExit(eim, player);
        player.changeMap(exitMap);
    }
}

function monsterValue(eim, mobId) {
    return 1;
}

// ---------- FILLER FUNCTIONS ----------
function playerUnregistered(eim, player) {}
function monsterKilled(mob, eim) {}
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose() {}
function disbandParty(eim, player) {}
function afterSetup(eim) {}
function changedLeader(eim, leader) {}
function leftParty(eim, player) {}