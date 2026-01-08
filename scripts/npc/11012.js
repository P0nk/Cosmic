var status = 0;
var TICKET_COST = 500; // 500 NX per ticket
var manualNumber = "";
var betType = "";
var currentDrawDate;
var ticketQty = 1;
var isQuickPick = false;
var isIBet = false; // New iBet flag
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
    msg += "Price per ticket: #r" + formatNumber(TICKET_COST) + " NX#k\r\n";
    msg += "Next Draw: #e#b" + currentDrawDate + " 12:00AM (GMT+8) #n\r\n\r\n";
    msg += "#L0##bBuy 4D Ticket#k (#dManual Entry#k)#l\r\n";
    msg += "#L3##bBuy 4D Ticket#k (#gQuick Pick#k)#l\r\n";
    msg += "#L1##bView Past Draw Results#k#l\r\n";
    msg += "#L2##rClaim Prize#k#l\r\n";
    msg += "#L4##bView My Past Bets#k#l\r\n";

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
                cm.sendGetNumber("How many Quick Pick tickets? (" + formatNumber(TICKET_COST) + " NX each)", 1, 1, 100);
                break;
            case 4: // Bet History
                showBetHistory();
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
            ticketQty = selection;
            var totalCost = ticketQty * TICKET_COST;
            // Check NX (1 = Credit)
            if (cm.getPlayer().getCSPoints(1) < totalCost) {
                cm.sendOk("You need " + formatNumber(totalCost) + " NX.");
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
            var msg = "You entered #e" + manualNumber + "#n.\r\n";
            msg += "Do you want this to be an #r#eiBet (System Entry)#n#k?\r\n\r\n";
            msg += "#d(iBet covers all permutations, e.g. 1234 covers 4321. Prize is shared.)#k\r\n";
            msg += "#L0#No, Direct Bet only (Exact Match)#l\r\n";
            msg += "#L1#Yes, iBet (Any Order)#l";
            cm.sendSimple(msg);
        }
    }

    // --- STEP 3: BET TYPE / EXECUTE QUICK PICK ---
    if (status === 3) {
        if (isQuickPick) {
            // EXECUTE QUICK PICK
            betType = (selection === 0) ? "BIG" : "SMALL";
            var totalCost = ticketQty * TICKET_COST;

            if (cm.getPlayer().getCSPoints(1) < totalCost) {
                cm.sendOk("Not enough NX.");
                cm.dispose();
                return;
            }

            cm.getPlayer().modifyCSPoints(1, -totalCost);

            var picks = [];
            for (var i = 0; i < ticketQty; i++) {
                var num = generateRandomNumber();
                picks.push(num);
                // Quick Pick is always Direct (isIBet = false)
                FourDBetManager.insertBet(cm.getPlayer().getId(), num, betType, currentDrawDate.toString(), "1", "NX", false);
            }
            cm.sendOk("Bought " + ticketQty + " Quick Picks for " + formatNumber(totalCost) + " NX.\r\nNumbers: #b" + picks.join(", ") + "#k");
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
            cm.sendGetNumber("How many tickets for " + manualNumber + " (" + typeStr + ")? (" + formatNumber(TICKET_COST) + " NX each)", 1, 1, 100);
        }
    }

    // --- STEP 5: EXECUTE MANUAL ---
    if (status === 5) {
        if (!isQuickPick) {
            ticketQty = selection;
            var totalCost = ticketQty * TICKET_COST;

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
                ticketQty.toString(),
                "NX",
                isIBet
            );

            var typeStr = isIBet ? "iBet" : "Direct";
            cm.sendOk("Placed " + ticketQty + " " + typeStr + " bets on #e" + manualNumber + "#n for " + formatNumber(totalCost) + " NX.");
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
        msg += "#b" + b.get("draw_date") + "#k | " + b.get("number") + " " + ibetStr + " | " + b.get("currency") + "\r\n";
    }
    cm.sendOk(msg);
    cm.dispose();
}

function generateRandomNumber() { return ("000" + Math.floor(Math.random() * 10000)).slice(-4); }
function formatNumber(num) { return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ","); }