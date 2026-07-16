var status = -1;
var selectedCategory = -1;
var selectedMap = -1;
var selectedName = "";

/*
 * Torv's Wagon Network
 *
 * Main categories:
 * 0 = Major Destinations
 * 1 = Hunting Grounds
 * 2 = Social Maps
 *
 * Hunting categories:
 * 100 = Early Training — Level 10–29
 * 101 = Second Job — Level 30–69
 * 102 = Third Job — Level 70–119
 * 103 = Fourth Job — Level 120+
 *
 * Level ranges are recommendations only.
 * Players may travel to any listed hunting ground.
 */

var majorDestinations = [
    { name: "Henesys", map: 100000000 },
    { name: "Ellinia", map: 101000000 },
    { name: "Perion", map: 102000000 },
    { name: "Kerning City", map: 103000000 },
    { name: "Lith Harbor", map: 104000000 },
    { name: "Sleepywood", map: 105040300 },
    { name: "Nautilus Harbor", map: 120000000 },

    { name: "Orbis", map: 200000000 },
    { name: "El Nath", map: 211000000 },

    { name: "Ludibrium", map: 220000000 },
    { name: "Omega Sector", map: 221000000 },
    { name: "Korean Folk Town", map: 222000000 },

    { name: "Aquarium", map: 230000000 },
    { name: "Leafre", map: 240000000 },

    { name: "Mu Lung", map: 250000000 },
    { name: "Herb Town", map: 251000000 },

    { name: "Ariant", map: 260000000 },
    { name: "Magatia", map: 261000000 },

    { name: "New Leaf City", map: 600000000 },
    { name: "Mushroom Shrine", map: 800000000 },
    { name: "Showa Town", map: 801000000 }
];

var earlyHunting = [
    { name: "Henesys Hunting Ground I", map: 104040000 },
    { name: "Pig Beach", map: 104010001 },
    { name: "Land of Wild Boar", map: 101040001 },
    { name: "Land of Wild Boar II", map: 101030001 },
    { name: "Monkey Forest I", map: 100040101 },
    { name: "Forest of Golem", map: 105040306 },
    { name: "Dangerous Valley", map: 106000001 },
    { name: "Drake's Meal Table", map: 105090300 },
    { name: "Ant Tunnel I", map: 105050000 }
];

var secondJobHunting = [
    { name: "Cloud Park I", map: 200010000 },
    { name: "Garden of Darkness I", map: 200010301 },
    { name: "Garden of Darkness II", map: 200010302 },
    { name: "Ludibrium Terrace Hall", map: 220010500 }
];

var thirdJobHunting = [
    { name: "Wolf Territory I", map: 211040500 },
    { name: "Wolf Territory II", map: 211040600 },
    { name: "Wolf Territory III", map: 211040800 },

    { name: "Forest of Dead Trees I", map: 211041100 },
    { name: "Forest of Dead Trees II", map: 211041200 },
    { name: "Forest of Dead Trees III", map: 211041300 },
    { name: "Forest of Dead Trees IV", map: 211041400 },

    { name: "Path of Time", map: 220050300 },
    { name: "Forgotten Path of Time 4", map: 220070300 },

    { name: "Goblin Forest 1", map: 250010503 },
    { name: "Goblin Forest 2", map: 250010504 },

    { name: "Wolf Spider Cavern", map: 600020300 }
];

var fourthJobHunting = [
    { name: "Deep Inside the Clocktower", map: 220080000 },

    { name: "Nest of a Dead Dragon", map: 240040510 },
    { name: "The Dragon Nest Left Behind", map: 240040511 },
    { name: "Destroyed Dragon Nest", map: 240040520 },
    { name: "Dangerous Dragon Nest", map: 240040521 }
];

var socialMaps = [
    { name: "Free Market", map: 910000000 },
    { name: "Amoria", map: 680000000 },
    { name: "White Wedding Lounge", map: 680000100 },
    { name: "Saint Maple Lounge", map: 680000200 },
    { name: "Ludibrium Cloud Balcony", map: 220010001 }
];

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }

    status++;

    if (status === 0) {
        showMainMenu();
        return;
    }

    if (status === 1) {
        selectedCategory = selection;
        showCategory();
        return;
    }

    if (status === 2) {
        handleDestinationSelection(selection);
        return;
    }

    if (status === 3) {
        completeTravel();
        return;
    }

    cm.dispose();
}

