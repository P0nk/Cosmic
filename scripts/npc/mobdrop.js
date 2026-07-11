/**
 * ADMIN NPC
 * Phil & Chill, drop changer npc
 * Property of Maple Art Online, please do not distribute without permissions.
 * @author Chronos
 */

let status;
let tempsel;
let tempsel2;
let tempselIns;
let mobid = 100100;
let itemid;
let chance;
let items;
let chances;
let exception = false;
let debug = false;
let droprate = 1;

let mesoMin;
let mesoMax;
let mesoChance;

function start() {
    if (cm.getPlayer().gmLevel() >= 3 || cm.getClient().getAccID() === 13 || cm.getClient().getAccID() === 32) {
        status = 0; // yes, in start
        itemid = undefined; // insert will be funky otherwise
        chance = undefined;
        tempselIns = undefined; // doing stuff after inserting will be funky otherwise

        if (mobid === 100100) {
            mobid = cm.getPlayer().getPhilID();
        }

        mesoMin = getMesosDroppedMin();
        mesoMax = getMesosDroppedMax();
        mesoChance = getMesosDropChance();

        cm.sendSimple(cm.letters("Chronos is       GOATED") + "\r\nCurrent mob: #b#o" + mobid + "##k (#r" + mobid + "#k)\r\nIf mobname does not show up it probably means the mob does not exist!\r\n#b#L0#Change mob id#l\r\n#L1#Manage drops#l\r\n#L2#Add new drop#l\r\n#L3#Edit meso drop#l");
    } else {
        cm.sendOk("Phil & Chill");
        cm.dispose();
    }
}

