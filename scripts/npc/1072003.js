/* Thief Job Instructor
    Thief 2nd Job Advancement
    Victoria Road : Construction Site North of Kerning City (102040000)
*/

var status;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }

    if (mode == 0 && type > 0) {
        cm.dispose();
        return;
    }

    if (mode == 1) {
        status++;
    } else {
        status--;
    }

    // -------------------------------------------------------------
    // REBIRTH PATH
    // If player is a Thief (400) and has Rebirths, we allow re-entry
    // regardless of previous quest completion status.
    // -------------------------------------------------------------
    if (cm.getPlayer().getReborns() > 0 && cm.getJob() == 400) {
        if (status == 0) {
             cm.sendNext("I see the spirit of a veteran thief in you. Although you have been reborn, you must prove your skills again for this life.");
        } else if (status == 1) {
             cm.sendAcceptDecline("I will let you enter the testing grounds immediately. Are you ready?");
        } else if (status == 2) {
             // Warp directly to the test map
             cm.warp(108000400, 0);
             cm.dispose();
        }
        return;
    }

    // -------------------------------------------------------------
    // STANDARD PATH (First-time players)
    // -------------------------------------------------------------
    if (status == 0) {
        if (cm.isQuestCompleted(100010)) {
            cm.sendOk("You're truly a hero!");
            cm.dispose();
        } else if (cm.isQuestCompleted(100009)) {
            cm.sendNext("Alright I'll let you in! Defeat the monsters inside, collect 30 Dark Marbles, then strike up a conversation with a colleague of mine inside. He'll give you #bThe Proof of a Hero#k, the proof that you've passed the test. Best of luck to you.");
            status = 3; // Jumps to the warp confirmation block
        } else if (cm.isQuestStarted(100009)) {
            cm.sendNext("Oh, isn't this a letter from the #bDark Lord#k?");
        } else {
            cm.sendOk("I can show you the way once you're ready for it.");
            cm.dispose();
        }
    } else if (status == 1) {
        cm.sendNextPrev("So you want to prove your skills? Very well...");
    } else if (status == 2) {
        cm.sendAcceptDecline("I will give you a chance if you're ready.");
    } else if (status == 3) {
        // Status 3 is shared by the 'isQuestCompleted(100009)' jump above
        cm.sendOk("You will have to collect me #b30 #t4031013##k. Good luck.");

        // Only run quest logic if not already completed/started to avoid errors
        if (!cm.isQuestCompleted(100009)) {
            cm.completeQuest(100009);
            cm.startQuest(100010);
            cm.gainItem(4031011, -1); // Remove Letter from Dark Lord
        }
    } else if (status == 4) {
        cm.warp(108000400, 0);
        cm.dispose();
    } else {
        cm.dispose();
    }
}