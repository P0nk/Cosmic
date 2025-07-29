// Merogie Bounty Board NPC
// Supports viewing, creating, and managing bounty quests (flat SQL schema)

var status = -1;
var selected = -1;
var questList = [];
var quest_index = -1;
var quest;

// quest related variables
var searchResults = [];
var rewardtype;

// Quest requirements
var quest_item = -1;
var quest_qty = -1;
var quest_item_blocked = [3020001,3020002,2002031,2002032,2002033,2002034,2002035,2002036];

// Quest rewards
var item1Id = -1;
var item1Qty = -1;
var item2Id = -1;
var item2Qty = -1;
var meso = -1;
var nx = -1;

// Variable to track max amount
var balance = -1;


var qm = Java.type("server.questboard.QuestBoardManager");
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
                      "#g#L0#View Available Bounties#l\r\n" +
                      "#r#L1#Create New Bounty (10M) #l\r\n" +
                      "#b#L2#Manage My Bounties#l");
    } // Menu
    else if (status === 1) {
        // re-routes selection
        if (selection == 0) { // -------------------------------------------- View Available Bounties
            viewAvailableQuests();
            status = 10;
        } else if (selection == 1) {
            // ask what itemid to set for quest requirement
            beginQuestCreation();
            status = 20;
        } else if (selection == 2) {
            manageCreatedQuests();
            status = 30;
        }
    }  // Handles rerouting of menu choices
    else if (status == 11) {
        // Selected quest page: list rewards; option to fulfill or go back to main menu.
        quest_index = selection;
        return actionQuestDetail(quest_index);

    } // Quest selection page
    else if (status == 12) {
        // handles fulfil or go back main menu
        return submitQuest(selection);

    } // Complete quest of go back menu
    else if (status == 21) {
        // list items with clickable results
        return listSearchName();
    } // -------------------------------------------- Create New Bounty
    else if (status == 22) {
        quest_item = selection;
        return itemQuantity()
    } // ask how many of the item to submit Output -> Qty_requirement
    else if (status == 23) {
        console.log(quest_item)
        console.log(cm.isEquipment(quest_item))
        if (cm.isEquipment(quest_item)) {
            console.log('test')
            quest_qty = 1
        }
        else if (quest_qty == -1) {
            quest_qty = Number(cm.getText())
        }
        return rewardType()
    } // ask for reward type (inventories, meso, nx)
    else if (status == 24) {
        rewardtype = selection
        return listInventory(rewardtype)

    } // input reward details (search inventory and list if item)
    else if (status == 25) {

        return rewardsQuantity(rewardtype, selection)
    } // stores the reward into rewardItem list and prompts qty
    else if (status == 26) {
        return storeQuantity(rewardtype);

    } // stores qty and ask if more reward.
    else if (status == 27) {
        return secondReward(selection)

//    } // handles second reward/ posting of quest to sql
//    else if (status == 31) { // -------------------------------------------- Manage My Bounties
//        // claim all button at top
//        // if not, click into quest for option to withdraw if #r or if #b then option to claim.
//    }
//    else if (status == 32) {
//        // Handle dialogue to say item is claimed and all claimed

    }
    else if (status === 31) {
        // Player picked one of their quests
        return handlePlayerQuestSelection(selection);

    } else if (status === 32) {
        // Player chose to claim, withdraw, or go back
        return handleQuestAction(selection);
    }

}

//function viewAvailableQuests() {
//    questList = qm.getOpenQuests();
// //   console.log(questList)
//    if (questList.length === 0) {
//        cm.sendOk("There are no active bounties available right now.");
//        cm.dispose();
//        return;
//    }
//    var msg = "#eAvailable Bounties:#n\r\n";
//    for (var i = 0; i < questList.length; i++) {
//        var q = questList[i];
//        msg += "#L" + i + "#Submit #r" + cm.numberWithCommas(q.requirement_quantity) + "x #i" + q.requirement_itemid + "##k for rewards\r\n";
//    }
//    cm.sendSimple(msg);
//}

function viewAvailableQuests() {
    questList = qm.getOpenQuests();
 //               console.log(questList);

    if (questList.length === 0) {
        cm.sendOk("There are no active bounties available right now.");
        cm.dispose();
        return;
    }

    var msg = "#eAvailable Bounties:#n\r\n";
    for (var i = 0; i < questList.length; i++) {
        var q = questList[i];
 //           console.log(q);
//              var map = playerQuests.get(i);
//              var questId = map.get("quest_id");
//              var status = map.get("status");
//              var isClaimed = map.get("is_req_claimed");
//              var reqItemId = map.get("requirement_itemid");
//              var reqQty = map.get("requirement_quantity");


        var creatorName = q.get("creator_name");
        var qid = q.get("quest_id");

     //       console.log(creatorName);
        msg += "#L" + i + "# Q_ID";
        msg += qid;
        msg += ": Help ";
        msg += "#e" +creatorName + "#n ";
        msg += "Collect #r" + cm.numberWithCommas(q.requirement_quantity) + "x #i" + q.requirement_itemid + "##k\r\n";
    }
       //     console.log(msg);
    cm.sendSimple(msg);
}



