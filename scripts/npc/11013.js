var status = 0;
var NX_MCOIN_ID = 3020001;
var MESO_BCOIN_ID = 3020002;
var manualNumber = "";
var betType = "";
var currentDrawDate;
var betAmount = 1;
var isQuickPick = false;
var dateList = null;
var MAX_HISTORY_BETS =100;
var MAX_HISTORY_DRAWS =7;
var FourDBetManager = Java.type("server.gambling.FourDBetManager");
var FourDResultManager = Java.type("server.gambling.FourDResultManager");
var FourDDrawScheduler = Java.type("server.gambling.FourDDrawScheduler");

function start() {
    status = 0;
    currentDrawDate = FourDDrawScheduler.getNextDrawDate();

    var msg = "#e#bWelcome to Merogie Pools~!#n#k\r\n";
    msg += "Hi there~ I'm your lovely lottery lady, Esther! You Can Bet and WIN BCOIN with me!\r\n";
    msg += "The Next 4D draw is on: #e#b" + currentDrawDate + " 12:00AM (GMT+8) #n\r\n\r\n";
    msg += "How may I help you today? Teehee~\r\n\r\n";
    msg += "#L0##bBuy 4D Ticket#k (#dManual Entry#k) - Got a lucky number?#l\r\n";
    msg += "#L3##bBuy 4D Ticket#k (#gQuick Pick#k) - Leave it to fate?~#l\r\n";
    msg += "#L1##bView Past Draw Results#k - Let's peek at history!#l\r\n";
    msg += "#L2##rClaim Prize#k - Ooooh, did you wiiin?#l\r\n";
    msg += "#L4##bView My Past Bets#k - Curious what you picked?#l\r\n";

    if (cm.getPlayer().isGM()) {
        msg += "#L99##k(GM Only) Force Today's Draw#l\r\n";
    }

    cm.sendSimple(msg);
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.sendOk("Alrighty~ come back anytime, okay? I'll be right here~");
        cm.dispose();
        return;
    }

    status++;

    // Handle result viewing selection
    if (status === 2 && dateList !== null) {
        var selectedDate = dateList.get(selection);
        dateList = null;

        try {
            var result = FourDResultManager.getResultByDate(java.time.LocalDate.parse(selectedDate));
            if (result !== null) {
                var msg = "#eResults for #b" + selectedDate + "#k:#n\r\n\r\n";
                msg += "#e1st:#n #r" + result.first + "#k\r\n";
                msg += "#e2nd:#n #r" + result.second + "#k\r\n";
                msg += "#e3rd:#n #r" + result.third + "#k\r\n\r\n";
                msg += "#eStarters:#n\r\n#b" + result.starters + "#k\r\n\r\n";
                msg += "#eConsolations:#n\r\n#b" + result.consolations + "#k\r\n\r\n";
                cm.sendOk(msg + "Maybe your number is next~ ");
            } else {
                cm.sendOk("Hmm... no result found for that date. Try another?");
            }
        } catch (e) {
            cm.sendOk("Oopsie! I couldn't retrieve the result properly.");
        }
        cm.dispose();
        return;
    }

    // Main menu logic
    if (status === 1) {
        switch (selection) {
            case 0: // Manual bet
                isQuickPick = false;
                cm.sendGetText("Type your 4-digit number (#r0000#k-#r9999#k):");
                break;

            case 1: // View past results
                dateList = FourDResultManager.getRecentDrawDates(MAX_HISTORY_DRAWS);

                if (!dateList || dateList.size() === 0) {
                    cm.sendOk("No draws yet~ Come back after today's magic happens!");
                    cm.dispose();
                    return;
                }
                var menu = "#eChoose a draw date to view its results:#n\r\n";
                for (var i = 0; i < dateList.size(); i++) {
                    menu += "#L" + i + "##b" + dateList.get(i) + "#k#l\r\n";
                }
                cm.sendSimple(menu);
                return;

            case 2:
                claimPrize();
                return;

            case 3: // Quick pick
                isQuickPick = true;
                cm.sendGetNumber("How many tickets? Each costs #v" + MESO_BCOIN_ID + "#:", 1, 1, 100);
                break;

            case 4: // Past bets
                var bets = FourDBetManager.getPastBets(cm.getPlayer().getId(), MAX_HISTORY_BETS);
                if (!bets || bets.size() === 0) {
                    cm.sendOk("No past bets found~ Wanna try your luck today?");
                } else {
                    var list = "#eYour Past Bets:#n\r\n";
                    for (var i = 0; i < bets.size(); i++) {
                        var b = bets.get(i);
                        list += "#b"
                        + b.get("draw_date")
                        + "#k -> "
                        + " [" + b.get("bet_type") + "] "
                        + b.get("number")
                        + " x "
                        +b.get("amount")

                        + "\r\n";
                    }
                    cm.sendOk(list);
                }
                cm.dispose();
                return;

            case 99: // GM option
                FourDDrawScheduler.forceDrawToday();
                cm.sendOk("Today's draw has been forced manually.");
                cm.dispose();
                return;
        }
    }

    if (status === 2) {
        if (isQuickPick) {
            betAmount = selection;
            if (betAmount <= 0 || !cm.haveItem(MESO_BCOIN_ID, betAmount)) {
                cm.sendOk("Aww~ not enough #v" + MESO_BCOIN_ID + "# BCoins.");
                cm.dispose();
                return;
            }
            cm.sendSimple("Choose your bet type:\r\n#L0#Big Bet (More coverage)#l\r\n#L1#Small Bet (Higher payout)#l");
        } else {
            manualNumber = cm.getText();
            if (!/^\d{4}$/.test(manualNumber)) {
                cm.sendOk("That's not a valid 4-digit number~ Try again!");
                cm.dispose();
                return;
            }
            cm.sendSimple("Betting on #e" + manualNumber + "#n~\r\n#L0#Big Bet#l\r\n#L1#Small Bet#l");
        }
    }

    if (status === 3) {
        betType = (selection === 0) ? "BIG" : "SMALL";

        if (!isQuickPick) {
            cm.sendGetNumber("How many #v" + MESO_BCOIN_ID + "# will you bet?", 1, 1, 100);
        } else {
            if (!cm.haveItem(MESO_BCOIN_ID, betAmount)) {
                cm.sendOk("Oops~ Not enough #v" + MESO_BCOIN_ID + "#.");
                cm.dispose();
                return;
            }
            cm.gainItem(MESO_BCOIN_ID, -betAmount);

            var picks = [];
            for (var i = 0; i < betAmount; i++) {
                var num = generateRandomNumber();
                if (!/^\d{4}$/.test(num)) continue; // extra safety
                picks.push(num);
                FourDBetManager.insertBet(cm.getPlayer().getId(), num, betType, currentDrawDate.toString(), "1","BCOIN");
            }

            cm.sendOk("You placed #e" + betAmount + "#n Quick Pick #b" + betType + "#k bet(s):\r\n#d" + picks.join(", ") +
                      "#k\r\nfor #b" + currentDrawDate + "#k.\r\nFingers crossed~");
            cm.dispose();
        }
    }

    if (status === 4 && !isQuickPick) {
        betAmount = selection;
        if (betAmount <= 0 || betAmount > 100) {
            cm.sendOk("That's not a valid number of bets.");
            cm.dispose();
            return;
        }
        if (!cm.haveItem(MESO_BCOIN_ID, betAmount)) {
            cm.sendOk("Oopsie~ You don't have enough #v" + MESO_BCOIN_ID + "#.");
            cm.dispose();
            return;
        }

        cm.gainItem(MESO_BCOIN_ID, -betAmount);
        FourDBetManager.insertBet(cm.getPlayer().getId(), manualNumber, betType, currentDrawDate.toString(), betAmount.toString(),"BCOIN");

        cm.sendOk("You've placed #e" + betAmount + "#n " + betType + " bet(s) on #e" + manualNumber +
                  "#n for #b" + currentDrawDate + "#k!\r\nGood luck, sweetheart~");
        cm.dispose();
    }
}

