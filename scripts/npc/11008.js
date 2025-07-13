// Merogie Bounty Board NPC (Production Version)

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

// Quest rewards
var item1Id = -1;
var item1Qty = -1;
var item2Id = -1;
var item2Qty = -1;
var meso = -1;
var nx = -1;
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
                      "#r#L1#Create New Bounty (10M)#l\r\n" +
                      "#b#L2#Manage My Bounties#l");
    } else if (status === 1) {
        if (selection == 0) {
            viewAvailableQuests(); status = 10;
        } else if (selection == 1) {
            beginQuestCreation(); status = 20;
        } else if (selection == 2) {
            manageCreatedQuests(); status = 30;
        }
    } else if (status == 11) {
        quest_index = selection;
        return actionQuestDetail(quest_index);
    } else if (status == 12) {
        return submitQuest(selection);
    } else if (status == 21) {
        return listSearchName();
    } else if (status == 22) {
        quest_item = selection;
        return itemQuantity();
    } else if (status == 23) {
        if (quest_qty == -1) quest_qty = Number(cm.getText());
        return rewardType();
    } else if (status == 24) {
        rewardtype = selection;
        return listInventory(rewardtype);
    } else if (status == 25) {
        return rewardsQuantity(rewardtype, selection);
    } else if (status == 26) {
        return storeQuantity(rewardtype);
    } else if (status == 27) {
        return secondReward(selection);
    } else if (status === 31) {
        return handlePlayerQuestSelection(selection);
    } else if (status === 32) {
        return handleQuestAction(selection);
    }
}

