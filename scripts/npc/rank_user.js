var status = -1;
var currentHandler = null;
const BILLION_COIN_ID = 3020002; // Item ID for "Billion Coin"

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
            { itemId: BILLION_COIN_ID, quantity: 2 }, // 2 billion coins - confirm this is item id or currency
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
            { itemId: BILLION_COIN_ID, quantity: 2 }, // 2 billion coins - confirm this is item id or currency
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
            { itemId: BILLION_COIN_ID, quantity: 3 }, // 3 billion coins - confirm this is item id or currency
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
            { itemId: BILLION_COIN_ID, quantity: 2 }, // 2 billion coins - confirm this is item id or currency
            { itemId: 4000371, quantity: 10000 }, // Speed Limit Sign - A piece of metal indicating speed limitation warning.
            { itemId: 2002001, quantity: 32000 }, // Speed Potion
            { itemId: 2002010, quantity: 32000 }, // Speed Pill.
            { itemId: 2040706, quantity: 1337 }, // Scroll for Shoes for Speed.
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

/***********************************************************************************************************************
 ******************************************* START LIQUID LOGIC ********************************************************
 **********************************************************************************************************************/
const Liquid = {
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

        skill.costItems.forEach(item => cm.gainItem(item.itemId, -item.quantity));

        if (player.updateUnlockedSkills) {
            player.updateUnlockedSkills(skill.id);
        }

        cm.sendOk(`Excellent! You have earned the skill #b${skill.name}#k.\r\nUse it wisely by typing @powerup.`);
        cm.dispose();
    }
};
const LiquidBera = {...Liquid}; // Clone of Liquid
/***********************************************************************************************************************
 ********************************************* END LIQUID LOGIC ********************************************************
 **********************************************************************************************************************/

/***********************************************************************************************************************
 ********************************************* START Irrigation  LOGIC *************************************************
 **********************************************************************************************************************/
const BUFF_OPTIONS = {
    STANDARD: {
        key: 'STANDARD',         // Internal key for the script
        id: 'standard_buff',     // Database identifier (buff_type_id)
        name: "Standard World Buff",
        cost: 5,                 // Cost in Billion Coins
        skills: [9101001, 9101002, 9101003, 9101008, 1005],
    },
    SUPER: {
        key: 'SUPER',
        id: 'super_buff',
        name: "Super World Buff",
        cost: 25,
        skills: [2311003, 2301004, 1301007, 4101004, 2001002, 1005, 2301003, 5121009, 4111001, 4211003, 4211005, 1321000, 2321004, 3121002],
    }
};

let NEEDED = null

function proceedToDonationDetails() {
    const buffChoice = BUFF_OPTIONS[this.selectedBuffKey];
    if (!buffChoice) {
        cm.sendOk("An error occurred. Please try talking to me again.");
        cm.dispose();
        return;
    }

    const currentProgress = cm.getBuffProgress(buffChoice.id);
    const threshold = buffChoice.cost;
    NEEDED = Math.max(0, threshold - currentProgress);

    if (NEEDED === 0) {
        cm.sendOk(`Looks like the ${buffChoice.name} goal is already met! Try again in a bit.`);
        cm.dispose();
        return;
    }

    let msg = `You're looking to contribute to the #b${buffChoice.name}#k.\r\n`;
    msg += `It requires a total of #r${threshold}#k #t${BILLION_COIN_ID}#s.\r\n`;
    msg += `Current Progress: #g${currentProgress} / ${threshold}#k.\r\n`;
    msg += `#r${NEEDED}#k more #t${BILLION_COIN_ID}#(s) needed to unleash the buff!\r\n\r\n`;
    msg += `How many #t${BILLION_COIN_ID}# #i${BILLION_COIN_ID}# would you like to donate? (Up to ${NEEDED})`;

    cm.sendGetText(msg, '0');
}


