/* Magician Job Instructor
    Magician 2nd Job Advancement
    Victoria Road : The Forest North of Ellinia (101020000)
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
        // REBIRTH PATH - RETURN
        // If player has the Proof (4031012), send them back.
        // -------------------------------------------------------------
        if (cm.haveItem(4031012, 1)){
             if (status == 0) {
                 cm.sendNext("Incredible. You have returned with the Proof. Your intellect is sharp indeed. Return to #bGrendel the Really Old#k to complete your advancement.");
             } else if (status == 1) {
                 cm.sendAcceptDecline("Would you like me to warp you back to the Magic Library?");
             } else if (status == 2) {
                 cm.warp(101000003, 0); // Warp to Grendel
                 cm.dispose();
             }
             return;
        }

        // -------------------------------------------------------------
        // REBIRTH SKIP (Veterans)
        // -------------------------------------------------------------
        if (cm.getPlayer().getReborns() > 0 && cm.getJob().getId() == 200 && cm.haveItem(4031009)) {
            if (status == 0) {
                 cm.sendNext("Ah, a veteran of the arcane arts. I sense your soul has been reborn. You need not listen to the basic instructions again.");
            } else if (status == 1) {
                 cm.sendAcceptDecline("I will transport you directly to the dimension of testing. Are you prepared?");
            } else if (status == 2) {
                 cm.gainItem(4031009, -1); // Remove Letter
                 cm.warp(108000200, 0); // Warp to Magician Test Map
                 cm.dispose();
            }
            return;
        }

        // -------------------------------------------------------------
        // STANDARD PATH (First-time players)
        // -------------------------------------------------------------
        if (status == 0) {
            if (cm.isQuestCompleted(100008)) {
                cm.sendOk("You have already proven your wisdom. Go forth!");
                cm.dispose();
            } else if (cm.isQuestCompleted(100007)) {
                // Quest Active state (Already finished collection, just needs to enter/talk)
                cm.sendNext("You have returned. Enter the portal, defeat the creatures, collect #b30 Dark Marbles#k, and speak to my colleague. He holds the #bProof of a Hero#k.");
                status = 3; // Skip explanation
            } else if (cm.isQuestStarted(100006)) {
                cm.sendNext("I see a letter from #bGrendel#k. So, you seek to expand your mind? Very well. I shall explain the test.");
            } else {
                cm.sendOk("I can show you the way only when your mind is ready.");
                cm.dispose();
            }
        } else if (status == 1) {
            cm.sendNextPrev("I will send you to a pocket dimension. Inside, you will find creatures born of dark magic. They yield no experience, for this is a test of will, not slaughter.");
        } else if (status == 2) {
            cm.sendNextPrev("You must collect #b30 #t4031013##k. These orbs represent the concentrated essence of their malice. Gather them and present them to the instructor inside.");
        } else if (status == 3) {
            cm.sendAcceptDecline("Once you enter, you cannot leave until the task is done. If you fall in battle, your experience will suffer. Do you possess the courage to proceed?");
        } else if (status == 4) {
            // Standard Quest Update
            if (!cm.isQuestCompleted(100006)) {
                cm.completeQuest(100006);
                cm.startQuest(100007);
                cm.gainItem(4031009, -1); // Remove Letter
            }
            cm.warp(108000200, 0); // Warp to Test Map
            cm.dispose();
        } else {
            cm.dispose();
        }
    }
}