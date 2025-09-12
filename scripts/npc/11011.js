/* NPC: Make EQUIP account-shareable (once)
 * Uses cm.setSharingFlag(Equip) from NPCConversationManager.java
 */

var status = 0, candidates = [], pickSlot = -1;
var nxCost = 50000
var rbLevel = -1;
function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    if (status === 0) {
        var inv   = cm.getInventory(1); // EQUIP
        var limit = inv.getSlotLimit();
        var IIP   = Packages.server.ItemInformationProvider.getInstance();
        candidates = [];

        for (var s = 1; s <= limit; s++) {
            var it = inv.getItem(s);
            if (!it) continue;
            var f = it.getFlag();

            var shared  = (f & Packages.constants.inventory.ItemConstants.KARMA_EQP)
                          === Packages.constants.inventory.ItemConstants.KARMA_EQP;
            if (!it.isUntradeable() || shared) continue;

            var id = it.getItemId(), name = IIP.getName(id) || (""+id);
            candidates.push([s, id, name]);
        }

        if (!candidates.length) {
            cm.sendOk("No eligible untradeable equips found.");
            return cm.dispose();
        }

        var txt = "Pick an untradeable equip to make #bTradeable within Account (once)#k.\r\n\r\n";
        for (var i = 0; i < candidates.length; i++) {
            var c = candidates[i];
            txt += "#L" + c[0] + "##v" + c[1] + "# " + c[2] + " (Slot " + c[0] + ")#l\r\n";
        }
        cm.sendSimple(txt);
    }

    else if (status === 1) {
        pickSlot = selection;
        if (pickSlot <= 0) { cm.sendOk("Cancelled."); return cm.dispose(); }
        var it = cm.getInventory(1).getItem(pickSlot);
        rbLevel = it.getHands()
        nxCost = (50_000 * (rbLevel + 1))
        if (!it) { cm.sendOk("Couldn’t find that item."); return cm.dispose(); }
        cm.sendYesNo("Convert #i"+it.getItemId()+"##z"+it.getItemId()+"# to #bTradeable (once)#k? It will cost " + (50 * (rbLevel + 1)) + "k nx");
    }

    else if (status === 2) {
        console.log(nxCost)
        if (cm.getCashShop().getCash(1) < nxCost) {
            cm.sendOk("You do not have enough NX!");
            return cm.dispose();
        }
        cm.gainCash(-nxCost);
        var it = cm.getInventory(1).getItem(pickSlot);
        if (!it) { cm.sendOk("Item missing."); return cm.dispose(); }

        it = cm.setSharingFlag(it);

        cm.sendOk("Done! #i"+it.getItemId()+"##z"+it.getItemId()+"# is now #bTradeable within Account (once)#k.");
        cm.dispose();
    }
}
