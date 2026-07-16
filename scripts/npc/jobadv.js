var status = -1;
var selectedJob = -1;
var selectedName = "";

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
        showAdvancement();
    } else if (status === 1) {
        handleSelection(selection);
    } else if (status === 2) {
        completeAdvancement();
    }
}

function showAdvancement() {
    var job = cm.getJobId();
    var level = cm.getLevel();

    if (job === 0) {
        cm.sendOk(
            "#eYou have not chosen your first profession yet.#n\r\n\r\n" +
            "Use #b@job#k to choose your first path."
        );
        cm.dispose();
        return;
    }

    // Second job advancement
    if (job === 100 || job === 200 || job === 300 ||
        job === 400 || job === 500) {

        if (level < 30) {
            denyLevel(30, "Second");
            return;
        }

        if (job === 100) {
            cm.sendSimple(
                "#eChoose your second Warrior path:#n\r\n\r\n" +
                "#L110##bFighter#k - Offensive sword and axe combat#l\r\n" +
                "#L120##bPage#k - Defensive combat and elemental charges#l\r\n" +
                "#L130##bSpearman#k - Spears, polearms, and party support#l"
            );
        } else if (job === 200) {
            cm.sendSimple(
                "#eChoose your second Magician path:#n\r\n\r\n" +
                "#L210##bFire/Poison Wizard#k - Fire and poison magic#l\r\n" +
                "#L220##bIce/Lightning Wizard#k - Ice and lightning magic#l\r\n" +
                "#L230##bCleric#k - Healing and holy magic#l"
            );
        } else if (job === 300) {
            cm.sendSimple(
                "#eChoose your second Bowman path:#n\r\n\r\n" +
                "#L310##bHunter#k - Bow mastery#l\r\n" +
                "#L320##bCrossbowman#k - Crossbow mastery#l"
            );
        } else if (job === 400) {
            cm.sendSimple(
                "#eChoose your second Thief path:#n\r\n\r\n" +
                "#L410##bAssassin#k - Claws and throwing stars#l\r\n" +
                "#L420##bBandit#k - Daggers and close combat#l"
            );
        } else {
            cm.sendSimple(
                "#eChoose your second Pirate path:#n\r\n\r\n" +
                "#L510##bBrawler#k - Knuckles and close combat#l\r\n" +
                "#L520##bGunslinger#k - Firearms and ranged combat#l"
            );
        }

        return;
    }

    // Third job advancement
    var thirdJob = getThirdJob(job);

    if (thirdJob !== -1) {
        if (level < 70) {
            denyLevel(70, "Third");
            return;
        }

        selectedJob = thirdJob;
        selectedName = getJobName(thirdJob);

        cm.sendYesNo(
            "#eYou are ready for your Third Job Advancement.#n\r\n\r\n" +
            "Forge your path as a #r" + selectedName + "#k?"
        );

        status = 1;
        return;
    }

    // Fourth job advancement
    var fourthJob = getFourthJob(job);

    if (fourthJob !== -1) {
        if (level < 120) {
            denyLevel(120, "Fourth");
            return;
        }

        selectedJob = fourthJob;
        selectedName = getJobName(fourthJob);

        cm.sendYesNo(
            "#eYou are ready for your Fourth Job Advancement.#n\r\n\r\n" +
            "Forge your final path as a #r" + selectedName + "#k?"
        );

        status = 1;
        return;
    }

    if (isFourthJob(job)) {
        cm.sendOk(
            "#eYou have already completed your final job advancement.#n"
        );
    } else {
        cm.sendOk(
            "#eThis command supports Explorer job advancements only.#n\r\n\r\n" +
            "Supported paths: Warrior, Magician, Bowman, Thief, and Pirate."
        );
    }

    cm.dispose();
}

