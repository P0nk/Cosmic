/* 92xxxxx_WeakerTierUpgrader.js (REVISED v8 - "F.Tier NNN" History Tag)
 *
 * Updates:
 * - Tag format changed to [F.Tier NNN...]
 * - "N" represents the specific tier used (1-9).
 * - "X" represents Tier 10.
 * - Length of the sequence represents current upgrade count.
 */

var status = 0;
var selectedSlot = -1;
var selectedItem = null;
var selectedItemId = -1;

var chosenTier = null;
var pendingDelta = null;

// ======================= CONFIG ========================
var FOOD_T1 = [4036173, 4036174];
var FOOD_T2 = [4036175, 4036176, 4036177];
var FOOD_T3 = [4036178, 4036179, 4036180, 4036181, 4036182, 4036183];
var FOOD_T4 = [4036184, 4036185, 4036186, 4036187, 4036188, 4036189];
var FOOD_T5 = [4036190, 4036191, 4036192, 4036193, 4036194, 4036195, 4036196, 4036197, 4036198, 4036199, 4036200];
var FOOD_T6 = [4036201, 4036202, 4036203, 4036204, 4036205, 4036206, 4036207, 4036208, 4036209, 4036210];
var FOOD_T7 = [4036211, 4036212, 4036213, 4036214, 4036215, 4036216, 4036217, 4036218, 4036219, 4036220, 4036221, 4036222, 4036223, 4036224, 4036225];
var FOOD_T8 = [4036226, 4036227, 4036228, 4036229, 4036230, 4036231, 4036232, 4036233, 4036234, 4036235, 4036236, 4036237, 4036238, 4036239, 4036240];
var FOOD_T9 = [4036241, 4036242, 4036243, 4036244, 4036245, 4036246, 4036247, 4036248, 4036249, 4036250, 4036251, 4036252, 4036253, 4036254, 4036255, 4036256, 4036257, 4036258, 4036259, 4036260, 4036261, 4036262, 4036263, 4036264, 4036265, 4036266];
var FOOD_T10 = [4036289, 4036290, 4036291, 4036292, 4036293, 4036294, 4036295, 4036296, 4036297, 4036298, 4036299, 4036300, 4036301, 4036302, 4036303, 4036304, 4036305, 4036306, 4036307];

var FOOD_PER_UPGRADE = 1;
var NX_COST_STEP = 10000;
var MAX_ENHANCE = 3; // Max string length

var BASE_FAIL_RATE = 0;
var FAIL_STEP_PER_TIER = 10;

var ItemConstants = Packages.constants.inventory.ItemConstants;
var CashShop = Packages.server.CashShop;

// ================== NX HELPERS ==================
function getNxCredit() {
    try { return cm.getPlayer().getCashShop().getCash(CashShop.NX_CREDIT); }
    catch (e) { return 0; }
}
function nxCostForTier(tier) {
    return tierIndex(tier) * NX_COST_STEP;
}

// ================== STATS PER TIER =====================
function tierDelta(tier, isWeapon) {
    switch (tier) {
        case 'T1': return isWeapon ? {str:0, dex:0, int:0, luk:0, watk:1, matk:1} : {str:1, dex:1, int:1, luk:1, watk:0, matk:0};
        case 'T2': return isWeapon ? {str:1, dex:1, int:1, luk:1, watk:2, matk:2} : {str:3, dex:3, int:3, luk:3, watk:0, matk:0};
        case 'T3': return isWeapon ? {str:2, dex:2, int:2, luk:2, watk:3, matk:3} : {str:5, dex:5, int:5, luk:5, watk:0, matk:0};
        case 'T4': return isWeapon ? {str:3, dex:3, int:3, luk:3, watk:4, matk:4} : {str:7, dex:7, int:7, luk:7, watk:0, matk:0};
        case 'T5': return isWeapon ? {str:4, dex:4, int:4, luk:4, watk:5, matk:5} : {str:9, dex:9, int:9, luk:9, watk:0, matk:0};
        case 'T6': return isWeapon ? {str:5, dex:5, int:5, luk:5, watk:6, matk:6} : {str:11, dex:11, int:11, luk:11, watk:0, matk:0};
        case 'T7': return isWeapon ? {str:6, dex:6, int:6, luk:6, watk:8, matk:8} : {str:13, dex:13, int:13, luk:13, watk:0, matk:0};
        case 'T8': return isWeapon ? {str:7, dex:7, int:7, luk:7, watk:10, matk:10} : {str:15, dex:15, int:15, luk:15, watk:0, matk:0};
        case 'T9': return isWeapon ? {str:8, dex:8, int:8, luk:8, watk:12, matk:12} : {str:17, dex:17, int:17, luk:17, watk:0, matk:0};
        case 'T10': return isWeapon ? {str:10, dex:10, int:10, luk:10, watk:16, matk:16} : {str:20, dex:20, int:20, luk:20, watk:0, matk:0};
    }
    return {str:0, dex:0, int:0, luk:0, watk:0, matk:0};
}

