var status = -1;
var currentHandler = null;
var savedBuffChoice = 2;
const BILLION_COIN_ID = 3020002; // Item ID for "Billion Coin"
var selectedSkillIndex = -1; // which self-buff skill player selected

var skills = [
    {
        id: 4101004,
        name: "Haste | MAX",
        costItems: [
            { itemId: BILLION_COIN_ID, quantity: 1 }
        ]
    },
    {
        id: 4111001,
        name: "Meso Up | 15",
        costItems: [
            { itemId: BILLION_COIN_ID, quantity: 2 },
            { itemId: 4020000, quantity: 300 },
            { itemId: 4020001, quantity: 300 },
            { itemId: 4020002, quantity: 300 },
            { itemId: 4020003, quantity: 300 },
            { itemId: 4020004, quantity: 300 },
            { itemId: 4020005, quantity: 300 },
            { itemId: 4020006, quantity: 300 },
            { itemId: 4020007, quantity: 300 }
        ]
    },
    {
        id: 2311003,
        name: "Holy Symbol | 15",
        costItems: [
            { itemId: BILLION_COIN_ID, quantity: 2 },
            { itemId: 4000001, quantity: 1500 },
            { itemId: 4000009, quantity: 1500 },
            { itemId: 4000012, quantity: 1500 },
            { itemId: 4000015, quantity: 1500 },
            { itemId: 4000500, quantity: 1500 },
            { itemId: 4000008, quantity: 1500 },
            { itemId: 4000040, quantity: 1 }
        ]
    },
    {
        id: 3121002,
        name: "Sharp eyes | 20",
        costItems: [
            { itemId: BILLION_COIN_ID, quantity: 3 },
            { itemId: 4000007, quantity: 3000 },
            { itemId: 4000013, quantity: 3000 },
            { itemId: 4000023, quantity: 3000 },
            { itemId: 4000076, quantity: 3000 },
            { itemId: 4000027, quantity: 3000 },
            { itemId: 1032008, quantity: 1 },
            { itemId: 4001017, quantity: 3 }
        ]
    },
    {
        id: 5121009,
        name: "Speed infusion | MAX",
        costItems: [
            { itemId: BILLION_COIN_ID, quantity: 2 },
            { itemId: 4000371, quantity: 10000 },
            { itemId: 2002001, quantity: 32000 },
            { itemId: 2002010, quantity: 32000 },
            { itemId: 2040706, quantity: 1337 }
        ]
    }
];

