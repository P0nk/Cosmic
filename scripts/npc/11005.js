var ScrollShopManager = Java.type("server.scrollshop.ScrollShopManager");

var status = -1;
var selectedService = -1;
var spellTraceBalance = 0;
var relationshipValue = 0;
var withdrawAmount = 0;
var basicShopItems = [];
var selectedCategoryPrefix = "";
var selectedItemId = -1;
var name = "";
var inv = null;
var selectedSlot = -1;
var selectedItem = null;
var itemQty = 0;
var itemId = 0;
var item = null;

const RELATIONSHIP_EARN_RATE = 0.50; // 50% of spell trace spent

function awardRelationshipFromSpend(spendAmount) {
    if (spendAmount <= 0) return 0;

    var cid = cm.getPlayer().getId();
    var curRel = ScrollShopManager.getRelationshipValue(cid);
    if (curRel < 0) curRel = 0;

    var gain = Math.floor(spendAmount * RELATIONSHIP_EARN_RATE);
    if (gain <= 0) return 0;

    var newRel = curRel + gain;
    ScrollShopManager.updateRelationshipValue(cid, newRel);

    relationshipValue = newRel;
    return gain;
}

// ===================== SCROLL → SPELL TRACE CONFIG =====================
var SPELL_TRACE_PER_SCROLL = 10;

var SCROLL_BLACKLIST = {
    2049100: true, // Chaos Scroll
    2049115: true, // Chaos Scroll of goodness
    2049116: true, // Incredible Chaos Scroll
    2049117: true, // Miraculous  Chaos Scroll
    2049300: true, // Clean Slate Scroll
    2049400: true, // Innocence Scroll
    2049600: true  // Potential Scroll

};

// ===================== RANDOM SPELL TRACE LOGIC =====================
function randInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function isBlacklistedScroll(itemId) {
    return SCROLL_BLACKLIST[itemId] === true;
}

// Categories
var categoryPrefix = [
    "20400", "20401", "20402", "20403", "20404", "20405", "20406", "20407",
    "20408", "20409", "20410", "20411", "20413", "20480", "20430", "20431",
    "20432", "20433", "20437", "20438", "20440", "20441", "20442", "20443",
    "20444", "20445", "20446", "20447", "20448", "20449"
];

var categoryNames = [
    "Helmet", "Face Accessory", "Eye Accessory", "Earring", "Topwear", "Overall",
    "Bottomwear", "Shoes", "Gloves", "Shield", "Cape", "Ring", "Belt", "Pet Equipment",
    "1H Sword", "1H Axe", "1H Blunt Weapon", "Dagger", "Wand", "Staff", "2H Sword",
    "2H Axe", "2H Blunt Weapon", "Spear", "Polearm", "Bow", "Crossbow", "Claw",
    "Knuckle", "Pistol"
];

