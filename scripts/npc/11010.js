/*
 * NPC: Menma (Dual-Service)
 * Functionality:
 * 1. Ore Pouch System - Store and withdraw ores, powders, stimulators.
 * 2. Potion Bank System - Consolidate all healing potions/food into HP/MP storage and withdraw standardized potions.
 *
 * Author: MerogieMS (Tom)
 * Updated: To include dynamic Potion Arrays
 */

// ============================================================================
// == CONFIGURATION SECTION ====================================================
// ============================================================================

// 1. Ore Names Mapping
var oreNames = {
    4010000: "Bronze Ore", 4010001: "Steel Ore", 4010002: "Mithril Ore",
    4010003: "Adamantium Ore", 4010004: "Silver Ore", 4010005: "Orihalcon Ore",
    4010006: "Gold Ore", 4010007: "Lidium Ore", 4020000: "Garnet Ore",
    4020001: "Amethyst Ore", 4020002: "Aquamarine Ore", 4020003: "Emerald Ore",
    4020004: "Opal Ore", 4020005: "Sapphire Ore", 4020006: "Topaz Ore",
    4020007: "Diamond Ore", 4020008: "Black Crystal Ore", 4004000: "Power Crystal Ore",
    4004001: "Wisdom Crystal Ore", 4004002: "DEX Crystal Ore", 4004003: "LUK Crystal Ore",
    4004004: "Dark Crystal Ore",
    4011000: "Bronze", 4011001: "Steel", 4011002: "Mithril",
    4011003: "Adamantium", 4011004: "Silver", 4011005: "Orihalcon",
    4011006: "Gold", 4011007: "Lidium", 4021000: "Garnet",
    4021001: "Amethyst", 4021002: "Aquamarine", 4021003: "Emerald",
    4021004: "Opal", 4021005: "Sapphire", 4021006: "Topaz",
    4021007: "Diamond", 4021008: "Black Crystal", 4005000: "Power Crystal",
    4005001: "Wisdom Crystal", 4005002: "DEX Crystal", 4005003: "LUK Crystal",
    4005004: "Dark Crystal",
    4007000: "Brown Magic Powder", 4007001: "White Magic Powder",
    4007002: "Blue Magic Powder", 4007003: "Green Magic Powder",
    4007004: "Yellow Magic Powder", 4007005: "Purple Magic Powder",
    4007006: "Red Magic Powder", 4007007: "Black Magic Powder"
};

// 2. Food Names (Abbreviated for length, logic remains valid)
var foodName = {
    // ... (Keep your existing huge list of foodName here, I won't repeat it all to save space) ...
    4036173: "Food_T1_01", 4036174: "Food_T1_02", 4036175: "Food_T2_01",
    // Ensure you keep your full list here in your actual file
};

// 3. Potion Bank Configuration (New Pots Added Here)
// Format: [ItemID, HealAmount, "Type"]
var potionConfig = [
    // HP Potions
    [2000007, 50, "HP"],   // Red Pill
    [2000008, 150, "HP"],  // Orange Pill
    [2000009, 300, "HP"],  // White Pill
    [2022013, 500, "HP"],  // Sushi (Salmon)
    [2022203, 800, "HP"],  // Laksa
    [2022205, 1800, "HP"], // Carrot Cake
    [2022206, 2200, "HP"], // Chicken Rice
    [2022207, 2600, "HP"], // Satay
    [2022476, 4000, "HP"], // Chicken Kapitan
    [2020013, 5000, "HP"], // Reindeer Milk

    // MP Potions
    [2000010, 100, "MP"],  // Blue Pill
    [2000011, 300, "MP"],  // Mana Elixir Pill
    [2022022, 500, "MP"],  // Fish Cake (Dish)
    [2022051, 800, "MP"],  // Buckwheat Paste
    [2022210, 1600, "MP"], // Dragon Fruit
    [2001002, 2000, "MP"], // Red Bean Sundae
    [2022211, 3200, "MP"], // Durian
    [2020014, 4050, "MP"], // Sunrise Dew
    [2020015, 5000, "MP"]  // Sunset Dew
];


