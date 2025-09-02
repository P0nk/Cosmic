/*
    Pink Bean Restart NPC (inside boss map)
    NPC ID: 2141002

    Allows leader to restart the fight without leaving.
*/

var status = 0;
var entryMap = 270050100; // Pink Bean battleground
var eventTime = 140; // minutes
var eventName = "PinkBeanBattle";

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }

    if (mode == 1) {
        status++;
    } else {
        cm.dispose();
        return;
    }

    if (status == 0) {
        cm.sendSimple("Do you wish to face #rPink Bean#k once again?\r\n\r\n"
            + "#b#L0#Restart the battle here#l\r\n"
            + "#L1#Leave the expedition#l");
    } else if (status == 1) {
        if (selection == 0) {
            var player = cm.getPlayer();
            var party = cm.getParty();

            if (party == null) {
                cm.sendOk("You must be in a party.");
                cm.dispose();
                return;
            }
            if (party.getLeader().getId() != player.getId()) {
                cm.sendOk("Only the party leader can restart the fight.");
                cm.dispose();
                return;
            }

            var eim = player.getEventInstance();
            if (eim == null) {
                cm.sendOk("No active event instance found.");
                cm.dispose();
                return;
            }

            // Reset stage and properties
            eim.setIntProperty("stage", 1);
            eim.setIntProperty("defeatedBoss", 0);
            eim.setIntProperty("fallenPlayers", 0);

            // Reset maps
            eim.getInstanceMap(270050100).resetPQ(1);
            eim.getInstanceMap(270050200).resetPQ(1);
            eim.getInstanceMap(270050300).resetPQ(1);

            // Remove all monsters
            eim.removeMonsters(entryMap);

            // Respawn Pink Bean's statue (8820000, no drops)
            const LifeFactory = Java.type('server.life.LifeFactory');
            const Point = Java.type('java.awt.Point');
            var mob = LifeFactory.getMonster(8820000);
            mob.disableDrops();
            eim.getInstanceMap(entryMap).spawnMonsterOnGroundBelow(mob, new Point(0, -42));

            // Restart timer
            eim.restartEventTimer(eventTime * 60000);

            // Trigger first wave
            eim.dropMessage(5, "[Expedition] The battle has restarted! Prepare yourselves!");
            eim.schedule("startWave", 5000);

            cm.dispose();

        } else if (selection == 1) {
            cm.warp(270050300, 0); // exit map
            cm.dispose();
        }
    }
}
