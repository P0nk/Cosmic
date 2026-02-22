/*
 * Subordinate 4.1 — Salvage Logic Fixed
 * - Salvage Filter: Hides Clean items (RB0/Lv1).
 * - Pity System: Triggers ONLY at RB0/Lv2 (1 upgrade done).
 * - Pity Refund: Returns 1 Diamond + 1.5m Mesos.
 */

var SubordinateManager = Java.type("server.subordinate.SubordinateManager");
var ItemInformationProvider = Java.type("server.ItemInformationProvider");
const ItemConstants = Packages.constants.inventory.ItemConstants;
const CashShop = Packages.server.CashShop;

// ========================= CONFIG =========================

const materials = {
    zakDiamond: 4032133,
    hTegg: 4001094,
    rockOfTime: 4021010,
    vonleonSeal: 4001693,
    cygnusCirclet: 4000659,
    gigaToadPurse: 4000703,
};
const matValues = Object.values(materials);

const FEES = [5e6, 15e6, 45e6, 95e6];
const AMOUNTS = [1, 2, 3, 3];

const RB_LEVELS = [70, 110, 140, 160, 180, 200];

const MIN_RATE_BASE = 1.25;
const MAX_RATE_BASE = 1.50;

const MIN_PREVIEW_FEE = 200_000;

const REFUND_RATE = 0.3;
const MAX_AUTO_ROLLS = 100;

const NX_MULT_COST = 2_000_000;
const BOOM_PROTECT_SCROLL = 3020003;

const BCOIN_ITEM_ID = 3020002;
const BCOIN_VALUE = 1_000_000_000;

const REBIRTH_BUCKET_MESOS = 150_000_000;

// ========================= STATE MACHINE =========================

const STEP = {
    INTRO: "INTRO",
    MENU: "MENU",
    PICK_ITEM: "PICK_ITEM",
    PREVIEW: "PREVIEW",
    AUTO_STAT_SELECT: "AUTO_STAT_SELECT",
    AUTO_TARGET_INPUT: "AUTO_TARGET_INPUT",
    REBIRTH_CONFIRM: "REBIRTH_CONFIRM",
    SALVAGE_CONFIRM: "SALVAGE_CONFIRM",
    RESET_RESULT: "RESET_RESULT",
};

let ctx = null;

function start() {
    ctx = freshCtx();
    ctx.step = STEP.MENU;
    return showMenu();
}

function freshCtx() {
    return {
        step: STEP.MENU,
        mode: null,
        slot: -1,
        selectedItem: null,
        newStats: null,
        nxMultiplier: false,
        maxRate: MAX_RATE_BASE,
        previewFee: 0,
        autoRerollEnabled: false,
        autoRerollTarget: 0,
        autoRerollStatIndex: 0
    };
}

function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();

    switch (ctx.step) {
        case STEP.INTRO:
            ctx.step = STEP.MENU;
            return showMenu();
        case STEP.MENU:
            return showMenu(selection);
        case STEP.PICK_ITEM:
            return handlePickItem(selection);
        case STEP.PREVIEW:
            return handlePreviewChoice(selection);
        case STEP.AUTO_STAT_SELECT:
            return handleAutoStatSelect(selection);
        case STEP.AUTO_TARGET_INPUT:
            return handleAutoTargetEntered();
        case STEP.REBIRTH_CONFIRM:
            return handleRebirthConfirm();
        case STEP.SALVAGE_CONFIRM:
            return handleSalvageConfirm();
        case STEP.RESET_RESULT:
            return handleResetResult(selection);
        default:
            cm.sendOk("Unexpected state. Please report to GM.");
            return cm.dispose();
    }
}

// ========================= MENU =========================

