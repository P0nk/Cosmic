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
        if (cm.getJobId() !== 0) {
            cm.sendOk(
                "#eYou have already chosen your path.#n\r\n\r\n" +
                "Your current job is #b" + cm.getJobName(cm.getJobId()) + "#k."
            );
            cm.dispose();
            return;
        }

        if (cm.getLevel() < 10) {
            cm.sendOk(
                "#eYou are not ready to choose a new path yet.#n\r\n\r\n" +
                "Return when you have reached at least #rLevel 10#k."
            );
            cm.dispose();
            return;
        }

        cm.sendSimple(
            "#eChoose the path you wish to forge:#n\r\n\r\n" +
            "#L100##bWarrior#k - A raider’s path of strength and close combat#l\r\n" +
            "#L200##bMagician#k - A rune-wielder’s path of magic and elemental power#l\r\n" +
            "#L300##bBowman#k - A ranger’s path of precision and ranged attacks#l\r\n" +
            "#L400##bThief#k - A scout’s path of speed, stealth, and critical strikes#l\r\n" +
            "#L500##bPirate#k - A corsair’s path of brawling and firearms#l"
        );
    } else if (status === 1) {
        switch (selection) {
            case 100:
                selectedJob = 100;
                selectedName = "Warrior";
                break;
            case 200:
                selectedJob = 200;
                selectedName = "Magician";
                break;
            case 300:
                selectedJob = 300;
                selectedName = "Bowman";
                break;
            case 400:
                selectedJob = 400;
                selectedName = "Thief";
                break;
            case 500:
                selectedJob = 500;
                selectedName = "Pirate";
                break;
            default:
                cm.sendOk("That profession is not currently available.");
                cm.dispose();
                return;
        }

        cm.sendYesNo(
            "#eBecome a #r" + selectedName + "#k?#n\r\n\r\n" +
            "This choice determines your future advancement path."
        );
    } else if (status === 2) {
        if (cm.getJobId() !== 0) {
            cm.sendOk("Your job has already changed.");
            cm.dispose();
            return;
        }

        cm.changeJobById(selectedJob);

        cm.sendOk(
            "#eYour path has been forged.#n\r\n\r\n" +
            "You are now a #r" + selectedName + "#k."
        );

        cm.dispose();
    }
}