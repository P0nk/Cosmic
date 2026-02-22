/*
    Boss Shop NPC Script - Selling Boss Materials
    NPC ID: 11007
    Materials: Zakum Eye of Fire, Horntail Mana Crystal, Pink Poppin, Von Leon Ticket
*/

var status = -1;
var ZAKUM_EYE_OF_FIRE = 4001017; // Zakum Eye of Fire (Material 1)
var HORNTAIL_MANA_CRYSTAL = 4021032; // Horntail Mana Crystal (Material 2)
var PINK_POPPIN = 4001189; // Pink Poppin (Material 3)
var VONLEON_TICKET = 4001694; // Von Leon Ticket (Material 4)
var WHEEL_OF_DESTINY = 5510000;
var material_list = [4001017, 4021032, 4001189, 4001694]

// Prices for the materials
var ZAKUM_PRICE = 10;  // 10M mesos
var HORNTAIL_PRICE = 20; // 20M mesos
var PINK_POPPIN_PRICE = 30; // 30M mesos
var VONLEON_TICKET_PRICE = 40; // 40M mesos
var WHEEL_PRICE_NX = 5000;

var selectedMaterial = 0;
var purchaseQty = 0;
var eligibility = []; // Global variable to store eligibility data

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && type > 0) {
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        // Debug: Print current status and selection
        console.log("Debug: Current status is " + status + ", selection is " + selection);

        if (status == 0) {
            // Retrieve eligibility for each material by passing character ID to checkEligibility
            eligibility = Packages.server.bossshop.BossShopManager.checkEligibility(cm.getPlayer().getId());

            // Build the message to show the player the remaining purchase limits
            var msg = "Hi there! I'm Garden, and I sell boss materials.\r\n" +
                "But be aware, there's a daily limit on how many materials you can buy from me.\r\n" +
                "Here's what you can still purchase today:\r\n" +
                "#bZakum Eye of Fire: " + eligibility[0] + "#r\r\n" +
                "#bHorntail Mana Crystal: " + eligibility[1] + "#r\r\n" +
                "#bPink Bean Poppin: " + eligibility[2] + "#r\r\n" +
                "#bVon Leon Ticket: " + eligibility[3] + "#r\r\n" +
                "#kWhat would you like to do?\r\n";

            // Display the clickable icons for Zakum Eye of Fire, Horntail Mana Crystal, Pink Poppin, and Von Leon Ticket
            msg += "#L0##i" + ZAKUM_EYE_OF_FIRE + "##b Zakum Eye of Fire#r (" + ZAKUM_PRICE + "M mesos)#l\r\n";
            msg += "#L1##i" + HORNTAIL_MANA_CRYSTAL + "##b Horntail Mana Crystal#r (" + HORNTAIL_PRICE + "M mesos)#l\r\n";
            msg += "#L2##i" + PINK_POPPIN + "##b Pink Bean Poppin#r (" + PINK_POPPIN_PRICE + "M mesos)#l\r\n";
            msg += "#L3##i" + VONLEON_TICKET + "##b Von Leon Ticket#r (" + VONLEON_TICKET_PRICE + "M mesos)#l\r\n";
            msg += "#L4##i" + WHEEL_OF_DESTINY + "##b Wheel of Destiny#r (" + WHEEL_PRICE_NX + " NX)#l\r\n";

            cm.sendSimple(msg);
        } else if (status == 1) {
            selectedMaterial = selection; // Set the selected material based on the player's choice

            if (selectedMaterial == 4) {
                cm.sendGetText("I'm able to sell you as many Wheels of Destiny as you have NX for.\r\nHow many would you like to purchase?");
                return;
            }

            // Get the current date and eligibility status for selected material
            var materialId = (selectedMaterial === 0) ? ZAKUM_EYE_OF_FIRE :
                (selectedMaterial === 1) ? HORNTAIL_MANA_CRYSTAL :
                    (selectedMaterial === 2) ? PINK_POPPIN :
                        VONLEON_TICKET;
            var maxPurchases = Packages.server.bossshop.BossShopScheduler.getMaterialPurchaseLimit(materialId);

            // Calculate the remaining quantity based on the eligibility
            var remainingQuantity = eligibility[selectedMaterial];

            var msg = "I'm able to sell you " + remainingQuantity + " more.\r\n";
            msg += "How many would you like to purchase?";

            cm.sendGetText(msg);
        } else if (status == 2) {
            purchaseQty = parseInt(cm.getText());

            if (isNaN(purchaseQty) || purchaseQty <= 0) {
                cm.sendOk("Please enter a valid number greater than 0.");
                cm.dispose();
                return;
            }

            if (selectedMaterial == 4) {
                var cost = purchaseQty * WHEEL_PRICE_NX;
                if (cm.getPlayer().getCashShop().getCash(1) < cost) {
                    cm.sendOk("You lack the funds. You need #b" + cost + " NX#k.");
                } else if (!cm.canHold(WHEEL_OF_DESTINY, purchaseQty)) {
                    cm.sendOk("You don't have enough space in your inventory for this purchase.");
                } else {
                    cm.gainCash(-cost);
                    cm.gainItem(WHEEL_OF_DESTINY, purchaseQty);
                    cm.sendOk("You have successfully purchased " + purchaseQty + " Wheel of Destiny.");
                }
                cm.dispose();
                return;
            }

            var materialId = (selectedMaterial === 0) ? ZAKUM_EYE_OF_FIRE :
                (selectedMaterial === 1) ? HORNTAIL_MANA_CRYSTAL :
                    (selectedMaterial === 2) ? PINK_POPPIN :
                        VONLEON_TICKET;
            var maxPurchases = Packages.server.bossshop.BossShopScheduler.getMaterialPurchaseLimit(materialId);

            // Validate if the player has enough remaining purchases
            if (eligibility[selectedMaterial] < purchaseQty) {
                cm.sendOk("Sorry, but you’ve reached the purchase limit for today. You can only buy " + eligibility[selectedMaterial] + " more of this item today.");
                cm.dispose();
                return;
            }

            var itemPrice = 0;
            if (materialId == ZAKUM_EYE_OF_FIRE) itemPrice = ZAKUM_PRICE * 1000000;
            else if (materialId == HORNTAIL_MANA_CRYSTAL) itemPrice = HORNTAIL_PRICE * 1000000;
            else if (materialId == PINK_POPPIN) itemPrice = PINK_POPPIN_PRICE * 1000000;
            else if (materialId == VONLEON_TICKET) itemPrice = VONLEON_TICKET_PRICE * 1000000;

            // Check if the player has enough mesos
            if (cm.getPlayer().getMeso() < itemPrice * purchaseQty) {
                cm.sendOk("You don't have enough mesos for this purchase.");
                cm.dispose();
                return;
            }

            // Inventory check: Ensure the player has space for the items
            if (!cm.canHold(materialId, purchaseQty)) {
                cm.sendOk("You don't have enough space in your inventory for this purchase.");
                cm.dispose();
                return;
            }

            // Attempt to make the purchase (pass to BossShopManager)
            var purchaseResult = Packages.server.bossshop.BossShopManager.buyMat(cm.getPlayer().getId(), materialId, purchaseQty);

            if (purchaseResult === "success") {
                cm.getPlayer().gainMeso(-itemPrice * purchaseQty, true); // Deduct the appropriate amount of mesos
                cm.gainItem(material_list[selectedMaterial], purchaseQty)
                cm.sendOk("You have successfully purchased " + purchaseQty + " " + (selectedMaterial === 0 ? "Zakum Eye of Fire" :
                    selectedMaterial === 1 ? "Horntail Mana Crystal" :
                        selectedMaterial === 2 ? "Pink Poppin" :
                            "Von Leon Ticket") + ".");
            } else {
                cm.sendOk("You have reached the daily purchase limit for this material.");
            }

            cm.dispose();
        }
    }
}
