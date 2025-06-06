/* Subordinate – Free Market (9201611) NPC: Item Upgrade & Rebirth */

var zakDiamond   = 4032133;
var hTegg        = 4001094;
var rockOfTime   = 4021010;
var previewFee   = 2500000;
var boomProtectScroll = 3020003;

// For levels 1–4, define upgrade cost and required materials
var upgradeConfig = {
    1: { fee: 15000000, mats: [{ id: zakDiamond, amt: 1 }] },
    2: { fee: 45000000, mats: [{ id: zakDiamond, amt: 3 }] },
    3: { fee: 125000000, mats: [{ id: hTegg,      amt: 1 }] },
    4: { fee: 275000000, mats: [{ id: hTegg,      amt: 3 }] }
};

var totalUpgradeFee = 460000000;
var totalRebirthMats = {};

// Dialogue state
var status       = 0;
var selectedItem = null;
var isRebirth    = false;
var salvage      = false;
var betterRoll   = false;

var newStats;
var bugSelection = false;
var ii;

function start() {
    status = 0;
    cm.sendNext("Hello! I'm Slimy's Subordinate! I facilitate Weapon Upgrading and Rebirths, what do you want to do today?");
}

function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    switch (status) {
        case 1:
            return choice();
        case 2:
            return showEquipList(selection);
        case 3:
            if (salvage) return salvageSelection(selection)
            else if (bugSelection) return buggedSelection(selection)
            else return handleSelection(selection);
        case 4:
            return processConfirmation(salvage, bugSelection, newStats);
        default:
            return cm.dispose();
    }
}

// === STEP 1: List choice of action ===
function choice() {
    var selStr = "\r\n#b#L0#Regular upgrades#l" +
                 "\r\n#b#L1#Premium upgrades#l" +
                 "\r\n#b#L2#Salvage my item!#l";
//                 "\r\n#b#L3#Turn in a bugged item!#l";
    cm.sendSimple(selStr);
}

// === STEP 2: List all equip items in your inventory ===
function showEquipList(selection) {
    if (selection == 0) {
        salvage = false;
        betterRoll = false;
    } else if (selection == 1) {
        betterRoll = true;
        salvage = false;
    } else if (selection == 2) {
        betterRoll = false;
        salvage = true;
    } else if (selection == 3) {
        betterRoll = false;
        salvage = false;
        bugSelection = true;
    }
    var inv      = cm.getInventory(1);
    var limit    = inv.getSlotLimit();
    var lines    = [];

    for (var slot = 1; slot <= limit; slot++) {
        var item = inv.getItem(slot);
        if (!item) continue;
        var name = Packages.server.ItemInformationProvider
                   .getInstance().getName(item.getItemId());
        lines.push(
            "#L" + slot + "#"
            + "#v" + item.getItemId() + "# "
            + name
            + " (Lv " + item.getItemLevel() + ")"
            + "#l"
        );
    }

    if (!lines.length) {
        cm.sendOk("You have no equippable items to select.");
        return cm.dispose();
    }
    if (salvage) {
        cm.sendSimple(
                "Select the item you want to salvage:\r\n"
              + lines.join("\r\n")
            );
    } else if (bugSelection) {
        cm.sendSimple(
                "Select the item that is bugged:\r\n"
              + lines.join("\r\n")
            );
    } else {
        cm.sendSimple(
            "Select the item you want to upgrade. "
          + "It costs Item required level / 2 to preview each upgrade.\r\n"
          + lines.join("\r\n")
        );
    }
}

