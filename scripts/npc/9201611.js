/* Subordinate – Free Market (9201611) NPC: Item Upgrade & Rebirth */

var zakDiamond   = 4032133;
var hTegg        = 4001094;
var rockOfTime   = 4021010;
var previewFee   = 2500000;

// For levels 1–4, define upgrade cost and required materials
var upgradeConfig = {
    1: { fee: 15000000, mats: [{ id: zakDiamond, amt: 1 }] },
    2: { fee: 45000000, mats: [{ id: zakDiamond, amt: 3 }] },
    3: { fee: 125000000, mats: [{ id: hTegg,      amt: 1 }] },
    4: { fee: 275000000, mats: [{ id: hTegg,      amt: 3 }] }
};

// Dialogue state
var status       = 0;
var selectedItem = null;
var isRebirth    = false;

function start() {
    status = 0;
    cm.sendSimple("Hey, wanna upgrade?\r\n#b#L0#Yes, upgrade my item!#l");
}

function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    switch (status) {
        case 1:
            return showEquipList();
        case 2:
            return handleSelection(selection);
        case 3:
            return processConfirmation();
        default:
            return cm.dispose();
    }
}

// === STEP 1: List all equip items in your inventory ===
function showEquipList() {
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
        cm.sendOk("You have no equippable items to upgrade.");
        return cm.dispose();
    }

    cm.sendSimple(
        "Select the item you want to upgrade. "
      + "It costs " + format(previewFee) + " mesos to preview each upgrade.\r\n"
      + lines.join("\r\n")
    );
}

// === STEP 2: Player picks an item ===
function handleSelection(slot) {
    selectedItem = cm.getInventory(1).getItem(slot);
    if (!selectedItem) {
        cm.sendOk("Invalid selection.");
        return cm.dispose();
    }

    var lvl   = selectedItem.getItemLevel();
    var hands = selectedItem.getHands();

    // Rebirth condition: level ≥5 but low-handed weapons only
    if (lvl >= 5 && hands <= 2) {
        isRebirth = true;
        return cm.sendYesNo(
            "Your item has reached its max upgrades. I can reset it with a base stat boost.\r\n"
          + "Cost: 1×#v" + rockOfTime + "# + 100k NX. Proceed?"
        );
    }

    // Regular upgrade: level 1–4, hands ≤3
    if (lvl >= 1 && lvl <= 4 && hands <= 3) {
        var cfg = upgradeConfig[lvl];
        if (!cfg) {
            cm.sendOk("No upgrade path configured for level " + lvl + ".");
            return cm.dispose();
        }

        if (cm.getMeso() < previewFee + cfg.fee) {
            cm.sendOk("You need at least "
                + format(previewFee + cfg.fee)
                + " mesos to preview and perform this upgrade.");
            return cm.dispose();
        }

        // Deduct preview fee
        cm.gainMeso(-previewFee);

        // Calculate tentative new stats
        var newStats = calcNewStats(selectedItem);
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
            "Cost: " + format(cfg.fee) + " mesos + " + amt + "x#v" + mat + "#"
        ].join("\r\n");

        return cm.sendYesNo(msg + warning);
    }

    // Otherwise, nothing to do
    cm.sendOk("Your item cannot be upgraded further or is ineligible.");
    cm.dispose();
}

// === STEP 3: Player confirms upgrade or rebirth ===
function processConfirmation() {
    if (isRebirth) return doRebirth();
    return doUpgrade();
}

function doUpgrade() {
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
        applyNewStats();
        cm.sendOk("By the blessing from Carbo, your item has been upgraded successfully!");
    } else if (boom) {
        cm.removeItemNPC(selectedItem.getPosition());
        cm.sendOk("Oh no! Your item was destroyed in the attempt.");
    } else {
        cm.sendOk("Upgrade failed. Better luck next time.");
    }

    return cm.dispose();
}

function doRebirth() {
    // Check resources
    if (!cm.haveItem(rockOfTime, 1)) {
        cm.sendOk("You need 1x#v" + rockOfTime + "# to rebirth.");
    } else if (cm.getCashShop().getCash(1) < 100000) {
        cm.sendOk("You need 100k NX to rebirth.");
    } else {
        cm.rebirthItem(selectedItem.getPosition(), selectedItem.getHands());
        cm.gainItem(rockOfTime, -1);
        cm.gainCash(-100000);
        cm.sendOk("Your item has been reborn. Go get stronger!");
    }
    return cm.dispose();
}

// === Helpers ===
function calcNewStats(item) {
    // Main stats 40–60% increase, defs 10–20%
    var mm = () => 1.4 + Math.random() * 0.2;
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

function applyNewStats() {
    var s = calcNewStats(selectedItem);
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
