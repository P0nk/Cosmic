var status = 0;
var NX_MCOIN_ID = 3020001;
var MESO_BCOIN_ID = 3020002;
var manualNumber = "";
var betType = "";
var currentDrawDate;
var betAmount = 1;
var isQuickPick = false;

var FourDBetManager = Java.type("server.gambling.FourDBetManager");
var FourDResultManager = Java.type("server.gambling.FourDResultManager");
var FourDDrawScheduler = Java.type("server.gambling.FourDDrawScheduler");

function start() {
    status = 0;
    currentDrawDate = FourDDrawScheduler.getNextDrawDate();
    cm.sendSimple("Welcome to the 4D Gambling Booth\r\n" +
        "#L0#Buy 4D Ticket (Manual)#l\r\n" +
        "#L3#Buy 4D Ticket (Quick Pick)#l\r\n" +
        "#L1#Check Today's Results#l\r\n" +
        "#L2#Claim Prize#l");
}

function action(mode, type, selection) {
    java.lang.System.out.println("[4D NPC] status = " + status + ", selection = " + selection);

    if (mode !== 1) {
        cm.dispose();
        return;
    }
    status++;

    if (status === 1) {
        switch (selection) {
            case 0:
                isQuickPick = false;
                cm.sendGetText("Enter your 4-digit number (0000 to 9999):");
                break;
            case 1:
                checkResults();
                return;
            case 2:
                claimPrize();
                return;
            case 3:
                isQuickPick = true;
                cm.sendGetNumber("How many Quick Picks would you like to bet? (Consumes that many NXT Coins)", 1, 1, 100);
                break;
        }
    } else if (status === 2) {
        if (isQuickPick) {
            betAmount = selection;
            if (!cm.haveItem(NX_MCOIN_ID, betAmount)) {
                cm.sendOk("You do not have enough NXT Coins.");
                cm.dispose();
                return;
            }
            cm.sendSimple("Choose your bet type for the Quick Pick numbers:\r\n#L0#Big Bet#l\r\n#L1#Small Bet#l");
        } else {
            manualNumber = cm.getText();
            if (!/^\d{4}$/.test(manualNumber)) {
                cm.sendOk("Invalid number. Must be 4 digits.");
                cm.dispose();
                return;
            }
            cm.sendSimple("Choose your bet type for number " + manualNumber + ":\r\n#L0#Big Bet (more coverage, lower payout)#l\r\n#L1#Small Bet (less coverage, higher payout)#l");
        }
    } else if (status === 3) {
        betType = (selection === 0) ? "BIG" : "SMALL";
        if (!isQuickPick) {
            cm.sendGetNumber("How many NXT Coins would you like to bet? (You must have enough in inventory)", 1, 1, 100);
        } else {
            if (!cm.haveItem(NX_MCOIN_ID, betAmount)) {
                cm.sendOk("You do not have enough NXT Coins.");
                cm.dispose();
                return;
            }
            cm.gainItem(NX_MCOIN_ID, -betAmount);
            var quickNumbers = [];
            for (var i = 0; i < betAmount; i++) {
                var num = generateRandomNumber();
                quickNumbers.push(num);
                FourDBetManager.insertBet(cm.getPlayer().getId(), num, betType, currentDrawDate.toString(), "1");
            }
            cm.sendOk("You placed " + betAmount + " Quick Pick " + betType + " bet(s):\r\n" + quickNumbers.join(", ") + "\r\nfor the draw on " + currentDrawDate + ".");
            cm.dispose();
        }
    } else if (status === 4 && !isQuickPick) {
        betAmount = selection;
        if (!cm.haveItem(NX_MCOIN_ID, betAmount)) {
            cm.sendOk("You do not have enough NXT Coins.");
            cm.dispose();
            return;
        }
        cm.gainItem(NX_MCOIN_ID, -betAmount);
            FourDBetManager.insertBet(cm.getPlayer().getId(), manualNumber, betType, currentDrawDate.toString(),betAmount.toString());

        cm.sendOk("You have placed " + betAmount + " " + betType + " bet(s) on number " + manualNumber + " for the draw on " + currentDrawDate + ".");
        cm.dispose();
    }
}

function generateRandomNumber() {
    var digits = [];
    for (var i = 0; i < 4; i++) {
        digits.push(Math.floor(Math.random() * 10));
    }
    return digits.join("");
}

function checkResults() {
    try {
        var result = FourDResultManager.getResultByDate(currentDrawDate);
        if (result !== null) {
            var output = "4D Results for " + result.date + ":\r\n";
            output += "1st Prize: " + result.first + "\r\n";
            output += "2nd Prize: " + result.second + "\r\n";
            output += "3rd Prize: " + result.third + "\r\n";
            output += "Starter Prizes: " + result.starters + "\r\n";
            output += "Consolation Prizes: " + result.consolations;
            cm.sendOk(output);
        } else {
            cm.sendOk("No results available yet.");
        }
    } catch (e) {
        cm.sendOk("Error retrieving results.");
    }
    cm.dispose();
}

function claimPrize() {
    try {
        var unclaimed = FourDBetManager.getUnclaimedWinningBets(cm.getPlayer().getId());
        var total = 0;
        for (var i = 0; i < unclaimed.size(); i++) {
            var row = unclaimed.get(i);
            var quantity = row.get("prize_quantity");
            if (quantity > 0) {
                total += quantity;
                FourDBetManager.markBetClaimed(row.get("bet_id"));
            }
        }
        if (total > 0) {
            cm.gainItem(MESO_BCOIN_ID, total);
            cm.sendOk("You have claimed " + total + " Meso BCoins. Congratulations!");
        } else {
            cm.sendOk("You have no unclaimed prizes.");
        }
    } catch (e) {
        cm.sendOk("Error during prize claim.");
    }
    cm.dispose();
}
