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
var quest_item = 0;
var quest_qty = 0;

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
        action = selection;
        return submitQuest(action);

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
        quest_qty = Number(cm.getText())
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

    } // handles second reward/ posting of quest to sql
    else if (status == 31) { // -------------------------------------------- Manage My Bounties
        // claim all button at top
        // if not, click into quest for option to withdraw if #r or if #b then option to claim.
    }
    else if (status == 32) {
        // Handle dialogue to say item is claimed and all claimed

    }
}

function viewAvailableQuests() {
    questList = qm.getOpenQuests();
    console.log(questList)
    if (questList.length === 0) {
        cm.sendOk("There are no active bounties available right now.");
        cm.dispose();
        return;
    }
    var msg = "#eAvailable Bounties:#n\r\n";
    for (var i = 0; i < questList.length; i++) {
        var q = questList[i];
        msg += "#L" + i + "#Submit #r" + cm.numberWithCommas(q.requirement_quantity) + "x #i" + q.requirement_itemid + "##k for rewards\r\n";
    }
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

function submitQuest(action) {
    if (action == 99) {
        if (cm.haveItem(quest.requirement_itemid, quest.requirement_quantity)) {
            qm.fulfillQuest(cm.getPlayer(), quest.quest_id)
            cm.gainItem(quest.requirement_itemid, -quest.requirement_quantity)
            if (quest.reward_meso > 0) cm.gainMeso(quest.reward_meso);
            if (quest.reward_nx > 0) cm.gainCash(quest.reward_nx);
            if (quest.reward_item1_id > 0) cm.gainItem(quest.reward_item1_id, quest.reward_item1_qty);
            if (quest.reward_item2_id > 0) cm.gainItem(quest.reward_item2_id, quest.reward_item2_qty);
            return cm.dispose()
        } else {
            cm.sendOk("You do not have the required items!")
            return cm.dispose()
        }
    } else if (action == 98) {
        status = 10
        action(1,0,0)
    }
}

// ----------------- Creating new Bounties -----------------
function beginQuestCreation() {
    cm.sendGetText("What item will you add to your collection quest?");

}

function listSearchName() {
    var query = cm.getText().trim().toLowerCase(); // get the player input
    var allItems = qm.getItemInformationProvider();  // List<Pair<Integer,String>>
    var maxResults = 50
//    searchResults = [];
    for each (var itemPair in allItems) {
        var id   = itemPair.getLeft();
        var name = itemPair.getRight();
        // stop if too many to avoid overrunning packet size
        if (searchResults.length > 99) {
            break;
        } else if (name.toLowerCase().indexOf(query) !== -1) {
            searchResults.push(itemPair);
        }
    }
    if (searchResults.length === 0) {
        cm.sendOk("No items found matching: #b" + query + "#k");
        cm.dispose();
    } else {
        var text = "Found " + searchResults.length + " matches for \"#b" + query + "#k\":";
        for (var i = 0; i < searchResults.length && i < maxResults; i++) {
            var p = searchResults[i];
            var itemId   = p.getLeft();
            var itemName = p.getRight();
            // #L<id># … #l = clickable; #i<id># = icon
            text += "\r\n#L" + itemId + "##i" + itemId + "# " + itemName + "#l";
        }
        if (searchResults.length > maxResults) {
//            text += "\r\n\r\n... and " + (searchResults.length - maxResults) + " more.";
            text += "\r\n\r\n... and more. Try to be more specific if you can't find your item";
        }
        cm.sendSimple(text);
    }
} // Status 21

function itemQuantity() { cm.sendGetText("How many do you need?"); } // Status 22

function rewardType() {
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
    if (balance < Number(cm.getText())) {
        cm.sendOk("Hey! You entered more than you have! As a punishment for being greedy, you have to repost the quest!")
        return cm.dispose()
    } else if (Number(cm.getText()) > 32000) {
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