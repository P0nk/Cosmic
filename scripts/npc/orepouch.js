// All supported ore IDs
var ores = [
    4004000, 4004001, 4004002, 4004003, 4004004, // crystals
    4010000, 4010001, 4010002, 4010003, 4010004, 4010005, 4010006, 4010007, // mineral ores
    4020000, 4020001, 4020002, 4020003, 4020004, 4020005, 4020006, 4020007, 4020008 // jewel ores
];

var status = 0;
var selectedIndex = -1;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }

    status++;

    if (status === 0) {
        cm.sendSimple("What would you like to do? (Item is not sharable across characters)\r\n#L0#Deposit all ores from inventory\r\n#L1#Withdraw ores from pouch");
    }

    // Deposit logic
    else if (status === 1 && selection === 0) {
        var deposited = 0;
        var skipped = [];

        for (var i = 0; i < ores.length; i++) {
            var id = ores[i];
            var qty = cm.getPlayer().getItemQuantity(id, false);
            if (qty > 0) {
                var success = cm.getPlayer().addOreToPouch(id, qty);
                if (success) {
                    cm.removeAll(i  d);
                    deposited += qty;
                } else {
                    skipped.push(Packages.server.MapleItemInformationProvider.getInstance().getName(id));
                }
            }
        }

        var msg = "Deposited " + deposited + " ores into your pouch.";
        if (skipped.length > 0) {
            msg += "\r\nCannot store more of: " + skipped.join(", ") + " (limit 32,767 reached)";
        }
        cm.sendOk(msg);
        cm.dispose();
    }

    // Withdraw logic - display list
    else if (status === 1 && selection === 1) {
        var pouch = cm.getPlayer().getOrePouch();
        if (pouch.size() === 0) {
            cm.sendOk("Your Ore Pouch is empty.");
            cm.dispose();
            return;
        }

        var text = "Select the ore you want to withdraw:\r\n";
        for (var i = 0; i < pouch.size(); i++) {
            var item = pouch.get(i);
            var name = Packages.server.MapleItemInformationProvider.getInstance().getName(item.getItemId());
            text += "#L" + i + "#" + name + " (x" + item.getQuantity() + ")#\r\n";
        }

        cm.sendSimple(text);
    }

    // Withdraw logic - perform withdrawal
    else if (status === 2) {
        var pouch = cm.getPlayer().getOrePouch();
        var item = pouch.get(selection);

        if (item == null) {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        }

        var itemId = item.getItemId();
        var quantity = item.getQuantity();

        if (cm.canHold(itemId, quantity)) {
            cm.gainItem(itemId, quantity);
            cm.getPlayer().removeOreFromPouch(itemId);
            cm.sendOk("Withdrawn successfully.");
        } else {
            cm.sendOk("Not enough space in your inventory.");
        }
        cm.dispose();
    }
}
