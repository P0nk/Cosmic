/* * Script: rebirth.js
 * Triggered by: Level Up or @reborn
 */

var status = -1;

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
        cm.sendOk("You have chosen to remain at Level 255 for now. You can use #b@reborn#k at any time to return here.");
        cm.dispose();
        return;
    }
    if (mode == 1)
        status++;
    else
        status--;

    if (status == 0) {
        cm.sendYesNo("Slimy here! Congratulations! You have reached the absolute limit of your power.\r\n\r\nWould you like to #bRebirth#k?\r\n\r\n#e[Implications]:#n\r\n- Level reset to 1\r\n- Job reset to Beginner\r\n- Skills unlearned\r\n- Stats reset to 13/5/4/4 \r\n- You gain #b+5 bonus AP#k per level per rebirth permanently.");
    } else if (status == 1) {
        // The player clicked Yes
        cm.getPlayer().doRebirth(); // Calls the Java method we created in Part 1
        cm.dispose();
    }
}