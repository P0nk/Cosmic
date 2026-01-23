/* Warrior Job Instructor
    Warrior 2nd Job Advancement
    Victoria Road : West Rocky Mountain IV (102020300)
*/

var status;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
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
        // If player has the Proof (4031012), send them back.
        // -------------------------------------------------------------
       if (cm.haveItem(4031012, 1)){
             if (status == 0) {
                        cm.sendNext("Hau! You have returned with the proof! You are truly a great warrior. Now, return to #bDances with Balrog#k to complete your ritual!");
                   } else if (status == 1) {
                        cm.sendAcceptDecline("Would you like me to warp you back to Perion?");
                   } else if (status == 2) {
                        cm.warp(102000003, 0); // Warp to Dances with Balrog
                        cm.dispose();
                   }
                   return;
               }

        // -------------------------------------------------------------
        // REBIRTH SKIP (Veterans)
        // -------------------------------------------------------------
        if (cm.getPlayer().getReborns() > 0 && cm.getJob().getId() == 100 && cm.haveItem(4031008)) {
            if (status == 0) {
                 cm.sendNext("Hau! It is you again! I see the scars of a thousand battles on you. Although you have been reborn, you must walk the path again... but I know your strength.");
            } else if (status == 1) {
                 cm.sendAcceptDecline("I will send you straight to the testing grounds. Are you ready, brave one?");
            } else if (status == 2) {
                 cm.gainItem(4031008, -1); // Remove Letter
                 cm.warp(108000300, 0); // Warp to Test Map
                 cm.dispose();
            }
            return;
        }

        // -------------------------------------------------------------
        // STANDARD PATH (First-time players)
        // -------------------------------------------------------------
        if (status == 0) {
            if (cm.isQuestCompleted(100004)) {
                cm.sendOk("You are truly a hero of Perion!");
                cm.dispose();
            } else if (cm.isQuestCompleted(100003)) {
                // Quest Active state
                cm.sendNext("Alright! Defeat the monsters inside, collect #b30 Dark Marbles#k, and speak to my brother inside. He will give you the #bProof of a Hero#k. May the spirits guide your blade.");
                status = 3; // Skip explanation
            } else if (cm.isQuestStarted(100003)) {
                cm.sendNext("Hau... Is that a letter from #bDances with Balrog#k? So you have come to prove your strength. Listen well, young brave.");
            } else {
                cm.sendOk("I can show you the way only when you are ready.");
                cm.dispose();
            }
        } else if (status == 1) {
            cm.sendNextPrev("I will send you to a hidden rocky canyon. The monsters there are possessed by evil spirits. They give no experience, only death.");
        } else if (status == 2) {
            cm.sendNextPrev("You must hunt them and collect #b30 #t4031013##k. These marbles contain their evil essence. Collect them and speak to my brother inside.");
        } else if (status == 3) {
            cm.sendAcceptDecline("If you leave before finishing, you fail. If you die, you lose experience. Are you brave enough to enter?");
        } else if (status == 4) {
            // Standard Quest Update
            if (!cm.isQuestCompleted(100003)) {
                cm.completeQuest(100003);
                cm.startQuest(100004);
                cm.gainItem(4031008, -1); // Remove Letter
            }
            cm.warp(108000300, 0); // Warp to Test Map
            cm.dispose();
        } else {
            cm.dispose();
        }
    }
}