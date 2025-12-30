/* 11020.js — Sunny Shop
 * Custom Creator Shop selling items for B-Coins (#v3020002#)
 * Logs all transactions through CreatorShopManager
 */

const CreatorShopManager = Packages.server.creator_shop.CreatorShopManager;
const SHOP_NPC_ID = 11020;
const BCOIN_ID = 3020002;
const CURRENCY = "BCOIN";

// Items priced in B-Coins

const ITEMS = [
    { id: 1103158, name: "[Sunny] Brown Plaid Pashmina",   price: 1 },
    { id: 1103159, name: "[Sunny] Green Plaid Pashmina",   price: 1 },
    { id: 1103160, name: "[Sunny] Pink Plaid Pashmina",    price: 1 },
    { id: 1103161, name: "[Sunny] Purple Plaid Pashmina",  price: 1 },
    { id: 1103162, name: "[Sunny] Red Plaid Pashmina",     price: 1 },
    { id: 1103163, name: "[Sunny] Fuschia Plaid Pashmina", price: 1 },
    { id: 1103164, name: "[Sunny] Ivory Plaid Pashmina",   price: 1 },
    { id: 1103165, name: "[Sunny] Black Plaid Pashmina",   price: 1 },
    { id: 1103166, name: "[Sunny] White Plaid Pashmina",   price: 1 },
    { id: 1103167, name: "[Sunny] Grey Plaid Pashmina",    price: 1 },
    { id: 1108000, name: "[Sunny] Gold Plaid Pashmina",    price: 1 },
    { id: 1108001, name: "[Sunny] Lime Green Plaid Pashmina", price: 1 },
    { id: 1053638, name: "[Sunny] Crimson BTS Love Swan",  price: 1 },
    { id: 1053639, name: "[Sunny] Blackpink BTS Love Swan", price: 1 }
];

function plural(n){ return n + " BCoin" + (n>1?"s":""); }

function start() {
    var t="#eWelcome to Sunny's Custom Shop!#n\r\nAll prices are in #v"+BCOIN_ID+"#.\r\n\r\n";
    for (var i=0;i<ITEMS.length;i++)
        t+="#L"+i+"##i"+ITEMS[i].id+"# #b"+ITEMS[i].name+
           "#k - "+plural(ITEMS[i].price)+"#l\r\n";
    cm.sendSimple(t);
}

function action(mode,type,sel){
    if(mode!=1)return cm.dispose();
    var it=ITEMS[sel]; if(!it)return cm.dispose();
    var have=cm.itemQuantity(BCOIN_ID);
    if(have<it.price){
        cm.sendOk("You need "+plural(it.price)+". You only have #b"+have+"#k.");
        return cm.dispose();
    }
    cm.gainItem(BCOIN_ID,-it.price);
    cm.gainItem(it.id,1);
    CreatorShopManager.logTransaction(SHOP_NPC_ID,cm.getPlayer(),it.id,it.price,CURRENCY);
    cm.sendOk("Thank you for supporting Sunny!\r\nEnjoy your #r#e"+it.name+"#k!");
    cm.dispose();
}
