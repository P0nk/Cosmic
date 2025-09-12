var oldBillionCoin = 4001253;
var newBillionCoin = 3020002;

function start() {
    status = 0;
    cm.sendNext("Hello! I'm Bob! I can change your old billion coin to NEW billion coins.");
}

function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    switch (status) {
        case 1:
            return choice();
        case 2:
            return cm.sendGetText("How many old Billion coins would you like to exchange?", "1");
        case 3:
            return exchange();
    }
}

function choice() {
    var selStr = "\r\n#b#L0#Please exchange my old billion coins#l" +
                 "\r\n#b#L1#Nothing, just trolling you.#l"
        cm.sendSimple(selStr);
}

function exchange() {
    count = parseInt(cm.getText());
    if (isNaN(count) || count <= 0) {
        cm.sendOk("Please enter a valid number.");
        cm.dispose();
        return;
    }
    if (!cm.haveItem(oldBillionCoin, count)) {
        cm.sendOk("You don't have enough old Billion coins.");
        cm.dispose();
        return;
    }
    cm.gainItem(oldBillionCoin, -count);
    cm.gainItem(newBillionCoin, count);
    cm.dispose();
    return;
}