// === STEP 3.1: Player picks an item to Upgrade ===
function handleSelection(slot) {
    // All the conditional Checks
    selectedItem = cm.getInventory(1).getItem(slot);
    ii = Packages.server.ItemInformationProvider.getInstance().getEquipLevelReq(selectedItem.getItemId())
    if (cm.getItemName(slot).includes("Reverse") ||
        cm.getItemName(slot).includes("Timeless")) {
            cm.sendOk("You cannot upgrade or salvage any Reverse or Timeless equips!");
            return cm.dispose();
        }
    if (!selectedItem) {
        cm.sendOk("Invalid selection.");
        return cm.dispose();
    }
    if (betterRoll) {
        newStats = calcBetterNewStats(selectedItem, selectedItem.getItemId());
    } else {
        newStats = calcNewStats(selectedItem, selectedItem.getItemId());
    }

    previewFee = (betterRoll ? ii/2 * 1000000 : ii/2 * 100000) // cost of better rol is 10x more
    var lvl   = selectedItem.getItemLevel();
    var hands = selectedItem.getHands();

    // Rebirth condition: level ≥5 but low-handed weapons only
    if (lvl >= 5 && hands <= 2) {
        isRebirth = true;
        return cm.sendYesNo(
            "Your item has reached its max upgrades. I can reset it with a base stat boost.\r\n"
          + "Cost: 1x#v" + rockOfTime + "# + 350k NX. Proceed?"
        );
    }

    // Regular upgrade: level 1–4, hands ≤=3
    if (lvl >= 1 && lvl <= 4 && hands <= 3) {
        var cfg = upgradeConfig[lvl];
        if (!cfg) {
            cm.sendOk("No upgrade path configured for level " + lvl + ".");
            return cm.dispose();
        }

        if (cm.getMeso() < previewFee + cfg.fee) {
            if (cm.haveItem(3020002, 1)) {
                cm.gainItem(3020002, -1)
                cm.gainMeso(1000000000);
            } else {
                cm.sendOk("You need at least "
                    + format(previewFee + cfg.fee)
                    + " mesos to preview and perform this upgrade.");
                return cm.dispose();
            }
        }

        // Deduct preview fee
        cm.gainMeso(-previewFee);

        // Calculate tentative new stats

        var mat      = cfg.mats[0].id;
        var amt      = cfg.mats[0].amt;
        var warning  = (lvl === 4)
                     ? "\r\nWARNING: 1% chance to destroy your item!"
                     : "";

        var msg = [
            "Upgrading will change stats as follows:",
            "STR: " + selectedItem.getStr() + " to " + newStats.str,
            "DEX: " + selectedItem.getDex() + " to " + newStats.dex,
            "INT: " + selectedItem.getInt() + " to " + newStats.int,
            "LUK: " + selectedItem.getLuk() + " to " + newStats.luk,
            "WATK: " + selectedItem.getWatk() + " to " + newStats.watk,
            "MATK: " + selectedItem.getMatk() + " to " + newStats.matk,
            "WDEF: " + selectedItem.getWdef() + " to " + newStats.wdef,
            "MDEF: " + selectedItem.getMdef() + " to " + newStats.mdef,
            "Cost: " + format(cfg.fee) + " + " + amt + "x#v" + mat + "#"
        ].join("\r\n");

        return cm.sendYesNo(msg + warning);
    }

    // Otherwise, nothing to do
    cm.sendOk("Your item cannot be upgraded further or is ineligible.");
    cm.dispose();
}

// === STEP 3.2: Player picks an item to Salvage ===
function getTotals(uptoLevel) {
  let totalFee = 0;
  const totalMats = {};    // { materialId: totalAmt, … }

  // loop from 1 → uptoLevel
  for (let lvl = 1; lvl <= uptoLevel; lvl++) {
    const step = upgradeConfig[lvl-1];
    if (!step) continue;   // in case some levels are missing

    // add the fee
    totalFee += step.fee;

    // accumulate each material
    step.mats.forEach(({ id, amt }) => {
      totalMats[id] = (totalMats[id] || 0) + amt;
    });
  }

  return { totalFee, totalMats };
}

function salvageSelection(slot) {
    selectedItem = cm.getInventory(1).getItem(slot);
    if (!selectedItem) {
        cm.sendOk("Invalid selection.");
        return cm.dispose();
    }
    // nothing to salvage if level 1 and hands = 0
    if (selectedItem.getItemLevel() === 1 && selectedItem.getHands() === 0) {
        cm.sendOk("Clean item selected, nothing to salvage.");
        return cm.dispose();
    }

    const lvl        = selectedItem.getItemLevel();
    const hands      = selectedItem.getHands();
    const { totalFee, totalMats } = getTotals(lvl);

    // 1) Initialize with guaranteed returns per hand
    const matsToReturn = {};
    matsToReturn[zakDiamond] = 4 * hands;
    matsToReturn[hTegg]      = 4 * hands;

    // 2) Merge in all mats used up through this level
    Object.keys(totalMats).forEach(id => {
        const used = totalMats[id] || 0;
        matsToReturn[id] = (matsToReturn[id] || 0) + used;
    });

    // 3) Compute 20% refund of full cost (including totalUpgradeFee per hand)
    const refundMesos = Math.floor((totalFee + totalUpgradeFee * hands) * 0.2);

    // 4) Build confirmation message
    let msg = "We will refund you " + format(refundMesos) + "\r\n";
    Object.entries(matsToReturn).forEach(([id, amt]) => {
        msg += amt + "x #v" + id + "#\r\n";
    });
    if (hands > 0) {
        msg += hands + "x #v" + rockOfTime + "#\r\n";
    }
    msg += "Are you sure you want to salvage this equip?";

    cm.sendYesNo(msg);
}