function showMenu(selection) {
    if (selection === undefined || selection === null) {
        const menu =
            "Good Day, I'm Subordinate. I provide item empowering and rebirthing services.\r\n\r\n" +
            "#e#b#L0#[REGULAR]#k#n Independent Stat Rolling.\r\n" +
            "      Balanced results with variation.#l\r\n\r\n" +
            "#e#r#L1#[PREMIUM]#k#n Unified Stat Rolling.\r\n" +
            "      Higher consistency and better peak potential.#l\r\n\r\n" +
            "#e#d#L2#[SALVAGE]#k#n Scrap upgraded items.\r\n" +
            "      Recover up to 30% of mesos and materials.#l\r\n\r\n" +
            "#L3#[GUIDE] Check Rebirth Rates#l";

        cm.sendSimple(menu);
        return;
    }

    if (selection === 0) ctx.mode = "REG";
    else if (selection === 1) ctx.mode = "PREM";
    else if (selection === 2) ctx.mode = "SALVAGE";
    else if (selection === 3) return showGuide();
    else return cm.dispose();

    ctx.step = STEP.PICK_ITEM;
    return showItemList();
}

function showGuide() {
    var msg = "#e#b[Subordinate Rebirth Carry-Over Rates]#k#n\r\n" +
        "When you Rebirth (Lv 5 -> Clean), you keep a % of stats.\r\n\r\n" +
        "#bBASE STATS (STR/DEX/INT/LUK/DEF):#k 17%\r\n" +
        "---------------------------------\r\n" +
        "#bATTACK RATES (WATK/MATK):#k\r\n" +
        " Claw: #r35%#k\r\n" +
        " Knuckle, Gun: #r33%#k\r\n" +
        " Dagger: #r30.5%#k\r\n" +
        " Bow, XBow: #r30%#k\r\n" +
        " 1H Weps, : #r30%#k\r\n" +
        " 2H Weps, Spear, Polearm: #r30%#k\r\n" +
        " Wand, Staff: #r29%#k\r\n" +
        " Others: #r30%#k\r\n" +
        "---------------------------------\r\n" +
        "#bARMOR ATTACK RATES:#k\r\n" +
        " Gloves, Shoes: #d30%#k\r\n" +
        " Hats, Capes, Overalls: #d28%#k";

    cm.sendOk(msg);
    cm.dispose();
}

function showItemList() {
    const inv = cm.getInventory(1);
    const limit = inv.getSlotLimit();
    const lines = [];

    for (let s = 1; s <= limit; s++) {
        const item = inv.getItem(s);
        if (!item) continue;

        if (!isWeapon(item)) continue;

        const itemId = item.getItemId();
        const name = ItemInformationProvider.getInstance().getName(itemId);

        if (ctx.mode === "SALVAGE") {
            // FILTER: Hide Clean Items (RB0 and Level 1)
            // Only allow if Hand > 0 OR Level > 1
            if (item.getHands() === 0 && item.getItemLevel() <= 1) continue;
        } else if (ctx.mode === "REG" || ctx.mode === "PREM") {
            if (item.getHands() >= 6 && item.getItemLevel() === 5) continue;
        }

        lines.push(
            "#L" + s + "#"
            + "#v" + itemId + "# " + name
            + " (RB: " + item.getHands() + " | Lv: " + item.getItemLevel() + ")"
            + "#l"
        );
    }

    if (!lines.length) {
        cm.sendOk("You have no valid upgradable items in your inventory.");
        return cm.dispose();
    }

    const hint =
        (ctx.mode === "REG") ? "Cost: Item Lv * 10k mesos to preview.\r\n"
            : (ctx.mode === "PREM") ? "Cost: Item Lv * 150k mesos to preview.\r\n"
                : "";

    cm.sendSimple("Select the item you want to proceed with.\r\n" + hint + "#e#r[WARNING] Mesos are deducted upon previewing stats!#n#b \r\n" + lines.join("\r\n"));
}

function isWeapon(item) {
    var itemId = item.getItemId();
    var isCashItem = Math.floor(itemId / 10000) === 170;
    if (isCashItem) return false;

    var isStandardWeapon = ItemConstants.isWeapon(itemId);
    var isWeaponRange = (itemId >= 1210000 && itemId < 1600000);
    var isArmorRange = (itemId >= 1000000 && itemId < 1140000);

    return isStandardWeapon || isWeaponRange || isArmorRange;
}

