/*
 * ForgeMS Belt Progression
 * NPC: Mr. Thunder
 * NPC ID: 1022003
 *
 * Progression:
 * 15 RB    = Novice Belt
 * 50 RB    = Apprentice Belt
 * 125 RB   = Journeyman Belt
 * 250 RB   = Master Belt
 * 500 RB   = Smith's Belt
 * 1,000 RB = Forge Belt
 * 2,000 RB = Prestige Belt I
 * 3,000 RB = Prestige Belt II
 * 4,000 RB = Prestige Belt III
 * 5,000 RB = Prestige Belt IV
 *
 * Rules:
 * - The Novice Belt is awarded at 15 total rebirths.
 * - Every later tier requires and consumes the previous belt.
 * - Belt tiers cannot be skipped.
 * - Equipped belts are detected.
 * - A belt must be unequipped before it can be exchanged.
 */

var status = -1;
var menuSelection = -1;

var currentBeltId = 0;
var currentBeltName = "";
var nextBeltId = 0;
var nextBeltName = "";
var requiredRebirths = 0;

var belts = [
    {
        name: "Novice Belt",
        id: 1132000,
        rebirths: 15
    },
    {
        name: "Apprentice Belt",
        id: 1132001,
        rebirths: 50
    },
    {
        name: "Journeyman Belt",
        id: 1132002,
        rebirths: 125
    },
    {
        name: "Master Belt",
        id: 1132003,
        rebirths: 250
    },
    {
        name: "Smith's Belt",
        id: 1132004,
        rebirths: 500
    },
    {
        name: "Forge Belt",
        id: 1132005,
        rebirths: 1000
    },
    {
        name: "Prestige Belt I",
        id: 1132006,
        rebirths: 2000
    },
    {
        name: "Prestige Belt II",
        id: 1132007,
        rebirths: 3000
    },
    {
        name: "Prestige Belt III",
        id: 1132008,
        rebirths: 4000
    },
    {
        name: "Prestige Belt IV",
        id: 1132009,
        rebirths: 5000
    }
];

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
        return;
    }

    if (mode === 0) {
        cm.dispose();
        return;
    }

    status++;

    if (status === 0) {
        showMainMenu();
        return;
    }

    if (status === 1) {
        menuSelection = selection;

        if (menuSelection === 0) {
            prepareBeltExchange();
            return;
        }

        if (menuSelection === 1) {
            showBeltProgression();
            return;
        }

        if (menuSelection === 2) {
            showExchangeRules();
            return;
        }

        cm.dispose();
        return;
    }

    if (status === 2 && menuSelection === 0) {
        completeBeltExchange();
        return;
    }

    cm.dispose();
}

function showMainMenu() {
    cm.sendSimple(
        "Greetings, adventurer.\r\n\r\n" +
        "I am #bMr. Thunder#k, the Coalition's Master Beltsmith. " +
        "Your belt represents the trials you have endured and the strength " +
        "you have forged through rebirth.\r\n\r\n" +
        "What can I do for you?\r\n\r\n" +
        "#L0##bExchange belts.#k#l\r\n" +
        "#L1##bView belt progression.#k#l\r\n" +
        "#L2##bView exchange rules.#k#l"
    );
}

