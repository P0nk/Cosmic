var status = 0;
var selectedClass;
var selectedItem;

// Define item price and currency ID
const PRICE = 20;
const CURRENCY_ID = 4001126;

// Define item sets for each class, showing only the first 5 items
const WARRIOR_ITEMS = [2290096, 2290125, 2290000, 2290001, 2290002, 2290003, 2290004, 2290005, 2290006, 2290007, 2290008, 2290009, 2290010, 2290011, 2290012, 2290013, 2290014, 2290015, 2290016, 2290017, 2290018, 2290019, 2290020, 2290021, 2290022, 2290023, 2290150, 2290151, 2290152, 2290153, 2290154, 2290155];
const MAGICIAN_ITEMS = [2290096, 2290125, 2290024, 2290025, 2290026, 2290027, 2290028, 2290029, 2290030, 2290031, 2290032, 2290033, 2290034, 2290035, 2290036, 2290037, 2290038, 2290039, 2290040, 2290041, 2290042, 2290043, 2290044, 2290045, 2290046, 2290047, 2290048, 2290049, 2290050, 2290051, 2290156, 2290157, 2290158, 2290159];
const BOWMAN_ITEMS = [2290096, 2290125, 2290052, 2290053, 2290054, 2290055, 2290056, 2290057, 2290058, 2290059, 2290060, 2290061, 2290062, 2290063, 2290064, 2290065, 2290066, 2290067, 2290068, 2290069, 2290070, 2290071, 2290072, 2290073, 2290074, 2290075, 2290160, 2290161, 2290162];
const THIEF_ITEMS = [2290096, 2290125, 2290076, 2290077, 2290078, 2290079, 2290080, 2290081, 2290082, 2290083, 2290084, 2290085, 2290086, 2290087, 2290088, 2290089, 2290090, 2290091, 2290092, 2290093, 2290094, 2290095, 2290163, 2290164, 2290165];
const PIRATE_ITEMS = [2290096, 2290125, 2290097, 2290098, 2290099, 2290100, 2290101, 2290102, 2290103, 2290104, 2290105, 2290106, 2290107, 2290108, 2290110, 2290111, 2290112, 2290113, 2290114, 2290115, 2290116, 2290117, 2290118, 2290119, 2290120, 2290121, 2290122, 2290123, 2290124, 2290166, 2290167, 2290168, 2290169, 2290170, 2290171];
const ARAN_ITEMS = [2290096, 2290125, 2290126, 2290127, 2290128, 2290129, 2290130, 2290131, 2290132, 2290133, 2290134, 2290135, 2290136, 2290137, 2290138, 2290139];
const DWARRIOR_ITEMS = [2290183, 2290184, 2290185, 2290186];
const BWIZARD_ITEMS = [2290178, 2290179, 2290180, 2290181, 2290182];
const WARCHER_ITEMS = [2290187, 2290188, 2290189, 2290190, 2290191];
const NWALKER_ITEMS = [2290192, 2290193, 2290194, 2290195];
const TBREAKER_ITEMS = [2290196, 2290197, 2290198, 2290199];
const SBEGINNER_ITEMS = [2290172, 2290173, 2290174, 2290175, 2290176];

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
    } else {
        status++;
        if (status == 0) {
            cm.sendSimple("Welcome! Please select your class:\r\n#b" +
                          "#L0#Warrior#l  #L1#Magician#l  #L2#Bowman#l  #L3#Thief#l  #L4#Pirate#l\r\n\t\t\t\t\t\t\t\t\t  #L5#Aran#l\r\n\r\n" +
                          "#L6#Blaze Wizard#l  #L7#Dawn Warrior#l  #L8#Wind Archer#l  \r\n\t\t\t\t#L9#Night Walker#l  #L10#Thunder Breaker#l\r\n\r\n\t\t\t\t\t\t\t" +
                          "#L11#Super Beginner#l");
        } else if (status == 1) {
            selectedClass = selection;
            cm.sendSimple("Please select an item to purchase:\r\n" + getItemOptions(selectedClass));
        } else if (status == 2) {
            selectedItem = selection;
            var itemId = getItemId(selectedClass, selectedItem);
            cm.sendYesNo("Are you sure you want to buy #v" + itemId + ":# for " + PRICE + " #v" + CURRENCY_ID + "#?");
        } else if (status == 3) {
            var itemId = getItemId(selectedClass, selectedItem);
            if (cm.haveItem(CURRENCY_ID, PRICE)) {
                cm.gainItem(CURRENCY_ID, -PRICE); // Deduct currency
                cm.gainItem(itemId, 1); // Give the item
                cm.sendOk("Purchase successful! Enjoy your item.");
            } else {
                cm.sendOk("You don't have enough #v" + CURRENCY_ID + "#.");
            }
            cm.dispose();
        }
    }
}

// Function to get item options for the selected class
function getItemOptions(classType) {
    var items;
    switch (classType) {
        case 0: items = WARRIOR_ITEMS; break;
        case 1: items = MAGICIAN_ITEMS; break;
        case 2: items = BOWMAN_ITEMS; break;
        case 3: items = THIEF_ITEMS; break;
        case 4: items = PIRATE_ITEMS; break;
        case 5: items = ARAN_ITEMS; break;
        case 6: items = DWARRIOR_ITEMS; break;
        case 7: items = BWIZARD_ITEMS; break;
        case 8: items = WARCHER_ITEMS; break;
        case 9: items = NWALKER_ITEMS; break;
        case 10: items = TBREAKER_ITEMS; break;
        case 11: items = SBEGINNER_ITEMS; break;
        default: items = [];
    }

    var options = "";
    for (var i = 0; i < items.length; i++) {
        options += "#L" + i + "# #v" + items[i] + ":# #l";
        if ((i + 1) % 5 == 0) { // Add a line break after every 5 items
            options += "\r\n";
        }
    }
    return options;
}

// Function to get the item ID based on class and selection
function getItemId(classType, selection) {
    switch (classType) {
        case 0: return WARRIOR_ITEMS[selection];
        case 1: return MAGICIAN_ITEMS[selection];
        case 2: return BOWMAN_ITEMS[selection];
        case 3: return THIEF_ITEMS[selection];
        case 4: return PIRATE_ITEMS[selection];
        case 5: return ARAN_ITEMS[selection];
        case 6: return DWARRIOR_ITEMS[selection];
        case 7: return BWIZARD_ITEMS[selection];
        case 8: return WARCHER_ITEMS[selection];
        case 9: return NWALKER_ITEMS[selection];
        case 10: return TBREAKER_ITEMS[selection];
        case 11: return SBEGINNER_ITEMS[selection];
        default: return null;
    }
}
