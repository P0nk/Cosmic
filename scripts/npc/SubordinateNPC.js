/*
 * Subordinate 3.1 — Explicit Java Manager Calls (9201611.js)
 *
 * Fixes:
 * 1) INTRO step added so the NPC correctly shows the menu after the initial sendNext().
 *    (Prevents the "Next" click from being interpreted as menu selection 0.)
 *
 * Notes:
 * - Still calls SubordinateManager via Java.type(...) like your gambling NPCs.
 * - Minimum level change is NOT done in JS; it must be enforced inside SubordinateManager + equip checks.
 */

var SubordinateManager = Java.type("server.subordinate.SubordinateManager");
var ItemInformationProvider = Java.type("server.ItemInformationProvider");

// ========================= CONFIG =========================

const materials = {
  zakDiamond:    4032133,
  hTegg:         4001094,
  rockOfTime:    4021010,
  vonleonSeal:   4001693,
  cygnusCirclet: 4000659,
  gigaToadPurse: 4000703,
};
const matValues = Object.values(materials);

const FEES    = [5e6, 15e6, 45e6, 95e6];
const AMOUNTS = [1, 2, 3, 3];

const REFUND_RATE = 0.3;
const MAX_AUTO_ROLLS = 100;

const NX_MULT_COST = 2_000_000;
const BOOM_PROTECT_SCROLL = 3020003;

const BCOIN_ITEM_ID = 3020002;
const BCOIN_VALUE   = 1_000_000_000;

const REBIRTH_BUCKET_MESOS = 150_000_000;

// ========================= STATE MACHINE =========================

const STEP = {
  INTRO: "INTRO",                 // NEW: handles the "Next" click after sendNext()
  MENU: "MENU",
  PICK_ITEM: "PICK_ITEM",
  PREVIEW: "PREVIEW",
  AUTO_TARGET_INPUT: "AUTO_TARGET_INPUT",
  REBIRTH_CONFIRM: "REBIRTH_CONFIRM",
  SALVAGE_CONFIRM: "SALVAGE_CONFIRM",
  RESET_RESULT: "RESET_RESULT",
};

let ctx = null;

function start() {
  ctx = freshCtx();
  ctx.step = STEP.INTRO;
  cm.sendNext("Hello! I'm Slimy's Subordinate! I facilitate Weapon Upgrading and Rebirths, what do you want to do today?");
}

function freshCtx() {
  return {
    step: STEP.MENU,
    mode: null,                 // "REG" | "PREM" | "SALVAGE" | "RESET"
    slot: -1,
    selectedItem: null,
    newStats: null,

    nxMultiplier: false,        // kept as in your original; you can add toggle later
    maxRate: 1.599,

    previewFee: 0,

    // premium auto reroll
    autoRerollEnabled: false,
    autoRerollTarget: 0,
  };
}

function action(mode, type, selection) {
  if (mode !== 1) return cm.dispose();

  switch (ctx.step) {
    case STEP.INTRO:
      // IMPORTANT: do NOT treat the first click as a menu choice.
      ctx.step = STEP.MENU;
      return showMenu(); // call without selection to display menu

    case STEP.MENU:
      return showMenu(selection);

    case STEP.PICK_ITEM:
      return handlePickItem(selection);

    case STEP.PREVIEW:
      return handlePreviewChoice(selection);

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
  "Good Day, I'm Subordinate. I provide item empowering and rebirthing service so long you can provide the necessary resources. Pick a service.\r\n\r\n" +

  "#b#L0#[Regular]#k Each stat is rolled independently.\r\n" +
  "      Balanced results with natural variation across stats.#l\r\n\r\n" +

  "#b#L1#[Premium]#k One roll applied to all stats.\r\n" +
  "      Higher consistency and better peak potential.#l\r\n\r\n" +

  "#b#L2#[Salvage]#k Scrap an upgraded item for partial refunds.\r\n" +
  "      Recover some mesos and materials based on progress.#l";
cm.sendSimple(menu);

    return;
  }

  if (selection === 0) ctx.mode = "REG";
  else if (selection === 1) ctx.mode = "PREM";
  else if (selection === 2) ctx.mode = "SALVAGE";
  else if (selection === 3) ctx.mode = "RESET";
  else return cm.dispose();

  ctx.step = STEP.PICK_ITEM;
  return showItemList();
}