// ========================= PICK ITEM =========================

function handlePickItem(slot) {
    ctx.slot = slot;
    ctx.selectedItem = cm.getInventory(1).getItem(slot);

    if (!ctx.selectedItem) {
        cm.sendOk("Invalid selection.");
        return cm.dispose();
    }

    if (ctx.mode === "SALVAGE") {
        ctx.step = STEP.SALVAGE_CONFIRM;
        return salvageSelection(slot);
    }

    ctx.step = STEP.PREVIEW;
    return doPreview();
}

// ========================= PREVIEW =========================

function doPreview() {
    const item = ctx.selectedItem;
    const iiReq = ItemInformationProvider.getInstance().getEquipLevelReq(item.getItemId());
    const lvl = item.getItemLevel();
    const hands = item.getHands();

    ctx.maxRate = MAX_RATE_BASE;

    // --- REBIRTH CHECK ---
    if (lvl === 5) {
        if (hands >= 6) {
            cm.sendOk("This item has reached its absolute limit (Rebirth 6).");
            return cm.dispose();
        }

        const rebirthMat = matValues[hands + 1] || 4001126;
        const rebirthNxCost = Math.trunc(curvedScale(hands));
        const nextReqLvl = (hands < RB_LEVELS.length) ? RB_LEVELS[hands] : 200;

        ctx.step = STEP.REBIRTH_CONFIRM;
        cm.sendYesNo(
            "Your item has reached max upgrades (Lv 5). You can Rebirth it now.\r\n\r\n"
            + "#e#b[Rebirth Effects]#k#n\r\n"
            + "- Stats reset to Clean.\r\n"
            + "- You carry over a % of your stats (Check Guide for rates).\r\n"
            + "- Item Required Level increases to #r" + nextReqLvl + "#k.\r\n\r\n"
            + "Cost: 1x#v" + rebirthMat + "# + " + Math.trunc(rebirthNxCost / 1000) + "k NX. Proceed?"
        );
        return;
    }

    const mat = matValues[hands];
    const amt = AMOUNTS[lvl - 1] || 1;

    if (!cm.haveItem(mat, amt)) {
        cm.sendOk("You lack " + amt + "x#v" + mat + "#.");
        return cm.dispose();
    }

    if (ctx.nxMultiplier && cm.getCashShop().getCash(1) < NX_MULT_COST) {
        cm.sendOk("You turned on NX Multiplier but don't have enough NX.");
        return cm.dispose();
    }

    // --- FEE CALCULATION ---
    let baseMult = (ctx.mode === "REG") ? 10000 : 150000;
    let calculatedFee = iiReq * baseMult;
    ctx.previewFee = Math.max(calculatedFee, MIN_PREVIEW_FEE);

    const needMesos = ctx.previewFee + (FEES[lvl - 1] || 0);

    let bcoinConvertedMsg = "";
    if (cm.getMeso() < needMesos) {
        if (cm.haveItem(BCOIN_ITEM_ID, 1)) {
            cm.gainItem(BCOIN_ITEM_ID, -1);
            cm.gainMeso(BCOIN_VALUE);
            bcoinConvertedMsg = "#e#rBCoin Converted!#n#k\r\n";
        } else {
            cm.sendOk("You need at least " + format(needMesos) + " mesos.");
            return cm.dispose();
        }
    }

    // Roll Stats
    ctx.newStats = (ctx.mode === "REG")
        ? calcNewStats(item, ctx.nxMultiplier, ctx.maxRate)
        : calcBetterNewStats(item, ctx.nxMultiplier, ctx.maxRate);

    // Apply Cost
    cm.gainMeso(-ctx.previewFee);
    if (ctx.nxMultiplier) {
        cm.gainCash(-NX_MULT_COST);
        cm.getPlayer().dropMessage(5, "Used 2mil NX for Preview.");
    }

    // Call generic auto-reroll
    const autoMsg = runAutoRerollIfEnabled();
    const s = ctx.newStats;
    const warning = (lvl === 4) ? "\r\n#rWARNING: 1% chance to destroy your item!#k" : "";

    const msg =
        bcoinConvertedMsg +
        "Previewing Upgrade (Lv " + (lvl) + " -> " + (lvl + 1) + "):\r\n" +
        "STR: " + item.getStr() + " > #b" + s.str + " (x" + s.mult[0].toFixed(3) + ")#k\r\n" +
        "DEX: " + item.getDex() + " > #b" + s.dex + " (x" + s.mult[1].toFixed(3) + ")#k\r\n" +
        "INT: " + item.getInt() + " > #b" + s.int + " (x" + s.mult[2].toFixed(3) + ")#k\r\n" +
        "LUK: " + item.getLuk() + " > #b" + s.luk + " (x" + s.mult[3].toFixed(3) + ")#k\r\n" +
        "WATK: " + item.getWatk() + " > #b" + s.watk + " (x" + s.mult[4].toFixed(3) + ")#k\r\n" +
        "MATK: " + item.getMatk() + " > #b" + s.matk + " (x" + s.mult[5].toFixed(3) + ")#k\r\n" +
        "Cost: " + format(FEES[lvl - 1]) + " + " + amt + "x#v" + mat + "#";

    let menu =
        "\r\n#L0#Reroll stats (Pay Preview Fee again)#l" +
        "\r\n#L1#Confirm Upgrade#l" +
        "\r\n#L2#Auto re-roll to target#l";

    ctx.step = STEP.PREVIEW;
    cm.sendSimple(msg + warning + autoMsg + menu);
}

