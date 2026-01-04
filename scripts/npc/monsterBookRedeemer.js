/*
 * Monster Book Redemption NPC (Bulk System)
 * Allows bulk trading of completed (Level 5) Monster Cards for permanent stats.
 *
 * Updates:
 * - Only offers Weapon Attack (WATK) and Magic Attack (MATK).
 * - Multiplier logic preserved (Special cards give 10x stats).
 */

var status = -1;
var book = null;

// Arrays to store the IDs of redeemable cards found in the player's book
var eligibleNormal = [];
var eligibleSpecial = [];

// User selections
var selectedTierList = null; // Will point to either eligibleNormal or eligibleSpecial
var selectedStat = -1;
var selectedQty = 0;
var multiplier = 1;

// Stat configuration
// [Name, value per card]
// 0 = WATK, 1 = MATK (Matches the Java side 'selectedStat' index logic)
var statConfig = [
    ["Weapon Attack", 1], // Base gain per normal card
    ["Magic Attack", 2]   // Base gain per normal card
];

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) {
        status++;
    }

    if (status == 0) {
        // Reset arrays
        eligibleNormal = [];
        eligibleSpecial = [];
        book = cm.getPlayer().getMonsterBook();

        // Scan the entire book
        var cards = book.getCards(); // Map<Integer, Integer>
        var iter = cards.entrySet().iterator();

        while (iter.hasNext()) {
            var entry = iter.next();
            var cardId = entry.getKey();
            var level = entry.getValue();

            // Check: Level 5 AND Not Redeemed
            if (level >= 5 && !book.isRedeemed(cardId)) {
                if (Math.floor(cardId / 1000) >= 2388) {
                    eligibleSpecial.push(cardId);
                } else {
                    eligibleNormal.push(cardId);
                }
            }
        }

        if (eligibleNormal.length == 0 && eligibleSpecial.length == 0) {
            cm.sendOk("You don't have any completed card sets (5/5) ready to redeem.");
            cm.dispose();
            return;
        }

        var text = "I found completed cards in your Monster Book ready for redemption.\r\nWhich collection would you like to use?\r\n\r\n";

        // Option 0: Normal
        if (eligibleNormal.length > 0) {
            text += "#L0#Redeem #bNormal Cards#k (Available: " + eligibleNormal.length + ")#l\r\n";
        }
        // Option 1: Special
        if (eligibleSpecial.length > 0) {
            text += "#L1#Redeem #rSpecial Cards#k (Available: " + eligibleSpecial.length + ") #e[10x Stats]#n#l\r\n";
        }

        cm.sendSimple(text);

    } else if (status == 1) {
        // Selection is 0 (Normal) or 1 (Special)
        if (selection == 0) {
            selectedTierList = eligibleNormal;
            multiplier = 1;
        } else {
            selectedTierList = eligibleSpecial;
            multiplier = 10;
        }

        var prompt = "You are redeeming #b" + selectedTierList.length + "#k cards.\r\n";
        prompt += "Which stat would you like to acquire?\r\n\r\n";

        for (var i = 0; i < statConfig.length; i++) {
            var val = statConfig[i][1] * multiplier;
            prompt += "#L" + i + "# " + statConfig[i][0] + " (+" + val + ")#l\r\n";
        }

        cm.sendSimple(prompt);

    } else if (status == 2) {
        // Selection is the Stat Index (0 for WATK, 1 for MATK)
        selectedStat = selection;

        // Generate a preview list of cards that will be used (Limit to first 10 for readability)
        var preview = "";
        var limit = selectedTierList.length > 10 ? 10 : selectedTierList.length;
        for (var i = 0; i < limit; i++) {
            preview += "#t" + selectedTierList[i] + "#, ";
        }
        if (selectedTierList.length > 10) {
            preview += "... and " + (selectedTierList.length - 10) + " others.";
        } else {
            preview = preview.substring(0, preview.length - 2); // remove trailing comma
        }

        var prompt = "You selected #b" + statConfig[selectedStat][0] + "#k.\r\n";
        prompt += "Available cards to redeem: " + selectedTierList.length + "\r\n";
        prompt += "Next cards in line: " + preview + "\r\n\r\n";
        prompt += "How many cards do you want to redeem?";

        // Min: 1, Max: Available count, Default: Available count
        cm.sendGetNumber(prompt, selectedTierList.length, 1, selectedTierList.length);

    } else if (status == 3) {
        selectedQty = selection;

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
        // Execute Redemption Loop
        var successCount = 0;

        // We iterate through the first 'selectedQty' items of our list
        for (var i = 0; i < selectedQty; i++) {
            var cardId = selectedTierList[i];

            // Call the Java method
            // Java side must interpret: 0 -> Passive WATK, 1 -> Passive MATK
            if (book.redeemCard(cm.getClient(), cardId, selectedStat)) {
                successCount++;
            }
        }

        cm.sendOk("Redemption Complete! Successfully redeemed " + successCount + " cards.");
        cm.dispose();
    }
}