// ================== ENHANCE TAG LOGIC (UPDATED) ==================

// Returns the history string (e.g., "1X5")
function getEnhanceHistory(item) {
    var owner = item.getOwner();
    if (!owner) return "";
    // Matches [F.Tier (sequence of 1-9 or X)]
    var m = owner.match(/\[F\.Tier ([1-9X]+)\]/);
    if (!m) return "";
    return m[1];
}

// Returns the integer count of upgrades based on string length
function getEnhanceLevel(item) {
    var hist = getEnhanceHistory(item);
    return hist.length;
}

// Converts Tier string to Single Character
function getTierChar(tier) {
    if (tier === "T10") return "X";
    // For T1-T9, strip the 'T'
    return tier.replace("T", "");
}

// Appends the new tier char to the existing tag
function addEnhanceTag(item, tierChar) {
    var currentOwner = item.getOwner() || "";
    var currentHistory = getEnhanceHistory(item); // e.g. "1X" or ""

    // Remove old tag pattern completely
    // Note: We remove ANY matching F.Tier tag to replace it with the updated one
    var cleanOwner = currentOwner.replace(/\s*\[F\.Tier [1-9X]+\]\s*/g, " ").trim();

    var newHistory = currentHistory + tierChar;
    var newTag = "[F.Tier " + newHistory + "]";

    item.setOwner(cleanOwner.length > 0 ? (cleanOwner + " " + newTag) : newTag);
}

// ================= HELPERS =================
function foodPoolForTier(tier) {
    switch (tier) {
        case 'T1': return FOOD_T1;
        case 'T2': return FOOD_T2;
        case 'T3': return FOOD_T3;
        case 'T4': return FOOD_T4;
        case 'T5': return FOOD_T5;
        case 'T6': return FOOD_T6;
        case 'T7': return FOOD_T7;
        case 'T8': return FOOD_T8;
        case 'T9': return FOOD_T9;
        case 'T10': return FOOD_T10;
    }
    return [];
}

function foodListForTier(tier, qty) {
    var pool = foodPoolForTier(tier);
    if (!pool || pool.length === 0) return "#r(No food configured for " + tier + ")#k";
    var parts = [];
    for (var i = 0; i < pool.length; i++) parts.push(qty + "x #v" + pool[i] + "#");
    return parts.join("\r\n");
}

function countTierFood(tier) {
    var pool = foodPoolForTier(tier);
    var total = 0;
    for (var i = 0; i < pool.length; i++) total += cm.itemQuantity(pool[i]);
    return total;
}

function hasEnoughTierFood(tier, qty) {
    return countTierFood(tier) >= qty;
}

function consumeTierFood(tier, qty) {
    var pool = foodPoolForTier(tier);
    var remaining = qty;
    for (var i = 0; i < pool.length && remaining > 0; i++) {
        var id = pool[i];
        var have = cm.itemQuantity(id);
        if (have <= 0) continue;
        var take = Math.min(have, remaining);
        if (take > 0) {
            cm.gainItem(id, -take);
            remaining -= take;
        }
    }
    return remaining === 0;
}

