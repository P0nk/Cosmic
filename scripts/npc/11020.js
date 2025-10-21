/* 11020.js — Sunny Shop
 * Custom Creator Shop (sells Sunny’s cosmetic items)
 * Logs all transactions through CreatorShopManager
 */

const CreatorShopManager = Packages.server.creator_shop.CreatorShopManager;

var status = 0;

// Configuration
const SHOP_NPC_ID = 11020;
const CURRENCY = "MESO";
const CREATOR_SHARE = 0.30; // reference only, claim NPC handles reward split

// Hardcoded items for sale
const ITEMS = [
    { id: 1003175, name: "Raven Horn Chaser Hat",  price: 2000000000 },
    { id: 1052314, name: "Raven Horn Chaser Coat", price: 60000000 },
    { id: 1072486, name: "Raven Horn Chaser Boots", price: 40000000 }
];

function start() {
    status = 0;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode != 1) {
        cm.dispose();
        return;
    }

    status++;
    if (status == 1) {
        var text = "#eWelcome to Sunny's Custom Shop!#n\r\n\r\n";
        for (var i = 0; i < ITEMS.length; i++) {
            text += "#L" + i + "##i" + ITEMS[i].id + "# #b" + ITEMS[i].name +
                    "#k - " + cm.numberWithCommas(ITEMS[i].price) + " " + CURRENCY + "#l\r\n";
        }
        cm.sendSimple(text);
    } else if (status == 2) {
        var item = ITEMS[selection];
        if (!item) {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        }

        // Check funds
        if (!CreatorShopManager.hasEnoughCurrency(cm.getPlayer(), item.price, CURRENCY)) {
            cm.sendOk("You don't have enough " + CURRENCY + " to purchase this item.");
            cm.dispose();
            return;
        }

        // Deduct currency + give item
        CreatorShopManager.deductCurrency(cm.getPlayer(), item.price, CURRENCY);
        cm.gainItem(item.id, 1);

        // Log transaction
        CreatorShopManager.logTransaction(SHOP_NPC_ID, cm.getPlayer(), item.id, item.price, CURRENCY);

        cm.sendOk("Thank you for supporting Sunny's creations!\r\nEnjoy your #b" + item.name + "#k!");
        cm.dispose();
    }
}
