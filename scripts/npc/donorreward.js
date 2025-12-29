/*
 * donorreward.js — Donor Rewards UI (Dashboard + Flat Shop + History)
 * v83 safe: no emoji/bullets that become '?'
 * Fixes:
 *  - Restores detailed HOME (milestones/rules/last credit/spend)
 *  - Restores SHOP category headers + currency max-buy info
 *  - Prevents crashes on insufficient DC (never calls Java)
 *  - Fixes "Purchase Failed" infinite loop via pendingJump
 */

var DonorCreditManager = Java.type("server.donor.DonorCreditManager");

var status = -1;
var page = "HOME";
var pendingJump = null;

// flat shop state
var allItems = [];
var selectedRow = null;
var selectedTimes = 1;

// Only currency items get quantity prompt (NXT, BCoin)
var CURRENCY_IDS = { "3020001": true, "3020002": true };

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

    // If we previously showed sendOk + set pendingJump,
    // the NEXT click should land on the target page cleanly.
    if (pendingJump != null) {
        page = pendingJump;
        pendingJump = null;
        status = -1; // reset state machine for new page
    }

    status++;

    try {
        if (page == "HOME") return homeFlow(sel);
        if (page == "SHOP") return shopFlow(sel);
        if (page == "QTY") return qtyFlow(sel);
        if (page == "CONFIRM") return confirmFlow(sel);
        if (page == "HISTORY") return historyFlow(sel);
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
    return "#i" + itemId + "# #t" + itemId + "#";
}

/* ============ HOME ============ */

function homeFlow(sel) {
    if (status == 0) {
        var st = getStatusObj();

        var lifetime = st.get("lifetimeCents");
        var bal = st.get("balanceCents");
        var earned = st.get("earnedCents");
        var spent = st.get("spentCents");
        var milestones = st.get("milestones");
        var toNext = st.get("toNextMilestoneCents");
        var bonusPer = st.get("milestoneBonusCentsPer");
        var lastCredit = safeStr(st.get("lastCreditAt"));
        var lastSpend = safeStr(st.get("lastSpendAt"));

        var txt = "#e[ Donor Rewards ]#n\r\n\r\n";
        txt += "Donor Credits (DC): #b" + fmtCents(bal) + "#k\r\n";
        txt += "Lifetime Donated: #b$" + fmtCents(lifetime) + " SGD#k\r\n";
        txt += "Total Earned: #b" + fmtCents(earned) + " DC#k\r\n";
        txt += "Total Spent: #b" + fmtCents(spent) + " DC#k\r\n\r\n";

        txt += "#eMilestones#n\r\n";
        txt += "Achieved: #b" + milestones + "#k\r\n";
        txt += "Next bonus in: #b$" + fmtCents(toNext) + " SGD#k (Bonus: #b+" + fmtCents(bonusPer) + " DC#k)\r\n\r\n";

        txt += "#eRules#n\r\n";
        txt += "- 1 SGD = 1 DC (supports cents)\r\n";
        txt += "- Bonus: +10.00 DC every $50 lifetime donated\r\n";
        txt += "- Spend DC in the Donor Shop\r\n\r\n";

        if (lastCredit.length > 0) txt += "Last credited: #b" + lastCredit + "#k\r\n";
        if (lastSpend.length > 0) txt += "Last spent:   #b" + lastSpend + "#k\r\n";

        txt += "\r\n#L0#Donor Shop#l\r\n";
        txt += "#L1#Transaction History#l\r\n";
        txt += "#L2#Exit#l\r\n";

        cm.sendSimple(txt);
        return;
    }

    if (sel == 0) return jump("SHOP");
    if (sel == 1) return jump("HISTORY");
    cm.dispose();
}

/* ============ SHOP (flat list) ============ */

