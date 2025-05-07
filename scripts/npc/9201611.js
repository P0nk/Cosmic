
/* Subordinate
        Free Market (9201611)
        
        Enhancing NPC:
*/

var selectedItem;
var newStats = {};
var zakDiamond = 4032133; // Required item: Zakum Diamond
var HTegg = 4001094; // Required item: HT egg
var rockOfTime = 4021010; // rebirth mat for 1st 2 rebirths
var billionCoin;
var cost;
var mat;
var amt;
var itemInfo = Packages.server.ItemInformationProvider.getInstance(); // Load item names
var level;
var success;
var boom;
var rate
var warning
var rebirth = false;

function start() {
    status = 0;
    cm.sendSimple("Hey wanna upgrade?\r\n#b#L0#Yes, upgrade my item!#l");
}

function action(mode, type, selection) {
    if (mode === 1) {
        status++;
    } else {
        cm.dispose();
        return;
    }

    if (status === 1) {
        var inventory = cm.getInventory(1); // Get equip inventory
        var itemList = "";
        var items = [];

        // list equip in inventory
        for (var i = 0; i <= inventory.getSlotLimit(); i++) {
            var item = inventory.getItem(i);
            if (item !== null) {
                // items.push(item);
                // itemList += "#L" + i + "#" + item + "#l\r\n";

//                 if (item.getItemLevel() == 7) { // Skip items with level 7
//                     continue;
//                 }
                var itemName = itemInfo.getName(item.getItemId());
                var itemIcon = "#v" + item.getItemId() + "#"; // Displays item icon
                var itemSlot = item.getPosition();

                itemList += "#L" + i + "#" + itemIcon + " " + itemName + " (Slot: " + itemSlot + ")#l\r\n";
            }
        }

        if (itemList === "") {
            cm.sendOk("You have no equippable items to upgrade.");
            cm.dispose();
        } else {
            cm.sendSimple("Select the item you want to upgrade. It costs 10m to preview the stats of each upgrade.\r\n" + itemList);
        }
    } else if (status === 2) {
        selectedItem = cm.getInventory(1).getItem(selection);
        if (selectedItem == null) {
            cm.sendOk("Invalid selection.");
            cm.dispose();
            return;
        } else if ((selectedItem.getItemId() == 1402180 || selectedItem.getItemId() == 1382235) && selectedItem.getItemLevel() >= 5) {
            cm.sendOk("Item too OP... Impossible to rebirth.");
            cm.dispose();
            return;
        } else if (selectedItem.getItemLevel() >= 5 && selectedItem.getHands() <= 2) {
            rebirth = true;
            cm.sendYesNo("Your item has reached it capabilities...I can make your items stronger but it will cost all your upgrades. \r\n" +
                         "Reseting your stats but giving your item Base stats a boost. \r\n" +
                         "It will cost you 1x #v" + rockOfTime + "#"+ "100k NX. Do you want to proceed?");
        } else if (selectedItem.getItemLevel() < 5 && selectedItem.getHands() <= 3) {
            var min_multiplier = 1.4
            var max_multiplier = 1.6

            newStats = {
                str: Math.floor(selectedItem.getStr() * ((Math.random() * (max_multiplier - min_multiplier)) + min_multiplier)),
                dex: Math.floor(selectedItem.getDex() * ((Math.random() * (max_multiplier - min_multiplier)) + min_multiplier)),
                int: Math.floor(selectedItem.getInt() * ((Math.random() * (max_multiplier - min_multiplier)) + min_multiplier)),
                luk: Math.floor(selectedItem.getLuk() * ((Math.random() * (max_multiplier - min_multiplier)) + min_multiplier)),
                watk: Math.floor(selectedItem.getWatk() * ((Math.random() * (max_multiplier - min_multiplier)) + min_multiplier)),
                matk: Math.floor(selectedItem.getMatk() * ((Math.random() * (max_multiplier - min_multiplier)) + min_multiplier)),
                wdef: Math.floor(selectedItem.getWdef() * ((Math.random() * (1.2 - 1.1)) + 1.1)),
                mdef: Math.floor(selectedItem.getMdef() * ((Math.random() * (1.2 - 1.1)) + 1.1)),
                lvl: selectedItem.getItemLevel() + 1
            };

            level = selectedItem.getItemLevel();
            if (level == 1) {
                cost = 15000000;
            } else if (level == 2) {
                cost = 45000000;
            } else if (level == 3) {
                cost = 125000000;
            } else if (level == 4) {
                cost = 275000000;
            }

            // Cost checker
            if (cm.getMeso() < cost + 2500000) {
                cm.sendOk("Buddy you won't have enough mesos after re-rolling! Come back with more mesos and try again.");
                cm.dispose();
                return;
            } // end

            if (level >= 1 && level <= 4) {
                cm.gainMeso(-2500000); // 10m cost for preview
                mat = (level <= 2) ? zakDiamond : HTegg;
                amt = (level % 2 == 1) ? 1 : 3;
                var message = "Upgrading this item will apply the following changes:\r\n" +
                              "STR: " + selectedItem.getStr() + " to " + newStats.str + "\r\n" +
                              "DEX: " + selectedItem.getDex() + " to " + newStats.dex + "\r\n" +
                              "INT: " + selectedItem.getInt() + " to " + newStats.int + "\r\n" +
                              "LUK: " + selectedItem.getLuk() + " to " + newStats.luk + "\r\n" +
                              "Weapon Attack: " + selectedItem.getWatk() + " to " + newStats.watk + "\r\n" +
                              "Magic Attack: " + selectedItem.getMatk() + " to " + newStats.matk + "\r\n" +
                              "Weapon Defense: " + selectedItem.getWdef() + " to " + newStats.wdef + "\r\n" +
                              "Magic Defense: " + selectedItem.getMdef() + " to " + newStats.mdef + "\r\n" +
                              "This will consume " + amt + " x #v" + mat + "# and cost " + cost + " mesos. Proceed?";
                if (selectedItem.getItemLevel() == 4) {
                    warning = "\r\nWARNING: Selected item has a chance to boom (1%), please proceed with caution."
                } else {
                    warning = ""
                }
                cm.sendYesNo(message + warning);
            } else if (selectedItem.getItemLevel() >= 6) { // for future use
              cm.sendOk("Level 6 and above detected.");
              cm.dispose();
              return;
            }
        } else {
            cm.sendOk("Item has reached max rebirth!");
            cm.dispose();
            return;
        }

    } else if (status === 3) {
        if (rebirth == false) {
            // Checks for materials in Inventory
            if (selectedItem.getItemLevel() == 1 && !cm.haveItem(zakDiamond, 1)) {
                cm.sendOk("You do not have enough " + "#v" + zakDiamond + "#" + ".");
                cm.dispose();
                return;
            } else if (selectedItem.getItemLevel() == 2 && !cm.haveItem(zakDiamond, 5)) {
                cm.sendOk("You do not have enough " + "#v" + zakDiamond + "#" + ".");
                cm.dispose();
                return;
            } else if (selectedItem.getItemLevel() == 3 && !cm.haveItem(HTegg, 1)) {
               cm.sendOk("You do not have enough " + "#v" + HTegg + "#" + ".");
               cm.dispose();
               return;
            } else if (selectedItem.getItemLevel() == 4 && !cm.haveItem(HTegg, 5)) {
               cm.sendOk("You do not have enough " + "#v" + HTegg + "#" + ".");
               cm.dispose();
               return;
            } else if (selectedItem.getItemLevel() == 5) { // for future use
                cm.sendOk("Hey there! Your item is too powerful! We need time to research more materials to further upgrade your gear.");
                cm.dispose();
                return;
            } else if (selectedItem.getItemLevel() >= 6) { // for Slimy's mess; takes the upgraded item and returns a clean item (with boosted stats of +5)
                  cm.replaceBoomedUpgradeItem(selectedItem.getPosition());
                  cm.sendOk("Hey there! Your item is too powerful! So removed your gear for safety reason (in case you hurt yourself).");
                  cm.gainItem(zakDiamond, 4);
                  cm.gainItem(HTegg, 4);
                  cm.gainMeso(1000000000)
                  cm.dispose();
                  return;
                  // this is for checking if the equip got a free upgrade, if it did, remove it
                  if (selectedItem.getLevel() == 1) {
                      cm.removeItemNPC(selectedItem.getPosition());
                      cm.sendOk("Hey there! Seems like you didn't change your item in the grace period, I'm sorry but I will have to remove you item.");
                  }
              }

            if (cm.getMeso() < cost) {
                cm.sendOk("You do not have enough mesos.");
                cm.dispose();
                return;
                }

            rate = Math.random(100000) * 100000;
            success = rate > ((selectedItem.getItemLevel()-1) * 10000); // % chance of failure
            boom = selectedItem.getItemLevel() == 4;
            boom_chance = Math.random(100000) * 100000 < 1000;
            console.log(cm.getPlayer() + " has rolled: " + rate + " Success: " + success + " Boom: " + boom + " Boom Chance: " + boom_chance);

            if (selectedItem !== null && newStats && success) {
                // Apply the stored stats
                // for future checks if item is not lvl 3 by the time itemLevel is lvl 5 then remove the item
                if (selectedItem.getItemLevel() == 5) {
                    selectedItem.setLevel(3);
                }
                selectedItem.setStr(newStats.str);
                selectedItem.setDex(newStats.dex);
                selectedItem.setInt(newStats.int);
                selectedItem.setLuk(newStats.luk);
                selectedItem.setWatk(newStats.watk);
                selectedItem.setMatk(newStats.matk);
                selectedItem.setWdef(newStats.wdef);
                selectedItem.setMdef(newStats.mdef);
                selectedItem.setItemLevel(newStats.lvl);
                // newItem.setUpgradeSlots(selectedItem.getUpgradeSlots() - 1);

                cm.getPlayer().forceUpdateItem(selectedItem);
                cm.gainMeso(-cost);
                cm.gainItem(mat, -amt);

                cm.sendOk("By the furious grace of Carbo, your item has been reborn in blazing glory!");
            } else if (boom && boom_chance) { // if item booms
                cm.gainMeso(-cost);
                cm.gainItem(mat, -amt);
                cm.removeItemNPC(selectedItem.getPosition());
                cm.sendOk("OMG item got destroyed! Merogie's hand slipped and hammered your item too hard. Get rekt son.");
            } else {
                cm.gainMeso(-cost);
                cm.gainItem(mat, -amt);
                cm.sendOk("Your item failed to upgraded!");
            }
            cm.dispose();
            return;
        } else if (rebirth == true) {
            if (selectedItem.getItemId() == 1402180 || selectedItem.getItemId() == 1382235) { // Just a double check
                cm.sendOk("Hello! Your item is already so op, you can't rebirth it!");
                cm.dispose();
                return;
            } else if (selectedItem.getItemLevel() == 1 || !cm.haveItem(rockOfTime, 1)) {
                cm.sendOk("You do not have enough " + "#v" + rockOfTime + "#" + ".");
                cm.dispose();
                return;
            } else if (cm.getCashShop().getCash(1) < 100000) {
                cm.sendOk("You do not have enough NX.");
                cm.dispose();
                return;
            } else {
                var hands = selectedItem.getHands();
                cm.rebirthItem(selectedItem.getPosition(), hands);
                cm.sendOk("Your Item has rebrithed. Go get stronger!");
                cm.gainItem(rockOfTime, -1);
                cm.gainCash(-100000)
                cm.dispose();
                return;
            }
        }
    }
}
