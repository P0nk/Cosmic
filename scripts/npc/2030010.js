/*
 * Zakum Quest NPC
 * Helps players leave the map and reset reactors (only if Zakum is dead)
 */
var status = 0;
var zakumAlive = false;

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

    // Check if we are in the Zakum-related maps
    if (cm.getMapId() === 280030000 || (cm.getMapId() >= 280030100 && cm.getMapId() <= 280030130)) {
        if (zakumAlive) {
            cm.sendSimple(
                "Zakum is still alive.\r\nWhat would you like to do?\r\n" +
                "#b#L0#Leave the map#l"
            );
        } else {
            cm.sendSimple(
                "Zakum has been defeated.\r\nWhat would you like to do?\r\n" +
                "#b#L1#Let me spawn Zakum again#l\r\n" +
                "#L0#Leave the map#l"
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
            mmd.close();
            channel.removeMiniDungeon(mapId);
        } else {
            mmd.unregisterPlayer(player);
            cm.warp(211042300, 0);
        }
        cm.dispose();
    } else if (selection === 1) {
        // Check if Zakum or his arms are still alive
        zakumAlive = false;
        var map = cm.getPlayer().getMap();
        var monsters = map.getMonsters();
        for (var i = 0; i < monsters.size(); i++) {
            var mob = monsters.get(i);
            // Check for any Zakum body or arm (IDs 8800000 - 8800010)
            if (mob.getId() >= 8800000 && mob.getId() <= 8800010) {
                zakumAlive = true;
                break;
            }
        }

        if (zakumAlive) {
            cm.sendOk("You cannot reset reactors while Zakum or his arms are still alive.");
        } else {
            // Check if the player has the necessary item to spawn Zakum
            if (cm.haveItem(4001017, 1)) {
                cm.spawnZakum(); // Spawns Zakum
                cm.gainItem(4001017, -1); // Remove the item from the player's inventory
                cm.sendOk("Zakum has been respawned.");
            } else {
                cm.sendOk("You do not have an #v4001017#.");
            }
        }
        cm.dispose();
    }
}
