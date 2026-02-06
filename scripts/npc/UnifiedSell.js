var status = 0;

function start() {
    status = -1;
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
    if (mode == 1)
        status++;
    else
        status--;

    if (status == 0) {
        var msg = "\t\t#e#bUnified Sell Assistant#k#n\r\n\r\n";

        // Status & Toggles (Values needed for display)
        var autoSellOn = cm.isAutoSellEnabled();
        var sellUntradablesOn = cm.isSellUntradables();
        var sellRebirthsOn = cm.isSellRebirths();

        // 1. Feature Group (Moved to Top)
        msg += "#L0#Sell From Slot (Slot Selection)#l\r\n";
        msg += "#L1#Buyback Items#l\r\n";
        msg += "#L2#Manage Locked Items#l\r\n";

        msg += "\r\n-------------------------\r\n";

        // 2. Sell All Group (Middle)
        msg += "#L3#Sell All Equips#l\r\n";
        msg += "#L4#Sell All Use#l\r\n";
        msg += "#L5#Sell All Etc#l\r\n";

        msg += "\r\n-------------------------\r\n";

        // 3. Toggles (Moved to Bottom)
        msg += "#L6#Auto-Sell: " + (autoSellOn ? "#g[ON]#k" : "#r[OFF]#k") + "#l\r\n";
        msg += "#L7#Sell Untradables: " + (sellUntradablesOn ? "#g[ON]#k" : "#r[OFF]#k") + "#l\r\n";
        msg += "#L8#Sell Rebirths: " + (sellRebirthsOn ? "#g[ON]#k" : "#r[OFF]#k") + "#l\r\n";

        cm.sendSimple(msg);
    } else if (status == 1) {
        if (selection == 0) {
            // Sell From Slot (Old 6)
            cm.dispose();
            cm.openNpc(9000042);
        } else if (selection == 1) {
            // Buyback (Old 7)
            cm.dispose();
            cm.openNpc(9010000, "buyback");
        } else if (selection == 2) {
            // Manage Locked (Old 8)
            cm.dispose();
            cm.openNpc(9010000, "sellAllAssistant");
        } else if (selection == 3) {
            // Sell All Equip (Old 3)
            var u = cm.isSellUntradables();
            var r = cm.isSellRebirths();
            var res = cm.sellAll("equip", u, r);
            cm.sendOk(res);
            cm.dispose();
        } else if (selection == 4) {
            // Sell All Use (Old 4)
            var u = cm.isSellUntradables();
            var r = cm.isSellRebirths();
            var res = cm.sellAll("use", u, r);
            cm.sendOk(res);
            cm.dispose();
        } else if (selection == 5) {
            // Sell All Etc (Old 5)
            var u = cm.isSellUntradables();
            var r = cm.isSellRebirths();
            var res = cm.sellAll("etc", u, r);
            cm.sendOk(res);
            cm.dispose();
        } else if (selection == 6) {
            // Auto Sell Toggle (Old 0)
            cm.toggleAutoSell();
            status = -1;
            action(1, 0, 0); // Reload menu
        } else if (selection == 7) {
            // Sell Untradables Toggle (Old 1)
            cm.toggleSellUntradables();
            status = -1;
            action(1, 0, 0);
        } else if (selection == 8) {
            // Sell Rebirths Toggle (Old 2)
            cm.toggleSellRebirths();
            status = -1;
            action(1, 0, 0);
        }
    }
}
