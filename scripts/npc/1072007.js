/* Thief Job Instructor
   Inside Thief Testing Ground (108000400)
   NPC ID: 1072007
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
            cm.sendNext("You move like a ghost. I see you are a veteran. I will not insult you by counting marbles.");
        }
        // -------------------------------------------------------------
        // 2. STANDARD CHECK (Has 30 Marbles)
        // -------------------------------------------------------------
        else if (cm.haveItem(4031013, 30)) {
            cm.sendNext("Thirty marbles... exactly. You are efficient. I like that.");
        }
        // -------------------------------------------------------------
        // 3. IN PROGRESS (Need more marbles)
        // -------------------------------------------------------------
        else {
            cm.sendSimple("You are not done yet. Bring me #b30 #t4031013##k from the Cold Eyes and Blue Mushrooms here. \r\n#b#L1#I can't do it. Let me out.#l");
        }
    } else if (status == 1) {
        if (completed || cm.getPlayer().getReborns() > 0 || cm.haveItem(4031013, 30)) {

            // Only remove marbles if they actually have them (Standard path)
            if (cm.haveItem(4031013, 30)) {
                cm.removeAll(4031013);
            }

            // Give the Proof of Hero
            if (!cm.haveItem(4031012)) {
                cm.gainItem(4031012, 1);
            }

            // [QUEST HANDLING]
            // Quest 100010 (Collection) -> 100011 (Return to Dark Lord)
            if (cm.getPlayer().getReborns() == 0 && cm.isQuestStarted(100010)) {
                cm.completeQuest(100010);
                cm.startQuest(100011);
            }

            cm.sendNextPrev("Here is the #bProof of a Hero#k. Hide it well. Return to the #bDark Lord#k in the Hideout.");
        } else {
            // Selection for "I want to leave"
            if (selection == 1) {
                cm.sendYesNo("Giving up? The Dark Lord does not tolerate failure. You will have to start over.");
            } else {
                cm.dispose();
            }
        }
    } else if (status == 2) {
        // Warp back to Construction Site (102040000)
        cm.warp(102040000, 0);
        cm.dispose();
    }
}