function handlePreviewChoice(selection) {
    if (selection === 0) return doPreview();

    if (selection === 2) {
        if (ctx.mode === "REG") {
            ctx.step = STEP.AUTO_STAT_SELECT;
            cm.sendSimple("Which stat do you want to target?\r\n" +
                "#L0# STR\r\n" +
                "#L1# DEX\r\n" +
                "#L2# INT\r\n" +
                "#L3# LUK\r\n" +
                "#L4# Weapon Attack\r\n" +
                "#L5# Magic Attack");
            return;
        }
        else {
            ctx.step = STEP.AUTO_TARGET_INPUT;
            cm.sendGetText("Enter your target rate (e.g., 1.42). Max: " + (ctx.maxRate - 0.01));
            return;
        }
    }

    return doUpgrade(ctx.newStats);
}

function handleAutoStatSelect(selection) {
    if (selection < 0 || selection > 5) return cm.dispose();

    ctx.autoRerollStatIndex = selection;
    ctx.step = STEP.AUTO_TARGET_INPUT;

    var statNames = ["STR", "DEX", "INT", "LUK", "WATK", "MATK"];
    cm.sendGetText("Enter target rate for #b" + statNames[selection] + "#k (e.g., 1.42). Max: " + (ctx.maxRate - 0.01));
}

function handleAutoTargetEntered() {
    const rate = parseFloat(String(cm.getText()));
    if (isNaN(rate) || rate < MIN_RATE_BASE || rate > ctx.maxRate) {
        cm.sendOk("Invalid number. Range: " + MIN_RATE_BASE + " - " + ctx.maxRate);
        return cm.dispose();
    }

    ctx.autoRerollEnabled = true;
    ctx.autoRerollTarget = rate;
    ctx.step = STEP.PREVIEW;
    return doPreview();
}

// ========================= UPGRADE LOGIC =========================

