/**
 * ADMIN NPC
 * Pason, shows stuff going on in the map you are at.
 * Maple Art Online
 * @author Chronos
 */

let status = 0;
let map; // MapleMap
let type;

/* COLLECTIONS */
let npcs; // MapleNPCs in MapleMap
let monsters; // MapleMonsters in MapleMap
let items; // MapleMapItems in MapleMap
let characters; // MapleCharacters in MapleMap
let portals; // MaplePortals in MapleMap

function start() {
    map = cm.getPlayer().getMap();
    initializeObjects();

    let str = "The Universe is under no obligation to make sense to you.#b";
    //if (!npcs.isEmpty())
   //     str += "\r\n#L0#NPCs#l";
    if (!monsters.isEmpty()) {
        str += "\r\n#L1#Monsters#l";
        cm.sendSimple(str);
        } else {
        cm.sendOk("Nothing has been sensed in your surroundings...");
        cm.dispose();
        }
   // if (!items.isEmpty())
   //     str += "\r\n#L2#Dropped items#l";
   // if (!characters.isEmpty())
   //     str += "\r\n#L3#Players#l";
   // if (!portals.isEmpty())
  //      str += "\r\n#L4#Portals#l";

}

function action(m, t, s) {
    if (m === -1) {
        cm.dispose();
        return;
    }
    status++;
    if (status === 1) {
        type = s;
        let str = "Look up at the stars and not down at your feet. Try to make sense of what you see, and wonder about what makes the universe exist. Be curious.";
        switch (type) {
            case 0:
                // npcs
                break;
            case 1:
                str += listMonsters();
                break;
        }
        cm.sendSimple(str);
    } else if (status === 2) {
        switch (type) {
            case 1: // mobs
                let monster = monsters.get(s).getId();
                if (monster !== null) {
                    cm.getPlayer().setPhilID(monsters.get(s).getId());
                    cm.getPlayer().openNpcIn("mobdrop", 200);
                }
                cm.dispose();
                break;
        }
    }
}

function listMonsters() {
    let str = "#b";
    for (let i = 0; i < monsters.length; i++) {
        let monster = monsters.get(i);
        str += "\r\n#L" + i + "#" + monster.getName() + " - " + monster.getId();
        str += "- " + monster.getHp() + "/" + monster.getMaxHp() + " hp";
        str += "#l";
    }
    return str;
}

function initializeObjects() {
    let objects = map.getMapObjects();
    npcs = cm.getNPCs(objects);
    monsters = cm.getMonsters(objects);
    items = cm.getMapItems(objects);
    characters = cm.getCharacters(map);
    portals = cm.getPortals(map);
}