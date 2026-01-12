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
//    4130000: "Gloves Production Stimulator",
//    4130001: "Shoes Production Stimulator",
//    4130002: "One-Handed Sword Forging Stimulator",
//    4130003: "One-Handed Axe Forging Stimulator",
//    4130004: "One-Handed Blunt Weapon Forging Stimulator",
//    4130005: "Two-Handed Sword Forging Stimulator",
//    4130006: "Two-Handed Axe Forging Stimulator",
//    4130007: "Two-Handed Mace Forging Stimulator",
//    4130008: "Spear Forging Stimulator",
//    4130009: "Pole Arm Forging Stimulator",
//    4130010: "Wand Production Stimulator",
//    4130011: "Staff Production Stimulator",
//    4130012: "Bow Production Stimulator",
//    4130013: "Crossbow Production Stimulator",
//    4130014: "Dagger Forging Stimulator",
//    4130015: "Claw Production Stimulator",
//    4130016: "Knuckler Production Stimulator",
//    4130017: "Gun Production Stimulator",
//    4130018: "Armor Production Stimulator",
//    4130019: "Topwear Production Stimulator",
//    4130020: "Bottomwear Production Stimulator",
//    4130021: "Overall Production Stimulator",
//    4130022: "Shield Production Stimulator",
//    4130023: "Katara Forging Stimulator"
};

var foodName = {
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

// Returns max quantity player can withdraw right now for this itemId,
// bounded by storedQty and maxCap (usually 32000).
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
    // == ORE POUCH SERVICE ====================================================
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

        var msg = "O-oh! I... deposited " + deposited + " ores into your pouch. I-I hope that's okay...";
        if (skipped.length > 0) {
            msg += "\r\nCould not store: " + skipped.join(", ") + " (limit 32,767 each)";
        }
        cm.sendOk(msg);
        cm.dispose();
    }

    // Ore withdraw menu (shows eligibility)
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

    // Ore quantity prompt (uses eligibility)
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

    // Ore withdrawal execution
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

    // withdraw ALL ores
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

/* * Updated Potion Bank
 * 1. Includes Blacklist (Gelt, Apple, Graham Pie) to ignore during Deposit.
 * 2. Updated Withdrawal list with specific IDs and Heal values.
 * 3. Withdrawal now shows "Stock available" (Qty) instead of raw HP/MP.
 */

// =========================================================================
// == CONFIGURATION ========================================================
// =========================================================================

// Items to IGNORE during deposit
var blacklist = [
    2022174, // Gelt Chocolate
    2022179, // Onyx Apple
    2022143  // Graham Pie (Verify this ID for your version, sometimes 2022003)
];

// Available Potions for Withdrawal
// Format: [ItemID, HealAmount, "Type"]
var potionList = [
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
    [2022022, 500, "MP"],  // Fish Cake
    [2022051, 800, "MP"],  // Buckwheat Paste
    [2022210, 1600, "MP"], // Dragon Fruit
    [2001002, 2000, "MP"], // Red Bean Sundae
    [2022211, 3200, "MP"], // Durian
    [2020014, 4050, "MP"], // Sunrise Dew
    [2020015, 5000, "MP"]  // Sunset Dew
];

// Helper to get Item Information Provider (Standard v83)
// If your server calls it differently, adjust this.
var ii = server.MapleItemInformationProvider.getInstance();


// =========================================================================
// == MAIN SCRIPT ==========================================================
// =========================================================================

