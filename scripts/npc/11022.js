var status = -1;
var currentHandler = null;
var savedBuffChoice = 2;
const BILLION_COIN_ID = 3020002; // Item ID for "Billion Coin"

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
//    console.log(`Action called with mode: ${mode}, type: ${type}, selection: ${selection}`);
    if (mode === -1 || (mode === 0 && status === 0)) {
        cm.dispose();
        return;
    }

    status += (mode === 1) ? 1 : -1;
//    console.log("Status updated: " + status);

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
        finalizeSkillUnlock();  // Finalize skill unlock for self buff
    } else if (status === 11) {  // World Buff donation flow starts at status 10
        console.log("selection: " + selection);
        savedBuffChoice = selection;
        console.log("savedBuffChoice: " + savedBuffChoice);
        proceedToDonationDetails(selection);

    } else if (status === 12) {
        processDonation(selection);  // Process the donation after input for world buff
    } else if (status === 13) {
        finalizeDonation();  // Finalize the donation after processing for world buff
    }
}

// Proceed to donation details for world buff
function proceedToDonationDetails(selection) {
//    console.log(`Proceeding with donation for buff option: ${selection}`);

    // Validate selection
    if (selection !== 0 && selection !== 1) {
        cm.sendOk("Invalid selection. Please choose a valid buff option.");
        cm.dispose();
        return;
    }

    let buffChoice = (selection === 0) ? BUFF_OPTIONS["STANDARD"] : BUFF_OPTIONS["SUPER"];

    console.log("Selected buff choice: " + JSON.stringify(buffChoice));

    const currentProgress = cm.getBuffProgress(buffChoice.id);
            console.log("currentProgress: " + currentProgress);
    const threshold = buffChoice.cost;
    const NEEDED = Math.max(0, threshold - currentProgress);
//    console.log(`Current progress for ${buffChoice.name}: ${currentProgress} / ${threshold}, NEEDED: ${NEEDED}`);

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
//    console.log("Showing buff donation options.");
    let msg = "Which world buff would you like to contribute to?\r\n";
    msg += `#L0##bDonate to Standard World Buff (Cost: 5 Billion Coins)#l\r\n`;
    msg += `#L1##bDonate to Super World Buff (Cost: 25 Billion Coins)#l\r\n`;
    cm.sendSimple(msg);
}



function processDonation(mode) {
    console.log(`Proceeding with donation, mode: ${mode}`);

    if (mode === 0) {
        cm.sendOk("Come back when you're ready to donate.");
        cm.dispose();
        return;
    }

    // Retrieve the selected buff based on current status
    console.log("savedBuffChoice: " + savedBuffChoice);
    const buffChoice = (savedBuffChoice === 0) ? BUFF_OPTIONS["STANDARD"] : BUFF_OPTIONS["SUPER"];
    console.log("Buff choice (retrieved from status): " + JSON.stringify(buffChoice));

    // Log donation mode and other parameters
    console.log("Donation Mode: " + mode);
    console.log("Buff Choice: " + JSON.stringify(buffChoice));

    // Retrieve the donation amount from user input
    const donationAmount = parseInt(cm.getText());
    console.log("Donation amount entered: " + donationAmount);

    // Check if donation amount is valid
    if (isNaN(donationAmount) || donationAmount <= 0) {
        cm.sendOk("Please enter a valid positive number of coins to donate.");
        cm.dispose();
        return;
    }

    // Log the current buff progress and the threshold needed
    const currentProgress = cm.getBuffProgress(buffChoice.id);
    const threshold = buffChoice.cost;
    console.log(`Current progress: ${currentProgress}, Threshold: ${threshold}`);

    // Calculate how many more coins are needed for the buff to be unlocked
    const NEEDED = Math.max(0, threshold - currentProgress);
    console.log(`NEEDED (remaining): ${NEEDED}`);

    if (donationAmount > NEEDED) {
        cm.sendOk("You're donating more than needed. Please enter a valid amount.");
        cm.dispose();
        return;
    }

    // Ensure the player has enough coins
    const hasCoins = cm.haveItem(BILLION_COIN_ID, donationAmount);
    console.log(`Does the player have enough coins? ${hasCoins}`);

    if (!hasCoins) {
        cm.sendOk(`You don't have ${donationAmount} #t${BILLION_COIN_ID}#(s).`);
        cm.dispose();
        return;
    }

    // Deduct the donation amount from the player
    console.log(`Deducting ${donationAmount} #t${BILLION_COIN_ID}#(s) from the player.`);
    cm.gainItem(BILLION_COIN_ID, -donationAmount);

    // Log the updated progress after the donation
    cm.updateBuffProgress(buffChoice.id, donationAmount);
    const newTotalProgress = cm.getBuffProgress(buffChoice.id);
    console.log(`New total progress after donation: ${newTotalProgress} / ${threshold}`);

    // If the donation goal is met, apply the buffs and finalize
    if (newTotalProgress >= threshold) {
        console.log("Donation goal reached, applying buffs.");
        cm.sendOk(`Thanks for your contribution of ${donationAmount} #t${BILLION_COIN_ID}#(s)! The ${buffChoice.name} goal has been reached! Buffs will be applied shortly.`);
        cm.broadcastWorldMessage(5, `${cm.getPlayer().getName()} has completed the funding for the ${buffChoice.name}! Buffs incoming!`);
        cm.applyGlobalBuff(buffChoice.skills);
        cm.resetBuffProgress(buffChoice.id);
    } else {
        console.log("Donation goal not yet met.");
        cm.sendOk(`Thanks for donating ${donationAmount} #t${BILLION_COIN_ID}#(s)! ${buffChoice.name} is now at ${newTotalProgress} / ${threshold}.`);
    }

    cm.dispose();
}

// Show skills for self buff
function showSkills() {
//    console.log("Showing skills for selection.");
    let msg = "Choose a skill to unlock (requires items):\r\n";
    skills.forEach((s, i) => {
        msg += `#L${i}##b${s.name}#l\r\n`;
    });
    cm.sendSimple(msg);
}

// Proceed with self buff skill unlock
function proceedToSkillUnlock(selection) {
//    console.log(`Proceeding with skill unlock for selection: ${selection}`);
    const skill = skills[selection];
    let costMsg = `To unlock #b${skill.name}#k, bring me the following items:\r\n`;
    skill.costItems.forEach(item => {
        costMsg += `#v${item.itemId}# x ${item.quantity}\r\n`;
    });
    costMsg += "\r\nDo you want to proceed and trade these items for power?";
    cm.sendYesNo(costMsg);
}