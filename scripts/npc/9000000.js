/*
    Raid NPC (Placeholder ID 9000000)
    Allows entry to the active Boss Raid map.
*/

var status = -1;

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) {
        status++;
    }

    if (status == 0) {
        var em = cm.getEventManager("BossRaid");
        if (em == null) {
            cm.sendOk("The event system is currently unavailable.");
            cm.dispose();
            return;
        }

        var state = em.getProperty("state");
        if (state == null || state != "active") {
            cm.sendOk("There is no Boss Raid happening right now. Keep an eye out for announcements!");
            cm.dispose();
        } else {
            var mapId = parseInt(em.getProperty("map"));
            cm.sendYesNo("A Boss Raid is active! Do you want to warp to the battlefield?");
        }
    } else if (status == 1) {
        var em = cm.getEventManager("BossRaid");
        if (em != null) {
            var mapId = parseInt(em.getProperty("map"));
            cm.warp(mapId);
        }
        cm.dispose();
    }
}