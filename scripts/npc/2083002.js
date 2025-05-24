var horntailAlive = false;

function start() {
    horntailAlive = false;
    for (var i = 8810000; i <= 8810018; i++) {
        if (cm.getPlayer().getMap().getMonsterById(i) !== null) {
            horntailAlive = true;
            break;
        }
    }

    var mapId = cm.getPlayer().getMapId();

    // Assuming Horntail fight is in map 240050400 (adjust if needed)
    if (mapId === 240060200) {
        if (horntailAlive) {
            cm.sendSimple(
                "Horntail is still alive.\r\nWhat would you like to do?\r\n" +
                "#b#L0#Leave the map#l"
            );
        } else {
            cm.sendSimple(
                "Horntail has been defeated.\r\nWhat would you like to do?\r\n" +
                "#b#L0#Leave the map#l\r\n" +
                "#L1#Let me spawn Horntail again, be careful he gets increasingly stronger!#l"
            );
        }
    } else {
        cm.sendYesNo("If you leave now, you'll have to start over. Are you sure you want to leave?");
    }
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }

    var mapId = cm.getPlayer().getMapId();

    // Handle non-Horntail maps (just leave)
    if (mapId !== 240060200) {
        // Warp out to appropriate map (adjust if needed)
        cm.warp(240050400);
        cm.dispose();
        return;
    }

    if (selection === 0) {
        // Leave the map immediately, no party checks
        cm.warp(240050400);
        cm.dispose();
    } else if (selection === 1) {
        horntailAlive = cm.getPlayer().getMap().getMonsterById(8810018) !== null;
        if (horntailAlive) {
            cm.sendOk("You cannot reset while Horntail is still alive.");
        } else {
            spawnHorntailWithScalingHP();
            cm.sendOk("Crystal has respawned. The next Horntail will be stronger!");
        }
        cm.dispose();
    }
}

function spawnHorntailWithScalingHP() {
    const Point = Java.type('java.awt.Point');

    var eim = cm.getPlayer().getEventInstance();
    if (eim == null) {
        cm.sendOk("You are not in an active expedition.");
        return;
    }

    var map = eim.getMapInstance(240060200);

    // Get clear count from EventManager
    var clears = eim.getClearCount();
    var multiplier = (0.15 * clears); // +15% HP per clear
    map.spawnHorntailOnGroundBelow(new Point(0, 120), multiplier);
}