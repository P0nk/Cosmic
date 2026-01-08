/* Monster Book Redemption NPC
    Status: Production / Light Debug
*/

// POLYFILL: Fallback for servers without native console.log
if (typeof console === 'undefined') {
    var console = {};
    console.log = function(msg) {
        java.lang.System.out.println(msg);
    };
}

var status = -1;
var validNormalQty = 0;
var validBossQty = 0;
var selectedStat = -1;
var selectedTierIsSpecial = false;
var quantityToRedeem = 0;

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
    if (mode == 1) {
        status++;
    } else {
        status--;
    }

    if (status == 0) {
        // --- STEP 1: CALCULATE AVAILABLE CARDS ---
        var player = cm.getPlayer();
        var mb = player.getMonsterBook();
        var cardMap = mb.getCards();
        var cardIds = cardMap.keySet().toArray();

        validNormalQty = 0;
        validBossQty = 0;

        for (var i = 0; i < cardIds.length; i++) {
            var cardId = cardIds[i];
            var level = cardMap.get(cardId);

            // Check if level 5 and NOT redeemed
            if (level >= 5 && !mb.isRedeemed(cardId)) {
                if (Math.floor(cardId / 1000) >= 2388) {
                    validBossQty++;
                } else {
                    validNormalQty++;
                }
            }
        }

        // Key Debug: Log initial state for auditing claims
        if (validNormalQty > 0 || validBossQty > 0) {
            console.log("[MB-Redeem] Player: " + player.getName() + " | Avail Normal: " + validNormalQty + " | Avail Boss: " + validBossQty);
        }

        if (validNormalQty == 0 && validBossQty == 0) {
            cm.sendOk("You do not have any Monster Book cards ready for redemption.\r\n\r\n#eRequirements:#n\r\n#b- Collect 5/5 of a card.\r\n- Card must not have been redeemed previously.#k");
            cm.dispose();
            return;
        }

        var text = "#e[Monster Book Redemption]#n\r\n";
        text += "You have #b" + validNormalQty + " Completed Normal Cards#k and #r" + validBossQty + " Completed Boss Cards#k.\r\n";
        text += "Choose which Stat to redeem:\r\n#b";
        text += "\r\n#L0# Weapon Attack (Norm: +2, Boss: +20)#l";
        text += "\r\n#L1# Magic Attack (Norm: +1, Boss: +10)#l";
        text += "\r\n#L2# Accuracy (Norm: +2, Boss: +20)#l";
        text += "\r\n#L3# Magic Defense (Norm: +5, Boss: +50)#l";
        text += "\r\n#L4# Weapon Defense (Norm: +5, Boss: +50)#l";
        text += "\r\n#L5# Avoidability (Norm: +3, Boss: +30)#l";

        cm.sendSimple(text);

    } else if (status == 1) {
        // --- STEP 2: SELECT NORMAL OR BOSS ---
        selectedStat = selection;

        var text = "You selected a stat. Now choose which Card Tier to consume:\r\n#b";
        var optionsAvailable = false;

        if (validNormalQty > 0) {
            text += "\r\n#L0# Normal Cards (Available: " + validNormalQty + ")#l";
            optionsAvailable = true;
        }
        if (validBossQty > 0) {
            text += "\r\n#L1# Boss Cards (Available: " + validBossQty + ")#l";
            optionsAvailable = true;
        }

        if (!optionsAvailable) {
            cm.sendOk("Error: No cards available.");
            cm.dispose();
            return;
        }

        cm.sendSimple(text);

    } else if (status == 2) {
        // --- STEP 3: SELECT QUANTITY ---
        if (selection == 0) {
             selectedTierIsSpecial = false; // Normal
        } else if (selection == 1) {
             selectedTierIsSpecial = true; // Boss
        } else {
             cm.dispose();
             return;
        }

        var maxAvailable = selectedTierIsSpecial ? validBossQty : validNormalQty;

        if (maxAvailable <= 0) {
             cm.dispose();
             return;
        }

        var text = "How many " + (selectedTierIsSpecial ? "Boss" : "Normal") + " cards would you like to redeem? (Max: " + maxAvailable + ")";
        cm.sendGetNumber(text, maxAvailable, 1, maxAvailable);

    } else if (status == 3) {
        // --- STEP 4: EXECUTE ---
        quantityToRedeem = selection;

        if (quantityToRedeem <= 0) {
            cm.dispose();
            return;
        }

        var player = cm.getPlayer();
        var mb = player.getMonsterBook();

        // Execute Java method
        var success = mb.redeemBulk(cm.getClient(), selectedStat, selectedTierIsSpecial, quantityToRedeem);

        // Key Debug: Log the final transaction result
        console.log("[MB-Redeem] Player: " + player.getName() + " | StatID: " + selectedStat + " | BossTier: " + selectedTierIsSpecial + " | Qty: " + quantityToRedeem + " | Success: " + success);

        if (success) {
            cm.sendOk("Success! Redeemed " + quantityToRedeem + " cards. Stats have been updated permanently.");
        } else {
            cm.sendOk("Transaction failed. An error occurred in the backend.");
        }

    } else if (status == 4) {
        // --- STEP 5: CLEANUP ---
        cm.dispose();
    }
}