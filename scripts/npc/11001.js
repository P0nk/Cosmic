/* Subordinate – Free Market (9201611) NPC: Item Upgrade & Rebirth */

// Constants
const invTypeEquip = 1;
const invTypeUse = 2;
const invTypeCash = 5;
const viciousHammerId = 5570000;

var status = 0;

// Choice
var hammering = false;
var scrolling = false;

// Auto-scroll variables
var equipInvSlot = null;
var equipSelected = null;
var equipName = null;
var equipId = null;
var scrollInvSlot = null;
var scrollSelected = null;
var scrollName = null;
var scrollId = null;
var requiredScrollCount = null;
var requiredWhiteScrollCount = null;
var whiteScrollId = 2340000;

// Auto-hammer variables
var requiredHammerCount = 0;
var actualHammerCount = 0;

function start() {
    status = 0;
    cm.sendNext("Hello! I'm Otto");
}

function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    switch (status) {
        case 1:
            return choice();
        case 2:
            if (selection === 0) {
                return showHammerableEquips();
            }
            return showScrollableEquips();
        case 3:
            if (hammering === true) {
                return confirmHammerCount();
            }
            return showApplicableScrolls(selection);
        case 4:
            if (hammering === true) {
                return hammerTime();
            }
            return confirmScrollCount(selection);
        case 5:
            if (hammering === true) {
                return cm.dispose();
            }
            return scrollItemOrStop(mode);
        default:
            return cm.dispose();
    }
}

function choice() {
    cm.sendSimple("Would you like me to:" +
        "\r\n#b#L0#Hammer all your equips?#l" +
        "\r\n#b#L1#Auto scroll a piece of equipment?#l"
    );
}

// Check equip inventory for all equips eligible to be hammered
function showHammerableEquips() {
    hammering = true;
    var inv = cm.getInventory(invTypeEquip);
    var lines = [];

    // Cycle through the equip inventory
    for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {

        // Retrieve the equipment inventory slot and verify an item is present
        var item = inv.getItem(slot);
        if (!item) continue;

        // Ensure it can be hammered, then add it to the list of hammerable equips as well as increasing the required number of vicious hammers
        var viciousSlots = inv.getViciousSlots(slot);
        if (viciousSlots < 2) {
            requiredHammerCount = requiredHammerCount + (2 - viciousSlots);
            lines.push("\t" + Packages.server.ItemInformationProvider.getInstance().getName(item.getItemId()) + " - " + (2 - viciousSlots) + " hammer slots");
        }
    }

    // Exit if no equipment has vicious hammer slots
    if (!lines.length) {
        cm.sendOk("You have no hammerable items.");
        return cm.dispose();
    }

    // Does the user actually want to hammer
    cm.sendYesNo("Would you like to hammer the following items:\r\n" + lines.join("\r\n"));
}

// Check equip inventory for all equips eligible to be scrolled
function showScrollableEquips() {
    scrolling = true;
    var inv = cm.getInventory(invTypeEquip);
    var lines = [];

    // Cycle through equip inventory
    for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {

        // Retrieve the equipment inventory slot and verify an item is present
        var item = inv.getItem(slot);
        if (!item) continue;

        // Ensure it can be scrolled, then add it to the list of scrollable equips
        var upgradeSlots = inv.getUpgradeSlots(slot);
        if (upgradeSlots) {
            let name = Packages.server.ItemInformationProvider.getInstance().getName(item.getItemId());
            lines.push("#L" + slot + "##v" + item.getItemId() + "# " + name);
        }
    }

    // No equipment valid for scrolling
    if (!lines.length) {
        cm.sendOk("You have no equips to select.");
        return cm.dispose();
    }

    cm.sendSimple("Select the equipment you want to scroll:\r\n" + lines.join("\r\n"));
}

// Verify the user has enough vicious hammers to hammer all their equips
function confirmHammerCount() {
    var inv = cm.getInventory(invTypeCash);

    // Cycle through cash inventory
    for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {

        // Retrieve the cash inventory slot, verify it's a vicious hammer, and add it to the vicious hammer count
        var item = inv.getItem(slot);
        if (!item) continue;
        if (item.getItemId() === viciousHammerId) {
            var hammers = item.getQuantity();
            actualHammerCount = actualHammerCount + hammers;
        }
    }

    // Need more hammers
    if (requiredHammerCount > actualHammerCount) {
        cm.sendOk("You don't have enough Vicious Hammers. You have " + actualHammerCount + " and you need " + requiredHammerCount);
        return cm.dispose();
    }

    cm.sendYesNo("You have the required Vicious Hammers. Would you like to hammer all your equips?")
}

