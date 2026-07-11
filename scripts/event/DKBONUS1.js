var returnTo = [910000010];
var rideTo = [910000010];
var trainRide = [910000008];
var myRide;
var returnMap;
var exitMap;
var onRide;

// Time Setting is in milliseconds (4 minutes)
var rideTime = 21 * 60 * 1000;

function init() {
    // Make sure the rideTime is not overridden by any other value
    rideTime = em.getTransportationTime(rideTime) || rideTime;
}

function setup(level, lobbyid) {
    var eim = em.newInstance("DKBONUS1" + lobbyid);
    return eim;
}

function afterSetup(eim) {}

function playerEntry(eim, player) {
    myRide = 0;

    exitMap = eim.getEm().getChannelServer().getMapFactory().getMap(rideTo[myRide]);
    returnMap = eim.getMapFactory().getMap(returnTo[myRide]);
    onRide = eim.getMapFactory().getMap(trainRide[myRide]);
    
    player.changeMap(onRide, onRide.getPortal(0));

    const PacketCreator = Java.type('tools.PacketCreator');
    player.sendPacket(PacketCreator.getClock(rideTime / 1000));
    player.sendPacket(PacketCreator.earnTitleMessage("You have 2 minutes to complete Bramble Scramble!."));
    eim.schedule("timeOut", rideTime);
}

function timeOut(eim) {
    end(eim);
}

function playerUnregistered(eim, player) {}

function playerExit(eim, player, success) {
    eim.unregisterPlayer(player);
    player.changeMap(success ? exitMap.getId() : returnMap.getId(), 0);
    player.getAbstractPlayerInteraction().cancelItem(2210044); // Cancel the item when the player exits
    player.getAbstractPlayerInteraction().cancelItem(2210042); // Cancel the item when the player exits
    eim.stopEventTimer();
}

function end(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++) {
        var player = party.get(i);
        playerExit(eim, party.get(i), true);
        player.getAbstractPlayerInteraction().cancelItem(2210044); // Cancel the item when the player exits
        player.getAbstractPlayerInteraction().cancelItem(2210042); // Cancel the item when the player exits
        eim.stopEventTimer();
    }
    eim.dispose();
}

function playerRevive(eim, player) { // player presses ok on the death pop up.
        eim.unregisterPlayer(player);
        end(eim);
}

function playerDisconnected(eim, player) {
    eim.unregisterPlayer(player);
    end(eim);
}


function cancelSchedule() {}

function dispose(eim) {}

// ---------- FILLER FUNCTIONS ----------

function monsterValue(eim, mobid) { return 0; }
function disbandParty(eim, player) {}
function monsterKilled(mob, eim) {}
function scheduledTimeout(eim) {}
function changedLeader(eim, leader) {}
function leftParty(eim, player) {}
function clearPQ(eim) {}
function allMonstersDead(eim) {}
