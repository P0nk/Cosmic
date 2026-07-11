var map = 677000004;
var quest = 3170;
var status = -1;

function start(mode, type, selection) {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else {
        cm.dispose();
        return;
    }
    if (status == 0) {
        if (cm.isQuestCompleted(quest)) {
            cm.sendYesNo("Seems like you are worthy of fighting my Master, would you like to purchase your way into the Audience Room?, at the cost of 1,000,000 Mesos.");
        } else {
            cm.sendOk("You must complete the quest The Knight's Magic Scroll first.");
            cm.dispose();
        }
    } else if (status == 1) {
        var itemId = 2030021;  // ID of the item to be purchased
        var price = 1000000;  // Price of the item in mesos
        if (cm.getMeso() >= price) {
            if (cm.canHold(itemId)) {
                cm.gainMeso(-price);
                cm.gainItem(itemId, 1);
                cm.sendOk("Thank you for your purchase!");
            } else {
                cm.sendOk("Please make sure you have enough space in your inventory.");
            }
        } else {
            cm.sendOk("You don't have enough mesos.");
        }
        cm.dispose();
    }
}