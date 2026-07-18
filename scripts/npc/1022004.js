/*
 * ForgeMS MSI Exchange
 * Temporary NPC Base: Mr. Smith
 * NPC ID: 1022004
 *
 * Phase 1:
 * - Non-NX equipment only.
 * - Requires 32,767 STR, DEX, INT and LUK.
 * - Transfers those stats into one selected equipment item.
 * - Resets the player's base stats to 4.
 *
 * Future:
 * - Track completion of required non-NX base equipment slots.
 * - Unlock NX MSI exchanges only after the base set is complete.
 */

var status = -1;
var mainSelection = -1;
var selectedInventorySlot = 0;

var MAX_STAT = 32767;
var RESET_STAT = 4;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
        return;
    }

    if (mode === 0) {
        if (status <= 0) {
            cm.dispose();
            return;
        }

        cm.sendOk(
            "The forge will remain ready when you return."
        );
        cm.dispose();
        return;
    }

    status++;

    if (status === 0) {
        showMainMenu();
        return;
    }

    if (status === 1) {
        mainSelection = selection;

        if (mainSelection === 0) {
            beginExchange();
            return;
        }

        if (mainSelection === 1) {
            showRequirements();
            return;
        }

        if (mainSelection === 2) {
            showExchangeRules();
            return;
        }

        cm.dispose();
        return;
    }

    if (status === 2 && mainSelection === 0) {
        selectedInventorySlot = selection;
        confirmSelectedItem();
        return;
    }

    if (status === 3 && mainSelection === 0) {
        completeExchange();
        return;
    }

    cm.dispose();
}

function showMainMenu() {
    cm.sendSimple(
        "Greetings, adventurer.\r\n\r\n" +
        "I am #bEitri#k, Master Smith of the Coalition.\r\n\r\n" +
        "Those who have forged every attribute to its mortal limit " +
        "may bind that strength into their equipment.\r\n\r\n" +
        "What would you like to do?\r\n\r\n" +
        "#L0##bExchange stats into an item.#k#l\r\n" +
        "#L1##bView MSI requirements.#k#l\r\n" +
        "#L2##bView exchange rules.#k#l"
    );
}

function beginExchange() {
    if (!hasMaximumPlayerStats()) {
        showMissingStats();
        return;
    }

    var menu = buildEligibleItemMenu();

    if (menu === "") {
        cm.sendOk(
            "You do not currently have any eligible equipment in your " +
            "#bEQUIP inventory#k.\r\n\r\n" +
            "The item must:\r\n\r\n" +
            "#b•#k Be a non-NX equipment item.\r\n" +
            "#b•#k Be located in your EQUIP inventory.\r\n" +
            "#b•#k Not already contain 32,767 of every base stat.\r\n\r\n" +
            "Unequip the item you wish to forge and speak with me again."
        );
        cm.dispose();
        return;
    }

    cm.sendSimple(
        "#eSelect an item to forge into an MSI:#n\r\n\r\n" +
        "#rThe selected item will be permanently modified and your " +
        "base STR, DEX, INT and LUK will each reset to " +
        RESET_STAT + ".#k\r\n\r\n" +
        menu
    );
}

function buildEligibleItemMenu() {
    var inventory = getEquipInventory();
    var list = inventory.list();
    var iterator = list.iterator();
    var menu = "";

    while (iterator.hasNext()) {
        var item = iterator.next();

        if (item === null) {
            continue;
        }

        var position = Number(item.getPosition());

        /*
         * Positive positions are items in the EQUIP inventory.
         * Equipped items normally use negative positions.
         */
        if (position <= 0) {
            continue;
        }

        if (!isEligibleEquipment(item)) {
            continue;
        }

        var itemId = Number(item.getItemId());

        menu +=
            "#L" + position + "#" +
            "#i" + itemId + "# " +
            "#b#t" + itemId + "##k" +
            "#l\r\n";
    }

    return menu;
}

