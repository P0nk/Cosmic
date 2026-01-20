/* Kyrin - Pirate Job Instructor
   Navigation Room (120000101)
*/

var status = -1;
var sel;

function start() {
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
        if (mode == 1)
            status++;
        else
            status--;

        // -------------------------------------------------------------
        // REBIRTH PATH
        // -------------------------------------------------------------
        if (cm.getPlayer().getReborns() > 0 && cm.getJob() == 500) {
            if (status == 0) {
                cm.sendNext("I see the adventurous spirit of a veteran pirate in you. Since you have been reborn, you do not need to hunt the crystals again.");
            } else if (status == 1) {
                // Pirate specific: Must choose path
                cm.sendSimple("Which path are you taking this time? I will give you the required crystals directly.\r\n#b#L0#Brawler (Knuckles)#l\r\n#L1#Gunslinger (Guns)#l");
            } else if (status == 2) {
                sel = selection;
                if (sel == 0) {
                    // Brawler Path
                    if (cm.canHold(4031856, 15)) {
                        cm.gainItem(4031856, 15); // Potent Power Crystal
                        cm.warp(108000502, 0);    // Warp to Brawler Test
                        cm.dispose();
                    } else {
                        cm.sendOk("Please make space in your Etc inventory.");
                        cm.dispose();
                    }
                } else {
                    // Gunslinger Path
                    if (cm.canHold(4031857, 15)) {
                        cm.gainItem(4031857, 15); // Potent Wind Crystal
                        cm.warp(108000501, 0);    // Warp to Gun Test
                        cm.dispose();
                    } else {
                        cm.sendOk("Please make space in your Etc inventory.");
                        cm.dispose();
                    }
                }
            }
            return;
        }

        // -------------------------------------------------------------
        // STANDARD PATH (Keep your existing standard quest logic below)
        // -------------------------------------------------------------
        if (status == 0) {
             // ... insert your standard Kyrin quest logic here ...
             // (If you need the standard script for Kyrin let me know)
             cm.sendOk("I can show you the way once you're ready for it.");
             cm.dispose();
        }
    }
}