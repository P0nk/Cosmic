/*
    Pink Bean Lure NPC (inside boss map)
    NPC ID: 2141002

    Allows leader to summon a new Pink Bean using the Pink Poppin.
*/

var status = 0;
var entryMap = 270050100; // Pink Bean battleground
var eventTime = 140; // minutes
var eventName = "PinkBeanBattle";
var pinkFishId = 4001189; // Pink Fish (Poppin) item ID

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
        cm.sendSimple("Do you dare to face the mighty #rPink Bean#k once again?\r\n\r\n"
            + "#b#L0#Lure out Pink Bean with the Pink Poppin!#l\r\n"
            + "#L1#Leave the expedition#l");
    } else if (status == 1) {
        if (selection == 0) {
            var player = cm.getPlayer();
            var party = cm.getParty();

            if (party == null) {
                cm.sendOk("You must be in a party to summon Pink Bean.");
                cm.dispose();
                return;
            }
            if (party.getLeader().getId() != player.getId()) {
                cm.sendOk("Only the party leader can summon Pink Bean.");
                cm.dispose();
                return;
            }

            // Check if the party leader has a Pink Fish (Poppin)
            if (!cm.haveItem(pinkFishId)) {
                cm.sendOk("You must possess the legendary #rPink Poppin#k to lure the great #rPink Bean#k. It is the only thing capable of attracting this interdimensional being.");
                cm.dispose();
                return;
            }

            // Consume one Pink Fish (Poppin)
            cm.gainItem(pinkFishId, -1); // Remove one Pink Poppin from inventory

            var eim = player.getEventInstance();
            if (eim == null) {
                cm.sendOk("No active event instance found.");
                cm.dispose();
                return;
            }

            // Reset stage and properties
            eim.setIntProperty("stage", 5);
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
            eim.dropMessage(5, "#b[Expedition] You managed to lure out Pink Bean! Watch out, here he comes!!!");
            eim.schedule("startWave", 5000);

            cm.sendOk("#rI can't believe you have it!#k Watch out, here he comes!!!");
            cm.dispose();

        } else if (selection == 1) {
            cm.warp(270050300, 0); // exit map
            cm.dispose();
        }
    }
}
