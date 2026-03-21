/**
 * ADMIN NPC
 * Purin, triggers search query
 * @author Chronos
 */

function start() {
    if (cm.getPlayer().getDataSearch() !== null) {
        cm.sendSimple(cm.getPlayer().getDataSearch());
    } else {
        cm.sendOk("Hello.");
        cm.dispose();
    }
}

function action(m, t, s) {
    if (m === 1) {
        let object = cm.getPlayer().getDataSearchArr().get(s);
        let type = cm.getPlayer().getDataSearchType();
        if (type === "item") {
            cm.gainItem(object);
            cm.sendOk("You got a #b#t" + object + "#");
        } else if (type === "mob") {
            cm.summonMob(object);
        } else if (type === "npc") {
            cm.makeNpc(object);
        }
    }
    cm.dispose();
}
