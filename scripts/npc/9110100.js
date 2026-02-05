/*
    Charity Box - 9110100
    Function: Donate 100 mesos for a blessing
*/

var status = -1;
var cost = 100;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) {
        status++;
    } else {
        status--;
    }

    if (status == 0) {
        cm.sendYesNo("Would you like to donate #b" + cost + " mesos#k to the shrine for good luck?");
    } else if (status == 1) {
        if (cm.getMeso() >= cost) {
            cm.gainMeso(-cost);
            // Show a random effect or just a message
            cm.sendOk("May the spirits of Mushroom Shrine bless you.");
        } else {
            cm.sendOk("You don't have enough mesos to donate.");
        }
        cm.dispose();
    }
}