function doUpgrade(newStats) {
    const item = ctx.selectedItem;
    const lvl = item.getItemLevel();
    const hands = item.getHands();

    if (lvl >= 5) return doRebirth();

    const mat = matValues[hands];
    const amt = AMOUNTS[lvl - 1] || 1;

    if (!cm.haveItem(mat, amt)) {
        cm.sendOk("You lack materials.");
        return cm.dispose();
    }

    cm.gainMeso(-FEES[lvl - 1]);
    cm.gainItem(mat, -amt);

    const successRate = 1 - 0.1 * (lvl - 2);
    const boomChance = (lvl === 4 ? 0.01 : 0);

    const roll = Math.random();
    const success = (roll < successRate);
    const boom = (!success && Math.random() < boomChance);

    if (success) {
        applyNewStats(newStats);
        cm.scrollPass(cm.getPlayer().getId());
        cm.getPlayer().dropMessage(5, "Upgrade succeeded!");

        if (item.getItemLevel() === 5) return cm.dispose();

        ctx.step = STEP.PREVIEW;
        return doPreview();
    }

    if (boom) {
        if (cm.haveItem(BOOM_PROTECT_SCROLL, 1)) {
            cm.gainItem(BOOM_PROTECT_SCROLL, -1);
            cm.getPlayer().dropMessage(5, "Item protected from destruction!");
            ctx.step = STEP.PREVIEW;
            return doPreview();
        }

        SubordinateManager.removeEquipFromSlot(cm.getClient(), item.getPosition());
        cm.scrollBoom(cm.getPlayer().getId());
        cm.sendOk("BOOM! Your item was destroyed.");
        return cm.dispose();
    }

    cm.scrollFail(cm.getPlayer().getId());
    cm.getPlayer().dropMessage(5, "Upgrade failed.");
    ctx.step = STEP.PREVIEW;
    return doPreview();
}

function applyNewStats(ns) {
    const item = ctx.selectedItem;
    item.setStr(ns.str);
    item.setDex(ns.dex);
    item.setInt(ns.int);
    item.setLuk(ns.luk);
    item.setWatk(ns.watk);
    item.setMatk(ns.matk);
    item.setWdef(ns.wdef);
    item.setMdef(ns.mdef);
    item.setItemLevel(ns.lvl);
    item.setLevel(ns.hiddenlvl);
    cm.getPlayer().forceUpdateItem(item);
}

// ========================= REBIRTH LOGIC =========================

function handleRebirthConfirm() {
    return doRebirth();
}

function doRebirth() {
    const item = ctx.selectedItem;
    const hands = item.getHands();

    const rebirthMat = matValues[hands + 1];
    const rebirthNxCost = Math.trunc(curvedScale(hands));

    if (!cm.haveItem(rebirthMat, 1)) {
        cm.sendOk("You need 1x#v" + rebirthMat + "# to rebirth.");
        return cm.dispose();
    }

    if (cm.getCashShop().getCash(1) < rebirthNxCost) {
        cm.sendOk("You need " + Math.trunc(rebirthNxCost / 1000) + "k NX to rebirth.");
        return cm.dispose();
    }

    SubordinateManager.rebirthItem(cm.getClient(), cm.getPlayer(), item.getPosition());

    cm.gainItem(rebirthMat, -1);
    cm.gainCash(-rebirthNxCost);
    cm.scrollPass(cm.getPlayer().getId());

    cm.sendOk("Rebirth Successful! Check your inventory for your renewed item.");
    return cm.dispose();
}

// ========================= AUTO REROLL (REG & PREM) =========================

