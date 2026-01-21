/* Magician Test Instructor
   Inside Magician Testing Ground (108000200)
   NPC ID: 1032003
*/

var status = -1;

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 0 && status == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) status++;
    else status--;

    if (status == 0) {
        // -------------------------------------------------------------
        // 1. REBIRTH CHECK (Bypass for Veterans)
        // -------------------------------------------------------------
        if (cm.getPlayer().getReborns() > 0) {
            cm.sendNext("I see that you are a veteran magician who has been #breborn#k. You do not need to prove your wisdom to me again via these marbles.");
        }
        // -------------------------------------------------------------
        // 2. STANDARD CHECK (Has 30 Marbles)
        // -------------------------------------------------------------
        else if (cm.haveItem(4031013, 30)) {
            cm.sendNext("Oh, you have collected all 30 #bDark Marbles#k! That is incredible. You have passed the test.");
        }
        // -------------------------------------------------------------
        // 3. IN PROGRESS (Need more marbles)
        // -------------------------------------------------------------
        else {
            cm.sendSimple("You need to collect #b30 Dark Marbles#k from the monsters in this map. Good luck.\r\n#L1#I want to give up and leave.#l");
        }
    }
    else if (status == 1) {
        // Handling Success (Reborn OR Has Marbles)
        if (cm.getPlayer().getReborns() > 0 || cm.haveItem(4031013, 30)) {

            // Only remove marbles if they actually have them (Standard path)
            if (cm.haveItem(4031013, 30)) {
                cm.removeAll(4031013);
            }

            // Give the Proof of Hero if they don't have it
            if (!cm.haveItem(4031012)) {
                cm.gainItem(4031012, 1);
            }

            // [QUEST HANDLING]
            // Standard Magician Quest Flow:
            // 100006 (Start at Grendel) -> 100007 (Collection) -> 100008 (Return with Proof)
            if (cm.isQuestStarted(100007)) {
                cm.completeQuest(100007);
                cm.startQuest(100008);
            }

            cm.sendNextPrev("Here is #bThe Proof of a Hero#k. Take this back to #bGrendel the Really Old#k in Ellinia.");
        }
        // Handling "Give Up" Selection
        else {
            if (selection == 1) {
                cm.sendYesNo("Are you sure you want to leave? You will have to start over if you return.");
            } else {
                cm.dispose();
            }
        }
    }
    else if (status == 2) {
        // Warp OUT to The Forest North of Ellinia (101020000)
        cm.warp(101020000, 0);
        cm.dispose();
    }
}