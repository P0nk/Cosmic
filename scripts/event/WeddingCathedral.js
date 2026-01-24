/*
    Cathedral Wedding (Refactored)
*/

var entryMap = 680000200;
var exitMap = 680000500;
var recruitMap = 680000000;
var clearMap = 680000500;

var minMapId = 680000100;
var maxMapId = 680000401;

var startMsgTime = 4;
var blessMsgTime = 5;
var eventTime = 10;
var ceremonyTime = 20;
var blessingsTime = 15;
var partyTime = 45;
var forceHideMsgTime = 10;
var eventBoss = true;
var isCathedral = true;
const maxLobbies = 1;

function init() {}
function getMaxLobbies() { return maxLobbies; }

function setEventExclusives(eim) {
    eim.setExclusiveItems([4031217, 4000313]);
}

function setEventRewards(eim) {
    eim.setEventRewards(1, [], []);
    eim.setEventClearStageExp([]);
}

function spawnCakeBoss(eim) {
    var mapObj = eim.getMapInstance(680000400);
    var mobObj = LifeFactory.getMonster(9400606);
    mapObj.spawnMonsterOnGroundBelow(mobObj, new java.awt.Point(777, -177));
}

function setup(level, lobbyid) {
    var eim = em.newMarriage("Wedding" + lobbyid);
    eim.setProperty("weddingId", "0");
    eim.setProperty("weddingStage", "0");
    eim.setProperty("guestBlessings", "0");
    eim.setProperty("isPremium", "1");
    eim.setProperty("canJoin", "1");
    eim.setProperty("groomId", "0");
    eim.setProperty("brideId", "0");
    eim.setProperty("confirmedVows", "-1");
    eim.setProperty("groomWishlist", "");
    eim.setProperty("brideWishlist", "");
    eim.initializeGiftItems();

    eim.getInstanceMap(680000400).resetPQ(level);
    if (eventBoss) spawnCakeBoss(eim);

    respawnStages(eim);
    eim.startEventTimer(eventTime * 60000);
    setEventRewards(eim);
    setEventExclusives(eim);
    return eim;
}

function respawnStages(eim) {
    eim.getMapInstance(680000400).instanceMapRespawn();
    eim.schedule("respawnStages", 15 * 1000);
}

function playerEntry(eim, player) {
    eim.setProperty("giftedItemG" + player.getId(), "0");
    eim.setProperty("giftedItemB" + player.getId(), "0");
    player.getAbstractPlayerInteraction().gainItem(4000313, 1);
    var map = eim.getMapInstance(entryMap);
    player.changeMap(map, map.getPortal(0));
}

function stopBlessings(eim) {
    var mapobj = eim.getMapInstance(entryMap + 10);
    mapobj.dropMessage(6, "Wedding Assistant: Alright people, our couple are preparing their vows to each other right now.");
    eim.setIntProperty("weddingStage", 2);
}

function sendWeddingAction(eim, type) {
    var chr = eim.getLeader();
    // Use FULL path to avoid injection conflicts if Wedding isn't global
    var Wedding = Packages.tools.packets.WeddingPackets;

    if (chr.getGender() == 0) {
        chr.getMap().broadcastMessage(Wedding.OnWeddingProgress(type == 2, eim.getIntProperty("groomId"), eim.getIntProperty("brideId"), type + 1));
    } else {
        chr.getMap().broadcastMessage(Wedding.OnWeddingProgress(type == 2, eim.getIntProperty("brideId"), eim.getIntProperty("groomId"), type + 1));
    }
}

function hidePriestMsg(eim) {
    sendWeddingAction(eim, 2);
}

function showStartMsg(eim) {
    var Wedding = Packages.tools.packets.WeddingPackets;
    eim.getMapInstance(entryMap + 10).broadcastMessage(Wedding.OnWeddingProgress(false, 0, 0, 0));
    eim.schedule("hidePriestMsg", forceHideMsgTime * 1000);
}

function showBlessMsg(eim) {
    var Wedding = Packages.tools.packets.WeddingPackets;
    eim.getMapInstance(entryMap + 10).broadcastMessage(Wedding.OnWeddingProgress(false, 0, 0, 1));
    eim.setIntProperty("guestBlessings", 1);
    eim.schedule("hidePriestMsg", forceHideMsgTime * 1000);
}

function showMarriedMsg(eim) {
    sendWeddingAction(eim, 3);
    eim.schedule("hidePriestMsg", 10 * 1000);
    eim.restartEventTimer(partyTime * 60000);
}

function scheduledTimeout(eim) {
    if (eim.getIntProperty("canJoin") == 1) {
        em.getChannelServer().closeOngoingWedding(isCathedral);
        eim.setIntProperty("canJoin", 0);

        var mapobj = eim.getMapInstance(entryMap);
        var chr = mapobj.getCharacterById(eim.getIntProperty("groomId"));
        if (chr != null) chr.changeMap(entryMap + 10, "we00");

        chr = mapobj.getCharacterById(eim.getIntProperty("brideId"));
        if (chr != null) chr.changeMap(entryMap + 10, "we00");

        mapobj.dropMessage(6, "Wedding Assistant: The couple are heading to the altar, hurry hurry talk to me to arrange your seat.");

        eim.setIntProperty("weddingStage", 1);
        eim.schedule("showStartMsg", startMsgTime * 60 * 1000);
        eim.schedule("showBlessMsg", blessMsgTime * 60 * 1000);
        eim.schedule("stopBlessings", blessingsTime * 60 * 1000);
        eim.startEventTimer(ceremonyTime * 60000);
    } else {
        end(eim);
    }
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, 0);
}

function isMarrying(eim, player) {
    var playerid = player.getId();
    return playerid == eim.getIntProperty("groomId") || playerid == eim.getIntProperty("brideId");
}

function changedMap(eim, player, mapid) {
    if (mapid < minMapId || mapid > maxMapId) {
        if (isMarrying(eim, player)) {
            eim.unregisterPlayer(player);
            end(eim);
        } else {
            eim.unregisterPlayer(player);
        }
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
}

function monsterKilled(mob, eim) {
    if (mob.getId() == 9400606) {
        eim.showClearEffect();
        eim.clearPQ();
    }
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function playerUnregistered(eim, player) {}
function playerLeft(eim, player) { if (!eim.isEventCleared()) playerExit(eim, player); }
function changedLeader(eim, leader) {}
function playerDead(eim, player) {}
function playerRevive(eim, player) { if (isMarrying(eim, player)) { eim.unregisterPlayer(player); end(eim); } else { eim.unregisterPlayer(player); } }
function playerDisconnected(eim, player) { if (isMarrying(eim, player)) { eim.unregisterPlayer(player); end(eim); } else { eim.unregisterPlayer(player); } }
function leftParty(eim, player) {}
function disbandParty(eim) {}
function monsterValue(eim, mobId) { return 1; }
function giveRandomEventReward(eim, player) { eim.giveEventReward(player); }
function allMonstersDead(eim) {}
function cancelSchedule() {}
function dispose(eim) {}