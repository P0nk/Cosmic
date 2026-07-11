var status = 0;
var selectedItem;
var tradeQuantity;
var tradeItems = [
    4020007, 4020005, 4020000, 4020004, 4020001, 4020002, 4020006, 4020003, 
    4004000, 4004001, 4004003, 4004002, 4020008, 4004004, 
    4010000, 4010001, 4010002, 4010003, 4010004, 4010005, 4010006, 4010007
];

function start() {
    cm.sendSimple("Which item would you like to trade for? \r\n" + getTradeOptions());
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
    } else {
        status++;
        if (status == 1) {
            if (selection == null || typeof selection !== 'number' || selection < 0 || selection >= tradeItems.length) {
                cm.sendOk("Invalid selection. Please try again.");
                cm.dispose();
                return;
            }

            selectedItem = tradeItems[selection];
            cm.sendGetNumber("How many #v" + selectedItem + "# would you like to trade for? Each requires two ores or crystals per item.", 1, 1, 100);
        } else if (status == 2) {
            tradeQuantity = selection;

            var deductionResult = spreadDeductAcrossItems(selectedItem, tradeQuantity);

            if (deductionResult.success) {
                cm.gainItem(selectedItem, tradeQuantity);

                // Sort by amount descending
                deductionResult.items.sort((a, b) => b.amount - a.amount);

                // Format cleanly
                var usedStr = deductionResult.items.map(d => d.amount + "x #v" + d.itemId + "#").join(", ");

                cm.sendOk(
                    "Trade successful! You received " + tradeQuantity + " #v" + selectedItem + "#.\r\n\r\n" +
                    "Items used:\r\n" + usedStr
                );
            } else {
                cm.sendOk("You do not have enough materials to complete this trade.");
            }
            cm.dispose();
        }
    }
}

function getTradeOptions() {
    var options = "";
    for (var i = 0; i < tradeItems.length; i++) {
        options += "#L" + i + "# #v" + tradeItems[i] + "# #l";
        if ((i + 1) % 5 == 0) {
            options += "\r\n";
        }
    }
    return options;
}

function spreadDeductAcrossItems(excludeItem, quantity) {
    var requiredTotal = 2 * quantity;
    var available = [];

    // Step 1: Get all items the player has (excluding the one being traded for)
    for (var i = 0; i < tradeItems.length; i++) {
        var itemId = tradeItems[i];
        if (itemId === excludeItem) continue;

        var count = cm.getItemQuantity(itemId);
        if (count > 0) {
            available.push({ itemId: itemId, amount: count });
        }
    }

    // Step 2: Check if total across all items is enough
    var totalAvailable = available.reduce((sum, item) => sum + item.amount, 0);
    if (totalAvailable < requiredTotal) {
        return { success: false };
    }

    // Step 3: Distribute deduction across all available items
    var deduction = [];
    var remaining = requiredTotal;

    // First pass — calculate proportional share
    for (var i = 0; i < available.length; i++) {
        var item = available[i];
        var share = Math.floor((item.amount / totalAvailable) * requiredTotal);
        share = Math.min(share, item.amount, remaining);

        if (share > 0) {
            deduction.push({ itemId: item.itemId, amount: share });
            remaining -= share;
        }
    }

    // Step 4: Distribute leftovers
    if (remaining > 0) {
        for (var i = 0; i < available.length && remaining > 0; i++) {
            var item = available[i];
            var existing = deduction.find(d => d.itemId === item.itemId);
            if ((existing ? existing.amount : 0) < item.amount) {
                if (existing) {
                    existing.amount += 1;
                } else {
                    deduction.push({ itemId: item.itemId, amount: 1 });
                }
                remaining--;
            }
        }
    }

    // Final sanity check
    var finalSum = deduction.reduce((sum, d) => sum + d.amount, 0);
    if (finalSum !== requiredTotal) {
        return { success: false };
    }

    // Apply deduction
    for (var i = 0; i < deduction.length; i++) {
        cm.gainItem(deduction[i].itemId, -deduction[i].amount);
    }

    return { success: true, items: deduction };
}