function isCashItem(itemId) {
    if (itemId >= 5000000) return true;
    if (itemId >= 1700000 && itemId < 1800000) return true;
    try {
        if (Packages.server.ItemInformationProvider.getInstance().isCash(itemId)) return true;
    } catch(e) {}
    return false;
}

function isWeapon(item) {
    var itemId = item.getItemId();
    if (isCashItem(itemId)) return false;
    var baseWeapon = ItemConstants.isWeapon(itemId);
    var inWeaponRange = (itemId >= 1300000 && itemId < 1500000);
    var isAccessory = ItemConstants.isAccessory(itemId);
    var isOverall = ItemConstants.isOverall(itemId);
    var isMedal = ItemConstants.isMedal(itemId);
    var isShield = (itemId >= 1092000 && itemId < 1100000);
    var inArmorRange = (itemId >= 1000000 && itemId < 1300000);
    var inCashWeaponRange = (itemId >= 1700000 && itemId < 1800000);
    var excluded = (inArmorRange || isShield || isAccessory || isOverall || isMedal || inCashWeaponRange);
    return (inWeaponRange || baseWeapon) && !excluded;
}

function applyStats(item, d) {
    item.setStr(item.getStr() + d.str);
    item.setDex(item.getDex() + d.dex);
    item.setInt(item.getInt() + d.int);
    item.setLuk(item.getLuk() + d.luk);
    item.setWatk(item.getWatk() + d.watk);
    item.setMatk(item.getMatk() + d.matk);
}

function statSummary(item, d) {
    function line(label, cur, add) {
        return label + ": " + cur + " -> " + (cur + add) + (add ? " (+" + add + ")" : "");
    }
    return [
        line("STR", item.getStr(), d.str),
        line("DEX", item.getDex(), d.dex),
        line("INT", item.getInt(), d.int),
        line("LUK", item.getLuk(), d.luk),
        line("WATK", item.getWatk(), d.watk),
        line("MATK", item.getMatk(), d.matk)
    ].join("\r\n");
}

function tierIndex(tier) {
    if (tier === "T1") return 1;
    if (tier === "T2") return 2;
    if (tier === "T3") return 3;
    if (tier === "T4") return 4;
    if (tier === "T5") return 5;
    if (tier === "T6") return 6;
    if (tier === "T7") return 7;
    if (tier === "T8") return 8;
    if (tier === "T9") return 9;
    if (tier === "T10") return 10;
    return 1;
}

function failRateForTier(tier) {
    var idx = tierIndex(tier);
    var fail = BASE_FAIL_RATE + (idx - 1) * FAIL_STEP_PER_TIER;
    return Math.max(0, Math.min(100, fail));
}

function successRateForTier(tier) {
    return Math.max(0, Math.min(100, 100 - failRateForTier(tier)));
}

function rollPercent(pct) {
    return (Math.random() * 100) < pct;
}

function fmt(n) {
    try { return java.text.NumberFormat.getInstance().format(n); }
    catch (e) { return "" + n; }
}

function guide() {
    var msg = "#e#b[Food Enhancement Guide]#k#n\r\n\r\n";
    msg += "- Enhancements appear as #b[F.Tier NNN]#k.\r\n";
    msg += "- The letters (1-9, X) show which tiers were used.\r\n";
    msg += "- Max enhancement per equip: #r+" + MAX_ENHANCE + "#k\r\n";
    msg += "- Cost per attempt: #b(Tier x " + fmt(NX_COST_STEP) + ") NX#k + " + FOOD_PER_UPGRADE + " Food\r\n";
    msg += "#dFailure Rate Rule:#k\r\n";
    msg += "T1 fail = " + BASE_FAIL_RATE + "%\r\n";
    msg += "Each next tier adds +" + FAIL_STEP_PER_TIER + "% fail.\r\n";
    return msg;
}

