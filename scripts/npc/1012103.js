//** Npc 1012103 will become the ForgeMS boss spawner npc -
// this npc will have a list of spawnable bosses and be ForgeMs custom skin//**

/*
 * Hrothgar — ForgeMS Beastmaster
 * Temporary NPC ID: 1012103
 *
 * Purpose:
 * - NPC-only FM boss spawner.
 * - Works wherever NPC 1012103 is physically placed.
 * - No level requirement.
 * - No party requirement.
 * - No quest requirement.
 * - No cooldown.
 * - No limit on living monsters.
 * - Players may spawn duplicate bosses.
 * - Selected monsters spawn at Hrothgar's NPC position.
 *
 * Intentionally excluded:
 * - Zakum
 * - Horntail
 * - Papulatus
 * - Pink Bean
 * - Anniversary Cake bosses
 * - Holiday/event-only bosses
 * - Competency/test versions
 * - Advanced/event variants
 */

var status = -1;
var selectedCategory = -1;
var selectedBoss = -1;

/*
 * Job Instructor Combat Clones
 */
var trainingMasters = [
    { name: "Warrior Master", mob: 9001000 },
    { name: "Magician Master", mob: 9001001 },
    { name: "Bowman Master", mob: 9001002 },
    { name: "Thief Master", mob: 9001003 },
    { name: "Pirate Master", mob: 9001008 }
];

/*
 * Victoria Island and Early Regional Bosses
 */
var victoriaBosses = [
    { name: "Mano", mob: 2220000 },
    { name: "Stumpy", mob: 3220000 },
    { name: "Deo", mob: 3220001 },
    { name: "King Slime", mob: 9300003 },
    { name: "Faust", mob: 5220002 },
    { name: "King Clang", mob: 5220000 },
    { name: "Mushmom", mob: 6130101 },
    { name: "Zombie Mushmom", mob: 6300005 },
    { name: "Blue Mushmom", mob: 8220007 },
    { name: "Dyle", mob: 6220000 },
    { name: "Jr. Balrog", mob: 8130100 },
    { name: "Crimson Balrog", mob: 8150000 }
];

/*
 * Ossyria, Aqua Road, Ludibrium and Regional Bosses
 */
var ossyriaBosses = [
    { name: "Timer", mob: 5220003 },
    { name: "Eliza", mob: 8220000 },
    { name: "Snowman", mob: 8220001 },
    { name: "Seruf", mob: 4220000 },
    { name: "Pianus — Left", mob: 8510000 },
    { name: "Pianus — Right", mob: 8520000 },
    { name: "Zeno", mob: 6220001 },
    { name: "Chimera", mob: 8220002 }
];

/*
 * Korean Folk Town, Mu Lung, Herb Town and Ariant Bosses
 */
var easternBosses = [
    { name: "Giant Centipede", mob: 5220004 },
    { name: "Tae Roon", mob: 7220000 },
    { name: "Nine-Tailed Fox", mob: 7220001 },
    { name: "King Sage Cat", mob: 7220002 },
    { name: "Lord Pirate", mob: 9300119 }
];

/*
 * Party Quest and Dungeon Bosses
 */
var partyQuestBosses = [
    { name: "Alishar", mob: 9300012 },
    { name: "Papa Pixie", mob: 9300039 },
    { name: "Ergoth", mob: 9300028 },
    { name: "Frankenroid", mob: 9300139 },
    { name: "Angry Frankenroid", mob: 9300140 },
    { name: "Poison Golem", mob: 9300176 },
    { name: "Charged Poison Golem", mob: 9300181 },
    { name: "Super-Charged Poison Golem", mob: 9300182 }
];

/*
 * Leafre and Temple of Time Bosses
 */
var leafreBosses = [
    { name: "Manon", mob: 8180000 },
    { name: "Griffey", mob: 8180001 },
    { name: "Leviathan", mob: 8220003 },
    { name: "Dodo", mob: 8220004 },
    { name: "Lilynouch", mob: 8220005 },
    { name: "Lyka", mob: 8220006 }
];

