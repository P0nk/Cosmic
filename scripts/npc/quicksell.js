/* NPC: Donation Box (9000041)
	Victoria Road : Henesys

	NPC Bazaar:
        * @author Ronan Lana
	* Then by PumpkinPie
*/

// Constants and variables
const InventoryType = Java.type('client.inventory.InventoryType');
const YamlConfig = Java.type('config.YamlConfig');
const InventoryTypeOptions = {
    StringMap: {
        1: "Equip",
        2: "Use",
        3: "Setup",
        4: "Etc"
    },
    Equip: 1,
    Use: 2,
    Setup: 3,
    Etc: 4
}
const options = [InventoryTypeOptions.Equip, InventoryTypeOptions.Use, InventoryTypeOptions.Setup, InventoryTypeOptions.Etc];

let actionStatus;
let inventoryType;
let inventory;
let firstItemSlot = 1, lastItemSlot = 96;

// Entry point
function start() {
    actionStatus = -1;
    action(1, 0, 0);
}

// Runtime function
function action(mode, type, selection) {
    actionStatus++;

    if (mode !== 1) {
        cm.dispose();
        return;
    }

    if (actionStatus === 0) {
        handleInitialStatus();
    } else if (actionStatus === 1) {
        handleInventoryStartSelectionStatus(selection);
    } else if (actionStatus === 2) {
        handleInventoryEndSelectionStatus(selection);
    } else if (actionStatus === 3) {
        handleTransactionStatus(selection);
    }
}

// Status actions
function handleInitialStatus() {
    let selectionOptionsString = "Hello, I am the #bDonation Box#k! I can clear your inventory of junk or glitched items by selling them all. ";
    for (let inventoryOption of options) {
        selectionOptionsString += `\r\n#L${inventoryOption}# ${InventoryTypeOptions.StringMap[inventoryOption]}#l`;
    }
    cm.sendSimple(selectionOptionsString);
}

function handleInventoryStartSelectionStatus(selection) {
    inventoryType = InventoryType.getByType(selection);
    inventory = cm.getPlayer().getInventory(inventoryType);
    let selectionString = createInventoryItemsString(inventory, 0, lastItemSlot, "Which slot in your inventory do you want to start selling from?\r\n");
    cm.sendSimple(selectionString);
}

function handleInventoryEndSelectionStatus(selection) {
    firstItemSlot = selection;
    let selectionString = createInventoryItemsString(inventory, firstItemSlot + 1, lastItemSlot, "Which slot in your inventory do you want to stop selling at?\r\n");
    cm.sendSimple(selectionString);
}

function handleTransactionStatus(selection) {
    lastItemSlot = selection;
    const transactionResult = cm.getPlayer().sellAllPosLast(inventoryType, firstItemSlot, lastItemSlot);
    const transactionMessage = transactionResult > -1
        ? `Transaction complete! You received #r${cm.numberWithCommas(transactionResult)} mesos#k from this action.`
        : "There was an error in your message, please try again.";
    cm.sendOk(transactionMessage);
    cm.dispose();
}

// Utilities
function createInventoryItemsString(inventory, startSlot, endSlot, initialMessage) {
    let selectionString = initialMessage;
    let formatCounter = 0;
    for (let itemSlot = startSlot; itemSlot <= endSlot; itemSlot++) {
        let result = appendItemString(inventory, itemSlot, selectionString, formatCounter);
        selectionString = result.selectionString;
        formatCounter = result.formatCounter;
    }
    return selectionString;
}

function appendItemString(inventory, itemSlot, selectionString, formatCounter) {
    if (inventory.getItem(itemSlot) !== null) {
        selectionString += `#L${itemSlot}##i${inventory.getItem(itemSlot).getItemId()}#   `;
        formatCounter++;
        if (formatCounter % 4 === 0) {
            selectionString += "\r\n";
        }
    }
    return { selectionString, formatCounter };
}