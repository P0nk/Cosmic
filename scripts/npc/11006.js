/* NPC: Maple Leaf Event Manager
   Logic: Accepts donations into a queue (Max 40k).
   10k leaves = 1 Hour boost.
   Automatically chains hours if leaves remain in queue.
*/

var status = -1;
var MapleLeafWorldBuffManager = Java.type("server.buffnpc.MapleLeafWorldBuffManager");
var leafItemId = 4001126;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === -1 || (mode === 0 && status === 0)) {
        cm.dispose();
        return;
    }

    if (mode === 1) {
        status++;
    } else {
        status--;
    }

    // --- Status 0: Dashboard ---
    if (status === 0) {
        // 1. Get Queued Leaves (Bank)
        var queued = MapleLeafWorldBuffManager.getQueuedLeaves();
        var maxCap = MapleLeafWorldBuffManager.getMaxCap();

        // 2. Get Total Time Remaining (Active + Queue)
        var timeRemaining = MapleLeafWorldBuffManager.getTotalTimeRemaining();

        var msg = "#b[World Spawn Event]#k\r\n";

        // Display Time Logic
        if (timeRemaining > 0) {
            var totalSeconds = Math.floor(timeRemaining / 1000);
            var hours = Math.floor(totalSeconds / 3600);
            var minutes = Math.floor((totalSeconds % 3600) / 60);

            msg += "Status: #gACTIVE#k\r\n";
            msg += "Total Time Left: #b" + hours + "h " + minutes + "m#k (Includes stored leaves)\r\n";
        } else {
            msg += "Status: #rINACTIVE#k\r\n";
        }

        msg += "\r\nLeaves stored: " + queued + " / " + maxCap + "\r\n";
        msg += "(Every 5,000 leaves extends the event by 1 hour)\r\n\r\n";

        // Check if full
        if (queued >= maxCap) {
            msg += "#rThe storage is full (20,000)! Please wait for the current leaves to be consumed.#k";
            cm.sendOk(msg);
            cm.dispose();
        } else {
            msg += "#L0#Donate Maple Leaves#l";
            cm.sendSimple(msg);
        }
    }

    // --- Status 1: Input ---
    else if (status === 1) {
        var queued = MapleLeafWorldBuffManager.getQueuedLeaves();
        var maxCap = MapleLeafWorldBuffManager.getMaxCap();
        var spaceAvailable = maxCap - queued;

        cm.sendGetText("How many leaves would you like to donate?\r\n(Max you can donate: " + spaceAvailable + ")");
    }

    // --- Status 2: Processing ---
    else if (status === 2) {
        var text = cm.getText();
        if (text == null || text === "") { cm.dispose(); return; }

        var donationAmount = parseInt(text);

        if (isNaN(donationAmount) || donationAmount <= 0) {
            cm.sendOk("Invalid number.");
            cm.dispose();
            return;
        }

        if (!cm.haveItem(leafItemId, donationAmount)) {
            cm.sendOk("You do not have enough items.");
            cm.dispose();
            return;
        }

        // Check overflow protection again
        if (!MapleLeafWorldBuffManager.canDonate(donationAmount)) {
            var space = MapleLeafWorldBuffManager.getMaxCap() - MapleLeafWorldBuffManager.getQueuedLeaves();
            cm.sendOk("That amount exceeds the limit! You can only donate " + space + " more leaves.");
            cm.dispose();
            return;
        }

        // Execute
        cm.gainItem(leafItemId, -donationAmount);
        MapleLeafWorldBuffManager.handleDonation(donationAmount);

        cm.sendOk("Donation successful! If the pool hit 5k, the timer has been extended.");
        cm.dispose();
    }
}