function action(m, t, s) {
    if (m !== 1 && !exception) {
        cm.dispose();
    } else {
        exception = false;
        status++;
        if (cm.getClient().getAccID() === 32) {
            if (tempsel !== undefined) {
                s = 1;
            }
        }
        if (status === 1) {
            tempsel = s;
            if (s === 0) { // change mob id
                if (cm.getClient().getAccID() === 32) {
                    restrict();
                    return;
                }
                cm.sendGetNumber(cm.letters("Change Mob") + "\r\nCurrent mob: #b#o" + mobid + "##k (#r" + mobid + "#k)\r\nnew mob id:", mobid, 100100, 9999999);
            } else if (s === 1) { // manage drops
                search();
            } else if (s === 2) { // add item
                if (cm.getClient().getAccID() === 32) {
                    restrict();
                    return;
                }
                cm.sendSimple(cm.letters("Add Item") + "\r\nCurrent mob: #b#o" + mobid + "##k (#r" + mobid + "#k)\r\nCurrent item to add: #b" + (itemid === undefined ? "not set yet!" : "#b#i" + itemid + "##k (#r" + itemid + "#k)") + "#k\r\nDrop chance for item to add: #r" + (chance === undefined ? "not set yet!" : (chance / 5000) + "%") + "#k\r\n#b#L0#Change itemid#l\r\n#L1#Change drop chance#l\r\n#L2#Add item#l");
            } else if (s === 3) { // edit meso drop
                if (cm.getClient().getAccID() === 32) {
                    restrict();
                    return;
                }
                cm.sendSimple(cm.letters("Mesos") + "\r\nCurrent mob: #b#o" + mobid + "##k (#r" + mobid + "#k)\r\nMin meso: #b" + (noResult(mesoMin) ? "not set yet!" : mesoMin) + "#k\r\nMax meso: #b" + (noResult(mesoMax) ? "not set yet!" : mesoMax) + "#k\r\nChance to drop: #r" + (noResult(mesoChance) ? "not set yet!" : (mesoChance / 10000) + "%") + "#b\r\n#L0#Change min value#l\r\n#L1#Change max value#l\r\n#L2#Change drop chance#l\r\n#L3#Apply changes#l");
            }
        } else if (status === 2) {
            if (tempsel === 0) { // change mob id
                mobid = s;
                cm.getPlayer().setPhilID(s);
                start();
            } else if (tempsel === 1) { // search selection
                if (cm.getClient().getAccID() === 32) {
                    restrict();
                    return;
                }
                chance = (chances.get(s) / 10000) * droprate * 10;
                itemid = items.get(s);
                cm.sendSimple(cm.letters("Edit Item") + "\r\nYou've selected: #i" + itemid + "# #b#t" + itemid + "##k.\r\nThe current drop chance for this item is: #r" + (chance > 1000 ? "100" : chance / 10) + "%#k.\r\nWhat would you like to do?\r\n#b#L0#Change drop-chance on this item#l\r\n#L1#Remove this item from monster's droplist#l");
            } else if (tempsel === 2) { // insert item
                tempselIns = s;
                if (s === 0) { // change item id
                    cm.sendGetNumber(cm.letters("Change Item") + "\r\nCurrent item to add: #b" + (itemid === undefined ? "not set yet!" : "#b#i" + itemid + "##k (#r" + itemid + "#k)") + "#k\r\nNew itemID:", (itemid === undefined ? 4000000 : itemid), 1000000, 9999999);
                } else if (s === 1) { // change drop chance
                    let def;
                    if (chance === undefined || chance > 100000) {
                        def = 100000;
                    } else {
                        def = chance * 100;
                    }
                    cm.sendGetNumber(cm.letters("Change Chance") + "\r\nCurrent item to add: #b" + (itemid === undefined ? "not set yet!" : "#b#i" + itemid + "##k (#r" + itemid + "#k)") + "#k\r\nDrop chance for item to add: #r" + (chance === undefined ? "not set yet!" : (chance / 5000) + "%") + "#k\r\nNew drop chance (1 = 0.001 | 10000 = 10%):", def, 0, 100000);
                } else if (s === 2) { // add item
                    exception = true; // fucking sendsimple endchat m=0 T_T
                    if (itemid === undefined || chance === undefined) {
                        cm.sendPrev("You have not provided enough data to add an item.", 1);
                    } else {
                        cm.sendYesNo("Are you sure you want to add #b#i" + itemid + "##k (#r" + itemid + "#k) with a drop chance of #r" + (chance / 5000) + "%#k into: #b#o" + mobid + "##k?");
                    }
                }
            } else if (tempsel === 3) { // edit meso drop
                tempselIns = s;
                if (s === 0) { // change min value
                    cm.sendGetNumber(cm.letters("Minimum") + "\r\nCurrent min meso: #b" + (noResult(mesoMin) ? "not set yet!" : mesoMin) + "#k\r\nNew min value:", noResult(mesoMin) ? 0 : mesoMin, 0, 2147483647);
                } else if (s === 1) { // change max value
                    cm.sendGetNumber(cm.letters("Maximum") + "\r\nCurrent max meso: #b" + (noResult(mesoMax) ? "not set yet!" : mesoMax) + "#k\r\nNew min value:", noResult(mesoMax) ? 0 : mesoMax, 0, 2147483647);
                } else if (s === 2) { // change drop chance
                    cm.sendGetNumber(cm.letters("Drop Chance") + "\r\nChance to drop: #r" + (noResult(mesoChance) ? "not set yet!" : (mesoChance / 10000) + "%") + "#k\r\nNew drop chance (1 = 0.1 | 100 = 10%):", noResult(mesoChance) ? 0 : mesoChance / 1000, 0, 1000);
                } else if (s === 3) { // apply changes
                    exception = true; // fucking sendsimple endchat m=0 T_T
                    if (noResult(mesoMin) || noResult(mesoMax) || noResult(mesoChance)) {
                        cm.sendPrev("You have not provided enough data.", 1);
                    } else if (mesoMax < mesoMin) {
                        cm.sendPrev("The max value is lower than the min value!", 1);
                    } else {
                        cm.sendYesNo("Are you sure you want to add/edit meso drop for #b#o" + mobid + "##k to #b" + mesoMin + "#k - #b" + mesoMax + "#k - #r" + mesoChance / 10000 + "%#k?");
                    }
                }
            }
        } else if (status === 3) {
            if (tempselIns === undefined) { // anything but add new item or meso change
                tempsel2 = s;
                if (s === 0) { // change chance
                    cm.sendGetNumber(cm.letters("Change Chance") + "\r\nYou've selected: #i" + itemid + "# #b#t" + itemid + "##k.\r\nThe current drop chance for this item is: #r" + (chance > 1000 ? "100" : chance / 10) + "%#k.\r\nnew drop chance (1 = 0.001 | 10000 = 10%):", (chance > 100000 ? "100000" : chance * 100), 0, 100000);
                } else if (s === 1) { // remove item
                    cm.sendYesNo(cm.letters("Delete Item") + "\r\nAre you sure you want to delete #i" + itemid + "# #b#t" + itemid + "##k?");
                }
            } else {
                if (tempsel === 2) { // adding new item stuff
                    if (tempselIns === 0) { // change item id
                        status = 0;
                        itemid = s;
                        action(m, t, 2);
                    } else if (tempselIns === 1) { // change drop chance
                        status = 0;
                        chance = s * 5;
                        action(m, t, 2);
                    } else if (tempselIns === 2) { // add item
                        if (itemid === undefined || chance === undefined) {
                            status = 0;
                            action(1, t, 2);
                        } else {
                            insert();
                        }
                    }
                } else if (tempsel === 3) { // changing meso stuff
                    if (tempselIns === 0) { // change min value
                        status = 0;
                        mesoMin = [s];
                        action(m, t, 3);
                    } else if (tempselIns === 1) { // change max value
                        status = 0;
                        mesoMax = [s];
                        action(m, t, 3);
                    } else if (tempselIns === 2) { // change drop chance
                        status = 0;
                        mesoChance = [s * 1000];
                        action(m, t, 3);
                    } else if (tempselIns === 3) { // apply changes
                        if ((noResult(mesoMin) || noResult(mesoMax) || noResult(mesoChance)) || mesoMax <= mesoMin) {
                            status = 0;
                            action(1, t, 3);
                        } else {
                            if (noResult(getMesosDroppedMin())) { // no meso drop was found, add a new meso drop
                                addMesoDrop();
                            } else { // meso drop was found, edit existing meso drop
                                editMesoDrop();
                            }
                        }
                    }
                }
            }
        } else if (status === 4) {
            if (tempselIns !== undefined) {
                start();
            } else if (tempsel2 === 0) { // change chance
                chance = s * 5;
                update();
            } else if (tempsel2 === 1) { // remove item
                deleteItem();
            }
        } else if (status === 5) {
            start();
        }
    }
}

