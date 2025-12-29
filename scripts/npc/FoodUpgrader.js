/* 92xxxxx_WeakerTierUpgrader.js (REVISED v4 - uses cm.gainCash(-amount))
 *
 * Rules:
 * - Any Food Tier can be used on ANY equipment.
 * - Max enhancement per equip = 3 (tracked via Owner tag: [F+X])
 * - Each attempt consumes:
 *     1x Food (from chosen tier) + 100,000 NX (NX_CREDIT)
 *   Even on FAIL.
 * - Failure rate increases by 13% per tier:
 *     Fail(T1)=BASE_FAIL
 *     Fail(T2)=BASE_FAIL+13
 *     ...
 * - Does NOT consume scroll slots.
 */

var status = 0;
var selectedSlot = -1;
var selectedItem = null;

var chosenTier = null;
var pendingDelta = null;

// ======================= CONFIG ========================
const FOOD_T1 = [4036173, 4036174];
const FOOD_T2 = [4036175, 4036176, 4036177];
const FOOD_T3 = [4036178, 4036179, 4036180, 4036181, 4036182, 4036183];
const FOOD_T4 = [4036184, 4036185, 4036186, 4036187, 4036188, 4036189];
const FOOD_T5 = [4036190, 4036191, 4036192, 4036193, 4036194, 4036195, 4036196, 4036197, 4036198, 4036199, 4036200];
const FOOD_T6 = [4036201, 4036202, 4036203, 4036204, 4036205, 4036206, 4036207, 4036208, 4036209, 4036210];
const FOOD_T7 = []; // WIP

const FOOD_PER_UPGRADE = 1;
const NX_COST_PER_ATTEMPT = 100000;
const MAX_ENHANCE = 3;

const BASE_FAIL_RATE = 10;      // T1 fail
const FAIL_STEP_PER_TIER = 13;  // +13% fail each tier

const ItemConstants = Packages.constants.inventory.ItemConstants;
const CashShop = Packages.server.CashShop; // for NX_CREDIT

// ================== NX HELPERS ==================
function getNxCredit() {
    try {
        return cm.getPlayer().getCashShop().getCash(CashShop.NX_CREDIT);
    } catch (e) {
        return 0;
    }
}

// ================== STATS PER TIER =====================
function tierDelta(tier, isWeapon) {
    switch (tier) {
        case 'T1':
            return isWeapon ? {str:1, dex:1, int:1, luk:1, watk:1, matk:1}
                            : {str:2, dex:2, int:2, luk:2, watk:0, matk:0};
        case 'T2':
            return isWeapon ? {str:2, dex:2, int:2, luk:2, watk:2, matk:2}
                            : {str:3, dex:3, int:3, luk:3, watk:0, matk:0};
        case 'T3':
            return isWeapon ? {str:3, dex:3, int:3, luk:3, watk:3, matk:3}
                            : {str:4, dex:4, int:4, luk:4, watk:0, matk:0};
        case 'T4':
            return isWeapon ? {str:4, dex:4, int:4, luk:4, watk:4, matk:4}
                            : {str:5, dex:5, int:5, luk:5, watk:0, matk:0};
        case 'T5':
            return isWeapon ? {str:5, dex:5, int:5, luk:5, watk:5, matk:5}
                            : {str:6, dex:6, int:6, luk:6, watk:0, matk:0};
        case 'T6':
            return isWeapon ? {str:6, dex:6, int:6, luk:6, watk:6, matk:6}
                            : {str:7, dex:7, int:7, luk:7, watk:0, matk:0};
        case 'T7':
            return {str:0, dex:0, int:0, luk:0, watk:0, matk:0};
    }
    return {str:0, dex:0, int:0, luk:0, watk:0, matk:0};
}

// ================== ENHANCE TAG (Owner) ==================
function getEnhanceLevel(item) {
    var owner = item.getOwner();
    if (!owner) return 0;
    var m = owner.match(/\[F\+(\d+)\]/);
    if (!m) return 0;
    return parseInt(m[1], 10) || 0;
}

