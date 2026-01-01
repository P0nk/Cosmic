var ScrollShopManager = Java.type("server.scrollshop.ScrollShopManager");

var status = -1;
var selectedService = -1;
var spellTraceBalance = 0;
var relationshipValue = 0;
var withdrawAmount = 0;
var basicShopItems = [];
var selectedCategoryPrefix = "";
var selectedItemId = -1;  // To track the selected item ID for purchase
var name = "";
var inv = "";
var itemQty = "" ;
var itemId = "" ;

// ===================== SCROLL → SPELL TRACE CONFIG =====================

// Spell Trace gained per scroll
var SPELL_TRACE_PER_SCROLL = 10;

// Scroll blacklist (never convertible)
var SCROLL_BLACKLIST = {
    2049100: true, // Chaos Scroll
    2049300: true, // Clean Slate Scroll
    2049400: true, // Innocence Scroll
    2049600: true  // Potential Scroll (example)
};

// Helper
function isBlacklistedScroll(itemId) {
    return SCROLL_BLACKLIST[itemId] === true;
}


// List for category prefixes
var categoryPrefix = [
    "20400", // Helmet
    "20401", // Face Accessory
    "20402", // Eye Accessory
    "20403", // Earring
    "20404", // Topwear
    "20405", // Overall
    "20406", // Bottomwear
    "20407", // Shoes
    "20408", // Gloves
    "20409", // Shield
    "20410", // Cape
    "20411", // Ring
    "20413", // Belt
    "20480", // Pet Equipment
    "20430", // 1H Sword
    "20431", // 1H Axe
    "20432", // 1H Blunt Weapon
    "20433", // Dagger
    "20437", // Wand
    "20438", // Staff
    "20440", // 2H Sword
    "20441", // 2H Axe
    "20442", // 2H Blunt Weapon
    "20443", // Spear
    "20444", // Polearm
    "20445", // Bow
    "20446", // Crossbow
    "20447", // Claw
    "20448", // Knuckle
    "20449"  // Pistol
];

// Corresponding list of category names in the same order
var categoryNames = [
    "Helmet",
    "Face Accessory",
    "Eye Accessory",
    "Earring",
    "Topwear",
    "Overall",
    "Bottomwear",
    "Shoes",
    "Gloves",
    "Shield",
    "Cape",
    "Ring",
    "Belt",
    "Pet Equipment",
    "1H Sword",
    "1H Axe",
    "1H Blunt Weapon",
    "Dagger",
    "Wand",
    "Staff",
    "2H Sword",
    "2H Axe",
    "2H Blunt Weapon",
    "Spear",
    "Polearm",
    "Bow",
    "Crossbow",
    "Claw",
    "Knuckle",
    "Pistol"
];

