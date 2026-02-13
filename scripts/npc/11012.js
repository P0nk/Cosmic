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

    var msg = "#e#bWelcome to Merogie Pools (NX)!#n#k\r\n";
    msg += "Hi! I'm Rebecca. You can bet using #bNX Cash#k here!\r\n";
    msg += "Next Draw: #e#b" + currentDrawDate + " 12:00AM (GMT+8) #n\r\n\r\n";
    msg += "#L0##bBuy 4D Ticket#k (#dManual Entry#k)#l\r\n";
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

    // --- VIEW PAST RESULTS (Intercept) ---
    if (status === 2 && dateList !== null) {
        var selectedDate = dateList.get(selection);
        try {
            var result = FourDResultManager.getResultByDate(java.time.LocalDate.parse(selectedDate));
            if (result !== null) {
                var msg = "#eResults for #b" + selectedDate + "#k:#n\r\n\r\n";
                msg += "1st: #r" + result.get("first") + "#k | 2nd: #r" + result.get("second") + "#k | 3rd: #r" + result.get("third") + "#k\r\n\r\n";
                msg += "Starters: " + result.get("starters") + "\r\n";
                msg += "Consolations: " + result.get("consolations");
                cm.sendOk(msg);
            }
        } catch (e) { cm.sendOk("Error retrieving results."); }
        cm.dispose();
        return;
    }

    // --- MAIN MENU ---
    if (status === 1) {
        switch (selection) {
            case 0: // Manual
                isQuickPick = false;
                cm.sendGetText("Enter your lucky 4-digit number (0000-9999):");
                break;
            case 1: // History
                dateList = FourDResultManager.getRecentDrawDates(MAX_HISTORY_DRAWS);
                if (!dateList || dateList.size() === 0) {
                    cm.sendOk("No history yet.");
                    cm.dispose();
                } else {
                    var menu = "Select a date:\r\n";
                    for (var i = 0; i < dateList.size(); i++) menu += "#L" + i + "#" + dateList.get(i) + "#l\r\n";
                    cm.sendSimple(menu);
                }
                return;
            case 2: // Claim
                claimPrize();
                return;
            case 3: // Quick Pick
                isQuickPick = true;
                cm.sendGetNumber("Enter NX Amount per bet:", 100, 100, 1000000);
                break;
            case 4: // Bet History
                showBetHistory();
                return;
            case 5: // Guide
                showGuide();
                return;
            case 99:
                FourDDrawScheduler.forceDrawToday();
                cm.sendOk("Draw forced.");
                cm.dispose();
                return;
        }
    }

    // --- STEP 2: INPUT VALIDATION / MANUAL IBET PROMPT ---
    if (status === 2) {
        if (isQuickPick) {
            betAmount = selection;
            // Check NX (1 = Credit)
            if (cm.getPlayer().getCSPoints(1) < betAmount) {
                cm.sendOk("You need " + formatNumber(betAmount) + " NX.");
                cm.dispose();
                return;
            }
            cm.sendSimple("Choose your bet type:\r\n#L0#Big Bet (1st/2nd/3rd/Starter/Consolation)#l\r\n#L1#Small Bet (1st/2nd/3rd Only)#l");
        } else {
            manualNumber = cm.getText();
            if (!/^\d{4}$/.test(manualNumber)) {
                cm.sendOk("Invalid number. Please enter 4 digits.");
                cm.dispose();
                return;
            }
            // NEW: iBet Prompt
            var perms = getPermutations(manualNumber);
            var msg = "You entered #e" + manualNumber + "#n.\r\n";
            msg += "Do you want this to be an #r#eiBet (System Entry)#n#k?\r\n";
            msg += "(iBet covers all " + perms + " permutations. You pay for each one.)\r\n\r\n";
            msg += "#L0#No, Direct Bet only (1 unit)#l\r\n";
            msg += "#L1#Yes, iBet (System Entry - " + perms + " units)#l";
            cm.sendSimple(msg);
        }
    }

    // --- STEP 3: BET TYPE / EXECUTE QUICK PICK ---
    if (status === 3) {
        if (isQuickPick) {
            // EXECUTE QUICK PICK
            betType = (selection === 0) ? "BIG" : "SMALL";

            if (cm.getPlayer().getCSPoints(1) < betAmount) {
                cm.sendOk("Not enough NX.");
                cm.dispose();
                return;
            }

            cm.getPlayer().modifyCSPoints(1, -betAmount);

            var picks = [];
            // Single QP Ticket for now
            var num = generateRandomNumber();
            num = sortString(num); // Sort
            picks.push(num);

            // iBet is false for QP
            FourDBetManager.insertBet(cm.getPlayer().getId(), num, betType, currentDrawDate.toString(), betAmount.toString(), "NX", false);

            cm.sendOk("Placed Quick Pick bet on #e" + num + "#n for " + formatNumber(betAmount) + " NX.");
            cm.dispose();
        } else {
            // MANUAL: HANDLE IBET SELECTION -> ASK TYPE
            isIBet = (selection === 1);
            var typeStr = isIBet ? "iBet (System)" : "Direct";
            cm.sendSimple("Betting on #e" + manualNumber + " (" + typeStr + ")#n.\r\nChoose bet type:\r\n#L0#Big Bet#l\r\n#L1#Small Bet#l");
        }
    }

    // --- STEP 4: MANUAL QUANTITY ---
    if (status === 4) {
        if (!isQuickPick) {
            betType = (selection === 0) ? "BIG" : "SMALL";
            var typeStr = isIBet ? "iBet" : "Direct";
            cm.sendGetNumber("Enter NX amount base per unit:\r\n(If iBet, Total = Base x Permutations)", 100, 100, 1000000);
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

            if (cm.getPlayer().getCSPoints(1) < totalCost) {
                cm.sendOk("Not enough NX. Need: " + formatNumber(totalCost));
                cm.dispose();
                return;
            }

            cm.getPlayer().modifyCSPoints(1, -totalCost);

            // Pass all parameters including isIBet
            FourDBetManager.insertBet(
                cm.getPlayer().getId(),
                manualNumber,
                betType,
                currentDrawDate.toString(),
                betAmount.toString(),
                "NX",
                isIBet
            );

            var typeStr = isIBet ? "iBet" : "Direct";
            cm.sendOk("Placed " + typeStr + " bet on #e" + manualNumber + "#n.\r\nTotal Cost: " + formatNumber(totalCost) + " NX.");
            cm.dispose();
        }
    }
}