function setEnhanceLevel(item, lvl) {
    var owner = item.getOwner();
    if (!owner) owner = "";
    owner = owner.replace(/\s*\[F\+\d+\]\s*/g, " ").trim();
    var tag = "[F+" + lvl + "]";
    item.setOwner(owner.length > 0 ? (owner + " " + tag) : tag);
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

function isWeapon(item) {
    var itemId = item.getItemId();
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
        return label + ": " + cur + " → " + (cur + add) + (add ? " (+" + add + ")" : "");
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
    return 1;
}

function failRateForTier(tier) {
    var idx = tierIndex(tier);
    var fail = BASE_FAIL_RATE + (idx - 1) * FAIL_STEP_PER_TIER;
    if (fail < 0) fail = 0;
    if (fail > 100) fail = 100;
    return fail;
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
    msg += "- Any Food tier works on any equipment.\r\n";
    msg += "- Max enhancement per equip: #r+" + MAX_ENHANCE + "#k\r\n";
    msg += "- Cost per attempt (always consumed): #b" + fmt(NX_COST_PER_ATTEMPT) + " NX#k + " + FOOD_PER_UPGRADE + " Food\r\n";
    msg += "- #rFail consumes Food + NX#k (no stats gained).\r\n\r\n";
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
    chosenTier = null;
    pendingDelta = null;

    cm.sendSimple(
        "Feed me any Food and I’ll bless your equip… sometimes.\r\n\r\n"
        + "#d- Any Food tier works on any equipment.\r\n"
        + "- Max enhance per equip: #r+" + MAX_ENHANCE + "#k\r\n"
        + "- Cost per attempt: #b" + fmt(NX_COST_PER_ATTEMPT) + " NX#k + " + FOOD_PER_UPGRADE + " Food\r\n"
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
            var name = iip.getName(itemId);
            var enh = getEnhanceLevel(it);

            lines.push("#L" + s + "##v" + itemId + "# " + name + "  #d[Food +" + enh + "/" + MAX_ENHANCE + "]#k#l");
        }

        if (!lines.length) {
            cm.sendOk("You don't seem to have any equips with you.");
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
        if (!inv2) {
            cm.sendOk("Inventory unavailable.");
            cm.dispose();
            return;
        }

        selectedItem = inv2.getItem(selectedSlot);
        if (!selectedItem) {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        }

        var curEnh = getEnhanceLevel(selectedItem);
        if (curEnh >= MAX_ENHANCE) {
            cm.sendOk("This equip is already at #bFood Enhance +" + curEnh + "#k.\r\nMax is #r+" + MAX_ENHANCE + "#k.");
            cm.dispose();
            return;
        }

        var tiers = ["T1","T2","T3","T4","T5","T6"];
        var menu = "Choose which Food Tier to use (any tier works on any equip):\r\n\r\n";
        for (var i = 0; i < tiers.length; i++) {
            var t = tiers[i];
            var have = countTierFood(t);
            var succ = successRateForTier(t);

            menu += "#L" + i + "#"
                 + "#b" + t + "#k "
                 + "(Have: " + have
                 + ", Cost: " + FOOD_PER_UPGRADE + " Food + " + fmt(NX_COST_PER_ATTEMPT) + " NX"
                 + ", Success: " + succ + "%)"
                 + "#l\r\n";
        }

        cm.sendSimple(menu);
        return;
    }

    // status 3: tier chosen -> preview + confirm
    if (status === 3) {
        var tiers2 = ["T1","T2","T3","T4","T5","T6"];
        chosenTier = tiers2[selection];
        if (!chosenTier) {
            cm.sendOk("Invalid tier selection.");
            cm.dispose();
            return;
        }

        var curEnh2 = getEnhanceLevel(selectedItem);
        if (curEnh2 >= MAX_ENHANCE) {
            cm.sendOk("This equip is already at #bFood Enhance +" + curEnh2 + "#k.\r\nMax is #r+" + MAX_ENHANCE + "#k.");
            cm.dispose();
            return;
        }

        if (!hasEnoughTierFood(chosenTier, FOOD_PER_UPGRADE)) {
            cm.sendOk(
                "You don't have enough " + chosenTier + " Food.\r\n\r\n"
                + "#dAcceptable " + chosenTier + " Food:#k\r\n"
                + foodListForTier(chosenTier, FOOD_PER_UPGRADE)
            );
            cm.dispose();
            return;
        }

        var nxBal = getNxCredit();
        if (nxBal < NX_COST_PER_ATTEMPT) {
            cm.sendOk("You need #b" + fmt(NX_COST_PER_ATTEMPT) + " NX#k per attempt.\r\n"
                + "Your current NX: #r" + fmt(nxBal) + "#k");
            cm.dispose();
            return;
        }

        var weaponFlag = isWeapon(selectedItem);
        pendingDelta = tierDelta(chosenTier, weaponFlag);

        var succ2 = successRateForTier(chosenTier);
        var fail2 = failRateForTier(chosenTier);

        var iip2 = Packages.server.ItemInformationProvider.getInstance();
        var itemName = iip2.getName(selectedItem.getItemId());

        var msg =
            "Tier chosen: #b" + chosenTier + "#k\r\n"
            + "Enhance: #b+" + curEnh2 + "#k → #b+" + (curEnh2 + 1) + "#k (Max +" + MAX_ENHANCE + ")\r\n\r\n"
            + "Target:\r\n#v" + selectedItem.getItemId() + "# " + itemName + "\r\n\r\n"
            + "#dPreview:#k\r\n" + statSummary(selectedItem, pendingDelta) + "\r\n\r\n"
            + "#dCosts (always consumed):#k\r\n"
            + fmt(NX_COST_PER_ATTEMPT) + " NX\r\n"
            + FOOD_PER_UPGRADE + " x (any one of the following):\r\n"
            + foodListForTier(chosenTier, FOOD_PER_UPGRADE) + "\r\n\r\n"
            + "#dChance:#k\r\n"
            + "Success: #b" + succ2 + "%#k\r\n"
            + "Fail: #r" + fail2 + "%#k\r\n"
            + "#rOn fail: Food + NX are consumed. No stats gained.#k\r\n\r\n"
            + "Proceed?";

        cm.sendYesNo(msg);
        return;
    }

    // status 4: execute upgrade (consume NX+food regardless, then roll)
    if (status === 4) {
        var inv3 = cm.getInventory(1);
        if (!inv3) {
            cm.sendOk("Inventory unavailable.");
            cm.dispose();
            return;
        }
        var liveItem = inv3.getItem(selectedSlot);
        if (!liveItem) {
            cm.sendOk("That equip is no longer in that slot.");
            cm.dispose();
            return;
        }
        selectedItem = liveItem;

        var curEnh3 = getEnhanceLevel(selectedItem);
        if (curEnh3 >= MAX_ENHANCE) {
            cm.sendOk("This equip is already at #bFood Enhance +" + curEnh3 + "#k.\r\nMax is #r+" + MAX_ENHANCE + "#k.");
            cm.dispose();
            return;
        }

        if (!hasEnoughTierFood(chosenTier, FOOD_PER_UPGRADE)) {
            cm.sendOk(
                "Looks like you don't have enough " + chosenTier + " Food.\r\n\r\n"
                + "#dAcceptable " + chosenTier + " Food:#k\r\n"
                + foodListForTier(chosenTier, FOOD_PER_UPGRADE)
            );
            cm.dispose();
            return;
        }

        var nxBal2 = getNxCredit();
        if (nxBal2 < NX_COST_PER_ATTEMPT) {
            cm.sendOk("You need #b" + fmt(NX_COST_PER_ATTEMPT) + " NX#k per attempt.\r\n"
                + "Your current NX: #r" + fmt(nxBal2) + "#k");
            cm.dispose();
            return;
        }

        // Consume NX first (fail still pays) — your 1-liner
        cm.gainCash(-NX_COST_PER_ATTEMPT);

        // Consume food (fail still pays)
        var ok3 = consumeTierFood(chosenTier, FOOD_PER_UPGRADE);
        if (!ok3) {
            // Refund NX if food consume failed (rare but fair)
            cm.gainCash(NX_COST_PER_ATTEMPT);
            cm.sendOk("Couldn't consume the required Food for some reason.");
            cm.dispose();
            return;
        }

        // Roll
        var succ3 = successRateForTier(chosenTier);
        if (!rollPercent(succ3)) {
            cm.sendOk("#rFailed!#k\r\nFood + NX were consumed.\r\nNo stats gained.");
            cm.dispose();
            return;
        }

        // Success -> apply stats + enhance +1
        var weaponFlag3 = isWeapon(selectedItem);
        var d3 = tierDelta(chosenTier, weaponFlag3);

        applyStats(selectedItem, d3);
        setEnhanceLevel(selectedItem, curEnh3 + 1);

        cm.getPlayer().forceUpdateItem(selectedItem);
        cm.sendOk("#bSuccess!#k\r\nFood Enhance +" + (curEnh3 + 1) + " applied.");
        cm.dispose();
        return;
    }
}
