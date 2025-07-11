// Merogie Bounty Board NPC
// Supports viewing, creating, and managing bounty quests (flat SQL schema)

var status = -1;
var selected = -1;
var questList = [];
var QUEST_MANAGER = Java.type("server.questboard.QuestBoardManager");
var ItemInfoProvider = Java.type("server.ItemInformationProvider").getInstance();

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }
    status++;

    if (status === 0) {
        cm.sendSimple("#eMerogie Bounty Board#n\r\nWhat would you like to do?\r\n" +
                      "#L0#📋 View Available Bounties#l\r\n" +
                      "#L1#📝 Create New Bounty (10M) #l\r\n" +
                      "#L2#📦 Manage My Bounties#l");
    } else if (status === 1) {
        selected = selection;
        switch (selected) {
            case 0:
                return viewAvailableQuests();
            case 1:
                return beginQuestCreation();
            case 2:
                return manageCreatedQuests();
        }
    }
}

function viewAvailableQuests() {
    questList = QUEST_MANAGER.getOpenQuests();
    if (questList.length === 0) {
        cm.sendOk("There are no active bounties available right now.");
        cm.dispose();
        return;
    }
    var msg = "#eAvailable Bounties:#n\r\n";
    for (var i = 0; i < questList.length; i++) {
        var q = questList[i];
        msg += "#L" + i + "#Submit #r" + q.requirement_quantity + "x #z" + q.requirement_itemid + "##k for rewards\r\n";
    }
    cm.sendSimple(msg);
    status = 10;
}

function actionQuestDetail(index) {
    var quest = questList[index];
    var msg = "#eBounty Details#n\r\n";
    msg += "#dRequirement:#k Submit " + quest.requirement_quantity + "x #z" + quest.requirement_itemid + "#\r\n";
    msg += "#gRewards:#k\r\n";
    if (quest.reward_meso > 0) msg += "- #r" + cm.numberWithCommas(quest.reward_meso) + " Mesos#k\r\n";
    if (quest.reward_nx > 0) msg += "- #b" + cm.numberWithCommas(quest.reward_nx) + " NX#k\r\n";
    if (quest.reward_item1_id > 0) msg += "- #z" + quest.reward_item1_id + "# x" + quest.reward_item1_qty + "\r\n";
    if (quest.reward_item2_id > 0) msg += "- #z" + quest.reward_item2_id + "# x" + quest.reward_item2_qty + "\r\n";
    msg += "\r\n#L99#✅ Submit materials#l  |  #L98#↩ Back#l";
    cm.sendSimple(msg);
    status = 11;
    selected = index;
}

function beginQuestCreation() {
    cm.sendOk("[WIP] Quest creation flow will be implemented here.");
    cm.dispose();
}

function manageCreatedQuests() {
    cm.sendOk("[WIP] Management of created quests will be available here.");
    cm.dispose();
}

// Status redirects
if (status === 10) {
    actionQuestDetail(selection);
} else if (status === 11 && selection === 98) {
    start();
} else if (status === 11 && selection === 99) {
    cm.sendOk("[WIP] Fulfillment logic here.");
    cm.dispose();
}






var status = 0;
var inputStage = 0;

var QUEST_MANAGER = Java.type("server.questboard.QuestBoardManager");
var ItemInfoProvider = Java.type("server.ItemInformationProvider").getInstance();

var questId = -1;
var title = "";
var description = "";
var deadline = null;
var repeatable = false;
var maxCompletions = 1;
var requirements = [];
var itemRewards = [];
var currencyRewards = [];

//function start() {
//    if (blacklist.includes(cm.getPlayer().getMapId())) {
//        cm.sendOk("Hmm I am still an apprentice.... I can't handle boss maps.");
//        cm.dispose();
//        return;
//    }
//    accountId = cm.getAccountIdByCharacterName(cm.getName());
//    currentMapId = cm.getMapId();
//    limit = cm.getMapLimit(accountId);
//    var count = cm.getSavedMaps(accountId).length;
//    status = 0;
//    cm.sendSimple("Hey there, apprentice wizard here. I can teleport you to maps you have been to before.\r\nWhat would you like to do? (" + count + "/" + limit + ")\r\n" +
//                  "#b#L0#Teleport to a saved map#l\r\n" +
//                  "#L1#Save current map#l\r\n" +
//                  "#L2#Manage Created Quest#l\r\n");
//}

function start() {

 // starting variables
    status = 0;
    inputStage = 0;
    questId = -1;
    requirements = [];
    itemRewards = [];
    currencyRewards = [];
    status = -1;
    action(1, 0, 0);

 cm.sendSimple("Notice Reads: This is the Bounty Board. You may create new bounties, view and complete existing bounties here. Please note that it will cost 10M Mesos to create a new bounty.  \r\nWhat would you like to do? \r\n" +
                   "#b#L0#View Available Bounties#l\r\n" +
                   "#L1#Create New Bounty Quest#l\r\n" +
                   "#L2#[Quest Issuer] Collect Materials#l");
}


