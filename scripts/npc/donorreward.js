/*
 * donorreward.js — Donor Rewards UI (Dashboard + Flat Shop + History)
 * v83 safe: no emoji/bullets that become '?'
 * Updated to support Item Duration, Maple Leaves Bundles, World Buff, and Icons
 */

var DonorCreditManager = Java.type("server.donor.DonorCreditManager");
var SubscriptionManager = Java.type("server.subscription.SubscriptionManager");

var status = -1;
var page = "HOME";
var pendingJump = null;

// flat shop state
var allItems = [];
var selectedRow = null;
var selectedTimes = 1;

// Items that trigger the "How many do you want?" prompt.
var CURRENCY_IDS = {
    "3020001": true, // Dinosaur Cash
    "3020002": true, // Other Coin
    "4001126": true  // Maple Leaf
};

// ============================================
// CONFIG: Custom Items (Leaves & Buff)
// ============================================
var customItems = [
    {
        id: "cust_leaves",
        type: "ITEM",
        name: "Maple Leaves", // Icon #i4001126# will be added automatically in code
        itemid: 4001126,
        qty: 5000,       // 1 Bundle = 1000
        priceCents: 350, // 1 DC
        category: "Special",
        desc: "Get 1000 Maple Leaves instantly."
    },
    {
        id: "cust_buff",
        type: "BUFF",
        name: "Activate World Buff", // Icon #i2022179# will be added automatically in code
        itemid: 2022179, // Onyx Apple Icon
        qty: 1,
        priceCents: 350, // 5 DC
        category: "Special",
        desc: "Cast GM Buffs on everyone & announce name!"
    }
];
// ============================================

function start() {
    status = -1;
    page = "HOME";
    pendingJump = null;

    allItems = [];
    selectedRow = null;
    selectedTimes = 1;

    action(1, 0, 0);
}

function action(m, t, sel) {
    if (m != 1) {
        cm.dispose();
        return;
    }

    if (pendingJump != null) {
        page = pendingJump;
        pendingJump = null;
        status = -1;
    }

    status++;

    try {
        if (page == "HOME") return homeFlow(sel);
        if (page == "SHOP") return shopFlow(sel);
        if (page == "QTY") return qtyFlow(sel);
        if (page == "CONFIRM") return confirmFlow(sel);
        if (page == "HISTORY") return historyFlow(sel);
        if (page == "SUBSCRIBE") return subscribeFlow(sel);
        if (page == "STAT_ALLOC") return statAllocFlow(sel);
        cm.dispose();
    } catch (e) {
        cm.sendOk("Error:\r\n\r\n" + e);
        cm.dispose();
    }
}

/* ============ helpers ============ */

function fmtCents(cents) {
    var v = Number(cents);
    if (isNaN(v)) v = 0;
    var neg = v < 0;
    v = Math.abs(v);
    var dollars = Math.floor(v / 100);
    var rem = v % 100;
    return (neg ? "-" : "") + dollars + "." + (rem < 10 ? "0" + rem : rem);
}

function safeStr(x) { return x == null ? "" : String(x); }

function getStatusObj() {
    return DonorCreditManager.getStatusByAccountId(cm.getClient().getAccID());
}

function getBalanceCents() {
    return Number(getStatusObj().get("balanceCents"));
}

function isCurrency(itemId) {
    return !!CURRENCY_IDS[String(itemId)];
}

function jump(p) {
    page = p;
    status = -1;
    action(1, 0, 0);
}

function itemLine(itemId) {
    // Shows Icon + WZ Name (e.g., #i4001126# #t4001126#)
    return "#i" + itemId + "# #t" + itemId + "#";
}

/* ============ HOME ============ */