// Spell Trace item IDs (example, replace with actual item IDs used for Spell Trace)
var spellTraceItemIDs = [4000999, 4001832]; // Add actual Spell Trace item IDs here

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

    // =========================================================================
    // == MAIN MENU ============================================================
    // =========================================================================
    if (status === 0) {
        spellTraceBalance = ScrollShopManager.getSpellTraceBalance(cm.getPlayer().getId()) || 0; // Set to 0 if null
        relationshipValue = ScrollShopManager.getRelationshipValue(cm.getPlayer().getId()) || 0; // Assuming this method exists to get player's relationship value
        console.log("Main Menu: Spell Trace Balance = " + spellTraceBalance);  // Print spellTraceBalance
        console.log("Main Menu: Relationship Value = " + relationshipValue);  // Print relationshipValue
        var text = "Welcome to the Scroll Shop! Your current Spell Trace Balance: #b" + spellTraceBalance + "#k.\r\n\n";
        text += "Please choose an option:\r\n";
        text += "#L0#Deposit Spell Trace\r\n";
        text += "#L1#Withdraw Spell Trace\r\n";
        text += "#L2#Convert Scrolls to Spell Trace (Select)\r\n";  // Convert scrolls to spell trace
        text += "#L3#Convert ALL Scrolls to Spell Trace\r\n";
        text += "#L4#Open Basic Shop (100% Success Rate)\r\n"; // Basic Shop
        if (relationshipValue > 3000) {
            text += "#L5#Open Intermediate Shop (60% Success Rate)\r\n"; // Intermediate Shop
        }
        if (relationshipValue > 10000) {
            text += "#L6#Open Advanced Shop (10% Success Rate)\r\n"; // Advanced Shop
        }
        text += "#L7#Donate Spell Trace to Boost Relationships\r\n"; // Donate Spell Trace
        cm.sendSimple(text);
    }

    // =========================================================================
    // == Deposit Spell Trace ==================================================
    // =========================================================================
    else if (status === 1 && selection === 0) {
        console.log("Deposit Spell Trace selected.");
        var deposited = 0;
        var skipped = [];

        // Loop through each Spell Trace item ID and deposit them from inventory
        for (var i = 0; i < spellTraceItemIDs.length; i++) {
            itemId = spellTraceItemIDs[i];
            var inventoryQty = cm.getPlayer().getItemQuantity(itemId, false);
            console.log("Checking inventory for Item ID: " + itemId + ", Quantity: " + inventoryQty); // Debug inventory check
            if (inventoryQty <= 0) continue;

            // Calculate the max amount that can be deposited (we assume no limit in this case)
            var maxDeposit = 2000000000; // Set a max limit if needed
            var toDeposit = Math.min(inventoryQty, maxDeposit);
            var loop_short = toDeposit / 32000;

            // Deposit the Spell Trace into the bank
            cm.gainItem(itemId, -toDeposit);  // Remove from inventory
            var newbalance = spellTraceBalance + toDeposit;
            ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), newbalance); // Update the database

            deposited += toDeposit;

            if (toDeposit < inventoryQty) {
                skipped.push("Spell Trace (Item ID: " + itemId + ") (partially deposited)");
            }
        }

        var msg = "You have successfully deposited " + deposited + " Spell Trace.";
        if (skipped.length > 0) {
            msg += "\r\nCould not store: " + skipped.join(", ");
        }
        console.log("Deposit result: " + msg); // Print deposit result
        cm.sendOk(msg);
        cm.dispose();
    }

    // =========================================================================
    // == Withdraw Spell Trace =================================================
    // =========================================================================
    else if (status === 1 && selection === 1) {
        console.log("Withdraw Spell Trace selected.");
        var withdrawable = spellTraceBalance; // Player's current balance
        cm.sendGetText("How many Spell Trace would you like to withdraw? Your current balance is: #b" + withdrawable + "#k.");
    }

    // Handle withdrawal text input (amount to withdraw)
    else if (status === 2) {
        withdrawAmount = parseInt(cm.getText());
        console.log("Withdrawal amount: " + withdrawAmount);  // Print withdrawal amount
        if (isNaN(withdrawAmount) || withdrawAmount <= 0 || withdrawAmount > spellTraceBalance) {
            cm.sendOk("Invalid amount. Please enter a valid number.");
            cm.dispose();
            return;
        }

        // Withdraw the amount
        var newBalance = spellTraceBalance - withdrawAmount;
        ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), newBalance);
        cm.gainItem(4000999, withdrawAmount); // Adjust the item ID as needed for the Spell Trace item
        cm.sendOk("You have successfully withdrawn " + withdrawAmount + " Spell Trace. Your new balance is: #b" + newBalance + "#k.");
        cm.dispose();
    }

    // =========================================================================
    // == Convert Scroll to Spell Trace ========================================
    // =========================================================================
    else if (status === 1 && selection === 2) {
        status = 10;
        console.log("Convert Scroll to Spell Trace selected.");
        inv = cm.getInventory(2); // Check inventory for scrolls (2 is the inventory type)
//        console.log("Inventory retrieved: " + inv);  // Print inventory object to check
        var lines = [];  // Array to store the scroll options
//        console.log("Lines initialized: " + lines);  // Print lines array to check

        // Loop through all inventory slots
        for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {
            var item = inv.getItem(slot);
//            console.log("Checking slot " + slot + ": " + item); // Print each item for debugging
            if (!item) continue;  // Skip if no item in the slot

            itemId = item.getItemId();  // Get the item ID from the inventory item
//            console.log("Item ID: " + itemId); // Print itemId
            name = Packages.server.ItemInformationProvider.getInstance().getName(itemId);  // Get the item name
//            console.log("Item Name: " + name); // Print item name

            // Check if the item ID starts with '204' (this means it's a scroll)
            if (itemId.toString().startsWith("204")) {
                itemQty = item.getQuantity(); // Get item quantity in the inventory
//                console.log("Item ID starts with 204: " + itemId + ", Quantity: " + itemQty);  // Print matching items for debugging
                lines.push(`#L${slot}##v${itemId}# ${name} (Qty: ${itemQty})#l`); // Display item with quantity
            }
        }

        // If there are valid scrolls, show the options to the player
        if (lines.length > 0) {
            cm.sendSimple("Select the scroll to convert to spell trace:\r\n" + lines.join("\r\n"));
//            console.log("Lines to display: " + lines.join("\r\n"));  // Print the lines for debugging
        } else {
            cm.sendOk("You have no scrolls to convert.");
            cm.dispose();
        }
    }

    // =========================================================================
    // == Select Item to Convert and Ask for Conversion Quantity =================
    // =========================================================================
    else if (status === 11) {
        selectedItem = inv.getItem(selection);  // Get selected item from inventory
           console.log("selectedItem: " + selectedItem );
        itemId = selectedItem.getItemId();
                  console.log("itemId: " + itemId );
        var itemName = Packages.server.ItemInformationProvider.getInstance().getName(itemId);
                  console.log("itemName: " + itemName );
            itemQty = selectedItem.getQuantity(); // Get item quantity
          console.log("itemQty: " + itemQty );
        // Print the selected item for debugging
        console.log("Selected Item: " + itemName + " (Item ID: " + itemId + ", Quantity: " + itemQty + ")");

        // Ask how many of the selected item the player wants to convert
        cm.sendGetText("How many #i"+ itemId+"##e#k" + itemName + "#n do you want to convert to Spell Trace? You have " + itemQty + " available.");
    }

    // =========================================================================
    // == Handle the Conversion Amount and Calculate Spell Trace =================
    // =========================================================================
    else if (status === 12) {
            console.log("cm.getText(): " + cm.getText() );
        var conversionAmount = parseInt(cm.getText());
        console.log("conversionAmount: " + conversionAmount );

        if (isNaN(conversionAmount) || conversionAmount <= 0) {
            cm.sendOk("Please enter a valid number of scrolls to convert.");
            cm.dispose();
            return;
        }

        console.log("itemQty: " + itemQty );
        console.log("conversionAmount: " + conversionAmount );
        if (conversionAmount > itemQty) {
            cm.sendOk("You do not have enough of this item to convert. You have " + itemQty + " available.");
            cm.dispose();
            return;
        }

        // Calculate the Spell Trace generated based on the conversion amount
        var spellTraceGenerated = conversionAmount * 10; // Example conversion rate: 10 Spell Trace per scroll
        console.log("Spell Trace generated: " + spellTraceGenerated);  // Print conversion result

        // Update the player's Spell Trace balance
        spellTraceBalance += spellTraceGenerated;
        ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance);

        console.log("selectedItem: " + selectedItem);  // Print conversion result
        console.log("itemId: " + itemId);  // Print conversion result
        // Update the player's inventory (remove converted scrolls)
        cm.gainItem(itemId, -conversionAmount);  // Remove the converted scrolls

        // Show the result to the player
        cm.sendOk("You have successfully converted #i "+ itemId +"##e#b " + conversionAmount + " " + name + "#n#k to #r#e" + spellTraceGenerated + " Spell Trace#n#k. Your new balance is: #b" + spellTraceBalance + "#k.");
        cm.dispose();
    }

