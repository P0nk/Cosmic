/*
    Kerning Train (Refactored)
*/

var returnTo = [103000100, 103000310];
var rideTo = [103000310, 103000100];
var trainRide = [103000301, 103000302];
var myRide;
var returnMap;
var exitMap;
var map;
var onRide;

var rideTime = 10 * 1000;

function init() {
    rideTime = em.getTransportationTime(rideTime);
}

function setup(level, lobbyid) {
    var eim = em.newInstance("KerningTrain_" + lobbyid);
    return eim;
}

function playerEntry(eim, player) {
    if (player.getMapId() == returnTo[0]) {
        myRide = 0;
    } else {
        myRide = 1;
    }

    exitMap = eim.getEm().getChannelServer().getMapFactory().getMap(rideTo[myRide]);
    returnMap = eim.getMapFactory().getMap(returnTo[myRide]);
    onRide = eim.getMapFactory().getMap(trainRide[myRide]);
    player.changeMap(onRide, onRide.getPortal(0));

    player.sendPacket(PacketCreator.getClock(rideTime / 1000));
    player.sendPacket(PacketCreator.earnTitleMessage("The next stop is at Kerning " + (myRide == 0 ? "Square" : "Subway") + " Station. The exit is to your left."));
    eim.schedule("timeOut", rideTime);
}

function timeOut(eim) {
    end(eim);
}

function playerExit(eim, player, success) {
    eim.unregisterPlayer(player);
    player.changeMap(success ? exitMap.getId() : returnMap.getId(), 0);
}

function end(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        playerExit(eim, party.get(i), true);
    }
    eim.dispose();
}

function playerDisconnected(eim, player) {
    playerExit(eim, player, false);
}

// ---------- FILLER FUNCTIONS ----------
function afterSetup(eim) {}
function playerUnregistered(eim, player) {}
function cancelSchedule() {}
function dispose(eim) {}
function monsterValue(eim, mobId) {return 0;}
function disbandParty(eim, player) {}
function monsterKilled(mob, eim) {}
function scheduledTimeout(eim) {}
function changedLeader(eim, leader) {}
function leftParty(eim, player) {}
function clearPQ(eim) {}
function allMonstersDead(eim) {}