var status = -1;
var selectedSkill = 0;

var skills = [
    {
        id: 4101004,
        name: "Haste | MAX",
        costItems: [
            { itemId: 4001253, quantity: 1 } // 1 billionCoin, you can adjust if needed
        ]
    },
    {
        id: 4111001,
        name: "Meso Up | 15",
        costItems: [
            { itemId: 4001253, quantity: 2 }, // 2 billion coins - confirm this is item id or currency
            { itemId: 4010000, quantity: 300 }, // Bronze Ore
            { itemId: 4010001, quantity: 300 }, // Steel Ore
            { itemId: 4010002, quantity: 300 }, // Mithril Ore
            { itemId: 4010003, quantity: 300 }, // Adamantium Ore
            { itemId: 4010004, quantity: 300 }, // Silver Ore
            { itemId: 4010006, quantity: 300 }, // Gold Ore
            { itemId: 4020000, quantity: 300 }, // Garnet Ore
            { itemId: 4020001, quantity: 300 }, // Amethyst Ore
            { itemId: 4020002, quantity: 300 }, // AquaMarine Ore
            { itemId: 4020003, quantity: 300 }, // Emerald Ore
            { itemId: 4020004, quantity: 300 }, // Opal Ore
            { itemId: 4020005, quantity: 300 }, // Sapphire Ore
            { itemId: 4020006, quantity: 300 }, // Topaz Ore
            { itemId: 4020007, quantity: 300 }  // Diamond Ore
        ]
    },
    {
        id: 2311003,
        name: "Holy Symbol | 15",
        costItems: [
            { itemId: 4001253, quantity: 2 }, // 2 billion coins - confirm this is item id or currency
            { itemId: 4000001, quantity: 1500 }, // Orange Mushroom Cap - A cap removed from a mushroom
            { itemId: 4000009, quantity: 1500 }, // Blue Mushroom Cap - A cap removed from a mushroom
            { itemId: 4000012, quantity: 1500 }, // Green Mushroom Cap - A cap removed from a mushroom
            { itemId: 4000015, quantity: 1500 }, // Horny Mushroom Cap - A cap removed from a mushroom
            { itemId: 4000500, quantity: 1500 }, // Poison Mushroom Cap - A cap from the Poison Mushroom. It contains potent poison.
            { itemId: 4000008, quantity: 1500 }, // Charm of the Undead - A charm taken out of an undead monster.
            { itemId: 4000008, quantity: 1500 }, // Charm of the Undead - A charm taken out of an undead monster.
            { itemId: 4000040, quantity: 1 }, // Mushmom Spore - A spore from Mushmom, a humongous mushroom
        ]
    },
    {
        id: 3121002, // placeholder id for Meso Up, you can change as needed
        name: "Sharp eyes | 20",
        costItems: [
            { itemId: 4001253, quantity: 3 }, // 3 billion coins - confirm this is item id or currency
            { itemId: 4000007, quantity: 3000 }, // Evil Eye Tail - A tail removed from a lizard
            { itemId: 4000013, quantity: 3000 }, // Curse Eye Tail - A tail removed from a lizard
            { itemId: 4000023, quantity: 3000 }, // Cold Eye Tail - A tail removed from a lizard
            { itemId: 4000076, quantity: 3000 }, // Fly-Eye Wing - A wing removed from Fly-Eye. Spiky and very dirty.
            { itemId: 1032008, quantity: 3000 }, // Cat's Eye - Earring
            { itemId: 4000027, quantity: 3000 }, // Wild Kargo Eye - An eye removed from Wild Kargo
            { itemId: 4001017, quantity: 3 }, // Eye of Fire
        ]
    },
    {
        id: 5121009, // placeholder id for Meso Up, you can change as needed
        name: "Speed infusion | MAX",
        costItems: [
            { itemId: 4001253, quantity: 2 }, // 2 billion coins - confirm this is item id or currency
            { itemId: 4000371, quantity: 10000 }, // Speed Limit Sign - A piece of metal indicating speed limitation warning.
            { itemId: 2002001, quantity: 99999 }, // Speed Potion
            { itemId: 2002010, quantity: 99999 }, // Speed Pill.
        ]
    }
];

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 0 && status == 0) {
        cm.sendOk("Come back when you want to get stronger.");
        cm.dispose();
        return;
    }
    if (mode == 1)
        status++;
    else
        status--;

    if (status == 0) {
        cm.sendNext("So you only care about yourself huh? I get that sometimes we all need to be selfish.");
    } else if (status == 1) {
        cm.sendNext("I am Liquid and I can make you very powerful but you have to earn that. With blood, sweat and possibly a numb finger.");
    } else if (status == 2) {
        cm.sendNext("Would you like to see what I can do for you?");
    } else if (status == 3) {
        var selStr = "So you would like to earn these skills huh?\r\n";
        for (var i = 0; i < skills.length; i++) {
            selStr += "#b#L" + i + "#" + skills[i].name + "#l\r\n";
        }
        cm.sendSimple(selStr);
    }
    else if (status == 4) {
        selectedSkill = selection;
        var skill = skills[selectedSkill];
        var costStr = "Aah, you want to earn the skill #b" + skill.name + "#k huh? Well bring me the following items:\r\n";
        for (var i = 0; i < skill.costItems.length; i++) {
            costStr += "#v" + skill.costItems[i].itemId + "# x " + skill.costItems[i].quantity + "\r\n";
        }
        cm.sendYesNo(costStr);
    } else if (status == 5) {
        var skill = skills[selectedSkill];
        // Check items
        var hasAllItems = true;
        for (var i = 0; i < skill.costItems.length; i++) {
            if (!cm.haveItem(skill.costItems[i].itemId, skill.costItems[i].quantity)) {
                hasAllItems = false;
                break;
            }
        }
        if (hasAllItems) {
            var player = cm.getPlayer();

            if (player.hasUnlockedSkill(skill.id)) {
                cm.sendOk("You already have the skill #b" + skill.name + "#k unlocked.");
            } else {
                // Remove items
                for (var i = 0; i < skill.costItems.length; i++) {
                    cm.gainItem(skill.costItems[i].itemId, -skill.costItems[i].quantity);
                }
                // Grant skill or buff here
                player.updateUnlockedSkills(skill.id);
                cm.sendOk("Excellent! You have earned the skill #b" + skill.name + "#k. Use it wisely by typing! @powerup");
            }
        } else {
            cm.sendOk("You do not have all the required items. Come back when you have collected them all.");
        }
        cm.dispose();
    } else {
        cm.dispose();
    }
}