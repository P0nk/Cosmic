// your material IDs
const materials = {
  zakDiamond:    4032133,
  hTegg:         4001094,
  rockOfTime:    4021010,
  vonleonSeal:   4001693,
  cygnusCirclet: 4000659,
  gigaToadPurse: 4000703,
};
const matValues = Object.values(materials);

// level-by-level parameters
const LEVELS    = [1,    2,     3,      4];
const FEES      = [15e6, 45e6, 125e6, 275e6];
const AMOUNTS   = [1,    3,     5,      7];


var nxMultiplier = false;
var nxMultiplierCost = 2000000;

// Fees + Protection scroll
var previewFee   = 2_500_000; // Fall back base intial fee
var boomProtectScroll = 3020003;

//choice
var salvage         = false; // must take precedence over upgrade
var upgradeNormal   = false;
var slot            = -1;
var reroll          = false;
var resetItem       = false;

//Upgrade variables
var selectedItem;
var newStats;
var max_rate = 1.599;
// Salvage variables
var totalUpgradeFee = 460000000;
var totalRebirthMats = {};

// Auto re-roll (premium only)
var autoRerollPremium = false;   // one-shot flag to trigger automation
var autoRerollTarget  = 0;       // e.g., 1.59

function start() {
    status = 0;
    cm.sendNext("Hello! I'm Slimy's Subordinate! I facilitate Weapon Upgrading and Rebirths, what do you want to do today?");
}

// ============================= Main NPC Chat Sequence =============================
function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    if (status === 1) {
//        Menu:
//        0: Normal Upgrade; upgradeNormal = true
//        1: Premium Upgrade; upgradeNormal = false
//        2: Salvage > status === 39
//        3: Reset Item
        return Menu();
    } else if (status === 2) {
//        Weapon selection menu to select weapon
//        if upgradeNormal: status = 19 must be 1 less than intention because of function action will status += 1
//        if not upgradeNormal: status = 29
        return weaponSelection(selection)
    } else if (status === 20) {
//        preview stats with normal roll
//        2 options given here:
//          0: Reroll
//          1: Upgrade!
//          if selection == 1 : status == 20
//          else status == 19: action(1, 0, undefined)
        if (!reroll) {
            slot = selection;
        }
        return preview(slot, upgradeNormal)
    } else if (status === 21) {
//        Handle weapon upgrade -- normal tier
//        cm.dispose()
        if (selection == 0) { // reroll
            status = 19;
            reroll = true;
            action(1, 0, 0);
            return;
        }
        doUpgrade(newStats)
    } else if (status === 30) {
        // preview stats with premium roll
        // 0: Reroll
        // 1: Upgrade!
        // 2: Auto re-roll to target (new)
        if (!reroll) {
            slot = selection;
        }
        return preview(slot, upgradeNormal);

    } else if (status === 31) {
        if (selection == 0) { // manual reroll
            status = 29;
            reroll = true;
            action(1, 0, 0);
            return;
        } else if (selection == 1) { // upgrade now
            return doUpgrade(newStats);
        } else if (selection == 2) { // set target for auto re-roll
            // Ask player for a rate between 1.40 and 1.60
            if (selectedItem.getHands() >= 4 && selectedItem.getHands() <= 6) {
                cm.sendGetText("Enter your target rate (1.40 to 1.55), e.g. 1.53:");
            } else {
                cm.sendGetText("Enter your target rate (1.40 to 1.599), e.g. 1.59:");
            }
//            status = 32;
            return;
        }
        return doUpgrade(newStats);

    } else if (status === 32) {
        // Receive target from sendGetText (premium only)
        var txt = String(cm.getText());
        var rate = parseFloat(txt);
        if (isNaN(rate) || rate < 1.40 || rate > max_rate) {
            cm.sendOk("Please enter a valid number between 1.40 and " + max_rate + ".");
            return cm.dispose();
        }
        autoRerollPremium = true;
        autoRerollTarget  = rate;

        // Re-enter premium preview with automation enabled
        reroll = true;     // keep current slot
        status = 29;       // back into premium preview step
        action(1, 0, 0);
        return;
    } else if (status === 40) {
//        Show Materials, Mesos and NX to refund if applicable
//        Use YesNo
        slot = selection;
        return salvageSelection(selection);
    } else if (status === 41) {
        return salvageItem()
    } else if (status === 42) {
//        Handle Salvage/Refund
//        cm.dispose()
    } else if (status === 50) {
        if (!reroll) {
            slot = selection
        }
        return innocenceItem(slot);
    } else if (status === 51) {
        if (selection == 0) { // reroll
            status = 49;
            reroll = true;
            action(1, 0, 0);
            return;
        } else {
            cm.sendOk("See you again!")
            return cm.dispose()
        }
    }
}
// =========================== Main NPC Chat Sequence End ===========================

