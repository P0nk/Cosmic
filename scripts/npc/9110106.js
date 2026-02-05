/*
    Princess Suzume - 9110106
    Function: Interaction (Haughty Princess)
*/

var status = -1;

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

        if (status == 0) {
            cm.sendSimple("You there! Commoner! Entertain me!\r\n#b#L0#Tell a joke#l\r\n#L1#Compliment her beauty#l\r\n#L2#Ignore her#l");
        } else if (status == 1) {
            if (selection == 0) {
                cm.sendNext("Hmph. That wasn't very funny. Work on your material.");
                status = 2; // End
            } else if (selection == 1) {
                cm.sendNext("Ohohoho! At least you have eyes. I suppose you may pass.");
                status = 2; // End
            } else {
                cm.sendNext("How rude! Guards! ...Oh, right, they're all ninjas now.");
                status = 2; // End
            }
        } else if (status == 2) {
            cm.dispose();
        } else if (status == 3) { // Fallback end
            cm.dispose();
        }
    }
}
