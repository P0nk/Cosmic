/*
 * Otto the Scroller (Redux)
 * Persona: The Sage Blacksmith
 * Optimized for readability, efficiency, and atmosphere.
 */

var status = 0;
var ii = Packages.server.ItemInformationProvider.getInstance();

// ================= CONSTANTS =================
var INV_EQUIP = 1;
var INV_USE = 2;
var INV_CASH = 5;

var ID_HAMMER = 5570000;
var ID_WHITE_SCROLL = 2340000;
var COST_HAMMER = 10000;

// ================= STATE VARS =================
var selectedMode = -1; // 0: Scroll, 1: Hammer, 2: Buy
var targetSlot = -1;
var scrollSlot = -1;

// ================= ENTRY POINT =================
function start() {
    status = 0;
    selectedMode = -1;
    targetSlot = -1;
    scrollSlot = -1;
    cm.sendNext("Greetings, traveler. I am Otto. The spirits of the anvil whisper to me...");
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }
    status++;

    try {
        if (status === 1) {
            cm.sendSimple("The potential within your equipment is vast, yet dormant. How shall we shape destiny today?\r\n" +
                "#b#L0#Awaken equipment (Auto-Scroll)#l\r\n" +
                "#b#L1#Expand capacity (Vicious Hammer)#l\r\n" +
                "#L2#Acquire ancient tools (Buy Hammers - 10k NX)#l");
        }
        else if (status === 2) {
            selectedMode = selection;
            if (selectedMode === 0) return listScrollableEquips();
            if (selectedMode === 1) return listHammerableEquips();
            if (selectedMode === 2) {
                cm.sendGetText("A wise investment. How many Hammers do you require?", "1");
                status = 9; // Jump to purchase logic
            }
        }
        else if (status === 3) {
            if (selectedMode === 0) return listApplicableScrolls(selection);
            if (selectedMode === 1) return confirmHammering();
        }
        else if (status === 4) {
            if (selectedMode === 0) return confirmScroll(selection);
            if (selectedMode === 1) return executeHammer();
        }
        else if (status === 5) {
            if (selectedMode === 0) return executeScroll();
        }

        // Purchase Branch
        else if (status === 10) {
            return executePurchase();
        }

    } catch (err) {
        cm.sendOk("The magic has been disrupted. (Error: " + err + ")");
        cm.dispose();
    }
}

// ==========================================================
//                     LOGIC: HAMMERING
// ==========================================================

function listHammerableEquips() {
    var inv = cm.getInventory(INV_EQUIP);
    var iter = inv.iterator();
    var lines = [];
    var totalNeeded = 0;

    while (iter.hasNext()) {
        var item = iter.next();
        if (isCash(item.getItemId())) continue;

        // Check if item can be hammered (max 2)
        var currentHammers = item.getVicious();
        if (currentHammers < 2) {
            var needed = 2 - currentHammers;
            totalNeeded += needed;
            var name = ii.getName(item.getItemId());
            lines.push("#v" + item.getItemId() + "# #b" + name + "#k (Needs: " + needed + ")");
        }
    }

    if (lines.length === 0) {
        cm.sendOk("You possess no artifacts that require the Vicious Hammer's touch.");
        cm.dispose();
        return;
    }

    cm.sendYesNo("These items can be expanded. Doing so requires #b" + totalNeeded + " Vicious Hammers#k in total.\r\n\r\n" + lines.join("\r\n"));
}

function confirmHammering() {
    var inv = cm.getInventory(INV_EQUIP);
    var cashInv = cm.getInventory(INV_CASH);

    // Calculate Needs
    var needed = 0;
    var iter = inv.iterator();
    while (iter.hasNext()) {
        var item = iter.next();
        if (!isCash(item.getItemId()) && item.getVicious() < 2) {
            needed += (2 - item.getVicious());
        }
    }

    // Calculate Haves
    var have = 0;
    var cashIter = cashInv.iterator();
    while (cashIter.hasNext()) {
        var cItem = cashIter.next();
        if (cItem.getItemId() === ID_HAMMER) {
            have += cItem.getQuantity();
        }
    }

    if (have < needed) {
        cm.sendOk("The ritual requires #b" + needed + "#k Hammers, but you only possess #r" + have + "#k. Return when you are prepared.");
        cm.dispose();
        return;
    }

    cm.sendYesNo("The stars align. You have sufficient Hammers. Shall we proceed with the expansion?");
}

