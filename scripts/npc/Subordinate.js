/*
 * Subordinate 5.0 - Final Integration
 */

var SubordinateManager = Java.type("server.subordinate.SubordinateManager");
var SubordinateMath    = Java.type("server.subordinate.SubordinateMath");
var ItemInformationProvider = Java.type("server.ItemInformationProvider");

// ================= CONSTANTS =================
const materials = {
    zakDiamond:    4032133,
    hTegg:         4001094,
    rockOfTime:    4021010,
    vonleonSeal:   4001693,
    cygnusCirclet: 4000659,
    gigaToadPurse: 4000703
};
const matValues = Object.values(materials);

// Upgrade Config
const FEES    = [15e6, 45e6, 125e6, 275e6];
const AMOUNTS = [1, 3, 5, 7];

// Rebirth Required Levels (Must match Java Constants)
// Index 0 = Rebirth 1 (Hands 0 -> 1)
const RB_LEVELS = [70, 110, 140, 160, 180, 200];

// Variables
var nxMultiplier = false;
var nxMultiplierCost = 2000000;
var boomProtectScroll = 3020003;

var previewFee = 0;
var upgradeNormal = false;
var slot = -1;
var reroll = false;
var selectedItem = null;
var newStats = null;
var max_rate = 1.599;

// ================= STATE MACHINE =================
function start() {
    status = 0;
    cm.sendNext("Hello! I'm Slimy's Subordinate! I facilitate Weapon Upgrading and Rebirths.");
}

function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    if (status === 1) {
        cm.sendSimple(
            "\r\n#b#L0#Regular upgrades (Independent Rolls)#l" +
            "\r\n#b#L1#Premium upgrades (Consistent Rolls)#l"
            // Add Salvage/Reset here if needed
        );
    }
    else if (status === 2) {
        if (selection === 0) { upgradeNormal = true; status = 19; }
        else if (selection === 1) { upgradeNormal = false; status = 29; }
        else return cm.dispose();

        return showItemList();
    }
    // ================= NORMAL FLOW =================
    else if (status === 20) {
        if (!reroll) slot = selection;
        return handlePreview(true);
    }
    else if (status === 21) {
        // 0 = Reroll, 1 = Upgrade
        if (selection === 0) { status = 19; reroll = true; action(1, 0, 0); }
        else doUpgrade();
    }
    // ================= PREMIUM FLOW =================
    else if (status === 30) {
        if (!reroll) slot = selection;
        return handlePreview(false);
    }
    else if (status === 31) {
        if (selection === 0) { status = 29; reroll = true; action(1, 0, 0); }
        else doUpgrade();
    }
    // ================= REBIRTH CONFIRMATION =========
    else if (status === 100) {
        return doRebirth();
    }
}

// ================= FUNCTIONS =================

function showItemList() {
    var inv = cm.getInventory(1);
    var limit = inv.getSlotLimit();
    var lines = [];

    for (var s = 1; s <= limit; s++) {
        var item = inv.getItem(s);
        if (!item) continue;

        // Basic Filter: Must be weapon range or specific armor
        // (Add your isWeapon/blacklist logic here if needed)
        if (item.getItemId() < 1000000) continue;

        var name = ItemInformationProvider.getInstance().getName(item.getItemId());
        lines.push("#L" + s + "##v" + item.getItemId() + "# " + name + " (Lv " + item.getItemLevel() + " / Hands " + item.getHands() + ")#l");
    }

    var div = upgradeNormal ? 100000 : 1000000;
    var costDesc = "Item Req Level / 2 * " + Math.floor(div/3);

    cm.sendSimple("Pick an item.\r\nBase Cost: " + costDesc + " mesos.\r\n" + lines.join("\r\n"));
}