// =============================== NPC Chat Functions ===============================
function Menu() {
    if (nxMultiplier) {
        nxMultSwitch = "#gON"
    } else {
        nxMultSwitch = "#rOFF"
    }
    var selStr = "\r\n#b#L0#Regular upgrades#l" +
                 "\r\n#b#L1#Premium upgrades#l" +
                 "\r\n#b#L2#Salvage my item!#l" +
                 "\r\n#b#L3#Reset my item's rebirth and levels#l";
    cm.sendSimple(selStr);
}

function weaponSelection(selection) {
    if (selection === 0) {
        upgradeNormal = true;
        status = 19; // jump to Normal Upgrade Handler
    } else if (selection === 1) {
        upgradeNormal = false;
        status = 29; // jump to Permium Upgrade Handler
    } else if (selection === 2) {
        status = 39; // jump to salvage
    } else if (selection === 3) {
        resetItem = true;
        status = 49; // jump to reset item
    } else {
        cm.sendOk("Error Encountered at weapon selection. Alert GM.")
        return cm.dispose();
    }

    var inv      = cm.getInventory(1);
    var limit    = inv.getSlotLimit();
    var lines    = [];

    for (var slot = 1; slot <= limit; slot++) {
        var item = inv.getItem(slot);
        if (!item) continue;
        var name = Packages.server.ItemInformationProvider
                   .getInstance().getName(item.getItemId());
        console.log("Hands: " + item.getHands() + "Level: " + item.getItemLevel())
        if ((cm.checkBlacklistedItem(slot) & selection === 2) || (item.getHands() >= 5 & item.getItemLevel() == 5)) { // Make sures any item they planned to salvage cant level up on their own
            continue;
        }
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
    } else if (upgradeNormal) {
        cm.sendSimple(
            "Select the item you want to upgrade. "
          + "It costs Item required level / 2 to preview each upgrade.\r\n"
          + lines.join("\r\n")
        );
    } else {
        cm.sendSimple(
            "Select the item you want to upgrade. "
          + "It costs Item required level / 20 to preview each upgrade.\r\n"
          + lines.join("\r\n")
        );
    }
}

