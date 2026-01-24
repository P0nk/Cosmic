/*
    Dollhouse Event (Refactored)
*/

var entryMap = 922000010;
var exitMap = 221024400;
var eventTime = 10; // 10 minutes

function init() {
    em.setProperty("noEntry", "false");
}

function setup(level, lobbyid) {
    var eim = em.newInstance("DollHouse_" + lobbyid);
    eim.setProperty("level", level);
    eim.setProperty("boss", "0");
    return eim;
}

function playerEntry(eim, player) {
    eim.getInstanceMap(entryMap).shuffleReactors();
    eim.setExclusiveItems([4031094]);

    player.changeMap(entryMap, 0);
    em.setProperty("noEntry", "true");

    player.sendPacket(PacketCreator.getClock(eventTime * 60));
    eim.startEventTimer(eventTime * 60000);
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    eim.dispose();
    em.setProperty("noEntry", "false");
}

function scheduledTimeout(eim) {
    var player = eim.getPlayers().get(0);
    playerExit(eim, eim.getPlayers().get(0));
    player.changeMap(exitMap, 4);
}

function playerDisconnected(eim, player) {
    playerExit(eim, player);
}

function clear(eim) {
    var player = eim.getPlayers().get(0);
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, 4);

    eim.dispose();
    em.setProperty("noEntry", "false");
}

function changedMap(eim, chr, mapid) {
    if (mapid != entryMap) {
        playerExit(eim, chr);
    }
}

// ---------- FILLER FUNCTIONS ----------
function playerUnregistered(eim, player) {}
function cancelSchedule() {}
function dispose() {}
function monsterValue(eim, mobid) {return 0;}
function disbandParty(eim, player) {}
function monsterKilled(mob, eim) {}
function afterSetup(eim) {}
function changedLeader(eim, leader) {}
function leftParty(eim, player) {}
function clearPQ(eim) {}
function allMonstersDead(eim) {}