const Irrigation = {
    selectedBuffKey: null,

    step0() {
        cm.sendNext("Heard you were looking for Irrigation? Yeah, that's me. Some say I help noobs, some say I enable them... I say, 'Why not both?' Got some #t" + BILLION_COIN_ID + "# burning a hole in your pocket for a good cause?");
    },

    step1() {
        let msg = "Alright, let's talk business. We're running a couple of 'community empowerment programs'. For the community, by the community. Your spare #b#t" + BILLION_COIN_ID + "##k can make a big difference:\r\n";
        msg += `#L0##bDonate to ${BUFF_OPTIONS.STANDARD.name} (#r${BUFF_OPTIONS.STANDARD.cost} Coins Total#k)#l\r\n`;
        msg += `#L1##bDonate to ${BUFF_OPTIONS.SUPER.name} (#r${BUFF_OPTIONS.SUPER.cost} Coins Total#k)#l\r\n`;
        msg += `#L2##bTell me more about these 'programs'.#l\r\n`;
        msg += `#L3##bActually, never mind.#l`;
        cm.sendSimple(msg);
    },

    step2(selection) {
        if (selection === 0) {
            this.selectedBuffKey = BUFF_OPTIONS.STANDARD.key;
            this.proceedToDonationDetails();
        } else if (selection === 1) {
            this.selectedBuffKey = BUFF_OPTIONS.SUPER.key;
            this.proceedToDonationDetails();
        } else if (selection === 2) {
            let explanation = "So, here's the deal, simple like a Slime's brain:\r\n\r\n";
            explanation += `1. #b${BUFF_OPTIONS.STANDARD.name}#k:\nCollect #r${BUFF_OPTIONS.STANDARD.cost}#k #t${BILLION_COIN_ID}#s, and BOOM! Everyone online gets a nice set of buffs.\r\n\r\n`;
            explanation += `2. #b${BUFF_OPTIONS.SUPER.name}#k:\nThis one's for the high rollers. Needs #r${BUFF_OPTIONS.SUPER.cost}#k #t${BILLION_COIN_ID}#s. If you manage that, everyone gets a taste of godhood.\r\n\r\n`;
            explanation += "When a goal is met, the buffs fire off, the donation counter resets, and we start the fun again. Any questions, or ready to contribute?";
            cm.sendNextPrev(explanation);
            cm.setNextAction("step1");
        } else if (selection === 3) {
            cm.sendOk("Scared of commitment, huh? Or just broke? Either way, the world remains... less buffed. Your loss!");
            cm.dispose();
        }
    },


    step3() {
        var quantity = parseInt(cm.getText());

        console.log()

        if (quantity > NEEDED) {
            cm.sendOk("Donating more than needed is just a waste of money!");
            cm.dispose();
            return;
        }

        if (isNaN(quantity) || quantity <= 0) {
            cm.sendOk("That is not a valid number. Please enter a positive number of coins to donate.");
            cm.dispose();
            return;
        }

        if (quantity <= 0) {
            cm.sendOk("Donating zero? Very funny. Try again when you're serious.");
            cm.dispose();
            return;
        }

        const buffChoice = BUFF_OPTIONS[this.selectedBuffKey];
        if (!buffChoice) {
            cm.sendOk("Something went wrong. Please restart.");
            cm.dispose();
            return;
        }

        if (!cm.haveItem(BILLION_COIN_ID, quantity)) {
            cm.sendOk(`You don't have ${quantity} #t${BILLION_COIN_ID}#(s).`);
            cm.dispose();
            return;
        }

        cm.gainItem(BILLION_COIN_ID, -quantity);
        const newTotalProgress = cm.updateBuffProgress(buffChoice.id, quantity);

        if (newTotalProgress >= buffChoice.cost) {
            cm.sendOk(`Incredible! Your donation of ${quantity} #t${BILLION_COIN_ID}#(s) has completed the ${buffChoice.name}!`);
            cm.broadcastWorldMessage(5, `${cm.getPlayer().getName()} has completed the funding for the ${buffChoice.name}! Buffs incoming!`);
            cm.applyGlobalBuff(buffChoice.skills);
            cm.resetBuffProgress(buffChoice.id);
            cm.dispose();
        } else {
            cm.sendOk(`Thanks for donating ${quantity} #t${BILLION_COIN_ID}#(s)! ${buffChoice.name} is now at ${newTotalProgress} / ${buffChoice.cost}.`);
            cm.dispose();
        }
    }
};
const IrrigationBera = {...Irrigation}; // Clone of Irrigation
/***********************************************************************************************************************
 ********************************************* END Irrigation LOGIC ****************************************************
 **********************************************************************************************************************/

const npcLogic = {};

npcLogic["Liquid"] = Liquid;
npcLogic["LiquidBera"] = LiquidBera;
npcLogic["Irrigation"] = Irrigation;
npcLogic["IrrigationBera"] = IrrigationBera;
