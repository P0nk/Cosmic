/*
 * NPC: Menma (Dual-Service)
 * Functionality:
 *   1. Ore Pouch System - Store and withdraw ores, powders, stimulators.
 *   2. Potion Bank System - Consolidate all healing potions/food into HP/MP storage and withdraw standardized potions.
 *
 * Author: MerogieMS (Tom)
 * Notes:
 *   - Fully backend-linked (no NPCConversationManager edits needed).
 *   - Compatible with OrePouchManager.java and PotionBankManager.java.
 *   - Uses ASCII/bold UI formatting (no emojis).
 */

// ============================================================================
// == ORE POUCH SECTION ========================================================
// ============================================================================

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
    4130023: "Katara Forging Stimulator",

// ================= FOOD TIER ITEMS =================
    // Tier 1
    4036173: "Food_T1_01",
    4036174: "Food_T1_02",

    // Tier 2
    4036175: "Food_T2_01",
    4036176: "Food_T2_02",
    4036177: "Food_T2_03",

    // Tier 3
    4036178: "Food_T3_01",
    4036179: "Food_T3_02",
    4036180: "Food_T3_03",
    4036181: "Food_T3_04",
    4036182: "Food_T3_05",
    4036183: "Food_T3_06",

    // Tier 4
    4036184: "Food_T4_01",
    4036185: "Food_T4_02",
    4036186: "Food_T4_03",
    4036187: "Food_T4_04",
    4036188: "Food_T4_05",
    4036189: "Food_T4_06",

    // Tier 5
    4036190: "Food_T5_01",
    4036191: "Food_T5_02",
    4036192: "Food_T5_03",
    4036193: "Food_T5_04",
    4036194: "Food_T5_05",
    4036195: "Food_T5_06",
    4036196: "Food_T5_07",
    4036197: "Food_T5_08",
    4036198: "Food_T5_09",
    4036199: "Food_T5_10",
    4036200: "Food_T5_11",

    // Tier 6
    4036201: "Food_T6_01",
    4036202: "Food_T6_02",
    4036203: "Food_T6_03",
    4036204: "Food_T6_04",
    4036205: "Food_T6_05",
    4036206: "Food_T6_06",
    4036207: "Food_T6_07",
    4036208: "Food_T6_08",
    4036209: "Food_T6_09",
    4036210: "Food_T6_10",

    // Tier 7
    4036211: "Food_T7_01",
    4036212: "Food_T7_02",
    4036213: "Food_T7_03",
    4036214: "Food_T7_04",
    4036215: "Food_T7_05",
    4036216: "Food_T7_06",
    4036217: "Food_T7_07",
    4036218: "Food_T7_08",
    4036219: "Food_T7_09",
    4036220: "Food_T7_10",
    4036221: "Food_T7_11",
    4036222: "Food_T7_12",
    4036223: "Food_T7_13",
    4036224: "Food_T7_14",
    4036225: "Food_T7_15",

    // Tier 8
    4036226: "Food_T8_01",
    4036227: "Food_T8_02",
    4036228: "Food_T8_03",
    4036229: "Food_T8_04",
    4036230: "Food_T8_05",
    4036231: "Food_T8_06",
    4036232: "Food_T8_07",
    4036233: "Food_T8_08",
    4036234: "Food_T8_09",
    4036235: "Food_T8_10",
    4036236: "Food_T8_11",
    4036237: "Food_T8_12",
    4036238: "Food_T8_13",
    4036239: "Food_T8_14",
    4036240: "Food_T8_15",

    // Tier 9
    4036241: "Food_T9_01",
    4036242: "Food_T9_02",
    4036243: "Food_T9_03",
    4036244: "Food_T9_04",
    4036245: "Food_T9_05",
    4036246: "Food_T9_06",
    4036247: "Food_T9_07",
    4036248: "Food_T9_08",
    4036249: "Food_T9_09",
    4036250: "Food_T9_10",
    4036251: "Food_T9_11",
    4036252: "Food_T9_12",
    4036253: "Food_T9_13",
    4036254: "Food_T9_14",
    4036255: "Food_T9_15",
    4036256: "Food_T9_16",
    4036257: "Food_T9_17",
    4036258: "Food_T9_18",
    4036259: "Food_T9_19",
    4036260: "Food_T9_20",
    4036261: "Food_T9_21",
    4036262: "Food_T9_22",
    4036263: "Food_T9_23",
    4036264: "Food_T9_24",
    4036265: "Food_T9_25",
    4036266: "Food_T9_26",

    // Tier 10
    4036267: "Food_T10_01",
    4036268: "Food_T10_02",
    4036269: "Food_T10_03",
    4036270: "Food_T10_04",
    4036271: "Food_T10_05",
    4036272: "Food_T10_06",
    4036273: "Food_T10_07",
    4036274: "Food_T10_08",
    4036275: "Food_T10_09",
    4036276: "Food_T10_10",
    4036277: "Food_T10_11",
    4036278: "Food_T10_12",
    4036279: "Food_T10_13",
    4036280: "Food_T10_14",
    4036281: "Food_T10_15",
    4036282: "Food_T10_16",
    4036283: "Food_T10_17",
    4036284: "Food_T10_18",
    4036285: "Food_T10_19",
    4036286: "Food_T10_20",
    4036287: "Food_T10_21",
    4036288: "Food_T10_22",

    // Legendary
    4036289: "Food_Legendary_01",
    4036290: "Food_Legendary_02",
    4036291: "Food_Legendary_03",
    4036292: "Food_Legendary_04",
    4036293: "Food_Legendary_05",
    4036294: "Food_Legendary_06",
    4036295: "Food_Legendary_07",
    4036296: "Food_Legendary_08",
    4036297: "Food_Legendary_09",
    4036298: "Food_Legendary_10",
    4036299: "Food_Legendary_11",
    4036300: "Food_Legendary_12",
    4036301: "Food_Legendary_13",
    4036302: "Food_Legendary_14",
    4036303: "Food_Legendary_15",
    4036304: "Food_Legendary_16",
    4036305: "Food_Legendary_17",
    4036306: "Food_Legendary_18",
    4036307: "Food_Legendary_19"

};