function homeFlow(sel) {
    if (status == 0) {
        var st = getStatusObj();
        var txt = "#e[ Donor Rewards ]#n\r\n\r\n";
        txt += "Donor Credits (DC): #b" + fmtCents(st.get("balanceCents")) + "#k\r\n";
        txt += "Lifetime Donated: #b$" + fmtCents(st.get("lifetimeCents")) + " SGD#k\r\n";
        txt += "Total Earned: #b" + fmtCents(st.get("earnedCents")) + " DC#k\r\n";
        txt += "Total Spent: #b" + fmtCents(st.get("spentCents")) + " DC#k\r\n\r\n";

        txt += "#eMilestones#n\r\n";
        txt += "Achieved: #b" + st.get("milestones") + "#k\r\n";
        txt += "Next bonus in: #b$" + fmtCents(st.get("toNextMilestoneCents")) + " SGD#k (Bonus: #b+" + fmtCents(st.get("milestoneBonusCentsPer")) + " DC#k)\r\n\r\n";

        txt += "#eRules#n\r\n";
        txt += "- 1 SGD = 1 DC (supports cents)\r\n";
        txt += "- Bonus: +10.00 DC every $50 lifetime donated\r\n";
        txt += "- Spend DC in the Donor Shop\r\n\r\n";

        var lastCredit = safeStr(st.get("lastCreditAt"));
        var lastSpend = safeStr(st.get("lastSpendAt"));
        if (lastCredit.length > 0) txt += "Last credited: #b" + lastCredit + "#k\r\n";
        if (lastSpend.length > 0) txt += "Last spent:   #b" + lastSpend + "#k\r\n";

        txt += "\r\n#L0#Donor Shop#l\r\n";
        txt += "#L1#Transaction History#l\r\n";
        txt += "#L2#Subscription Benefits#l\r\n";
        txt += "#L3#Stat Allocation#l\r\n";
        txt += "#L4#Exit#l\r\n";

        cm.sendSimple(txt);
        return;
    }

    if (sel == 0) return jump("SHOP");
    if (sel == 1) return jump("HISTORY");
    if (sel == 2) return jump("SUBSCRIBE");
    if (sel == 3) return jump("STAT_ALLOC");
    cm.dispose(); // sel == 4 = Exit
}

/* ============ SHOP ============ */

function buildFlatShopList() {
    allItems = [];
    // 1. Inject Custom Items
    for (var k in customItems) {
        var ci = customItems[k];
        allItems.push({
            isCustom: true,
            id: ci.id,
            type: ci.type,
            name: ci.name,
            itemid: ci.itemid,
            qty: ci.qty,
            priceCents: ci.priceCents,
            category: ci.category,
            desc: ci.desc,
            period: 0
        });
    }
    // 2. Fetch DB Items
    var cats = DonorCreditManager.getCategories();
    if (cats != null && cats.size() > 0) {
        for (var i = 0; i < cats.size(); i++) {
            var cat = String(cats.get(i));
            var list = DonorCreditManager.getItemsByCategory(cat);
            if (list == null || list.size() == 0) continue;
            for (var j = 0; j < list.size(); j++) {
                var row = list.get(j);
                allItems.push({
                    isCustom: false,
                    id: row.get("id"),
                    itemid: Number(row.get("itemid")),
                    qty: Number(row.get("qty")),
                    priceCents: Number(row.get("priceCents")),
                    category: cat,
                    stock: row.get("stock"),
                    period: row.get("period")
                });
            }
        }
    }
}

