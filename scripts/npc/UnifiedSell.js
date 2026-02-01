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

        // Status & Toggles
        // Use persistence methods exposed in NPCConversationManager
        var autoSellOn = cm.isAutoSellEnabled();
        var sellUntradablesOn = cm.isSellUntradables();
        var sellRebirthsOn = cm.isSellRebirths();

        // Aligned Toggles: State | Button
        msg += "#L0#Auto-Sell: " + (autoSellOn ? "#g[ON]#k" : "#r[OFF]#k") + "#l\r\n";
        msg += "#L1#Sell Untradables: " + (sellUntradablesOn ? "#g[ON]#k" : "#r[OFF]#k") + "#l\r\n";
        msg += "#L2#Sell Rebirths: " + (sellRebirthsOn ? "#g[ON]#k" : "#r[OFF]#k") + "#l\r\n";

        msg += "\r\n-------------------------\r\n";
        msg += "#L3#Sell All Equips#l\r\n";
        msg += "#L4#Sell All Use#l\r\n";
        msg += "#L5#Sell All Etc#l\r\n";
        msg += "\r\n-------------------------\r\n";
        msg += "#L6#Sell From Slot (Slot Selection)#l\r\n";
        msg += "#L7#Buyback Items#l\r\n";
        msg += "#L8#Manage Locked Items#l\r\n";

        cm.sendSimple(msg);
    } else if (status == 1) {
        if (selection == 0) {
            cm.toggleAutoSell();
            status = -1;
            action(1, 0, 0); // Reload menu
        } else if (selection == 1) {
            cm.toggleSellUntradables();
            status = -1;
            action(1, 0, 0);
        } else if (selection == 2) {
            cm.toggleSellRebirths();
            status = -1;
            action(1, 0, 0);
        } else if (selection == 3) {
            // NPCConversationManager.sellAll now calls SellAllCommand.sellAllItems with PERSISTENT flags
            // But wait, the API I added `sellAll(type, untradables, rebirths)` in previous step...
            // Actually, I didn't change the API in NPCConversationManager to use defaults.
            // Let me check my memory context... I added `toggleSellUntradables` etc.
            // I did NOT change `sellAll` signature in NPCConversationManager.
            // The existing `cm.sellAll` takes arguments.
            // So here I must pass the persistent values explicitly.
            var u = cm.isSellUntradables();
            var r = cm.isSellRebirths();
            var res = cm.sellAll("equip", u, r);
            cm.sendOk(res);
            cm.dispose();
        } else if (selection == 4) {
            var u = cm.isSellUntradables();
            var r = cm.isSellRebirths();
            var res = cm.sellAll("use", u, r);
            cm.sendOk(res);
            cm.dispose();
        } else if (selection == 5) {
            var u = cm.isSellUntradables();
            var r = cm.isSellRebirths();
            var res = cm.sellAll("etc", u, r);
            cm.sendOk(res);
            cm.dispose();
        } else if (selection == 6) {
            // Redirect to 9000042 (Original Sell From Slot NPC)
            cm.dispose();
            cm.openNpc(9000042);
        } else if (selection == 7) {
            cm.dispose();
            cm.openNpc(9010000, "buyback");
        } else if (selection == 8) {
            cm.dispose();
            cm.openNpc(9010000, "sellAllAssistant");
        }
    }
}
