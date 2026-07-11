var status = -1;

function start() {
    // Show initial options to the player
    cm.sendOk("Hello! How can I assist you today?");
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose(); // Player clicked "Close"
    } else {
        if (mode == 1) { // Player clicked "Next"
            status++;
        } else { // Player clicked "Back"
            status--;
            cm.dispose();
        }

        // Check if we're in the main menu (status 0) and handle selection
        if (status == 0) {
            if (selection == 0) {
                cm.dispose();
                cm.changeMusic("BgmUI/ShopBgm");
                cm.openNpc(9010004, "rooty");
            } else if (selection == 1) {
                cm.dispose();
                cm.changeMusic("BgmUI/ShopBgm");
                cm.openNpc(2616, "donorshop");
            } else if (selection == 2) {
                cm.dispose();
                cm.openNpc(9400530, "retrojqrewards");
            } else if (selection == 3) {
                cm.dispose();
                cm.openNpc(9400530, "damageskinoption");
            } else if (selection == 4) {
                cm.dispose();
                cm.openNpc(9400530, "serverinfo");
            } else {
                // If an invalid selection was made, close the conversation
                cm.sendOk("Invalid selection. Please try again.");
                cm.dispose();
            }
        }
    }
}