function shopFlow(sel) {
    if (status == 0) {
        buildFlatShopList();
        if (allItems.length == 0) {
            cm.sendOk("Donor shop is currently empty.\r\nAsk an admin to add items.");
            cm.dispose();
            return;
        }

        var bal = getBalanceCents();
        var txt = "#e[ Donor Shop ]#n\r\n";
        txt += "Balance: #b" + fmtCents(bal) + " DC#k\r\n\r\n";

        var lastCat = null;
        for (var k = 0; k < allItems.length; k++) {
            var r = allItems[k];
            var cat = r.category;
            if (lastCat == null || cat != lastCat) {
                txt += "\r\n#e[ " + cat + " ]#n\r\n";
                lastCat = cat;
            }

            // --- Display Name Logic (UPDATED) ---
            var dName = "";
            if (r.isCustom) {
                // Manually add Icon + Custom Name
                // e.g. #i4001126# 1000 Maple Leaves
                dName = "#i" + r.itemid + "# " + r.name;
            } else {
                // Standard: Icon + WZ Name
                dName = itemLine(r.itemid);
            }
            // ------------------------------------

            var durTxt = "";
            if (r.period > 0) durTxt = " #b(" + r.period + " Days)#k";
            else if (!r.isCustom) durTxt = " #b(Perm)#k";

            txt += "#L" + k + "#" + dName;

            if (r.isCustom && r.type == "BUFF") {
                txt += " - #r" + fmtCents(r.priceCents) + " DC#k";
            } else {
                txt += "  x" + r.qty + "  -  #r" + fmtCents(r.priceCents) + " DC#k";
            }

            txt += durTxt;

            if (isCurrency(r.itemid)) {
                var maxAffordable = Math.floor(bal / r.priceCents);
                if (maxAffordable < 0) maxAffordable = 0;
                txt += "  #d(Max: " + maxAffordable + ")#k";
            }
            if (r.stock != null) txt += "  #dStock: " + r.stock + "#k";
            txt += "#l\r\n";
        }

        txt += "\r\n#L999#Back#l";
        cm.sendSimple(txt);
        return;
    }

    if (sel == 999) return jump("HOME");
    if (sel < 0 || sel >= allItems.length) { cm.dispose(); return; }

    selectedRow = allItems[sel];
    selectedTimes = 1;

    // Buffs skip qty select
    if (selectedRow.isCustom && selectedRow.type == "BUFF") return jump("CONFIRM");
    // Items in CURRENCY_IDS (Leaves + Coins) go to QTY
    if (isCurrency(selectedRow.itemid)) return jump("QTY");

    return jump("CONFIRM");
}

/* ============ QTY (Prompt for Bundles/Quantity) ============ */

function qtyFlow(sel) {
    if (status == 0) {
        if (selectedRow == null) return jump("SHOP");

        var max = 100; // Cap to prevent math overflow
        var price = selectedRow.priceCents;
        var bal = getBalanceCents();

        var affordable = Math.floor(bal / price);
        if (selectedRow.stock != null) max = Math.min(max, selectedRow.stock);
        max = Math.min(max, affordable);

        if (max < 1) {
            cm.sendOk("You do not have enough DC for this.");
            pendingJump = "SHOP";
            return;
        }
        if (max > 10000) max = 10000;

        // Custom Text for Maple Leaves
        var txt = "#eQuantity Selection#n\r\n\r\n";

        if (selectedRow.id === "cust_leaves") {
            // Specific text for Bundles
            // Uses #i# to show icon in the prompt too
            txt += "Item: #i" + selectedRow.itemid + "# (Maple Leaves)\r\n";
            txt += "One Bundle contains: #b" + selectedRow.qty + " Leaves#k\r\n";
            txt += "Price per Bundle: #r" + fmtCents(price) + " DC#k\r\n";
            txt += "\r\nHow many #bBUNDLES#k would you like to buy? (1 - " + max + "):";
        } else {
            // Default text
            txt += "Item: " + itemLine(selectedRow.itemid) + "\r\n";
            txt += "Per purchase gives: #b" + selectedRow.qty + "#k\r\n";
            txt += "Price each: #r" + fmtCents(price) + " DC#k\r\n";
            txt += "\r\nEnter how many you want to buy (1 - " + max + "):";
        }

        cm.sendGetNumber(txt, 1, 1, max);
        return;
    }

    selectedTimes = sel;
    return jump("CONFIRM");
}

/* ============ CONFIRM ============ */