// ----- View Quests
function viewAvailableQuests() {
    questList = qm.getOpenQuests();
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

function submitQuest(selection) {
    if (selection == 99) {
        if (cm.haveItem(quest.requirement_itemid, quest.requirement_quantity)) {
            qm.fulfillQuest(cm.getPlayer(), quest.quest_id);
            cm.gainItem(quest.requirement_itemid, -quest.requirement_quantity);
            qm.claimReward(cm.getPlayer(), quest.quest_id);
            return cm.dispose();
        } else {
            cm.sendOk("You do not have the required items!");
            return cm.dispose();
        }
    } else if (selection == 98) {
        status = 0;
        action(1,0,0);
    }
}

// ----- Quest Creation
function beginQuestCreation() { cm.sendGetText("What item will you add to your collection quest?"); }
function itemQuantity() { cm.sendGetText("How many do you need?"); }

function rewardType() {
    var msg = "You are creating a quest with these requirements:\r\n" + "#i" + quest_item + "# x" + quest_qty + "\r\n";
    if (item2Id == -1) {
        cm.sendSimple(msg + "Please select the rewards:\r\n#b#L0#Use#l\r\n#L1#Set-up#l\r\n#L2#Etc#l\r\n#L3#Cash#l\r\n#L4#Mesos#l\r\n#L5#Nx#l");
    } else {
        cm.sendSimple(msg + "Please select the rewards:\r\n#b#L0#Mesos#l\r\n#L1#Nx#l");
    }
}

function listSearchName() {
    var query = cm.getText().trim().toLowerCase();
    searchResults = [];
    var allItems = qm.getItemInformationProvider();
    var maxResults = 30;

    for each (var itemPair in allItems) {
        var id = itemPair.getLeft();
        var name = itemPair.getRight();
        if (name && name.toLowerCase().includes(query)) {
            searchResults.push(itemPair);
        }
    }

    if (searchResults.length > maxResults) {
        cm.sendOk("Too many matches (" + searchResults.length + "). Please refine your search.");
        cm.dispose(); return;
    }
    if (searchResults.length === 0) {
        cm.sendOk("No items found for \"" + query + "\".");
        cm.dispose(); return;
    }

    var text = "Found " + searchResults.length + " match(es):\r\n";
    for (var i = 0; i < searchResults.length; i++) {
        var item = searchResults[i];
        text += "#L" + item.getLeft() + "##i" + item.getLeft() + "# " + item.getRight() + "#l\r\n";
    }

    cm.sendSimple(text);
}

function listInventory(rewardtype) {
    rewardtype = (item2Id == -1) ? rewardtype : rewardtype + 4;
    if (rewardtype <= 3) {
        var inv = cm.getInventory(rewardtype + 2);
        var limit = inv.getSlotLimit();
        var lines = [];
        for (var slot = 1; slot <= limit; slot++) {
            var item = inv.getItem(slot);
            if (!item) continue;
            var name = ItemInfoProvider.getName(item.getItemId());
            lines.push("#L" + slot + "#" + "#v" + item.getItemId() + "# " + name + "#l");
        }
        cm.sendSimple("Select the item you wish to give as a reward:\r\n" + lines.join("\r\n"));
    } else if (rewardtype == 4) {
        balance = cm.getMeso();
        cm.sendGetText("Enter Mesos (Max " + cm.numberWithCommas(balance) + ")");
        status = 25;
    } else if (rewardtype == 5) {
        balance = cm.getCashShop().getCash(1);
        cm.sendGetText("Enter NX (Max " + cm.numberWithCommas(balance) + ")");
        status = 25;
    }
}

function rewardsQuantity(rewardtype, slot) {
    var inv = cm.getInventory(rewardtype + 2);
    var item = inv.getItem(slot);
    var name = ItemInfoProvider.getName(item.getItemId());
    balance = cm.getPlayer().getItemQuantity(item.getItemId(), false);
    if (item1Id == -1) item1Id = item.getItemId();
    else item2Id = item.getItemId();
    cm.sendGetText("How much of #i" + item.getItemId() + "# (Max " + balance + ")");
}

function storeQuantity() {
    rewardtype = (item2Id == -1 || item2Qty == -1) ? rewardtype : rewardtype + 4;
    var storedItem = (item2Id == -1) ? item1Id : item2Id;
    var qty = Number(cm.getText());

    if (rewardtype < 4) {
        if (balance < qty || qty > 32000) {
            cm.sendOk("Invalid quantity. Quest creation cancelled.");
            return cm.dispose();
        }
        if (item1Qty == -1) item1Qty = qty;
        else item2Qty = qty;
    } else if (rewardtype === 4) meso = qty;
    else if (rewardtype === 5) nx = qty;

    cm.sendSimple("You have added:\r\n" + checkExistingRewards() +
                  "\r\nAdd more rewards?\r\n#b#L0#Yes#l\r\n#L1#No, post quest#l");
}

function secondReward(selection) {
    if (selection == 0) {
        status = 22; action(1,0,0);
    } else if (selection == 1) {
        qm.createQuest(cm.getPlayer(), quest_item, quest_qty, meso, nx, item1Id, item1Qty, item2Id, item2Qty);
        cm.sendOk("Quest posted!");
        removeRewards(); cm.dispose();
    }
}

function checkExistingRewards() {
    var rewards = [];
    if (item1Id != -1) rewards.push("#i" + item1Id + "# x" + item1Qty);
    if (item2Id != -1) rewards.push("#i" + item2Id + "# x" + item2Qty);
    if (meso != -1) rewards.push(cm.numberWithCommas(meso) + " mesos");
    if (nx != -1) rewards.push(cm.numberWithCommas(nx) + " nx");
    return rewards.join("\r\n");
}

function removeRewards() {
    if (item1Id != -1) cm.gainItem(item1Id, -item1Qty);
    if (item2Id != -1) cm.gainItem(item2Id, -item2Qty);
    if (meso != -1) cm.gainMeso(-meso);
    if (nx != -1) cm.gainCash(-nx);
}

// ----- Manage Created Quests
var playerQuests = [];
var selectedQuestId = -1;

function manageCreatedQuests() {
    playerQuests = qm.getPlayerQuests(cm.getPlayer());
    var total = playerQuests.size();
    //console.log("[ManageQuests] Retrieved " + total + " quests.");

    if (total === 0) {
        cm.sendOk("You haven't posted any bounty quests yet.");
        cm.dispose(); return;
    }

    var msg = "#eYour Bounty Quests:#n\r\n";
    var skipped = 0;

    for (var i = 0; i < total; i++) {
        var map = playerQuests.get(i);
        var status = map.get("status");
        var claimed = map.get("is_req_claimed");
        var itemId = map.get("requirement_itemid");
        var qty = map.get("requirement_quantity");

        if (status === "WITHDRAWN" || claimed != 0) {
            skipped++; continue;
        }

        var itemName = ItemInfoProvider.getName(itemId) || "(Unknown)";
        msg += "#L" + i + "# #v" + itemId + "# " + itemName + " x" + qty + " : " + status;

        if (status === "COMPLETED" && claimed == 0) msg += " #b(Claim)#k";
        else if (status === "OPEN") msg += " #r(Withdraw)#k";
        msg += "#l\r\n";

        //console.log("[ManageQuests] #" + map.get("quest_id") + ": " + itemName + " x" + qty + " [" + status + "]");
    }

    //console.log("[ManageQuests] Skipped " + skipped + " quests.");
    cm.sendSimple(msg);
}

function handlePlayerQuestSelection(selection) {
    var quest = playerQuests[selection];
    selectedQuestId = quest.get("quest_id");

    var options = "";
    if (quest.get("status") === "COMPLETED" && quest.get("is_req_claimed") == 0) options += "#L0#Claim submitted materials#l\r\n";
    if (quest.get("status") === "OPEN") options += "#L1#Withdraw this quest#l\r\n";
    options += "#L2#Back to quest list#l";

    cm.sendSimple("What would you like to do with Quest #" + selectedQuestId + "?\r\n\r\n" + options);
}

function handleQuestAction(selection) {
    if (selection === 0) {
        var success = qm.claimRequirements(cm.getPlayer(), selectedQuestId);
        cm.sendOk(success ? "You've reclaimed the required items!" : "Unable to claim items.");
        cm.dispose();
    } else if (selection === 1) {
        var withdrawn = qm.withdrawQuest(cm.getPlayer(), selectedQuestId);
        cm.sendOk(withdrawn ? "Quest withdrawn successfully." : "Unable to withdraw.");
        cm.dispose();
    } else if (selection === 2) {
        status = 30;
        action(1,0,0);
    }
}
