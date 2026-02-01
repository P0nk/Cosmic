const ItemConstants = Java.type('constants.inventory.ItemConstants');
const InventoryType = Java.type('client.inventory.InventoryType');

var items = null;
var mode = 0; // 0: Main menu, 1: Category select (Instance), 2: Item select (Instance), 3: ID Lock Menu, 4: Remove ID Select
var selectedCategory = null;
var selectedIndex = -1;

function start() {
    showMainMenu();
}

function action(userMode, type, selection) {
    if (userMode !== 1) {
        cm.dispose();
        return;
    }

    switch (mode) {
        case 0: // Main Menu
            if (selection === 0) {
                mode = 1;
                showCategoryMenu();
            } else if (selection === 1) {
                mode = 3; // ID Lock Menu
                showIDLockMenu();
            } else if (selection === 2) {
                cm.sendOk("Instance Lock: Locks a specific item in your inventory.\r\nID Lock: Locks ALL items with that ID (e.g. all Snail Shells).");
                cm.dispose();
            } else {
                cm.dispose();
            }
            break;

        case 1: // Category Select (Instance)
            selectedCategory = getInventoryTypeFromSelection(selection);
            items = cm.getPlayer().getInventory(selectedCategory).list().toArray();

            if (!items || items.length === 0) {
                cm.sendOk("You have no items in that category.");
                cm.dispose();
                return;
            }

            mode = 2;
            showItemList();
            break;

        case 2: // Item Select (Instance)
            selectedIndex = selection;

            if (selectedIndex < 0 || selectedIndex >= items.length) {
                cm.sendOk("Invalid item.");
                cm.dispose();
                return;
            }

            toggleItemLock(selectedIndex);
            showItemList();
            break;

        case 3: // ID Lock Menu
            if (selection === 0) {
                // View Locked IDs
                showLockedIDs();
                mode = 3; // Stay in menu (requires re-send in showLockedIDs or back button)
            } else if (selection === 1) {
                // Add ID
                cm.sendGetNumber("Enter the Item ID you want to globally lock:", 0, 0, 9999999);
                mode = 5; // Handle Add Input
            } else if (selection === 2) {
                // Remove ID
                showRemoveIDList();
                mode = 4;
            } else {
                mode = 0;
                showMainMenu();
            }
            break;

        case 4: // Remove ID Select
            var lockedIds = cm.getPlayer().getExcludedSellItems().toArray();
            if (selection >= 0 && selection < lockedIds.length) {
                var idToRemove = lockedIds[selection];
                cm.getPlayer().toggleSellExclusion(idToRemove);
                cm.sendOk("Item ID " + idToRemove + " has been unlocked.");
            }
            cm.dispose();
            break;

        case 5: // Handle Add Input
            var idToAdd = selection;
            if (idToAdd > 0) {
                if (cm.getPlayer().isExcludedFromSell(idToAdd)) {
                    cm.sendOk("Item ID " + idToAdd + " is already locked.");
                } else {
                    cm.getPlayer().toggleSellExclusion(idToAdd);
                    cm.sendOk("Item ID " + idToAdd + " has been globally locked.");
                }
            } else {
                cm.sendOk("Invalid Item ID.");
            }
            cm.dispose();
            break;
    }
}

function showMainMenu() {
    var text = "Item Lock Management\r\n";
    text += "#L0#Lock Specific Items (Instance Lock)\r\n";
    text += "#L1#Manage Global ID Locks (Prevents selling ANY item of this ID)\r\n";
    text += "#L2#Help\r\n";
    text += "#L3#Exit\r\n";
    cm.sendSimple(text);
}

function showCategoryMenu() {
    var text = "Select inventory category (Instance Lock):\r\n";
    text += "#L0#Equip\r\n";
    text += "#L1#Use\r\n";
    text += "#L2#Etc\r\n";
    cm.sendSimple(text);
}

function showItemList() {
    var text = "Select an item to lock/unlock (Instance):\r\n\r\n";
    for (var i = 0; i < items.length; i++) {
        var item = items[i];
        if (item != null && item.getItemId() > 0) {
            var locked = isItemLocked(item) ? "#r[Locked]#k" : "";
            text += "#L" + i + "#" + locked + "#v" + item.getItemId() + "##t" + item.getItemId() + "#\r\n";
        }
    }
    cm.sendSimple(text);
}

function showIDLockMenu() {
    var text = "Global ID Lock Management:\r\n";
    text += "#L0#View Locked IDs\r\n";
    text += "#L1#Add Item ID to Lock\r\n";
    text += "#L2#Remove Item ID from Lock\r\n";
    text += "#L3#Back to Main Menu\r\n";
    cm.sendSimple(text);
}

function showLockedIDs() {
    var lockedIds = cm.getPlayer().getExcludedSellItems().toArray();
    var text = "Currently Locked Item IDs:\r\n";
    if (lockedIds.length == 0) {
        text += "No items locked.\r\n";
    } else {
        for (var i = 0; i < lockedIds.length; i++) {
            text += "- " + lockedIds[i] + " (#t" + lockedIds[i] + "#)\r\n";
        }
    }
    cm.sendOk(text);
    // Note: sendOk ends conversation usually, but loop sends back to start if mode not updated?
    // Actually in standard scripts, sendOk logic expects dispose or prev. 
    // We'll dispose after viewing.
    cm.dispose();
}

function showRemoveIDList() {
    var lockedIds = cm.getPlayer().getExcludedSellItems().toArray();
    if (lockedIds.length == 0) {
        cm.sendOk("No locked IDs to remove.");
        cm.dispose();
        return;
    }
    var text = "Select ID to unlock:\r\n";
    for (var i = 0; i < lockedIds.length; i++) {
        text += "#L" + i + "#" + lockedIds[i] + " (#t" + lockedIds[i] + "#)#l\r\n";
    }
    cm.sendSimple(text);
}

function isItemLocked(item) {
    return (item.getFlag() & ItemConstants.SELLALL_PROTECTED) === ItemConstants.SELLALL_PROTECTED;
}

function toggleItemLock(index) {
    var item = items[index];
    if (isItemLocked(item)) {
        item.setFlag(item.getFlag() & ~ItemConstants.SELLALL_PROTECTED);
    } else {
        item.setFlag(item.getFlag() | ItemConstants.SELLALL_PROTECTED);
    }
}

function getInventoryTypeFromSelection(selection) {
    switch (selection) {
        case 0: return InventoryType.EQUIP;
        case 1: return InventoryType.USE;
        case 2: return InventoryType.ETC;
        default: return InventoryType.USE; // Should not happen
    }
}