/* Magician Test Instructor
   Inside Magician Testing Ground (108000200)
   NPC ID: 1032003
*/

var status = -1;
var completed = false;

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
            cm.sendNext("I sense a powerful aura from you. You have walked this path before, Reborn One. You need not prove your wisdom with these marbles again.");
        }
        // -------------------------------------------------------------
        // 2. STANDARD CHECK (Has 30 Marbles)
        // -------------------------------------------------------------
        else if (cm.haveItem(4031013, 30)) {
            cm.sendNext("Wonderful. You have collected all 30 #bDark Marbles#k. Your focus is absolute.");
        }
        // -------------------------------------------------------------
        // 3. IN PROGRESS (Need more marbles)
        // -------------------------------------------------------------
        else {
            cm.sendSimple("The test is simple, yet demanding. Collect #b30 #t4031013##k from the creatures here to prove your resolve. \r\n#b#L1#I wish to leave this place.#l");
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
            // Quest 100007 (Collection) -> 100008 (Return to Grendel)
            // Only update if not reborn/already done to avoid DB errors
            if (cm.getPlayer().getReborns() == 0 && cm.isQuestStarted(100007)) {
                cm.completeQuest(100007);
                cm.startQuest(100008);
            }

            cm.sendNextPrev("Take this #bProof of a Hero#k. Return to #bGrendel the Really Old#k. He awaits your return in the Magic Library.");
        }
        // Handling "Give Up" Selection
        else {
            if (selection == 1) {
                cm.sendYesNo("Are you certain? Wisdom requires perseverance. If you leave now, you must start the test anew.");
            } else {
                cm.dispose();
            }
        }
    }
    else if (status == 2) {
        // Warp OUT to Forest North of Ellinia (101020000)
        cm.warp(101020000, 0);
        cm.dispose();
    }
}