function buildFlatShopList() {
    allItems = [];

    var cats = DonorCreditManager.getCategories();
    if (cats == null || cats.size() == 0) return;

    for (var i = 0; i < cats.size(); i++) {
        var cat = String(cats.get(i));
        var list = DonorCreditManager.getItemsByCategory(cat);
        if (list == null || list.size() == 0) continue;

        for (var j = 0; j < list.size(); j++) {
            var row = list.get(j);
            row.put("category", cat);
            allItems.push(row);
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
            var cat = safeStr(r.get("category"));

            if (lastCat == null || cat != lastCat) {
                txt += "\r\n#e[ " + cat + " ]#n\r\n";
                lastCat = cat;
            }

            var itemId = Number(r.get("itemid"));
            var qty = Number(r.get("qty"));
            var price = Number(r.get("priceCents"));
            var stock = r.get("stock"); // may be null

            if (isNaN(price) || price <= 0) {
                // bad data safety
                txt += "#d" + itemLine(itemId) + "  x" + qty + "  -  INVALID PRICE#k\r\n";
                continue;
            }

            txt += "#L" + k + "#";
            txt += itemLine(itemId);
            txt += "  x" + qty + "  -  #r" + fmtCents(price) + " DC#k";

            // show max buy hint for currencies (like before)
            if (isCurrency(itemId)) {
                var maxAffordable = Math.floor(bal / price);
                if (maxAffordable < 0) maxAffordable = 0;
                txt += "  #d(Max: " + maxAffordable + ")#k";
            }

            if (stock != null) txt += "  #dStock: " + stock + "#k";
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

    var itemId2 = Number(selectedRow.get("itemid"));
    if (isCurrency(itemId2)) return jump("QTY");
    return jump("CONFIRM");
}

/* ============ QTY (currency only) ============ */

function qtyFlow(sel) {
    if (status == 0) {
        if (selectedRow == null) return jump("SHOP");

        var itemId = Number(selectedRow.get("itemid"));
        var baseQty = Number(selectedRow.get("qty"));
        var price = Number(selectedRow.get("priceCents"));
        var stock = selectedRow.get("stock");

        if (isNaN(price) || price <= 0) {
            cm.sendOk("This item has an invalid price.\r\nPlease inform an admin.");
            pendingJump = "SHOP";
            return;
        }

        var bal = getBalanceCents();
        var maxAffordable = Math.floor(bal / price);
        if (maxAffordable < 1) {
            cm.sendOk("You do not have enough DC for this.");
            pendingJump = "SHOP";
            return;
        }

        var maxByStock = (stock == null) ? maxAffordable : Math.min(maxAffordable, Number(stock));
        if (maxByStock < 1) {
            cm.sendOk("Out of stock.");
            pendingJump = "SHOP";
            return;
        }

        if (maxByStock > 10000) maxByStock = 10000;

        var txt = "#eQuantity#n\r\n\r\n";
        txt += "Item: " + itemLine(itemId) + "\r\n";
        txt += "Per purchase gives: #b" + baseQty + "#k\r\n";
        txt += "Price each: #r" + fmtCents(price) + " DC#k\r\n";
        txt += "\r\nEnter how many you want to buy (1 - " + maxByStock + "):";

        cm.sendGetNumber(txt, 1, 1, maxByStock);
        return;
    }

    selectedTimes = sel;
    return jump("CONFIRM");
}

/* ============ CONFIRM ============ */

function confirmFlow(sel) {
    if (status == 0) {
        if (selectedRow == null) return jump("SHOP");

        var itemId = Number(selectedRow.get("itemid"));
        var baseQty = Number(selectedRow.get("qty"));
        var priceEach = Number(selectedRow.get("priceCents"));
        var stock = selectedRow.get("stock");
        var times = (selectedTimes <= 0 ? 1 : selectedTimes);

        if (isNaN(priceEach) || priceEach <= 0) {
            cm.sendOk("This shop item has an invalid price.\r\nPlease inform an admin.");
            pendingJump = "SHOP";
            return;
        }

        var totalQty = baseQty * times;
        var totalPrice = priceEach * times;

        if (isNaN(totalPrice) || totalPrice <= 0) {
            cm.sendOk("This purchase amount is invalid.\r\nPlease inform an admin.");
            pendingJump = "SHOP";
            return;
        }

        var bal = getBalanceCents();
        var after = bal - totalPrice;

        // HARD BLOCK: never show Buy screen if insufficient
        if (after < 0) {
            cm.sendOk(
                "You do not have enough Donor Credits.\r\n\r\n" +
                "Required: " + fmtCents(totalPrice) + " DC\r\n" +
                "Your Balance: " + fmtCents(bal) + " DC"
            );
            pendingJump = "SHOP";
            return;
        }

        var txt = "#eConfirm Purchase#n\r\n\r\n";
        txt += "Item: " + itemLine(itemId) + "\r\n";
        txt += "Times: #b" + times + "#k\r\n";
        txt += "Total Quantity: #b" + totalQty + "#k\r\n";
        txt += "Total Cost: #r" + fmtCents(totalPrice) + " DC#k\r\n";
        if (stock != null) txt += "Stock: #b" + stock + "#k\r\n";
        txt += "\r\nBalance: #b" + fmtCents(bal) + " DC#k\r\n";
        txt += "After: #b" + fmtCents(after) + " DC#k\r\n\r\n";
        txt += "#L0#Buy#l\r\n";
        txt += "#L1#Cancel#l";

        cm.sendSimple(txt);
        return;
    }

    // status == 1 (player clicked Buy/Cancel)
    if (sel == 1) return jump("SHOP");

    if (selectedRow == null) {
        cm.sendOk("Session expired. Please try again.");
        pendingJump = "SHOP";
        return;
    }

    // final re-check before calling Java (race safe)
    var bal2 = getBalanceCents();
    var priceEach2 = Number(selectedRow.get("priceCents"));
    var times2 = (selectedTimes <= 0 ? 1 : selectedTimes);
    var totalPrice2 = priceEach2 * times2;

    if (isNaN(totalPrice2) || totalPrice2 <= 0) {
        cm.sendOk("Invalid purchase.");
        pendingJump = "SHOP";
        return;
    }

    if (bal2 < totalPrice2) {
        cm.sendOk("You no longer have enough Donor Credits.");
        pendingJump = "SHOP";
        return;
    }

    try {
        var shopItemId = Number(selectedRow.get("id"));
        var res = DonorCreditManager.buyFromShop(cm.getClient(), shopItemId, times2);

        var msg = "Purchase complete!\r\n\r\n";
        msg += "You received: " + itemLine(res.itemId) + " x" + res.qty + "\r\n";
        msg += "Cost: " + fmtCents(res.priceCents) + " DC\r\n";
        msg += "New Balance: " + fmtCents(res.newBalanceCents) + " DC\r\n\r\n";
        msg += "Type @donorreward to open again.";

        cm.sendOk(msg);
        cm.dispose();
    } catch (e) {
        // IMPORTANT: do NOT stay on CONFIRM page; jump back after OK
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
            var at = safeStr(r.get("at"));
            var type = safeStr(r.get("type"));
            var delta = Number(r.get("dcDeltaCents"));
            var ref = safeStr(r.get("ref"));

            txt += at + "\r\n";
            txt += "Type: " + type + "   DC: " + (delta >= 0 ? "+" : "-") + fmtCents(Math.abs(delta)) + "\r\n";
            if (ref.length > 0) txt += "Ref: " + ref + "\r\n";
            txt += "-----------------------------\r\n";
        }

        cm.sendOk(txt);
        cm.dispose();
        return;
    }

    cm.dispose();
}