// All ore item IDs
var ores = Object.keys(oreNames).map(function (id) { return parseInt(id); });
// All food item IDs
var food_ids = Object.keys(foodName).map(function (id) { return parseInt(id); });

// Java class helpers
var JavaShort = Java.type("java.lang.Short");
var ItemInfo = Packages.server.ItemInformationProvider.getInstance();
var potionBank = Packages.server.potionbank.PotionBankManager;

var status = 0;
var selectedIndex = -1;
var selectedService = -1;
var withdrawAmount = 0;
var prevSelection = -1;

// ========================= INVENTORY WITHDRAW ELIGIBILITY HELPERS =========================

function getEligibleWithdrawByCanHold(itemId, storedQty, maxCap) {
    var hi = Math.min(storedQty, maxCap);

    if (hi <= 0) return 0;
    if (!cm.canHold(itemId, 1)) return 0;
    if (cm.canHold(itemId, hi)) return hi;

    // binary search for max
    var lo = 1;
    while (lo < hi) {
        var mid = Math.floor((lo + hi + 1) / 2);
        if (cm.canHold(itemId, mid)) lo = mid;
        else hi = mid - 1;
    }
    return lo;
}

// =========================================================================
// == SCRIPT START ==========================================================
// =========================================================================
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
        var text = "H-hello... um... I'm Menma... I-I think I... I was alive... once? But now, I'm... here... as a ghost... \r\n\n"
                 + "I... don't really know what I'm supposed to do... but... I can help you with some things? P-please be patient with me...\r\n\r\n";
        text += "#bPlease choose... I'm sorry if it's confusing.:#k\r\n";
        text += "#L0#Menma helps your keep your heavy etcs.. (Ore Pouch)\r\n";
        text += "#L1#Menma helps to mix your potions.. (Potion Bank)\r\n";
        text += "#L2#Menma keeps your food for you.. (Food Bank)\r\n";
        cm.sendSimple(text);
    }

    // =========================================================================
    // == ORE POUCH SERVICE (Unchanged) ========================================
    // =========================================================================
    else if (status === 1 && selection === 0) {
        selectedService = 0;
        var text = "O-oh! You want to store... ores? Or take them out? M-maybe I can help...\r\n\r\n"
                 + "I'm not sure how much I can carry, but... I'll try my best...";
        text += "\r\n#L10#Deposit ores and powders\r\n";
        text += "#L11#Withdraw ores\r\n";
        text += "#L12#Withdraw all ores\r\n";
        cm.sendSimple(text);
    }
    // ... [Insert all your Ore Pouch Logic here - lines 152 to 395 remain unchanged] ...
    // (For brevity in the fix, I am assuming you keep the Ore Logic exactly as it was)

    // START INSERTING ORE LOGIC HERE IF COPYING FULL FILE
    else if (status === 2 && selectedService === 0 && selection === 10) {
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
                continue;
            }

            var toDeposit = Math.min(inventoryQty, maxAllowed);
            cm.gainItem(id, JavaShort.valueOf(-toDeposit));
            cm.getPlayer().addOreToPouch(id, toDeposit);
            deposited += toDeposit;

            if (toDeposit < inventoryQty) {
                skipped.push((oreNames[id] || ("Ore (" + id + ")")) + " (partially deposited)");
            }
        }

        var msg = "O-oh! I... deposited " + deposited + " ores into your pouch. I-I hope that's okay...";
        if (skipped.length > 0) {
            msg += "\r\nCould not store: " + skipped.join(", ") + " (limit 32,767 each)";
        }
        cm.sendOk(msg);
        cm.dispose();
    }
    else if (status === 2 && selectedService === 0 && selection === 11) {
        var pouch = cm.getPlayer().getOrePouch();
        if (pouch.size() === 0) {
            cm.sendOk("Your Ore Pouch is empty... I'm sorry...");
            cm.dispose();
            return;
        }

        var text = "Y-you want to take some ores out? I... I checked your inventory...\r\n\r\n";
        var any = false;

        for (var i = 0; i < pouch.size(); i++) {
            var item = pouch.get(i);
            var itemId = item.getItemId();
            if (!oreNames.hasOwnProperty(itemId)) continue;

            var name = oreNames[itemId] || ItemInfo.getName(itemId);
            var stored = item.getQuantity();
            var eligible = getEligibleWithdrawByCanHold(itemId, stored, 32000);

            text += "#L" + i + "##v" + itemId + "# " + name
                 + "  (Stored: " + stored + " | Eligible: #b" + eligible + "#k)\r\n";
            any = true;
        }

        if (!any) {
            cm.sendOk("Your Ore Pouch is empty... I'm sorry...");
            cm.dispose();
            return;
        }

        cm.sendSimple(text);
    }
    else if (status === 3 && selectedService === 0) {
        var pouch = cm.getPlayer().getOrePouch();

        if (selection < 0 || selection >= pouch.size()) {
            cm.sendOk("I don't understand.. I'm sorry..");
            cm.dispose();
            return;
        }

        selectedIndex = selection;
        var item = pouch.get(selection);
        var itemId = item.getItemId();

        if (!oreNames.hasOwnProperty(itemId)) {
            cm.sendOk("I... can't withdraw that from here.. I'm sorry...");
            cm.dispose();
            return;
        }

        var name = oreNames[itemId] || ItemInfo.getName(itemId);
        var stored = item.getQuantity();
        var eligible = getEligibleWithdrawByCanHold(itemId, stored, 32000);

        if (eligible <= 0) {
            cm.sendOk("I... I'm so sorry, but your inventory has no space for #b" + name + "#k right now...");
            cm.dispose();
            return;
        }

        cm.sendGetNumber(
            "How many.. #b" + name + "#k should Menma get for you..?\r\n"
          + "(Stored: " + stored + " | Eligible now: #b" + eligible + "#k)",
            eligible, 1, eligible
        );
    }
    else if (status === 4 && selectedService === 0) {
        withdrawAmount = selection;

        var pouch = cm.getPlayer().getOrePouch();
        var item = pouch.get(selectedIndex);
        if (item == null) {
            cm.sendOk("Menma must have lost it.. I can't find it anymore.. I'm sorry...");
            cm.dispose();
            return;
        }

        var itemId = item.getItemId();
        var quantity = item.getQuantity();
        var name = oreNames[itemId] || ItemInfo.getName(itemId);

        var eligible = getEligibleWithdrawByCanHold(itemId, quantity, 32000);
        if (eligible <= 0) {
            cm.sendOk("I... I'm so sorry, but there isn't enough space in your inventory... Please make some room...");
            cm.dispose();
            return;
        }

        // clamp to eligible
        if (withdrawAmount <= 0 || withdrawAmount > eligible) {
            withdrawAmount = eligible;
        }

        if (cm.canHold(itemId, withdrawAmount)) {
            cm.gainItem(itemId, withdrawAmount);
            if (withdrawAmount === quantity) {
                cm.getPlayer().removeOreFromPouch(itemId);
            } else {
                item.setQuantity(quantity - withdrawAmount);
                Packages.server.inventory.OrePouchManager.saveOrePouchItems(cm.getPlayer().getId(), pouch);
            }
            cm.sendOk("Here you go.. your #b#e" + withdrawAmount + " " + name + "#n#k. Menma hopes she did a good job..");
        } else {
            cm.sendOk("I... I'm so sorry, but there isn't enough space in your inventory... Please make some room... I'll try again when you can...");
        }
        cm.dispose();
    }
    else if (status === 2 && selectedService === 0 && selection === 12) {
        var pouch = cm.getPlayer().getOrePouch();

        var i = 0;
        while (i < pouch.size() && !oreNames.hasOwnProperty(pouch.get(i).getItemId())) i++;

        if (i === pouch.size()) {
            cm.sendOk("Your Ore Pouch is empty... I'm sorry...");
            cm.dispose();
            return;
        }

        var withdrawn = 0;

        for (var j = pouch.size() - 1; j >= 0; j--) {
            var item = pouch.get(j);
            var itemId = item.getItemId();
            if (!oreNames.hasOwnProperty(itemId)) continue;

            var qty = item.getQuantity();

            if (!cm.canHold(itemId, qty)) {
                cm.sendOk("I... I'm so sorry, but there isn't enough space in your inventory... Please make some room... I'll try again when you can...");
                cm.dispose();
                return;
            }

            cm.gainItem(itemId, qty);
            cm.getPlayer().removeOreFromPouch(itemId);
            withdrawn += qty;
        }

        cm.sendOk("Menma's given you all the items from the pouch.. I hope I didn't miss anything..\r\nTotal items withdrawn: " + withdrawn + ".");
        cm.dispose();
    }

    // =========================================================================
    // == POTION BANK SERVICE (UPDATED) ========================================
    // =========================================================================
    else if (status === 1 && selection === 1) {
        selectedService = 1;

        var hp = potionBank.getBankedHP(cm.getPlayer().getId());
        var mp = potionBank.getBankedMP(cm.getPlayer().getId());

        var text = "U-um, I think I can help with potions... I... I don't really know much about these things, but I hope I can help...\r\n"
                 + "\r\n#e#r[Potion Bank Summary]#n#k\r\n\r\n";
        text += "Stored HP: #b" + hp + "#k\r\n";
        text += "Stored MP: #b" + mp + "#k\r\n\r\n";
        text += "#L20#Deposit all healing items\r\n";
        text += "#L21#Withdraw potions\r\n";
        cm.sendSimple(text);
    }

    // Deposit healing items - preview before confirm
    else if (status === 2 && selectedService === 1 && selection === 20) {
        prevSelection = 20;

        var preview = cm.getPlayer().previewConsolidatePotions();
        if (preview == null) {
            cm.sendOk("Menma can't find any.. I'm sorry...");
            cm.dispose();
            return;
        }

        var detail  = preview.get("detailText");
        var totalHP = preview.get("totalHP");
        var totalMP = preview.get("totalMP");

        var msg = "Menma will take care of these items for you.. (deposit into Potion Bank):\r\n\r\n";
        msg += detail + "\r\n";
        msg += "#bEstimated HP gained: " + totalHP + "#k\r\n";
        msg += "#bEstimated MP gained: " + totalMP + "#k\r\n";
        msg += "\r\n#rWARNING: Any total exceeding 2,000,000,000 HP or MP will be voided!#k\r\n\r\n";
        msg += "Proceed with consolidation?";
        cm.sendYesNo(msg);
    }

    // Confirm deposit
    else if (status === 3 && selectedService === 1 && prevSelection === 20) {
        cm.getPlayer().consolidatePotions();
        cm.sendOk("Menma has stored your potions.. I hope that's okay..");
        prevSelection = -1;
        cm.dispose();
    }

    // Withdraw potion menu (DYNAMIC GENERATION)
    else if (status === 2 && selectedService === 1 && selection === 21) {
        prevSelection = 21;

        var list = "You want your potions back? Where did Menma put it..:\r\n";
        list += "-----------------------------------------\r\n";

        for (var i = 0; i < potionConfig.length; i++) {
            var item = potionConfig[i];
            var pid = item[0];
            var pval = item[1];
            var ptype = item[2];

            // Format: #L0##v2000007# Red Pill (50 HP)
            list += "#L" + i + "##v" + pid + "# " + ItemInfo.getName(pid) + " (" + pval + " " + ptype + ")\r\n";
        }
        cm.sendSimple(list);
    }

    // Potion type selection (withdraw)
    else if (status === 3 && selectedService === 1 && prevSelection === 21) {
        // Validation
        if (selection < 0 || selection >= potionConfig.length) {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        }

        selectedIndex = selection;
        var potion = potionConfig[selectedIndex]; // Use global config

        var itemId = potion[0];
        var heal = potion[1];
        var pType = potion[2];

        var banked = (pType === "HP")
            ? potionBank.getBankedHP(cm.getPlayer().getId())
            : potionBank.getBankedMP(cm.getPlayer().getId());

        var bankMax = Math.floor(banked / heal);
        var cap = Math.min(32000, bankMax);
        var eligible = getEligibleWithdrawByCanHold(itemId, cap, 32000);

        if (eligible <= 0) {
            var reason = (bankMax <= 0)
                ? ("Not enough " + pType + " stored.")
                : ("Not enough inventory space for " + ItemInfo.getName(itemId) + ".");
            cm.sendOk(reason);
            cm.dispose();
            return;
        }

        cm.sendGetNumber(
            "How many #b" + ItemInfo.getName(itemId) + "#k should Menma withdraw?\r\n"
          + "(Stored " + pType + ": " + banked + " | Eligible now: #b" + eligible + "#k)",
            eligible, 1, eligible
        );
    }

    // Potion withdrawal execution
    else if (status === 4 && selectedService === 1 && prevSelection === 21) {
        // Validation
         if (selectedIndex < 0 || selectedIndex >= potionConfig.length) {
            cm.sendOk("I don't understand.. I'm sorry..");
            cm.dispose();
            return;
        }

        var potion = potionConfig[selectedIndex];
        var itemId = potion[0];
        var heal = potion[1];
        var pType = potion[2];

        var banked = (pType === "HP")
            ? potionBank.getBankedHP(cm.getPlayer().getId())
            : potionBank.getBankedMP(cm.getPlayer().getId());

        var bankMax = Math.floor(banked / heal);
        var cap = Math.min(32000, bankMax);
        var eligible = getEligibleWithdrawByCanHold(itemId, cap, 32000);

        if (eligible <= 0) {
            cm.sendOk("Not enough " + pType + " stored, or not enough inventory space.");
            prevSelection = -1;
            cm.dispose();
            return;
        }

        var num = selection;
        if (num <= 0) num = 1;
        if (num > eligible) num = eligible;

        var total = heal * num;

        var success = (pType === "HP")
            ? potionBank.withdrawHP(cm.getPlayer(), total)
            : potionBank.withdrawMP(cm.getPlayer(), total);

        if (success) {
            cm.gainItem(itemId, num);
            cm.sendOk("Withdrawn " + num + "x " + ItemInfo.getName(itemId) + ".\r\nTotal " + pType + " deducted: " + total + ".");
        } else {
            cm.sendOk("Not enough " + pType + " stored.");
        }

        prevSelection = -1;
        cm.dispose();
    }

    // =========================================================================
    // == FOOD BANK SERVICE (Unchanged) ========================================
    // =========================================================================
    else if (status === 1 && selection === 2) {
        selectedService = 2;

        var text = "Food... food is something I understand... well... not really, but it’s important, right?\r\n"
                 + "I-I can help store some... but, please, be gentle with me...\r\n\r\n"
                 + "Food bank Options:\r\n\r\n";
        text += "#L20#Deposit all food\r\n";
        text += "#L21#Withdraw food from bank\r\n";
        text += "#L22#Withdraw all food\r\n";
        cm.sendSimple(text);
    }
    // ... [Insert all Food Bank Logic here lines 566 to 740 remain unchanged] ...
    else if (status === 2 && selectedService === 2 && selection === 20) {
        var deposited = 0;
        var skipped = [];
        var pouch = cm.getPlayer().getOrePouch();

        for (var i = 0; i < food_ids.length; i++) {
            var id = food_ids[i];
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
                skipped.push(foodName[id] || ("Food (" + id + ")"));
                continue;
            }

            var toDeposit = Math.min(inventoryQty, maxAllowed);
            cm.gainItem(id, JavaShort.valueOf(-toDeposit));
            cm.getPlayer().addOreToPouch(id, toDeposit);
            deposited += toDeposit;

            if (toDeposit < inventoryQty) {
                skipped.push((foodName[id] || ("Food (" + id + ")")) + " (partially deposited)");
            }
        }

        var msg = "Deposited " + deposited + " food into the food bank.";
        if (skipped.length > 0) {
            msg += "\r\nCould not store: " + skipped.join(", ") + " (limit 32,767 each)";
        }
        cm.sendOk(msg);
        cm.dispose();
    }
    else if (status === 2 && selectedService === 2 && selection === 21) {
        var pouch = cm.getPlayer().getOrePouch();

        var has_food = false;
        for (var k = 0; k < pouch.size(); k++) {
            if (foodName.hasOwnProperty(pouch.get(k).getItemId())) { has_food = true; break; }
        }

        if (!has_food) {
            cm.sendOk("Your Food bank is empty.");
            cm.dispose();
            return;
        }

        var text = "Select the food to withdraw (Menma checked your inventory space):\r\n\r\n";
        for (var i = 0; i < pouch.size(); i++) {
            var item = pouch.get(i);
            var itemId = item.getItemId();
            if (!foodName.hasOwnProperty(itemId)) continue;

            var name = foodName[itemId] || ItemInfo.getName(itemId);
            var stored = item.getQuantity();
            var eligible = getEligibleWithdrawByCanHold(itemId, stored, 32000);

            text += "#L" + i + "##v" + itemId + "# " + name
                 + "  (Stored: " + stored + " | Eligible: #b" + eligible + "#k)\r\n";
        }

        cm.sendSimple(text);
    }
    else if (status === 3 && selectedService === 2) {
        var pouch = cm.getPlayer().getOrePouch();

        if (selection < 0 || selection >= pouch.size()) {
            cm.sendOk("I don't understand.. I'm sorry..");
            cm.dispose();
            return;
        }

        selectedIndex = selection;
        var item = pouch.get(selection);
        var itemId = item.getItemId();

        if (!foodName.hasOwnProperty(itemId)) {
            cm.sendOk("I... can't withdraw that from here.. I'm sorry...");
            cm.dispose();
            return;
        }

        var name = foodName[itemId] || ItemInfo.getName(itemId);
        var stored = item.getQuantity();
        var eligible = getEligibleWithdrawByCanHold(itemId, stored, 32000);

        if (eligible <= 0) {
            cm.sendOk("Not enough inventory space to withdraw #b" + name + "#k right now.");
            cm.dispose();
            return;
        }

        cm.sendGetNumber(
            "How many #b" + name + "#k to withdraw?\r\n"
          + "(Stored: " + stored + " | Eligible now: #b" + eligible + "#k)",
            eligible, 1, eligible
        );
    }
    else if (status === 4 && selectedService === 2) {
        withdrawAmount = selection;

        var pouch = cm.getPlayer().getOrePouch();
        var item = pouch.get(selectedIndex);
        if (item == null) {
            cm.sendOk("This item no longer exists.");
            cm.dispose();
            return;
        }

        var itemId = item.getItemId();
        var quantity = item.getQuantity();
        var name = foodName[itemId] || ItemInfo.getName(itemId);

        var eligible = getEligibleWithdrawByCanHold(itemId, quantity, 32000);
        if (eligible <= 0) {
            cm.sendOk("Not enough space in inventory.");
            cm.dispose();
            return;
        }

        // clamp
        if (withdrawAmount <= 0 || withdrawAmount > eligible) withdrawAmount = eligible;

        if (cm.canHold(itemId, withdrawAmount)) {
            cm.gainItem(itemId, withdrawAmount);
            if (withdrawAmount === quantity) {
                cm.getPlayer().removeOreFromPouch(itemId);
            } else {
                item.setQuantity(quantity - withdrawAmount);
                Packages.server.inventory.OrePouchManager.saveOrePouchItems(cm.getPlayer().getId(), pouch);
            }
            cm.sendOk("Withdrawn " + withdrawAmount + " of " + name + ".");
        } else {
            cm.sendOk("Not enough space in inventory.");
        }
        cm.dispose();
    }
    else if (status === 2 && selectedService === 2 && selection === 22) {
        var pouch = cm.getPlayer().getOrePouch();

        var i = 0;
        while (i < pouch.size() && !foodName.hasOwnProperty(pouch.get(i).getItemId())) i++;

        if (i === pouch.size()) {
            cm.sendOk("Your Food bank is empty.");
            cm.dispose();
            return;
        }

        var withdrawn = 0;

        for (var j = pouch.size() - 1; j >= 0; j--) {
            var item = pouch.get(j);
            var itemId = item.getItemId();
            if (!foodName.hasOwnProperty(itemId)) continue;

            var qty = item.getQuantity();

            if (!cm.canHold(itemId, qty)) {
                cm.sendOk("Not enough inventory space to withdraw all food.");
                cm.dispose();
                return;
            }

            cm.gainItem(itemId, qty);
            cm.getPlayer().removeOreFromPouch(itemId);
            withdrawn += qty;
        }

        cm.sendOk("Withdrawn all food from the Food bank.\r\nTotal items withdrawn: " + withdrawn + ".");
        cm.dispose();
    }
}