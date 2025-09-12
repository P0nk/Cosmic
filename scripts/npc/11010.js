// Friendly ore name mapping
var oreNames = {
    4010000: "Bronze Ore",
    4010001: "Steel Ore",
    4010002: "Mithril Ore",
    4010003: "Adamantium Ore",
    4010004: "Silver Ore",
    4010005: "Orihalcon Ore",
    4010006: "Gold Ore",
    4010007: "Lidium Ore",
    4020000: "Garnet Ore",
    4020001: "Amethyst Ore",
    4020002: "Aquamarine Ore",
    4020003: "Emerald Ore",
    4020004: "Opal Ore",
    4020005: "Sapphire Ore",
    4020006: "Topaz Ore",
    4020007: "Diamond Ore",
    4020008: "Black Crystal Ore",
    4004000: "Power Crystal Ore",
    4004001: "Wisdom Crystal Ore",
    4004002: "DEX Crystal Ore",
    4004003: "LUK Crystal Ore",
    4004004: "Dark Crystal Ore",

    4011000: "Bronze",
    4011001: "Steel",
    4011002: "Mithril",
    4011003: "Adamantium",
    4011004: "Silver",
    4011005: "Orihalcon",
    4011006: "Gold",
    4011007: "Lidium",
    4021000: "Garnet",
    4021001: "Amethyst",
    4021002: "Aquamarine",
    4021003: "Emerald",
    4021004: "Opal",
    4021005: "Sapphire",
    4021006: "Topaz",
    4021007: "Diamond",
    4021008: "Black Crystal",
    4005000: "Power Crystal",
    4005001: "Wisdom Crystal",
    4005002: "DEX Crystal",
    4005003: "LUK Crystal",
    4005004: "Dark Crystal",

    // Magic Powders
    4007000: "Brown Magic Powder",
    4007001: "White Magic Powder",
    4007002: "Blue Magic Powder",
    4007003: "Green Magic Powder",
    4007004: "Yellow Magic Powder",
    4007005: "Purple Magic Powder",
    4007006: "Red Magic Powder",
    4007007: "Black Magic Powder",

    // Stimulators
    4130000: "Gloves Production Stimulator",
    4130001: "Shoes Production Stimulator",
    4130002: "One-Handed Sword Forging Stimulator",
    4130003: "One-Handed Axe Forging Stimulator",
    4130004: "One-Handed Blunt Weapon Forging Stimulator",
    4130005: "Two-Handed Sword Forging Stimulator",
    4130006: "Two-Handed Axe Forging Stimulator",
    4130007: "Two-Handed Mace Forging Stimulator",
    4130008: "Spear Forging Stimulator",
    4130009: "Pole Arm Forging Stimulator",
    4130010: "Wand Production Stimulator",
    4130011: "Staff Production Stimulator",
    4130012: "Bow Production Stimulator",
    4130013: "Crossbow Production Stimulator",
    4130014: "Dagger Forging Stimulator",
    4130015: "Claw Production Stimulator",
    4130016: "Knuckler Production Stimulator",
    4130017: "Gun Production Stimulator",
    4130018: "Armor Production Stimulator",
    4130019: "Topwear Production Stimulator",
    4130020: "Bottomwear Production Stimulator",
    4130021: "Overall Production Stimulator",
    4130022: "Shield Production Stimulator",
    4130023: "Katara Forging Stimulator"
};

// All ore item IDs
var ores = Object.keys(oreNames).map(function(id) {
    return parseInt(id);
});

// Java short conversion
var JavaShort = Java.type("java.lang.Short");

