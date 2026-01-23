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
    // REBIRTH PATH - RETURN
    // If player has the Proof (4031012), send them back.
    // -------------------------------------------------------------
    if (cm.haveItem(4031012, 1)){
         if (status == 0) {
             cm.sendNext("Shhh... You have the Proof. Good work. Now get back to the #bDark Lord#k before anyone sees you.");
         } else if (status == 1) {
             cm.sendAcceptDecline("I can sneak you back to the Hideout. Ready?");
         } else if (status == 2) {
             cm.warp(103000003, 0); // Warp to Dark Lord
             cm.dispose();
         }
         return;
    }

    // -------------------------------------------------------------
    // REBIRTH SKIP (Veterans)
    // -------------------------------------------------------------
    if (cm.getPlayer().getReborns() > 0 && cm.getJob().getId() == 400 && cm.haveItem(4031011)) {
        if (status == 0) {
             cm.sendNext("I recognize that shadow... You are a veteran. You don't need to prove your stealth to me again.");
        } else if (status == 1) {
             cm.sendAcceptDecline("I'll open the back door to the testing grounds. Go.");
        } else if (status == 2) {
             cm.gainItem(4031011, -1); // Remove Letter
             cm.warp(108000400, 0); // Warp to Thief Test Map
             cm.dispose();
        }
        return;
    }

    // -------------------------------------------------------------
    // STANDARD PATH (First-time players)
    // -------------------------------------------------------------
    if (status == 0) {
        if (cm.isQuestCompleted(100011)) {
            cm.sendOk("You are already one of us. Go away.");
            cm.dispose();
        } else if (cm.isQuestCompleted(100010)) {
            // Quest Active state
            cm.sendNext("You're back. Get in there, hunt #b30 Dark Marbles#k, and give them to the agent inside. He has the Proof.");
            status = 3; // Skip explanation
        } else if (cm.isQuestStarted(100009)) {
            cm.sendNext("Hand it over... Ah, the #bDark Lord#k's seal. So you want to move up in the organization?");
        } else {
            cm.sendOk("I'm working here. Get lost.");
            cm.dispose();
        }
    } else if (status == 1) {
        cm.sendNextPrev("I'm sending you to an abandoned area. It's crawling with monsters that have been corrupted. They don't give EXP, but they drop what we need.");
    } else if (status == 2) {
        cm.sendNextPrev("Bring back #b30 #t4031013##k. Collect them and find the agent hiding inside. He will give you the Proof.");
    } else if (status == 3) {
        cm.sendAcceptDecline("If you die, you lose EXP. If you leave, you fail. Do you have the guts?");
    } else if (status == 4) {
        // Standard Quest Update
        if (!cm.isQuestCompleted(100009)) {
            cm.completeQuest(100009);
            cm.startQuest(100010);
            cm.gainItem(4031011, -1); // Remove Letter
        }
        cm.warp(108000400, 0); // Warp to Test Map
        cm.dispose();
    } else {
        cm.dispose();
    }
}