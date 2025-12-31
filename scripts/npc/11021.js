/* 11021.js — Creator Shop & Season 1 Redemption
 * 1. Creator Shop: Rewards creators in B-Coins
 * 2. Season 1 Redemption: Checks account creation date & webadmin tag
 */

var CreatorShopManager = Packages.server.creator_shop.CreatorShopManager;
var EnvLoader = Packages.tools.EnvLoader;
var DatabaseConnection = Packages.tools.DatabaseConnection;

// --- CONFIGURATION: CREATOR SHOP ---
var BCOIN_ID = 3020002;
var CURRENCY = "BCOIN";
var CREATOR_SHOPS = [
    {
        npcId: 11020,
        name: "Sunny's Shop",
        share: 0.50,
        password: EnvLoader.get("SUNNY_SHOP_PASSWORD")
    }
];

// --- CONFIGURATION: REDEMPTION ---
var CUTOFF_DATE = "2025-12-31";

// 1. FIXED REWARDS: Everyone gets these
// Format: [ItemID, Quantity]
var REDEMPTION_REWARDS = [
    [3015700, 1],   // Eggy New Year Chair
//    [1142987, 1],   // MerogieMS Season 1 Medal
    [3020001, 2]    // NXT
];

// 2. RANDOM REWARD: Everyone gets exactly ONE of these
// Format: ItemID only (Quantity is always 1)
var RANDOM_REWARD_POOL = [
    1002600,
    1002601,
    1002602,
    1002603,
    1002959
];

// --- INTERNAL STATE VARIABLES ---
var status = 0;
var selectedFeature = -1;
var selectedShop = null;
var shopPassword = null;
var shopReward = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 0 && status == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) status++;
    else status--;

    // --- MAIN MENU ---
    if (status == 0) {
        var msg = "#e[ Admina - System Interface ]#n\r\n\r\n" +
                  "Hello #h #. How can I assist you today?\r\n" +
                  "#L0#Creator Shop Earnings Board#l\r\n" +
                  "#L1#Season 1 Account Redemption#l";
        cm.sendSimple(msg);
    }

    // --- ROUTING ---
    else if (status == 1) {
        if (selectedFeature == -1) {
            selectedFeature = selection;
        }

        if (selectedFeature == 0) {
            handleCreatorShop(selection, true);
        } else if (selectedFeature == 1) {
            handleRedemption();
        }
    }

    else {
        if (selectedFeature == 0) {
            handleCreatorShop(selection, false);
        } else if (selectedFeature == 1) {
            cm.dispose();
        }
    }
}

// ==========================================================
// FEATURE 1: SEASON 1 REDEMPTION
// ==========================================================
function handleRedemption() {
    var accId = cm.getPlayer().getAccountID();
    var conn = DatabaseConnection.getConnection();

    try {
        var ps = conn.prepareStatement("SELECT createdat, webadmin FROM accounts WHERE id = ?");
        ps.setInt(1, accId);
        var rs = ps.executeQuery();

        if (rs.next()) {
            var createdDate = rs.getTimestamp("createdat");
            var claimStatus = rs.getInt("webadmin");
            var cutoffTimestamp = java.sql.Timestamp.valueOf(CUTOFF_DATE + " 00:00:00");

            if (claimStatus != 0) {
                cm.sendOk("[Error] #eRedemption Failed#n\r\n\r\nYou have already claimed the Season 1 reward on this account.");
                rs.close();
                ps.close();
                cm.dispose();
                return;
            }

            if (createdDate.after(cutoffTimestamp)) {
                cm.sendOk("[Error] #eRedemption Failed#n\r\n\r\nThis account was created after the cutoff date (" + CUTOFF_DATE + ").\r\nOnly Season 1 accounts are eligible.");
                rs.close();
                ps.close();
                cm.dispose();
                return;
            }

            // --- ELIGIBLE ---

            // 1. Mark as claimed
            var updatePs = conn.prepareStatement("UPDATE accounts SET webadmin = 1 WHERE id = ?");
            updatePs.setInt(1, accId);
            updatePs.executeUpdate();
            updatePs.close();

            // 2. Give Fixed Rewards
            for (var i = 0; i < REDEMPTION_REWARDS.length; i++) {
                var itemID = REDEMPTION_REWARDS[i][0];
                var count = REDEMPTION_REWARDS[i][1];
                cm.gainItem(itemID, count);
            }

            // 3. Give Random Reward (1 item from pool)
            var randomIndex = Math.floor(Math.random() * RANDOM_REWARD_POOL.length);
            var randomItemID = RANDOM_REWARD_POOL[randomIndex];
            cm.gainItem(randomItemID, 1);

            cm.sendOk("[Success] #eRedemption Successful!#n\r\n\r\nThank you for playing Season 1!\r\nI have verified your account and granted your rewards (including a random hat!).");
        } else {
            cm.sendOk("Error: Could not locate account data.");
        }

        rs.close();
        ps.close();

    } catch (e) {
        cm.sendOk("System Error during redemption: " + e);
        print("Redemption Error: " + e);
    }

    cm.dispose();
}

// ==========================================================
// FEATURE 2: CREATOR SHOP
// ==========================================================
function handleCreatorShop(selection, isInit) {
    var shopStep = status - 1;

    if (shopStep == 0) {
        var t = "#e[ Creator Shop Earnings Board ]#n\r\n";
        for (var i = 0; i < CREATOR_SHOPS.length; i++) {
            t += "#L" + i + "#View #b" + CREATOR_SHOPS[i].name + "#k#l\r\n";
        }
        cm.sendSimple(t);
    }

    else if (shopStep == 1) {
        selectedShop = CREATOR_SHOPS[selection];
        if (!selectedShop) { cm.dispose(); return; }

        if (!selectedShop.password || selectedShop.password.length === 0) {
            cm.sendOk("[Error] This shop is not configured.");
            cm.dispose();
            return;
        }
        cm.sendGetText("Enter password for #b" + selectedShop.name + "#k:");
    }

    else if (shopStep == 2) {
        shopPassword = cm.getText();
        if (shopPassword !== selectedShop.password) {
            cm.sendOk("[Error] Wrong password.");
            cm.dispose();
            return;
        }

        var tot = CreatorShopManager.getUnclaimedTotalByCurrency(selectedShop.npcId, CURRENCY);
        if (tot <= 0) {
            cm.sendOk("No unclaimed earnings for " + selectedShop.name + ".");
            cm.dispose();
            return;
        }

        shopReward = Math.floor(tot * selectedShop.share);

        cm.sendYesNo(
            "[Success] Password OK!\r\n\r\n" +
            "Total: #b" + fmt(tot) + "#k " + plural(tot) +
            "\r\nYour share (" + (selectedShop.share * 100) + "%): #b" + fmt(shopReward) +
            "#k " + plural(shopReward) +
            "\r\n\r\nClaim now?"
        );
    }

    else if (shopStep == 3) {
        if (shopReward <= 0) { cm.dispose(); return; }

        cm.gainItem(BCOIN_ID, shopReward);
        CreatorShopManager.markClaimed(selectedShop.npcId);

        cm.sendOk(
            "[Success] Claimed #b" + fmt(shopReward) + "#k " + plural(shopReward) +
            " from #b" + selectedShop.name + "#k!\r\nThank you!"
        );
        cm.dispose();
    }
}

function fmt(n) { return java.text.NumberFormat.getInstance().format(n); }
function plural(n) { return n + " B-Coin" + (n > 1 ? "s" : ""); }