function runAutoRerollIfEnabled() {
    if (!ctx.autoRerollEnabled) return "";

    const item = ctx.selectedItem;
    const iiReq = ItemInformationProvider.getInstance().getEquipLevelReq(item.getItemId());

    let baseMult = (ctx.mode === "REG") ? 10000 : 150000;
    let rollFee = iiReq * baseMult;
    rollFee = Math.max(rollFee, MIN_PREVIEW_FEE);

    let surcharge = (ctx.mode === "REG") ? 1.30 : 1.10;
    const perAutoCost = Math.floor(rollFee * surcharge);

    let iterations = 0;
    let extraMesosSpent = 0;

    let currentVal = getTargetStatValue(ctx.newStats);

    let virtualMeso = cm.getMeso();

    while (currentVal < ctx.autoRerollTarget) {
        if (iterations >= MAX_AUTO_ROLLS) break;

        if (virtualMeso < perAutoCost) {
            cm.getPlayer().dropMessage(5, "Auto-roll stopped: Insufficient Mesos.");
            break;
        }

        virtualMeso -= perAutoCost;
        extraMesosSpent += perAutoCost;

        ctx.newStats = (ctx.mode === "REG")
            ? calcNewStats(item, ctx.nxMultiplier, ctx.maxRate)
            : calcBetterNewStats(item, ctx.nxMultiplier, ctx.maxRate);

        iterations++;
        currentVal = getTargetStatValue(ctx.newStats);
    }

    if (extraMesosSpent > 0) cm.gainMeso(-extraMesosSpent);

    ctx.autoRerollEnabled = false;

    if (iterations === 0) return "";

    let statLabel = (ctx.mode === "REG") ? ["STR", "DEX", "INT", "LUK", "WATK", "MATK"][ctx.autoRerollStatIndex] : "All Stats";

    return (
        "\r\n#d[Auto Results - " + statLabel + "]#k\r\n" +
        "Target: x" + ctx.autoRerollTarget + "\r\n" +
        "Hit: x" + currentVal.toFixed(3) + "\r\n" +
        "Attempts: " + iterations + "\r\n" +
        "Mesos Spent: " + format(extraMesosSpent) + "\r\n"
    );
}

function getTargetStatValue(statsObj) {
    if (!statsObj || !Array.isArray(statsObj.mult)) return 1.0;

    if (ctx.mode === "REG") {
        return statsObj.mult[ctx.autoRerollStatIndex];
    } else {
        return statsObj.mult[0];
    }
}

// ========================= SALVAGE =========================

function salvageSelection(slot) {
    ctx.selectedItem = cm.getInventory(1).getItem(slot);
    const item = ctx.selectedItem;

    if (!item) return cm.dispose();

    const lvl = item.getItemLevel();
    const hands = item.getHands();

    // --- SPECIAL PITY CHECK FOR LV 2 / RB 0 ---
    if (hands === 0 && lvl === 2) {
        const pityMeso = Math.floor(5000000 * REFUND_RATE); // 1.5m
        let msg = "Salvaging this item will return:\r\n";
        msg += "#b" + format(pityMeso) + "#k in Mesos.\r\n";
        msg += "Items to be returned:\r\n";
        msg += "- 1x #v" + materials.zakDiamond + "# #d(Pity Refund)#k\r\n";
        msg += "\r\nConfirm salvage?";

        ctx.step = STEP.SALVAGE_CONFIRM;
        cm.sendYesNo(msg);
        return;
    }
    // ------------------------------------------

    const totals = getTotals(lvl, hands);
    const refundMesos = Math.floor((totals.totalFee + REBIRTH_BUCKET_MESOS * hands) * REFUND_RATE);

    let msg = "Salvaging this item will return:\r\n";
    msg += "#b" + format(refundMesos) + "#k in Mesos.\r\n";
    msg += "Items to be returned:\r\n";

    var hasMaterials = false;
    Object.keys(totals.totalMats).forEach(function (matId) {
        var amt = Math.floor(totals.totalMats[matId] * REFUND_RATE);
        if (amt > 0) {
            msg += "- " + amt + "x #v" + matId + "#\r\n";
            hasMaterials = true;
        }
    });

    if (!hasMaterials) msg += "(No recoverable materials)\r\n";

    msg += "\r\nConfirm salvage?";

    ctx.step = STEP.SALVAGE_CONFIRM;
    cm.sendYesNo(msg);
}

