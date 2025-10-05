/* 92xxxxx_WeakerTierUpgrader.js
 * Level-banded, weaker item upgrader using FoodDropper tiers.
 * - Shows full list of acceptable Food items for the tier when previewing or when missing items.
 * - Applies fixed stat bonuses by tier (T1..T6).
 * - Consumes ONE upgrade slot per upgrade; blocks when 0.
 */

var status = 0;
var selectedSlot = -1;
var selectedItem = null;
var pendingTier = null;
var pendingDelta = null;
var preUpgradeItem = null;

// ======================= CONFIG ========================
const FOOD_T1 = [4036173, 4036174];
const FOOD_T2 = [4036175, 4036176, 4036177];
const FOOD_T3 = [4036178, 4036179, 4036180, 4036181, 4036182, 4036183];
const FOOD_T4 = [4036184, 4036185, 4036186, 4036187, 4036188, 4036189];
const FOOD_T5 = [4036190, 4036191, 4036192, 4036193, 4036194, 4036195, 4036196, 4036197, 4036198, 4036199, 4036200];
const FOOD_T6 = [4036201, 4036202, 4036203, 4036204, 4036205, 4036206, 4036207, 4036208, 4036209, 4036210];
const FOOD_T7 = [];

const FOOD_PER_UPGRADE = 1;
const MESO_FEE = { T1:0, T2:0, T3:0, T4:0, T5:0, T6:0, T7:0 };

const ItemConstants = Packages.constants.inventory.ItemConstants;

// ================== STATS PER TIER =====================
function tierDelta(tier, isWeapon) {
    switch (tier) {
        case 'T1':
            return isWeapon ? {str:2, dex:2, int:2, luk:2, watk:4, matk:4}
                            : {str:3, dex:3, int:3, luk:3, watk:0, matk:0};
        case 'T2':
            return isWeapon ? {str:3, dex:3, int:3, luk:3, watk:5, matk:5}
                            : {str:5, dex:5, int:5, luk:5, watk:0, matk:0};
        case 'T3':
            return isWeapon ? {str:4, dex:4, int:4, luk:4, watk:6, matk:6}
                            : {str:7, dex:7, int:7, luk:7, watk:0, matk:0};
        case 'T4':
            return isWeapon ? {str:5, dex:5, int:5, luk:5, watk:7, matk:7}
                            : {str:9, dex:9, int:9, luk:9, watk:0, matk:0};
        case 'T5':
            return isWeapon ? {str:6, dex:6, int:6, luk:6, watk:8, matk:8}
                            : {str:11, dex:11, int:11, luk:11, watk:0, matk:0};
        case 'T6':
            return isWeapon ? {str:7, dex:7, int:7, luk:7, watk:9, matk:9}
                            : {str:9, dex:9, int:9, luk:9, watk:1, matk:1};
        case 'T7':
            return {str:0, dex:0, int:0, luk:0, watk:0, matk:0};
    }
    return {str:0, dex:0, int:0, luk:0, watk:0, matk:0};
}

// ================= HELPERS =================
function getEquipReqLevel(itemId) {
    return Packages.server.ItemInformationProvider
        .getInstance().getEquipLevelReq(itemId);
}
function tierForReqLevel(req) {
    if (req <= 30) return 'T1';
    if (req <= 50) return 'T2';
    if (req <= 80) return 'T3';
    if (req <= 120) return 'T4';
    if (req <= 150) return 'T5';
    if (req <= 160) return 'T6';
    return 'T7';
}

function isWeapon(item) {
    var itemId = item.getItemId();
    var itemName = Packages.server.ItemInformationProvider.getInstance().getName(itemId);

    // Step 1: Base server check
    var baseWeapon = ItemConstants.isWeapon(itemId);

    // Step 2: Range-based detection
    var inWeaponRange = (itemId >= 1300000 && itemId < 1500000);
    var inCashWeaponRange = (itemId >= 1700000 && itemId < 1800000);

    // Step 3: Type-based exclusions
    var isAccessory = ItemConstants.isAccessory(itemId);
    var isOverall = ItemConstants.isOverall(itemId);
    var isMedal = ItemConstants.isMedal(itemId);
    var isShield = (itemId >= 1092000 && itemId < 1100000);
    var inArmorRange = (itemId >= 1000000 && itemId < 1300000);

    // Step 4: Consolidate exclusion logic
    var excluded = (
        inArmorRange ||
        isShield ||
        isAccessory ||
        isOverall ||
        isMedal ||
        inCashWeaponRange
    );

    // Step 5: Final classification
    var finalFlag = (inWeaponRange || baseWeapon) && !excluded;

    // Step 6: Full debug output
    console.log("====== [TierUpgrader Debug] ======");
    console.log("Item: " + itemName + " (" + itemId + ")");
    console.log("baseWeapon: " + baseWeapon);
    console.log("inWeaponRange: " + inWeaponRange);
    console.log("inCashWeaponRange: " + inCashWeaponRange);
    console.log("isAccessory: " + isAccessory);
    console.log("isOverall: " + isOverall);
    console.log("isMedal: " + isMedal);
    console.log("isShield: " + isShield);
    console.log("inArmorRange: " + inArmorRange);
    console.log("excluded: " + excluded);
    console.log("FINAL WEAPON FLAG: " + finalFlag);
    console.log("=================================");

    return finalFlag;
}