// ======================= SCRIPT FLOW ===================
function start() {
    status = 0;
    selectedSlot = -1;
    selectedItem = null;
    selectedItemId = -1;
    chosenTier = null;
    pendingDelta = null;

    cm.sendSimple(
        "Feed me any Food and I will bless your equip... sometimes.\r\n\r\n"
        + "#d- Items are tagged with [F.Tier ...]\r\n"
        + "- Max enhance per equip: #r+" + MAX_ENHANCE + "#k\r\n"
        + "- Cost per attempt: #b(Tier x " + fmt(NX_COST_STEP) + ") NX#k + " + FOOD_PER_UPGRADE + " Food\r\n"
        + "- #rFail consumes Food + NX#k.\r\n"
        + "- No scroll slots are used.\r\n\r\n"
        + "#b#L0#Proceed#l\r\n"
        + "#b#L1#Guide please#l"
    );
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }
    status++;

    // status 1: menu choice
    if (status === 1) {
        if (selection == 1) {
            cm.sendOk(guide());
            cm.dispose();
            return;
        }

        var inv = cm.getInventory(1);
        if (!inv) {
            cm.sendOk("I can't access your equip inventory right now.");
            cm.dispose();
            return;
        }

        var limit = inv.getSlotLimit();
        var lines = [];
        var iip = Packages.server.ItemInformationProvider.getInstance();

        for (var s = 1; s <= limit; s++) {
            var it = inv.getItem(s);
            if (!it) continue;

            var itemId = it.getItemId();
            if (isCashItem(itemId)) continue;

            var name = iip.getName(itemId);

            // Get current history to display (e.g., "1X5")
            var hist = getEnhanceHistory(it);
            var count = hist.length;

            // Display: [F.Tier 1X5] (3/3) or [Food +0/3]
            var tagDisplay = (count > 0) ? "[F.Tier " + hist + "]" : "[No Food]";

            lines.push("#L" + s + "##v" + itemId + "# " + name + "  #d" + tagDisplay + " (" + count + "/" + MAX_ENHANCE + ")#k#l");
        }

        if (lines.length === 0) {
            cm.sendOk("You don't have any upgradeable equips. (Cash items are excluded)");
            cm.dispose();
            return;
        }

        cm.sendSimple("Pick the equip to enhance:\r\n" + lines.join("\r\n"));
        return;
    }

    // status 2: equip picked -> choose food tier
    if (status === 2) {
        selectedSlot = selection;
        var inv2 = cm.getInventory(1);
        selectedItem = inv2.getItem(selectedSlot);

        if (!selectedItem) {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        }

        selectedItemId = selectedItem.getItemId();

        if (isCashItem(selectedItemId)) {
            cm.sendOk("You cannot upgrade Cash items.");
            cm.dispose();
            return;
        }

        var curCount = getEnhanceLevel(selectedItem);
        if (curCount >= MAX_ENHANCE) {
            cm.sendOk("This equip is already maxed at #b" + curCount + "/" + MAX_ENHANCE + "#k upgrades.");
            cm.dispose();
            return;
        }

        // Tiers T10 to T1
        var tiers = ["T10", "T9", "T8", "T7", "T6", "T5", "T4", "T3", "T2", "T1"];

        var menu = "Choose which Food Tier to use (any tier works on any equip):\r\n\r\n";
        for (var i = 0; i < tiers.length; i++) {
            var t = tiers[i];
            var have = countTierFood(t);
            var succ = successRateForTier(t);

            menu += "#L" + i + "#"
                 + "#b" + t + "#k "
                 + "(Have: " + have
                 + ", Cost: " + FOOD_PER_UPGRADE + " Food + " + fmt(nxCostForTier(t)) + " NX"
                 + ", Success: " + succ + "%)"
                 + "#l\r\n";
        }

        cm.sendSimple(menu);
        return;
    }

    // status 3: tier chosen -> preview + confirm
    if (status === 3) {
        var tiers2 = ["T10", "T9", "T8", "T7", "T6", "T5", "T4", "T3", "T2", "T1"];
        chosenTier = tiers2[selection];

        if (!chosenTier || selectedItem.getItemId() !== selectedItemId) {
             cm.sendOk("Error. Please start over.");
             cm.dispose();
             return;
        }

        var curCount2 = getEnhanceLevel(selectedItem);
        if (curCount2 >= MAX_ENHANCE) {
            cm.sendOk("This equip is already maxed.");
            cm.dispose();
            return;
        }

        if (!hasEnoughTierFood(chosenTier, FOOD_PER_UPGRADE)) {
            cm.sendOk("You don't have enough " + chosenTier + " Food.\r\n\r\n" + "#dAcceptable " + chosenTier + " Food:#k\r\n" + foodListForTier(chosenTier, FOOD_PER_UPGRADE));
            cm.dispose();
            return;
        }

        var nxNeed = nxCostForTier(chosenTier);
        if (getNxCredit() < nxNeed) {
            cm.sendOk("You need #b" + fmt(nxNeed) + " NX#k.");
            cm.dispose();
            return;
        }

        var weaponFlag = isWeapon(selectedItem);
        pendingDelta = tierDelta(chosenTier, weaponFlag);

        var succ2 = successRateForTier(chosenTier);
        var fail2 = failRateForTier(chosenTier);

        var iip2 = Packages.server.ItemInformationProvider.getInstance();
        var itemName = iip2.getName(selectedItem.getItemId());

        var hist2 = getEnhanceHistory(selectedItem);
        var nextChar = getTierChar(chosenTier);

        var msg =
            "Tier chosen: #b" + chosenTier + "#k\r\n"
            + "Current Tag: #b[F.Tier " + (hist2 === "" ? "None" : hist2) + "]#k\r\n"
            + "Result Tag: #b[F.Tier " + (hist2 + nextChar) + "]#k\r\n\r\n"
            + "Target:\r\n#v" + selectedItem.getItemId() + "# " + itemName + "\r\n\r\n"
            + "#dPreview:#k\r\n" + statSummary(selectedItem, pendingDelta) + "\r\n\r\n"
            + "#dCosts:#k " + fmt(nxNeed) + " NX + " + FOOD_PER_UPGRADE + " Food\r\n"
            + "Success: #b" + succ2 + "%#k | Fail: #r" + fail2 + "%#k\r\n"
            + "Proceed?";

        cm.sendYesNo(msg);
        return;
    }

    // status 4: execute
    if (status === 4) {
        var inv3 = cm.getInventory(1);
        var liveItem = inv3.getItem(selectedSlot);

        if (!liveItem || liveItem.getItemId() !== selectedItemId) {
            cm.sendOk("Item changed or missing.");
            cm.dispose();
            return;
        }
        selectedItem = liveItem;

        if (getEnhanceLevel(selectedItem) >= MAX_ENHANCE) {
            cm.sendOk("Already maxed.");
            cm.dispose();
            return;
        }

        if (!hasEnoughTierFood(chosenTier, FOOD_PER_UPGRADE)) {
            cm.sendOk("Not enough food.");
            cm.dispose();
            return;
        }

        var nxNeed2 = nxCostForTier(chosenTier);
        if (getNxCredit() < nxNeed2) {
            cm.sendOk("Not enough NX.");
            cm.dispose();
            return;
        }

        cm.gainCash(-nxNeed2);

        if (!consumeTierFood(chosenTier, FOOD_PER_UPGRADE)) {
            cm.gainCash(nxNeed2); // Refund NX if food fails
            cm.sendOk("Food consumption failed.");
            cm.dispose();
            return;
        }

        if (!rollPercent(successRateForTier(chosenTier))) {
            cm.sendOk("#rFailed!#k\r\nFood + NX consumed. No stats gained.");
            cm.dispose();
            return;
        }

        // Success
        var weaponFlag3 = isWeapon(selectedItem);
        var d3 = tierDelta(chosenTier, weaponFlag3);

        // 1. Apply Stats
        applyStats(selectedItem, d3);

        // 2. Update Tag (Append new tier char)
        var tChar = getTierChar(chosenTier);
        addEnhanceTag(selectedItem, tChar);

        cm.getPlayer().forceUpdateItem(selectedItem);
        cm.sendOk("#bSuccess!#k\r\nStats applied and tag updated to " + getEnhanceHistory(selectedItem));
        cm.dispose();
        return;
    }
}