function actionQuestDetail(index) {
    quest = questList[index];
    var msg = "#eBounty Details#n\r\n";
    msg += "#dRequirement:#k Submit #r" + cm.numberWithCommas(quest.requirement_quantity) + "x #i" + quest.requirement_itemid + "#\r\n";
    msg += "#gRewards:#k\r\n";
    if (quest.reward_meso > 0) msg += "- #r" + cm.numberWithCommas(quest.reward_meso) + " Mesos#k\r\n";
    if (quest.reward_nx > 0) msg += "- #b" + cm.numberWithCommas(quest.reward_nx) + " NX#k\r\n";
    if (quest.reward_item1_id > 0) msg += "- #i" + quest.reward_item1_id + "# x" + quest.reward_item1_qty + "\r\n";
    if (quest.reward_item2_id > 0) msg += "- #i" + quest.reward_item2_id + "# x" + quest.reward_item2_qty + "\r\n";
    msg += "\r\n#L99#Submit materials#l          #L98#Back#l";
    cm.sendSimple(msg);
    selected = index;
}

function submitQuest(selection) {
    if (selection == 99) {
        if (cm.haveItem(quest.requirement_itemid, quest.requirement_quantity)) {
            console.log(quest.quest_id)
            var canClaim = qm.claimReward(cm.getPlayer(), quest.quest_id)
            if (!canClaim) {
                cm.sendOk("Hey you don't have enough space please make sure you have at least 2 empty slots!")
                return cm.dispose()
            }
            qm.fulfillQuest(cm.getPlayer(), quest.quest_id)
            cm.gainItem(quest.requirement_itemid, -quest.requirement_quantity)

//            if (quest.reward_meso > 0) cm.gainMeso(quest.reward_meso);
//            if (quest.reward_nx > 0) cm.gainCash(quest.reward_nx);
//            if (quest.reward_item1_id > 0) cm.gainItem(quest.reward_item1_id, quest.reward_item1_qty);
//            if (quest.reward_item2_id > 0) cm.gainItem(quest.reward_item2_id, quest.reward_item2_qty);
            return cm.dispose()
        } else {
            cm.sendOk("You do not have the required items!")
            return cm.dispose()
        }
    } else if (selection == 98) {
        status = 0;
        action(1,0,0);
    }
}

// ----------------- Creating new Bounties -----------------
function beginQuestCreation() {
    cm.sendGetText("What item will you add to your collection quest?");

}

function listSearchName() {
    var query = cm.getText().trim().toLowerCase();
    searchResults = [];

    var allItems = qm.getItemInformationProvider();  // List<Pair<Integer,String>>
    var maxResults = 30; // "magical" (34) crashes game

    for each (var itemPair in allItems) {
        var id = itemPair.getLeft();
        var name = itemPair.getRight();


        if (name && name.toLowerCase().includes(query) && Number(id) >= 2000000 && Number(id) != 0 && Number(id) <5000000 && !quest_item_blocked.includes(Number(id))) {
            searchResults.push(itemPair);
            }
    }
    if (searchResults.length > maxResults) {
        cm.sendOk("Too many matches (" + searchResults.length + ") for \"" + query + "\".\r\n" +
                  "Please refine your search to return fewer than " + maxResults + " results.");
        cm.dispose();
        return;
    }
    if (searchResults.length === 0) {
        cm.sendOk("No items found for \"" + query + "\".");
        cm.dispose();
        return;
    }

    var text = "Found " + searchResults.length + " match(es):\r\n";
    for (var i = 0; i < searchResults.length; i++) {
        var item = searchResults[i];
        text += "#L" + item.getLeft() + "##i" + item.getLeft() + "# " + item.getRight() + "#l\r\n";
    }

    cm.sendSimple(text);
}

//} // Status 21

function itemQuantity() {
    if (cm.isEquipment(quest_item)) {
        cm.sendGetNumber("You have selected an Equipment, the maximum amount is 1.", 1, 1, 1)
    } else {
        cm.sendGetText("How many do you need?");  // Status 22
    }
}