function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    if (status === 1) {
//        currentMapId = cm.getMapId();
        switch (selection) {
            case 0: // View Available Bounties
            // list all quest can click into each quest for reward..
            // s quest name is requirement and quantity.
            // option to fulfill or go back to main menu.



            case 1: // Create New Bounty Quest

                                // ask what itemid to set for quest requirement
                                // ask how many of the item to submit Output -> Qty_requirement
                                /// ask if have more requirements (loop back to start)
                                // ask for reward type (inventories, meso, nx)
                                // input reward details (search inventory and list if item)
                                // ask if more reward.
                                // tax - 10m per quest creation.
                                // create quest.





            case 2: // Manage Created Quest


                 // retrieve all quest by player
                 // for i in quest if completed #b else #r #g if completed and claimed.
                 // claim all button at top
                 // if not, click into quest for option to withdraw if #r or if #b then option to claim.


        }



function action(mode, type, selection) {
    if (mode !== 1)
    return
    cm.dispose();
    status++;

    switch (status) {
        case 1:
            title = cm.getText();
            cm.sendNext("Great! Now give a brief description of your quest.");
            break;
        case 2:
            description = cm.getText();
            cm.sendYesNo("Would you like to set a deadline?");
            break;
        case 3:
            if (cm.getYesNo()) {
                cm.sendGetText("Please input the deadline in YYYY-MM-DD format:");
            } else {
                deadline = null;
                status = 4; // Skip to repeatable
                action(1, 0, 0);
            }
            break;
        case 4:
            if (deadline === null) {
                deadline = null;
            } else {
                var dateInput = cm.getText();
                try {
                    deadline = java.sql.Timestamp.valueOf(dateInput + " 23:59:59");
                } catch (e) {
                    cm.sendOk("Invalid date. Please try again.");
                    status--;
                    return;
                }
            }
            cm.sendYesNo("Should this quest be repeatable?");
            break;
        case 5:
            repeatable = cm.getYesNo();
            cm.sendGetNumber("What is the max number of times a player can complete this quest?", 1, 1, 999);
            break;
        case 6:
            maxCompletions = selection;
            cm.sendYesNo("Ready to enter quest requirements?");
            break;
        case 7:
            if (cm.getYesNo()) {
                cm.sendGetNumber("Enter the Item ID for the first requirement:", 1000000, 1000000, 9999999);
                inputStage = 1;
            } else {
                status = 10;
                action(1, 0, 0);
            }
            break;

        // -- Collect Requirements --
        case 8:
            if (inputStage === 1) {
                requirements.push({ itemId: selection, quantity: 0 });
                cm.sendGetNumber("Enter the quantity required for this item:", 1, 1, 999);
                inputStage = 2;
            } else if (inputStage === 2) {
                requirements[requirements.length - 1].quantity = selection;
                cm.sendYesNo("Add another requirement?");
                inputStage = 3;
            } else if (inputStage === 3) {
                if (cm.getYesNo()) {
                    cm.sendGetNumber("Enter the Item ID for the next requirement:", 1000000, 1000000, 9999999);
                    inputStage = 1;
                    status = 7;
                } else {
                    status = 10;
                    action(1, 0, 0);
                }
            }
            break;

        // -- Collect Rewards --
        case 10:
            cm.sendYesNo("Ready to add item rewards?");
            break;
        case 11:
            if (cm.getYesNo()) {
                cm.sendGetNumber("Enter the Item ID to reward:", 1000000, 1000000, 9999999);
                inputStage = 4;
            } else {
                status = 14;
                action(1, 0, 0);
            }
            break;
        case 12:
            if (inputStage === 4) {
                itemRewards.push({ itemId: selection, quantity: 0 });
                cm.sendGetNumber("Enter the quantity to reward:", 1, 1, 999);
                inputStage = 5;
            } else if (inputStage === 5) {
                itemRewards[itemRewards.length - 1].quantity = selection;
                cm.sendYesNo("Add another item reward?");
                inputStage = 6;
            } else if (inputStage === 6) {
                if (cm.getYesNo()) {
                    cm.sendGetNumber("Enter another Item ID to reward:", 1000000, 1000000, 9999999);
                    inputStage = 4;
                    status = 11;
                } else {
                    status = 14;
                    action(1, 0, 0);
                }
            }
            break;

        // -- Currency Rewards --
        case 14:
            cm.sendYesNo("Do you want to reward Mesos?");
            break;
        case 15:
            if (cm.getYesNo()) {
                cm.sendGetNumber("How many Mesos?", 1000, 1000, 2100000000);
                inputStage = 7;
            } else {
                status = 17;
                action(1, 0, 0);
            }
            break;
        case 16:
            currencyRewards.push({ type: "MESO", amount: selection, nxType: 0 });
            status = 17;
            action(1, 0, 0);
            break;

        case 17:
            cm.sendYesNo("Do you want to reward NX?");
            break;
        case 18:
            if (cm.getYesNo()) {
                cm.sendGetNumber("How much NX to reward?", 100, 100, 1000000);
                inputStage = 8;
            } else {
                status = 20;
                action(1, 0, 0);
            }
            break;
        case 19:
            currencyRewards.push({ type: "NX", amount: selection, nxType: 1 });
            status = 20;
            action(1, 0, 0);
            break;

        // -- COMMIT TO DATABASE --
        case 20:
            try {
                questId = QUEST_MANAGER.getMaxQuestId() + 1;
                var taxPaid = 1000000; // flat rate for now

                QUEST_MANAGER.insertQuest(questId, title, description, cm.getPlayer().getId(), cm.getPlayer().isGM(), deadline, repeatable, maxCompletions, taxPaid);

                for (var i = 0; i < requirements.length; i++) {
                    var r = requirements[i];
                    QUEST_MANAGER.insertRequirement(questId, r.itemId, r.quantity);
                }

                for (var i = 0; i < itemRewards.length; i++) {
                    var ir = itemRewards[i];
                    QUEST_MANAGER.insertItemReward(questId, ir.itemId, ir.quantity);
                }

                for (var i = 0; i < currencyRewards.length; i++) {
                    var cr = currencyRewards[i];
                    QUEST_MANAGER.insertCurrencyReward(questId, cr.type, cr.amount, cr.nxType);
                }

                cm.sendOk("Your quest has been successfully registered! Quest ID: " + questId);
            } catch (e) {
                cm.sendOk("An error occurred while saving the quest: " + e);
            }
            cm.dispose();
            break;
    }
}
