/* NPC Buyback:
 * @author PumpkinPie
*/

// Constants and variables
const TransactionService = Java.type('server.transactions.TransactionService');
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
let selectedTransactionIndex;

// Entry point
function start() {
    actionStatus = -1;
    action(1, 0, 0);
}

// Runtime function
function action(mode, type, selection) {
    if (cm.isEndChat(mode, type)) {
        cm.dispose();
        return;
    }

    if (mode === 1) {
        actionStatus++;
    } else {
        actionStatus--;
    }

    if (actionStatus === 0) {
        handleInitialStatus();
    } else if (actionStatus === 1) {
        handleConfirmTransaction(selection);
    } else if (actionStatus === 2) {
        handleProcessTransaction(selection);
    }
}

// Status actions
function handleInitialStatus() {
    let selectionString = "#bBuyback#n\r\n\r\n";

    let result = TransactionService.getTransactionSelectionListString(cm.getPlayer().getId());

    if (!result.getResult()) {
        cm.sendOk(result.getMessage());
        cm.dispose()
        return;
    }

    selectionString += result.getMessage();
    cm.sendSimple(selectionString);
}

function handleConfirmTransaction(selection) {
    selectedTransactionIndex = selection;

    let result = TransactionService.getTransactionItemListString(cm.getPlayer().getId(), selectedTransactionIndex);

    if (!result.getResult()) {
        cm.sendPrev(result.getMessage());
        return;
    }

    cm.sendNextPrev(result.getMessage());
}

function handleProcessTransaction(selection) {
    let result = TransactionService.processTransaction(cm.getPlayer(), selectedTransactionIndex);

    if (!result.getResult()) {
        cm.sendOk(result.getMessage());
        cm.dispose();
        return;
    }

    cm.sendOk(result.getMessage());
    actionStatus = -1;
}