function executeHammer() {
    var inv = cm.getInventory(INV_EQUIP);
    var hammersConsumed = 0;
    var iter = inv.iterator();

    // We iterate slightly differently here to allow modification
    // Use a standard for-loop on slots to be safe during modification
    for (var i = 1; i <= inv.getSlotLimit(); i++) {
        var item = inv.getItem(i);
        if (item == null || isCash(item.getItemId())) continue;

        var vic = item.getVicious();
        if (vic < 2) {
            var apply = 2 - vic;
            item.setVicious(2);
            item.setUpgradeSlots(item.getUpgradeSlots() + apply);
            hammersConsumed += apply;
            cm.getPlayer().forceUpdateItem(item);
        }
    }

    if (hammersConsumed > 0) {
        cm.gainItem(ID_HAMMER, -hammersConsumed);
        cm.sendOk("It is done. Your equipment now breathes deeper, ready for more enchantments.");
    } else {
        cm.sendOk("No changes were made.");
    }
    cm.dispose();
}

// ==========================================================
//                     LOGIC: SCROLLING
// ==========================================================

function listScrollableEquips() {
    var inv = cm.getInventory(INV_EQUIP);
    var lines = [];

    for (var i = 1; i <= inv.getSlotLimit(); i++) {
        var item = inv.getItem(i);
        if (item == null || isCash(item.getItemId())) continue;

        // Must have slots to scroll
        if (item.getUpgradeSlots() > 0) {
            var name = ii.getName(item.getItemId());
            lines.push("#L" + i + "##v" + item.getItemId() + "# #b" + name + "#k (" + item.getUpgradeSlots() + " slots)#l");
        }
    }

    if (lines.length === 0) {
        cm.sendOk("I see no equipment here that craves enchantment (0 slots available).");
        cm.dispose();
        return;
    }

    cm.sendSimple("Which artifact shall we imbue with power?\r\n" + lines.join("\r\n"));
}

function listApplicableScrolls(selection) {
    targetSlot = selection;
    var targetItem = cm.getInventory(INV_EQUIP).getItem(targetSlot);

    if (targetItem == null) {
        cm.sendOk("The item has vanished.");
        cm.dispose();
        return;
    }

    var validScrollIDs = ii.getScrollsByItemId(targetItem.getItemId()); // ArrayList of Integers
    var inv = cm.getInventory(INV_USE);
    var lines = [];

    // Map scroll IDs to inventory slots
    for (var i = 1; i <= inv.getSlotLimit(); i++) {
        var item = inv.getItem(i);
        if (item == null) continue;

        var id = item.getItemId();
        // Check if this item ID is in the valid list
        // Note: contains() might require exact type matching (int vs double) in Rhino
        for (var s = 0; s < validScrollIDs.size(); s++) {
            if (validScrollIDs.get(s) == id) {
                var sName = ii.getName(id);
                lines.push("#L" + i + "##v" + id + "# #b" + sName + "#k#l");
                break;
            }
        }
    }

    if (lines.length === 0) {
        cm.sendOk("You possess no scrolls capable of enhancing this specific artifact.");
        cm.dispose();
        return;
    }

    cm.sendSimple("And which scroll shall act as the catalyst?\r\n" + lines.join("\r\n"));
}

function confirmScroll(selection) {
    scrollSlot = selection;
    var scrollItem = cm.getInventory(INV_USE).getItem(scrollSlot);
    var targetItem = cm.getInventory(INV_EQUIP).getItem(targetSlot);
    var whiteQty = cm.itemQuantity(ID_WHITE_SCROLL);

    if (scrollItem == null || targetItem == null) {
        cm.sendOk("A disturbance in the force... items are missing.");
        cm.dispose();
        return;
    }

    var msg = "You are about to enchant #b" + ii.getName(targetItem.getItemId()) + "#k.\r\n";
    msg += "Using: #r" + ii.getName(scrollItem.getItemId()) + "#k\r\n\r\n";
    msg += "I will apply scrolls continuously until:\r\n";
    msg += "1. The item runs out of slots.\r\n";
    msg += "2. You run out of scrolls.\r\n";

    if (whiteQty > 0) {
        msg += "3. You run out of #bWhite Scrolls#k (I will use them automatically).\r\n";
    }

    cm.sendYesNo(msg);
}