function showMainMenu() {
    cm.sendSimple(
        "#eThe roads are open, traveler.#n\r\n\r\n" +
        "My wagon runs from the old towns to hunting trails most folk " +
        "have long forgotten.\r\n\r\n" +
        "The Coalition covers the fare. Keep your coin for the road ahead.\r\n\r\n" +

        "#L0##bMajor Destinations#k\r\n" +
        "Towns, continents, and major settlements.#l\r\n\r\n" +

        "#L1##rHunting Grounds#k\r\n" +
        "Recommended training routes organized by advancement.#l\r\n\r\n" +

        "#L2##dSocial Maps#k\r\n" +
        "Gathering places for trading, conversation, and rest.#l"
    );
}

function showCategory() {
    if (selectedCategory === 0) {
        showDestinationList(
            "#eMajor Destinations#n\r\n\r\n" +
            "The wagon can carry you across the old roads and sea routes.\r\n\r\n",
            majorDestinations
        );
        return;
    }

    if (selectedCategory === 1) {
        showHuntingMenu();
        return;
    }

    if (selectedCategory === 2) {
        showDestinationList(
            "#eSocial Maps#n\r\n\r\n" +
            "Looking for company rather than combat?\r\n\r\n",
            socialMaps
        );
        return;
    }

    cm.sendOk("That road is not currently open.");
    cm.dispose();
}

function showHuntingMenu() {
    cm.sendSimple(
        "#eHunting Grounds#n\r\n\r\n" +
        "These routes are recommendations, not restrictions.\r\n" +
        "A seasoned traveler may take any road they choose.\r\n\r\n" +

        "#L100##gEarly Training - Level 10-29#k#l\r\n" +
        "#L101##bSecond Job - Level 30-69#k#l\r\n" +
        "#L102##dThird Job - Level 70-119#k#l\r\n" +
        "#L103##rFourth Job - Level 120+#k#l"
    );
}

function handleDestinationSelection(selection) {
    if (selectedCategory === 0) {
        confirmDestination(majorDestinations, selection);
        return;
    }

    if (selectedCategory === 2) {
        confirmDestination(socialMaps, selection);
        return;
    }

    if (selectedCategory === 1) {
        showHuntingTier(selection);
        return;
    }

    if (selectedCategory === 100) {
        confirmDestination(earlyHunting, selection);
        return;
    }

    if (selectedCategory === 101) {
        confirmDestination(secondJobHunting, selection);
        return;
    }

    if (selectedCategory === 102) {
        confirmDestination(thirdJobHunting, selection);
        return;
    }

    if (selectedCategory === 103) {
        confirmDestination(fourthJobHunting, selection);
        return;
    }

    cm.sendOk("That road is not currently marked on my map.");
    cm.dispose();
}

function showHuntingTier(selection) {
    var routes;
    var heading;

    if (selection === 100) {
        routes = earlyHunting;
        heading = "#eEarly Training - Level 10-29#n\r\n\r\n";
    } else if (selection === 101) {
        routes = secondJobHunting;
        heading = "#eSecond Job - Level 30-69#n\r\n\r\n";
    } else if (selection === 102) {
        routes = thirdJobHunting;
        heading = "#eThird Job - Level 70-119#n\r\n\r\n";
    } else if (selection === 103) {
        routes = fourthJobHunting;
        heading = "#eFourth Job - Level 120+#n\r\n\r\n";
    } else {
        cm.sendOk("That hunting route is not available.");
        cm.dispose();
        return;
    }

    selectedCategory = selection;

    showDestinationList(
        heading +
        "Choose the trail you wish to follow.\r\n\r\n",
        routes
    );

    /*
     * Keep the next click at the destination-selection stage.
     * action() will increment status back to 2 when a map is selected.
     */
    status = 1;
}

function showDestinationList(header, destinations) {
    if (destinations.length === 0) {
        cm.sendOk(
            header +
            "Torv has not charted any destinations in this category yet."
        );
        cm.dispose();
        return;
    }

    var menu = header;

    for (var i = 0; i < destinations.length; i++) {
        menu += "#L" + i + "##m" + destinations[i].map + "##l\r\n";
    }

    cm.sendSimple(menu);
}

function confirmDestination(destinations, selection) {
    if (selection < 0 || selection >= destinations.length) {
        cm.sendOk("That road is not marked on my map.");
        cm.dispose();
        return;
    }

    selectedMap = destinations[selection].map;
    selectedName = destinations[selection].name;

    cm.sendYesNo(
        "#e" + selectedName + "#n\r\n\r\n" +
        "That road may be rough, but the wagon will hold.\r\n\r\n" +
        "Climb aboard?"
    );
}

function completeTravel() {
    if (selectedMap < 0) {
        cm.sendOk("I seem to have lost the road.");
        cm.dispose();
        return;
    }

    cm.warp(selectedMap, 0);
    cm.dispose();
}