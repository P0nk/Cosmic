/*
    2x EXP Event Script (Refactored)
*/

var timer1;
var timer2;
var timer3;
var timer4;

function init() {
    /*
    if(em.getChannelServer().getId() == 1) { // Only run on channel 1.
        timer1 = em.scheduleAtTimestamp("start", 1428220800000);
        timer2 = em.scheduleAtTimestamp("stop", 1428228000000);
    }
    */
}

function cancelSchedule() {
    if (timer1 != null) timer1.cancel(true);
    if (timer2 != null) timer2.cancel(true);
    if (timer3 != null) timer3.cancel(true);
    if (timer4 != null) timer4.cancel(true);
}

function start() {
    // PacketCreator is now Global!
    var Server = Java.type('net.server.Server');
    var world = Server.getInstance().getWorld(em.getChannelServer().getWorld());
    world.setExpRate(8);
    world.broadcastPacket(PacketCreator.serverNotice(6, "The Bunny Onslaught Survival Scanner (BOSS) has detected an Easter Bunny onslaught soon! The GM team has activated the Emergency XP Pool (EXP) that doubles experience gained for the next two hours!"));
}

function stop() {
    // PacketCreator is now Global!
    var Server = Java.type('net.server.Server');
    var world = Server.getInstance().getWorld(em.getChannelServer().getWorld());
    world.setExpRate(4);
    world.broadcastPacket(PacketCreator.serverNotice(6, "Unfortunately the Emergency XP Pool (EXP) has run out of juice for now and needs to recharge causing the EXP rate to go back to normal."));
}

// ---------- FILLER FUNCTIONS ----------
function dispose() {}
function setup(eim, leaderid) {}
function monsterValue(eim, mobid) {return 0;}
function disbandParty(eim, player) {}
function playerDisconnected(eim, player) {}
function playerEntry(eim, player) {}
function monsterKilled(mob, eim) {}
function scheduledTimeout(eim) {}
function afterSetup(eim) {}
function changedLeader(eim, leader) {}
function playerExit(eim, player) {}
function leftParty(eim, player) {}
function clearPQ(eim) {}
function allMonstersDead(eim) {}
function playerUnregistered(eim, player) {}