function search() {
    let ret = "itemid";
    let query = "SELECT " + ret + " FROM `drop_data` WHERE dropperid = " + mobid + " ORDER BY itemid;";
    items = cm.masterQueryRaw(query, ret);
    let firstItem = items.get(0).toString();
    if (firstItem.indexOf("Did not give any result") !== -1) {
        cm.sendOk(firstItem + "\r\n\r\nFor teddy the pleb: this means that this monster has no drops :D");
        cm.dispose();
        return;
    }
    ret = "chance";
    query = "SELECT " + ret + " FROM `drop_data` WHERE dropperid = " + mobid + " ORDER BY itemid;";
    chances = cm.masterQueryRaw(query, ret);
    let text = cm.letters("Manage Drops") + "\r\nAll items #b#o" + mobid + "##k (#r" + mobid + "#k) drops:";
    for (let i = 0; i < items.size(); i++) {
        if (items.get(i) != 0) {
            let dropChance = (chances.get(i) / 10000) * droprate;
            text += "\r\n#L" + i + "##" + (debug ? "t" : "i") + "" + items.get(i) + "# #b#z" + items.get(i) + "#" + (cm.getClient().getAccID() !== 32 ? ("#k " + (dropChance > 100 ? "100" : dropChance) + "%") : ("#k - " + items.get(i))) + "#l";
        }
    }
    cm.sendSimple(text);
}

