/*
 * Monster Book Redemption NPC (Bulk System) - DEBUG VERSION
 */

var status = -1;
var book = null;

var eligibleNormal = [];
var eligibleSpecial = [];

var selectedTierList = null;
var selectedStat = -1;
var selectedQty = 0;
var multiplier = 1;

var statConfig = [
    ["Weapon Attack", 2],
    ["Magic Attack", 1]
];

function start() {
    java.lang.System.out.println("[NPC DEBUG] start() called.");
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    java.lang.System.out.println("[NPC DEBUG] action called. Mode: " + mode + ", Status: " + status + ", Selection: " + selection);

    if (mode == -1) {
        java.lang.System.out.println("[NPC DEBUG] Mode -1. Disposing.");
        cm.dispose();
        return;
    }
    if (mode == 0) {
        java.lang.System.out.println("[NPC DEBUG] Mode 0. Disposing.");
        cm.dispose();
        return;
    }
    if (mode == 1) {
        status++;
    }

    java.lang.System.out.println("[NPC DEBUG] New Status: " + status);

    if (status == 0) {
        eligibleNormal = [];
        eligibleSpecial = [];

        try {
            book = cm.getPlayer().getMonsterBook();
            var cards = book.getCards();
            var iter = cards.entrySet().iterator();

            while (iter.hasNext()) {
                var entry = iter.next();
                var cardId = entry.getKey();
                var level = entry.getValue();

                if (level >= 5 && !book.isRedeemed(cardId)) {
                    if (Math.floor(cardId / 1000) >= 2388) {
                        eligibleSpecial.push(cardId);
                    } else {
                        eligibleNormal.push(cardId);
                    }
                }
            }
            java.lang.System.out.println("[NPC DEBUG] Cards Found - Normal: " + eligibleNormal.length + " Special: " + eligibleSpecial.length);
        } catch (e) {
            java.lang.System.out.println("[NPC DEBUG] ERROR in status 0 card scanning: " + e);
        }

        if (eligibleNormal.length == 0 && eligibleSpecial.length == 0) {
            cm.sendOk("You don't have any completed card sets (5/5) ready to redeem.");
            cm.dispose();
            return;
        }

        var text = "I found completed cards in your Monster Book ready for redemption.\r\nWhich collection would you like to use?\r\n\r\n";
        if (eligibleNormal.length > 0) {
            text += "#L0#Redeem #bNormal Cards#k (Available: " + eligibleNormal.length + ")#l\r\n";
        }
        if (eligibleSpecial.length > 0) {
            text += "#L1#Redeem #rSpecial Cards#k (Available: " + eligibleSpecial.length + ") #e[10x Stats]#n#l\r\n";
        }
        cm.sendSimple(text);

    } else if (status == 1) {
        java.lang.System.out.println("[NPC DEBUG] Status 1. Selection: " + selection);
        if (selection == 0) {
            selectedTierList = eligibleNormal;
            multiplier = 1;
        } else {
            selectedTierList = eligibleSpecial;
            multiplier = 10;
        }

        if (selectedTierList == null) {
             java.lang.System.out.println("[NPC DEBUG] ERROR: selectedTierList is null!");
             cm.dispose(); return;
        }

        var prompt = "You are redeeming #b" + selectedTierList.length + "#k cards.\r\n";
        prompt += "Which stat would you like to acquire?\r\n\r\n";

        for (var i = 0; i < statConfig.length; i++) {
            var val = statConfig[i][1] * multiplier;
            prompt += "#L" + i + "# " + statConfig[i][0] + " (+" + val + ")#l\r\n";
        }
        cm.sendSimple(prompt);

    } else if (status == 2) {
        selectedStat = selection;
        java.lang.System.out.println("[NPC DEBUG] Status 2. Stat Index: " + selectedStat);

        var preview = "";
        var limit = selectedTierList.length > 10 ? 10 : selectedTierList.length;
        for (var i = 0; i < limit; i++) {
            preview += "#t" + selectedTierList[i] + "#, ";
        }
        if (selectedTierList.length > 10) {
            preview += "... and " + (selectedTierList.length - 10) + " others.";
        } else {
            preview = preview.substring(0, preview.length - 2);
        }

        var prompt = "You selected #b" + statConfig[selectedStat][0] + "#k.\r\n";
        prompt += "Available cards to redeem: " + selectedTierList.length + "\r\n";
        prompt += "Next cards in line: " + preview + "\r\n\r\n";
        prompt += "How many cards do you want to redeem?";

        cm.sendGetNumber(prompt, selectedTierList.length, 1, selectedTierList.length);

    } else if (status == 3) {
        selectedQty = selection;
        java.lang.System.out.println("[NPC DEBUG] Status 3. Qty: " + selectedQty);

        var statName = statConfig[selectedStat][0];
        var statValPerCard = statConfig[selectedStat][1] * multiplier;
        var totalGain = selectedQty * statValPerCard;

        var confirmMsg = "Please Confirm your selection:\r\n\r\n";
        confirmMsg += "Cards Used: #b" + selectedQty + "#k\r\n";
        confirmMsg += "Stat Chosen: #b" + statName + "#k\r\n";
        confirmMsg += "Total Bonus: #r+" + totalGain + " " + statName + "#k\r\n\r\n";
        confirmMsg += "Are you sure you want to proceed?";

        cm.sendYesNo(confirmMsg);

    } else if (status == 4) {
        java.lang.System.out.println("[NPC DEBUG] Status 4. Executing Redemption Loop...");
        var successCount = 0;

        try {
            for (var i = 0; i < selectedQty; i++) {
                var cardId = selectedTierList[i];
                java.lang.System.out.println("[NPC DEBUG] Calling Java redeemCard for CardID: " + cardId);

                // IMPORTANT: This call jumps to Java
                if (book.redeemCard(cm.getClient(), cardId, selectedStat)) {
                    successCount++;
                } else {
                    java.lang.System.out.println("[NPC DEBUG] Java redeemCard returned FALSE for CardID: " + cardId);
                }
            }
        } catch (e) {
            java.lang.System.out.println("[NPC DEBUG] CRASH inside Redemption Loop: " + e);
        }

        java.lang.System.out.println("[NPC DEBUG] Loop finished. Success Count: " + successCount);
        cm.sendOk("Redemption Complete! Successfully redeemed " + successCount + " cards.");
        cm.dispose();
    }
}