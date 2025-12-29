var horntailAlive = false;

var MANA_CRYSTAL = 4021032; // Mana Crystal
var HT_MAP = 240060200;

function isHorntailAlive() {
    for (var i = 8810000; i <= 8810018; i++) {
        if (cm.getPlayer().getMap().getMonsterById(i) !== null) {
            return true;
        }
    }
    return false;
}

function start() {
    horntailAlive = isHorntailAlive();

    var mapId = cm.getPlayer().getMapId();

    if (mapId === HT_MAP) {
        if (horntailAlive) {
            cm.sendSimple(
                "Horntail is still alive.\r\nWhat would you like to do?\r\n" +
                "#b#L0#Leave the map#l"
            );
        } else {
            cm.sendSimple(
                "Horntail has been defeated.\r\nWhat would you like to do?\r\n" +
                "#b#L0#Leave the map#l\r\n" +
                "#L1#Replace the broken crystal using a #v" + MANA_CRYSTAL + "# Mana Crystal (x1)#l"
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

    if (selection === 0 || selection === -1) {
        if (cm.getMapId() > 240050400) {
            cm.warp(240050600);
        } else {
            cm.warp(240040700, "out00");
        }
        cm.dispose();
        return;
    }

    if (selection === 1) {
        // Re-check all Horntail parts
        horntailAlive = isHorntailAlive();
        if (horntailAlive) {
            cm.sendOk("You cannot reset while Horntail is still alive.");
            cm.dispose();
            return;
        }

        // Require Mana Crystal
        if (!cm.haveItem(MANA_CRYSTAL, 1)) {
            cm.sendOk(
                "The summoning crystal has shattered.\r\n\r\n" +
                "You need a #v" + MANA_CRYSTAL + "# Mana Crystal (x1) to replace it."
            );
            cm.dispose();
            return;
        }

        // Consume crystal and respawn
        cm.gainItem(MANA_CRYSTAL, -1);
        spawnHorntailWithScalingHP();

        cm.sendOk(
            "The restored crystal glows violently...\r\n" +
            "Horntail has been summoned once more!"
        );
        cm.dispose();
        return;
    }

    cm.dispose();
}

function spawnHorntailWithScalingHP() {
    const Point = Java.type('java.awt.Point');

    var eim = cm.getPlayer().getEventInstance();
    if (eim == null) {
        cm.sendOk("You are not in an active expedition.");
        return;
    }

    var map = eim.getMapInstance(HT_MAP);

    // Get clear count (cap at 6 runs)
    var clears = eim.getClearCount();
    var cappedClears = Math.min(clears, 6);

    // +15% HP per clear, max +90%
    var multiplier = 0.15 * cappedClears;

    map.spawnHorntailOnGroundBelow(new Point(90, 0), multiplier);
}
