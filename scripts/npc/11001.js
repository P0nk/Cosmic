/* Subordinate – Free Market (9201611) NPC: Item Upgrade & Rebirth */

// Constants
const equipInv = 1;
const useInv = 2;

var status = 0;

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
var whiteScrollInvSlot = null;
var whiteScrollId = 2340000;

// Auto-hammer variables

function start() {
    status = 0;
    cm.sendNext("Hello! I'm Otto Scroller");
}

function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    switch (status) {
        case 1:
            return showScrollableEquips();
        case 2:
            return showApplicableScrolls(selection);
        case 3:
            return confirmScrollCount(selection);
        case 4:
            return scrollItemOrStop(mode);
        default:
            return cm.dispose();
    }
}

function showScrollableEquips() {
    var inv = cm.getInventory(equipInv);
    var lines = [];

    for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {
        var item = inv.getItem(slot);
        if (!item) continue;
        var upgradeSlots = inv.getUpgradeSlots(slot);
        if (upgradeSlots) {
            var name = Packages.server.ItemInformationProvider.getInstance().getName(item.getItemId());
            lines.push(
                "#L" + slot + "#"
                + "#v" + item.getItemId() + "# "
                + name
                + " (Lv " + item.getItemLevel() + ")"
                + "#l"
            );
        }
    }

    if (!lines.length) {
        cm.sendOk("You have no equippable items to select.");
        return cm.dispose();
    }
    cm.sendSimple("Select the equipment you want to scroll:\r\n" + lines.join("\r\n"));
}

