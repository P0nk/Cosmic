/* Warrior Test Instructor
   Inside Warrior Testing Ground (108000300)
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
        // Check for marbles (Standard)
        if (cm.haveItem(4031013, 30)) {
            completed = true;
            cm.sendNext("Hau! You have collected all 30 #bDark Marbles#k! Your strength is undeniable. The spirits are pleased.");
        }
        // Check for Rebirth (Veterans skip)
        else if (cm.getPlayer().getReborns() > 0 && cm.getJob().getId() == 100) {
            completed = true;
            cm.sendNext("Hau! I remember you, veteran! I know your strength exceeds this test. I will let you pass, but do not tell the others!");
        }
        // Not enough marbles
        else {
            completed = false;
            cm.sendSimple("You must collect #b30 #t4031013##k from the spirits in this canyon. Prove your worth! \r\n#b#L1#I am not strong enough. Let me out.#l");
        }
    } else if (status == 1) {
        if (completed) {
            // Remove marbles if they exist (Standard path)
            cm.removeAll(4031013);

            // Handle Quests (Only if not Reborn/Already done to prevent DB errors)
            if (cm.getPlayer().getReborns() == 0) {
                cm.completeQuest(100004);
                cm.startQuest(100005);
            }

            // Give Proof of Hero
            if (!cm.haveItem(4031012)) {
                cm.gainItem(4031012, 1);
            }

            cm.sendNextPrev("Take this #bProof of a Hero#k. Return to #bDances with Balrog#k in Perion. He will guide you to your final destiny.");
        } else {
            // Selection for "I want to leave"
            if (selection == 1) {
                cm.sendYesNo("Are you sure? A warrior does not give up easily. You will have to start over.");
            } else {
                cm.dispose();
            }
        }
    } else if (status == 2) {
        // Warp back to West Rocky Mountain IV (102020300)
        cm.warp(102020300, 0);
        cm.dispose();
    }
}