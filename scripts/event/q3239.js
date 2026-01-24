/*
    Quest 3239 Event (Refactored)
*/

var entryMap;
var exitMap;
var eventLength = 20;

function init() {
    em.setProperty("noEntry", "false");
    entryMap = em.getChannelServer().getMapFactory().getMap(922000000);
    exitMap = em.getChannelServer().getMapFactory().getMap(922000009);
}

function setup(level, lobbyid) {
    var eim = em.newInstance("q3239_" + lobbyid);
    eim.setExclusiveItems([4031092]);
    return eim;
}

function playerEntry(eim, player) {
    var im = eim.getInstanceMap(entryMap.getId());

    im.clearDrops();
    im.resetReactors();
    im.shuffleReactors();

    eim.startEventTimer(eventLength * 60 * 1000);

    player.changeMap(entryMap, 0);
    em.setProperty("noEntry", "true");
}

function changedMap(eim, player, mapid) {
    if (mapid != entryMap.getId())
        playerExit(eim, player);
}

function playerExit(eim, player) {
    end(eim);
}

function playerDisconnected(eim, player) {
    end(eim);
}

function scheduledTimeout(eim) {
    end(eim);
}

function end(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        var player = party.get(i);
        eim.unregisterPlayer(player);
        player.changeMap(exitMap);
    }

    eim.dispose();
    em.setProperty("noEntry", "false");
}

// ---------- FILLER FUNCTIONS ----------
function disbandParty(eim, player) {}
function afterSetup(eim) {}
function playerUnregistered(eim, player) {}
function changedLeader(eim, leader) {}
function leftParty(eim, player) {}
function clearPQ(eim) {}
function dispose() {}
function cancelSchedule() {}
function allMonstersDead(eim) {}
function monsterValue(eim, mobId) {}
function monsterKilled(mob, eim) {}