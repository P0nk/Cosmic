var KarmaRewardSystem = Java.type("server.quest.KarmaRewardSystem");

var status = -1;
var karmaPoints = 0;      
var completedQuests = 0;  
var redeemedMilestones = 0; 
var milestoneThreshold = 100; // Keep 10 for testing, change to 100 later

// BCoin Item ID
var rewardItemID = 3020002; 

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(m, t, s) {
    if (m != 1) {
        cm.dispose();
        return;
    }
    status++;

    if (status == 0) {
        // Fetch data
        karmaPoints = KarmaRewardSystem.calculateKarmaPoints(cm.getPlayer());
        completedQuests = KarmaRewardSystem.getCompletedQuestCount(cm.getPlayer());
        redeemedMilestones = KarmaRewardSystem.getRedeemedMilestoneCount(cm.getPlayer());

        var nextMilestoneTarget = (redeemedMilestones + 1) * milestoneThreshold;
        var questsToNext = nextMilestoneTarget - completedQuests;
        if (questsToNext < 0) questsToNext = 0;

        // SCENARIO 1: Nothing to redeem (No points, No milestone ready)
        if (karmaPoints <= 0 && completedQuests < nextMilestoneTarget) {
            var msg = "#e[ Empress Cygnus ]#n\r\n";
            msg += "Ah, noble hero... Your heart shines with kindness...\r\n\r\n";
            msg += "Do not be discouraged, my brave one. Continue your journey and #e#bcomplete more quests#n#k to aid Maple World and its citizens.\r\n";

            // ADDED: Progress Display
                        msg += "#dYour Progress:#k\r\n";
                        msg += " - Total Quests Completed: #b" +  questsToNext+ "#k\r\n";
                        msg += " - Quests until next Milestone: " + completedQuests + "\r\n\r\n";

            msg += "When your deeds are complete, I shall reward you with the power to become even stronger.\r\n";
            msg += "\r\n#b#L99#I understand, I will continue to help the world.#l#n";
            cm.sendSimple(msg);
            return;
        }

        // SCENARIO 2: Rewards Available
        var msg = "#e[ Empress Cygnus ]#n\r\n";
        msg += "Noble Adventurer! You have earned Karma through your noble deeds. Your efforts are a beacon of hope for all.\r\n\r\n";

        msg += "#dYour Achievements:#k\r\n";
        msg += " - Available Karma Points: #r" + karmaPoints + "#k\r\n";
        msg += " - Total Quests Completed: #b" + completedQuests + "#k\r\n";
        msg += " - Milestones Claimed: #g" + redeemedMilestones + "#k\r\n";

        if (questsToNext > 0) {
            msg += " - Quests until next Milestone: " + questsToNext + "\r\n";
        } else {
             msg += " - #e#rA New Milestone is Available!#k#n\r\n";
        }
        msg += "\r\n";

        // Option 0: Redeem Basic Points
        if (karmaPoints > 0) {
             msg += "#b#L0#Empress, I wish to redeem " + karmaPoints + " Karma Points (" + (karmaPoints * 2) + " AP).#l#k\r\n";
        }

        // Option 1: Claim Milestone
        // Logic: You can claim if Total Quests >= Next Target
        if (completedQuests >= nextMilestoneTarget) {
            // Check if they need to redeem points first.
            if (karmaPoints >= milestoneThreshold) {
                 msg += "\r\n#r(Please redeem your Karma Points above before claiming the Milestone)#k\r\n";
            } else {
                 msg += "#b#L1#I have reached a milestone! Claim Reward (BCoin + 100 AP)#l#k\r\n";
            }
        }

        cm.sendSimple(msg);

    } else if (status == 1) {
        if (s == 0) {
            // Redeem Basic Points
            if (karmaPoints > 0) {
                if (KarmaRewardSystem.redeemKarmaPoints(cm.getPlayer())) {
                    cm.sendOk("#e[ Empress Cygnus ]#n\r\n\r\nThank you, my hero! For your good deeds, you have been awarded AP! Your efforts make Maple World a safer place.");
                } else {
                    cm.sendOk("#e[ Empress Cygnus ]#n\r\n\r\nAlas, my dear hero, an error occurred while redeeming your Karma Points. Do not worry, please try again.");
                }
            } else {
                cm.dispose();
            }
        } else if (s == 1) {
            // Claim Milestone
            if (KarmaRewardSystem.claimNextMilestone(cm.getPlayer())) {
                cm.gainItem(rewardItemID, 1);
                cm.sendOk("#e[ Empress Cygnus ]#n\r\n\r\nCongratulations! You have reached a significant milestone in your journey.\r\n\r\nPlease accept this gift:\r\n#i" + rewardItemID + "# #t" + rewardItemID + "#\r\n#b+100 Bonus AP#k");
            } else {
                cm.sendOk("#e[ Empress Cygnus ]#n\r\n\r\nYou cannot claim this milestone yet. Ensure you have redeemed your pending Karma Points first.");
            }
        } else {
            // Handles "I understand" (s=99) or invalid selections
            cm.dispose();
        }
    } else {
        cm.dispose();
    }
}