function foodListForTier(tier, qty) {
    var pool = foodPoolForTier(tier);
    if (!pool || pool.length === 0) return "#r(No food configured for " + tier + ")#k";
    var parts = [];
    for (var i = 0; i < pool.length; i++) {
        parts.push(qty + "x #v" + pool[i] + "#");
    }
    return parts.join("\r\n");
}

function findUsableFoodId(tier, need) {
    var pool = foodPoolForTier(tier);
    for (var i = 0; i < pool.length; i++) {
        var id = pool[i];
        if (cm.haveItem(id, need)) return id;
    }
    return 0;
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
    var lines = [];
    lines.push(line("STR", item.getStr(), d.str));
    lines.push(line("DEX", item.getDex(), d.dex));
    lines.push(line("INT", item.getInt(), d.int));
    lines.push(line("LUK", item.getLuk(), d.luk));
    lines.push(line("WATK", item.getWatk(), d.watk));
    lines.push(line("MATK", item.getMatk(), d.matk));
    return lines.join("\r\n");
}

function countTierFood(tier) {
    var pool = foodPoolForTier(tier);
    var total = 0;
    for (var i = 0; i < pool.length; i++) {
        total += cm.itemQuantity(pool[i]);
    }
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

function guide() {
    var msg = "#e#b[Equipment Upgrade Guide]#k#n\r\n\r\n";
    msg += "Items are divided into tiers based on their required level. Each tier grants bonus stats upon upgrading.\r\n\r\n";

    msg += "#dTier by Required Level:#k\r\n";
    msg += "T1: Lv. 1–30\r\n";
    msg += "T2: Lv. 31–50\r\n";
    msg += "T3: Lv. 51–80\r\n";
    msg += "T4: Lv. 81–120\r\n";
    msg += "T5: Lv. 121–150\r\n";
    msg += "T6: Lv. 151–160\r\n";
    msg += "T7: Lv. 161+ (WIP)\r\n\r\n";

    msg += "#bWeapon Upgrade Bonuses:#k\r\n";
    msg += "T1: +2 STR/DEX/INT/LUK, +4 WATK/MATK\r\n";
    msg += "T2: +3 STR/DEX/INT/LUK, +5 WATK/MATK\r\n";
    msg += "T3: +4 STR/DEX/INT/LUK, +6 WATK/MATK\r\n";
    msg += "T4: +5 STR/DEX/INT/LUK, +7 WATK/MATK\r\n";
    msg += "T5: +6 STR/DEX/INT/LUK, +8 WATK/MATK\r\n";
    msg += "T6: +7 STR/DEX/INT/LUK, +9 WATK/MATK\r\n";
    msg += "T7: (Placeholder)\r\n\r\n";

    msg += "#bArmor Upgrade Bonuses:#k\r\n";
    msg += "T1: +3 All Stats\r\n";
    msg += "T2: +5 All Stats\r\n";
    msg += "T3: +7 All Stats\r\n";
    msg += "T4: +9 All Stats\r\n";
    msg += "T5: +11 All Stats\r\n";
    msg += "T6: +9 All Stats, +1 WATK/MATK\r\n";
    msg += "T7: (Placeholder)\r\n";
    return msg;
}

// ======================= SCRIPT FLOW ===================
function start() {
    status = 0;
    cm.sendNext(
        "Hey there! Free food keeps dropping and apparently if I chew on them and spit on any equipment, they get upgraded!\r\n"
        + "#d> Tier = equip's required level.\r\n"
        + "> Each success consumes #r1 upgrade slot#k and the required Food.\r\n"
        + "> No upgrades if the item has #r0 slots#k.\r\n"
        + "> T7 to be confirmed.#k"
        + "\r\n#b#L0#Proceed#l"
        + "\r\n#b#L1#Guide please#l"
    );
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }
    status++;

    if (status === 1) {
        if (selection == 0) {
            cm.sendOk(guide());
            cm.dispose();
        } else if (selection == 1) {
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
                var name = iip.getName(it.getItemId());
                var req = getEquipReqLevel(it.getItemId());
                var tier = tierForReqLevel(req);
                var slots = it.getUpgradeSlots();
                lines.push(
                    "#L" + s + "##v" + it.getItemId() + "# " + name
                    + " #d(Tier: " + tier + ")#k  [Slots: " + slots + "]#l"
                );
            }

            if (!lines.length) {
                cm.sendOk("You don't seem to have any equips with you.");
                cm.dispose();
                return;
            }

            cm.sendSimple("Pick the equip to enhance:\r\n" + lines.join("\r\n"));
            return;
        }
    }

    if (status === 2) {
        selectedSlot = selection;
        var inv = cm.getInventory(1);
        if (!inv) {
            cm.sendOk("Inventory unavailable.");
            cm.dispose();
            return;
        }
        selectedItem = inv.getItem(selectedSlot);
        preUpgradeItem = inv.getItem(selectedSlot);
        if (!selectedItem) {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        }

        var slots = selectedItem.getUpgradeSlots();
        if (slots <= 0) {
            cm.sendOk("That item has #rno upgrade slots#k left. I can't work on it.");
            cm.dispose();
            return;
        }

        var req = getEquipReqLevel(selectedItem.getItemId());
        pendingTier = tierForReqLevel(req);
        var foodList = foodListForTier(pendingTier, FOOD_PER_UPGRADE);
        var foodId = findUsableFoodId(pendingTier, FOOD_PER_UPGRADE);
        var weaponFlag = isWeapon(selectedItem);
        pendingDelta = tierDelta(pendingTier, weaponFlag);
        var fee = MESO_FEE[pendingTier] || 0;

        if (!foodId) {
            cm.sendOk(
                "You're missing the right Food for this tier.\r\n\r\n"
                + "#dAcceptable " + pendingTier + " Food:#k\r\n"
                + foodList
            );
            cm.dispose();
            return;
        }

        var msg =
            "I'll apply a #b" + pendingTier + "#k enhancement to:\r\n"
            + "#v" + selectedItem.getItemId() + "# "
            + Packages.server.ItemInformationProvider.getInstance().getName(selectedItem.getItemId()) + "\r\n\r\n"
            + "#dPreview:#k\r\n" + statSummary(selectedItem, pendingDelta)
            + "\r\n\r\n#dCosts:#k\r\n"
            + (fee > 0 ? (fee + " mesos\r\n") : "")
            + FOOD_PER_UPGRADE + " x (any one of the following):\r\n"
            + foodList;

        cm.sendYesNo(msg + "\r\n\r\nProceed? (Consumes #rone upgrade slot#k)");
        return;
    }

    if (status === 3) {
        var fee = MESO_FEE[pendingTier] || 0;

        if (fee > 0 && cm.getMeso() < fee) {
            cm.sendOk("You don't have enough mesos.");
            cm.dispose();
            return;
        }

        if (!hasEnoughTierFood(pendingTier, FOOD_PER_UPGRADE)) {
            var foodList = foodListForTier(pendingTier, FOOD_PER_UPGRADE);
            cm.sendOk(
                "Looks like you don't have enough Food for this tier.\r\n\r\n"
                + "#dAcceptable " + pendingTier + " Food:#k\r\n"
                + foodList
            );
            cm.dispose();
            return;
        }

        if (fee > 0) cm.gainMeso(-fee);
        var ok = consumeTierFood(pendingTier, FOOD_PER_UPGRADE);
        if (!ok) {
            var foodList2 = foodListForTier(pendingTier, FOOD_PER_UPGRADE);
            cm.sendOk(
                "Couldn't consume the required Food for some reason.\r\n\r\n"
                + "#dAcceptable " + pendingTier + " Food:#k\r\n"
                + foodList2
            );
            cm.dispose();
            return;
        }

        selectedItem.setUpgradeSlots(selectedItem.getUpgradeSlots() - 1);
        applyStats(selectedItem, pendingDelta);
        cm.getPlayer().forceUpdateItem(selectedItem);

        cm.sendOk("Done! One slot consumed.");
        cm.dispose();
    }
}