function rewardType() {
    if (!cm.isEquipment(quest_item)) {
        if ( Number(cm.getText()) < 1 ) {
            cm.sendOk("Hey! Are you trying to create a quest or test the system! As punishment, you have to repost the quest!")
            return cm.dispose();
        }
    }
    msg = "You are creating a quest with these requirements:\r\n" + "#i" + quest_item + "# x" + quest_qty +"\r\n"
    if (item2Id == -1) {
        cm.sendSimple(msg + "Please select the rewards you wish to grant upon quest completion!\r\n" +
                      "#b#L0#Use#l\r\n" +
                      "#b#L1#Set-up#l\r\n" +
                      "#b#L2#Etc#l\r\n" +
                      "#b#L3#Cash#l\r\n" +
                      "#b#L4#Mesos#l\r\n" +
                      "#b#L5#Nx#l\r\n")
    } else {
        cm.sendSimple(msg + "Please select the rewards you wish to grant upon quest completion!\r\n" +
                      "#b#L0#Mesos#l\r\n" +
                      "#b#L1#Nx#l\r\n")
    }
} // Status 23

function listInventory(rewardtype) {
    rewardtype = (item2Id == -1) ? rewardtype : rewardtype + 4
    if (rewardtype <= 3) { // Items
        var inv      = cm.getInventory(rewardtype + 2); // 2-USE; 3-SETUP; 4-ETC; 5-CASH
        var limit    = inv.getSlotLimit();
        var lines    = [];
        for (var slot = 1; slot <= limit; slot++) {
            var item = inv.getItem(slot);
            if (!item) continue;
            if (item.getItemId() == item1Id) continue;
            var name = Packages.server.ItemInformationProvider
                       .getInstance().getName(item.getItemId());
            lines.push(
                "#L" + slot + "#"
                + "#v" + item.getItemId() + "# "
                + name
                + "#l"
            );
        }
        cm.sendSimple(
            "Select the item you wish to give as a reward.\r\n"
          + lines.join("\r\n")
        );
    } else if (rewardtype == 4) { // Mesos
        balance = cm.getMeso();
        cm.sendGetText("How much Mesos do you want to give? (Max " + cm.numberWithCommas(balance) + ")");
        status = 25;
    } else if (rewardtype == 5) { // NX
        balance = cm.getCashShop().getCash(1)
        cm.sendGetText("How much NX do you want to give? (Max " + cm.numberWithCommas(balance) + ")");
        status = 25;
    }
} // Status 24

function rewardsQuantity(rewardtype, slot) {
    var inv     = cm.getInventory(rewardtype + 2); // 2-USE; 3-SETUP; 4-ETC; 5-CASH
    var item    = inv.getItem(slot);
    var name    = Packages.server.ItemInformationProvider
                   .getInstance().getName(item.getItemId());
    balance = cm.getPlayer().getItemQuantity(item.getItemId(), false)
    // Store the reward to the variables
    if (item1Id == -1) {
        item1Id = item.getItemId()
    } else {
        item2Id = item.getItemId()
    }
    cm.sendGetText("How much of #i" + item.getItemId() + "# do you want to give as a reward? (Max " + balance + ")")
} // status 25

function storeQuantity() {
    rewardtype = (item2Id == -1 || item2Qty == -1) ? rewardtype : rewardtype + 4
    storedItem = (item2Id == -1) ? item1Id : item2Id
    if (balance < Number(cm.getText()) ||  Number(cm.getText()) < 1) {
        cm.sendOk("Hey! You entered more than you have! As a punishment for being greedy, you have to repost the quest!")
        return cm.dispose()
    } else if (rewardtype < 4 && Number(cm.getText()) > 32000) {
        cm.sendOk("Hey! You can't enter more than 32,000!")
        return cm.dispose()
    }
    if (rewardtype < 4) {
        msg = "You have registered x" + cm.getText() + " #i" + storedItem + "# as a reward."
        if (item1Qty == -1) {
            item1Qty = Number(cm.getText());
        } else {
            item2Qty = Number(cm.getText());
        }
    }
    else if (rewardtype === 4) {
        meso = Number(cm.getText());
        msg = "You have registered " + cm.numberWithCommas(meso) + " mesos as a reward."
    } else if (rewardtype === 5) {
        nx = Number(cm.getText());
        msg = "You have registered " + cm.numberWithCommas(nx) + " nx as a reward."
    }

    cm.sendSimple("You have added these rewards to the quest\r\n" +
                  checkExistingRewards() +
                  "\r\nDo you wish to add more rewards?\r\n" +
                  "#b#L0#Yes#l\r\n" +
                  "#b#L1#No, proceed to post quest#l\r\n")
} // status 26

function secondReward(selection) {
    if (selection == 0) {
        status = 22;
        action(1,0,0);
    } else if (selection == 1) {
            balance = cm.getMeso();
        if (balance < meso + 10000000){
        cm.sendOk("Insufficent Funds to Pay Questboard Tax!")
        return cm.dispose()
        }
        // Post quest
        qm.createQuest(cm.getPlayer(), quest_item, quest_qty, meso, nx, item1Id, item1Qty, item2Id, item2Qty)
        cm.sendOk("Quest has been posted!")
        removeRewards()
        return cm.dispose()
    }
} // status 27

