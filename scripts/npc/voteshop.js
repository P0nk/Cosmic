/* GTop100 Vote Reward Center
   Script: voteshop.js
   Description: Unlimited Claims + 8 AM SGT Reset Countdown.
*/

var status = -1;
var summary = null;

function start() {
    Packages.server.voting.VoteManager.linkPendingVotes(cm.getPlayer());
    summary = Packages.server.voting.VoteManager.calculate(cm.getPlayer());
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1 || mode == 0) {
        cm.dispose();
        return;
    }
    status++;

    if (status == 0) {
        var timeLeft = getTimeUntilReset();

        var msg = "#e<Vote Reward Center>#n\r\n";
        msg += "Welcome, #h #. Here are your voting statistics.\r\n";
        msg += "------------------------------------------------\r\n";
        msg += "Lifetime Votes: #b" + summary.lifetimeVotes + "#k\r\n";
        msg += "Current Streak: #b" + summary.projectedStreak + " Days#k\r\n";
        msg += "Unclaimed Votes: #r" + summary.count + "#k\r\n";
//        msg += "GTop Vote Reset in  in: #b" + timeLeft + "#k\r\n";
        msg += "------------------------------------------------\r\n";

        // Logic: Blue (#b) if votes exist, Black (#k) if empty.
        var color = (summary.count > 0) ? "#b" : "#k";

        msg += "Reward Packages (Values include Streak Bonus):\r\n";
        msg += "#L0# " + color + "Claim " + formatNumber(summary.totalNx) + " NX#k#l\r\n";
        msg += "#L1# " + color + "Claim " + formatNumber(summary.totalMeso) + " Mesos#k#l\r\n";
        msg += "#L2# " + color + "Claim " + formatNumber(summary.totalLeaves) + " Maple Leaves#k#l\r\n";
        msg += "#L3# " + color + "Claim " + formatNumber(summary.totalElixirs) + " Power Elixirs#k#l\r\n";

        msg += "\r\n#eOther Options:#n\r\n";
        msg += "#L98# #d[History] View recent transactions#k#l\r\n";
        msg += "#L99# #d[Guide] How does the voting system work?#k#l";

        cm.sendSimple(msg);

    } else if (status == 1) {
        if (selection == 98) {
            var history = Packages.server.voting.VoteManager.getHistory(cm.getPlayer().getAccountID());
            cm.sendOk("#e<Recent Transactions>#n\r\n\r\n" + history);
            cm.dispose();
            return;
        }

        if (selection == 99) {
            var guide = "#e<Voting System Guide>#n\r\n\r\n";
            guide += "#e1. How to Guarantee Your Vote:#n\r\n";
            guide += "To ensure you receive credit regardless of your IP address, please append your username to the voting URL.\r\n";
            guide += "Example: #d...server-105154?vote=1&pingUsername=" + cm.getPlayer().getName() + "#k\r\n\r\n";

            guide += "#e2. Streak System:#n\r\n";
            guide += "Vote consecutive days to build your reward multiplier.\r\n";
            guide += "Streaks update daily at #r08:00 AM (GMT+8)#k.\r\n\r\n";

            guide += "#e3. Unlimited Claims:#n\r\n";
            guide += "You can claim rewards as many times as you have valid votes.";

            cm.sendOk(guide);
            cm.dispose();
            return;
        }

        if (summary.count <= 0) {
            cm.sendOk("You don't have any verified votes yet.\r\nPlease vote on GTop100 and ensure you enter your #bCharacter Name#k in the 'pingUsername' field.");
            cm.dispose();
            return;
        }

        var result = Packages.server.voting.VoteManager.claim(cm, selection);

        if (result.equals("Success")) {
            var rewardInfo = "";
            var amount = "";

            if (selection == 0) { rewardInfo = "NX"; amount = summary.totalNx; }
            else if (selection == 1) { rewardInfo = "Mesos"; amount = summary.totalMeso; }
            else if (selection == 2) { rewardInfo = "Maple Leaves"; amount = summary.totalLeaves; }
            else if (selection == 3) { rewardInfo = "Power Elixirs"; amount = summary.totalElixirs; }

            cm.sendOk("Success! You claimed #b" + formatNumber(amount) + " " + rewardInfo + "#k.\r\nYour streak is now #g" + summary.projectedStreak + " Days#k.");
        } else {
            cm.sendOk("Error: " + result);
        }
        cm.dispose();
    }
}

function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

function getTimeUntilReset() {
    var now = new Date();
    // Logic: Calculate time until 00:00 UTC (which is 08:00 GMT+8)
    var utcHours = now.getUTCHours();
    var utcMinutes = now.getUTCMinutes();

    // 24 - current UTC hours gives hours until 00:00 UTC
    var hoursLeft = 23 - utcHours;
    var minutesLeft = 59 - utcMinutes;

    var minutesStr = (minutesLeft < 10) ? "0" + minutesLeft : minutesLeft;
    return hoursLeft + "h " + minutesStr + "m";
}