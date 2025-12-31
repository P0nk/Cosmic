// Declare the KarmaRewardSystem Manager
var KarmaRewardSystem = Java.type("server.quest.KarmaRewardSystem");

var status = -1;
var karmaPoints = 0;

function start() {
    // Fetch the character's Karma Points using the KarmaRewardSystem manager
    karmaPoints = KarmaRewardSystem.calculateKarmaPoints(cm.getPlayer());

    // Check if the player has any Karma Points
    if (karmaPoints <= 0) {
        // If the player doesn't have Karma Points, Empress Cygnus will kindly ask for help
        var msg = "#e[ Empress Cygnus ]#n\r\n";
        msg += "Ah, noble hero... Your heart shines with kindness..\n";
        msg += "Do not be discouraged, my brave one. Continue your journey and#e#kcomplete more quests#n#k to aid Maple World and its citizens.\n";
        msg += "When your deeds are complete, I shall reward you with the power to become even stronger.\r\n";
        msg += "#b#L0#I understand, I will continue to help the world.#l#n";
        cm.sendSimple(msg);
        cm.dispose();
        return;
    }

    // Themed message based on Karma Points
    var msg = "#e[ Empress Cygnus ]#n\r\n";

    if (karmaPoints < 5) {
        msg += "Noble Adventurer! You have earned some Karma through your noble deeds. Continue your journey, and more will come!\n";
    } else if (karmaPoints >= 5 && karmaPoints <= 15) {
        msg += "Great Karma, hero! Your actions have made a difference in the world. Keep it up, and you will be rewarded!\n";
    } else if (karmaPoints > 15) {
        msg += "Savior of the world! Your deeds have transcended the ordinary. The world is forever in your debt, hero!\n";
    }

    // Only one option: "I wish to receive my rewards"
    msg += "Come, let me bless you with the strength to grow even greater and save Maple World!\r\n";
    msg += "#b#L0#Thank You Cygnus, I wish to receive my rewards.#l#n";
    cm.sendSimple(msg);
}

function action(m, t, s) {
    if (m == 1) { // Handle selection
        status++;

        if (status == 0) { // First status, display the reward option
            if (s == 0) {
                // Spend all Karma Points to gain AP
                if (karmaPoints >= 1) {
                    // Redeem all Karma Points for AP
                    var result = KarmaRewardSystem.redeemKarmaPoints(cm.getPlayer());  // Redeem all Karma Points
                    if (result) {
                        cm.sendOk("Thank you, my hero! For your good deeds, you have been awarded " + karmaPoints + " AP! Your efforts are a beacon of hope for all.");
                        karmaPoints = KarmaRewardSystem.calculateKarmaPoints(cm.getPlayer());  // Update Karma Points
                    } else {
                        cm.sendOk("Ah, my dear hero, an error occurred while redeeming your Karma Points. Do not worry, you may try again.");
                    }
                } else {
                    cm.sendOk("Alas, you do not have enough Karma Points to redeem at this moment. Continue your noble journey, and more rewards shall await you.");
                }
            }
        }
        cm.dispose();  // End the NPC interaction
    }

    // If the player exits or if there's any other reason to dispose the interaction
    cm.dispose();  // Ensure conversation ends gracefully
}