var status = 0;
var selectedIndex = -1;
var withdrawAmount = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    java.lang.System.out.println("[OrePouch] status = " + status + ", selection = " + selection);

    if (mode !== 1) {
        cm.dispose();
        return;
    }

    status++;

    // Menu
    if (status === 0) {
        cm.sendSimple("Helloo I'm Menma! I can help hold on to your ores/powders and stimulators! Would you like my help?\r\n#L0#Deposit all items from inventory\r\n#L1#Withdraw items from pouch");
    }

    // Deposit
    else if (status === 1 && selection === 0) {
        var deposited = 0;
        var skipped = [];
        var pouch = cm.getPlayer().getOrePouch();

        for (var i = 0; i < ores.length; i++) {
            var id = ores[i];
            var inventoryQty = cm.getPlayer().getItemQuantity(id, false);
            if (inventoryQty <= 0) continue;

            var pouchQty = 0;
            for (var j = 0; j < pouch.size(); j++) {
                if (pouch.get(j).getItemId() === id) {
                    pouchQty = pouch.get(j).getQuantity();
                    break;
                }
            }

            var maxAllowed = 32767 - pouchQty;
            if (maxAllowed <= 0) {
                skipped.push(oreNames[id] || ("Ore (" + id + ")"));
                java.lang.System.out.println("[OrePouch] Skipped " + id + ": pouch full");
                continue;
            }

            var toDeposit = Math.min(inventoryQty, maxAllowed);
            cm.gainItem(id, JavaShort.valueOf(-toDeposit));
            cm.getPlayer().addOreToPouch(id, toDeposit);
            java.lang.System.out.println("[OrePouch] Deposited " + id + " x" + toDeposit);
            deposited += toDeposit;

            if (toDeposit < inventoryQty) {
                skipped.push((oreNames[id] || ("Ore (" + id + ")")) + " (partially deposited)");
                java.lang.System.out.println("[OrePouch] Partially deposited " + id + ": " + toDeposit + " of " + inventoryQty);
            }
        }

        var msg = "Deposited " + deposited + " items into your pouch.";
        if (skipped.length > 0) {
            msg += "\r\nCannot store more of: " + skipped.join(", ") + " (limit 32,767 reached)";
        }

        cm.sendOk(msg);
        cm.dispose();
    }

    // Withdraw menu
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
            var itemId = item.getItemId();
            var name = Packages.server.ItemInformationProvider.getInstance().getName(item.getItemId());
            text += "#L" + i + "##v" + itemId + "# "+ name +" (" + item.getQuantity() + ")\r\n";
        }

        cm.sendSimple(text);
    }

    // Quantity selection
    else if (status === 2) {
        var pouch = cm.getPlayer().getOrePouch();

        if (selection < 0 || selection >= pouch.size()) {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        }

        var item = pouch.get(selection);
        if (item == null) {
            cm.sendOk("This item no longer exists in your pouch.");
            cm.dispose();
            return;
        }

        selectedIndex = selection;
        var itemId = item.getItemId();
        var quantity = item.getQuantity();
        var name = oreNames[itemId] || ("Ore (" + itemId + ")");

        // Default to max amount
        cm.sendGetNumber("How many #b" + name + "#k would you like to withdraw?\r\n(Max: " + quantity + ")", quantity, 1, quantity);
    }

    // Perform withdrawal
    else if (status === 3) {
        withdrawAmount = selection;

        var pouch = cm.getPlayer().getOrePouch();
        var item = pouch.get(selectedIndex);

        if (item == null) {
            cm.sendOk("This item no longer exists in your pouch.");
            cm.dispose();
            return;
        }

        var itemId = item.getItemId();
        var quantity = item.getQuantity();
        var name = oreNames[itemId] || ("Ore (" + itemId + ")");

        if (withdrawAmount <= 0 || withdrawAmount > quantity) {
            cm.sendOk("Invalid quantity.");
            cm.dispose();
            return;
        }

        if (cm.canHold(itemId, withdrawAmount)) {
            cm.gainItem(itemId, withdrawAmount);
            java.lang.System.out.println("[OrePouch] Withdrawing: " + itemId + " x" + withdrawAmount);

            if (withdrawAmount === quantity) {
                cm.getPlayer().removeOreFromPouch(itemId);
            } else {
                item.setQuantity(quantity - withdrawAmount);
                Packages.server.inventory.OrePouchManager.saveOrePouchItems(cm.getPlayer().getId(), pouch);
            }

            cm.sendOk("Withdrawn " + withdrawAmount + " successfully.");
        } else {
            cm.sendOk("Not enough space in your inventory.");
        }

        cm.dispose();
    }
}