function prepareBeltExchange() {
    var rebirths = getTotalRebirths();
    var highestBeltIndex = getHighestOwnedBeltIndex();

    /*
     * Player owns no belt from the progression.
     * Their first reward is the Novice Belt at 15 rebirths.
     */
    if (highestBeltIndex === -1) {
        currentBeltId = 0;
        currentBeltName = "";

        nextBeltId = belts[0].id;
        nextBeltName = belts[0].name;
        requiredRebirths = belts[0].rebirths;

        if (rebirths < requiredRebirths) {
            cm.sendOk(
                "Your first belt is:\r\n\r\n" +
                "#i" + nextBeltId + "# #b" + nextBeltName + "#k\r\n\r\n" +
                "Required total rebirths: #r" +
                formatNumber(requiredRebirths) + "#k\r\n" +
                "Your total rebirths: #b" +
                formatNumber(rebirths) + "#k\r\n\r\n" +
                "You need #r" +
                formatNumber(requiredRebirths - rebirths) +
                " more rebirths#k before you can receive it."
            );

            cm.dispose();
            return;
        }

        if (!cm.canHold(nextBeltId)) {
            cm.sendOk(
                "Your equipment inventory is full.\r\n\r\n" +
                "Make at least one equipment slot available and speak with me again."
            );

            cm.dispose();
            return;
        }

        cm.sendYesNo(
            "You have reached #b" +
            formatNumber(rebirths) +
            " total rebirths#k and earned your first progression belt.\r\n\r\n" +
            "You will receive:\r\n\r\n" +
            "#i" + nextBeltId + "# #b" + nextBeltName + "#k\r\n\r\n" +
            "Would you like to claim it?"
        );

        return;
    }

    /*
     * Player already owns the final progression belt.
     */
    if (highestBeltIndex >= belts.length - 1) {
        var finalBelt = belts[belts.length - 1];

        cm.sendOk(
            "You have completed the ForgeMS belt progression.\r\n\r\n" +
            "#i" + finalBelt.id + "# #b" + finalBelt.name + "#k\r\n\r\n" +
            "You have forged every belt currently available."
        );

        cm.dispose();
        return;
    }

    var currentBelt = belts[highestBeltIndex];
    var nextBelt = belts[highestBeltIndex + 1];

    currentBeltId = currentBelt.id;
    currentBeltName = currentBelt.name;

    nextBeltId = nextBelt.id;
    nextBeltName = nextBelt.name;
    requiredRebirths = nextBelt.rebirths;

    /*
     * The player owns the correct belt, but has not reached the next
     * rebirth milestone.
     */
    if (rebirths < requiredRebirths) {
        cm.sendOk(
            "Your next belt is:\r\n\r\n" +
            "#i" + nextBeltId + "# #b" + nextBeltName + "#k\r\n\r\n" +
            "Required total rebirths: #r" +
            formatNumber(requiredRebirths) + "#k\r\n" +
            "Your total rebirths: #b" +
            formatNumber(rebirths) + "#k\r\n\r\n" +
            "You need #r" +
            formatNumber(requiredRebirths - rebirths) +
            " more rebirths#k before you can exchange belts."
        );

        cm.dispose();
        return;
    }

    /*
     * The belt may be equipped. It must be moved into the player's
     * EQUIP inventory before gainItem can safely consume it.
     */
    if (!hasUnequippedItem(currentBeltId)) {
        cm.sendOk(
            "You possess the required belt, but it appears to be equipped.\r\n\r\n" +
            "#i" + currentBeltId + "# #b" + currentBeltName + "#k\r\n\r\n" +
            "Unequip it and place it in your #bEQUIP inventory#k before " +
            "attempting the exchange."
        );

        cm.dispose();
        return;
    }

    cm.sendYesNo(
        "You have met the requirement for your next belt.\r\n\r\n" +
        "#i" + currentBeltId + "# #b" + currentBeltName + "#k\r\n" +
        "#e                         ↓#n\r\n" +
        "#i" + nextBeltId + "# #b" + nextBeltName + "#k\r\n\r\n" +
        "Required total rebirths: #r" +
        formatNumber(requiredRebirths) + "#k\r\n" +
        "Your total rebirths: #b" +
        formatNumber(rebirths) + "#k\r\n\r\n" +
        "#rYour current belt will be consumed during the exchange.#k\r\n\r\n" +
        "Would you like to exchange belts?"
    );
}

function completeBeltExchange() {
    var rebirths = getTotalRebirths();

    /*
     * Recheck the rebirth requirement before changing anything.
     */
    if (rebirths < requiredRebirths) {
        cm.sendOk(
            "You no longer meet the rebirth requirement for this exchange."
        );

        cm.dispose();
        return;
    }

    /*
     * Initial Novice Belt reward.
     */
    if (currentBeltId === 0) {
        if (ownsAnyProgressionBelt()) {
            cm.sendOk(
                "You already possess a belt from this progression."
            );

            cm.dispose();
            return;
        }

        if (!cm.canHold(nextBeltId)) {
            cm.sendOk(
                "Your equipment inventory is full.\r\n\r\n" +
                "Make at least one equipment slot available and speak with me again."
            );

            cm.dispose();
            return;
        }

        cm.gainItem(nextBeltId, 1);

        cm.sendOk(
            "Your journey through the forge has begun.\r\n\r\n" +
            "You received:\r\n\r\n" +
            "#i" + nextBeltId + "# #b" + nextBeltName + "#k\r\n\r\n" +
            "Continue earning rebirths and return when you are ready " +
            "for the next exchange."
        );

        cm.dispose();
        return;
    }

    /*
     * Confirm that the exact previous belt is still unequipped and
     * inside the EQUIP inventory.
     */
    if (!hasUnequippedItem(currentBeltId)) {
        cm.sendOk(
            "The required belt could not be found in your EQUIP inventory.\r\n\r\n" +
            "The exchange has been cancelled."
        );

        cm.dispose();
        return;
    }

    /*
     * Prevent an exchange if the player somehow obtained a higher belt
     * while the confirmation window was open.
     */
    var highestBeltIndex = getHighestOwnedBeltIndex();
    var expectedCurrentIndex = getBeltIndexById(currentBeltId);

    if (highestBeltIndex !== expectedCurrentIndex) {
        cm.sendOk(
            "Your belt progression changed before the exchange was completed.\r\n\r\n" +
            "Please speak with me again."
        );

        cm.dispose();
        return;
    }

    /*
     * Removing the current belt creates the inventory slot needed for
     * the next belt.
     */
    cm.gainItem(currentBeltId, -1);
    cm.gainItem(nextBeltId, 1);

    cm.sendOk(
        "The exchange is complete.\r\n\r\n" +
        "You received:\r\n\r\n" +
        "#i" + nextBeltId + "# #b" + nextBeltName + "#k\r\n\r\n" +
        "Wear it as proof of everything you have forged."
    );

    cm.dispose();
}

