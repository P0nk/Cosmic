/* 11011.js — Food Upgrader NPC (Guide + Upgrade + Donor Auto) — FINAL (TAG: F. Tier)
 *
 * RULES:
 * - Any Food tier can be used on ANY equipment.
 * - Does NOT consume scroll/upgrade slots.
 * - Max enhancements per equip: +3 (tracked via owner tag: [F. Tier (6)] / [F. Tier (66)] / [F. Tier (666)])
 * - Each attempt costs: 1 Food (chosen tier) + 100,000 NX (cm.gainCash(-amount))
 * - Fail rate increases by 13% per tier:
 *     Fail(T1)=10%, T2=23%, T3=36%, T4=49%, T5=62%, T6=75%
 * - Fail consumes Food + NX.
 * - Cash equips are NOT eligible (blocked via ItemInformationProvider.isCash -> WZ info/cash).
 *
 * MAIN MENU:
 *  0) Upgrade equipment with Food (single attempt flow)
 *  1) Guide & Food List
 *  2) Check my equips' Food Enhance status
 *  3) [Donor Only] Auto Food!  (only shows if GMLevel >= 1)
 *
 * TAG SYSTEM (NO BACKWARD COMPATIBILITY):
 * - ONLY supports: [F. Tier (digits)]
 *   Example: Tier 6 used 3 times => [F. Tier (666)]
 * - Enhancement level = number of digits in the tag (max 3)
 */

const ItemConstants = Packages.constants.inventory.ItemConstants;
const CashShop = Packages.server.CashShop;

var status = 0;

// Flow state
var selectedSlot = -1;
var selectedItem = null;
var chosenTier = null;
var pendingDelta = null;

var flowMode = 0; // 0=single flow, 1=auto flow

// Persisted GM detection for this conversation
var detectedGm = 0;

// ======================= CONFIG ========================
const FOOD_T1 = [4036173, 4036174];
const FOOD_T2 = [4036175, 4036176, 4036177];
const FOOD_T3 = [4036178, 4036179, 4036180, 4036181, 4036182, 4036183];
const FOOD_T4 = [4036184, 4036185, 4036186, 4036187, 4036188, 4036189];
const FOOD_T5 = [4036190, 4036191, 4036192, 4036193, 4036194, 4036195, 4036196, 4036197, 4036198, 4036199, 4036200];
const FOOD_T6 = [4036201, 4036202, 4036203, 4036204, 4036205, 4036206, 4036207, 4036208, 4036209, 4036210];

const FOOD_PER_ATTEMPT = 1;
const NX_COST_PER_ATTEMPT = 100000;
const MAX_ENHANCE = 3;

// Failure scaling
const BASE_FAIL_RATE = 10;
const FAIL_STEP_PER_TIER = 13;

// Safety cap so auto mode can't freeze
const AUTO_ATTEMPT_CAP = 500;

// ================== STATS PER TIER (YOUR VALUES) =====================
function tierDelta(tier, isWeaponFlag) {
    switch (tier) {
        case 'T1':
            return isWeaponFlag ? {str:2, dex:2, int:2, luk:2, watk:4, matk:4}
                                : {str:3, dex:3, int:3, luk:3, watk:0, matk:0};
        case 'T2':
            return isWeaponFlag ? {str:3, dex:3, int:3, luk:3, watk:5, matk:5}
                                : {str:5, dex:5, int:5, luk:5, watk:0, matk:0};
        case 'T3':
            return isWeaponFlag ? {str:4, dex:4, int:4, luk:4, watk:6, matk:6}
                                : {str:7, dex:7, int:7, luk:7, watk:0, matk:0};
        case 'T4':
            return isWeaponFlag ? {str:5, dex:5, int:5, luk:5, watk:7, matk:7}
                                : {str:9, dex:9, int:9, luk:9, watk:0, matk:0};
        case 'T5':
            return isWeaponFlag ? {str:6, dex:6, int:6, luk:6, watk:8, matk:8}
                                : {str:11, dex:11, int:11, luk:11, watk:0, matk:0};
        case 'T6':
            return isWeaponFlag ? {str:7, dex:7, int:7, luk:7, watk:9, matk:9}
                                : {str:13, dex:13, int:13, luk:13, watk:0, matk:0};
        default:
            return {str:0, dex:0, int:0, luk:0, watk:0, matk:0};
    }
}

