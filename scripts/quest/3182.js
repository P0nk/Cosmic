/*  Author: Rulax
	NPC Name: 		Alcaster (2020005)
	Map(s): 		El Nath Market
	Quest Name: 	Alcaster's Crystal
*/

var status = -1;

function start(mode, type, selection) {
    status++;
    if (mode != 1) {
        if (type == 1 && mode == 0) {
            status -= 2;
        } else {
            qm.sendOk("It's not that big a favor... Aren't you a selfish person! Tsk tsk...");
            qm.dispose();
            return;
        }
    }
    if (status == 0) {
        if (!qm.canHold(2430159) && !qm.haveItem(2430159)) {
            qm.sendOk("Your inventory seems to be full. Please make some space and come back.");
            qm.dispose();
            return;
        }
        qm.sendAcceptDecline("I've delivered #b#p2160000##k's letter to his family. Now, could you do me a favor?");
    } else if (status == 1) {
        if (!qm.canHold(2430159)) {
            qm.sendOk("Your inventory seems to be full. Please make some space and come back.");
            qm.dispose();
            return;
        }
        qm.forceStartQuest(3182);
        qm.gainItem(2430159, 1);
        qm.dispose();
    }
}

function end(mode, type, selection)  {
    if (mode == -1) {
        qm.dispose();
    } else {
        if (mode == 0 && type > 0) {
            qm.dispose();
            return;
        }

        if (mode == 1) {
            status++;
        } else {
            status--;
        }
    }

    if (status == 0) {
         qm.sendNext("It's you... Did you deliver my letter to Alcaster?");
    } else if (status == 1) {
        qm.sendNext("You weren't kidding... I'm not cold anymore. I'm not in pain either! I can move freely! Ha ha ha ha! Thanks a lot!");
    } else if (status == 2) {
        qm.sendOk("I owe Alcaster big! And you, too.");
        qm.forceCompleteQuest();
        qm.dispose();
    }
}