// ----------------- Manage Bounties -----------------
function manageCreatedQuests() {
    cm.sendOk("[WIP] Management of created quests will be available here.");
    console.log(qm.getPlayerQuests())
    cm.dispose();
}


// ----------------- Debug check -----------------
function checkExistingRewards() {
    rewards = []
    if (item1Id != -1) {
        rewards.push("#i" + item1Id + "# x" + item1Qty);
    }
    if (item2Id != -1) {
        rewards.push("#i" + item2Id + "# x" + item2Qty);
    }
    if (meso != -1) {
        rewards.push(cm.numberWithCommas(meso) + " mesos")
    }
    if (nx != -1) {
        rewards.push(cm.numberWithCommas(nx) + " nx")
    }
    return rewards.join("\r\n")
}

function removeRewards() {
    if (item1Id != -1) {
        cm.gainItem(item1Id, -item1Qty)
    }
    if (item2Id != -1) {
        cm.gainItem(item2Id, -item2Qty)
    }
    if (meso != -1) {
        cm.gainMeso(-meso)
    }
    if (nx != -1) {
        cm.gainCash(-nx);
    }
}


// -- Manage Created Bounties
var playerQuests = [];
var selectedQuestId = -1;
var ItemInfoProvider = Java.type("server.ItemInformationProvider").getInstance();

function manageCreatedQuests() {
    playerQuests = qm.getPlayerQuests(cm.getPlayer());
    console.log(playerQuests);
    var total = playerQuests.size();
    console.log("[ManageQuests] Retrieved " + total + " quests for player.");

    if (total === 0) {
        cm.sendOk("You haven't posted any bounty quests yet.");
        cm.dispose();
        return;
    }

    var msg = "#eYour Bounty Quests:#n\r\n";
    var skipped = 0;
    var canClaim = false;
    var canWithdraw = false;
    for (var i = 0; i < total; i++) {
        var map = playerQuests.get(i);
        var questId = map.get("quest_id");
        var status = map.get("status");
        var isClaimed = map.get("is_req_claimed");
        var reqItemId = map.get("requirement_itemid");
        var reqQty = map.get("requirement_quantity");


        if (status === "WITHDRAWN" || isClaimed != 0) {
            skipped++;
            continue;
        }

        // Get item name
        var itemName = ItemInfoProvider.getName(reqItemId);
        if (!itemName) itemName = "(Unknown Item)";

        // Add icon + name + quantity
        msg += "#L" + i + "#"
             + "#v" + reqItemId + "# "  // item icon
             + itemName + " x" + reqQty + " : " + status;

        if (status === "COMPLETED" && isClaimed == 0) {
            canClaim = true;
            msg += " #b(Claim)#k";
        } else if (status === "OPEN") {
            canWithdraw = true;
            msg += " #r(Withdraw)#k";
        }

        msg += "#l\r\n";
        // Log debug info
        console.log("[ManageQuests] #" + questId + ": " + itemName + " x" + reqQty + " [" + status + "]");
    }
    //console.log("[ManageQuests] Skipped " + skipped + " quests.");
    if (canClaim || canWithdraw) {
        cm.sendSimple(msg);
    } else {
        cm.sendOk("You do not have any quest posted or to claim.")
        return cm.dispose()
    }

}


// Handles selection from player's posted quests
function handlePlayerQuestSelection(selection) {
    var quest = playerQuests[selection];

    selectedQuestId = quest.get("quest_id");

    var options = "";

    if (quest.get("status") === "COMPLETED" && quest.get("is_req_claimed") == 0) {
        options += "#L0#Claim submitted materials#l\r\n";
    }

    if (quest.get("status") === "OPEN") {
        options += "#L1#Withdraw this quest#l\r\n";
    }

    options += "#L2#Back to quest list#l";
    cm.sendSimple("What would you like to do with Quest #" + selectedQuestId + "?\r\n\r\n" + options);
}

// Handle action after selecting a quest option
function handleQuestAction(selection) {
    if (selection === 0) {
        // Claim requirements
        var success = qm.claimRequirements(cm.getPlayer(), selectedQuestId);
        cm.sendOk(success ? "You've reclaimed the required items for this quest!" : "Unable to claim — either already claimed or not completed.");
        return cm.dispose();
    } else if (selection === 1) {
        // Withdraw
        var withdrawn = qm.withdrawQuest(cm.getPlayer(), selectedQuestId);
        cm.sendOk(withdrawn ? "Quest withdrawn successfully." : "Unable to withdraw this quest.");
        return cm.dispose();
    } else if (selection === 2) {
        // Go back
        status = 30; // reload menu
        action(1,0,0)
    }
}