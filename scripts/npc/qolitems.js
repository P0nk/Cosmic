var status = -1;
var item1 = 5130000; // Replace with actual item ID
var item2 = 5520000; // Replace with actual item ID
var item3 = 5510000; // Item ID for 10 million meso cost
var item4 = 5041000; // Item ID for 2 million meso cost
var currency = 4001126; // Currency item ID
var costItem1 = 100; // Cost per item for item1 and item2
var costItem3 = 10000000; // 10 million meso cost
var costItem4 = 2000000; // 2 million meso cost

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0) {
            var message = "Hello! I can sell you some special items. What would you like to buy?\r\n\r\n";
            message += "#L0# #i" + item1 + ":# for " + costItem1 + " #i" + currency + ":# each#l\r\n";
            message += "#L1# #i" + item2 + ":# for " + costItem1 + " #i" + currency + ":# each#l\r\n";
            message += "#L2# #i" + item3 + ":# for 10m meso each?#l\r\n";
            message += "#L3# #i" + item4 + ":# for 2m  meso each?#l\r\n";
            cm.sendSimple(message);
        } else if (status == 1) {
            if (selection == 0 || selection == 1) {
                var item = selection == 0 ? item1 : item2;
                cm.sendGetNumber("How many #i" + item + ":# would you like to buy?", 1, 1, 100);
                status = 10 + selection;
            } else if (selection == 2 || selection == 3) {
                var item = selection == 2 ? item3 : item4;
                var cost = selection == 2 ? costItem3 : costItem4;
                cm.sendGetNumber("How many #i" + item + ":# would you like to buy?", 1, 1, 100);
                status = 20 + selection - 2;
            }
        } else if (status == 11 || status == 12) {
            var item = status == 11 ? item1 : item2;
            var quantity = selection;
            var totalCost = quantity * costItem1;
            if (cm.haveItem(currency, totalCost)) {
                cm.gainItem(item, quantity);
                cm.gainItem(currency, -totalCost);
                cm.sendOk("Thank you for your purchase of " + quantity + " #i" + item + ":#.");
            } else {
                cm.sendOk("You do not have enough #i" + currency + ":#.");
            }
            cm.dispose();
        } else if (status == 21 || status == 22) {
            var item = status == 21 ? item3 : item4;
            var cost = status == 21 ? costItem3 : costItem4;
            var quantity = selection;
            var totalCost = quantity * cost;
            if (cm.getMeso() >= totalCost) {
                cm.gainItem(item, quantity);
                cm.gainMeso(-totalCost);
                cm.sendOk("Thank you for your purchase of " + quantity + " #i" + item + ":#.");
            } else {
                cm.sendOk("You do not have enough meso.");
            }
            cm.dispose();
        }
    }
}
