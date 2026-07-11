/*  Author: Rulax
	Item Name: 		Thunder Stone (2430200)
	Description: 	Consumes stones to create Dream Key
*/

var status;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        im.dispose();
    } else {
        if (mode == 0 && type > 0) {
            im.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0) {
        if (!im.hasItem(4000660, 5)
            || !im.hasItem(4000661,5)
            || !im.hasItem(4000662,5)
            || !im.hasItem(4000663,5))
        {
            im.playerMessage(5, "You need 5 of each stone in order to craft a Dream Key.");
            im.dispose();
            return;
        }
             im.gainItem(4000660,-5);
             im.gainItem(4000661,-5);
             im.gainItem(4000662,-5);
             im.gainItem(4000663,-5);
             im.gainItem(2430200,-1);
             im.gainItem(4032923, 1);
             im.playerMessage(5, "You have obtained the Dream Key.");
                }
            }
            im.dispose();
}