const BUFF_OPTIONS = {
    STANDARD: {
        key: 'STANDARD',
        id: 'standard_buff',
        name: "Standard World Buff",
        cost: 5,
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

// Declare the SkillUpgradeManager
var SelfBuffSkillUpgradeManager = Java.type("server.buffnpc.SelfBuffSkillUpgradeManager");

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

    if (status === 0) {
        cm.sendSimple("Welcome! I can help you unlock powerful skills or contribute to world buffs.\r\n#L0##bUnlock powerful skills#l\r\n#L1##bContribute to world buffs#l");
    } else if (status === 1) {
        if (selection === 0) {
            showSkills();  // Show skills for self buff
        } else if (selection === 1) {
            showBuffOptions();  // Show world buff options
            status = 10;  // Start world buff flow from status 10
        }
    } else if (status === 2) {
        proceedToSkillUnlock(selection);  // Proceed with skill unlock for self buff
    } else if (status === 3) {
        finalizeSkillUnlock(mode);  // Finalize skill unlock for self buff
    } else if (status === 11) {  // World Buff donation flow starts at status 10
        savedBuffChoice = selection;
        proceedToDonationDetails(selection);
    } else if (status === 12) {
        processDonation(selection);  // Process the donation after input for world buff
    } else if (status === 13) {
        finalizeDonation();  // Finalize the donation after processing for world buff
    }
}

// Proceed to donation details for world buff
function proceedToDonationDetails(selection) {
    if (selection !== 0 && selection !== 1) {
        cm.sendOk("Invalid selection. Please choose a valid buff option.");
        cm.dispose();
        return;
    }

    let buffChoice = (selection === 0) ? BUFF_OPTIONS["STANDARD"] : BUFF_OPTIONS["SUPER"];

    const currentProgress = cm.getBuffProgress(buffChoice.id);
    const threshold = buffChoice.cost;
    const NEEDED = Math.max(0, threshold - currentProgress);

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

// Handle world buff donation options
function showBuffOptions() {
    let msg = "Which world buff would you like to contribute to?\r\n";
    msg += `#L0##bDonate to Standard World Buff (Cost: 5 Billion Coins)#l\r\n`;
    msg += `#L1##bDonate to Super World Buff (Cost: 25 Billion Coins)#l\r\n`;
    cm.sendSimple(msg);
}

function processDonation(mode) {
    if (mode === 0) {
        cm.sendOk("Come back when you're ready to donate.");
        cm.dispose();
        return;
    }

    const buffChoice = (savedBuffChoice === 0) ? BUFF_OPTIONS["STANDARD"] : BUFF_OPTIONS["SUPER"];

    const donationAmount = parseInt(cm.getText());

    if (isNaN(donationAmount) || donationAmount <= 0) {
        cm.sendOk("Please enter a valid positive number of coins to donate.");
        cm.dispose();
        return;
    }

    const currentProgress = cm.getBuffProgress(buffChoice.id);
    const threshold = buffChoice.cost;
    const NEEDED = Math.max(0, threshold - currentProgress);

    if (donationAmount > NEEDED) {
        cm.sendOk("You're donating more than needed. Please enter a valid amount.");
        cm.dispose();
        return;
    }

    const hasCoins = cm.haveItem(BILLION_COIN_ID, donationAmount);

    if (!hasCoins) {
        cm.sendOk(`You don't have ${donationAmount} #t${BILLION_COIN_ID}#(s).`);
        cm.dispose();
        return;
    }

    cm.gainItem(BILLION_COIN_ID, -donationAmount);
    cm.updateBuffProgress(buffChoice.id, donationAmount);
    const newTotalProgress = cm.getBuffProgress(buffChoice.id);

    if (newTotalProgress >= threshold) {
        cm.sendOk(`Thanks for your contribution of ${donationAmount} #t${BILLION_COIN_ID}#(s)! The ${buffChoice.name} goal has been reached! Buffs will be applied shortly.`);
        cm.broadcastWorldMessage(5, `${cm.getPlayer().getName()} has completed the funding for the ${buffChoice.name}! Buffs incoming!`);
        cm.applyGlobalBuff(buffChoice.skills);
        cm.resetBuffProgress(buffChoice.id);
    } else {
        cm.sendOk(`Thanks for donating ${donationAmount} #t${BILLION_COIN_ID}#(s)! ${buffChoice.name} is now at ${newTotalProgress} / ${threshold}.`);
    }

    cm.dispose();
}

// Show skills for self buff
function showSkills() {
    let msg = "Choose a skill to unlock (requires items):\r\n";
    skills.forEach((s, i) => {
        msg += `#L${i}##b${s.name}#l\r\n`;
    });
    cm.sendSimple(msg);
}

// Proceed with self buff skill unlock
function proceedToSkillUnlock(selection) {
    selectedSkillIndex = selection;
    const skill = skills[selection];
    let costMsg = `To unlock #b${skill.name}#k, bring me the following items:\r\n`;
    skill.costItems.forEach(item => {
        costMsg += `#v${item.itemId}# x ${item.quantity}\r\n`;
    });
    costMsg += "\r\nDo you want to proceed and trade these items for power?";
    cm.sendYesNo(costMsg);
}

function finalizeSkillUnlock(mode) {
    if (mode === 0) {
        cm.sendOk("Come back when you're ready.");
        cm.dispose();
        return;
    }

    if (selectedSkillIndex < 0 || selectedSkillIndex >= skills.length) {
        cm.sendOk("Something went wrong (invalid skill selection). Try again.");
        cm.dispose();
        return;
    }

    const player = cm.getPlayer();
    const skill = skills[selectedSkillIndex];

    var already = SelfBuffSkillUpgradeManager.hasSkill(player, skill.id);
    if (already) {
        cm.sendOk("You already have unlocked this skill.");
        cm.dispose();
        return;
    }

    const hasAll = skill.costItems.every(item => cm.haveItem(item.itemId, item.quantity));
    if (!hasAll) {
        cm.sendOk("You don't have all the required items. Come back when you have collected them all.");
        cm.dispose();
        return;
    }

    skill.costItems.forEach(item => cm.gainItem(item.itemId, -item.quantity));

    var success = SelfBuffSkillUpgradeManager.unlockSkill(player, skill.id);
    if (!success) {
        cm.sendOk("Something went wrong while saving. Please contact a GM.");
        cm.dispose();
        return;
    }

    cm.sendOk(`Excellent! You have earned the skill #b${skill.name}#k.`);
    cm.dispose();
}
