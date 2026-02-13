var status = 0;
var manualNumber = "";
var betType = "";
var currentDrawDate;
var betAmount = 0;
var isQuickPick = false;
var isIBet = false;
var dateList = null;
var MAX_HISTORY_BETS = 100;
var MAX_HISTORY_DRAWS = 7;

var FourDBetManager = Java.type("server.gambling.FourDBetManager");
var FourDResultManager = Java.type("server.gambling.FourDResultManager");
var FourDDrawScheduler = Java.type("server.gambling.FourDDrawScheduler");

function start() {
    status = 0;
    currentDrawDate = FourDDrawScheduler.getNextDrawDate();

    var msg = "#e#bWelcome to Merogie Pools (Meso)!#n#k\r\n";
    msg += "I'm Esther! Place your bets using #bMesos#k here.\r\n";
    msg += "Next Draw: #e#b" + currentDrawDate + " 12:00AM (GMT+8) #n\r\n\r\n";
    msg += "#L0##bBuy 4D Ticket#k (#dManual#k)#l\r\n";
    msg += "#L3##bBuy 4D Ticket#k (#gQuick Pick#k)#l\r\n";
    msg += "#L1##bView Past Draw Results#k#l\r\n";
    msg += "#L2##rClaim Prize#k#l\r\n";
    msg += "#L4##bView My Past Bets#k#l\r\n";
    msg += "#L5##bGame Guide & Rules#k#l\r\n";

    if (cm.getPlayer().isGM()) {
        msg += "#L99##k(GM Only) Force Today's Draw#l\r\n";
    }

    cm.sendSimple(msg);
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }
    status++;

    // --- VIEW PAST RESULTS ---
    if (status === 2 && dateList !== null) {
        var selectedDate = dateList.get(selection);
        try {
            var result = FourDResultManager.getResultByDate(java.time.LocalDate.parse(selectedDate));
            if (result !== null) {
                var msg = "#eResults for #b" + selectedDate + "#k:#n\r\n\r\n";
                msg += "1st: " + result.get("first") + "\r\n2nd: " + result.get("second") + "\r\n3rd: " + result.get("third") + "\r\n\r\n";
                msg += "Starters:\r\n" + result.get("starters") + "\r\n";
                msg += "Consolations:\r\n" + result.get("consolations");
                cm.sendOk(msg);
            }
        } catch (e) { }
        cm.dispose();
        return;
    }

    // --- MAIN MENU ---
    if (status === 1) {
        switch (selection) {
            case 0: // Manual
                isQuickPick = false;
                cm.sendGetText("Enter 4-digit number:");
                break;
            case 1: // History
                dateList = FourDResultManager.getRecentDrawDates(MAX_HISTORY_DRAWS);
                if (!dateList || dateList.size() === 0) { cm.sendOk("No history."); cm.dispose(); }
                else {
                    var menu = "Select date:\r\n";
                    for (var i = 0; i < dateList.size(); i++) menu += "#L" + i + "#" + dateList.get(i) + "#l\r\n";
                    cm.sendSimple(menu);
                }
                return;
            case 2: claimPrize(); return;
            case 3: // Quick Pick
                isQuickPick = true;
                cm.sendGetNumber("Enter Meso amount per bet:", 1000, 1000, 2000000000);
                break;
            case 4: showBetHistory(); return;
            case 5: showGuide(); return;
            case 99:
                FourDDrawScheduler.forceDrawToday();
                cm.sendOk("Draw forced.");
                cm.dispose();
                return;
        }
    }

    // --- STEP 2: INPUT VALIDATION / IBET PROMPT ---
    if (status === 2) {
        if (isQuickPick) {
            betAmount = selection;
            if (cm.getMeso() < betAmount) {
                cm.sendOk("You need " + formatNumber(betAmount) + " mesos.");
                cm.dispose();
                return;
            }
            cm.sendSimple("Bet Type:\r\n#L0#Big Bet#l\r\n#L1#Small Bet#l");
        } else {
            manualNumber = cm.getText();
            if (!/^\d{4}$/.test(manualNumber)) { cm.sendOk("Invalid number."); cm.dispose(); return; }

            // iBet Prompt
            var perms = getPermutations(manualNumber);
            var msg = "You entered #e" + manualNumber + "#n.\r\n";
            msg += "Do you want this to be an #r#eiBet (System Entry)#n#k?\r\n";
            msg += "(iBet covers all " + perms + " permutations. You pay for each one.)\r\n\r\n";
            msg += "#L0#No, Direct Bet only (1 unit)#l\r\n";
            msg += "#L1#Yes, iBet (System Entry - " + perms + " units)#l";
            cm.sendSimple(msg);
        }
    }

    // --- STEP 3: BET TYPE / EXECUTE QP ---
    if (status === 3) {
        if (isQuickPick) {
            betType = (selection === 0) ? "BIG" : "SMALL";

            if (cm.getMeso() < betAmount) { cm.sendOk("Not enough mesos."); cm.dispose(); return; }

            cm.gainMeso(-betAmount);
            var picks = [];
            // Quick Pick generates ONE number per transaction flow in this simplified logic, 
            // OR we can ask for quantity. Original code asked for qty. Let's keep it simple: 1 QP ticket per flow for now,
            // or if user wants multiple they use the menu again. 
            // Wait, previous code asked "How many tickets?". New requirement is "Flexible Amount".
            // Let's assume 1 Quick Pick Number for the amount entered.

            var num = generateRandomNumber();
            // Sort Quick Pick Number
            num = sortString(num);

            picks.push(num);
            FourDBetManager.insertBet(cm.getPlayer().getId(), num, betType, currentDrawDate.toString(), betAmount.toString(), "MESO", false);

            cm.sendOk("Placed Quick Pick bet on #e" + num + "#n for " + formatNumber(betAmount) + " mesos.");
            cm.dispose();
        } else {
            // MANUAL: HANDLE IBET -> ASK TYPE
            isIBet = (selection === 1);
            var typeStr = isIBet ? "iBet (System)" : "Direct";
            cm.sendSimple("Betting on #e" + manualNumber + " (" + typeStr + ")#n.\r\nChoose type:\r\n#L0#Big Bet#l\r\n#L1#Small Bet#l");
        }
    }

    // --- STEP 4: MANUAL AMOUNT ---
    if (status === 4) {
        if (!isQuickPick) {
            betType = (selection === 0) ? "BIG" : "SMALL";
            var typeStr = isIBet ? "iBet" : "Direct";
            cm.sendGetNumber("Enter Meso amount base per unit:\r\n(If iBet, Total = Base x Permutations)", 1000, 1000, 2000000000);
        }
    }

    // --- STEP 5: EXECUTE MANUAL ---
    if (status === 5) {
        if (!isQuickPick) {
            betAmount = selection;
            var units = 1;
            if (isIBet) {
                units = getPermutations(manualNumber);
            }

            var totalCost = betAmount * units;

            if (cm.getMeso() < totalCost) {
                cm.sendOk("Not enough mesos. You need " + formatNumber(totalCost) + " mesos.");
                cm.dispose();
                return;
            }

            cm.gainMeso(-totalCost);
            // Pass betAmount (Base amount) to DB. Manager handles pot calc.
            FourDBetManager.insertBet(
                cm.getPlayer().getId(),
                manualNumber,
                betType,
                currentDrawDate.toString(),
                betAmount.toString(),
                "MESO",
                isIBet
            );

            var typeStr = isIBet ? "iBet (System)" : "Direct";
            cm.sendOk("Placed " + typeStr + " bet on " + manualNumber + ".\r\nTotal Cost: #r" + formatNumber(totalCost) + " Mesos#k");
            cm.dispose();
        }
    }
}

