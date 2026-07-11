var status = 0;
var selectedItem;
var rewardSelection;
var currencyType;

// Define constants for currency IDs and amount needed
const DK_CURRENCY_ID = 4036550; // Donkey Kong currency with icon
const YOSHI_CURRENCY_ID = 4036553; // Yoshi currency with icon
const CURRENCY_NEEDED = 100;

const DK_REWARDS = [5002110, 5002111]; // Replace with actual DK JQ reward item IDs
const YOSHI_REWARDS = [5002100, 5002101, 5002102, 5002103, 5002104, 5002105, 5002106, 5002107, 5002108, 5002109]; // Replace with actual Yoshi JQ reward item IDs

const time = 180; // 6 months in days

function start() {
    cm.sendSimple("Which Jump Quest currency would you like to exchange?\r\n#b#L0#Donkey Kong JQ#l\r\n#L1#Yoshi Island JQ#l");
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose(); // Exit conversation
    } else {
        status++;
        if (status == 1) {
            currencyType = selection;
            cm.sendSimple("Select a reward to exchange for:\r\n" + getRewardOptions());
        } else if (status == 2) {
            rewardSelection = selection;
            selectedItem = (currencyType == 0) ? DK_REWARDS[rewardSelection] : YOSHI_REWARDS[rewardSelection];
            let currencyIcon = (currencyType == 0) ? DK_CURRENCY_ID : YOSHI_CURRENCY_ID;
            cm.sendYesNo("Are you sure you want to exchange " + CURRENCY_NEEDED + " #v" + currencyIcon + "# for #v" + selectedItem + "#?");
        } else if (status == 3) {
            let currencyId = (currencyType == 0) ? DK_CURRENCY_ID : YOSHI_CURRENCY_ID;
            if (cm.haveItem(currencyId, CURRENCY_NEEDED)) {
                cm.gainItem(currencyId, -CURRENCY_NEEDED); // Deduct currency
                
                // Correct expiration based on the example you provided
                cm.gainItem(selectedItem, 1, false, true, (time * 1000 * 60 * 60 * 24)); // 180 days expiration
                
                cm.sendOk("Trade successful! Enjoy your reward.");
            } else {
                cm.sendOk("You don't have enough #v" + currencyId + "# for this reward.");
            }
            cm.dispose(); // End the conversation
        }
    }
}

// Function to generate the reward selection menu
function getRewardOptions() {
    var options = "";
    var rewards = (currencyType == 0) ? DK_REWARDS : YOSHI_REWARDS;

    for (var i = 0; i < rewards.length; i++) {
        options += "#L" + i + "# #v" + rewards[i] + "# #l";
        
        // Format the display for better readability
        if ((i + 1) % 3 == 0) {
            options += "\r\n";
        }
    }
    return options;
}