function executeScroll() {
    var invUse = cm.getInventory(INV_USE);
    var invEquip = cm.getInventory(INV_EQUIP);

    var equip = invEquip.getItem(targetSlot);
    var scroll = invUse.getItem(scrollSlot);

    if (equip == null || scroll == null) {
        cm.sendOk("The items required for the ritual are gone.");
        cm.dispose();
        return;
    }

    var scrollId = scroll.getItemId();

    // Stats Tracking
    var startStats = getEquipStats(equip);
    var scrollsUsed = 0;
    var whitesUsed = 0;
    var successCount = 0;
    var failCount = 0;
    var boom = false;

    // Safety: Don't waste White Scrolls on 100% scrolls
    // Assuming 100% scrolls are defined by ID ranges or data.
    // If we can't detect rate easily, we default to using WS if the user has them.
    // Optimization: Check strict boolean for using WS

    while (equip.getUpgradeSlots() > 0 && scroll.getQuantity() > 0) {

        // Determine WS usage
        var useWhite = false;
        if (cm.itemQuantity(ID_WHITE_SCROLL) > 0) {
             // Optional: Add logic here to NOT use WS if scroll success is 100
             // For now, we assume user wants protection if they have WS
             useWhite = true;
        }

        // Consume Items
        cm.gainItem(scrollId, -1);
        scrollsUsed++;

        if (useWhite) {
            cm.gainItem(ID_WHITE_SCROLL, -1);
            whitesUsed++;
        }

        // Execute via Server Core
        // Note: checking return type. Some sources return the Equip, others return Boolean, others null on boom
        var prevLevel = equip.getLevel(); // Using level to detect success (usually increases on scroll success)
        var prevSlots = equip.getUpgradeSlots();

        var newEquip = ii.scrollEquipWithId(equip, scrollId, useWhite, 0, false);

        if (newEquip == null) {
            boom = true;
            break; // Item destroyed
        }

        equip = newEquip; // Update reference

        // Analyze Result
        if (equip.getLevel() > prevLevel) {
            successCount++;
        } else if (equip.getUpgradeSlots() < prevSlots && !useWhite) {
            failCount++;
        } else {
            // Failed but slot saved (White Scroll) OR failed and slot lost but level didn't go up
            // This logic depends heavily on source.
            // If useWhite was true and slots didn't change, it was a 'saved' fail.
            failCount++;
        }

        // Force update the specific slot
        cm.getPlayer().forceUpdateItem(equip);

        // Re-fetch scroll item to check quantity loop condition
        scroll = invUse.getItem(scrollSlot);
        if (scroll == null) break;

        // Stop if we ran out of WS but logic dictates we should have them?
        // Current logic: It just stops using them if they run out.
    }

    if (boom) {
        cm.sendOk("Disaster! The chaotic energies were too great. Your artifact has been #rdestroyed#k.");
    } else {
        var endStats = getEquipStats(equip);
        var report = generateReport(startStats, endStats);

        var msg = "#bRitual Complete.#k\r\n";
        msg += "Scrolls Used: " + scrollsUsed + "\r\n";
        msg += "White Scrolls: " + whitesUsed + "\r\n";
        msg += "Success: " + successCount + " | Fail: " + failCount + "\r\n\r\n";
        msg += "#dChanges:#k\r\n" + report;

        cm.sendOk(msg);
    }
    cm.dispose();
}

// ==========================================================
//                     LOGIC: PURCHASING
// ==========================================================

function executePurchase() {
    var input = cm.getText();
    var qty = parseInt(input);

    if (isNaN(qty) || qty < 1) {
        cm.sendOk("That is not a valid number.");
        cm.dispose();
        return;
    }

    var cost = qty * COST_HAMMER;
    if (cm.getPlayer().getCashShop().getCash(1) < cost) {
        cm.sendOk("You lack the funds. You need #b" + cost + " NX#k.");
    } else {
        cm.gainCash(-cost);
        cm.gainItem(ID_HAMMER, qty);
        cm.sendOk("A wise purchase. Use them well.");
    }
    cm.dispose();
}

// ==========================================================
//                     HELPERS
// ==========================================================

function isCash(id) {
    return id >= 5000000 || (id >= 1700000 && id < 1800000); // Standard Cash ranges
}

function getEquipStats(equip) {
    // Returns simple object snapshot
    return {
        str: equip.getStr(), dex: equip.getDex(), int: equip.getInt(), luk: equip.getLuk(),
        watk: equip.getWatk(), matk: equip.getMatk(),
        wdef: equip.getWdef(), mdef: equip.getMdef(),
        acc: equip.getAcc(), avoid: equip.getAvoid(),
        speed: equip.getSpeed(), jump: equip.getJump()
    };
}

function generateReport(oldS, newS) {
    var txt = "";
    var fields = ["str", "dex", "int", "luk", "watk", "matk"]; // Add others if desired

    for (var i = 0; i < fields.length; i++) {
        var f = fields[i];
        var diff = newS[f] - oldS[f];
        if (diff !== 0) {
            txt += f.toUpperCase() + ": " + (diff > 0 ? "+" : "") + diff + "  ";
        }
    }
    return txt === "" ? "No stats changed." : txt;
}