// ================== FOOD TAG (OWNER) ==================
// Extracts the digit-string inside "F. Tier( ... )", e.g. "123"
function getTierHistory(item) {
    var owner = item.getOwner();
    if (!owner) return "";
    var m = owner.match(/F\.\s*Tier\s*\(\s*([0-9]*)\s*\)/);
    return m ? (m[1] || "") : "";
}

// Enhance count = number of digits recorded
function getEnhanceLevel(item) {
    return getTierHistory(item).length;
}

// Removes the whole "F. Tier(...)" tag from an owner string
function clearFoodTag(owner) {
    if (!owner) return "";
    return owner.replace(/\s*F\.\s*Tier\s*\(\s*[0-9]*\s*\)\s*/g, " ").trim();
}


function appendTier(item, digit) {
    var owner = item.getOwner();
    var history = getTierHistory(item);
    history += digit;

    owner = clearFoodTag(owner);
    var tag = "F. Tier (" + history + ")";
    item.setOwner(owner.length > 0 ? (owner + " " + tag) : tag);
}

// ================== FOOD HELPERS ==================
function foodPoolForTier(tier) {
    switch (tier) {
        case 'T1': return FOOD_T1;
        case 'T2': return FOOD_T2;
        case 'T3': return FOOD_T3;
        case 'T4': return FOOD_T4;
        case 'T5': return FOOD_T5;
        case 'T6': return FOOD_T6;
        default: return [];
    }
}

function foodListForTier(tier, qty) {
    var pool = foodPoolForTier(tier);
    if (!pool || pool.length === 0) return "#r(No food configured for " + tier + ")#k";
    var iip = Packages.server.ItemInformationProvider.getInstance();
    var parts = [];
    for (var i = 0; i < pool.length; i++) {
        var id = pool[i];
        parts.push(qty + "x #v" + id + "# " + iip.getName(id));
    }
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

// ================== CASH EQUIP BLOCK ==================
function isCashEquip(item) {
    if (!item) return false;
    try {
        // Uses WZ info/cash (0=normal, 1=cash)
        return Packages.server.ItemInformationProvider.getInstance().isCash(item.getItemId());
    } catch (e) {
        // Fallback: only block cash weapon skins range if something goes wrong
        var id = item.getItemId();
        return (id >= 1700000 && id < 1800000);
    }
}

// ================== NX HELPERS ==================
function getNxCredit() {
    try {
        return cm.getPlayer().getCashShop().getCash(CashShop.NX_CREDIT);
    } catch (e) {
        return 0;
    }
}

function fmt(n) {
    try { return java.text.NumberFormat.getInstance().format(n); }
    catch (e) { return "" + n; }
}

// ================== WEAPON DETECTION ==================
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

// ================== APPLY + PREVIEW ==================
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
    var lines = [];
    lines.push(line("STR", item.getStr(), d.str));
    lines.push(line("DEX", item.getDex(), d.dex));
    lines.push(line("INT", item.getInt(), d.int));
    lines.push(line("LUK", item.getLuk(), d.luk));
    lines.push(line("WATK", item.getWatk(), d.watk));
    lines.push(line("MATK", item.getMatk(), d.matk));
    return lines.join("\r\n");
}

// ================== FAIL CHANCE ==================
function tierIndex(tier) {
    if (tier === "T1") return 1;
    if (tier === "T2") return 2;
    if (tier === "T3") return 3;
    if (tier === "T4") return 4;
    if (tier === "T5") return 5;
    if (tier === "T6") return 6;
    return 1;
}