// =========================================================================
// == Convert ALL Scrolls to Spell Trace ===================================
// =========================================================================
else if (status === 1 && selection === 3) {
    status = 70;

    var inv = cm.getInventory(2); // USE inventory
    var totalScrolls = 0;
    var totalSpellTrace = 0;
    var skipped = [];

    for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {
        var item = inv.getItem(slot);
        if (!item) continue;

        var itemId = item.getItemId();
        var qty = item.getQuantity();

        // Must be scroll
        if (!itemId.toString().startsWith("204")) continue;

        // Blacklist check
        if (isBlacklistedScroll(itemId)) {
            skipped.push(
                Packages.server.ItemInformationProvider.getInstance().getName(itemId)
            );
            continue;
        }

        totalScrolls += qty;
        totalSpellTrace += qty * SPELL_TRACE_PER_SCROLL;

        // Remove scrolls
        cm.gainItem(itemId, -qty);
    }

    if (totalScrolls === 0) {
        cm.sendOk("You have no eligible scrolls to convert.");
        cm.dispose();
        return;
    }

    // Update balance (DB is source of truth)
    spellTraceBalance += totalSpellTrace;
    ScrollShopManager.updateSpellTraceBalance(
        cm.getPlayer().getId(),
        spellTraceBalance
    );

    var msg = "You converted #b" + totalScrolls + "#k scrolls into "
            + "#r" + totalSpellTrace + "#k Spell Trace.\r\n\r\n"
            + "New Balance: #b" + spellTraceBalance + "#k";

    if (skipped.length > 0) {
        msg += "\r\n\r\nSkipped (blacklisted):\r\n- " + skipped.join("\r\n- ");
    }

    cm.sendOk(msg);
    cm.dispose();
}




    // =========================================================================
    // == Open Basic Shop ======================================================
    // =========================================================================
    else if (status === 1 && selection === 4) {
        console.log("Open Basic Shop selected.");
        status = 20;
        var text = "Please select an equipment category to view scrolls:\r\n";
        for (var i = 0; i < categoryNames.length; i++) {
             var category = categoryNames[i];
             text += "#L" + i + "# " + category + " Scrolls#l\r\n"; // Display categories for selection
                    }

        cm.sendSimple(text); // Show category selection options
        }

        // =========================================================================
        // == Equipment Category Selected (status 21) ============================
        // =========================================================================
        else if (status === 21) {
            console.log ("selection: "+selection);
            console.log ("CategoryPrefix: "+categoryPrefix);
            selectedCategoryPrefix =categoryPrefix[selection];
            console.log ("selectedCategoryPrefix: "+selectedCategoryPrefix);
            console.log("Selected Equipment Category Prefix: " + selectedCategoryPrefix); // Print selected category prefix
            basicShopItems = ScrollShopManager.getShopByCategory(100, selectedCategoryPrefix); // Retrieve items based on the selected category prefix
            console.log("Items retrieved from the database: " + basicShopItems);

            if (basicShopItems.length === 0) {
                cm.sendOk("No scrolls available for this category.");
                cm.dispose();
                return;
            }

            var text = "Available Scrolls for your selection:\r\n";
            for (var i = 0; i < basicShopItems.length; i++) {
                var item = basicShopItems[i];
                itemId = item[0];
                var summary = item[1];

                // Format the text to display the icon and summary
                text += "#L" + i + "##i" + itemId + "##b" + summary + "#l\r\n";
            }

            cm.sendSimple(text); // Display the items for the selected category
        }

        // =========================================================================
        // == Purchase Item from Shop (status 22) =================================
        // =========================================================================
        else if (status === 22) {
            selectedItemId = basicShopItems[selection][0]; // Get selected item ID
             name = Packages.server.ItemInformationProvider.getInstance().getName(selectedItemId);
//            var selectedItemName = basicShopItems[selection][1]; // Get selected item name
            console.log("Selected item: " + selectedItemId + " (" + name + ")");

            cm.sendGetText("How many of #i" +selectedItemId + "##l#e#k" + name + "#n#k would you like to buy using Spell Trace? They cost 20 each."); // Ask user how many to buy
//            status = 23; // Proceed to asking for the purchase amount
            console.log("name1: " + name );
        }

        // =========================================================================
        // == Finalizing the Purchase (status 23) =================================
        // =========================================================================
        else if (status === 23) {
                    console.log("name2: " + name );
            var purchaseAmount = parseInt(cm.getText());
            console.log("Purchase amount: " + purchaseAmount); // Print purchase amount

            if (isNaN(purchaseAmount) || purchaseAmount <= 0) {
                cm.sendOk("Please enter a valid number of scrolls to purchase.");
                cm.dispose();
                return;
            }

            // Check if player has enough Spell Trace to make the purchase
            var itemPrice = 20; // Define the cost of each scroll (this is an example, modify as needed)
            var totalCost = purchaseAmount * itemPrice;

            if (spellTraceBalance < totalCost) {
                cm.sendOk("You do not have enough Spell Trace to complete this purchase.");
                cm.dispose();
                return;
            }

            // Deduct Spell Trace and complete the purchase
            spellTraceBalance -= totalCost;
            ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance); // Update balance in the database
            cm.gainItem(selectedItemId, purchaseAmount); // Add purchased scrolls to the player's inventory
            console.log("name3: " + name );


            cm.sendOk("You have successfully purchased " + purchaseAmount + " " + name + "(s) for " + totalCost + " Spell Trace.");
            cm.dispose(); // End the conversation
        }

    // =========================================================================
    // == Open Intermediate Shop ======================================================
    // =========================================================================
    else if (status === 1 && selection === 5) {
        console.log("Open Intermediate Shop selected.");
        status = 30;
        var text = "Please select an equipment category to view scrolls:\r\n";
        for (var i = 0; i < categoryNames.length; i++) {
             var category = categoryNames[i];
             text += "#L" + i + "# " + category + " Scrolls#l\r\n"; // Display categories for selection
                    }

        cm.sendSimple(text); // Show category selection options
        }

        // =========================================================================
        // == Equipment Category Selected (status 31) ============================
        // =========================================================================
        else if (status === 31) {
            console.log ("selection: "+selection);
            console.log ("CategoryPrefix: "+categoryPrefix);
            selectedCategoryPrefix =categoryPrefix[selection];
            console.log ("selectedCategoryPrefix: "+selectedCategoryPrefix);
            console.log("Selected Equipment Category Prefix: " + selectedCategoryPrefix); // Print selected category prefix
            basicShopItems = ScrollShopManager.getShopByCategory(60, selectedCategoryPrefix); // Retrieve items based on the selected category prefix
            console.log("Items retrieved from the database: " + basicShopItems);

            if (basicShopItems.length === 0) {
                cm.sendOk("No scrolls available for this category.");
                cm.dispose();
                return;
            }

            var text = "Available Scrolls for your selection:\r\n";
            for (var i = 0; i < basicShopItems.length; i++) {
                var item = basicShopItems[i];
                itemId = item[0];
                var summary = item[1];

                // Format the text to display the icon and summary
                text += "#L" + i + "##i" + itemId + "##b" + summary + "#l\r\n";
            }

            cm.sendSimple(text); // Display the items for the selected category
        }

        // =========================================================================
        // == Purchase Item from Shop (status 32) =================================
        // =========================================================================
        else if (status === 32) {
            selectedItemId = basicShopItems[selection][0]; // Get selected item ID
             name = Packages.server.ItemInformationProvider.getInstance().getName(selectedItemId);
//            var selectedItemName = basicShopItems[selection][1]; // Get selected item name
            console.log("Selected item: " + selectedItemId + " (" + name + ")");

            cm.sendGetText("How many of #i" +selectedItemId + "##l#e#k" + name + "#n#k would you like to buy using Spell Trace? They cost 40 each."); // Ask user how many to buy
//            status = 23; // Proceed to asking for the purchase amount
            console.log("name1: " + name );
        }

        // =========================================================================
        // == Finalizing the Purchase (status 33) =================================
        // =========================================================================
        else if (status === 33) {
                    console.log("name2: " + name );
            var purchaseAmount = parseInt(cm.getText());
            console.log("Purchase amount: " + purchaseAmount); // Print purchase amount

            if (isNaN(purchaseAmount) || purchaseAmount <= 0) {
                cm.sendOk("Please enter a valid number of scrolls to purchase.");
                cm.dispose();
                return;
            }

            // Check if player has enough Spell Trace to make the purchase
            var itemPrice = 40; // Define the cost of each scroll (this is an example, modify as needed)
            var totalCost = purchaseAmount * itemPrice;

            if (spellTraceBalance < totalCost) {
                cm.sendOk("You do not have enough Spell Trace to complete this purchase.");
                cm.dispose();
                return;
            }

            // Deduct Spell Trace and complete the purchase
            spellTraceBalance -= totalCost;
            ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance); // Update balance in the database
            cm.gainItem(selectedItemId, purchaseAmount); // Add purchased scrolls to the player's inventory
            console.log("name3: " + name );


            cm.sendOk("You have successfully purchased " + purchaseAmount + " " + name + "(s) for " + totalCost + " Spell Trace.");
            cm.dispose(); // End the conversation
        }


    // =========================================================================
    // == Open Advance Shop ======================================================
    // =========================================================================
    else if (status === 1 && selection === 6) {
        console.log("Open Intermediate Shop selected.");
        status = 40;
        var text = "Please select an equipment category to view scrolls:\r\n";
        for (var i = 0; i < categoryNames.length; i++) {
             var category = categoryNames[i];
             text += "#L" + i + "# " + category + " Scrolls#l\r\n"; // Display categories for selection
                    }

        cm.sendSimple(text); // Show category selection options
        }

        // =========================================================================
        // == Equipment Category Selected (status 31) ============================
        // =========================================================================
        else if (status === 41) {
            console.log ("selection: "+selection);
            console.log ("CategoryPrefix: "+categoryPrefix);
            selectedCategoryPrefix =categoryPrefix[selection];
            console.log ("selectedCategoryPrefix: "+selectedCategoryPrefix);
            console.log("Selected Equipment Category Prefix: " + selectedCategoryPrefix); // Print selected category prefix
            basicShopItems = ScrollShopManager.getShopByCategory(10, selectedCategoryPrefix); // Retrieve items based on the selected category prefix
            console.log("Items retrieved from the database: " + basicShopItems);

            if (basicShopItems.length === 0) {
                cm.sendOk("No scrolls available for this category.");
                cm.dispose();
                return;
            }

            var text = "Available Scrolls for your selection:\r\n";
            for (var i = 0; i < basicShopItems.length; i++) {
                var item = basicShopItems[i];
                itemId = item[0];
                var summary = item[1];

                // Format the text to display the icon and summary
                text += "#L" + i + "##i" + itemId + "##b" + summary + "#l\r\n";
            }

            cm.sendSimple(text); // Display the items for the selected category
        }

        // =========================================================================
        // == Purchase Item from Shop (status 42) =================================
        // =========================================================================
        else if (status === 42) {
            selectedItemId = basicShopItems[selection][0]; // Get selected item ID
             name = Packages.server.ItemInformationProvider.getInstance().getName(selectedItemId);
//            var selectedItemName = basicShopItems[selection][1]; // Get selected item name
            console.log("Selected item: " + selectedItemId + " (" + name + ")");

            cm.sendGetText("How many of #i" +selectedItemId + "##l#e#k" + name + "#n#k would you like to buy using Spell Trace? They cost 55 each."); // Ask user how many to buy
//            status = 23; // Proceed to asking for the purchase amount
            console.log("name1: " + name );
        }

        // =========================================================================
        // == Finalizing the Purchase (status 43) =================================
        // =========================================================================
        else if (status === 43) {
                    console.log("name2: " + name );
            var purchaseAmount = parseInt(cm.getText());
            console.log("Purchase amount: " + purchaseAmount); // Print purchase amount

            if (isNaN(purchaseAmount) || purchaseAmount <= 0) {
                cm.sendOk("Please enter a valid number of scrolls to purchase.");
                cm.dispose();
                return;
            }

            // Check if player has enough Spell Trace to make the purchase
            var itemPrice = 55; // Define the cost of each scroll (this is an example, modify as needed)
            var totalCost = purchaseAmount * itemPrice;

            if (spellTraceBalance < totalCost) {
                cm.sendOk("You do not have enough Spell Trace to complete this purchase.");
                cm.dispose();
                return;
            }

            // Deduct Spell Trace and complete the purchase
            spellTraceBalance -= totalCost;
            ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance); // Update balance in the database
            cm.gainItem(selectedItemId, purchaseAmount); // Add purchased scrolls to the player's inventory
            console.log("name3: " + name );


            cm.sendOk("You have successfully purchased " + purchaseAmount + " " + name + "(s) for " + totalCost + " Spell Trace.");
            cm.dispose(); // End the conversation
        }

    // =========================================================================
    // == Relationship Empowerment ======================================================
    // =========================================================================
    else if (status === 1 && selection === 7) {
    // Prompt the player for the donation amount
    status = 60;
    cm.sendGetText("How many Spell Trace would you like to donate? Your current Spell Trace balance is: #b" + spellTraceBalance + "#k.");

        }