function handlePreview(isNormal) {
    selectedItem = cm.getInventory(1).getItem(slot);
    if (!selectedItem) {
        cm.sendOk("Item not found.");
        return cm.dispose();
    }

    var iiReq = ItemInformationProvider.getInstance().getEquipLevelReq(selectedItem.getItemId());
    var lvl = selectedItem.getItemLevel();
    var hands = selectedItem.getHands();

    // === REBIRTH CHECK ===
    if (lvl === 5) {
        // Calculate the NEXT required level for the warning
        var nextReq = (hands < RB_LEVELS.length) ? RB_LEVELS[hands] : 200;
        var rebirthMat = matValues[hands + 1];
        var rebirthNxCost = Math.trunc(curvedScale(hands));

        if (!matValues[hands + 1]) {
            cm.sendOk("You have reached the absolute maximum Rebirths for this item.");
            return cm.dispose();
        }

        status = 99; // Next step will be 100 (doRebirth)
        cm.sendYesNo(
            "Your item has reached Level 5! I can Rebirth it to unlock higher potential.\r\n\r\n" +
            "#e#r[WARNING] Upon Rebirth, this item will require Level " + nextReq + " to equip!#k#n\r\n\r\n" +
            "Cost: 1x #v" + rebirthMat + "# + " + Math.trunc(rebirthNxCost/1000) + "k NX.\r\n" +
            "Proceed?"
        );
        return;
    }

    // === UPGRADE PREVIEW ===
    var mat = matValues[hands];
    var amt = AMOUNTS[lvl - 1];

    // Fee Calculation: (Req / 2) * Multiplier / 3
    var baseFee = (iiReq / 2.0);
    if (isNormal) {
        previewFee = Math.floor((baseFee * 100000) / 3);
    } else {
        previewFee = Math.floor((baseFee * 1000000) / 3);
    }

    // Check Resources
    if (cm.getMeso() < previewFee + FEES[lvl-1]) {
         cm.sendOk("You need " + cm.numberWithCommas(previewFee + FEES[lvl-1]) + " mesos.");
         return cm.dispose();
    }
    if (!cm.haveItem(mat, amt)) {
        cm.sendOk("You need " + amt + "x #v" + mat + "#.");
        return cm.dispose();
    }

    // Deduct Preview Fee
    cm.gainMeso(-previewFee);

    // Call Java Math
    newStats = SubordinateMath.simulateUpgrade(selectedItem, !isNormal, nxMultiplier, max_rate);

    var msg = "Stats Preview (Level " + (lvl) + " > " + (lvl+1) + "):\r\n" +
              "STR: " + selectedItem.getStr() + " > " + newStats.str + "\r\n" +
              "DEX: " + selectedItem.getDex() + " > " + newStats.dex + "\r\n" +
              "INT: " + selectedItem.getInt() + " > " + newStats.int_ + "\r\n" +
              "LUK: " + selectedItem.getLuk() + " > " + newStats.luk + "\r\n" +
              "WATK: " + selectedItem.getWatk() + " > " + newStats.watk + "\r\n" +
              "MATK: " + selectedItem.getMatk() + " > " + newStats.matk + "\r\n" +
              "\r\n#L0#Reroll#l\r\n#L1#Upgrade#l";

    cm.sendSimple(msg);
}

function doUpgrade() {
    var lvl = selectedItem.getItemLevel();
    var hands = selectedItem.getHands();
    var mat = matValues[hands];
    var amt = AMOUNTS[lvl - 1];

    // Deduct Final Upgrade Cost
    cm.gainMeso(-FEES[lvl - 1]);
    cm.gainItem(mat, -amt);

    // Calc Success/Boom
    var successRate = 1 - 0.1 * (lvl - 1);
    var boomChance  = (lvl === 4 ? 0.005 : 0);
    var roll = Math.random();

    if (roll < successRate) {
        // Success: Call Manager to apply stats
        SubordinateManager.applyUpgrade(cm.getPlayer(), selectedItem, newStats);
        cm.scrollPass(cm.getPlayer().getId());
        cm.sendOk("Upgrade Success!");
    } else {
        // Fail
        if (Math.random() < boomChance) {
             // Boom Logic
             if (cm.haveItem(boomProtectScroll, 1)) {
                 cm.gainItem(boomProtectScroll, -1);
                 cm.sendOk("Upgrade Failed, but Protection Scroll saved your item!");
             } else {
                 cm.removeItem(selectedItem.getPosition(), 1);
                 cm.scrollBoom(cm.getPlayer().getId());
                 cm.sendOk("BOOM! Your item was destroyed.");
             }
        } else {
            // Just Fail
            cm.scrollFail(cm.getPlayer().getId());
            cm.sendOk("Upgrade Failed.");
        }
    }
    cm.dispose();
}

function doRebirth() {
    var hands = selectedItem.getHands();
    var rebirthMat = matValues[hands+1];
    var cost = Math.trunc(curvedScale(hands));

    if (!cm.haveItem(rebirthMat, 1) || cm.getCashShop().getCash(1) < cost) {
        cm.sendOk("You need 1x #v"+rebirthMat+"# and " + cm.numberWithCommas(cost) + " NX.");
        return cm.dispose();
    }

    // Call Manager to Rebirth
    // It handles: Delete Old -> Create New -> Apply Formula -> Set Req Level -> Force Update
    SubordinateManager.rebirthItem(cm.getClient(), cm.getPlayer(), slot);

    cm.gainItem(rebirthMat, -1);
    cm.gainCash(-cost);

    cm.scrollPass(cm.getPlayer().getId());
    cm.sendOk("Item Reborn! Its stats have been carried over and Level reset.");
    cm.dispose();
}

function curvedScale(hands) {
    var start = 100_000.0;
    var end   = 500_000_000.0;
    var p     = 1.3;
    var t     = hands / 7.0;
    var r     = end / start;
    return start * Math.pow(r, Math.pow(t, p));
}