// === STEP 3.3: Player submitting a bugged item ===
function buggedSelection(slot) {
    selectedItem = cm.getInventory(1).getItem(slot);
    msg = "Thanks for submitting your bugged items. I will return you an item that is at the same rebirth and same upgrade level, with an average roll of 1.5x." +
          "This Item will be removed:" +
          "\r\n#L" + slot + "#" + // show slot number
          "#v" + selectedItem.getItemId() + "# " + // show item icon
          Packages.server.ItemInformationProvider.getInstance().getName(selectedItem.getItemId()); + // Item name
          " (Lv " + selectedItem.getItemLevel() + ")" + // Item level
          "#l\r\nAre you sure?"
    cm.sendSimple(msg)
}

// === STEP 4: Player confirms upgrade or rebirth ===
function processConfirmation(salvage, bugSelection, newStats) {
    if (salvage) return salvageItem()
    else if (isRebirth) return doRebirth();
    else if (bugSelection) return doBugHandler();
    return doUpgrade(newStats);
}

function doBugHandler() {
    var lvl        = selectedItem.getItemLevel();
    var hands      = selectedItem.getHands();
    cm.bugItemHandler(selectedItem.getPosition())
    cm.sendOk('Check your new item.')
    return cm.dispose()

}

function salvageItem() {
    var lvl        = selectedItem.getItemLevel();
    var hands      = selectedItem.getHands();
    var { totalFee, totalMats } = getTotals(lvl);

    // 1) Initialize with guaranteed returns per hand
    var matsToReturn = {};
    matsToReturn[zakDiamond] = 4 * hands;
    matsToReturn[hTegg]      = 4 * hands;

    // 2) Merge in all mats used up through this level
    Object.keys(totalMats).forEach(id => {
        const used = totalMats[id] || 0;
        matsToReturn[id] = (matsToReturn[id] || 0) + used;
    });

    // 3) Compute 20% refund of full cost (including totalUpgradeFee per hand)
    const refundMesos = Math.floor((totalFee + totalUpgradeFee * hands) * 0.2);

    // 4) Build confirmation message
    var returnstr = "I have salvaged your items, please check."
    cm.gainMeso(refundMesos)
    Object.entries(matsToReturn).forEach(([id, amt]) => {
        cm.gainItem(parseInt(id), amt);
    });
    cm.gainItem(rockOfTime, hands);
    cm.gainCash(350000 * hands * 0.6);
    cm.removeItemNPC(selectedItem.getPosition());
    return cm.dispose();
}

function doUpgrade(newStats) {
    var lvl  = selectedItem.getItemLevel();
    var cfg  = upgradeConfig[lvl];
    var mat  = cfg.mats[0].id;
    var amt  = cfg.mats[0].amt;

    // Check materials
    if (!cm.haveItem(mat, amt)) {
        cm.sendOk("You lack " + amt + "x#v" + mat + "#.");
        return cm.dispose();
    }

    // Deduct cost & materials
    cm.gainMeso(-cfg.fee);
    cm.gainItem(mat, -amt);

    // Success roll
    var successRate = 1 - 0.1 * (lvl - 1);
    var boomChance  = (lvl === 4 ? 0.01 : 0);
    var roll        = Math.random();
    var success     = (roll < successRate);
    var boom        = (!success && Math.random() < boomChance);

    if (success) {
        applyNewStats(newStats);
        cm.sendOk("By the blessing from Carbo, your item has been upgraded successfully!");
        cm.scrollPass(cm.getPlayer().getId());
    } else if (boom) {
        if (cm.haveItem(boomProtectScroll, 1)) {
            cm.sendOk("BOOM SHAKA LA.. eh? what? AL AKAHS MOOB?!?! Huh? Did time rewind? Weird... What was I doing...");
            cm.gainItem(boomProtectScroll, -1)
        } else {
            cm.removeItemNPC(selectedItem.getPosition());
            cm.sendOk("BOOM SHAKA LAKA! BOOM BOOM BOOM~~ Your item has exploded into fireworks by Merogie!");
            cm.scrollFail(cm.getPlayer().getId());
        }
    } else {
        cm.sendOk("Upgrade failed. Better luck next time.");
        cm.scrollFail(cm.getPlayer().getId());
    }

    return cm.dispose();
}