function confirmSelectedItem() {
    var item = getSelectedItem();

    if (item === null) {
        cm.sendOk(
            "I could not locate that item. It may have been moved."
        );
        cm.dispose();
        return;
    }

    if (!isEligibleEquipment(item)) {
        cm.sendOk(
            "That item is no longer eligible for the MSI exchange."
        );
        cm.dispose();
        return;
    }

    var itemId = Number(item.getItemId());

    cm.sendYesNo(
        "You have selected:\r\n\r\n" +
        "#i" + itemId + "# #b#t" + itemId + "##k\r\n\r\n" +
        "The item will receive:\r\n\r\n" +
        "#b+" + formatNumber(MAX_STAT) + " STR\r\n" +
        "+" + formatNumber(MAX_STAT) + " DEX\r\n" +
        "+" + formatNumber(MAX_STAT) + " INT\r\n" +
        "+" + formatNumber(MAX_STAT) + " LUK#k\r\n\r\n" +
        "Your character’s base STR, DEX, INT and LUK will each be " +
        "reset to #r" + RESET_STAT + "#k.\r\n\r\n" +
        "#rThis exchange cannot be undone.#k\r\n\r\n" +
        "Do you wish to continue?"
    );
}

function completeExchange() {
    /*
     * Recheck everything before modifying the character or item.
     */
    if (!hasMaximumPlayerStats()) {
        cm.sendOk(
            "You no longer meet the MSI stat requirements."
        );
        cm.dispose();
        return;
    }

    var item = getSelectedItem();

    if (item === null) {
        cm.sendOk(
            "The selected item could not be found. The exchange was cancelled."
        );
        cm.dispose();
        return;
    }

    if (!isEligibleEquipment(item)) {
        cm.sendOk(
            "The selected item is no longer eligible. The exchange was cancelled."
        );
        cm.dispose();
        return;
    }

    /*
     * The inventory item must be an Equip object for stat setters.
     */
    var equip = item;

    equip.setStr(MAX_STAT);
    equip.setDex(MAX_STAT);
    equip.setInt(MAX_STAT);
    equip.setLuk(MAX_STAT);

    resetPlayerStats();

    /*
     * Notify the client that the inventory item was modified.
     */
    forceInventoryUpdate(equip);

    var itemId = Number(equip.getItemId());

    cm.sendOk(
        "The forging is complete.\r\n\r\n" +
        "#i" + itemId + "# #b#t" + itemId + "##k\r\n\r\n" +
        "This item now contains:\r\n\r\n" +
        "#b+" + formatNumber(MAX_STAT) + " STR\r\n" +
        "+" + formatNumber(MAX_STAT) + " DEX\r\n" +
        "+" + formatNumber(MAX_STAT) + " INT\r\n" +
        "+" + formatNumber(MAX_STAT) + " LUK#k\r\n\r\n" +
        "Your strength has been returned to the beginning. " +
        "Forge it again."
    );

    cm.dispose();
}

function isEligibleEquipment(item) {
    if (item === null) {
        return false;
    }

    var itemId = Number(item.getItemId());

    /*
     * Block all NX equipment during Phase 1.
     */
    if (isNxItem(itemId)) {
        return false;
    }

    /*
     * Prevent repeatedly exchanging an already completed MSI.
     */
    if (isAlreadyMsi(item)) {
        return false;
    }

    return true;
}

function isAlreadyMsi(item) {
    return Number(item.getStr()) >= MAX_STAT &&
        Number(item.getDex()) >= MAX_STAT &&
        Number(item.getInt()) >= MAX_STAT &&
        Number(item.getLuk()) >= MAX_STAT;
}

function hasMaximumPlayerStats() {
    var player = cm.getPlayer();

    return Number(player.getStr()) >= MAX_STAT &&
        Number(player.getDex()) >= MAX_STAT &&
        Number(player.getInt()) >= MAX_STAT &&
        Number(player.getLuk()) >= MAX_STAT;
}

function showMissingStats() {
    var player = cm.getPlayer();

    var str = Number(player.getStr());
    var dex = Number(player.getDex());
    var intStat = Number(player.getInt());
    var luk = Number(player.getLuk());

    cm.sendOk(
        "#eMSI Exchange Requirements#n\r\n\r\n" +
        "You must reach the maximum value in all four base stats.\r\n\r\n" +
        buildStatLine("STR", str) +
        buildStatLine("DEX", dex) +
        buildStatLine("INT", intStat) +
        buildStatLine("LUK", luk) +
        "\r\nContinue forging your character and return when every " +
        "attribute has reached #r" + formatNumber(MAX_STAT) + "#k."
    );

    cm.dispose();
}