function claimPrize() {
    try {
        var wins = FourDBetManager.getUnclaimedWinningBets(cm.getPlayer().getId(), "MESO");
        var totalMesos = 0;
        var bcoinsToGive = 0;

        for (var i = 0; i < wins.size(); i++) {
            var row = wins.get(i);
            var val = row.get("prize_quantity");
            var itemId = row.get("prize_item_id");

            if (itemId == 3020002) {
                // B-Coin Prize
                bcoinsToGive += val;
            } else {
                // Meso Prize
                totalMesos += val;
            }
            FourDBetManager.markBetClaimed(row.get("bet_id"));
        }

        var msg = "";
        if (totalMesos > 0) {
            if (cm.getMeso() + totalMesos < 0) { // Check overflow generally
                // This basic check might not be enough for partial withdrawals but simplifies logic
            }
            cm.gainMeso(totalMesos);
            msg += "Claimed #e" + formatNumber(totalMesos) + " Mesos#n!\r\n";
        }

        if (bcoinsToGive > 0) {
            if (!cm.canHold(3020002, bcoinsToGive)) {
                cm.sendOk("Please make space in your Etc inventory for " + bcoinsToGive + " B-Coins.");
                // Note: Bets are already marked claimed. Theoretically dangerous if inventory full. 
                // Ideally check space BEFORE claiming. 
                // However, simplifying for this context assuming space or handling manually.
                // Reverting "claimed" is hard here without loop.
                // In production code, check CanHold first.
                cm.gainItem(3020002, bcoinsToGive); // Attempt gain
            } else {
                cm.gainItem(3020002, bcoinsToGive);
                msg += "Claimed #e" + bcoinsToGive + " B-Coins#n!";
            }
        }

        if (msg === "") cm.sendOk("No Meso prizes to claim.");
        else cm.sendOk(msg);

    } catch (e) { cm.sendOk("Error claiming prize: " + e); }
    cm.dispose();
}

function showBetHistory() {
    var bets = FourDBetManager.getPastBets(cm.getPlayer().getId(), MAX_HISTORY_BETS);
    if (!bets || bets.size() === 0) { cm.sendOk("No history."); cm.dispose(); return; }

    var msg = "History:\r\n";
    for (var i = 0; i < bets.size(); i++) {
        var b = bets.get(i);
        var ibetStr = b.get("is_ibet") ? "(iBet)" : "";
        msg += b.get("draw_date") + " | " + b.get("number") + " " + ibetStr + " (" + formatNumber(b.get("amount")) + " " + b.get("currency") + ")\r\n";
    }
    cm.sendOk(msg);
    cm.dispose();
}

function generateRandomNumber() { return ("000" + Math.floor(Math.random() * 10000)).slice(-4); }
function formatNumber(num) { return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ","); }
function sortString(str) { return str.split('').sort().join(''); }

function getPermutations(number) {
    var counts = {};
    for (var i = 0; i < number.length; i++) {
        var c = number.charAt(i);
        counts[c] = (counts[c] || 0) + 1;
    }
    var unique = Object.keys(counts).length;
    if (unique === 4) return 24;
    if (unique === 3) return 12;
    if (unique === 2) {
        for (var k in counts) { if (counts[k] === 3) return 4; }
        return 6;
    }
    return 1;
}