function showItemList() {
  const inv = cm.getInventory(1);
  const limit = inv.getSlotLimit();
  const lines = [];

  for (let s = 1; s <= limit; s++) {
    const item = inv.getItem(s);
    if (!item) continue;

    const itemId = item.getItemId();
    const name = ItemInformationProvider.getInstance().getName(itemId);

    // Filters
    if (ctx.mode === "SALVAGE") {
      if (cm.checkBlacklistedItem(s)) continue;
    } else if (ctx.mode === "REG" || ctx.mode === "PREM") {
      if (item.getHands() >= 5 && item.getItemLevel() === 5) continue;
    }

    lines.push(
      "#L" + s + "#"
      + "#v" + itemId + "# " + name
      + " (Lv " + item.getItemLevel() + ")"
      + "#l"
    );
  }

  if (!lines.length) {
    cm.sendOk("You have no equippable items to select.");
    return cm.dispose();
  }

  const hint =
    (ctx.mode === "REG") ? "It costs Item required level / 2 to preview each upgrade.\r\n"
  : (ctx.mode === "PREM") ? "It costs Item required level / 20 to preview each upgrade.\r\n"
  : "";

  cm.sendSimple("Select the item you want to proceed with.\r\n" + hint + lines.join("\r\n"));
}

// ========================= PICK ITEM =========================

function handlePickItem(slot) {
  ctx.slot = slot;
  ctx.selectedItem = cm.getInventory(1).getItem(slot);

  if (!ctx.selectedItem) {
    cm.sendOk("Invalid selection.");
    return cm.dispose();
  }

  if (ctx.mode === "RESET") {
    return doReset(slot);
  }

  if (ctx.mode === "SALVAGE") {
    ctx.step = STEP.SALVAGE_CONFIRM;
    return salvageSelection(slot);
  }

  // REG/PREM
  ctx.step = STEP.PREVIEW;
  return doPreview();
}

// ========================= PREVIEW =========================

