// Merogie Bounty Board NPC
// Supports viewing, creating, and managing bounty quests (flat SQL schema)

var status = -1;
var selected = -1;
var questList = [];

// quest related variables
var searchResults = [];
var rewardtype;
// Quest requirements
var quest_item = 0;
var quest_qty = 0;

// Quest rewards
var rewardItem = [];
var quantity = [];
var numOfRewards = 0;


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
        quest_id = selection;
        return actionQuestDetail(quest_id);

    } // Quest selection page
    else if (status == 12) {
        // handles fulfil or go back main menu
        action = selection;
        return questPageFinal(action);

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
        quest_qty = cm.getText()
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

    } // handles second reward
    else if (status == 28) {
        // create quest.

    }
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
        msg += "#L" + i + "#Submit #r" + q.requirement_quantity + "x #z" + q.requirement_itemid + "##k for rewards\r\n";
    }
    cm.sendSimple(msg);
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
    selected = index;
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
        console.log(text)
        if (searchResults.length > maxResults) {
//            text += "\r\n\r\n... and " + (searchResults.length - maxResults) + " more.";
            text += "\r\n\r\n... and more. Try to be more specific if you can't find your item";
        }
        cm.sendSimple(text);
    }
}

function itemQuantity() { cm.sendGetText("How many do you need?"); }

function rewardType() {
    msg = "You are creating a quest with these requirements:\r\n" + "#i" + quest_item + "# x" + quest_qty +"\r\n"
    cm.sendSimple(msg + "Please select the rewards you wish to grant upon quest completion!\r\n" +
                  "#b#L0#Use#l\r\n" +
                  "#b#L1#Set-up#l\r\n" +
                  "#b#L2#Etc#l\r\n" +
                  "#b#L3#Cash#l\r\n" +
                  "#b#L4#Mesos#l\r\n" +
                  "#b#L5#Nx#l\r\n")
}

function listInventory(rewardtype) {
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
        cm.sendGetText("How much Mesos do you want to give?");
        status = 25;
    } else if (rewardtype == 5) { // NX
        cm.sendGetText("How much NX do you want to give?");
        status = 25;
    }
}

function rewardsQuantity(rewardtype, slot) {
    var inv     = cm.getInventory(rewardtype + 2); // 2-USE; 3-SETUP; 4-ETC; 5-CASH
    var item    = inv.getItem(slot);
    var name    = Packages.server.ItemInformationProvider
                   .getInstance().getName(item.getItemId());
//        console.log(name)
    rewardItem.push(item.getItemId())
    cm.sendGetText("How much of #i" + item.getItemId() + "# do you want to give as a reward?")
}

function storeQuantity() {
    if (rewardtype < 4) {
        msg = "You have registered x" + cm.getText() + " #i" + rewardItem[0] + "# as a reward."
    }
    else if (rewardtype === 4) {
        rewardItem.push("Mesos")
        msg = "You have registered " + cm.numberWithCommas(cm.getText()) + " mesos as a reward."
    } else if (rewardtype === 5) {
        rewardItem.push("NX")
        msg = "You have registered " + cm.numberWithCommas(cm.getText()) + " nx as a reward."
    }
    quantity.push(cm.getText())
    numOfRewards = numOfRewards + 1;
    if (numOfRewards < 2) {
        cm.sendSimple(msg + "\r\nDo you wish to add more rewards?\r\n" +
                      "#b#L0#Yes#l\r\n" +
                      "#b#L1#No, proceed to post quest#l\r\n")
    } else if (numOfRewards === 2) {
        lines = [];
        for (var i = 0; i < 2; i++) {
            text = "#i" + rewardItem[i] + "#  x" + quantity[i]
            line.push(text)
        }
        msg = "You have registered this items as a reward:\r\n" + lines.join("\r\n") +
              "#b#L0#Post quest?#l\r\n" +
              "#b#L1#No#l"
        cm.sendSimple(msg)
    }
}

function secondReward(selection) {
    if (selection == 0) {
        qm.createQuest(cm.getPlayer(), quest_item, quest_qty, meso, nx, item1Id, item1Qty, item2Id, item2Qty)

    }
}
// ----------------- Manage Bounties -----------------
function manageCreatedQuests() {
    cm.sendOk("[WIP] Management of created quests will be available here.");
    cm.dispose();
}