// All ore item IDs
var ores = Object.keys(oreNames).map(function (id) {
    return parseInt(id);
});

// Java class helpers
var JavaShort = Java.type("java.lang.Short");
var ItemInfo = Packages.server.ItemInformationProvider.getInstance();
var potionBank = Packages.server.potionbank.PotionBankManager;

var status = 0;
var selectedIndex = -1;
var selectedService = -1;
var withdrawAmount = 0;
var prevSelection = -1;

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
        var text = "Hello, I'm Menma!\r\n\r\n";
        text += "#bSelect a service to use:#k\r\n";
        text += "#L0#Manage Ore Pouch\r\n";
        text += "#L1#Access Potion Bank\r\n";
        cm.sendSimple(text);
    }

    // =========================================================================
    // == ORE POUCH SERVICE ====================================================
    // =========================================================================
    else if (status === 1 && selection === 0) {
        selectedService = 0;
        var text = "Ore Pouch Options:\r\n\r\n";
        text += "#L10#Deposit all ores, powders, and stimulators\r\n";
        text += "#L11#Withdraw items from pouch\r\n";
        cm.sendSimple(text);
    }

    // Ore deposit
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

        var msg = "Deposited " + deposited + " ores into your pouch.";
        if (skipped.length > 0) {
            msg += "\r\nCould not store: " + skipped.join(", ") + " (limit 32,767 each)";
        }
        cm.sendOk(msg);
        cm.dispose();
    }

    // Ore withdraw menu
    else if (status === 2 && selectedService === 0 && selection === 11) {
        var pouch = cm.getPlayer().getOrePouch();
        if (pouch.size() === 0) {
            cm.sendOk("Your Ore Pouch is empty.");
            cm.dispose();
            return;
        }

        var text = "Select the ore to withdraw:\r\n";
        for (var i = 0; i < pouch.size(); i++) {
            var item = pouch.get(i);
            var itemId = item.getItemId();
            var name = oreNames[itemId] || ItemInfo.getName(itemId);
            text += "#L" + i + "##v" + itemId + "# " + name + " (" + item.getQuantity() + ")\r\n";
        }
        cm.sendSimple(text);
    }

    // Ore quantity prompt
    else if (status === 3 && selectedService === 0) {
        var pouch = cm.getPlayer().getOrePouch();

        if (selection < 0 || selection >= pouch.size()) {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        }

        selectedIndex = selection;
        var item = pouch.get(selection);
        var itemId = item.getItemId();
        var name = oreNames[itemId] || ItemInfo.getName(itemId);
        var quantity = item.getQuantity();

        cm.sendGetNumber("How many #b" + name + "#k to withdraw?\r\n(Max: " + quantity + ")", quantity, 1, quantity);
    }

    // Ore withdrawal execution
    else if (status === 4 && selectedService === 0) {
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
        var name = oreNames[itemId] || ItemInfo.getName(itemId);

        if (withdrawAmount <= 0 || withdrawAmount > quantity) {
            cm.sendOk("Invalid quantity.");
            cm.dispose();
            return;
        }

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
// =========================================================================
// == POTION BANK SERVICE ==================================================
// =========================================================================
// 🔧 CHANGE: Removed "Check balance" option since summary already shows balance
else if (status === 1 && selection === 1) {
    selectedService = 1;
    var hp = potionBank.getBankedHP(cm.getPlayer().getId());
    var mp = potionBank.getBankedMP(cm.getPlayer().getId());
    var text = "Potion Bank Summary:\r\n\r\n";
    text += "Stored HP: #b" + hp + "#k\r\n";
    text += "Stored MP: #b" + mp + "#k\r\n\r\n";
    text += "#L20#Deposit all healing items\r\n";
    text += "#L21#Withdraw potions\r\n";
    cm.sendSimple(text);
}

// Deposit healing items - now shows preview before confirming
// 🔧 CHANGE: use .get() for Java Map keys, improved formatting and warning
else if (status === 2 && selectedService === 1 && selection === 20) {
    prevSelection = selection; // remember this was a deposit action
    var preview = cm.getPlayer().previewConsolidatePotions();
    if (preview == null) {
        cm.sendOk("No healing items found in your inventory.");
        cm.dispose();
        return;
    }

    var detail  = preview.get("detailText");
    var totalHP = preview.get("totalHP");
    var totalMP = preview.get("totalMP");

    var msg = "These items will be consolidated into your Potion Bank:\r\n\r\n";
    msg += detail + "\r\n";
    msg += "#bEstimated HP gained: " + totalHP + "#k\r\n";
    msg += "#bEstimated MP gained: " + totalMP + "#k\r\n";
    msg += "\r\n#rWARNING: Any total exceeding 2,000,000,000 HP or MP will be voided!#k\r\n\r\n";
    msg += "Proceed with consolidation?";
    cm.sendYesNo(msg);
}

// Confirm deposit (only if player came from Deposit path)
else if (status === 3 && selectedService === 1 && prevSelection === 20) {
    cm.getPlayer().consolidatePotions();
    cm.sendOk("All food and potions have been deposited into your Potion Bank.");
    prevSelection = -1; // reset
    cm.dispose();
}

// Withdraw potion menu
else if (status === 2 && selectedService === 1 && selection === 21) {
    prevSelection = 21; // track which branch player is in
    var list = "Select a potion to withdraw:\r\n";
    list += "-----------------------------------------\r\n";
    list += "#L0##v2000000# Red Potion (50 HP)\r\n";
    list += "#L1##v2000001# Orange Potion (150 HP)\r\n";
    list += "#L2##v2000002# White Potion (300 HP)\r\n";
    list += "#L3##v2000003# Blue Potion (100 MP)\r\n";
    list += "#L4##v2000006# Mana Elixir (300 MP)\r\n";
    cm.sendSimple(list);
}

// Potion type selection (withdraw)
else if (status === 3 && selectedService === 1 && prevSelection === 21) {
    var potions = [
        [2000000, 50, "HP"],
        [2000001, 150, "HP"],
        [2000002, 300, "HP"],
        [2000003, 100, "MP"],
        [2000006, 300, "MP"]
    ];
    selectedIndex = selection;
    var potion = potions[selectedIndex];
    if (!potion) {
        cm.sendOk("Invalid selection.");
        cm.dispose();
        return;
    }
    cm.sendGetNumber("How many #b" + ItemInfo.getName(potion[0]) + "#k would you like to withdraw?", 1, 1, 32000);
}

// Potion withdrawal execution (actual withdrawal)
else if (status === 4 && selectedService === 1 && prevSelection === 21) {
    var potions = [
        [2000000, 50, "HP"],
        [2000001, 150, "HP"],
        [2000002, 300, "HP"],
        [2000003, 100, "MP"],
        [2000006, 300, "MP"]
    ];
    var potion = potions[selectedIndex];
    var num = selection;
    if (!potion) {
        cm.sendOk("Invalid selection.");
        cm.dispose();
        return;
    }

    num = Math.max(1, Math.min(num, 32000));

    var itemId = potion[0];
    var heal = potion[1];
    var type = potion[2];
    var total = heal * num;

    var success = (type === "HP")
        ? potionBank.withdrawHP(cm.getPlayer(), total)
        : potionBank.withdrawMP(cm.getPlayer(), total);

    if (success) {
        cm.gainItem(itemId, num);
        cm.sendOk("Withdrawn " + num + "x " + ItemInfo.getName(itemId) +
            ". Total " + type + " deducted: " + total + ".");
    } else {
        cm.sendOk("Not enough " + type + " stored.");
    }
    prevSelection = -1; // ✅ reset after transaction
    cm.dispose();
}



}
