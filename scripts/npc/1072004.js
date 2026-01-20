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
        // REBIRTH PATH (Fix for "Marbles not dropping")
        // -------------------------------------------------------------
        if (cm.getPlayer().getReborns() > 0 && cm.getJob() == 100) {
            if (status == 0) {
                 cm.sendNext("I see the courage of a veteran warrior in you. Since you have been reborn, you do not need to hunt the monsters again.");
            } else if (status == 1) {
                 cm.sendAcceptDecline("I will give you the **30 Dark Marbles** directly and warp you in. Simply hand them to the instructor inside to pass the test. Are you ready?");
            } else if (status == 2) {
                 if (cm.canHold(4031013, 30)) {
                     // Give the 30 Marbles directly
                     cm.gainItem(4031013, 30);
                     // Warp to Test Map
                     cm.warp(108000300, 0);
                     cm.dispose();
                 } else {
                     cm.sendOk("Please make some space in your Etc inventory for the marbles.");
                     cm.dispose();
                 }
            }
            return;
        }

        // -------------------------------------------------------------
        // STANDARD PATH (First-time players)
        // -------------------------------------------------------------
        if (status == 0) {
            if (cm.isQuestCompleted(100004)) {
                cm.sendOk("You're truly a hero!");
                cm.dispose();
            } else if (cm.isQuestCompleted(100003)) {
                cm.sendNext("Alright I'll let you in! Defeat the monsters inside, collect 30 Dark Marbles, then strike up a conversation with a colleague of mine inside. He'll give you #bThe Proof of a Hero#k, the proof that you've passed the test. Best of luck to you.");
                status = 4;
            } else if (cm.isQuestStarted(100003)) {
                cm.sendNext("Hmmm...it is definitely the letter from #bDances with Balrog#k...so you came all the way here to take the test and make the 2nd job advancement as the warrior. Alright, I'll explain the test to you. Don't sweat it too much, it's not that complicated.");
            } else {
                cm.sendOk("I can show you the way once your ready for it.");
                cm.dispose();
            }
        } else if (status == 1) {
            cm.sendNextPrev("I'll send you to a hidden map. You'll see monsters you don't normally see. They look the same like the regular ones, but with a totally different attitude. They neither boost your experience level nor provide you with item.");
        } else if (status == 2) {
            cm.sendNextPrev("You'll be able to acquire a marble called #b#t4031013##k while knocking down those monsters. It is a special marble made out of their sinister, evil minds. Collect 30 of those, and then go talk to a colleague of mine in there. That's how you pass the test.");
        } else if (status == 3) {
            cm.sendYesNo("Once you go inside, you can't leave until you take care of your mission. If you die, your experience level will decrease..so you better really buckle up and get ready...well, do you want to go for it now?");
        } else if (status == 4) {
            cm.sendNext("Alright I'll let you in! Defeat the monsters inside, collect 30 Dark Marbles, then strike up a conversation with a colleague of mine inside. He'll give you #bThe Proof of a Hero#k, the proof that you've passed the test. Best of luck to you.");

            if (!cm.isQuestCompleted(100003)) {
                cm.completeQuest(100003);
                cm.startQuest(100004);
                cm.gainItem(4031008, -1);
            }
        } else if (status == 5) {
            cm.warp(108000300, 0);
            cm.dispose();
        } else {
            cm.dispose();
        }
    }
}