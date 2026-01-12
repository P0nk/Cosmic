var horntailAlive = false;

var MANA_CRYSTAL = 4021032; // Mana Crystal
var HT_MAP = 240060200;
var HT_REACTOR = 2401000; // The ID of the purple crystal reactor that spawns HT

function isHorntailAlive() {
    for (var i = 8810000; i <= 8810018; i++) {
        if (cm.getPlayer().getMap().getMonsterById(i) !== null) {
            return true;
        }
    }
    return false;
}

// Helper to check if the original crystal is still standing
function isCrystalIntact() {
    var reactor = cm.getPlayer().getMap().getReactorById(HT_REACTOR);
    // If reactor exists and state < 1 (meaning state 0, unbroken), it is intact
    if (reactor !== null && reactor.getState() < 1) {
        return true;
    }
    return false;
}

function start() {
    horntailAlive = isHorntailAlive();
    var crystalIntact = isCrystalIntact();
    var mapId = cm.getPlayer().getMapId();

    if (mapId === HT_MAP) {
        var msg = "";

        if (horntailAlive) {
            msg = "Horntail is still alive.\r\n";
        } else if (crystalIntact) {
            msg = "The summoning crystal is currently intact. Break it to start the battle!\r\n";
        } else {
            msg = "Horntail has been defeated.\r\n";
        }

        msg += "What would you like to do?\r\n\r\n";
        msg += "#b#L0#Leave the map#l\r\n";
        msg += "#L2#Clear items on the floor (Reduce Lag)#l\r\n"; // NEW OPTION

        // Only show Respawn option if HT is dead AND the original crystal is gone/broken
        if (!horntailAlive && !crystalIntact) {
            msg += "#L1#Replace the broken crystal using a #v" + MANA_CRYSTAL + "# Mana Crystal (x1)#l";
        }

        cm.sendSimple(msg);
    } else {
        cm.sendYesNo("If you leave now, you'll have to start over. Are you sure you want to leave?");
    }
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }

    // Selection 0: Leave Map
    if (selection === 0 || selection === -1) {
        if (cm.getMapId() > 240050400) {
            cm.warp(240050600);
        } else {
            cm.warp(240040700, "out00");
        }
        cm.dispose();
        return;
    }

    // Selection 2: Clear Drops (NEW)
    if (selection === 2) {
        // Calls the built-in method we found in your MapleMap.java
        cm.getPlayer().getMap().clearDrops();
        cm.sendOk("The floor has been swept of all items.");
        cm.dispose();
        return;
    }

    // Selection 1: Respawn HT
    if (selection === 1) {
        // 1. Security Check: Is HT Alive?
        horntailAlive = isHorntailAlive();
        if (horntailAlive) {
            cm.sendOk("You cannot reset while Horntail is still alive.");
            cm.dispose();
            return;
        }

        // 2. Security Check: Is the original Crystal still there? (Prevents double spawn)
        if (isCrystalIntact()) {
            cm.sendOk("Please break the purple crystal on the right side of the map to summon the first Horntail.");
            cm.dispose();
            return;
        }

        // 3. Item Check
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
    var cappedClears = Math.min(clears, 2);

    // +15% HP per clear, max +90%
    // var multiplier = 0.15 * cappedClears;
    var multiplier = 0 * cappedClears;

    map.spawnHorntailOnGroundBelow(new Point(90, 0), multiplier);
}