function claimPrize() {
    try {
        var wins = FourDBetManager.getUnclaimedWinningBets(cm.getPlayer().getId(), "NX");
        var total = 0;

        for (var i = 0; i < wins.size(); i++) {
            var row = wins.get(i);
            var prizeVal = row.get("prize_quantity");
            // Ignore prize_item_id as NX rewards stay NX
            if (prizeVal > 0) {
                total += prizeVal;
                FourDBetManager.markBetClaimed(row.get("bet_id"));
            }
        }

        if (total > 0) {
            cm.getPlayer().modifyCSPoints(1, total);
            cm.sendOk("Congratulations! You claimed #e" + formatNumber(total) + " NX#n!");
        } else {
            cm.sendOk("No unclaimed NX prizes found.");
        }
    } catch (e) { cm.sendOk("Error claiming prize."); }
    cm.dispose();
}

function showBetHistory() {
    var bets = FourDBetManager.getPastBets(cm.getPlayer().getId(), MAX_HISTORY_BETS);
    if (!bets || bets.size() === 0) {
        cm.sendOk("No bet history.");
        cm.dispose();
        return;
    }
    var msg = "Your Recent Bets:\r\n";
    for (var i = 0; i < bets.size(); i++) {
        var b = bets.get(i);
        var ibetStr = b.get("is_ibet") ? "(iBet)" : "";
        msg += "#b" + b.get("draw_date") + "#k | " + b.get("number") + " " + ibetStr + " | " + formatNumber(b.get("amount")) + " " + b.get("currency") + "\r\n";
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