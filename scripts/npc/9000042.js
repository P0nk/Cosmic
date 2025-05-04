
/* Subordinate
        Free Market (9201611)

        Enhancing NPC:
*/

var selectedItem;
var newStats = {};
var itemInfo = Packages.server.ItemInformationProvider.getInstance(); // Load item names

function start() {
    status = 0;
    cm.sendSimple("Hey, selling goods?\r\n#b#L0#Yes, I have many items to sell!#l");
}

function action(mode, type, selection) {
    if (mode === 1) {
        status++;
    } else {
        cm.dispose();
        return;
    }

    if (status === 1) {
        var inventory = cm.getInventory(1); // Get equip inventory
        var itemList = "";
        var items = [];

        // list equip in inventory
        for (var i = 0; i <= inventory.getSlotLimit(); i++) {
            var item = inventory.getItem(i);
            if (item !== null) {
                // items.push(item);
                // itemList += "#L" + i + "#" + item + "#l\r\n";
                // var itemName = itemInfo.getName(item.getItemId());
                var itemName = "#t" + item.getItemId() + "#";
                var itemIcon = "#v" + item.getItemId() + "#"; // Displays item icon

                var itemSlot = item.getPosition();

                itemList += "#L" + i + "#" + itemIcon + " " + itemName + " (Slot: " + itemSlot + ")#l\r\n";
            }
        }

        if (itemList === "") {
            cm.sendOk("You have no items to sell.");
            cm.dispose();
        } else {
            cm.sendSimple("Select the starting item you want to sell:\r\n" + itemList);
        }
    } else if (status === 2) {
        selectedItem = cm.getInventory(1).getItem(selection);
        if (selectedItem == null) {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        } else {
            console.log(selectedItem.getPosition())
            mesosgain = cm.getPlayer().sellAllItemsFromSlot(1, selectedItem.getPosition()); // 1 - Equip Inventory
            cm.sendOk("Transaction complete! You received #r" + mesosgain + " mesos#k from this action.");
//            cm.getPlayer().dropMessage(6, "Transaction complete! You received " + mesosgain.toLocaleString("en-US") + " from this action.");
            cm.dispose();
            return;
        }

    }
}
