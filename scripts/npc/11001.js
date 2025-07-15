// Otto the Scroller – Equipment Enhancer NPC (NPC ID: 11001)

const invTypeEquip = 1;
const invTypeUse = 2;
const invTypeCash = 5;
const viciousHammerId = 5570000;
const viciousHammerCost = 10000;
const whiteScrollId = 2340000;

var status = 0;
var hammering = false;
var scrolling = false;

// Scroll-related globals
var equipInvSlot = null;
var equipSelected = null;
var equipName = null;
var scrollInvSlot = null;
var scrollSelected = null;
var scrollId = null;
var requiredWhiteScrollCount = null;

// Hammer-related globals
var requiredHammerCount = 0;
var actualHammerCount = 0;

function start() {
    status = 0;
    cm.sendNext("Hello! I'm Otto the Scroller.");
}

function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    try {
        switch (status) {
            case 1:
                cm.sendSimple("What would you like to do?\r\n" +
                     "#b#L0#Auto-scroll an equip using scrolls#l\r\n" +
                    "#b#L1#Hammer all your equips (using Vicious Hammers)#l\r\n" +
                    "#L2#Purchase Vicious Hammers (10,000 NX each)#l");
                break;
            case 2:
                switch (selection) {
                    case 0: return showScrollableEquips();
                    case 1: return showHammerableEquips();
                    case 2:
                        cm.sendGetText("How many Vicious Hammers would you like to buy?", "1");
                        status = 10;
                        break;
                }
                break;
            case 3:
                if (hammering) return confirmHammerCount();
                return showApplicableScrolls(selection);
            case 4:
                if (hammering) return hammerTime();
                return confirmScrollStart(selection);
            case 5:
                if (hammering) return cm.dispose();
                return scrollItemOrStop(mode);
            case 11:
                return handleHammerPurchase();
        }
    } catch (err) {
        cm.sendOk("Something broke: " + err);
        return cm.dispose();
    }
}

// === Hammer Flow ===

function showHammerableEquips() {
    hammering = true;
    var inv = cm.getInventory(invTypeEquip);
    var lines = [];
    requiredHammerCount = 0;

    for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {

        var item = inv.getItem(slot);
        if (!item) continue;
        var name = Packages.server.ItemInformationProvider.getInstance().getName(item.getItemId());
        if (!item || !(item instanceof Packages.client.inventory.Equip)) continue;
        if (inv.getEquipStat(slot, "cash") === 1) continue;
        if (item.getVicious() >= 2) continue;

        var needed = 2 - item.getVicious();
        requiredHammerCount += needed;
        lines.push(`#v${item.getItemId()}# ${name} - Needs ${needed} hammer(s)`);
    }

    if (!lines.length) {
        cm.sendOk("You have no items that can be hammered.");
        return cm.dispose();
    }

    cm.sendYesNo("Would you like to hammer these items?\r\n" + lines.join("\r\n"));
}

function confirmHammerCount() {
    var inv = cm.getInventory(invTypeCash);
    actualHammerCount = 0;

    for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {
        var item = inv.getItem(slot);
        if (item && item.getItemId() === viciousHammerId) {
            actualHammerCount += item.getQuantity();
        }
    }

    if (requiredHammerCount > actualHammerCount) {
        cm.sendOk("You need " + requiredHammerCount + " Vicious Hammers but only have " + actualHammerCount + ".");
        return cm.dispose();
    }

    cm.sendYesNo("You have enough Vicious Hammers. Proceed?");
}

function hammerTime() {
    var inv = cm.getInventory(invTypeEquip);

    for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {
        var item = inv.getItem(slot);
        if (!item) continue;
        if (!item || !(item instanceof Packages.client.inventory.Equip)) continue;
        if (inv.getEquipStat(slot, "cash") === 1) continue;

        var needed = 2 - item.getVicious();
        for (var i = 0; i < needed; i++) {
            item.setVicious(item.getVicious() + 1);
            item.setUpgradeSlots(item.getUpgradeSlots() + 1);
        }
        cm.getPlayer().forceUpdateItem(item);
    }

    cm.removeAmount(viciousHammerId, requiredHammerCount);
    cm.sendOk("All eligible equips have been hammered.");
    cm.dispose();
}

// === Purchase Flow ===

function handleHammerPurchase() {
    var count = parseInt(cm.getText());
    if (isNaN(count) || count <= 0) {
        cm.sendOk("Please enter a valid number.");
        return cm.dispose();
    }

    var totalCost = viciousHammerCost * count;
    if (cm.getCashShop().getCash(1) < totalCost) {
        cm.sendOk("You do not have enough NX. Required: " + totalCost);
        return cm.dispose();
    }

    cm.gainItem(viciousHammerId, count);
    cm.gainCash(-totalCost);
    cm.sendCashNoti("You purchased " + count + " Vicious Hammer(s) for " + totalCost + " NX.");
    cm.dispose();
}

// === Scroll Flow ===

