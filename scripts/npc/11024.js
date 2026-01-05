/* Monster Book Redemption NPC
    Debug Mode: ON
*/

// POLYFILL: If the server engine doesn't support console.log natively, fallback to System.out
if (typeof console === 'undefined') {
    var console = {};
    console.log = function(msg) {
        java.lang.System.out.println(msg);
    };
}

var status = -1;
// Debug: Declare variables globally for tracking
var validNormalQty = 0;
var validBossQty = 0;
var selectedStat = -1;
var selectedTierIsSpecial = false;
var quantityToRedeem = 0;

function start() {
    console.log("[Debug] Function start() called.");
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    console.log("[Debug] action() called. Mode: " + mode + ", Status: " + status + ", Selection: " + selection);

    if (mode == -1) {
        console.log("[Debug] Mode is -1, disposing.");
        cm.dispose();
        return;
    }
    if (mode == 0 && status == 0) {
        console.log("[Debug] Mode 0 & Status 0, disposing.");
        cm.dispose();
        return;
    }
    if (mode == 1) {
        status++;
    } else {
        status--;
    }

    console.log("[Debug] New Status: " + status);

    if (status == 0) {
        // --- STEP 1: CALCULATE AVAILABLE CARDS ---

        console.log("[Debug] getting player instance...");
        var player = cm.getPlayer();
        console.log("[Debug] getting MonsterBook instance...");
        var mb = player.getMonsterBook();

        // Reset counters
        validNormalQty = 0;
        validBossQty = 0;

        console.log("[Debug] Fetching card map keys...");
        var cardMap = mb.getCards();
        // Convert keys to array to avoid Iterator crashes in JS
        var cardIds = cardMap.keySet().toArray();

        console.log("[Debug] Looping through " + cardIds.length + " cards.");

        for (var i = 0; i < cardIds.length; i++) {
            var cardId = cardIds[i];
            var level = cardMap.get(cardId);

            // Check if level 5 and NOT redeemed
            if (level >= 5 && !mb.isRedeemed(cardId)) {
                // Check if Special (Boss)
                if (Math.floor(cardId / 1000) >= 2388) {
                    validBossQty++;
                } else {
                    validNormalQty++;
                }
            }
        }

        console.log("[Debug] Count Result - Normal: " + validNormalQty + ", Boss: " + validBossQty);

        if (validNormalQty == 0 && validBossQty == 0) {
            cm.sendOk("You do not have any Monster Book cards ready for redemption.\r\n\r\n#eRequirements:#n\r\n#b- Collect 5/5 of a card.\r\n- Card must not have been redeemed previously.#k");
            cm.dispose();
            return;
        }

        var text = "#e[Monster Book Redemption]#n\r\n";
        text += "You have #b" + validNormalQty + " Completed Normal Cards#k and #r" + validBossQty + " Completed Boss Cards#k.\r\n";
        text += "Choose which Stat to redeem:\r\n#b";

        // Selection 0-5 mapping to Java Stat Type
        text += "\r\n#L0# Weapon Attack (Norm: +2, Boss: +20)#l";
        text += "\r\n#L1# Magic Attack (Norm: +1, Boss: +10)#l";
        text += "\r\n#L2# Accuracy (Norm: +2, Boss: +20)#l";
        text += "\r\n#L3# Magic Defense (Norm: +5, Boss: +50)#l";
        text += "\r\n#L4# Weapon Defense (Norm: +5, Boss: +50)#l";
        text += "\r\n#L5# Avoidability (Norm: +3, Boss: +30)#l";

        console.log("[Debug] Sending Simple text: " + text);
        cm.sendSimple(text);

    } else if (status == 1) {
        // --- STEP 2: SELECT NORMAL OR BOSS ---

        console.log("[Debug] User selected stat index: " + selection);
        selectedStat = selection;

        var text = "You selected a stat. Now choose which Card Tier to consume:\r\n#b";
        var optionsAvailable = false;

        // FIXED LOGIC: Hardcoding IDs 0 and 1 so the selection check in status 2 is always accurate
        if (validNormalQty > 0) {
            text += "\r\n#L0# Normal Cards (Available: " + validNormalQty + ")#l";
            optionsAvailable = true;
        }
        if (validBossQty > 0) {
            text += "\r\n#L1# Boss Cards (Available: " + validBossQty + ")#l";
            optionsAvailable = true;
        }

        if (!optionsAvailable) {
            console.log("[Debug] No options available (logic error or empty), disposing.");
            cm.sendOk("Error: No cards available.");
            cm.dispose();
            return;
        }

        console.log("[Debug] Sending Tier selection menu.");
        cm.sendSimple(text);

    } else if (status == 2) {
        // --- STEP 3: SELECT QUANTITY ---

        console.log("[Debug] User selected tier index: " + selection);

        // Because we hardcoded L0 and L1 in status 1, we can rely on selection ID
        if (selection == 0) {
             selectedTierIsSpecial = false; // Normal
        } else if (selection == 1) {
             selectedTierIsSpecial = true; // Boss
        } else {
             // Should not happen unless packet editing
             console.log("[Debug] Invalid Tier Selection: " + selection);
             cm.dispose();
             return;
        }

        console.log("[Debug] Selected Special Tier? " + selectedTierIsSpecial);

        var maxAvailable = selectedTierIsSpecial ? validBossQty : validNormalQty;
        console.log("[Debug] Max Available for this tier: " + maxAvailable);

        if (maxAvailable <= 0) {
             console.log("[Debug] Max available is 0, aborting.");
             cm.dispose();
             return;
        }

        var text = "How many " + (selectedTierIsSpecial ? "Boss" : "Normal") + " cards would you like to redeem? (Max: " + maxAvailable + ")";

        // sendGetNumber(String text, int def, int min, int max)
        console.log("[Debug] Sending GetNumber. Def: " + maxAvailable + " Min: 1 Max: " + maxAvailable);
        cm.sendGetNumber(text, maxAvailable, 1, maxAvailable);

} else if (status == 3) {
        // --- STEP 4: EXECUTE ---

        quantityToRedeem = selection;
        console.log("[Debug] Quantity selected: " + quantityToRedeem);

        // Basic validation
        if (quantityToRedeem <= 0) {
            cm.dispose();
            return;
        }

        var player = cm.getPlayer();
        var mb = player.getMonsterBook();

        console.log("[Debug] Calling Java redeemBulk...");

        // Execute the Java method
        var success = mb.redeemBulk(cm.getClient(), selectedStat, selectedTierIsSpecial, quantityToRedeem);

        console.log("[Debug] Java returned success: " + success);

        if (success) {
            // Send the success message.
            // IMPORTANT: We do NOT dispose here. We wait for the user to click "OK".
            cm.sendOk("Success! Redeemed " + quantityToRedeem + " cards. Stats have been updated permanently.");
        } else {
            cm.sendOk("Transaction failed. An error occurred in the backend.");
        }
        // Do not put cm.dispose() here!
        // Letting the function end implies "wait for user input".

    } else if (status == 4) {
        // --- STEP 5: CLEANUP ---
        // The user clicked "OK" on the result message. Now we close properly.
        console.log("[Debug] User clicked OK on result. Disposing.");
        cm.dispose();
    }
}