function showApplicableScrolls(equipInvSlotNum) {
    equipInvSlot = equipInvSlotNum;
    equipSelected = cm.getInventory(equipInv).getItem(equipInvSlotNum);
    equipId = equipSelected.getItemId();
    equipName = Packages.server.ItemInformationProvider.getInstance().getName(equipId);

    var inv = cm.getInventory(useInv);
    var lines = [];

    // Retrieve scroll id's valid for the selected armor
    let validScrolls = Packages.server.ItemInformationProvider.getInstance().getScrollsByItemId(equipId);
    for (var scroll = 0; scroll < validScrolls.length; scroll++) {

        for (var useSlot = 1; useSlot <= inv.getSlotLimit(); useSlot++) {
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

function confirmScrollCount(useInvSlotNum) {
    // Populate scroll globals (scrobals)
    scrollInvSlot = useInvSlotNum;
    scrollSelected = cm.getInventory(useInv).getItem(scrollInvSlot);
    scrollId = scrollSelected.getItemId();
    scrollName = Packages.server.ItemInformationProvider.getInstance().getName(scrollId);

    // Get the number of upgrade slots and scroll success percentage
    equipUpgradeSlots = cm.getInventory(equipInv).getUpgradeSlots(equipInvSlot);
    scrollSuccessPercent = cm.getInventory(useInv).getScrollSuccess(scrollInvSlot);

    // Calculate the number of scrolls required
    if (scrollSuccessPercent === 100) {
        requiredScrollCount = equipUpgradeSlots;
        requiredWhiteScrollCount = 0;
    } else {
        console.log("required scroll count = " + equipUpgradeSlots + " / " + "(" + scrollSuccessPercent + " / " + 100 + ")");
        requiredScrollCount = Math.ceil(equipUpgradeSlots / (scrollSuccessPercent / 100));
        requiredWhiteScrollCount = requiredScrollCount;
    }

    var lines = [];

    // Verify the user has the required scrolls
    actualScrollCount = cm.getInventory(useInv).getItem(scrollInvSlot).getQuantity();
    if (actualScrollCount < requiredScrollCount) {
        // Add error message indicating lack of scrolls
        lines.push("Lacking required number of " + scrollName + "\r\n\tYou need " + requiredScrollCount + " and you have " + actualScrollCount);
    }
    if (requiredWhiteScrollCount > 0) {
        whiteScrollInvSlot = cm.getInventory(useInv).getWhiteScrollSlot();
        actualWhiteScrollCount = cm.getInventory(useInv).getInventoryWhiteScrollCount();
        if (actualWhiteScrollCount < requiredWhiteScrollCount) {
            // Add error message indicating lack of white scrolls
            lines.push("Lacking required number of White Scrolls\r\n\tYou need " + requiredWhiteScrollCount + " and you have " + actualWhiteScrollCount);
        }
    }
    {
        whiteScrollInvSlot = -1
        actualWhiteScrollCount = -1;
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

    // Calculate new stats
    //newStats = calcNewStats(equipSelected, requiredScrollCount);

    // Update equip
    //applyNewStats(newStats);

    cm.sendOk("Congratulations! I scrolled your equip (hopefully)!")
    cm.dispose();
}

function calcNewStats(equip, scrollCount) {

    slots = cm.getInventory(equipInv).getUpgradeSlots(equipInvSlot);
    useInventory = cm.getInventory(useInv);

    extraStr = slots * useInventory.getScrollStatBoost(scrollInvSlot, "STR"); // STR
    extraDex = slots * useInventory.getScrollStatBoost(scrollInvSlot, "DEX"); // DEX
    extraInt = slots * useInventory.getScrollStatBoost(scrollInvSlot, "INT"); // INT
    extraLuk = slots * useInventory.getScrollStatBoost(scrollInvSlot, "LUK"); // LUK
    extraHp = slots * useInventory.getScrollStatBoost(scrollInvSlot, "MHP"); // MHP
    extraMp = slots * useInventory.getScrollStatBoost(scrollInvSlot, "MMP"); // MMP
    extraWatk = slots * useInventory.getScrollStatBoost(scrollInvSlot, "PAD"); // PAD
    extraMatk = slots * useInventory.getScrollStatBoost(scrollInvSlot, "MAD"); // MAD
    extraWdef = slots * useInventory.getScrollStatBoost(scrollInvSlot, "PDD"); // PDD
    extraMdef = slots * useInventory.getScrollStatBoost(scrollInvSlot, "MDD"); // MDD
    extraAcc = slots * useInventory.getScrollStatBoost(scrollInvSlot, "ACC"); // ACC
    extraAvoid = slots * useInventory.getScrollStatBoost(scrollInvSlot, "EVA"); // EVA
    extraSpeed = slots * useInventory.getScrollStatBoost(scrollInvSlot, "Speed"); // Speed
    extraJump = slots * useInventory.getScrollStatBoost(scrollInvSlot, "Jump"); // Jump

    return {
        str: equip.getStr() + extraStr,
        dex: equip.getDex() + extraDex,
        int: equip.getInt() + extraInt,
        luk: equip.getLuk() + extraLuk,
        hp: equip.getHp() + extraHp,
        mp: equip.getMp() + extraMp,
        watk: equip.getWatk() + extraWatk,
        matk: equip.getMatk() + extraMatk,
        wdef: equip.getWdef() + extraWdef,
        mdef: equip.getMdef() + extraMdef,
        acc: equip.getAcc() + extraAcc,
        avoid: equip.getAvoid() + extraAvoid,
        speed: equip.getSpeed() + extraSpeed,
        jump: equip.getJump() + extraJump,
    };
}

function applyNewStats(newStats) {
    var s = newStats;
    equipSelected.setStr(s.str);
    equipSelected.setDex(s.dex);
    equipSelected.setInt(s.int);
    equipSelected.setLuk(s.luk);
    equipSelected.setHp(s.hp);
    equipSelected.setMp(s.mp);
    equipSelected.setWatk(s.watk);
    equipSelected.setMatk(s.matk);
    equipSelected.setWdef(s.wdef);
    equipSelected.setMdef(s.mdef);
    equipSelected.setAcc(s.acc);
    equipSelected.setAvoid(s.avoid);
    equipSelected.setSpeed(s.speed);
    equipSelected.setJump(s.jump);
    equipSelected.setUpgradeSlots(0);
    cm.getPlayer().forceUpdateItem(equipSelected);
}