function showScrollableEquips() {
    scrolling = true;
    var inv = cm.getInventory(invTypeEquip);
    var lines = [];

    for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {
        var item = inv.getItem(slot);
        if (!item) continue;
        var name = Packages.server.ItemInformationProvider.getInstance().getName(item.getItemId());
        if (inv.getUpgradeSlots(slot) > 0) {
            lines.push(`#L${slot}##v${item.getItemId()}# ${name}#l`);
        }
    }

    if (!lines.length) {
        cm.sendOk("No scrollable equips found.");
        return cm.dispose();
    }

    cm.sendSimple("Select the equip to scroll:\r\n" + lines.join("\r\n"));
}

function showApplicableScrolls(equipSlot) {
    equipInvSlot = equipSlot;
    equipSelected = cm.getInventory(invTypeEquip).getItem(equipSlot);
    equipName = Packages.server.ItemInformationProvider.getInstance().getName(equipSelected.getItemId());

    var inv = cm.getInventory(invTypeUse);
    var lines = [];
    var validScrolls = Packages.server.ItemInformationProvider.getInstance().getScrollsByItemId(equipSelected.getItemId());

    for (var scroll of validScrolls) {
        for (var slot = 1; slot <= inv.getSlotLimit(); slot++) {
            var item = inv.getItem(slot);
                    if (!item) continue;
                    var name = Packages.server.ItemInformationProvider.getInstance().getName(item.getItemId());
            if (item && item.getItemId() === scroll) {
                lines.push(`#L${slot}##v${scroll}# ${name}#l`);
            }
        }
    }

    if (!lines.length) {
        cm.sendOk("No applicable scrolls found.");
        return cm.dispose();
    }

    cm.sendSimple("Select the scroll to use:\r\n" + lines.join("\r\n"));
}

function confirmScrollStart(useSlot) {
    scrollInvSlot = useSlot;
    scrollSelected = cm.getInventory(invTypeUse).getItem(scrollInvSlot);
    scrollId = scrollSelected.getItemId();

    requiredWhiteScrollCount = scrollSelected.getEquipStat ? scrollSelected.getEquipStat("success") < 100 : 0;

    cm.sendYesNo(`Proceed to scroll your #b${equipName}#k?\r\n` +
        `This will use one scroll per attempt until:\r\n` +
        `- Upgrade slots run out, or\r\n` +
        `- Scrolls/white scrolls are depleted.`);
}

function scrollItemOrStop(mode) {
    if (mode === 0) {
        cm.sendOk("Come back when you're ready.");
        return cm.dispose();
    }

    const statFields = ["STR", "DEX", "INT", "LUK", "WATK", "MATK", "WDEF", "MDEF", "ACC", "AVOID", "SPEED", "JUMP"];
    const getStats = (equip) => ({
        STR: equip.getStr(), DEX: equip.getDex(), INT: equip.getInt(), LUK: equip.getLuk(),
        WATK: equip.getWatk(), MATK: equip.getMatk(), WDEF: equip.getWdef(), MDEF: equip.getMdef(),
        ACC: equip.getAcc(), AVOID: equip.getAvoid(), SPEED: equip.getSpeed(), JUMP: equip.getJump()
    });

    const instance = Packages.server.ItemInformationProvider.getInstance();
    const player = cm.getPlayer();

    let scrollsUsed = 0;
    let whiteScrollsUsed = 0;
    let success = 0;
    let fail = 0;
    let cursed = 0;

    let equip = equipSelected;
    let scrollQty = scrollSelected.getQuantity();
    let whiteQty = cm.getItemQuantity(whiteScrollId);

    const originalStats = getStats(equip);

    while (equip.getUpgradeSlots() > 0 && scrollQty > 0 && whiteQty > 0) {
        scrollsUsed++;
        scrollQty--;
        cm.gainItem(scrollId, -1);

        let usingWhiteScroll = false;
        if (whiteQty > 0) {
            usingWhiteScroll = true;
            whiteQty--;
            whiteScrollsUsed++;
            cm.gainItem(whiteScrollId, -1);
        }

        const preLevel = equip.getLevel();
        const preSlots = equip.getUpgradeSlots();
        const preStats = getStats(equip);

        const result = instance.scrollEquipWithId(equip, scrollId, usingWhiteScroll, 0, false);

        if (result === null) {
            cursed++;
            cm.sendOk("Unfortunately, your item was destroyed by a cursed scroll.");
            return cm.dispose();
        }

        equip = result;
        player.forceUpdateItem(equip);

        const postLevel = equip.getLevel();
        const postSlots = equip.getUpgradeSlots();

        if (postLevel > preLevel) {
            success++;
        } else if (postSlots < preSlots && !usingWhiteScroll) {
            fail++;
        } else {
            // Rare case: slot preserved due to white scroll, no stat gain
            fail++;
        }
    }

    const finalStats = getStats(equip);
    const statChanges = [];

    for (const field of statFields) {
        const delta = finalStats[field] - originalStats[field];
        if (delta !== 0) {
            statChanges.push((delta > 0 ? "+" : "") + delta + " " + field);
        }
    }

    const summary = [
        `#bScroll Summary:#k`,
        `Total Scrolls Used: ${scrollsUsed}`,
        `White Scrolls Used: ${whiteScrollsUsed}`,
        `Successes: ${success} | Failures: ${fail} | Cursed: ${cursed}`,
        ``,
        `#bStat Changes:#k`,
        statChanges.length ? statChanges.join(", ") : "No stat change detected."
    ];

    cm.sendOk(summary.join("\r\n"));
    cm.dispose();
}
