
let enabled = true;
let status = 0;
let slot;
let item;
let playermesos = 0;
let sfmesocost = 0;
let spelltracecost = 0;
let currentStar = 0;
let newStar = 0;
let restart;

function start() {
    status = 0;
    restart = false;
    if (enabled) {
        if(cm.getStarForceEquipSlot() == "") {
            cm.sendOk("You don't have any items that can be starforced.")
            cm.dispose()
        } else {
            cm.sendSimple("Which item would you like to enhance?\r\n" + cm.getStarForceEquipSlot());
        }
    } else {
        cm.sendOk("Disabled for now");
        cm.dispose();
    }
}

function action(m, t, s) {
    if (m !== 1) {
        cm.dispose();
    } else {
        status++;
        if (status === 1) {
            slot = slot || s;
            item = cm.getSFItem(slot);
            if (item.getUpgradeSlots() === 0) {
                cm.sendOk("This item cannot be enhanced any further.");
                cm.dispose();
                return;
            }
            if(item.getItemId())
            playermesos = cm.getMeso();
            sfmesocost = cm.getMesoStarforceCost(item.getLevel(), cm.getReqLevel(item.getItemId()), cm.isSuperior(item.getItemId()));
            spelltracecost = cm.getSpellTraceCost(item.getLevel(), cm.getReqLevel(item.getItemId()), cm.isSuperior(item.getItemId()));

            var message = "Attempting to enhance #i" + item.getItemId() + "# from level " + item.getLevel() + " to " + (item.getLevel() + 1) + " will cost you:\r\n"
            message += "#fUI/UIWindow.img/Shop/meso# " + cm.numberWithCommas(sfmesocost) + "\r\n" + "#fUI/UIWindow.img/Shop/trace# " + spelltracecost + "\r\n\r\n"
            message += "\t\t\t\t\t\t\t\t\tSTR: " + cm.itemToEquip(item).getStr() + " -> " + (cm.itemToEquip(item).getStr() + cm.getSFGain(item.getItemId(), item.getLevel(), cm.isSuperior(item.getItemId()), "str")) + "\r\n"
            message += "\t\t\t\t\t\t\t\t\tDEX: " + cm.itemToEquip(item).getDex() + " -> " + (cm.itemToEquip(item).getDex() + cm.getSFGain(item.getItemId(), item.getLevel(), cm.isSuperior(item.getItemId()), "dex")) + "\r\n"
            message += "\t\t\t\t\t\t\t\t\tINT: " + cm.itemToEquip(item).getInt() + " -> " + (cm.itemToEquip(item).getInt() + cm.getSFGain(item.getItemId(), item.getLevel(), cm.isSuperior(item.getItemId()), "int")) + "\r\n"
            message += "\t\t\t\t\t\t\t\t\tLUK: " + cm.itemToEquip(item).getLuk() + " -> " + (cm.itemToEquip(item).getLuk() + cm.getSFGain(item.getItemId(), item.getLevel(), cm.isSuperior(item.getItemId()), "luk")) + "\r\n"

            if(cm.itemToEquip(item).getWatk() != 0 && cm.getSFGain(item.getItemId(), item.getLevel(), cm.isSuperior(item.getItemId()), "watk") != 0) {
                message += "\t\t\t\t\t\t\t\t\tWATK: " + cm.itemToEquip(item).getWatk() + " -> " + (cm.itemToEquip(item).getWatk() + cm.getSFGain(item.getItemId(), item.getLevel(), cm.isSuperior(item.getItemId()), "watk")) + "\r\n"
            }
            if(cm.itemToEquip(item).getMatk() != 0 && cm.getSFGain(item.getItemId(), item.getLevel(), cm.isSuperior(item.getItemId()), "matk") != 0) {
                message += "\t\t\t\t\t\t\t\t\tMATK: " + cm.itemToEquip(item).getMatk() + " -> " + (cm.itemToEquip(item).getMatk() + cm.getSFGain(item.getItemId(), item.getLevel(), cm.isSuperior(item.getItemId()), "matk"))
            }
            message += "\r\n\r\nThe success rate is: #e#b" + cm.getProp(cm.getStarForceEquip(slot)) + "#k#n%. Is that okay?"

            currentStar = item.getLevel();
            cm.sendYesNo(message);
        } else if (status === 2) {
            if(playermesos < sfmesocost) {
                cm.sendOk("You don't possess enough mesos.");
                cm.dispose();
            } else if (!cm.haveItem(4000999, spelltracecost)) {
                cm.sendOk("You don't possess enough spell traces");
                cm.dispose();
            } else {
                cm.gainMeso(-sfmesocost);
                cm.gainItem(4000999, -spelltracecost);
                cm.StarForceEquip(cm.getSFItem(slot));
                newStar = cm.getSFItem(slot).getLevel();
                cm.sendOk('Complete');
                cm.sendMessageToPlayer("Current starforce level of item: " + newStar);
                status = 0;
            }
        }
    }
}