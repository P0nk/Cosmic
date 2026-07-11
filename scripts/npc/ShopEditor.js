/**
 * In game shop editor
 * @author Chronos
 */

let status;
let shopId;
let menu;
let shopItems;
let selectedItem;
let debug = false;
let state;

let addItem;
let addPrice;

function start() {
    if (cm.getPlayer().gmLevel() >= 3) {
        menu = undefined;
        state = undefined;
        status = 0;

        let str = `${cm.letters('Merry Christmas')}\r\n\r\nCurrent shop id: #r${shopId}#b`;
        str += `\r\n\r\n#L${MenuState.EDIT_ID}#Change shop id#l`;
        str += `\r\n#L${MenuState.ALL_SHOPS}#Browse shop ids#l`;
        if (shopId !== undefined) {
            if (cm.doesShopExist(shopId)) {
                str += `\r\n#L${MenuState.MANAGE_ITEMS}#Manage items#l`;
                str += `\r\n#L${MenuState.ADD_ITEM}#Add new item#l`;
            } else {
                str += '\r\n\r\n\r\n#r#eThis shop was not found in the database!';
                str += `\r\n#b#n#L${MenuState.ADD_SHOP}#Add this shop to the database#l`;
            }
        }

        cm.sendSimple(str);
    } else {
        cm.sendOk('yoooo chronos is a cool dude');
        cm.dispose();
    }
}

function action(m, t, s) {
    if (cm.isEndChat(m, t)) {
        cm.dispose();
        return;
    }

    status++;
    if (menu === undefined) {
        menu = s;
    }

    switch (menu) {
        case MenuState.EDIT_ID:
            changeShopIdPrompt(s);
            break;
        case MenuState.ALL_SHOPS:
            allShopsPrompt(s);
            break;
        case MenuState.MANAGE_ITEMS:
            manageItemsPrompt(m, s);
            break;
        case MenuState.ADD_ITEM:
            addItemPrompt(m, s);
            break;
        case MenuState.ADD_SHOP:
            addShopPrompt(s);
            break;
    }
}

function changeShopIdPrompt(s) {
    if (status === 1) {
        cm.sendGetNumber('Enter new shop id:', shopId ?? 0, 0, 2147483647);
    } else {
        shopId = s;
        start();
    }
}

function addShopPrompt(s) {
    if (status === 1) {
        cm.sendGetNumber('Enter NPC id:', 1, 1, 2147483647);
    } else {
        cm.addShopToDb(shopId, s);
        start();
    }
}

function allShopsPrompt(s) {
    if (status === 1) {
        const shops = cm.getAllShopIds();

        if (shops.size() === 0) {
            cm.sendOk('You do not have any shops in the database');
            cm.dispose();
            return;
        }

        let str = 'Select a shop:#b';
        for (const kv of shops.entrySet()) {
            str += `\r\n#L${kv.getKey()}#${kv.getKey()} - #p${kv.getValue()}##l`;
        }
        cm.sendSimple(str);
    } else {
        shopId = s;
        start();
    }
}

function manageItemsPrompt(m, s) {
    shopItems = cm.getShopItems(shopId);
    if (shopItems.size() === 0) {
        if (status === 1) {
            cm.sendOk('This shop has no items at all.\r\nClick OK to return to the main menu.');
        } else {
            start();
        }
        return;
    }

    if (status === 1) {
        showShopItems();
    } else if (status === 2) {
        selectedItem = shopItems.get(s);
        showManageItemOptions();
    } else if (status === 3) {
        state = s;
        if (state === ManageState.EDIT) {
            editPricePrompt();
        } else {
            removeItemPrompt();
        }
    } else if (status === 4) {
        if (state === ManageState.EDIT) {
            cm.setShopItemPrice(shopId, selectedItem, s);
            start();
        } else {
            if (m === YesNoResponse.YES) {
                cm.removeShopItem(shopId, selectedItem);
            }
            start();
        }
    }
}

function showShopItems() {
    let str = `Items for shop #r${shopId}#k:#b`;
    for (let i = 0; i < shopItems.size(); i++) {
        const item = shopItems.get(i);
        str += `\r\n#L${i}#${getItemDetails(item)}#b#l`;
    }
    cm.sendSimple(str);
}

function showManageItemOptions() {
    let str = `You have selected ${getItemDetails(selectedItem)}#b`;
    str += '\r\n#L0#Edit price#l';
    str += '\r\n#L1#Remove item#l';
    cm.sendSimple(str);
}

function editPricePrompt() {
    cm.sendGetNumber(`Choose the new price for ${getItemDetails(selectedItem)}`, selectedItem.getPrice(), 0, 2147483647);
}

function removeItemPrompt() {
    cm.sendYesNo(`Are you sure you want to remove ${getItemDetails(selectedItem)}#k from this store`);
}

function getItemDetails(item) {
    return `#${debug ? 't' : 'i'}${item.getItemId()}# #t${item.getItemId()}# #k- #d${item.getPrice()} mesos`;
}

function addItemPrompt(m, s) {
    if (status === 1) {
        if (state === AddState.CHANGE_ITEM) {
            addItem = s;
        } else if (state === AddState.CHANGE_PRICE) {
            addPrice = s;
        }
        addItemMenu();
    } else if (status === 2) {
        state = s;
        if (state === AddState.CHANGE_ITEM) {
            changeItemIdPrompt();
        } else if (state === AddState.CHANGE_PRICE) {
            changeItemPricePrompt();
        } else if (state === AddState.ADD_ITEM) {
            addItemConfirmationPrompt();
        }
    } else if (status === 3) {
        cm.addShopItem(shopId, addItem, addPrice);
        start();
    }
}

function addItemMenu() {
    let str = `Current shop: #b${shopId}`;
    str += `\r\n#kCurrent item to add: ${addItem ? `#${debug ? 't' : 'i'}${addItem}# #b#t${addItem}#` : '#rnot set yet'}`;
    str += `\r\n#kCurrent price: ${addPrice ? `#d${addPrice} mesos` : '#rnot set yet'}`;
    str += '\r\n#b#L0#Change item id#l';
    str += '\r\n#L1#Change price#l';
    if (addItem && addPrice) {
        str += '\r\n#e#g#L2#Add item#l';
    }
    cm.sendSimple(str);
}

function changeItemIdPrompt() {
    cm.sendGetNumber('Change the item id:', addItem ?? 4000000, 0, 2147483647);
    status = 0;
}

function changeItemPricePrompt() {
    cm.sendGetNumber('Change the item price:', addPrice ?? 1, 1, 2147483647);
    status = 0;
}

function addItemConfirmationPrompt() {
    cm.sendYesNo(`Are you sure you want to add #${debug ? 't' : 'i'}${addItem}# #b#t${addItem}# #kfor\r\n#d${addPrice} mesos?`);
}

const ManageState = {
    EDIT: 0,
    REMOVE: 1
};

const MenuState = {
    EDIT_ID: 0,
    ALL_SHOPS: 1,
    MANAGE_ITEMS: 2,
    ADD_ITEM: 3,
    ADD_SHOP: 4
}

const YesNoResponse = {
    NO: 0,
    YES: 1
}

const AddState = {
    CHANGE_ITEM: 0,
    CHANGE_PRICE: 1,
    ADD_ITEM: 2,
}