/*
 * Zipangu Bosses
 */
var zipanguBosses = [
    { name: "Black Crow", mob: 9400014 },
    { name: "Anego", mob: 9400121 },
    { name: "The Boss", mob: 9400300 }
];

/*
 * Masteria Bosses
 */
var masteriaBosses = [
    { name: "Headless Horseman", mob: 9400549 },
    { name: "Geist Balrog", mob: 9400572 },
    { name: "Bigfoot", mob: 9400575 },
    { name: "MV", mob: 9400589 },
    { name: "Margana", mob: 9400590 },
    { name: "Red Nirg", mob: 9400591 },
    { name: "Rellik", mob: 9400592 },
    { name: "Hsalf", mob: 9400593 },
    { name: "Master Guardian", mob: 9400594 }
];

/*
 * Singapore and Malaysia Bosses
 */
var singaporeMalaysiaBosses = [
    { name: "Captain Latanica", mob: 9420513 },
    { name: "Targa", mob: 9420541 },
    { name: "Angry Targa", mob: 9420543 },
    { name: "Furious Targa", mob: 9420544 },
    { name: "Scarlion", mob: 9420546 },
    { name: "Angry Scarlion", mob: 9420548 },
    { name: "Furious Scarlion", mob: 9420549 }
];

/*
 * Standard Balrog Encounter Forms
 *
 * These are not Crimson Balrog and are separate from Zakum/HT/Pap/PB.
 */
var balrogBosses = [
    { name: "Balrog — Form 1", mob: 8830000 },
    { name: "Balrog — Form 2", mob: 8830001 },
    { name: "Balrog — Form 3", mob: 8830002 }
];

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
        return;
    }

    /*
     * Cancel / Back handling.
     */
    if (mode === 0) {
        if (status <= 0) {
            cm.dispose();
            return;
        }

        /*
         * Return from a category to the main menu.
         */
        if (status === 1) {
            status = -1;
            selectedCategory = -1;
            selectedBoss = -1;
            action(1, 0, 0);
            return;
        }

        /*
         * Return from confirmation to the selected category.
         */
        if (status === 2) {
            status = 0;
            selectedBoss = -1;
            showBossMenu(selectedCategory);
            return;
        }

        cm.dispose();
        return;
    }

    status++;

    /*
     * Main menu.
     */
    if (status === 0) {
        showMainMenu();
        return;
    }

    /*
     * Category selected.
     */
    if (status === 1) {
        selectedCategory = selection;
        selectedBoss = -1;

        showBossMenu(selectedCategory);
        return;
    }

    /*
     * Boss selected.
     */
    if (status === 2) {
        selectedBoss = selection;

        var boss = getSelectedBoss();

        if (boss === null) {
            cm.sendOk(
                "That creature is not recorded in my bestiary."
            );
            cm.dispose();
            return;
        }

        cm.sendYesNo(
            "#e" + boss.name + "#n\r\n\r\n" +
            "I will release this creature here at my position.\r\n\r\n" +
            "There is no limit to how many beasts may occupy these grounds.\r\n\r\n" +
            "Shall I call it forth?"
        );

        return;
    }

    /*
     * Confirmed spawn.
     */
    if (status === 3) {
        var selected = getSelectedBoss();

        if (selected === null) {
            cm.sendOk(
                "The trail has gone cold. Speak with me again."
            );
            cm.dispose();
            return;
        }

        /*
         * Spawns one selected mob at Hrothgar's NPC position.
         *
         * Players may repeat the interaction to spawn unlimited
         * copies of the same or different bosses.
         */
        cm.spawnMonster(selected.mob, 1);

        cm.sendOk(
            "#e" + selected.name + " has been released!#n\r\n\r\n" +
            "Raise your weapon. The hunt begins."
        );

        cm.dispose();
    }
}

