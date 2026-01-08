var status = 0;
var TICKET_COST = 1000000; // 1 Million Mesos
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

    var msg = "#e#bWelcome to Merogie Pools (Meso)!#n#k\r\n";
    msg += "I'm Esther! Place your bets using #bMesos#k here.\r\n";
    msg += "Price per ticket: #r" + formatNumber(TICKET_COST) + " Mesos#k\r\n";
    msg += "Next Draw: #e#b" + currentDrawDate + " 12:00AM (GMT+8) #n\r\n\r\n";
    msg += "#L0##bBuy 4D Ticket#k (#dManual#k)#l\r\n";
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
        } catch (e) {}
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
                cm.sendGetNumber("How many tickets? (" + formatNumber(TICKET_COST) + " mesos each)", 1, 1, 100);
                break;
            case 4: showBetHistory(); return;
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
            ticketQty = selection;
            var total = ticketQty * TICKET_COST;
            if (cm.getMeso() < total) {
                cm.sendOk("You need " + formatNumber(total) + " mesos.");
                cm.dispose();
                return;
            }
            cm.sendSimple("Bet Type:\r\n#L0#Big Bet#l\r\n#L1#Small Bet#l");
        } else {
            manualNumber = cm.getText();
            if (!/^\d{4}$/.test(manualNumber)) { cm.sendOk("Invalid number."); cm.dispose(); return; }

            // iBet Prompt
            var msg = "You entered #e" + manualNumber + "#n.\r\n";
            msg += "Do you want this to be an #r#eiBet (System Entry)#n#k?\r\n\r\n";
            msg += "#d(iBet covers all permutations. Prize is shared.)#k\r\n";
            msg += "#L0#No, Direct Bet only#l\r\n";
            msg += "#L1#Yes, iBet#l";
            cm.sendSimple(msg);
        }
    }

    // --- STEP 3: BET TYPE / EXECUTE QP ---
    if (status === 3) {
        if (isQuickPick) {
            betType = (selection === 0) ? "BIG" : "SMALL";
            var total = ticketQty * TICKET_COST;

            if (cm.getMeso() < total) { cm.sendOk("Not enough mesos."); cm.dispose(); return; }

            cm.gainMeso(-total);
            var picks = [];
            for (var i = 0; i < ticketQty; i++) {
                var num = generateRandomNumber();
                picks.push(num);
                // Quick Pick is always isIBet = false
                FourDBetManager.insertBet(cm.getPlayer().getId(), num, betType, currentDrawDate.toString(), "1", "MESO", false);
            }
            cm.sendOk("Bought " + ticketQty + " tickets for " + formatNumber(total) + " mesos.\r\nNumbers: " + picks.join(", "));
            cm.dispose();
        } else {
            // MANUAL: HANDLE IBET -> ASK TYPE
            isIBet = (selection === 1);
            var typeStr = isIBet ? "iBet (System)" : "Direct";
            cm.sendSimple("Betting on #e" + manualNumber + " (" + typeStr + ")#n.\r\nChoose type:\r\n#L0#Big Bet#l\r\n#L1#Small Bet#l");
        }
    }

    // --- STEP 4: MANUAL QUANTITY ---
    if (status === 4) {
        if (!isQuickPick) {
            betType = (selection === 0) ? "BIG" : "SMALL";
            var typeStr = isIBet ? "iBet" : "Direct";
            cm.sendGetNumber("How many tickets on " + manualNumber + " (" + typeStr + ")? (" + formatNumber(TICKET_COST) + " mesos each)", 1, 1, 100);
        }
    }

    // --- STEP 5: EXECUTE MANUAL ---
    if (status === 5) {
        if (!isQuickPick) {
            ticketQty = selection;
            var total = ticketQty * TICKET_COST;

            if (cm.getMeso() < total) { cm.sendOk("Not enough mesos."); cm.dispose(); return; }

            cm.gainMeso(-total);
            // Pass isIBet
            FourDBetManager.insertBet(
                cm.getPlayer().getId(),
                manualNumber,
                betType,
                currentDrawDate.toString(),
                ticketQty.toString(),
                "MESO",
                isIBet
            );

            var typeStr = isIBet ? "iBet" : "Direct";
            cm.sendOk("Placed " + ticketQty + " " + typeStr + " bets on " + manualNumber + ".\r\nTotal: " + formatNumber(total) + " Mesos");
            cm.dispose();
        }
    }
}

function claimPrize() {
    try {
        var wins = FourDBetManager.getUnclaimedWinningBets(cm.getPlayer().getId(), "MESO");
        var total = 0;

        for (var i = 0; i < wins.size(); i++) {
            var row = wins.get(i);
            var val = row.get("prize_quantity");
            if (val > 0) {
                total += val;
                FourDBetManager.markBetClaimed(row.get("bet_id"));
            }
        }

        if (total > 0) {
            // Safety check for Meso Cap (Java logic handles calc, JS handles display)
            if (cm.getMeso() + total < 0) { // Simple overflow check
                cm.sendOk("You have too many mesos to claim this prize! Clear some space.");
            } else {
                cm.gainMeso(total);
                cm.sendOk("Claimed #e" + formatNumber(total) + " Mesos#n!");
            }
        } else {
            cm.sendOk("No Meso prizes to claim.");
        }
    } catch (e) { cm.sendOk("Error claiming prize."); }
    cm.dispose();
}

function showBetHistory() {
    var bets = FourDBetManager.getPastBets(cm.getPlayer().getId(), MAX_HISTORY_BETS);
    if (!bets || bets.size() === 0) { cm.sendOk("No history."); cm.dispose(); return; }

    var msg = "History:\r\n";
    for (var i = 0; i < bets.size(); i++) {
        var b = bets.get(i);
        var ibetStr = b.get("is_ibet") ? "(iBet)" : "";
        msg += b.get("draw_date") + " | " + b.get("number") + " " + ibetStr + " (" + b.get("currency") + ")\r\n";
    }
    cm.sendOk(msg);
    cm.dispose();
}

function generateRandomNumber() { return ("000" + Math.floor(Math.random() * 10000)).slice(-4); }
function formatNumber(num) { return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ","); }