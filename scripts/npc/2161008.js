/*
    Von Leon (Human form) - NPC 2161008
    Appears after Von Leon dies, offers rematch.
*/

var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && type > 0) {
            cm.sendOk("Very well... you may rest now.");
            cm.dispose();
            return;
        }
        if (mode == 1) status++;
        else status--;

        if (status == 0) {
            cm.sendYesNo("You have bested me this time... Do you dare to face me again?");
        } else if (status == 1) {
            var eim = cm.getPlayer().getEventInstance();
            if (eim != null) {
                var map = eim.getMapInstance(211070100);
                const LifeFactory = Java.type('server.life.LifeFactory');
                const Point = Java.type('java.awt.Point');

                var mesocost = 100_000_000;
                if (cm.getMeso() < mesocost) {
                    if (cm.haveItem(3020002, 1)) {
                        cm.gainItem(3020002, -1)
                        cm.gainMeso(1000000000);
                        cm.gainMeso(-mesocost);
                    } else {
                        cm.sendOk("You need at least " + mesocost + " mesos to preview and perform this upgrade.");
                        return cm.dispose();
                    }
                } else {
                    cm.gainMeso(-mesocost);
                }
                // remove myself
                map.destroyNPC(cm.getNpc());

                // respawn Von Leon
                var boss = LifeFactory.getMonster(8840000);
                eim.registerMonster(boss);
                map.spawnMonsterOnGroundBelow(boss, new Point(49, -181));
            }
            cm.dispose();
        }
    }
}