function preview(slot, upgradeNormal) {
    // All the conditional Checks
    selectedItem = cm.getInventory(1).getItem(slot);
    ii = Packages.server.ItemInformationProvider.getInstance().getEquipLevelReq(selectedItem.getItemId())
    var lvl   = selectedItem.getItemLevel();
    var hands = selectedItem.getHands();
    var hiddenlvl = selectedItem.getLevel();
    var rebirthNxCost = curvedScale(hands)

    // Checks before previewing
    if (!selectedItem) {
        cm.sendOk("Invalid selection.");
        return cm.dispose();
    }
    if (selectedItem.getHands() >= 4 && selectedItem.getHands() <= 6) {
        max_rate = 1.549;
    }
    // Rebirth condition: level = 5 but and not rebirthed 3 times
    if (lvl == 5 && hands <= 5) {
        isRebirth = true;
        cm.sendYesNo(
            "Your item has reached its max upgrades. I can reset it with a base stat boost.\r\n"
          + "Cost: 1x#v" + matValues[hands+1] + "# + " + Math.trunc(rebirthNxCost/1000) + "k NX. Proceed?"
        );
        return;
    }
    // checks if hiddenlvl is correct ( if rb0 item lvl = 1, hiddenlvl = 0, if rb 3 item lvl = 2, hiddenlvl = 13
//    console.log(hiddenlvl)
//    console.log(lvl)
//    console.log(hands*4 + lvl - 1)
//    if ((hands*4 + lvl - 1) != hiddenlvl) {
//        cm.sendOk("Looks like you have an antique equipment... Let me help you destroy it... MUAHAHAHA");
//        return cm.dispose();
//    }

    // reworked how materials are selected
    var mat  = matValues[hands];
    var amt  = AMOUNTS[lvl-1];

    // Check materials
    if (!cm.haveItem(mat, amt)) {
        cm.sendOk("You lack " + amt + "x#v" + mat + "#.");
        return cm.dispose();
    }

    if (nxMultiplier && cm.getCashShop().getCash(1) < nxMultiplierCost) {
        cm.sendOk("You turned on NX Multiplier but don't have enough NX to roll. Don't think I'm a 5 year old kid! I'm not easy to scam.")
        return cm.dispose();
    }

    // Assigning the stats to preview
    if (upgradeNormal) {
        newStats = calcNewStats(selectedItem, selectedItem.getItemId(), nxMultiplier, max_rate);
    } else {
        console.log(max_rate);
        newStats = calcBetterNewStats(selectedItem, selectedItem.getItemId(), nxMultiplier, max_rate);
    }

    previewFee = (upgradeNormal ? ii/2 * 100000 : ii/2 * 1000000) // cost of better rol is 10x more

    // ============================ For Auto roll [Huge Chunk] ==========================
    // ===== Auto re-roll (premium only) =====
    var autoMsg = "";
    if (!upgradeNormal && autoRerollPremium && autoRerollTarget >= 1.40 && autoRerollTarget <= max_rate) {
        var perAutoCost = Math.floor(previewFee * 1.2); // +20% each automated reroll
        var iterations = 0;
        var extraMesosSpent = 0;
        var extraNxSpent = 0;

        // Helper: get the effective multiplier used for main stats in premium
        var currentMult = (Array.isArray(newStats.mult) && newStats.mult.length)
                          ? Math.max.apply(null, newStats.mult)
                          : 1.0;

        // Keep trying until we meet/exceed target, or can't afford next auto reroll
        while (currentMult < autoRerollTarget) {
            // Check mesos for another auto reroll
            if (cm.getMeso() < perAutoCost + FEES[lvl-1]) {
                    if (cm.haveItem(3020002, 1)) {
                        cm.gainItem(3020002, -1)
                        cm.gainMeso(1000000000);
                    } else {
                        cm.getPlayer().dropMessage(5, "You are declared bankrupt!");
                        break;
                    }
            }
//            if (cm.getMeso() < perAutoCost) break;
            // Optional: also ensure player still has enough to eventually pay upgrade fee (not required for preview loops)
            // if (cm.getMeso() < perAutoCost + FEES[lvl-1]) break;

            // Charge auto reroll mesos
            cm.gainMeso(-perAutoCost);
            extraMesosSpent += perAutoCost;

            // Charge NX if multiplier mode is on (mirror normal preview behavior)
            if (nxMultiplier) {
                if (cm.getCashShop().getCash(1) < nxMultiplierCost) {
                    // Not enough NX for further auto tries
                    cm.getPlayer().dropMessage(5, "Auto re-roll stopped: not enough NX.");
                    break;
                }
                cm.gainCash(-nxMultiplierCost);
                extraNxSpent += nxMultiplierCost;
            }

            // Re-roll premium stats
            newStats = calcBetterNewStats(selectedItem, selectedItem.getItemId(), nxMultiplier, max_rate);
            iterations++;
            currentMult = (Array.isArray(newStats.mult) && newStats.mult.length)
                          ? Math.max.apply(null, newStats.mult)
                          : 1.0;
        }

        // One-shot run; disable after use
        autoRerollPremium = false;

        if (iterations > 0) {
            autoMsg =
                "\r\n\r\n#d[Auto Re-roll Summary]#k\r\n" +
                "Target: x" + autoRerollTarget.toFixed(3) + "\r\n" +
                "Attempts: " + iterations + "\r\n" +
                "Best roll reached: x" + currentMult.toFixed(3) + (currentMult >= autoRerollTarget ? " #g(OK)#k" : " #r(Stopped)#k") + "\r\n" +
                "Extra mesos spent: " + Math.floor(extraMesosSpent).toLocaleString() + "\r\n" +
                (nxMultiplier ? ("Extra NX spent: " + cm.numberWithCommas(extraNxSpent) + "\r\n") : "");
        }
    }
    // ======================= Auto roll end ================================

    // Regular upgrade: level 1–4, hands ≤=3
    if (lvl >= 1 && lvl <= 4 && hands <= 6) {
        if (cm.getMeso() < previewFee + FEES[lvl-1]) {
            if (cm.haveItem(3020002, 1)) {
                cm.gainItem(3020002, -1)
                cm.gainMeso(1000000000);
            } else {
                cm.sendOk("You need at least "
                    + format(previewFee + FEES[lvl-1])
                    + " mesos to preview and perform this upgrade.");
                return cm.dispose();
            }
        }

        // Deduct preview fee
//        cm.gainMeso(-previewFee);
        if (nxMultiplier) {
            cm.gainCash(-nxMultiplierCost);
            cm.getPlayer().dropMessage(5, "You have used 2mil nx. You have " + cm.numberWithCommas(cm.getCashShop().getCash(1)) + "nx remaining.");
        }


        // Calculate tentative new stats
        var warning  = (lvl === 4)
                     ? "\r\nWARNING: 1% chance to destroy your item!"
                     : "";

        var msg = [
            "Upgrading will change stats as follows:",
            "STR: " + selectedItem.getStr() + " to " + newStats.str + " (x" + newStats.mult[0].toFixed(3) + ")",
            "DEX: " + selectedItem.getDex() + " to " + newStats.dex + " (x" + newStats.mult[1].toFixed(3) + ")",
            "INT: " + selectedItem.getInt() + " to " + newStats.int + " (x" + newStats.mult[2].toFixed(3) + ")",
            "LUK: " + selectedItem.getLuk() + " to " + newStats.luk + " (x" + newStats.mult[3].toFixed(3) + ")",
            "WATK: " + selectedItem.getWatk() + " to " + newStats.watk + " (x" + newStats.mult[4].toFixed(3) + ")",
            "MATK: " + selectedItem.getMatk() + " to " + newStats.matk + " (x" + newStats.mult[5].toFixed(3) + ")",
            "WDEF: " + selectedItem.getWdef() + " to " + newStats.wdef,
            "MDEF: " + selectedItem.getMdef() + " to " + newStats.mdef,
            "Cost: " + format(FEES[lvl-1]) + " + " + amt + "x#v" + mat + "#"
        ].join("\r\n");

        var menu = "\r\n#L0#Reroll preview stats#l\r\n#L1#Proceed with upgrade#l";
        if (!upgradeNormal) {
            menu += "\r\n#L2#Auto re-roll to target#l"; // premium-only option
        }
        return cm.sendSimple(msg + warning + autoMsg + menu);

    } else {
        cm.sendOk("No upgrade path configured for level " + lvl + ".");
        return cm.dispose();
    }
    // Otherwise, nothing to do
    cm.sendOk("Your item cannot be upgraded further or is ineligible.");
    cm.dispose();
}