function doPreview() {
  const item = ctx.selectedItem;
  const iiReq = ItemInformationProvider.getInstance().getEquipLevelReq(item.getItemId());
  const lvl = item.getItemLevel();
  const hands = item.getHands();

    ctx.maxRate = 1.4; // global cap now

  // Rebirth prompt
  if (lvl === 5 && hands <= 5) {
    const rebirthMat = matValues[hands + 1];
    const rebirthNxCost = Math.trunc(curvedScale(hands));
    ctx.step = STEP.REBIRTH_CONFIRM;

    cm.sendYesNo(
      "Your item has reached its max upgrades. I can reset it with a base stat boost.\r\n"
      + "Cost: 1x#v" + rebirthMat + "# + " + Math.trunc(rebirthNxCost / 1000) + "k NX. Proceed?"
    );
    return;
  }

  const mat = matValues[hands];
  const amt = AMOUNTS[lvl - 1];

  if (!cm.haveItem(mat, amt)) {
    cm.sendOk("You lack " + amt + "x#v" + mat + "#.");
    return cm.dispose();
  }

  if (ctx.nxMultiplier && cm.getCashShop().getCash(1) < NX_MULT_COST) {
    cm.sendOk("You turned on NX Multiplier but don't have enough NX to roll.");
    return cm.dispose();
  }

  // Roll preview stats
  ctx.newStats = (ctx.mode === "REG")
    ? calcNewStats(item, ctx.nxMultiplier, ctx.maxRate)
    : calcBetterNewStats(item, ctx.nxMultiplier, ctx.maxRate);

  // Preview fee
  ctx.previewFee = (ctx.mode === "REG")
    ? (iiReq / 2) * 100_000
    : (iiReq / 2) * 1_000_000;

  // Auto reroll (premium only) - runs once if enabled
  const autoMsg = (ctx.mode === "PREM") ? runPremiumAutoRerollIfEnabled() : "";

  // validate upgrade path
  if (lvl < 1 || lvl > 4 || hands > 6) {
    cm.sendOk("No upgrade path configured for this item.");
    return cm.dispose();
  }

  // Ensure player can afford preview + upgrade
  const needMesos = ctx.previewFee + FEES[lvl - 1];
  if (cm.getMeso() < needMesos) {
    if (cm.haveItem(BCOIN_ITEM_ID, 1)) {
      cm.gainItem(BCOIN_ITEM_ID, -1);
      cm.gainMeso(BCOIN_VALUE);
    } else {
      cm.sendOk("You need at least " + format(needMesos) + " mesos to preview and perform this upgrade.");
      return cm.dispose();
    }
  }

  // Charge preview
  cm.gainMeso(-ctx.previewFee);
  if (ctx.nxMultiplier) {
    cm.gainCash(-NX_MULT_COST);
    cm.getPlayer().dropMessage(5, "You have used 2mil NX. Remaining NX: " + cm.numberWithCommas(cm.getCashShop().getCash(1)));
  }

  const s = ctx.newStats;
  const warning = (lvl === 4) ? "\r\nWARNING: 1% chance to destroy your item!" : "";

  const msg =
    "Upgrading will change stats as follows:\r\n" +
    "STR: " + item.getStr()  + " to " + s.str  + " (x" + s.mult[0].toFixed(3) + ")\r\n" +
    "DEX: " + item.getDex()  + " to " + s.dex  + " (x" + s.mult[1].toFixed(3) + ")\r\n" +
    "INT: " + item.getInt()  + " to " + s.int  + " (x" + s.mult[2].toFixed(3) + ")\r\n" +
    "LUK: " + item.getLuk()  + " to " + s.luk  + " (x" + s.mult[3].toFixed(3) + ")\r\n" +
    "WATK: " + item.getWatk() + " to " + s.watk + " (x" + s.mult[4].toFixed(3) + ")\r\n" +
    "MATK: " + item.getMatk() + " to " + s.matk + " (x" + s.mult[5].toFixed(3) + ")\r\n" +
    "WDEF: " + item.getWdef() + " to " + s.wdef + "\r\n" +
    "MDEF: " + item.getMdef() + " to " + s.mdef + "\r\n" +
    "Cost: " + format(FEES[lvl - 1]) + " + " + amt + "x#v" + mat + "#";

  let menu =
    "\r\n#L0#Reroll preview stats#l" +
    "\r\n#L1#Proceed with upgrade#l";

  if (ctx.mode === "PREM") {
    menu += "\r\n#L2#Auto re-roll to target#l";
  }

  ctx.step = STEP.PREVIEW;
  cm.sendSimple(msg + warning + autoMsg + menu);
}

function handlePreviewChoice(selection) {
  if (selection === 0) return doPreview();

  if (selection === 2 && ctx.mode === "PREM") {
    ctx.step = STEP.AUTO_TARGET_INPUT;
    const hands = ctx.selectedItem.getHands();
    if (hands >= 4 && hands <= 6) {
      cm.sendGetText("Enter your target rate (1.40 to 1.55), e.g. 1.53:");
    } else {
      cm.sendGetText("Enter your target rate (1.40 to 1.599), e.g. 1.59:");
    }
    return;
  }

  return doUpgrade(ctx.newStats);
}

function handleAutoTargetEntered() {
  const rate = parseFloat(String(cm.getText()));
  if (isNaN(rate) || rate < 1.40 || rate > ctx.maxRate) {
    cm.sendOk("Please enter a valid number between 1.40 and " + ctx.maxRate + ".");
    return cm.dispose();
  }

  ctx.autoRerollEnabled = true;
  ctx.autoRerollTarget = rate;

  ctx.step = STEP.PREVIEW;
  return doPreview();
}

// ========================= UPGRADE / REBIRTH =========================

