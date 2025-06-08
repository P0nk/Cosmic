/* Cootie the Really Small – Teleport NPC */

var status = 0;
var accountId = 0;
var mapList = [];
var limit = 10;
var mapChoice = 0;
var slotIncrement = 5;
var increaseCost = 100000000;
var VIPTpRock = 5041000
var VIPTpRockCost = 1000;
var currentMapId;
var blacklist = [280030000,280030100,280030101,280030102,280030103,280030104,280030105, // Zakum maps
                 240060000,240060100,240060200, // Horntail maps
                 270050100,
                 211070100] // Von Leon maps

function start() {
    if (blacklist.includes(cm.getPlayer().getMapId())) {
        cm.sendOk("Hmm I am still an apprentice.... I can't handle boss maps.");
        cm.dispose();
        return;
    }
    accountId = cm.getAccountIdByCharacterName(cm.getName());
    currentMapId = cm.getMapId();
    limit = cm.getMapLimit(accountId);
    var count = cm.getSavedMaps(accountId).length;
    status = 0;
    cm.sendSimple("Hey there, apprentice wizard here. I can teleport you to maps you have been to before.\r\nWhat would you like to do? (" + count + "/" + limit + ")\r\n" +
                  "#b#L0#Teleport to a saved map#l\r\n" +
                  "#L1#Save current map#l\r\n" +
                  "#L2#Increase saved map limit (100,000,000 mesos)#l\r\n" +
                  "#L3#Remove a saved map#l\r\n" +
                  "#L4#Purchase VIP teleport rocks#l");
}

function action(mode, type, selection) {
    if (mode !== 1) return cm.dispose();
    status++;

    if (status === 1) {
        switch (selection) {
            case 0: // If selected Teleport to Map option
                mapList = cm.getSavedMaps(accountId);
                if (mapList.length === 0) {
                    cm.sendOk("You haven't saved any maps yet.");
                    return cm.dispose();
                }
                var menu = "Choose a map to teleport to:\r\n";
                for (var i = 0; i < mapList.length; i++) {
                    menu += "#L" + i + "##m" + mapList[i] + "##l\r\n";
                }
                cm.sendSimple(menu);
                status = 10;
                break;

            case 1: // If selected Save Map option
                var count = cm.getSavedMaps(accountId).length;
                if (count >= limit) {
                    cm.sendOk("You've reached your saved map limit (" + limit + "). Increase your limit to save more maps.");
                    return cm.dispose();
                }
                cm.saveCurrentMap(accountId, currentMapId)
                cm.sendOk("Saved current map: " + cm.getMapName(currentMapId));
                cm.dispose();
                break;

            case 2: // If selected Increase Saved map list size
                if (cm.getMeso() < increaseCost) {
                    cm.sendOk("You need 100,000,000 mesos to increase your save limit.");
                    return cm.dispose();
                }

                cm.gainMeso(-increaseCost);
                cm.increaseMapLimit(accountId, slotIncrement);
                cm.sendOk("Your save limit has been increased from " + limit + " to " + (limit + 5) + ".");
                cm.dispose();
                break;
            case 3: // If selected Remove map from saved list
                mapList = cm.getSavedMaps(accountId);
                if (mapList.length === 0) {
                    cm.sendOk("You don't have any saved maps to remove.");
                    return cm.dispose();
                }
                var rmMenu = "Select a map to remove:\r\n";
                for (var i = 0; i < mapList.length; i++) {
                    rmMenu += "#L" + i + "##m" + mapList[i] + "##l\r\n";
                }
                cm.sendSimple(rmMenu);
                status = 20;
                break;
            case 4: // If selected Purchasing VIP Teleport Rocks
                cm.sendGetText("How many VIP Teleport Rocks would you like to buy?", "1");
                status = 30;
        }

    } else if (status === 11) { // Handles Map Warping
        var newMap = parseInt(Math.floor(mapList[selection] / 100000000)); // 9
        var curMap = parseInt(Math.floor(currentMapId / 100000000)); // 1
//        console.log("NewMap: " + newMap + " Current Map: " + curMap + " Cost: " +parseInt(Math.abs(newMap - curMap)));
        var usedRocks = (parseInt(Math.abs(newMap - curMap)) == 0 ? 1 : parseInt(Math.abs(newMap - curMap)));
        if (!cm.haveItem(VIPTpRock, usedRocks)) {
            cm.sendOk("You need a #v" + VIPTpRock + "# to teleport.");
        } else if (mapList[selection]) {
            cm.warp(mapList[selection]);
            cm.gainItem(VIPTpRock, - usedRocks); // Cost
        } else {
            cm.sendOk("Invalid map selected.");
        }
        cm.dispose();
    } else if (status === 21) { // Handles Map removal
        var mapIdToRemove = mapList[selection];
        if (mapIdToRemove) {
          cm.removeSavedMap(accountId, mapIdToRemove);
          cm.sendOk("Removed map: #m" + mapList[selection] + "#");
        } else {
          cm.sendOk("Invalid selection.");
        }
        cm.dispose();
    } else if (status === 31) { // Handles VIP Teleport rock purchases
        purchaseCount = parseInt(cm.getText());
        if (cm.getCashShop().getCash(1) < purchaseCount * VIPTpRockCost) {
            cm.sendOk("You do not have enough NX");
        } else {
            cm.gainItem(VIPTpRock, purchaseCount);
            cm.gainCash(-purchaseCount * VIPTpRockCost);
            cm.sendCashNoti("You used " + (purchaseCount * VIPTpRockCost) + " NX to purchase VIP Teleport rocks");
        }
        cm.dispose();
    }
}

// ========= Useful functions =========
function getContinentName(mapId) {
    var region = Math.floor(mapId / 1000000);
    switch (region) {
        case 1: return "Victoria Island";
        case 2: return "Ossyria";
        case 3: return "World Tour";
        case 6: return "Crimsonwood";
        case 7: return "Temple of Time";
        default: return "Unknown Region";
    }
}