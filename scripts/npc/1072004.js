/* Warrior Test Instructor
   Inside Warrior Testing Ground (108000300)
   NPC ID: 102020400
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
            cm.sendNext("I see that you are a veteran warrior who has been #breborn#k. You do not need to prove your strength to me again via these marbles.");
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
            // Only attempt to update the quest if the player is actually on the quest step.
            // This prevents "You have already completed this quest" errors for Reborns.
            if (cm.isQuestStarted(100004)) {
                cm.completeQuest(100004);
                cm.startQuest(100005);
            }

            cm.sendNextPrev("Here is #bThe Proof of a Hero#k. Take this back to #bDances with Balrog#k in Perion.");
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
        // Warp OUT to West Rocky Mountain IV (102020300)
        cm.warp(102020300, 0);
        cm.dispose();
    }
}