function claimPrize() {
    try {
        // Get unclaimed winning bets for this player using BCOIN currency
        var playerId = cm.getPlayer().getId();
        print("[DEBUG] Player ID: " + playerId);
        var wins = FourDBetManager.getUnclaimedWinningBets(playerId, "BCOIN");
        print("[DEBUG] Retrieved winning bets. Count: " + wins.size());

        var total = 0;

        // Loop through all unclaimed winning bets
        for (var i = 0; i < wins.size(); i++) {
            var row = wins.get(i);
            var betId = row.get("bet_id");
            var qty = row.get("prize_quantity");

            print("[DEBUG] Processing bet ID: " + betId + ", prize_quantity: " + qty);

            if (qty > 0) {
                total += qty;

                // Mark bet as claimed in DB
                print("[DEBUG] Marking bet ID " + betId + " as claimed.");
                FourDBetManager.markBetClaimed(betId);
            }
        }

        // Issue the prize item if total > 0
        if (total > 0) {
            print("[DEBUG] Awarding total BCOINs: " + total);
            print("[DEBUG] MESO_BCOIN_ID: " + MESO_BCOIN_ID);
            cm.gainItem(MESO_BCOIN_ID, total);
            cm.sendOk("You claimed #e" + total + "#n #v" + MESO_BCOIN_ID + "#!\r\nCome win more next time~");
        } else {
            print("[DEBUG] No prizes to claim.");
            cm.sendOk("No prizes to claim just yet~ Keep playing!");
        }

    } catch (e) {
        print("[ERROR] Exception during claimPrize: " + e);
        cm.sendOk("Eek! Something went wrong checking your prize.");
    }

    cm.dispose();
}


function generateRandomNumber() {
    return ("000" + Math.floor(Math.random() * 10000)).slice(-4);
}
