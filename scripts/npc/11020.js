/* 11020.js — Sunny Shop
 * Custom Creator Shop (sells Sunny’s cosmetic items)
 * Logs all transactions through CreatorShopManager
 * Adds auto-convert from B-Coin (1 b meso) when mesos are insufficient.
 */

const CreatorShopManager = Packages.server.creator_shop.CreatorShopManager;

// Configuration
const SHOP_NPC_ID = 11020;
const CURRENCY = "MESO";
const CREATOR_SHARE = 0.30;          // Reference only; claim NPC handles payout
const BCOIN_ID = 4031997;            // 1 b meso coin

// Hard-coded items
const ITEMS = [
    { id: 1103158, name: "[Sunny] Brown Plaid Pashmina",   price: 2000000000 },
    { id: 1103159, name: "[Sunny] Green Plaid Pashmina",   price: 2000000000 },
    { id: 1103160, name: "[Sunny] Pink Plaid Pashmina",    price: 2000000000 },
    { id: 1103161, name: "[Sunny] Purple Plaid Pashmina",  price: 2000000000 },
    { id: 1103162, name: "[Sunny] Red Plaid Pashmina",     price: 2000000000 },
    { id: 1103163, name: "[Sunny] Fuschia Plaid Pashmina", price: 2000000000 },
    { id: 1103164, name: "[Sunny] Ivory Plaid Pashmina",   price: 2000000000 },
    { id: 1103165, name: "[Sunny] Black Plaid Pashmina",   price: 2000000000 },
    { id: 1103166, name: "[Sunny] White Plaid Pashmina",   price: 2000000000 },
    { id: 1103167, name: "[Sunny] Grey Plaid Pashmina",    price: 2000000000 },
    { id: 1108000, name: "[Sunny] Gold Plaid Pashmina",    price: 2000000000 },
    { id: 1108001, name: "[Sunny] Lime Green Plaid Pashmina", price: 2000000000 },
    { id: 1053638, name: "[Sunny] Crimson BTS Love Swan",  price: 2000000000 },
    { id: 1053639, name: "[Sunny] Blackpink BTS Love Swan", price: 2000000000 }
];

// Long-safe number formatter
function formatNumber(num) {
    return java.text.NumberFormat.getInstance().format(num);
}

var status = 0;

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

    // Step 1 — list items
    if (status == 1) {
        var text = "#eWelcome to Sunny's Custom Shop!#n\r\n\r\n";
        for (var i = 0; i < ITEMS.length; i++) {
            text += "#L" + i + "##i" + ITEMS[i].id + "# #b" + ITEMS[i].name +
                    "#k - " + formatNumber(ITEMS[i].price) + " " + CURRENCY + "#l\r\n";
        }
        cm.sendSimple(text);
    }

    // Step 2 — handle purchase
    else if (status == 2) {
        var item = ITEMS[selection];
        if (!item) {
            cm.sendOk("Invalid selection.");
            return cm.dispose();
        }

        var price = item.price;
        var player = cm.getPlayer();

        // Auto-convert 1 b meso coins if not enough mesos
        while (cm.getMeso() < price && cm.haveItem(BCOIN_ID, 1)) {
            cm.gainItem(BCOIN_ID, -1);
            cm.gainMeso(1_000_000_000);
        }

        // Check again after conversion
        if (cm.getMeso() < price) {
            cm.sendOk(
                "You don't have enough mesos to purchase this item.\r\n" +
                "Try bringing more mesos or B-Coins (#v" + BCOIN_ID + "#)."
            );
            return cm.dispose();
        }

        // Deduct mesos
        cm.gainMeso(-price);

        // Give item
        cm.gainItem(item.id, 1);

        // Log transaction (still uses unified MESO currency)
        CreatorShopManager.logTransaction(SHOP_NPC_ID, player, item.id, price, CURRENCY);

        cm.sendOk("Thank you for supporting Sunny's creations!\r\nEnjoy your #b" + item.name + "#k!");
        cm.dispose();
    }
}