function showMainMenu() {
    selectedCategory = -1;
    selectedBoss = -1;

    cm.sendSimple(
        "#eEvery beast has a name. Every hunt leaves a scar.#n\r\n\r\n" +
        "The Coalition has tracked monsters from every corner of Maple World.\r\n" +
        "Name your quarry, and I will loose it upon these grounds.\r\n\r\n" +

        "#L0##bTraining Masters#k#l\r\n" +
        "Combat clones of the five job instructors.\r\n\r\n" +

        "#L1##bVictoria Island Bosses#k#l\r\n" +
        "Mushmoms, Balrogs, and regional guardians.\r\n\r\n" +

        "#L2##bOssyria and Aqua Bosses#k#l\r\n" +
        "Clocktower, underwater, and mountain threats.\r\n\r\n" +

        "#L3##bEastern Regional Bosses#k#l\r\n" +
        "Korean Folk Town, Mu Lung, Herb Town, and Ariant.\r\n\r\n" +

        "#L4##dParty Quest and Dungeon Bosses#k#l\r\n" +
        "Guardians once bound to party expeditions.\r\n\r\n" +

        "#L5##rLeafre and Temple Bosses#k#l\r\n" +
        "Dragons and guardians of forgotten time.\r\n\r\n" +

        "#L6##dZipangu Bosses#k#l\r\n" +
        "Black Crow, Anego, and The Boss.\r\n\r\n" +

        "#L7##rMasteria Bosses#k#l\r\n" +
        "The greatest threats of Phantom Forest and Crimsonwood.\r\n\r\n" +

        "#L8##rSingapore and Malaysia Bosses#k#l\r\n" +
        "Latanica, Targa, and Scarlion.\r\n\r\n" +

        "#L9##rBalrog Expedition Forms#k#l\r\n" +
        "The standard Balrog encounter forms."
    );
}

function showBossMenu(category) {
    var bosses = getCategory(category);

    if (bosses === null) {
        cm.sendOk(
            "That section of the bestiary is unavailable."
        );
        cm.dispose();
        return;
    }

    var menu =
        "#e" + getCategoryTitle(category) + "#n\r\n\r\n" +
        "Choose the creature you wish to release.\r\n\r\n";

    for (var i = 0; i < bosses.length; i++) {
        menu +=
            "#L" + i + "##b" +
            bosses[i].name +
            "#k#l\r\n";
    }

    cm.sendSimple(menu);
}

function getCategory(category) {
    switch (category) {
        case 0:
            return trainingMasters;

        case 1:
            return victoriaBosses;

        case 2:
            return ossyriaBosses;

        case 3:
            return easternBosses;

        case 4:
            return partyQuestBosses;

        case 5:
            return leafreBosses;

        case 6:
            return zipanguBosses;

        case 7:
            return masteriaBosses;

        case 8:
            return singaporeMalaysiaBosses;

        case 9:
            return balrogBosses;

        default:
            return null;
    }
}

function getCategoryTitle(category) {
    switch (category) {
        case 0:
            return "Training Masters";

        case 1:
            return "Victoria Island Bosses";

        case 2:
            return "Ossyria and Aqua Bosses";

        case 3:
            return "Eastern Regional Bosses";

        case 4:
            return "Party Quest and Dungeon Bosses";

        case 5:
            return "Leafre and Temple Bosses";

        case 6:
            return "Zipangu Bosses";

        case 7:
            return "Masteria Bosses";

        case 8:
            return "Singapore and Malaysia Bosses";

        case 9:
            return "Balrog Expedition Forms";

        default:
            return "Unknown Beasts";
    }
}

function getSelectedBoss() {
    var bosses = getCategory(selectedCategory);

    if (bosses === null) {
        return null;
    }

    if (selectedBoss < 0 || selectedBoss >= bosses.length) {
        return null;
    }

    return bosses[selectedBoss];
}