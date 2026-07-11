var status = -1;

function start() {
    // Show initial options to the player
    cm.sendSimple("Hello i am in charge of Damage Skins! \r\n What would you like to do?\r\n#b#L0#Purchase Damage Skins#l\r\n#L1#Damage Skins Storage#l");
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
                cm.openNpc(9400530, "damageskin");
            } else if (selection == 1) { 
                cm.dispose();
                cm.openNpc(9400530, "damageskinstorage");
            } else {
                // If an invalid selection was made, close the conversation
                cm.sendOk("Invalid selection. Please try again.");
                cm.dispose();
            }
        }
    }
}
