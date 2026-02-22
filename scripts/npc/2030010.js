/*
 * Zakum Quest NPC
 * Helps players leave the map and reset reactors (only if Zakum is dead)
 */
var status = 0;
var zakumAlive = false;
var ZAKUM_ALTAR = 2111001; // Standard Zakum Altar ID. Change if needed.

// Helper: Check if the Altar is still ready to be used normally
function isAltarIntact() {
    var reactor = cm.getPlayer().getMap().getReactorById(ZAKUM_ALTAR);
    // If reactor exists and state < 1 (meaning ready/unbroken), it is intact
    if (reactor !== null && reactor.getState() < 1) {
        return true;
    }
    return false;
}

function start() {
    // Check if any Zakum bodies or arms (IDs 8800000 - 8800010) are alive in the map
    zakumAlive = false;
    var map = cm.getPlayer().getMap();
    var monsters = map.getMonsters(); // Get all monsters in the map
    for (var i = 0; i < monsters.size(); i++) {
        var mob = monsters.get(i);
        // Check if the monster is any of the Zakum bodies or arms
        if (mob.getId() >= 8800000 && mob.getId() <= 8800010) {
            zakumAlive = true;
            break;  // Exit the loop if any Zakum body or arm is found
        }
    }

    var altarReady = isAltarIntact();

    // Check if we are in the Zakum-related maps
    if (cm.getMapId() === 280030000 || (cm.getMapId() >= 280030100 && cm.getMapId() <= 280030130)) {
        if (zakumAlive) {
            cm.sendSimple(
                "Zakum is still alive.\r\nWhat would you like to do?\r\n" +
                "#b#L0#Leave the map#l"
            );
        } else {
            var msg = "Zakum has been defeated.\r\nWhat would you like to do?\r\n";

            // Only show Respawn option if Altar is NOT ready (meaning it was used already)
            if (!altarReady) {
                msg += "#b#L1#Let me spawn Zakum again#l\r\n";
            } else {
                if (cm.getPlayer().getWorld() === 1) { // Bera
                    msg += "#b#L2#Offer an Eye of Fire to summon Zakum#l\r\n";
                } else {
                    msg += "\r\n#k(Drop an Eye of Fire on the altar to summon the first Zakum)#k\r\n";
                }
            }

            msg += "#b#L0#Leave the map#l";
            cm.sendSimple(msg);
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

    // Handle non-Zakum maps (just leave)
    if (mapId !== 280030000 && (mapId < 280030100 || mapId > 280030130)) {
        cm.getPlayer().getClient().getChannelServer().removeMiniDungeon(mapId);
        cm.warp(211042300, 0);
        cm.dispose();
        return;
    }

    if (selection === 0) {
        player = cm.getPlayer();
        map = player.getMap();
        players = map.countPlayers();
        channel = player.getClient().getChannelServer();
        mmd = channel.getMiniDungeon(map.getId());
        if (players <= 1 || player.isPartyLeader()) {
            map.clearMapObjects();
            map.warpEveryone(211042300, 0);
            if (mmd != null) { // Null check added for safety
                mmd.close();
                channel.removeMiniDungeon(mapId);
            }
        } else {
            if (mmd != null) {
                mmd.unregisterPlayer(player);
            }
            cm.warp(211042300, 0);
        }
        cm.dispose();
    } else if (selection === 1) {
        // Double Check: Is Zakum alive?
        zakumAlive = false;
        var map = cm.getPlayer().getMap();
        var monsters = map.getMonsters();
        for (var i = 0; i < monsters.size(); i++) {
            var mob = monsters.get(i);
            if (mob.getId() >= 8800000 && mob.getId() <= 8800010) {
                zakumAlive = true;
                break;
            }
        }

        if (zakumAlive) {
            cm.sendOk("You cannot reset while Zakum or his arms are still alive.");
            cm.dispose();
            return;
        }

        // Double Check: Is Altar intact? (Double Spawn Protection)
        if (isAltarIntact()) {
            cm.sendOk("The Altar is currently active. Please drop an Eye of Fire on it to summon Zakum normally first.");
            cm.dispose();
            return;
        }

        // Check items and Spawn
        if (cm.haveItem(4001017, 1)) {
            cm.gainItem(4001017, -1);

            // --- AUTO CLEAR FUNCTION ---
            // Clears floor items before spawning
            cm.getPlayer().getMap().clearDrops();
            // ---------------------------

            cm.spawnZakum();
            cm.sendOk("The floor has been cleared and Zakum has been respawned.");
        } else {
            cm.sendOk("You do not have an #v4001017#.");
        }
        cm.dispose();
    } else if (selection === 2) {
        // Double Check: Is Zakum alive?
        zakumAlive = false;
        var map = cm.getPlayer().getMap();
        var monsters = map.getMonsters();
        for (var i = 0; i < monsters.size(); i++) {
            var mob = monsters.get(i);
            if (mob.getId() >= 8800000 && mob.getId() <= 8800010) {
                zakumAlive = true;
                break;
            }
        }

        if (zakumAlive) {
            cm.sendOk("You cannot spawn Zakum while he is already alive.");
            cm.dispose();
            return;
        }

        // Check items and Spawn
        if (cm.haveItem(4001017, 1)) {
            cm.gainItem(4001017, -1);

            // --- AUTO CLEAR FUNCTION ---
            // Clears floor items before spawning
            cm.getPlayer().getMap().clearDrops();
            // ---------------------------

            cm.spawnZakum();
            cm.getPlayer().getMap().broadcastMessage(Packages.tools.PacketCreator.musicChange("Bgm06/FinalFight"));
            cm.getPlayer().getMap().broadcastMessage(Packages.tools.PacketCreator.serverNotice(5, "Zakum is summoned by the force of Eye of Fire."));

            cm.sendOk("The floor has been cleared and Zakum has been summoned.");
        } else {
            cm.sendOk("You do not have an Eye of Fire (#v4001017#).");
        }
        cm.dispose();
    }
}