/*
 * Finds the highest belt owned by the player.
 *
 * The second argument being true makes the check include equipped items.
 * This prevents equipped belts from being mistaken for missing belts.
 */
function getHighestOwnedBeltIndex() {
    for (var i = belts.length - 1; i >= 0; i--) {
        if (hasItemAnywhere(belts[i].id)) {
            return i;
        }
    }

    return -1;
}

/*
 * Checks the player's inventory and equipped items.
 */
function hasItemAnywhere(itemId) {
    return Number(
        cm.getPlayer().getItemQuantity(itemId, true)
    ) > 0;
}

/*
 * Checks only normal inventory storage.
 *
 * False excludes equipped items, so this confirms the belt is available
 * for gainItem to consume.
 */
function hasUnequippedItem(itemId) {
    return Number(
        cm.getPlayer().getItemQuantity(itemId, false)
    ) > 0;
}

function ownsAnyProgressionBelt() {
    return getHighestOwnedBeltIndex() >= 0;
}

function getBeltIndexById(itemId) {
    for (var i = 0; i < belts.length; i++) {
        if (belts[i].id === itemId) {
            return i;
        }
    }

    return -1;
}

function showBeltProgression() {
    cm.sendOk(
        "#eForgeMS Belt Progression#n\r\n\r\n" +
        "#i1132000# #bNovice Belt#k — #r15 RB#k\r\n" +
        "#i1132001# #bApprentice Belt#k — #r50 RB#k\r\n" +
        "#i1132002# #bJourneyman Belt#k — #r125 RB#k\r\n" +
        "#i1132003# #bMaster Belt#k — #r250 RB#k\r\n" +
        "#i1132004# #bSmith's Belt#k — #r500 RB#k\r\n" +
        "#i1132005# #bForge Belt#k — #r1,000 RB#k\r\n" +
        "#i1132006# #bPrestige Belt I#k — #r2,000 RB#k\r\n" +
        "#i1132007# #bPrestige Belt II#k — #r3,000 RB#k\r\n" +
        "#i1132008# #bPrestige Belt III#k — #r4,000 RB#k\r\n" +
        "#i1132009# #bPrestige Belt IV#k — #r5,000 RB#k\r\n\r\n" +
        "The #bNovice Belt#k is awarded at 15 total rebirths. " +
        "Every later tier requires and consumes the previous belt."
    );

    cm.dispose();
}

function showExchangeRules() {
    cm.sendOk(
        "#eForgeMS Belt Exchange Rules#n\r\n\r\n" +
        "#b•#k The Novice Belt is awarded at #r15 total rebirths#k.\r\n" +
        "#b•#k Every later tier requires the previous belt.\r\n" +
        "#b•#k The previous belt is consumed during the exchange.\r\n" +
        "#b•#k Belt tiers cannot be skipped.\r\n" +
        "#b•#k Rebirth requirements use your total rebirth count.\r\n" +
        "#b•#k Equipped belts are recognized by the system.\r\n" +
        "#b•#k A belt must be unequipped before it can be exchanged."
    );

    cm.dispose();
}

/*
 * Change this function only if your rebirth system uses a different getter.
 *
 * Possible alternatives:
 * cm.getPlayer().getRebirths()
 * cm.getPlayer().getTotalRebirths()
 */
function getTotalRebirths() {
    return Number(
        cm.getPlayer().getReborns()
    );
}

function formatNumber(number) {
    return String(number).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}