function confirmFlow(sel) {
    if (status == 0) {
        if (selectedRow == null) return jump("SHOP");

        var r = selectedRow;
        var times = (selectedTimes <= 0 ? 1 : selectedTimes);
        var totalCost = r.priceCents * times;
        var bal = getBalanceCents();
        var after = bal - totalCost;

        if (after < 0) {
            cm.sendOk("You do not have enough Donor Credits.");
            pendingJump = "SHOP";
            return;
        }

        var txt = "#eConfirm Purchase#n\r\n\r\n";

        // Show icon in confirmation too for consistency
        var confirmName = "";
        if (r.isCustom) {
            confirmName = "#i" + r.itemid + "# " + r.name;
        } else {
            confirmName = itemLine(r.itemid);
        }

        if (r.isCustom && r.type == "BUFF") {
            txt += "Action: " + confirmName + "\r\n";
            txt += "Desc: " + r.desc + "\r\n";
        } else {
            txt += "Item: " + confirmName + "\r\n";
            if (r.id === "cust_leaves") {
                txt += "Bundles: #b" + times + "#k\r\n";
                txt += "Total Leaves: #b" + (r.qty * times) + "#k\r\n";
            } else {
                txt += "Times: #b" + times + "#k\r\n";
                txt += "Total Quantity: #b" + (r.qty * times) + "#k\r\n";
            }
        }

        txt += "Total Cost: #r" + fmtCents(totalCost) + " DC#k\r\n";
        txt += "Balance After: #b" + fmtCents(after) + " DC#k\r\n\r\n";
        txt += "#L0#Buy#l\r\n";
        txt += "#L1#Cancel#l";

        cm.sendSimple(txt);
        return;
    }

    if (sel == 1) return jump("SHOP"); // Cancelled

    if (selectedRow == null) {
        cm.sendOk("Session expired.");
        pendingJump = "SHOP";
        return;
    }

    var r = selectedRow;
    var times = (selectedTimes <= 0 ? 1 : selectedTimes);
    var totalCost = r.priceCents * times;
    var totalQty = r.qty * times;

    // --- A. CUSTOM ITEMS ---
    if (r.isCustom) {
        if (DonorCreditManager.deductFunds(cm.getClient().getAccID(), totalCost, "CUSTOM:" + r.id)) {
            if (r.type == "ITEM") {
                cm.gainItem(r.itemid, totalQty);
                cm.sendOk("Purchase complete!\r\nReceived: " + totalQty + " " + r.name);
            } else if (r.type == "BUFF") {
                var player = cm.getPlayer();
                DonorCreditManager.applyWorldBuff(player.getWorld(), player.getName());
                cm.sendOk("World Buff Activated and Announced!");
            }
        } else {
            cm.sendOk("Transaction failed. Insufficient funds.");
        }
        cm.dispose();
        return;
    }

    // --- B. DB ITEMS ---
    try {
        var res = DonorCreditManager.buyFromShop(cm.getClient(), Number(r.id), times);
        var msg = "Purchase complete!\r\n\r\n";
        msg += "You received: " + itemLine(res.itemId) + " x" + res.qty + "\r\n";
        msg += "Cost: " + fmtCents(res.priceCents) + " DC\r\n";
        msg += "New Balance: " + fmtCents(res.newBalanceCents) + " DC\r\n\r\n";
        msg += "Type @donorreward to open again.";
        cm.sendOk(msg);
        cm.dispose();
    } catch (e) {
        var err = (e && e.getMessage) ? e.getMessage() : ("" + e);
        cm.sendOk("Purchase failed:\r\n\r\n" + err);
        pendingJump = "SHOP";
        return;
    }
}

/* ============ HISTORY ============ */

function historyFlow(sel) {
    if (status == 0) {
        var list = DonorCreditManager.getRecentTxns(cm.getClient().getAccID(), 10);
        var txt = "#e[ Donor Transaction History ]#n\r\n\r\n";
        if (list == null || list.size() == 0) {
            txt += "No transactions yet.";
            cm.sendOk(txt);
            cm.dispose();
            return;
        }
        for (var i = 0; i < list.size(); i++) {
            var r = list.get(i);
            var delta = Number(r.get("dcDeltaCents"));
            txt += safeStr(r.get("at")) + "\r\n";
            txt += "Type: " + safeStr(r.get("type")) + "   DC: " + (delta >= 0 ? "+" : "-") + fmtCents(Math.abs(delta)) + "\r\n";
            if (safeStr(r.get("ref")).length > 0) txt += "Ref: " + safeStr(r.get("ref")) + "\r\n";
            txt += "-----------------------------\r\n";
        }
        cm.sendOk(txt);
        cm.dispose();
        return;
    }
    cm.dispose();
}

/* ============ SUBSCRIBE ============ */

