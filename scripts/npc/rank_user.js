var status = -1;
var currentHandler = null;

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
            { itemId: 4000027, quantity: 3000 }, // Wild Kargo Eye - An eye removed from Wild Kargo
            { itemId: 1032008, quantity: 1 }, // Cat's Eye - Earring
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
    currentHandler = null;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === -1 || (mode === 0 && status === 0)) {
        cm.dispose();
        return;
    }

    status += (mode === 1) ? 1 : -1;

    const pnpc = cm.getPlayerNPCByScriptid(cm.getNpc());
    if (!pnpc) {
        cm.sendOk("Hi, how're you doing?");
        cm.dispose();
        return;
    }

    const npcName = pnpc.getName();

    // Assign handler once
    if (status === 0) {
        if (npcLogic[npcName]) {
            currentHandler = npcLogic[npcName];
            currentHandler.init?.(pnpc); // optional init
        } else {
            const GameConstants = Java.type('constants.game.GameConstants');
            var branchJobName = GameConstants.getJobName(pnpc.getJob());
            var rankStr = "Hi, I am #b" + pnpc.getName() + "#k, #r" + GameConstants.ordinal(pnpc.getWorldJobRank()) + "#k in the #r" + branchJobName + "#k class to reach the max level and obtain a statue on " + GameConstants.WORLD_NAMES[cm.getPlayer().getWorld()] + ".\r\n";
            rankStr += "\r\n    World rank: #e#b" + GameConstants.ordinal(pnpc.getWorldRank()) + "#k#n";
            rankStr += "\r\n    Overall " + branchJobName + " rank: #e#b" + GameConstants.ordinal(pnpc.getOverallJobRank()) + "#k#n";
            rankStr += "\r\n    Overall rank: #e#b" + GameConstants.ordinal(pnpc.getOverallRank()) + "#k#n";

            cm.sendOk(rankStr);
            return;
        }
    }

    // Execute dynamic step method: step0, step1, step2, ...
    const stepFn = currentHandler?.[`step${status}`];
    if (typeof stepFn === "function") {
        stepFn(selection);
    } else {
        cm.dispose(); // No further steps
    }
}

const npcLogic = {
    "Liquid": {
        skills: skills,
        skillSelection: -1,

        step0() {
            cm.sendNext("So you only care about yourself, huh? I get that sometimes we all need to be selfish.");
        },

        step1() {
            cm.sendNextPrev("I am Liquid, and I can make you very powerful — but you must earn it with blood, sweat, and possibly a numb finger.");
        },

        step2() {
            cm.sendNextPrev("I've gathered forbidden knowledge — skills thought unattainable by many. But nothing comes free.");
        },

        step3() {
            cm.sendNextPrev("Would you like to see what I can do for you?");
        },

        step4() {
            let msg = "So you would like to earn these skills, huh?\r\n#r(Note: All skills require items to unlock!)#k\r\n";
            this.skills.forEach((s, i) => {
                msg += `#L${i}##b${s.name}#l\r\n`;
            });
            cm.sendSimple(msg);
        },

        step5(selection) {
            this.skillSelection = selection;
            const skill = this.skills[selection];

            let costMsg = `To unlock #b${skill.name}#k, bring me the following items:\r\n`;
            skill.costItems.forEach(item => {
                costMsg += `#v${item.itemId}# x ${item.quantity}\r\n`;
            });
            costMsg += "\r\nDo you want to proceed and trade these items for power?";
            cm.sendYesNo(costMsg);
        },

        step6(mode) {
            if (mode === 0) {
                cm.sendOk("Come back when you have the items to unlock this skill.");
                cm.dispose();
                return;
            }

            const skill = this.skills[this.skillSelection];
            const hasAll = skill.costItems.every(item => cm.haveItem(item.itemId, item.quantity));

            if (!hasAll) {
                cm.sendOk("You don't have all the required items. Come back when you have collected them all.");
                cm.dispose();
                return;
            }

            const player = cm.getPlayer();
            if (player.hasUnlockedSkill && player.hasUnlockedSkill(skill.id)) {
                cm.sendOk(`You already have the skill #b${skill.name}#k unlocked.`);
                cm.dispose();
                return;
            }

            // Remove items
            skill.costItems.forEach(item => cm.gainItem(item.itemId, -item.quantity));

            // Grant skill
            if (player.updateUnlockedSkills) {
                player.updateUnlockedSkills(skill.id);
            } else {
                cm.teachSkill(skill.id, 30, 30);
            }

            cm.sendOk(`Excellent! You have earned the skill #b${skill.name}#k.\r\nUse it wisely by typing @powerup.`);
            cm.dispose();
        }
    }
};

// Example of NPC logic. Increase steps by number, and it will work automatically
// npcLogic["Bob"] = {
//     init() {
//         this.stage = 0;
//     },
//     step0() {
//         cm.sendNext("Hey, this is step 0.");
//     },
//     step1() {
//         cm.sendNext("And now you're on step 1.");
//     },
//     step2() {
//         cm.sendNext("Almost done! Step 2.");
//     },
//     step3() {
//         cm.sendOk("Step 3 complete. All done!");
//         cm.dispose();
//     }
// };