else if (status === 61) {
    // Retrieve the donation amount entered by the player
    withdrawAmount = parseInt(cm.getText());
    console.log("Donated Amount: " + withdrawAmount);

    // Check if the donation amount is valid
    if (isNaN(withdrawAmount) || withdrawAmount <= 0 || withdrawAmount > spellTraceBalance) {
        cm.sendOk("Invalid amount. You either entered a non-numeric value or the amount exceeds your balance.");
        cm.dispose();
        return;
    }
 // Calculate the new relationship value after donation
    var newRelationshipValue = relationshipValue + (withdrawAmount * 1);  // Example: 10 relationship points per Spell Trace donated
    var newSpellTraceBalance = spellTraceBalance - withdrawAmount;

// Function to determine the relationship message based on the value
function getRelationshipMessage(newRelationshipValue) {
    if (relationshipValue < 1000) {
        return "Starting your journey, keep donating to grow your bond!";
    } else if (relationshipValue < 2000) {
        return "You're building a strong relationship, keep it up!";
    } else if (relationshipValue < 3000) {
        return "Your relationship is solid, you're on a great path!";
    } else if (relationshipValue < 4000) {
        return "You're well on your way, your bond is growing stronger!";
    } else if (relationshipValue < 5000) {
        return "You're becoming an important ally, great progress!";
    } else if (relationshipValue < 10000) {
        return "A valued partner, your relationship is truly remarkable!";
    } else {
        return "You're a legendary partner, your bond is unbreakable!";
    }
}


    console.log("New Relationship Value: " + newRelationshipValue);
    console.log("New Spell Trace Balance: " + newSpellTraceBalance);

    // Update the player's relationship value and Spell Trace balance
    ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), newSpellTraceBalance);
    ScrollShopManager.updateRelationshipValue(cm.getPlayer().getId(), newRelationshipValue);

    // Provide feedback to the player
    cm.sendOk("You have successfully donated " + withdrawAmount + " Spell Trace. #b" + getRelationshipMessage(newRelationshipValue) + "#k. Your Spell Trace balance is: #b" + newSpellTraceBalance + "#k.");

    cm.dispose();

}

}
