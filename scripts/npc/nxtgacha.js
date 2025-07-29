var status = 0;
var costItem = 3020001;
var costAmount = 1;

var rarityRates = {
    "COMMON": 80.0,
    "RARE": 15.0,
    "ULTRA_RARE": 5.0
};

//// AUGUST NX GACHA LIST ////
var lootTable = {
    "COMMON": [
        [1052948, "COMMON"], // Evening Orchid
        [1052594, "COMMON"], // Green Dinosaur Overall
        [1004540, "COMMON"], // Evening Orchid Hat
        [1003802, "COMMON"], // Green Dinosaur Hat
        [1082511, "COMMON"], // Green Dinosaur Gloves
        [1072791, "COMMON"] // Green Dinosaur Shoes
    ],
    "RARE": [
        [1050381, "RARE"], // Kinesis Uniform
        [1051544, "RARE"], // Soft Blushed
        [1051539, "RARE"], // Crimson Fate Topcoat
        [1053352, "RARE"], // Veritas Attire
        [1053351, "RARE"], // Cutie Pie Coat
        [1004003, "RARE"], // Pink Nero Hoodie
        [1004004, "RARE"], // Grey Nero Hoodie
        [1004167, "RARE"], // Dinosaur Snapback
        [1004589, "RARE"], // Jay's Sterilized Kitty Eye Patch
        [1102801, "RARE"], // Silver Wolf Coat
        [1102820, "RARE"]  // Hazy Night Tassel
    ],
    "ULTRA_RARE": [
        [1702588, "ULTRA_RARE"], // black cat plush
        [1702718, "ULTRA_RARE"], // shadow warrior sword
        [1702557, "ULTRA_RARE"]  // duster
    ]
};

function start() {
    status = -1;
        // Debug print all variables
        java.lang.System.out.println("==== NXT Gachapon Debug Start ====");
        java.lang.System.out.println("costItem: " + costItem);
        java.lang.System.out.println("costAmount: " + costAmount);
        java.lang.System.out.println("rarityRates: " + JSON.stringify(rarityRates));
        java.lang.System.out.println("lootTable: ");

        for (var rarity in lootTable) {
            java.lang.System.out.println(" - " + rarity + ": " + lootTable[rarity].length + " items");
            for (var i = 0; i < lootTable[rarity].length; i++) {
                java.lang.System.out.println("   > " + lootTable[rarity][i][0] + " [" + lootTable[rarity][i][1] + "]");
            }
        }
        java.lang.System.out.println("==== NXT Gachapon Debug End ====");

    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode !== 1) {
        cm.dispose();
        return;
    }
    status++;

    if (status === 0) {
        cm.sendYesNo("Welcome to the #bPremium NX Gachapon#k!\r\n" +
                     "It costs #r" + costAmount + "#k#v" + costItem + "# per roll.\r\n" +
                     "Would you like to roll?");
    } else if (status === 1) {
        if (!cm.haveItem(costItem, costAmount)) {
            cm.sendOk("You need at least #r" + costAmount + " NXT#k to roll.");
            cm.dispose();
            return;
        }

        // Roll rarity
        var rarity = pickRarity();
        var pool = lootTable[rarity];
        if (!pool || pool.length === 0) {
            cm.sendOk("There are no items configured for rarity: " + rarity);
            cm.dispose();
            return;
        }

        // Pick random item in selected rarity pool
        var selected = pool[Math.floor(Math.random() * pool.length)];
        var itemId = selected[0];
        var itemRarity = selected[1];

        java.lang.System.out.println("==== can hold check Start ====");
        if (!cm.canHold(itemId)) {
            java.lang.System.out.println("==== can hold check entered ====");
            cm.sendOk("Please ensure you have at least one free slot.");
            cm.dispose();
            return;
        }
        java.lang.System.out.println("==== can hold check passed ====");

        cm.gainItem(itemId, 1);
        // Consume NXT
        cm.gainItem(costItem, -costAmount);
        cm.sendOk("You received: #b#t" + itemId + "##k (" + itemRarity + ")!");

        // Broadcast ultra rare
        if (itemRarity === "ULTRA_RARE") {
            cm.getPlayer().getMap().broadcastMessage(
                Packages.tools.PacketCreator.serverNotice(6,
                    "[Premium NX Gachapon] " + cm.getPlayer().getName() + " has pulled a ULTRA RARE item: " +
                    Packages.server.ItemInformationProvider.getInstance().getName(itemId) + "!"
                )
            );
        }

        cm.dispose();
    }
}

function pickRarity() {
    var roll = Math.random() * 100;
    var cumulative = 0;
    for (var key in rarityRates) {
        cumulative += rarityRates[key];
        if (roll <= cumulative) {
            return key;
        }
    }
    return "COMMON";
}
