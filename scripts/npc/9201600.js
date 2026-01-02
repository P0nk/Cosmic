/* Cootie the Really Small – Teleport NPC (DROP-IN)
 *
 * - Uses TeleportSavedMapManager (server.teleport) for:
 *   getAccountIdByCharacterName, getSavedMaps, saveCurrentMap, getMapLimit, increaseMapLimit, removeSavedMap
 *
 * - Prevents duplicate saved map PK errors by relying on manager-side existence check.
 */

var TeleportSavedMapManager = Java.type("server.teleport.TeleportSavedMapManager");

// ========================= CONFIG =========================
var status = 0;
var accountId = 0;
var mapList = [];
var limit = 10;
var slotIncrement = 5;
var increaseCost = 100000000;

var VIPTpRock = 5041000;
var VIPTpRockCost = 1000;

var currentMapId = 0;

var blacklist = [
  280030000, 280030100, 280030101, 280030102, 280030103, 280030104, 280030105, // Zakum maps
  240060000, 240060100, 240060200, 240050400,                                 // Horntail maps
  270050100, 270050000,                                                       // Pinkbean
  211070100,                                                                  // Von Leon maps
  300000012                                                                   // Jail
];

// =========================================================

function start() {
  status = 0;

  // Block boss maps
  if (blacklist.includes(cm.getPlayer().getMapId())) {
    cm.sendOk("Hmm I am still an apprentice.... I can't handle boss maps.");
    cm.dispose();
    return;
  }

  // Resolve accountId from character name via manager
  accountId = TeleportSavedMapManager.getAccountIdByCharacterName(cm.getName());
  if (accountId <= 0) {
    cm.sendOk("Hmm... I couldn't find your account information. Please relog and try again.");
    cm.dispose();
    return;
  }

  currentMapId = cm.getMapId();
  limit = TeleportSavedMapManager.getMapLimit(accountId);
  var count = TeleportSavedMapManager.getSavedMaps(accountId).length;

  var menu =
    "Hey there, apprentice wizard here. I can teleport you to maps you have been to before.\r\n" +
    "What would you like to do? (" + count + "/" + limit + ")\r\n" +
    "#b#L0#Teleport to a saved map#l\r\n" +
    "#L1#Save current map#l\r\n" +
    "#L2#Increase saved map limit (100,000,000 mesos)#l\r\n" +
    "#L3#Remove a saved map#l\r\n" +
    "#L4#Purchase VIP teleport rocks#l#k";

  cm.sendSimple(menu);
}

function action(mode, type, selection) {
  if (mode !== 1) {
    cm.dispose();
    return;
  }

  status++;

  // ========================= MAIN MENU =========================
  if (status === 1) {
    currentMapId = cm.getMapId();

    switch (selection) {
      case 0: // Teleport to saved map
        mapList = TeleportSavedMapManager.getSavedMaps(accountId);
        if (mapList.length === 0) {
          cm.sendOk("You haven't saved any maps yet.");
          cm.dispose();
          return;
        }

        var menu = "Choose a map to teleport to:\r\n";
        for (var i = 0; i < mapList.length; i++) {
          menu += "#L" + i + "##m" + mapList[i] + "##l\r\n";
        }
        cm.sendSimple(menu);

        // Next click should go to status === 11
        status = 10;
        return;

      case 1: // Save current map
        limit = TeleportSavedMapManager.getMapLimit(accountId);
        var count = TeleportSavedMapManager.getSavedMaps(accountId).length;

        if (count >= limit) {
          cm.sendOk("You've reached your saved map limit (" + limit + "). Increase your limit to save more maps.");
          cm.dispose();
          return;
        }

        currentMapId = cm.getMapId(); // ensure map name resolves properly
        var saved = TeleportSavedMapManager.saveCurrentMap(accountId, currentMapId);

        if (saved) {
          cm.sendOk("Saved current map: " + cm.getMapName(currentMapId));
        } else {
          cm.sendOk("That map is already saved.");
        }

        cm.dispose();
        return;

      case 2: // Increase limit
        if (cm.getMeso() < increaseCost) {
          cm.sendOk("You need 100,000,000 mesos to increase your save limit.");
          cm.dispose();
          return;
        }

        cm.gainMeso(-increaseCost);
        TeleportSavedMapManager.increaseMapLimit(accountId, slotIncrement);

        var newLimit = TeleportSavedMapManager.getMapLimit(accountId);
        cm.sendOk("Your save limit has been increased to " + newLimit + ".");
        cm.dispose();
        return;

      case 3: // Remove a saved map
        mapList = TeleportSavedMapManager.getSavedMaps(accountId);
        if (mapList.length === 0) {
          cm.sendOk("You don't have any saved maps to remove.");
          cm.dispose();
          return;
        }

        var rmMenu = "Select a map to remove:\r\n";
        for (var r = 0; r < mapList.length; r++) {
          rmMenu += "#L" + r + "##m" + mapList[r] + "##l\r\n";
        }
        cm.sendSimple(rmMenu);

        // Next click should go to status === 21
        status = 20;
        return;

      case 4: // Buy VIP TP rocks
        cm.sendGetText("How many VIP Teleport Rocks would you like to buy?\r\n(Each costs " + VIPTpRockCost + " NX)", "1");
        // Next click should go to status === 31
        status = 30;
        return;
    }

    // If somehow fallthrough:
    cm.dispose();
    return;
  }

  // ========================= TELEPORT HANDLER =========================
  if (status === 11) {
    var usedRocks = 1;

    if (!cm.haveItem(VIPTpRock, usedRocks)) {
      cm.sendOk("You need a #v" + VIPTpRock + "# to teleport.");
      cm.dispose();
      return;
    }

    var targetMapId = mapList[selection];
    if (targetMapId && targetMapId > 0) {
      cm.warp(targetMapId);
      cm.gainItem(VIPTpRock, -usedRocks);
    } else {
      cm.sendOk("Invalid map selected.");
    }

    cm.dispose();
    return;
  }

  // ========================= REMOVE HANDLER =========================
  if (status === 21) {
    var mapIdToRemove = mapList[selection];

    if (mapIdToRemove && mapIdToRemove > 0) {
      TeleportSavedMapManager.removeSavedMap(accountId, mapIdToRemove);
      cm.sendOk("Removed map: #m" + mapIdToRemove + "#");
    } else {
      cm.sendOk("Invalid selection.");
    }

    cm.dispose();
    return;
  }

  // ========================= PURCHASE HANDLER =========================
  if (status === 31) {
    var purchaseCount = parseInt(cm.getText());

    if (isNaN(purchaseCount) || purchaseCount <= 0) {
      cm.sendOk("Please enter a valid amount.");
      cm.dispose();
      return;
    }

    var totalCost = purchaseCount * VIPTpRockCost;

    if (cm.getCashShop().getCash(1) < totalCost) {
      cm.sendOk("You do not have enough NX.");
      cm.dispose();
      return;
    }

    cm.gainItem(VIPTpRock, purchaseCount);
    cm.gainCash(-totalCost);
    cm.sendCashNoti("You used " + totalCost + " NX to purchase VIP Teleport Rocks.");
    cm.dispose();
    return;
  }

  cm.dispose();
}

// ========= Useful functions (optional) =========
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
