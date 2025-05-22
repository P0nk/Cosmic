var status;
var ticketId = 5220000;
var ticketCount = 0;
var mapName = ["Henesys", "Ellinia", "Perion", "Kerning City", "Sleepywood", "Mushroom Shrine", "Showa Spa (M)", "Showa Spa (F)", "Ludibrium", "New Leaf City", "El Nath", "Nautilus"];
var curMapName = "";
var modeSelection = -1; // 0 = single use, 1 = multi-use

function start() {
    status = -1;
    var npcId = cm.getNpc();
    var index;

    if (npcId === 9100109) {
        index = 9; // New Leaf City
    } else if (npcId === 9100117) {
        index = 11; // Nautilus
    } else {
        index = npcId - 9100100;
    }

    if (index < 0 || index >= mapName.length) {
        cm.sendOk("Invalid NPC configuration. Please report this issue.");
        cm.dispose();
        return;
    }

    curMapName = mapName[index];
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode < 0) {
        cm.dispose();
        return;
    }

    if (mode == 1) {
        status++;
    } else {
        status--;
    }

    // Player has tickets
    if (status == 0) {
        if (cm.haveItem(ticketId)) {
            cm.sendSimple("Welcome to the #b" + curMapName + "#k Gachapon! You have #b" + cm.itemQuantity(ticketId) + "#k tickets.\r\n\r\n#L0#Use 1 Gachapon ticket (single draw)#l\r\n#L1#Use multiple Gachapon tickets (bulk draw)#l");
        } else {
            cm.sendSimple("Welcome to the " + curMapName + " Gachapon. How may I help you?\r\n\r\n#L2#What is Gachapon?#l\r\n#L3#Where can you buy Gachapon tickets?#l");
        }

    } else if (status == 1) {
        // Ticket usage logic
        if (selection == 0) {
            modeSelection = 0; // single
            if (cm.canHold(1302000) && cm.canHold(2000000) && cm.canHold(3010001) && cm.canHold(4000000)) {
                cm.gainItem(ticketId, -1);
                cm.doGachapon(true);
                cm.dispose();
            } else {
                cm.sendOk("Please have at least one slot in your #rEQUIP, USE, SET-UP, #kand #rETC#k inventories free.");
            }
            cm.dispose();

        } else if (selection == 1) {
            modeSelection = 1;
            cm.sendGetText("How many Gachapon tickets would you like to use?", "1");

        } else if (selection == 2) {
            cm.sendNext("Play Gachapon to earn rare scrolls, equipment, chairs, mastery books, and other cool items! All you need is a #bGachapon Ticket#k to win a random item.");
        } else if (selection == 3) {
            cm.sendNext("Gachapon Tickets are available in the #rCash Shop#k. Click on the red SHOP at the lower right-hand corner of the screen to purchase tickets using NX or Maple Points.");
        } else {
            cm.dispose();
        }

    } else if (status == 2 && modeSelection == 1) {
        // Multi-ticket logic
        ticketCount = parseInt(cm.getText());

        if (isNaN(ticketCount) || ticketCount <= 0) {
            cm.sendOk("Please enter a valid number of tickets.");
            cm.dispose();
            return;
        }

        if (!cm.haveItem(ticketId, ticketCount)) {
            cm.sendOk("You don't have enough Gachapon tickets.");
            cm.dispose();
            return;
        }

        if (cm.hasEnoughSlotsForGachapon(ticketCount)) {
            cm.doGachaponWithDelay(ticketCount);
        } else {
            cm.sendOk("You need at least " + ticketCount + " free slots in each of your #rEQUIP, USE, SET-UP,#k and #rETC#k inventories.");
        }
        cm.dispose();

    } else if (status == 2) {
        cm.sendNextPrev("You'll find a variety of items from the " + curMapName + " Gachapon, mostly related to this area.");
    } else {
        cm.dispose();
    }
}