function handleSelection(selection) {
    // Second-job menu selections use the destination job ID.
    if (selectedJob === -1) {
        if (!isValidSecondJob(cm.getJobId(), selection)) {
            cm.sendOk("That advancement is not valid for your current path.");
            cm.dispose();
            return;
        }

        selectedJob = selection;
        selectedName = getJobName(selection);

        cm.sendYesNo(
            "#eAdvance as a #r" + selectedName + "#k?#n\r\n\r\n" +
            "This decision determines your future advancement path."
        );

        return;
    }

    // Third/fourth advancement confirmation reaches this stage directly.
    completeAdvancement();
}

function completeAdvancement() {
    if (selectedJob === -1) {
        cm.dispose();
        return;
    }

    cm.changeJobById(selectedJob);

    cm.sendOk(
        "#eYour path has been reforged.#n\r\n\r\n" +
        "You are now a #r" + selectedName + "#k."
    );

    cm.dispose();
}

function denyLevel(requiredLevel, advancementName) {
    cm.sendOk(
        "#eYou are not ready for your " + advancementName +
        " Job Advancement.#n\r\n\r\n" +
        "Return when you reach #rLevel " + requiredLevel + "#k."
    );

    cm.dispose();
}

function isValidSecondJob(currentJob, nextJob) {
    if (currentJob === 100) {
        return nextJob === 110 || nextJob === 120 || nextJob === 130;
    }

    if (currentJob === 200) {
        return nextJob === 210 || nextJob === 220 || nextJob === 230;
    }

    if (currentJob === 300) {
        return nextJob === 310 || nextJob === 320;
    }

    if (currentJob === 400) {
        return nextJob === 410 || nextJob === 420;
    }

    if (currentJob === 500) {
        return nextJob === 510 || nextJob === 520;
    }

    return false;
}

function getThirdJob(job) {
    switch (job) {
        case 110: return 111;
        case 120: return 121;
        case 130: return 131;

        case 210: return 211;
        case 220: return 221;
        case 230: return 231;

        case 310: return 311;
        case 320: return 321;

        case 410: return 411;
        case 420: return 421;

        case 510: return 511;
        case 520: return 521;
    }

    return -1;
}

function getFourthJob(job) {
    switch (job) {
        case 111: return 112;
        case 121: return 122;
        case 131: return 132;

        case 211: return 212;
        case 221: return 222;
        case 231: return 232;

        case 311: return 312;
        case 321: return 322;

        case 411: return 412;
        case 421: return 422;

        case 511: return 512;
        case 521: return 522;
    }

    return -1;
}

function isFourthJob(job) {
    return job === 112 || job === 122 || job === 132 ||
        job === 212 || job === 222 || job === 232 ||
        job === 312 || job === 322 ||
        job === 412 || job === 422 ||
        job === 512 || job === 522;
}

function getJobName(job) {
    switch (job) {
        case 110: return "Fighter";
        case 120: return "Page";
        case 130: return "Spearman";

        case 210: return "Fire/Poison Wizard";
        case 220: return "Ice/Lightning Wizard";
        case 230: return "Cleric";

        case 310: return "Hunter";
        case 320: return "Crossbowman";

        case 410: return "Assassin";
        case 420: return "Bandit";

        case 510: return "Brawler";
        case 520: return "Gunslinger";

        case 111: return "Crusader";
        case 121: return "White Knight";
        case 131: return "Dragon Knight";

        case 211: return "Fire/Poison Mage";
        case 221: return "Ice/Lightning Mage";
        case 231: return "Priest";

        case 311: return "Ranger";
        case 321: return "Sniper";

        case 411: return "Hermit";
        case 421: return "Chief Bandit";

        case 511: return "Marauder";
        case 521: return "Outlaw";

        case 112: return "Hero";
        case 122: return "Paladin";
        case 132: return "Dark Knight";

        case 212: return "Fire/Poison Arch Mage";
        case 222: return "Ice/Lightning Arch Mage";
        case 232: return "Bishop";

        case 312: return "Bowmaster";
        case 322: return "Marksman";

        case 412: return "Night Lord";
        case 422: return "Shadower";

        case 512: return "Buccaneer";
        case 522: return "Corsair";
    }

    return "Unknown Job";
}