// Check use inventory for all scrolls valid for the equip
function showApplicableScrolls(equipInvSlotNum) {
    equipInvSlot = equipInvSlotNum;
    equipSelected = cm.getInventory(invTypeEquip).getItem(equipInvSlotNum);
    equipId = equipSelected.getItemId();
    equipName = Packages.server.ItemInformationProvider.getInstance().getName(equipId);

    var inv = cm.getInventory(invTypeUse);
    var lines = [];

    // Retrieve scroll id's valid for the selected equip
    let validScrolls = Packages.server.ItemInformationProvider.getInstance().getScrollsByItemId(equipId);

    // Cycle through valid scrolls
    for (var scroll = 0; scroll < validScrolls.length; scroll++) {

        // Cycle through inventory, checking if the user has the valid scroll
        for (var useSlot = 1; useSlot <= inv.getSlotLimit(); useSlot++) {

            // Retrieve the use inventory slot, verify it's a valid scroll, and add it to the list of scrolls the user has
            var item = inv.getItem(useSlot);
            if (!item) continue;
            if (item.getItemId() === validScrolls[scroll]) {
                var name = Packages.server.ItemInformationProvider.getInstance().getName(validScrolls[scroll]);
                lines.push(
                    "#L" + useSlot + "#"
                    + "#v" + validScrolls[scroll] + "# "
                    + name
                    + "#l"
                )
            }
        }
    }

    if (!lines.length) {
        cm.sendOk("No scrolls available for your armor type");
        return cm.dispose();
    }

    cm.sendSimple("Select the scroll you want to use:\r\n" + lines.join("\r\n"));
}

// Hammer all equipment in user inventory
function hammerTime() {

    var equipInventory = cm.getInventory(invTypeEquip);

    // Cycle through equip inventory
    for (var equipSlot = 1; equipSlot <= equipInventory.getSlotLimit(); equipSlot++) {

        // Retrieve the equip inventory slot and verify it's present
        var equipItem = equipInventory.getItem(equipSlot);
        if (!equipItem) continue;

        // Apply hammers if applicable and update the item
        var hammeredItem = equipInventory.applyHammerToItem(equipItem);
        if (!hammeredItem) continue;
        cm.getPlayer().forceUpdateItem(hammeredItem);
    }

    // Remove vicious hammers
    cm.removeAmount(viciousHammerId, requiredHammerCount)

    cm.sendOk("All equips hammered");
    cm.dispose();
}

// Verify the user has the correct number of scrolls to scroll their equipment
function confirmScrollCount(useInvSlotNum) {

    let useInv = cm.getInventory(invTypeUse)

    // Populate scroll globals (scrobals)
    scrollInvSlot = useInvSlotNum;
    scrollSelected = useInv.getItem(scrollInvSlot);
    scrollId = scrollSelected.getItemId();
    scrollName = Packages.server.ItemInformationProvider.getInstance().getName(scrollId);

    // Get the number of upgrade slots and scroll success percentage
    equipUpgradeSlots = cm.getInventory(invTypeEquip).getUpgradeSlots(equipInvSlot);
    scrollSuccessPercent = useInv.getScrollSuccess(scrollInvSlot);

    // Calculate the number of scrolls required
    if (scrollSuccessPercent === 100) {
        requiredScrollCount = equipUpgradeSlots;
        requiredWhiteScrollCount = 0;
    } else {
        requiredScrollCount = Math.ceil(equipUpgradeSlots / (scrollSuccessPercent / 100));
        requiredWhiteScrollCount = requiredScrollCount;
    }

    var lines = [];

    // Verify the user has the required scrolls
    actualScrollCount = useInv.getItem(scrollInvSlot).getQuantity();
    if (actualScrollCount < requiredScrollCount) {
        // Add error message indicating lack of scrolls
        lines.push("Lacking required number of " + scrollName + "\r\n\tYou need " + requiredScrollCount + " and you have " + actualScrollCount);
    }
    if (requiredWhiteScrollCount > 0) {
        actualWhiteScrollCount = useInv.getInventoryWhiteScrollCount();
        if (actualWhiteScrollCount < requiredWhiteScrollCount) {
            // Add error message indicating lack of white scrolls
            lines.push("Lacking required number of White Scrolls\r\n\tYou need " + requiredWhiteScrollCount + " and you have " + actualWhiteScrollCount);
        }
    }

    // Not enough scrolls
    if (lines.length) {
        cm.sendOk(lines.join("\r\n"));
        return cm.dispose();
    }

    cm.sendYesNo("You have the required scrolls:\r\n"
        + "\t" + requiredScrollCount + " " + scrollName + "\r\n"
        + "\t" + requiredWhiteScrollCount + " White Scroll\r\n"
        + "Would you like to scroll your " + equipName + "?\r\n");
}

// Scroll the equipment
function scrollItemOrStop(mode) {
    // "No" response
    if (mode === 0) {
        cm.sendOk("Come back when you have something to scroll");
        cm.dispose();
        return;
    }

    // "Yes" response - time to scroll
    // Deduct scrolls
    cm.gainItem(scrollId, -requiredScrollCount);
    if (requiredWhiteScrollCount > 0) {
        cm.gainItem(whiteScrollId, -requiredWhiteScrollCount);
    }

    // Upgrade equip
    for (iterate = 1; iterate <= requiredScrollCount; iterate++) {
        equipSelected = Packages.server.ItemInformationProvider.getInstance().scrollEquipWithId(equipSelected, scrollId, true, 2049115, false);
    }
    cm.getPlayer().forceUpdateItem(equipSelected);

    cm.sendOk("Congratulations! I scrolled your equip (hopefully)!")
    cm.dispose();
}