function doUpgrade(newStats) {
  const item = ctx.selectedItem;
  const lvl = item.getItemLevel();
  const hands = item.getHands();

  if (lvl === 5) return doRebirth(); // safety

  const mat = matValues[hands];
  const amt = AMOUNTS[lvl - 1];

  if (!cm.haveItem(mat, amt)) {
    cm.sendOk("You lack " + amt + "x#v" + mat + "#.");
    return cm.dispose();
  }

  cm.gainMeso(-FEES[lvl - 1]);
  cm.gainItem(mat, -amt);

  const successRate = 1 - 0.1 * (lvl - 2);
  const boomChance  = (lvl === 4 ? 0.005 : 0);

  const roll = Math.random();
  const success = (roll < successRate);
  const boom = (!success && Math.random() < boomChance);

  if (success) {
    applyNewStats(newStats);
    cm.scrollPass(cm.getPlayer().getId());
    cm.getPlayer().dropMessage(5, "Upgrade succeeded!");

    if (hands === 5 && lvl === 4) return cm.dispose();

    ctx.step = STEP.PREVIEW;
    return doPreview();
  }

  if (boom) {
    if (cm.haveItem(BOOM_PROTECT_SCROLL, 1)) {
      cm.gainItem(BOOM_PROTECT_SCROLL, -1);
      cm.getPlayer().dropMessage(5, "Your item would have boomed, but Protection saved it!");
      ctx.step = STEP.PREVIEW;
      return doPreview();
    }

    SubordinateManager.removeEquipFromSlot(cm.getClient(), item.getPosition());
    cm.scrollBoom(cm.getPlayer().getId());
    cm.sendOk("BOOM! Your item exploded.");
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

function handleRebirthConfirm() {
  return doRebirth();
}

function doRebirth() {
  const item = ctx.selectedItem;

  if (item.getItemId() === 1402180 || item.getItemId() === 1382235) {
    cm.sendOk("Hello! Your item is already so op, you can't rebirth it!");
    return cm.dispose();
  }

  const hands = item.getHands();
  const rebirthMat = matValues[hands + 1];
  const rebirthNxCost = Math.trunc(curvedScale(hands));

  if (!cm.haveItem(rebirthMat, 1)) {
    cm.sendOk("You need 1x#v" + rebirthMat + "# to rebirth.");
    return cm.dispose();
  }

  if (cm.getCashShop().getCash(1) < rebirthNxCost) {
    cm.sendOk("You need " + Math.trunc(rebirthNxCost / 1000) + "k NX to rebirth your item.");
    return cm.dispose();
  }

  SubordinateManager.rebirthItem(cm.getClient(), cm.getPlayer(), item.getPosition());

  cm.gainItem(rebirthMat, -1);
  cm.gainCash(-rebirthNxCost);
  cm.scrollPass(cm.getPlayer().getId());
  cm.sendOk("Your item has been reborn. Go get stronger!");
  return cm.dispose();
}

// ========================= PREMIUM AUTO REROLL =========================

function runPremiumAutoRerollIfEnabled() {
  if (!ctx.autoRerollEnabled) return "";
  if (ctx.autoRerollTarget < 1.40 || ctx.autoRerollTarget > ctx.maxRate) {
    ctx.autoRerollEnabled = false;
    return "";
  }

  const item = ctx.selectedItem;
  const lvl = item.getItemLevel();
  const perAutoCost = Math.floor(ctx.previewFee * 1.05);

  let iterations = 0;
  let extraMesosSpent = 0;
  let extraNxSpent = 0;

  let best = getBestMult(ctx.newStats);
  let rollCount = 0;

  while (best < ctx.autoRerollTarget) {
    if (rollCount >= MAX_AUTO_ROLLS) {
      cm.getPlayer().dropMessage(5, "You rolled 100 times but couldn't get the desired multiplier... Better luck next time!");
      break;
    }

    if (cm.getMeso() < perAutoCost + FEES[lvl - 1]) {
      if (cm.haveItem(BCOIN_ITEM_ID, 1)) {
        cm.gainItem(BCOIN_ITEM_ID, -1);
        cm.gainMeso(BCOIN_VALUE);
      } else {
        cm.getPlayer().dropMessage(5, "You are declared bankrupt!");
        break;
      }
    }

    rollCount++;
    cm.gainMeso(-perAutoCost);
    extraMesosSpent += perAutoCost;

    if (ctx.nxMultiplier) {
      if (cm.getCashShop().getCash(1) < NX_MULT_COST) {
        cm.getPlayer().dropMessage(5, "Auto re-roll stopped: not enough NX.");
        break;
      }
      cm.gainCash(-NX_MULT_COST);
      extraNxSpent += NX_MULT_COST;
    }

    ctx.newStats = calcBetterNewStats(item, ctx.nxMultiplier, ctx.maxRate);
    iterations++;
    best = getBestMult(ctx.newStats);
  }

  ctx.autoRerollEnabled = false;

  if (iterations <= 0) return "";

  return (
    "\r\n\r\n#d[Auto Re-roll Summary]#k\r\n" +
    "Target: x" + ctx.autoRerollTarget.toFixed(3) + "\r\n" +
    "Attempts: " + iterations + "\r\n" +
    "Best roll reached: x" + best.toFixed(3) + (best >= ctx.autoRerollTarget ? " #g(OK)#k" : " #r(Stopped)#k") + "\r\n" +
    "Extra mesos spent: " + Math.floor(extraMesosSpent).toLocaleString() + "\r\n" +
    (ctx.nxMultiplier ? ("Extra NX spent: " + cm.numberWithCommas(extraNxSpent) + "\r\n") : "")
  );
}

function getBestMult(statsObj) {
  if (!statsObj || !Array.isArray(statsObj.mult) || !statsObj.mult.length) return 1.0;
  return Math.max.apply(null, statsObj.mult);
}

// ========================= SALVAGE =========================

function salvageSelection(slot) {
  ctx.selectedItem = cm.getInventory(1).getItem(slot);
  const item = ctx.selectedItem;

  if (!item) {
    cm.sendOk("Invalid selection.");
    return cm.dispose();
  }

  if (item.getItemLevel() === 1 && item.getHands() === 0) {
    cm.sendOk("Clean item selected, nothing to salvage.");
    return cm.dispose();
  }

  const lvl = item.getItemLevel();
  const hands = item.getHands();
  const totals = getTotals(lvl, hands);

  const matsToReturn = {};
  Object.keys(totals.totalMats).forEach(id => {
    matsToReturn[id] = (matsToReturn[id] || 0) + (totals.totalMats[id] || 0);
  });

  const refundMesos = Math.floor((totals.totalFee + REBIRTH_BUCKET_MESOS * hands) * REFUND_RATE);

  let msg = "We will refund you " + format(refundMesos) + "\r\n";
  Object.entries(matsToReturn).forEach(([id, amt]) => {
    msg += amt + "x #v" + id + "#\r\n";
  });
  msg += "Are you sure you want to salvage this equip?";

  ctx.step = STEP.SALVAGE_CONFIRM;
  cm.sendYesNo(msg);
}

function handleSalvageConfirm() {
  return salvageItem();
}

function salvageItem() {
  const item = ctx.selectedItem;
  if (!item) return cm.dispose();

  const lvl = item.getItemLevel();
  const hands = item.getHands();
  const totals = getTotals(lvl, hands);

  const matsToReturn = {};
  Object.keys(totals.totalMats).forEach(id => {
    matsToReturn[id] = (matsToReturn[id] || 0) + (totals.totalMats[id] || 0);
  });

  const refundMesos = Math.floor((totals.totalFee + REBIRTH_BUCKET_MESOS * hands) * REFUND_RATE);

  cm.gainMeso(refundMesos);
  Object.entries(matsToReturn).forEach(([id, amt]) => {
    cm.gainItem(parseInt(id, 10), amt);
  });

  SubordinateManager.removeEquipFromSlot(cm.getClient(), item.getPosition());
  return cm.dispose();
}

function getTotals(uptoLevel, hands) {
  let totalFee = 0;
  const totalMats = {};
  const maxLevelPerHand = 4;

  for (let h = 0; h < hands; h++) {
    const matId = matValues[h];
    for (let l = 1; l <= maxLevelPerHand; l++) {
      totalFee += (FEES[l - 1] || 0);
      const amt = (AMOUNTS[l - 1] || 0);
      if (matId && amt > 0) totalMats[matId] = (totalMats[matId] || 0) + amt;
    }
  }

  const currentMatId = matValues[hands];
  const doneLevelsThisHand = Math.max(0, Math.min(maxLevelPerHand, (uptoLevel - 1)));
  for (let l = 1; l <= doneLevelsThisHand; l++) {
    totalFee += (FEES[l - 1] || 0);
    const amt = (AMOUNTS[l - 1] || 0);
    if (currentMatId && amt > 0) totalMats[currentMatId] = (totalMats[currentMatId] || 0) + amt;
  }

  return { totalFee, totalMats };
}

// ========================= RESET (explicit manager) =========================

function doReset(slot) {
  if (cm.getCashShop().getCash(1) < 100_000) {
    cm.sendOk("You need 100k NX to reset your item.");
    return cm.dispose();
  }

  const newSlot = SubordinateManager.replaceWithCleanCopy(cm.getClient(), cm.getPlayer(), slot);
  if (newSlot <= 0) {
    cm.sendOk("Failed to reset item (no slot?).");
    return cm.dispose();
  }

  cm.gainCash(-100_000);

  const newItem = cm.getInventory(1).getItem(newSlot);
  ctx.slot = newSlot;
  ctx.step = STEP.RESET_RESULT;

  cm.sendSimple(
    "Item has been reset. Item Stats:\r\n" +
    listNonZeroStats(newItem) +
    "\r\n#b#L0#Roll again...#l\r\n#b#L1#thats good enough!#l"
  );
}

function handleResetResult(selection) {
  if (selection === 0) {
    ctx.step = STEP.PICK_ITEM;
    ctx.mode = "RESET";
    return doReset(ctx.slot);
  }

  cm.sendOk("See you again!");
  return cm.dispose();
}

// ========================= Stat Rollers =========================

function calcNewStats(item, nxMultiplier, maxRate) {
  const min = 1.10;
  const max = 1.40;

  const mm = () => min + Math.random() * (max - min);
  const dm = () => min + Math.random() * (max - min);

  const values = Array.from({ length: 6 }, mm);

  return {
    str:  Math.floor(item.getStr()  * values[0]),
    dex:  Math.floor(item.getDex()  * values[1]),
    int:  Math.floor(item.getInt()  * values[2]),
    luk:  Math.floor(item.getLuk()  * values[3]),
    watk: Math.floor(item.getWatk() * values[4]),
    matk: Math.floor(item.getMatk() * values[5]),
    wdef: Math.floor(item.getWdef() * dm()),
    mdef: Math.floor(item.getMdef() * dm()),
    lvl:  item.getItemLevel() + 1,
    hiddenlvl: item.getLevel() + 1,
    mult: values
  };
}

function calcBetterNewStats(item, nxMultiplier, maxRate) {
  const min = 1.10;
  const max = 1.40;

  const roll = min + Math.random() * (max - min);
  const dm   = min + Math.random() * (max - min);

  const values = new Array(6).fill(roll);

  return {
    str:  Math.floor(item.getStr()  * values[0]),
    dex:  Math.floor(item.getDex()  * values[1]),
    int:  Math.floor(item.getInt()  * values[2]),
    luk:  Math.floor(item.getLuk()  * values[3]),
    watk: Math.floor(item.getWatk() * values[4]),
    matk: Math.floor(item.getMatk() * values[5]),
    wdef: Math.floor(item.getWdef() * dm),
    mdef: Math.floor(item.getMdef() * dm),
    lvl:  item.getItemLevel() + 1,
    hiddenlvl: item.getLevel() + 1,
    mult: values
  };
}


// ========================= Utils =========================

function listNonZeroStats(item) {
  if (!item) return "(no item)";
  const stats = [
    ["STR", () => item.getStr()],
    ["DEX", () => item.getDex()],
    ["INT", () => item.getInt()],
    ["LUK", () => item.getLuk()],
    ["WATK", () => item.getWatk()],
    ["MATK", () => item.getMatk()],
    ["HP", () => item.getHp()],
    ["MP", () => item.getMp()],
    ["WDEF", () => item.getWdef()],
    ["MDEF", () => item.getMdef()],
    ["Speed", () => item.getSpeed()],
    ["Jump", () => item.getJump()],
    ["Acc", () => item.getAcc()],
    ["Avoid", () => item.getAvoid()],
  ];

  const lines = [];
  for (let i = 0; i < stats.length; i++) {
    const label = stats[i][0];
    const val = stats[i][1]();
    if (val !== 0) lines.push(label + ": " + val);
  }
  return lines.join("\r\n");
}

function format(n) {
  return cm.numberWithCommas(n) + " mesos";
}

function curvedScale(hands) {
  const start = 100_000.0;
  const end = 500_000_000.0;
  const p = 1.3;
  const t = hands / 7.0;
  const r = end / start;
  return start * Math.pow(r, Math.pow(t, p));
}