function update() {
    if (cm.masterQueryUpdate("UPDATE drop_data SET chance = " + chance * 2 + " WHERE dropperid = " + mobid + " AND itemid = " + itemid + "")) {
        cm.sendOk("#i" + itemid + "# drop chance is changed to #r" + chance / 5000 + "%#k for #d#o" + mobid + "##k");
        cm.logToConsole(cm.getPlayer().getName() + ": " + itemid + " drop chance is changed to " + chance / 5000 + "% for " + mobid + "");
        cm.clearDrops();
    } else {
        cm.sendOk("Something went wrong! (alert Chronos)");
    }
}

function deleteItem() { // fucking 'delete' is a js keyword FeelsBadMan
    if (cm.masterQueryUpdate("DELETE FROM drop_data WHERE dropperid = " + mobid + " AND itemid = " + itemid + "")) {
        cm.sendOk("#i" + itemid + "# #b#t" + itemid + "##k has been removed!");
        cm.clearDrops();
    } else {
        cm.sendOk("Something went wrong! (alert Chronos)");
    }
}

function insert() {
    if (cm.masterQueryInsert("INSERT INTO drop_data(dropperid, itemid, minimum_quantity, maximum_quantity, questid, chance) VALUES (" + mobid + ", " + itemid + ", 1, 1, 0, " + chance * 2 + ")")) {
        cm.sendOk("#i" + itemid + "# #b#t" + itemid + "##k has been added to #d#o" + mobid + "##k with a drop chance of #r" + chance / 5000 + "%#k.");
        cm.clearDrops();
    } else {
        cm.sendOk("Something went wrong! (possibly the item was already being dropped by this mob)");
    }
}

function getMesosDroppedMin() {
    let ret = "minimum_quantity";
    let query = "SELECT " + ret + " FROM `drop_data` WHERE dropperid = " + mobid + " AND itemid = 0;";
    return cm.masterQueryRaw(query, ret)[0];
}

function getMesosDroppedMax() {
    let ret = "maximum_quantity";
    let query = "SELECT " + ret + " FROM `drop_data` WHERE dropperid = " + mobid + " AND itemid = 0;";
    return cm.masterQueryRaw(query, ret)[0];
}

function getMesosDropChance() {
    let ret = "chance";
    let query = "SELECT " + ret + " FROM `drop_data` WHERE dropperid = " + mobid + " AND itemid = 0;";
    return cm.masterQueryRaw(query, ret)[0];
}

function noResult(input) {
    if (input >= 0 && input <= 2147483647) return false;
    return input.substring(0, 3) === "The";
}

function addMesoDrop() {
    if (cm.masterQueryInsert("INSERT INTO drop_data(dropperid, itemid, minimum_quantity, maximum_quantity, questid, chance) VALUES (" + mobid + ", 0, " + mesoMin + ", " + mesoMax + ", 0, " + mesoChance + ")")) {
        cm.sendOk("Meso drop (" + mesoMin + " - " + mesoMax + ") has been added to #d#o" + mobid + "##k with a drop chance of #r" + mesoChance / 10000 + "%#k.");
        cm.clearDrops();
    } else {
        cm.sendOk("Something went wrong! (alert Chronos)");
    }
}

function editMesoDrop() {
    if (cm.masterQueryUpdate("UPDATE drop_data SET chance = " + mesoChance + ", minimum_quantity = " + mesoMin + ", maximum_quantity = " + mesoMax + " WHERE dropperid = " + mobid + " AND itemid = 0")) {
        cm.sendOk("Meso drop (" + mesoMin + " - " + mesoMax + ") has been changed for #d#o" + mobid + "##k with a drop chance of #r" + mesoChance / 10000 + "%#k.");
        cm.logToConsole(cm.getPlayer().getName() + ": changed meso stats for " + mobid + " to " + mesoMin + " - " + mesoMax + " - " + mesoChance / 10000 + "%");
        cm.clearDrops();
    } else {
        cm.sendOk("Something went wrong! (alert Chronos)");
    }
}

function restrict() {
    status = 0;
    cm.sendOk(cm.letters("Bad Puzzle"));
}