function calcNewStats(item, itemId, nxMultiplier, max_rate) {
    // Main stats 40–60% increase, defs 10–20%
//    if (parseInt(itemId/10000) < 130) {
    if (nxMultiplier) {
        var mm = () => 1.4 + Math.random() * 0.22;
    } else {
        console.log(max_rate);
        var mm = () => 1.4 + Math.random() * (max_rate - 1.4);
    }

    var dm = () => 1.1 + Math.random() * 0.1;
    var values = Array.from({ length: 6 }, mm);
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

function calcBetterNewStats(item, itemId, nxMultiplier, max_rate) {
    // Main stats 55–60% increase, defs 10–20%
//    if (parseInt(itemId/10000) < 130) {
//        var mm = 1.4 + Math.random() * 0.2;
//    } else {
//        var mm = 1.4 + Math.random() * 0.2;
//    }
    if (nxMultiplier) {
        var mm = 1.4 + Math.random() * 0.22;
    } else {
        console.log(max_rate)
        var mm = 1.4 + Math.random() * (max_rate - 1.4);
    }
//    var mm = 1.4 + Math.random() * 0.2;
//    var dm = () => 1.1 + Math.random() * 0.1;
    var dm = 1.1 + Math.random() * 0.1;
    var values = new Array(6).fill(mm);
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

function doUpgrade(newStats) {
    var lvl   = selectedItem.getItemLevel();
    var hands = selectedItem.getHands();

    // Rebirth handoff
    if (lvl == 5) {
        console.log('Rebirth')
        return doRebirth();
    }

    var mat = matValues[hands];
    var amt = AMOUNTS[lvl - 1];

    if (!cm.haveItem(mat, amt)) {
        cm.sendOk("You lack " + amt + "x#v" + mat + "#.");
        return cm.dispose();
    }

    // Pay cost
    cm.gainMeso(-FEES[lvl - 1]);
    cm.gainItem(mat, -amt);

    // Roll outcomes
    var successRate = 1 - 0.1 * (lvl - 1);
    var boomChance  = (lvl === 4 ? 0.005 : 0);
    var roll        = Math.random();
    var success     = (roll < successRate);
    var boom        = (!success && Math.random() < boomChance);

    // Where to loop back: normal -> 19, premium -> 29
    var loopStatus = upgradeNormal ? 19 : 29;

    if (success) {
        applyNewStats(newStats);
        cm.scrollPass(cm.getPlayer().getId());
        cm.getPlayer().dropMessage(5, "Upgrade succeeded!");
        // loop back to preview without changing the selected slot
        if (hands == 5 && lvl == 4) {
            return cm.dispose
        }
        reroll = true;          // <— keep current slot
        status = loopStatus;
        action(1, 0, 0);
        return;
    }

    if (boom) {
        if (cm.haveItem(boomProtectScroll, 1)) {
            cm.gainItem(boomProtectScroll, -1);
            cm.getPlayer().dropMessage(5, "Your item would have boomed, but Protection saved it!");
            // loop back again
            reroll = true;
            status = loopStatus;
            action(1, 0, 0);
            return;
        } else {
            cm.removeItemNPC(selectedItem.getPosition());
            cm.scrollBoom(cm.getPlayer().getId());
            cm.sendOk("BOOM! Your item exploded.");
            return cm.dispose();
        }
    }

    // Failure (no boom)
    cm.scrollFail(cm.getPlayer().getId());
    cm.getPlayer().dropMessage(5, "Upgrade failed.");
    reroll = true;
    status = loopStatus;
    action(1, 0, 0);
    return;
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
    selectedItem.setLevel(s.hiddenlvl)
    cm.getPlayer().forceUpdateItem(selectedItem);
}

function doRebirth() {
    if (selectedItem.getItemId() == 1402180 || selectedItem.getItemId() == 1382235) { // Just a double check for Kaiserium and Alicia
        cm.sendOk("Hello! Your item is already so op, you can't rebirth it!");
        cm.dispose();
        return;
    }
    var hands = selectedItem.getHands();
    var rebirthMat = matValues[hands+1]
    var rebirthNxCost = Math.trunc(curvedScale(hands))
    // Check materials
    if (!cm.haveItem(rebirthMat, 1)) {
        cm.sendOk("You need 1x#v" + rebirthMat + "# to rebirth.");
    } else if (cm.getCashShop().getCash(1) < rebirthNxCost) {
        cm.sendOk("You need " + Math.trunc(rebirthNxCost/1000) + "k NX to rebirth your item.");
    } else {
        cm.rebirthItem(selectedItem.getPosition(), selectedItem.getHands());
        cm.gainItem(rebirthMat, -1);
        cm.gainCash(-rebirthNxCost);
        cm.scrollPass(cm.getPlayer().getId());
        cm.sendOk("Your item has been reborn. Go get stronger!");
    }
    return cm.dispose();
}

function format(n) {
    return cm.numberWithCommas(n) + " mesos";
}

function getTotals(uptoLevel, hands) {
    /*
    Function to handle the materials to refund
    Will loop through the levels and  and decide the total materials to refund for that rebrith
    */
  let totalFee = 0;
  const totalMats = {};    // { materialId: totalAmt, … }
//  if (0 <= hands <= 3) { // able to configure to hand different upgradeConfigRb0-3 if needed here
//    upgradeConfig = upgradeConfigRb0
//  }

  // loop from 1 → uptoLevel
  for (let lvl = 1; lvl <= uptoLevel; lvl++) {
//    const step = upgradeConfig[lvl-1];
//    if (!step) continue;   // in case some levels are missing

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
//    cm.gainCash(350000 * hands * 0.6);
    cm.removeItemNPC(selectedItem.getPosition());
    return cm.dispose();
}

function innocenceItem(slot) {
    selectedItem = cm.getInventory(1).getItem(slot);
    ii = Packages.server.ItemInformationProvider.getInstance().getEquipLevelReq(selectedItem.getItemId())
    var lvl   = selectedItem.getItemLevel();
    var hands = selectedItem.getHands();
    if (!selectedItem) {
        cm.sendOk("Invalid selection.");
        return cm.dispose();
    }

    newItem = cm.getInventory(1).getItem(cm.replaceItem(slot))
    cm.gainCash(-100_000);
    cm.sendSimple("Item has been reset. Item Stats:\r\n" + listNonZeroStats(newItem) +"\r\n#b#L0#Roll again...#l\r\n#b#L1#thats good enough!#l")
}

function listNonZeroStats(item) {
  const stats = [
    { label: "STR", fn: () => item.getStr() },
    { label: "DEX", fn: () => item.getDex() },
    { label: "INT", fn: () => item.getInt() },
    { label: "LUK", fn: () => item.getLuk() },
    { label: "WATK", fn: () => item.getWatk() },
    { label: "MATK", fn: () => item.getMatk() },
    { label: "HP", fn: () => item.getHp() },
    { label: "MP", fn: () => item.getMp() },
    { label: "WDEF", fn: () => item.getWdef() },
    { label: "MDEF", fn: () => item.getMdef() },
    { label: "Speed", fn: () => item.getSpeed() },
    { label: "Jump", fn: () => item.getJump() },
    { label: "Acc", fn: () => item.getAcc() },
    { label: "Avoid", fn: () => item.getAvoid() },
  ];

  const lines = stats
    .map(({ label, fn }) => {
      const val = fn();
      return val !== 0 ? `${label}: ${val}` : null;
    })
    .filter(line => line !== null);

  return lines.join("\r\n");
}

function curvedScale(hands) {
//        Used for scaling rebirth cost
//        0 → 100,000
//        1 → 197,128
//        2 → 531,813
//        3 → 1,696,096
//        4 → 6,123,898
//        5 → 24,459,082
//        6 → 106,478,473
//        7 → 500,000,000
    var start = 100_000.0;
    var end   = 500_000_000.0;
    var p     = 1.3;                // tweak this for more/less curve
    var t     = hands / 7.0;
    var r     = end / start;        // 5000
    return start * Math.pow(r, Math.pow(t, p));
}