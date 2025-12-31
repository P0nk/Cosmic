/*
    Von Leon (Human form) - NPC 2161008
    Appears after Von Leon dies, offers rematch.
    Revised: Ticket-based respawn with themed dialogue.
*/

var status = 0;
var VON_LEON_TICKET = 4001694;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }

    if (mode == 0 && type > 0) {
        cm.sendOk(
            "Hmph... then crawl away while you still can.\r\n" +
            "I will be waiting."
        );
        cm.dispose();
        return;
    }

    if (mode == 1) status++;
    else status--;

    if (status == 0) {
        cm.sendYesNo(
            "So... you have struck me down once.\r\n\r\n" +
            "But do not mistake that for victory.\r\n" +
            "My curse binds me to this throne.\r\n\r\n" +
            "If you possess the courage — and the means —\r\n" +
            "present a #v" + VON_LEON_TICKET + "# and face me once more.\r\n\r\n" +
            "#rDo you dare challenge Von Leon again?#k"
        );
    }
    else if (status == 1) {
        var eim = cm.getPlayer().getEventInstance();
        if (eim == null) {
            cm.sendOk("This place no longer resonates with battle.");
            cm.dispose();
            return;
        }

        if (!cm.haveItem(VON_LEON_TICKET, 1)) {
            cm.sendOk(
                "You dare speak of challenge without tribute?\r\n\r\n" +
                "Bring me a #v" + VON_LEON_TICKET + "#.\r\n" +
                "Only then shall my wrath be unleashed once more."
            );
            cm.dispose();
            return;
        }

        // Consume ticket
        cm.gainItem(VON_LEON_TICKET, -1);

        var map = eim.getMapInstance(211070100);
        const LifeFactory = Java.type('server.life.LifeFactory');
        const Point = Java.type('java.awt.Point');

        // Remove NPC (myself)
        map.destroyNPC(cm.getNpc());

        // Respawn Von Leon
        var boss = LifeFactory.getMonster(8840000);
        eim.registerMonster(boss);
        map.spawnMonsterOnGroundBelow(boss, new Point(49, -181));

        map.broadcastMessage(
            Packages.tools.PacketCreator.serverNotice(
                6,
                "Von Leon roars as dark energy floods the throne room once more!"
            )
        );

        cm.dispose();
    }
}
