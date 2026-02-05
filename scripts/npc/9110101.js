/*
    Sentsu - 9110101
    Function: Armor Seller (Shop)
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
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0) {
            cm.sendNext("I sell the finest armor in Zipangu. Take a look!");
        } else if (status == 1) {
            cm.openShop(1000); // Shop ID updated to 1000
            cm.dispose();
        }
    }
}