if (status === 1 && selection === 1) {
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

// -------------------------------------------------------------------------
// -- DEPOSIT LOGIC (Rewritten in JS to support Blacklist) -----------------
// -------------------------------------------------------------------------

else if (status === 2 && selectedService === 1 && selection === 20) {
    prevSelection = 20;

    // Calculate deposit manually to exclude Blacklist
    var inv = cm.getInventory(2); // Use Inventory
    var iter = inv.list().iterator();

    var totalHP = 0;
    var totalMP = 0;
    var count = 0;

    // We store items to remove in a temporary list to use in the next step
    // But since we can't easily pass objects between states without session vars,
    // we will recalculate in the next step. This step is just for PREVIEW.

    while (iter.hasNext()) {
        var item = iter.next();
        var id = item.getItemId();

        // 1. Blacklist Check
        if (blacklist.indexOf(id) !== -1) {
            continue;
        }

        // 2. Check if it heals HP or MP
        // Note: usage of 'ii' defined at top
        var healHP = ii.getItemEffect(id).getHp();
        var healMP = ii.getItemEffect(id).getMp();

        if (healHP > 0 || healMP > 0) {
            var qty = item.getQuantity();
            totalHP += (healHP * qty);
            totalMP += (healMP * qty);
            count++;
        }
    }

    if (count === 0) {
        cm.sendOk("Menma can't find any potions to deposit... (or they are all blacklisted!)");
        cm.dispose();
        return;
    }

    var msg = "Menma will take care of these items for you.. (deposit into Potion Bank):\r\n\r\n";
    msg += "Items found: #b" + count + "#k\r\n";
    msg += "#bEstimated HP gained: " + totalHP + "#k\r\n";
    msg += "#bEstimated MP gained: " + totalMP + "#k\r\n";
    msg += "\r\n#rWARNING: Items will be removed from your inventory!#k\r\n";
    msg += "Proceed with consolidation?";
    cm.sendYesNo(msg);
}

// Confirm deposit (Manual JS Removal)
else if (status === 3 && selectedService === 1 && prevSelection === 20) {

    var inv = cm.getInventory(2);
    var iter = inv.list().iterator();
    var totalHP = 0;
    var totalMP = 0;

    // Iterating backwards or using a robust method is safer for removal,
    // but standard removeAll logic usually requires iterating first.
    // We will collect IDs and Quantities first to avoid ConcurrentModification.

    var toRemove = []; // [ [itemId, qty, healHP, healMP], ... ]

    while (iter.hasNext()) {
        var item = iter.next();
        var id = item.getItemId();

        if (blacklist.indexOf(id) !== -1) continue;

        var healHP = ii.getItemEffect(id).getHp();
        var healMP = ii.getItemEffect(id).getMp();

        if (healHP > 0 || healMP > 0) {
            toRemove.push([id, item.getQuantity(), healHP, healMP]);
        }
    }

    // Execute Removal and Addition
    for (var i = 0; i < toRemove.length; i++) {
        var rec = toRemove[i];
        // Remove item
        cm.gainItem(rec[0], -rec[1]);
        // Add to bank
        totalHP += (rec[2] * rec[1]);
        totalMP += (rec[3] * rec[1]);
    }

    // Update Bank
    if (totalHP > 0) potionBank.depositHP(cm.getPlayer(), totalHP); // Assuming depositHP adds to current
    if (totalMP > 0) potionBank.depositMP(cm.getPlayer(), totalMP);

    cm.sendOk("Menma has stored your potions..\r\nAdded #b" + totalHP + " HP#k and #b" + totalMP + " MP#k.");
    prevSelection = -1;
    cm.dispose();
}

// -------------------------------------------------------------------------
// -- WITHDRAW LOGIC (Updated List & Display) ------------------------------
// -------------------------------------------------------------------------

else if (status === 2 && selectedService === 1 && selection === 21) {
    prevSelection = 21;

    var list = "You want your potions back? Where did Menma put it..:\r\n";
    list += "-----------------------------------------\r\n";

    // Dynamic List Generation
    for (var i = 0; i < potionList.length; i++) {
        var pId = potionList[i][0];
        var pVal = potionList[i][1];
        var pType = potionList[i][2];

        list += "#L" + i + "##v" + pId + "# #t" + pId + "# (" + pVal + " " + pType + ")\r\n";
    }

    cm.sendSimple(list);
}

// Potion type selection (withdraw)
else if (status === 3 && selectedService === 1 && prevSelection === 21) {

    selectedIndex = selection;
    var potion = potionList[selectedIndex]; // Use the new list

    if (!potion) {
        cm.sendOk("Invalid selection.");
        cm.dispose();
        return;
    }

    var itemId = potion[0];
    var heal = potion[1];
    var pType = potion[2];

    var banked = (pType === "HP")
        ? potionBank.getBankedHP(cm.getPlayer().getId())
        : potionBank.getBankedMP(cm.getPlayer().getId());

    // Calculate how many POTIONS they can afford
    var maxQtyAfford = Math.floor(banked / heal);

    // Check Inventory Limit (Max 32000 or slot limit)
    var cap = Math.min(32000, maxQtyAfford);
    var eligible = getEligibleWithdrawByCanHold(itemId, cap, 32000); // Your existing helper

    if (eligible <= 0) {
        var reason = (maxQtyAfford <= 0)
            ? ("Not enough " + pType + " stored to make even one " + ItemInfo.getName(itemId) + ".")
            : ("Not enough inventory space for " + ItemInfo.getName(itemId) + ".");
        cm.sendOk(reason);
        cm.dispose();
        return;
    }

    // User requested: "Show them how many of that pot they can withdraw"
    cm.sendGetNumber(
        "You have selected #t" + itemId + "# (" + heal + " " + pType + ").\r\n" +
        "Current Bank Balance: " + banked + " " + pType + "\r\n" +
        "You can withdraw up to: #b" + eligible + " items#k\r\n" +
        "How many would you like?",
        eligible, 1, eligible
    );
}

// Potion withdrawal execution
else if (status === 4 && selectedService === 1 && prevSelection === 21) {

    // Re-access the list using the index saved from previous step
    var potion = potionList[selectedIndex];

    if (!potion) {
        cm.sendOk("I don't understand.. I'm sorry..");
        cm.dispose();
        return;
    }

    var itemId = potion[0];
    var heal = potion[1];
    var pType = potion[2];

    var num = selection;
    var totalCost = heal * num;

    // Double check availability (security)
    var banked = (pType === "HP")
        ? potionBank.getBankedHP(cm.getPlayer().getId())
        : potionBank.getBankedMP(cm.getPlayer().getId());

    if (banked < totalCost) {
        cm.sendOk("You don't have enough points left!");
        cm.dispose();
        return;
    }

    var success = (pType === "HP")
        ? potionBank.withdrawHP(cm.getPlayer(), totalCost)
        : potionBank.withdrawMP(cm.getPlayer(), totalCost);

    if (success) {
        cm.gainItem(itemId, num);
        cm.sendOk("Withdrawn " + num + "x #t" + itemId + "#.\r\nTotal " + pType + " deducted: " + totalCost + ".");
    } else {
        cm.sendOk("An error occurred withdrawing points.");
    }

    prevSelection = -1;
    cm.dispose();
}

    // =========================================================================
    // == FOOD BANK SERVICE ====================================================
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

    // deposit all food
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

    // single food withdraw menu (shows eligibility)
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

    // Food quantity prompt (uses eligibility)
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

    // Food withdrawal execution
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

    // withdraw ALL food
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