function buildStatLine(name, currentValue) {
    var met = currentValue >= MAX_STAT;
    var marker = met ? "#bComplete#k" : "#rIncomplete#k";

    return (
        "#b•#k " + name + ": " +
        formatNumber(currentValue) + " / " +
        formatNumber(MAX_STAT) + " — " +
        marker + "\r\n"
    );
}

function showRequirements() {
    cm.sendOk(
        "#eForgeMS MSI Requirements#n\r\n\r\n" +
        "Your character must have:\r\n\r\n" +
        "#b•#k " + formatNumber(MAX_STAT) + " STR\r\n" +
        "#b•#k " + formatNumber(MAX_STAT) + " DEX\r\n" +
        "#b•#k " + formatNumber(MAX_STAT) + " INT\r\n" +
        "#b•#k " + formatNumber(MAX_STAT) + " LUK\r\n\r\n" +
        "A successful exchange places those four stats onto the " +
        "selected equipment item and resets your character’s four " +
        "base stats to " + RESET_STAT + "."
    );

    cm.dispose();
}

function showExchangeRules() {
    cm.sendOk(
        "#eForgeMS MSI Exchange Rules#n\r\n\r\n" +
        "#b•#k All four base stats must be " +
        formatNumber(MAX_STAT) + ".\r\n" +
        "#b•#k The selected item must be inside the EQUIP inventory.\r\n" +
        "#b•#k Existing attack, defense, upgrades and other item stats remain.\r\n" +
        "#b•#k The item receives " + formatNumber(MAX_STAT) +
        " STR, DEX, INT and LUK.\r\n" +
        "#b•#k Your character’s four base stats reset after the exchange.\r\n" +
        "#b•#k An item that is already MSI cannot be exchanged again.\r\n" +
        "#b•#k NX equipment is currently locked.\r\n" +
        "#b•#k Players must complete the required non-NX base MSI set " +
        "before NX MSI equipment becomes available."
    );

    cm.dispose();
}

/*
 * Inventory access
 *
 * Cosmic builds may call this enum InventoryType.EQUIP.
 * If your source uses MapleInventoryType instead, this import/function
 * will need the corresponding enum name changed.
 */
function getEquipInventory() {
    var InventoryType = Java.type("client.inventory.InventoryType");
    return cm.getPlayer().getInventory(InventoryType.EQUIP);
}

function getSelectedItem() {
    var inventory = getEquipInventory();
    return inventory.getItem(selectedInventorySlot);
}

/*
 * Checks the WZ cash/NX flag through ItemInformationProvider.
 *
 * Depending on your Cosmic revision, the provider class or method may be:
 *
 * ItemInformationProvider.getInstance().isCash(itemId)
 *
 * or:
 *
 * MapleItemInformationProvider.getInstance().isCash(itemId)
 */
function isNxItem(itemId) {
    var ItemInformationProvider =
        Java.type("server.ItemInformationProvider");

    return ItemInformationProvider
        .getInstance()
        .isCash(itemId);
}

function resetPlayerStats() {
    var player = cm.getPlayer();

    player.setStr(RESET_STAT);
    player.setDex(RESET_STAT);
    player.setInt(RESET_STAT);
    player.setLuk(RESET_STAT);

    /*
     * Recalculates and sends updated character stats.
     */
    player.updateSingleStat(
        Java.type("client.Stat").STR,
        RESET_STAT
    );

    player.updateSingleStat(
        Java.type("client.Stat").DEX,
        RESET_STAT
    );

    player.updateSingleStat(
        Java.type("client.Stat").INT,
        RESET_STAT
    );

    player.updateSingleStat(
        Java.type("client.Stat").LUK,
        RESET_STAT
    );
}

/*
 * Sends the modified equipment back to the client.
 *
 * The exact inventory update helper can vary between Cosmic revisions.
 */
function forceInventoryUpdate(equip) {
    var InventoryManipulator =
        Java.type("server.InventoryManipulator");

    InventoryManipulator.updateItem(
        cm.getClient(),
        getEquipInventory().getType(),
        equip
    );

    cm.getPlayer().equipChanged();
}

function formatNumber(number) {
    return String(number).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}