var spellTraceItemIDs = [4000999, 4001832];

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
        spellTraceBalance = ScrollShopManager.getSpellTraceBalance(cm.getPlayer().getId()) || 0;
        relationshipValue = ScrollShopManager.getRelationshipValue(cm.getPlayer().getId()) || 0;

        var text = "Welcome to the Scroll Shop! Your current Spell Trace Balance: #b" + spellTraceBalance + "#k.\r\n\n";
        text += "Please choose an option:\r\n";
        text += "#L0#Deposit Spell Trace\r\n";
        text += "#L1#Withdraw Spell Trace\r\n";
        text += "#L2#Convert Scrolls to Spell Trace (Select)\r\n";
        // UPDATED L3: Now implies Books as well
        text += "#L3#Convert ALL Scrolls to Spell Trace\r\n";
        // NEW L4: Convert Books Only
        text += "#L4#Convert ALL Skill/Mastery Books Only\r\n";
        // SHIFTED DOWN
        text += "#L5#Open Basic Shop (100% Scroll)\r\n";
        if (relationshipValue > 2000) {
            text += "#L6#Open Intermediate Shop (60% Scroll)\r\n";
        }
        if (relationshipValue > 5000) {
            text += "#L7#Open Advanced Shop (10% Scroll)\r\n";
        }
        if (relationshipValue > 10000) {
            text += "#L8#Open Master Shop\r\n";
        }

        text += "#L9#Donate Spell Trace to Boost Relationships\r\n";
        cm.sendSimple(text);
    }

    // =========================================================================
    // == Deposit Spell Trace ==================================================
    // =========================================================================
    else if (status === 1 && selection === 0) {
        var deposited = 0;
        var skipped = [];

        for (var i = 0; i < spellTraceItemIDs.length; i++) {
            itemId = spellTraceItemIDs[i];
            var inventoryQty = cm.getPlayer().getItemQuantity(itemId, false);
            if (inventoryQty <= 0) continue;

            var maxDeposit = 2000000000;
            var toDeposit = Math.min(inventoryQty, maxDeposit);

            cm.gainItem(itemId, -toDeposit);
            var newbalance = spellTraceBalance + toDeposit;
            ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), newbalance);

            deposited += toDeposit;

            if (toDeposit < inventoryQty) {
                skipped.push("Spell Trace (Item ID: " + itemId + ") (partially deposited)");
            }
        }

        var msg = "You have successfully deposited " + deposited + " Spell Trace.";
        if (skipped.length > 0) {
            msg += "\r\nCould not store: " + skipped.join(", ");
        }
        cm.sendOk(msg);
        cm.dispose();
    }

    // =========================================================================
    // == Withdraw Spell Trace =================================================
    // =========================================================================
    else if (status === 1 && selection === 1) {
        var withdrawable = spellTraceBalance;
        cm.sendGetText("How many Spell Trace would you like to withdraw? Your current balance is: #b" + withdrawable + "#k.");
    }
    else if (status === 2 && selection === 1) {
        // Flow control catch
    }
    else if (status === 2 && withdrawAmount === 0) {
        withdrawAmount = parseInt(cm.getText());
        if (isNaN(withdrawAmount) || withdrawAmount <= 0 || withdrawAmount > spellTraceBalance) {
            cm.sendOk("Invalid amount. Please enter a valid number.");
            cm.dispose();
            return;
        }

        var newBalance = spellTraceBalance - withdrawAmount;
        ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), newBalance);
        cm.gainItem(4000999, withdrawAmount);
        cm.sendOk("You have successfully withdrawn " + withdrawAmount + " Spell Trace. Your new balance is: #b" + newBalance + "#k.");
        cm.dispose();
    }

  // =========================================================================
  // == Convert Scroll to Spell Trace (Individual) ===========================
  // =========================================================================
  else if (status === 1 && selection === 2) {
      status = 10;
      inv = cm.getInventory(2);
      var lines = [];

      for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {
          item = inv.getItem(slot);
          if (!item) continue;

          itemId = item.getItemId();
          var sId = itemId.toString();
          name = Packages.server.ItemInformationProvider.getInstance().getName(itemId);

          // Logic:
          // 1. Must start with 204 (Scroll)
          // OR
          // 2. Must start with 228/229 AND name contains "Book"
          var isScroll = sId.startsWith("204");
          var isBook = (sId.startsWith("228") || sId.startsWith("229")) && name.indexOf("Book") !== -1;

          if (isScroll || isBook) {
              itemQty = item.getQuantity();
              lines.push(`#L${slot}##v${itemId}# ${name} (Qty: ${itemQty})#l`);
          }
      }

      if (lines.length > 0) {
          cm.sendSimple("Select the item to convert to spell trace:\r\n" + lines.join("\r\n"));
      } else {
          cm.sendOk("You have no eligible scrolls or books to convert.");
          cm.dispose();
      }
  }

  else if (status === 11) {
      var invNow = cm.getInventory(2);
      if (invNow == null) {
          cm.sendOk("Session expired. Please try again.");
          cm.dispose();
          return;
      }

      selectedSlot = selection;
      selectedItem = invNow.getItem(selectedSlot);

      if (selectedItem == null) {
          cm.sendOk("That slot is empty. Please try again.");
          cm.dispose();
          return;
      }

      itemId = selectedItem.getItemId();
      var sId = itemId.toString();
      itemQty = selectedItem.getQuantity();
      name = Packages.server.ItemInformationProvider.getInstance().getName(itemId);

      // Validate: Is it a Scroll (204) OR a valid Book (228/229 + Name check)
      var isScroll = sId.startsWith("204");
      var isBook = (sId.startsWith("228") || sId.startsWith("229")) && name.indexOf("Book") !== -1;

      if (!isScroll && !isBook) {
          cm.sendOk("That is not a valid Scroll or Book.");
          cm.dispose();
          return;
      }

      // Blacklist check (Safety check)
      if (isBlacklistedScroll(itemId)) {
          cm.sendOk("This item is not eligible for conversion.");
          cm.dispose();
          return;
      }

      cm.sendGetText("How many #i" + itemId + "# #b" + name + "#k do you want to convert?\r\nYou have: #b" + itemQty + "#k");
  }

  else if (status === 12) {
      var conversionAmount = parseInt(cm.getText());

      if (isNaN(conversionAmount) || conversionAmount <= 0) {
          cm.sendOk("Please enter a valid number of items to convert.");
          cm.dispose();
          return;
      }

      if (conversionAmount > itemQty) {
          cm.sendOk("You do not have enough of this item to convert. You have " + itemQty + " available.");
          cm.dispose();
          return;
      }

      var spellTraceGenerated = 0;
      // Calculate traces generated (random 6-14 per item)
      for (var i = 0; i < conversionAmount; i++) {
          spellTraceGenerated += randInt(6, 14);
      }

      spellTraceBalance += spellTraceGenerated;
      ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance);

      cm.gainItem(itemId, -conversionAmount);

      cm.sendOk("You have successfully converted #i "+ itemId +"##e#b " + conversionAmount + " " + name + "#n#k to #r#e" + spellTraceGenerated + " Spell Trace#n#k. Your new balance is: #b" + spellTraceBalance + "#k.");
      cm.dispose();
  }

    // =========================================================================
    // == L3: Convert ALL Scrolls to Spell Trace (SCROLLS ONLY) ================
    // =========================================================================
    else if (status === 1 && selection === 3) {
        var inv = cm.getInventory(2);
        var totalItems = 0;
        var totalSpellTrace = 0;
        var skipped = [];

        for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {
            item = inv.getItem(slot);
            if (!item) continue;

            itemId = item.getItemId();
            var qty = item.getQuantity();
            var sId = itemId.toString();

            // Strict check only for Scrolls (204)
            if (!sId.startsWith("204")) continue;

            // Blacklist check
            if (isBlacklistedScroll(itemId)) {
                skipped.push(Packages.server.ItemInformationProvider.getInstance().getName(itemId));
                continue;
            }

            totalItems += qty;
            var gained = 0;
            for (var i = 0; i < qty; i++) {
                gained += randInt(6, 14);
            }

            totalSpellTrace += gained;
            cm.gainItem(itemId, -qty);
        }

        if (totalItems === 0) {
            cm.sendOk("You have no eligible scrolls to convert.");
            cm.dispose();
            return;
        }

        spellTraceBalance += totalSpellTrace;
        ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance);

        var msg = "You converted #b" + totalItems + "#k scrolls into "
                + "#r" + totalSpellTrace + "#k Spell Trace.\r\n\r\n"
                + "New Balance: #b" + spellTraceBalance + "#k";

        if (skipped.length > 0) {
            msg += "\r\n\r\nSkipped (blacklisted):\r\n- " + skipped.join("\r\n- ");
        }

        cm.sendOk(msg);
        cm.dispose();
    }

    // =========================================================================
    // == L4: Convert ALL Books Only ===========================================
    // =========================================================================
    else if (status === 1 && selection === 4) {
        var inv = cm.getInventory(2);
        var totalBooks = 0;
        var totalSpellTrace = 0;

        for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {
            item = inv.getItem(slot);
            if (!item) continue;

            itemId = item.getItemId();
            var qty = item.getQuantity();
            var sId = itemId.toString();

            // 1. Check ID Prefix
            if (!sId.startsWith("228") && !sId.startsWith("229")) continue;

            // 2. Check Name for "Book"
            var itemName = Packages.server.ItemInformationProvider.getInstance().getName(itemId);
            if (itemName == null || itemName.indexOf("Book") === -1) {
                continue;
            }

            totalBooks += qty;
            var gained = 0;
            for (var i = 0; i < qty; i++) {
                gained += randInt(6, 14); // Same rate as scrolls
            }

            totalSpellTrace += gained;
            cm.gainItem(itemId, -qty);
        }

        if (totalBooks === 0) {
            cm.sendOk("You have no eligible Books to convert.");
            cm.dispose();
            return;
        }

        spellTraceBalance += totalSpellTrace;
        ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance);

        var msg = "You converted #b" + totalBooks + "#k Books into "
                + "#r" + totalSpellTrace + "#k Spell Trace.\r\n\r\n"
                + "New Balance: #b" + spellTraceBalance + "#k";

        cm.sendOk(msg);
        cm.dispose();
    }

    // =========================================================================
    // == Open Basic Shop (Formerly L4, Now L5) ================================
    // =========================================================================
    else if (status === 1 && selection === 5) {
        status = 20;
        var text = "Please select an equipment category to view scrolls:\r\n";
        for (var i = 0; i < categoryNames.length; i++) {
             var category = categoryNames[i];
             text += "#L" + i + "# " + category + " Scrolls#l\r\n";
        }
        cm.sendSimple(text);
    }

    // Basic Shop Category Selected
    else if (status === 21) {
        selectedCategoryPrefix = categoryPrefix[selection];
        basicShopItems = ScrollShopManager.getShopByCategory(100, selectedCategoryPrefix);

        if (basicShopItems.length === 0) {
            cm.sendOk("No scrolls available for this category.");
            cm.dispose();
            return;
        }

        var text = "Available Scrolls for your selection:\r\n";
        for (var i = 0; i < basicShopItems.length; i++) {
            item = basicShopItems[i];
            itemId = item[0];
            var summary = item[1];
            text += "#L" + i + "##i" + itemId + "##b" + summary + "#l\r\n";
        }
        cm.sendSimple(text);
    }

    // Basic Shop Item Selected
    else if (status === 22) {
        selectedItemId = basicShopItems[selection][0];
        name = Packages.server.ItemInformationProvider.getInstance().getName(selectedItemId);
        cm.sendGetText("How many of #i" +selectedItemId + "##l#e#k" + name + "#n#k would you like to buy using Spell Trace? They cost 20 each.");
    }

    // Basic Shop Finalize
    else if (status === 23) {
        var purchaseAmount = parseInt(cm.getText());
        if (isNaN(purchaseAmount) || purchaseAmount <= 0) {
            cm.sendOk("Please enter a valid number.");
            cm.dispose();
            return;
        }
        var itemPrice = 20;
        var totalCost = purchaseAmount * itemPrice;

        if (spellTraceBalance < totalCost) {
            cm.sendOk("You do not have enough Spell Trace.");
            cm.dispose();
            return;
        }

        spellTraceBalance -= totalCost;
        ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance);
        var relGain = awardRelationshipFromSpend(totalCost);
        cm.gainItem(selectedItemId, purchaseAmount);
        cm.sendOk("You have successfully purchased " + purchaseAmount + " " + name + "(s) for " + totalCost + " Spell Trace.\r\n#dRelationship gained:#k +" + relGain);
        cm.dispose();
    }

    // =========================================================================
    // == Open Intermediate Shop (Formerly L5, Now L6) =========================
    // =========================================================================
    else if (status === 1 && selection === 6) {
        status = 30;
        var text = "Please select an equipment category to view scrolls:\r\n";
        for (var i = 0; i < categoryNames.length; i++) {
             var category = categoryNames[i];
             text += "#L" + i + "# " + category + " Scrolls#l\r\n";
        }
        cm.sendSimple(text);
    }

    else if (status === 31) {
        selectedCategoryPrefix = categoryPrefix[selection];
        basicShopItems = ScrollShopManager.getShopByCategory(60, selectedCategoryPrefix);
        if (basicShopItems.length === 0) {
            cm.sendOk("No scrolls available for this category.");
            cm.dispose();
            return;
        }
        var text = "Available Scrolls for your selection:\r\n";
        for (var i = 0; i < basicShopItems.length; i++) {
            item = basicShopItems[i];
            itemId = item[0];
            var summary = item[1];
            text += "#L" + i + "##i" + itemId + "##b" + summary + "#l\r\n";
        }
        cm.sendSimple(text);
    }

    else if (status === 32) {
        selectedItemId = basicShopItems[selection][0];
        name = Packages.server.ItemInformationProvider.getInstance().getName(selectedItemId);
        cm.sendGetText("How many of #i" +selectedItemId + "##l#e#k" + name + "#n#k would you like to buy using Spell Trace? They cost 30 each.");
    }

    else if (status === 33) {
        var purchaseAmount = parseInt(cm.getText());
        if (isNaN(purchaseAmount) || purchaseAmount <= 0) {
            cm.sendOk("Please enter a valid number.");
            cm.dispose();
            return;
        }
        var itemPrice = 30;
        var totalCost = purchaseAmount * itemPrice;

        if (spellTraceBalance < totalCost) {
            cm.sendOk("You do not have enough Spell Trace.");
            cm.dispose();
            return;
        }
        spellTraceBalance -= totalCost;
        ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance);
        cm.gainItem(selectedItemId, purchaseAmount);
        cm.sendOk("You have successfully purchased " + purchaseAmount + " " + name + "(s) for " + totalCost + " Spell Trace.");
        cm.dispose();
    }

    // =========================================================================
    // == Open Advance Shop (Formerly L6, Now L7) ==============================
    // =========================================================================
    else if (status === 1 && selection === 7) {
        status = 40;
        var text = "Please select an equipment category to view scrolls:\r\n";
        for (var i = 0; i < categoryNames.length; i++) {
             var category = categoryNames[i];
             text += "#L" + i + "# " + category + " Scrolls#l\r\n";
        }
        cm.sendSimple(text);
    }

    else if (status === 41) {
        selectedCategoryPrefix = categoryPrefix[selection];
        basicShopItems = ScrollShopManager.getShopByCategory(10, selectedCategoryPrefix);
        if (basicShopItems.length === 0) {
            cm.sendOk("No scrolls available for this category.");
            cm.dispose();
            return;
        }
        var text = "Available Scrolls for your selection:\r\n";
        for (var i = 0; i < basicShopItems.length; i++) {
            item = basicShopItems[i];
            itemId = item[0];
            var summary = item[1];
            text += "#L" + i + "##i" + itemId + "##b" + summary + "#l\r\n";
        }
        cm.sendSimple(text);
    }

    else if (status === 42) {
        selectedItemId = basicShopItems[selection][0];
        name = Packages.server.ItemInformationProvider.getInstance().getName(selectedItemId);
        cm.sendGetText("How many of #i" +selectedItemId + "##l#e#k" + name + "#n#k would you like to buy using Spell Trace? They cost 20 each.");
    }

    else if (status === 43) {
        var purchaseAmount = parseInt(cm.getText());
        if (isNaN(purchaseAmount) || purchaseAmount <= 0) {
            cm.sendOk("Please enter a valid number.");
            cm.dispose();
            return;
        }
        var itemPrice = 20;
        var totalCost = purchaseAmount * itemPrice;
        if (spellTraceBalance < totalCost) {
            cm.sendOk("You do not have enough Spell Trace.");
            cm.dispose();
            return;
        }
        spellTraceBalance -= totalCost;
        ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance);
        cm.gainItem(selectedItemId, purchaseAmount);
        cm.sendOk("You have successfully purchased " + purchaseAmount + " " + name + "(s) for " + totalCost + " Spell Trace.");
        cm.dispose();
    }

    // =========================================================================
    // == Master Shop Menu (Formerly L7, Now L8) ===============================
    // =========================================================================
    else if (status === 1 && selection === 8) {
        status = 50;
        var CHAOS_SCROLL = 2049100;
        var WHITE_SCROLL = 2340000;
        var PRICE = 100;

        var text = "Master Scroll Shop\r\n";
        text += "Your Spell Trace: #b" + spellTraceBalance + "#k\r\n\r\n";
        text += "#L0##i" + CHAOS_SCROLL + "# #bChaos Scroll#k  #d(" + PRICE + " Spell Trace)#k#l\r\n";
        text += "#L1##i" + WHITE_SCROLL + "# #bWhite Scroll#k  #d(" + PRICE + " Spell Trace)#k#l\r\n";
        cm.sendSimple(text);
    }

    else if (status === 51) {
        var CHAOS_SCROLL = 2049100;
        var WHITE_SCROLL = 2340000;
        var PRICE = 100;
        if (selection === 0) selectedItemId = CHAOS_SCROLL;
        else if (selection === 1) selectedItemId = WHITE_SCROLL;
        else {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        }
        name = Packages.server.ItemInformationProvider.getInstance().getName(selectedItemId);
        var maxAffordable = Math.floor(spellTraceBalance / PRICE);
        if (maxAffordable <= 0) {
            cm.sendOk("You do not have enough Spell Trace.\r\n\r\nCost: #b" + PRICE + "#k each\r\nYou have: #r" + spellTraceBalance + "#k");
            cm.dispose();
            return;
        }
        cm.sendGetNumber(
            "How many of #i" + selectedItemId + "# #b" + name + "#k would you like to buy?\r\n\r\n" +
            "Cost: #b" + PRICE + "#k Spell Trace each\r\n" +
            "Your Spell Trace: #b" + spellTraceBalance + "#k\r\n" +
            "You can afford up to: #e#b" + maxAffordable + "#n#k\r\n",
            1, 1, maxAffordable
        );
    }

    else if (status === 52) {
        var PRICE = 100;
        var qty = selection;
        if (qty <= 0) { cm.sendOk("Invalid quantity."); cm.dispose(); return; }
        var totalCost = qty * PRICE;
        if (spellTraceBalance < totalCost) {
            cm.sendOk("You do not have enough Spell Trace."); cm.dispose(); return;
        }
        spellTraceBalance -= totalCost;
        ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), spellTraceBalance);
        var relGain = awardRelationshipFromSpend(totalCost);
        cm.gainItem(selectedItemId, qty);
        var itemName = Packages.server.ItemInformationProvider.getInstance().getName(selectedItemId);
        cm.sendOk("Purchased #b" + qty + "#k #i" + selectedItemId + "# #b" + itemName + "#k\r\n" +
            "Cost: #b" + totalCost + "#k Spell Trace\r\n" +
            "Remaining Spell Trace: #b" + spellTraceBalance + "#k");
        cm.dispose();
    }

    // =========================================================================
    // == Relationship Empowerment (Formerly L8, Now L9) =======================
    // =========================================================================
    else if (status === 1 && selection === 9) {
        status = 60;
        cm.sendGetText("How many Spell Trace would you like to donate? Your current Spell Trace balance is: #b" + spellTraceBalance + "#k.");
    }

    else if (status === 61) {
        withdrawAmount = parseInt(cm.getText());
        if (isNaN(withdrawAmount) || withdrawAmount <= 0 || withdrawAmount > spellTraceBalance) {
            cm.sendOk("Invalid amount.");
            cm.dispose();
            return;
        }
        var newRelationshipValue = relationshipValue + (withdrawAmount * 1);
        var newSpellTraceBalance = spellTraceBalance - withdrawAmount;

        function getRelationshipMessage(val) {
            if (val < 1000) return "Starting your journey, keep donating to grow your bond!";
            if (val < 2000) return "You're building a strong relationship, keep it up!";
            if (val < 3000) return "Your relationship is solid, you're on a great path!";
            if (val < 4000) return "You're well on your way, your bond is growing stronger!";
            if (val < 5000) return "You're becoming an important ally, great progress!";
            if (val < 10000) return "A valued partner, your relationship is truly remarkable!";
            return "You're a legendary partner, your bond is unbreakable!";
        }

        ScrollShopManager.updateSpellTraceBalance(cm.getPlayer().getId(), newSpellTraceBalance);
        ScrollShopManager.updateRelationshipValue(cm.getPlayer().getId(), newRelationshipValue);

        cm.sendOk("You have successfully donated " + withdrawAmount + " Spell Trace. #b" + getRelationshipMessage(newRelationshipValue) + "#k. Your Spell Trace balance is: #b" + newSpellTraceBalance + "#k.");
        cm.dispose();
    }
}