function subscribeFlow(sel) {
    var chr = cm.getPlayer();
    var info = SubscriptionManager.getInfo(chr.getId());

    if (status == 0) {
        var bal = getBalanceCents();
        var txt = "#e[ Subscription Benefits ]#n\r\n\r\n";

        if (info != null) {
            var active = info.get("active");
            txt += "Status: " + (active ? "#bACTIVE#k" : "#rEXPIRED#k") + "\r\n";
            txt += "Tier: #b" + safeStr(info.get("tier")) + "#k\r\n";
            txt += "Expires: #b" + safeStr(info.get("expiresAt")).substring(0, 10) + "#k (" + info.get("daysLeft") + " days left)\r\n";
            txt += "Unspent Stat Points: #b" + info.get("unspentPoints") + "#k\r\n";
            txt += "Total Stat Points Earned: #b" + info.get("totalPoints") + "#k\r\n\r\n";
        } else {
            txt += "You do not have an active subscription.\r\n\r\n";
        }

        txt += "#ePerks (while active):#n\r\n";
        txt += "- 1.5x EXP from all sources\r\n";
        txt += "- 1.5x Meso from monster drops\r\n";
        txt += "- +5 permanent stat points per month purchased\r\n";
        txt += "  (STR / DEX / INT / LUK / Speed / Jump - not WATK/MATK)\r\n\r\n";
        txt += "Balance: #b" + fmtCents(bal) + " DC#k\r\n\r\n";
        txt += "#L0#Buy 1 Month  (#r5.00 DC#k)#l\r\n";
        txt += "#L1#Buy 1 Year   (#r50.00 DC#k - 2 months free!)#l\r\n";

        if (info != null && Number(info.get("unspentPoints")) > 0) {
            txt += "#L2#Allocate Stat Points (" + info.get("unspentPoints") + " available)#l\r\n";
        }
        txt += "#L999#Back#l";
        cm.sendSimple(txt);
        return;
    }

    if (sel == 999) return jump("HOME");
    if (sel == 2) return jump("STAT_ALLOC");

    var tier = (sel == 1) ? "ANNUAL" : "MONTHLY";
    var result = SubscriptionManager.subscribe(cm.getClient().getAccID(), chr.getId(), tier);
    if (result.success) {
        cm.sendOk("#eSubscription activated!#n\r\n\r\n" + result.message);
    } else {
        cm.sendOk("#eFailed:#n " + result.message);
    }
    cm.dispose();
}

/* ============ STAT ALLOC ============ */

var statAllocChoice = null;

function statAllocFlow(sel) {
    var chr = cm.getPlayer();
    var info = SubscriptionManager.getInfo(chr.getId());
    var available = (info != null) ? Number(info.get("unspentPoints")) : 0;

    if (status == 0) {
        if (available <= 0) {
            cm.sendOk("You have no unspent stat points.\r\nPurchase more subscription months to earn more!");
            cm.dispose();
            return;
        }

        var txt = "#e[ Allocate Subscriber Stat (" + available + " left) ]#n\r\n\r\n";
        txt += "Choose a stat to increase:\r\n\r\n";
        txt += "#L0#STR#l\r\n";
        txt += "#L1#DEX#l\r\n";
        txt += "#L2#INT#l\r\n";
        txt += "#L3#LUK#l\r\n";
        txt += "#L4#Speed#l\r\n";
        txt += "#L5#Jump#l\r\n";
        txt += "#L999#Back#l";
        cm.sendSimple(txt);
        return;
    }

    if (status == 1) {
        if (sel == 999) return jump("SUBSCRIBE");

        var statNames = ["str", "dex", "int", "luk", "speed", "jump"];
        if (sel < 0 || sel >= statNames.length) { cm.dispose(); return; }

        statAllocChoice = statNames[sel];

        cm.sendGetNumber("How many points would you like to add to #b" + statAllocChoice.toUpperCase() + "#k?\r\n(Available: " + available + ")", 1, 1, available);
        return;
    }

    if (status == 2) {
        var amountRaw = sel;
        if (amountRaw <= 0 || amountRaw > available) {
            cm.sendOk("Invalid amount entered.");
            pendingJump = "STAT_ALLOC";
            return;
        }

        var result = SubscriptionManager.allocateStat(chr, statAllocChoice, amountRaw);
        if (result.success) {
            cm.sendNext("#eApplied!#n\r\n" + result.message + "\r\n\r\nNote: Re-equip your Medal to see the stats update immediately!");
            pendingJump = "STAT_ALLOC";
        } else {
            cm.sendOk("#eFailed:#n " + result.message);
            cm.dispose();
        }
    }
}