function tierDigit(tier) {
    return "" + tierIndex(tier);
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

// ================== GM/DONOR CHECK (FINAL, NO DEBUG) ==================
function getGmLevelSafe() {
    try {
        var p = cm.getPlayer();
        if (p == null) return 0;

        // Your Character.java: public int gmLevel() { return gmLevel; }
        if (p.gmLevel) {
            var v0 = p.gmLevel();
            var n0 = parseInt(v0, 10);
            if (!isNaN(n0)) return n0;
        }

        // Some bases expose getGMLevel (older)
        if (p.getGMLevel) {
            var v1 = p.getGMLevel();
            var n1 = parseInt(v1, 10);
            if (!isNaN(n1)) return n1;
        }

        // Fallback boolean
        if (p.isGM && p.isGM()) return 2;

        // Job fallback
        if (p.getJob && (p.getJob() == 900 || p.getJob() == 910)) return 2;
    } catch (e) {}
    return 0;
}

function isDonor() {
    // donor is gm level >= 1
    return detectedGm >= 1;
}

// ================== INFO PAGES ==================
function guideText() {
    var msg = "#e#b[Food Enhancement Guide]#k#n\r\n\r\n";
    msg += "#dRules:#k\r\n";
    msg += "- Any Food tier can be used on ANY equipment.\r\n";
    msg += "- Does NOT consume scroll/upgrade slots.\r\n";
    msg += "- Max enhancements per equip: #r+" + MAX_ENHANCE + "#k\r\n";
    msg += "- Cost per attempt: " + FOOD_PER_ATTEMPT + " Food + #b" + fmt(NX_COST_PER_ATTEMPT) + " NX#k\r\n";
    msg += "- #rFail consumes Food + NX.#k\r\n";
    msg += "- #rCash equips cannot be upgraded.#k\r\n\r\n";

    msg += "#dFail chance rule:#k\r\n";
    msg += "Fail(T1) = " + BASE_FAIL_RATE + "%\r\n";
    msg += "Each tier adds +" + FAIL_STEP_PER_TIER + "% fail.\r\n\r\n";

    msg += "#dTier Chances:#k\r\n";
    var tiers = ["T1","T2","T3","T4","T5","T6"];
    for (var i = 0; i < tiers.length; i++) {
        var t = tiers[i];
        msg += t + ": Success " + successRateForTier(t) + "% / Fail " + failRateForTier(t) + "%\r\n";
    }

    msg += "\r\n#dAccepted Food Items by Tier:#k\r\n";
    for (var j = 0; j < tiers.length; j++) {
        var tt = tiers[j];
        msg += "\r\n#b" + tt + "#k\r\n" + foodListForTier(tt, FOOD_PER_ATTEMPT) + "\r\n";
    }

    msg += "\r\n#dEnhancement tracking:#k\r\n";
    msg += "Enhancements are stored as: #bF. Tier (6)#k / #bF. Tier (66)#k / #bF. Tier (666)#k\r\n";

    if (isDonor()) {
        msg += "\r\n#dDonor Only:#k\r\n";
        msg += "- Auto Food can loop until max is reached.\r\n";
    }

    return msg;
}

function listEquipEnhanceStatus() {
    var inv = cm.getInventory(1);
    if (!inv) return "#r(Equip inventory unavailable)#k";

    var limit = inv.getSlotLimit();
    var iip = Packages.server.ItemInformationProvider.getInstance();
    var out = [];

    for (var s = 1; s <= limit; s++) {
        var it = inv.getItem(s);
        if (!it) continue;

        var itemId = it.getItemId();
        var name = iip.getName(itemId);
        var enh = getEnhanceLevel(it);

        out.push("#v" + itemId + "# " + name + "  #d[Food +" + enh + "/" + MAX_ENHANCE + "]#k");
    }

    if (!out.length) return "#r(No equips found.)#k";
    return out.join("\r\n");
}

// ======================= SCRIPT FLOW ===================
function start() {
    status = 0;
    selectedSlot = -1;
    selectedItem = null;
    chosenTier = null;
    pendingDelta = null;
    flowMode = 0;

    var nx = getNxCredit();

    // Detect once; used for menu + access checks
    detectedGm = getGmLevelSafe();
    var donor = (detectedGm >= 1);

    var text =
        "Hey hey! Bob chews Food and judges your gear.\r\n\r\n"
        + "#dYour NX:#k #b" + fmt(nx) + "#k\r\n"
        + "#dCost per attempt:#k 1 Food + #b" + fmt(NX_COST_PER_ATTEMPT) + " NX#k\r\n"
        + "#dMax per equip:#k #r+" + MAX_ENHANCE + "#k\r\n"
        + "#rFail consumes Food + NX.#k\r\n"
        + "#rCash equips cannot be upgraded.#k\r\n\r\n"
        + "#b#L0#Upgrade equipment with Food#l\r\n"
        + "#b#L1#Guide & Food List#l\r\n"
        + "#b#L2#Check my equips' Food Enhance status#l";

    if (donor) {
        text += "\r\n#b#L3##r[Donor Only]#k Auto Food!#l";
    }

    cm.sendSimple(text);
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }
    status++;

    // ===== Main menu routing =====
    if (status === 1) {
        if (selection === 1) {
            cm.sendOk(guideText());
            cm.dispose();
            return;
        }
        if (selection === 2) {
            cm.sendOk("#e#b[Your Equip Enhance Status]#k#n\r\n\r\n" + listEquipEnhanceStatus());
            cm.dispose();
            return;
        }
        if (selection === 3) {
            if (detectedGm < 1) {
                cm.sendOk("This option is for donors only.");
                cm.dispose();
                return;
            }
            flowMode = 1; // auto
        } else {
            flowMode = 0; // single
        }

        // selection 0 or 3 -> pick equip (NON-CASH ONLY)
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

            if (isCashEquip(it)) continue;

            var enh = getEnhanceLevel(it);
            var itemId = it.getItemId();
            var name = iip.getName(itemId);

            lines.push("#L" + s + "##v" + itemId + "# " + name
                + "  #d[Food +" + enh + "/" + MAX_ENHANCE + "]#k#l");
        }

        if (!lines.length) {
            cm.sendOk("You don't seem to have any eligible equips with you.\r\n(Cash equips are blocked.)");
            cm.dispose();
            return;
        }

        cm.sendSimple("Pick the equip to enhance:\r\n" + lines.join("\r\n"));
        return;
    }

    // ===== Pick equip -> choose tier =====
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

        if (isCashEquip(selectedItem)) {
            cm.sendOk("Bob refuses to upgrade cash equips.\r\nBring a normal equip next time!");
            cm.dispose();
            return;
        }

        var curEnh = getEnhanceLevel(selectedItem);
        if (curEnh >= MAX_ENHANCE) {
            cm.sendOk("This equip is already at Food Enhance +" + curEnh + ".\r\nMax is +" + MAX_ENHANCE + ".");
            cm.dispose();
            return;
        }

        var tiers = ["T1","T2","T3","T4","T5","T6"];
        var menu = "Choose Food Tier (any tier works on any equip):\r\n\r\n";
        for (var i = 0; i < tiers.length; i++) {
            var t = tiers[i];
            var have = countTierFood(t);
            var succ = successRateForTier(t);
            menu += "#L" + i + "##b" + t + "#k "
                 + "(Have: " + have
                 + ", Cost: 1 Food + " + fmt(NX_COST_PER_ATTEMPT) + " NX"
                 + ", Success: " + succ + "%)#l\r\n";
        }

        cm.sendSimple(menu);
        return;
    }

    // ===== Pick tier -> preview confirm =====
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
            cm.sendOk("This equip is already at Food Enhance +" + curEnh2 + ".\r\nMax is +" + MAX_ENHANCE + ".");
            cm.dispose();
            return;
        }

        if (!hasEnoughTierFood(chosenTier, FOOD_PER_ATTEMPT)) {
            cm.sendOk(
                "You don't have enough " + chosenTier + " Food.\r\n\r\n"
                + "#dAccepted " + chosenTier + " Food:#k\r\n"
                + foodListForTier(chosenTier, FOOD_PER_ATTEMPT)
            );
            cm.dispose();
            return;
        }

        var nxBal = getNxCredit();
        if (nxBal < NX_COST_PER_ATTEMPT) {
            cm.sendOk("You need " + fmt(NX_COST_PER_ATTEMPT) + " NX per attempt.\r\n"
                + "Your current NX: " + fmt(nxBal));
            cm.dispose();
            return;
        }

        pendingDelta = tierDelta(chosenTier, isWeapon(selectedItem));

        var succ2 = successRateForTier(chosenTier);
        var fail2 = failRateForTier(chosenTier);

        var iip2 = Packages.server.ItemInformationProvider.getInstance();
        var itemName = iip2.getName(selectedItem.getItemId());

        var msg =
            "Tier: #b" + chosenTier + "#k\r\n"
            + "Enhance: #b+" + curEnh2 + "#k -> #b+" + (curEnh2 + 1) + "#k (Max +" + MAX_ENHANCE + ")\r\n\r\n"
            + "Target:\r\n#v" + selectedItem.getItemId() + "# " + itemName + "\r\n\r\n"
            + "#dPreview (on success):#k\r\n" + statSummary(selectedItem, pendingDelta)
            + "\r\n\r\n#dCosts (always consumed):#k\r\n"
            + fmt(NX_COST_PER_ATTEMPT) + " NX\r\n"
            + "1 Food (any one of the following):\r\n"
            + foodListForTier(chosenTier, FOOD_PER_ATTEMPT)
            + "\r\n\r\n#dChance:#k\r\n"
            + "Success: #b" + succ2 + "%#k\r\n"
            + "Fail: #r" + fail2 + "%#k\r\n"
            + "#rFail consumes Food + NX. No stats gained.#k\r\n\r\n";

        if (flowMode === 1) {
            msg += "#bAuto Food Mode (Donor):#k\r\n"
                + "Bob will keep trying until your equip reaches max.\r\n"
                + "or you run out of NX/Food.\r\n\r\n";
        }

        msg += "Proceed?";

        cm.sendYesNo(msg);
        return;
    }

    // ===== Execute attempt(s) =====
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

        if (isCashEquip(selectedItem)) {
            cm.sendOk("Bob refuses to upgrade cash equips.");
            cm.dispose();
            return;
        }

        var curEnh3 = getEnhanceLevel(selectedItem);
        if (curEnh3 >= MAX_ENHANCE) {
            cm.sendOk("This equip is already at Food Enhance +" + curEnh3 + ".\r\nMax is +" + MAX_ENHANCE + ".");
            cm.dispose();
            return;
        }

        // ===== SINGLE MODE =====
        if (flowMode === 0) {
            if (getNxCredit() < NX_COST_PER_ATTEMPT) {
                cm.sendOk("Not enough NX to attempt.");
                cm.dispose();
                return;
            }
            if (!hasEnoughTierFood(chosenTier, FOOD_PER_ATTEMPT)) {
                cm.sendOk("Not enough " + chosenTier + " Food to attempt.");
                cm.dispose();
                return;
            }

            cm.gainCash(-NX_COST_PER_ATTEMPT);
            var okFood = consumeTierFood(chosenTier, FOOD_PER_ATTEMPT);
            if (!okFood) {
                cm.gainCash(NX_COST_PER_ATTEMPT);
                cm.sendOk("Couldn't consume the required Food for some reason.");
                cm.dispose();
                return;
            }

            if (!rollPercent(successRateForTier(chosenTier))) {
                cm.sendOk(
                    "#rBob was not impressed...#k\r\n\r\n"
                    + "Your food failed to impress Bob, and the upgrade #rfailed#k.\r\n"
                    + "#dFood + NX were still consumed.#k"
                );
                cm.dispose();
                return;
            }

            var d = tierDelta(chosenTier, isWeapon(selectedItem));
            applyStats(selectedItem, d);
            appendTier(selectedItem, tierDigit(chosenTier));
            cm.getPlayer().forceUpdateItem(selectedItem);

            cm.sendOk(
                "#bPassed!#k\r\n\r\n"
                + "Your food #bimpressed Bob#k!\r\n"
                + "Bob happily upgraded your gear.\r\n\r\n"
                + "#dFood Enhance +" + (curEnh3 + 1) + " applied.#k"
            );
            cm.dispose();
            return;
        }

        // ===== AUTO MODE (DONOR) =====
        if (detectedGm < 1) {
            cm.sendOk("This option is for donors only.");
            cm.dispose();
            return;
        }

        var attempts = 0;
        var successes = 0;
        var fails = 0;
        var nxSpent = 0;
        var foodSpent = 0;

        while (curEnh3 < MAX_ENHANCE && attempts < AUTO_ATTEMPT_CAP) {
            if (getNxCredit() < NX_COST_PER_ATTEMPT) break;
            if (!hasEnoughTierFood(chosenTier, FOOD_PER_ATTEMPT)) break;

            cm.gainCash(-NX_COST_PER_ATTEMPT);
            nxSpent += NX_COST_PER_ATTEMPT;

            var okF = consumeTierFood(chosenTier, FOOD_PER_ATTEMPT);
            if (!okF) {
                cm.gainCash(NX_COST_PER_ATTEMPT);
                nxSpent -= NX_COST_PER_ATTEMPT;
                break;
            }
            foodSpent += FOOD_PER_ATTEMPT;

            attempts++;

            if (!rollPercent(successRateForTier(chosenTier))) {
                fails++;
                continue;
            }

            var d2 = tierDelta(chosenTier, isWeapon(selectedItem));
            applyStats(selectedItem, d2);
            appendTier(selectedItem, tierDigit(chosenTier));
            curEnh3++;
            successes++;
        }

        cm.getPlayer().forceUpdateItem(selectedItem);

        var finalEnh = getEnhanceLevel(selectedItem);
        var stopReason = "";
        if (finalEnh >= MAX_ENHANCE) stopReason = "Reached max.";
        else if (attempts >= AUTO_ATTEMPT_CAP) stopReason = "Hit safety cap (" + AUTO_ATTEMPT_CAP + " attempts).";
        else if (getNxCredit() < NX_COST_PER_ATTEMPT) stopReason = "Not enough NX to continue.";
        else if (!hasEnoughTierFood(chosenTier, FOOD_PER_ATTEMPT)) stopReason = "Not enough Food to continue.";
        else stopReason = "Stopped.";

        cm.sendOk(
            "#e#b[Donor Auto Food Summary]#k#n\r\n\r\n"
            + "Final Enhance: #b" + finalEnh + "/" + MAX_ENHANCE + "#k\r\n"
            + "Tier Tag: #b[F. Tier (" + getTierHistory(selectedItem) + ")]#k\r\n\r\n"
            + "Attempts: #b" + attempts + "#k\r\n"
            + "Successes: #b" + successes + "#k\r\n"
            + "Fails: #r" + fails + "#k\r\n\r\n"
            + "NX Spent: #b" + fmt(nxSpent) + "#k\r\n"
            + "Food Spent: #b" + foodSpent + "#k\r\n\r\n"
            + "#d" + stopReason + "#k"
        );
        cm.dispose();
        return;
    }
}