function doRebirth() {
    if (selectedItem.getItemId() == 1402180 || selectedItem.getItemId() == 1382235) { // Just a double check
        cm.sendOk("Hello! Your item is already so op, you can't rebirth it!");
        cm.dispose();
        return;
    }
    // Check materials
    if (!cm.haveItem(rockOfTime, 1)) {
        cm.sendOk("You need 1x#v" + rockOfTime + "# to rebirth.");
    } else if (cm.getCashShop().getCash(1) < 350000) {
        cm.sendOk("You need 350k NX to rebirth your item.");
    } else {
        cm.rebirthItem(selectedItem.getPosition(), selectedItem.getHands());
        cm.gainItem(rockOfTime, -1);
        cm.gainCash(-350000);
        cm.scrollPass(cm.getPlayer().getId());
        cm.sendOk("Your item has been reborn. Go get stronger!");
    }
    return cm.dispose();
}

// === Helpers ===
function calcNewStats(item, itemId) {
    // Main stats 40–60% increase, defs 10–20%
    if (parseInt(itemId/10000) < 130) {
        var mm = () => 1.4 + Math.random() * 0.2;
    } else {
        var mm = () => 1.4 + Math.random() * 0.2;
    }
    var dm = () => 1.1 + Math.random() * 0.1;
    return {
        str:  Math.floor(item.getStr()  * mm()),
        dex:  Math.floor(item.getDex()  * mm()),
        int:  Math.floor(item.getInt()  * mm()),
        luk:  Math.floor(item.getLuk()  * mm()),
        watk: Math.floor(item.getWatk() * mm()),
        matk: Math.floor(item.getMatk() * mm()),
        wdef: Math.floor(item.getWdef() * dm()),
        mdef: Math.floor(item.getMdef() * dm()),
        lvl:  item.getItemLevel() + 1
    };
}

function calcBetterNewStats(item, itemId) {
    // Main stats 55–60% increase, defs 10–20%
    if (parseInt(itemId/10000) < 130) {
        var mm = 1.4 + Math.random() * 0.2;
    } else {
        var mm = 1.4 + Math.random() * 0.2;
    }
//    var mm = 1.4 + Math.random() * 0.2;
//    var dm = () => 1.1 + Math.random() * 0.1;
    var dm = 1.1 + Math.random() * 0.1;
    return {
        str:  Math.floor(item.getStr()  * mm),
        dex:  Math.floor(item.getDex()  * mm),
        int:  Math.floor(item.getInt()  * mm),
        luk:  Math.floor(item.getLuk()  * mm),
        watk: Math.floor(item.getWatk() * mm),
        matk: Math.floor(item.getMatk() * mm),
        wdef: Math.floor(item.getWdef() * dm),
        mdef: Math.floor(item.getMdef() * dm),
        lvl:  item.getItemLevel() + 1
    };
}

function applyNewStats(newStats) {
    var s = newStats;
    selectedItem.setStr(s.str);
    selectedItem.setDex(s.dex);
    selectedItem.setInt(s.int);
    selectedItem.setLuk(s.luk);
    selectedItem.setWatk(s.watk);
    selectedItem.setMatk(s.matk);
    selectedItem.setWdef(s.wdef);
    selectedItem.setMdef(s.mdef);
    selectedItem.setItemLevel(s.lvl);
    cm.getPlayer().forceUpdateItem(selectedItem);
}

function format(n) {
    return cm.numberWithCommas(n) + " mesos";
}