function handleSalvageConfirm() {
    const item = ctx.selectedItem;
    const lvl = item.getItemLevel();
    const hands = item.getHands();

    // --- SPECIAL PITY ACTION FOR LV 2 / RB 0 ---
    if (hands === 0 && lvl === 2) {
        // Refund 30% of 5m = 1.5m
        cm.gainMeso(1500000);
        // Force refund 1 Diamond
        cm.gainItem(materials.zakDiamond, 1);
        cm.getPlayer().dropMessage(5, "Pity Refund: Returned 1x Zakum Diamond.");

        SubordinateManager.removeEquipFromSlot(cm.getClient(), item.getPosition());
        cm.sendOk("Salvage complete.");
        return cm.dispose();
    }
    // -------------------------------------------

    const totals = getTotals(lvl, hands);
    const refundMesos = Math.floor((totals.totalFee + REBIRTH_BUCKET_MESOS * hands) * REFUND_RATE);

    cm.gainMeso(refundMesos);

    Object.keys(totals.totalMats).forEach(function (matId) {
        var amt = Math.floor(totals.totalMats[matId] * REFUND_RATE);
        if (amt > 0) cm.gainItem(parseInt(matId), amt);
    });

    SubordinateManager.removeEquipFromSlot(cm.getClient(), item.getPosition());
    cm.sendOk("Salvage complete.");
    return cm.dispose();
}

function getTotals(uptoLevel, hands) {
    let totalFee = 0;
    const totalMats = {};

    for (let h = 0; h < hands; h++) {
        let matId = matValues[h];
        if (matId) {
            totalMats[matId] = (totalMats[matId] || 0) + (1 + 2 + 3 + 3);
            totalFee += (5e6 + 15e6 + 45e6 + 95e6);
        }
    }

    let matIdCurrent = matValues[hands];
    // Loop FIXED to strictly less than for standard cases to accumulate past levels
    // But Pity handles the Lv 2 case explicitly now.
    for (let l = 1; l < uptoLevel; l++) {
        totalFee += (FEES[l - 1] || 0);
        if (matIdCurrent) {
            totalMats[matIdCurrent] = (totalMats[matIdCurrent] || 0) + (AMOUNTS[l - 1] || 0);
        }
    }

    return { totalFee: totalFee, totalMats: totalMats };
}

// ========================= UTILS =========================

function calcNewStats(item, nxMultiplier, maxRate) {
    const roll = function () { return MIN_RATE_BASE + Math.random() * (maxRate - MIN_RATE_BASE); };
    const defRoll = function () { return 1.1 + Math.random() * 0.1; };

    const m = [roll(), roll(), roll(), roll(), roll(), roll()];

    return {
        str: Math.floor(item.getStr() * m[0]),
        dex: Math.floor(item.getDex() * m[1]),
        int: Math.floor(item.getInt() * m[2]),
        luk: Math.floor(item.getLuk() * m[3]),
        watk: Math.floor(item.getWatk() * m[4]),
        matk: Math.floor(item.getMatk() * m[5]),
        wdef: Math.floor(item.getWdef() * defRoll()),
        mdef: Math.floor(item.getMdef() * defRoll()),
        lvl: item.getItemLevel() + 1,
        hiddenlvl: item.getLevel() + 1,
        mult: m
    };
}

function calcBetterNewStats(item, nxMultiplier, maxRate) {
    const roll = MIN_RATE_BASE + Math.random() * (maxRate - MIN_RATE_BASE);
    const defRoll = 1.1 + Math.random() * 0.1;

    const m = [roll, roll, roll, roll, roll, roll];

    return {
        str: Math.floor(item.getStr() * m[0]),
        dex: Math.floor(item.getDex() * m[1]),
        int: Math.floor(item.getInt() * m[2]),
        luk: Math.floor(item.getLuk() * m[3]),
        watk: Math.floor(item.getWatk() * m[4]),
        matk: Math.floor(item.getMatk() * m[5]),
        wdef: Math.floor(item.getWdef() * defRoll),
        mdef: Math.floor(item.getMdef() * defRoll),
        lvl: item.getItemLevel() + 1,
        hiddenlvl: item.getLevel() + 1,
        mult: m
    };
}

function format(n) {
    return cm.numberWithCommas(n) + " mesos";
}

function curvedScale(hands) {
    return 100000 * Math.pow(5000, hands / 7.0);
}