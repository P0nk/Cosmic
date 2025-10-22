/* 11021.js — Creator Shop Claim Board
 * View creator shops, claim earnings (password-protected)
 * Simplified: no use of tempData methods.
 */

const CreatorShopManager = Packages.server.creator_shop.CreatorShopManager;

var status = 0;
var selectedShop = null;
var enteredPassword = null;
var pendingReward = 0;

// Define all creator shops here
const CREATOR_SHOPS = [
    { npcId: 11020, name: "Sunny's Shop", share: 0.60, password: "SUNNY123" }
];

function start() {
    status = 0;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode != 1) {
        cm.dispose();
        return;
    }
    status++;

    // Step 1 — Choose shop
    if (status == 1) {
        var text = "#e[ Creator Shop Earnings Board ]#n\r\n\r\n";
        text += "Select a creator shop to manage.\r\n";
        for (var i = 0; i < CREATOR_SHOPS.length; i++) {
            text += "#L" + i + "#View #b" + CREATOR_SHOPS[i].name + "#k#l\r\n";
        }
        cm.sendSimple(text);
    }

    // Step 2 — Password input
    else if (status == 2) {
        selectedShop = CREATOR_SHOPS[selection];
        if (!selectedShop) {
            cm.sendOk("Invalid shop.");
            cm.dispose();
            return;
        }
        cm.sendGetText("Please enter the password for #b" + selectedShop.name + "#k to proceed:");
    }

    // Step 3 — Verify password, show earnings only after success
    else if (status == 3) {
        enteredPassword = cm.getText();

        if (enteredPassword !== selectedShop.password) {
            cm.sendOk("❌ Incorrect password. Claim cancelled.");
            cm.dispose();
            return;
        }

        var total = CreatorShopManager.getUnclaimedTotal(selectedShop.npcId);
        if (total <= 0) {
            cm.sendOk("No unclaimed balance available for " + selectedShop.name + ".");
            cm.dispose();
            return;
        }

        pendingReward = Math.floor(total * selectedShop.share);

        cm.sendYesNo(
            "✅ Password verified!\r\n\r\n" +
            "Total unclaimed earnings: #b" + cm.numberWithCommas(total) + "#k mesos\r\n" +
            "Your share (" + (selectedShop.share * 100) + "%): #b" +
            cm.numberWithCommas(pendingReward) + "#k mesos\r\n\r\n" +
            "Would you like to claim your reward now?"
        );
    }

    // Step 4 — Claim payout
    else if (status == 4) {
        if (pendingReward <= 0 || selectedShop == null) {
            cm.sendOk("No pending reward found.");
            cm.dispose();
            return;
        }

        cm.gainMeso(pendingReward);
        CreatorShopManager.markClaimed(selectedShop.npcId);

        cm.sendOk(
            "💰 Successfully claimed #b" + cm.numberWithCommas(pendingReward) +
            "#k mesos for #b" + selectedShop.name + "#k!\r\nThank you for your contributions!"
        );

        // Reset local state
        pendingReward = 0;
        selectedShop = null;
        cm.dispose();
    }
}
