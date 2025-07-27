const ItemConstants = Java.type('constants.inventory.ItemConstants');
const InventoryType = Java.type('client.inventory.InventoryType');

var items = null;
var mode = 0; // 0: Main menu, 1: Category select, 2: Item select
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
        case 0:
            if (selection === 0) {
                mode = 1;
                showCategoryMenu();
            } else if (selection === 1) {
                cm.sendOk("Use this assistant to lock or unlock items so they won’t be sold accidentally with @sellall.");
                cm.dispose();
            } else {
                cm.dispose();
            }
            break;

        case 1:
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

        case 2:
            selectedIndex = selection;

            if (selectedIndex < 0 || selectedIndex >= items.length) {
                cm.sendOk("Invalid item.");
                cm.dispose();
                return;
            }

            toggleItemLock(selectedIndex);
            cm.sendOk("Item has been " + (isItemLocked(items[selectedIndex]) ? "locked." : "unlocked."));
            cm.dispose();
            break;
    }
}

function showMainMenu() {
    var text = "Hello! Welcome to Kuro's Sell Assistant! What would you like to do today?\r\n";
    text += "#L0# Lock or unlock items\r\n";
    text += "#L1# Help\r\n";
    text += "#L2# Exit\r\n";
    cm.sendSimple(text);
}

function showCategoryMenu() {
    var text = "Select the inventory category to manage:\r\n";
    text += "#L0# Equip\r\n";
    text += "#L1# Use\r\n";
    text += "#L2# Etc\r\n";
    cm.sendSimple(text);
}

function showItemList() {
    var text = "Select an item to lock or unlock:\r\n\r\n";
    for (var i = 0; i < items.length; i++) {
        var item = items[i];
        var name = Packages.server.ItemInformationProvider.getInstance().getName(item.getItemId());
        if (item != null && item.getItemId() > 0) {
            var locked = isItemLocked(item) ? "#n#r [Locked]#n#k" : "";
            text += "#L" + i + "##v" + item.getItemId() + "# x" + item.getQuantity() + name + locked + "#l\r\n";
        }
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
        default: return InventoryType.USE;
    }
}
