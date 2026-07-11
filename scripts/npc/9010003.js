/*
* Job Master
* ID: 9010003
*/

let depression;
let jobs = [];
let level = 0;
let job;

function start() {
    level = cm.getPlayer().getLevel();
    depression = 0;

    if (cm.getJobId() === 700 && level < 120) {
        cm.sendOk("You're now a Super Beginner.");
        cm.dispose();
        return;
    } else if (cm.getJobId() === 700 && level >= 120) {
        jobs.push(710); // Allow progression to job ID 710
    }

    // Check if the player is a Bard (job ID 710)
    if (cm.getJobId() === 710) {
        cm.sendOk("You are now a SynthMaster!");
        cm.dispose();
        return;
    }

    if (cm.getPlayer().getSkillLevel(1051) !== 0) {
        if (level < 120) {
            cm.sendOk("Come back when you're good, kiddo.");
        } else {
            cm.changeJobById(700);
            cm.teachSkill(1051, -1, 1, -1);   // Chain Attack 2
            cm.teachSkill(1052, -1, 1, -1);   // Chain Attack
        }
        cm.dispose();
        return;
    }

    // Check if the player is eligible for specific job advancements with item 4036541 at rebirth level 3 or higher
    const hasRequiredItem = cm.hasItem(4036541);
    const isEligibleForSpecificJobs = [112, 122, 132, 212, 222, 232, 312, 322, 412, 422, 512, 522, 2112, 1112, 1212, 1312, 1412, 1512, 700].includes(cm.getJobId());
    const isRebirthLevel3OrHigher = cm.getChar().getReborns() >= 3;

    if (isEligibleForSpecificJobs) {
        if (!isRebirthLevel3OrHigher) {
            cm.sendOk("You need to be rebirth level 3.");
            cm.dispose();
            return;
        }
        if (!hasRequiredItem) {
            cm.sendOk("You need #i4036541# to get the your ultimate power upgrade.");
            cm.dispose();
            return;
        }
    }


    if (jobs.length === 0) {
        fillJobs();
    }

    if (jobs.length === 0) {
        cm.sendOk("You are not eligible for a new job.");
        cm.dispose();
        return;
    }

    let str = "Choose your Job:#b";
    for (let i = 0; i < jobs.length; i++) {
        str += `\r\n#L${i}#${cm.getJobName(jobs[i])}#l`;
    }

    cm.sendSimple(str);
}

function action(m, t, s) {
    if (cm.isEndChat(m, t)) {
        cm.sendOk("The end, you're dead.");
        cm.dispose();
        return;
    }
    depression++;

    if (depression === 1) {
        job = jobs[s];
        cm.sendYesNo(`Are you sure you want job advance to a (#r${cm.getJobName(jobs[s])}#k)?`);
    } else if (depression === 2) {
        if (m === 0) { // no
            start();
        } else { // yes
            jobAdvance();
            cm.dispose();
        }
    }
}

function jobAdvance() {
    if (job !== 700) {
        cm.changeJobById(job);
    }
    giveRewards();
}

function giveRewards() {
    // everyone gets these depressions
    if (job === 0 || job === 1000 || job === 2000) return;
    cm.gainItem(2000005, 50); // Power Elixirs

    // job specific depressions
    switch (job) {
        case 100: // both 100 and 1100
        case 1100:
            cm.gainItem(1302077, 1);
            break;
        case 700:
            cm.gainItem(1302024, 1);
            cm.gainItem(1040014, 1);
            cm.gainItem(1002419, 1);
            cm.gainItem(1060004, 1);
            cm.gainItem(1072368, 1);
            cm.gainItem(1082245, 1);
            cm.gainItem(1102174, 1);
            cm.gainItem(1092003, 1);
            cm.teachSkill(1051, 1, 1, -1);   // Chain Attack 2
            cm.teachSkill(1052, 1, 1, -1);   // Chain Attack
            cm.sendOk("You're on the right path now.");
            break;
        case 2100:
            // Teach skills after advancing to job ID 2100
            cm.gainItem(1442000, 1);
            cm.teachSkill(21000000, 0, 10, -1);   // Combo Ability
            cm.teachSkill(21110002, 0, 20, -1);   // Full Swing
            cm.teachSkill(21100000, 0, 20, -1);   // Polearm Mastery
            cm.teachSkill(21100002, 0, 30, -1);   // Final Charge
            cm.teachSkill(21100004, 0, 20, -1);   // Combo Smash
            cm.teachSkill(21100005, 0, 20, -1);   // Combo Drain
            break;
        case 200:
        case 1200:
            // Job advancement from 1000 to 1200
            cm.gainItem(1372043, 1); // First item ID and quantity
            cm.gainItem(1382100, 1); // Second item ID and quantity
            break;
        case 232:
            // BAHAMUT SKILL AT LEVEL 30 FOR BISHOP
            cm.teachSkill(2321003, 0, 30, -1);   // BAHAMUT
            break;
        case 300:
        case 1300:
            cm.gainItem(1452051, 1); // First item ID and quantity
            cm.gainItem(1462092, 1); // Second item ID and quantity
            cm.gainItem(2060000, 999); // Third item ID and quantity
            cm.gainItem(2061000, 999); // Fourth item ID and quantity
            break;
        case 400:
        case 1400:
            cm.gainItem(1472061, 1); // Item ID and quantity
            cm.gainItem(1332063, 1); // Second ID and quantity
            cm.gainItem(2070015, 999); // Third item ID and quantity
            break;
        case 500:
        case 1500:
            cm.gainItem(1492014, 1); // Item ID and quantity
            cm.gainItem(1482063, 1); // Second ID and quantity
            cm.gainItem(2330000, 999); // Third item ID and quantity
            break;
        case 1411:
        case 411:
            cm.teachSkill(4111002, 30, 30, -1);   // Shadow Partner
            cm.teachSkill(14111000, 30, 30, -1);   // Shadow Partner
            break;
        case 113:
            cm.gainItem(4036541, -1);
            cm.teachSkill(1131074, 1, 1, -1);
            cm.teachSkill(1131075, 1, 1, -1);
            cm.teachSkill(1131105, 1, 1, -1);
            cm.teachSkill(1131109, 1, 1, -1);
            break;
        case 123:
            cm.gainItem(4036541, -1);
            cm.teachSkill(1231074, 1, 1, -1);
            cm.teachSkill(1231075, 1, 1, -1);
            cm.teachSkill(1231105, 1, 1, -1);
            cm.teachSkill(1231109, 1, 1, -1);
            break;
        case 133:
            cm.gainItem(4036541, -1);
            cm.teachSkill(1331074, 1, 1, -1);
            cm.teachSkill(1331075, 1, 1, -1);
            cm.teachSkill(1331105, 1, 1, -1);
            cm.teachSkill(1331109, 1, 1, -1);
            break;
        case 213:
            cm.gainItem(4036541, -1);
            cm.teachSkill(2131006, 1, 1, -1);
            cm.teachSkill(2131067, 1, 1, -1);
            cm.teachSkill(2131072, 1, 1, -1);
            cm.teachSkill(2131079, 1, 1, -1);
            break;
        case 223:
            cm.gainItem(4036541, -1);
            cm.teachSkill(2231006, 1, 1, -1);
            cm.teachSkill(2231067, 1, 1, -1);
            cm.teachSkill(2231072, 1, 1, -1);
            cm.teachSkill(2231079, 1, 1, -1);
            break;
        case 233:
            cm.gainItem(4036541, -1);
            cm.teachSkill(2331006, 1, 1, -1);
            cm.teachSkill(2331067, 1, 1, -1);
            cm.teachSkill(2331072, 1, 1, -1);
            cm.teachSkill(2331079, 1, 1, -1);
            break;
        case 313:
            cm.gainItem(4036541, -1);
            cm.teachSkill(3131003, 1, 1, -1);
            cm.teachSkill(3131007, 1, 1, -1);
            cm.teachSkill(3131012, 1, 1, -1);
            cm.teachSkill(3131056, 1, 1, -1);
            break;
        case 323:
            cm.gainItem(4036541, -1);
            cm.teachSkill(3231003, 1, 1, -1);
            cm.teachSkill(3231007, 1, 1, -1);
            cm.teachSkill(3231012, 1, 1, -1);
            cm.teachSkill(3231056, 1, 1, -1);
            break;
        case 413:
            cm.gainItem(4036541, -1);
            cm.teachSkill(4131031, 1, 1, -1);
            cm.teachSkill(4131042, 1, 1, -1);
            cm.teachSkill(4131073, 1, 1, -1);
            cm.teachSkill(4131076, 1, 1, -1);
            break;
        case 423:
            cm.gainItem(4036541, -1);
            cm.teachSkill(4231031, 1, 1, -1);
            cm.teachSkill(4231042, 1, 1, -1);
            cm.teachSkill(4231073, 1, 1, -1);
            cm.teachSkill(4231076, 1, 1, -1);
            break;
        case 513:
            cm.gainItem(4036541, -1);
            cm.teachSkill(5131033, 1, 1, -1);
            cm.teachSkill(5131044, 1, 1, -1);
            cm.teachSkill(5131074, 1, 1, -1);
            cm.teachSkill(5131075, 1, 1, -1);
            break;
        case 523:
            cm.gainItem(4036541, -1);
            cm.teachSkill(5231033, 1, 1, -1);
            cm.teachSkill(5231044, 1, 1, -1);
            cm.teachSkill(5231074, 1, 1, -1);
            cm.teachSkill(5231075, 1, 1, -1);
            break;
        case 2113:
            cm.gainItem(4036541, -1);
            cm.teachSkill(21131074, 1, 1, -1);
            cm.teachSkill(21131075, 1, 1, -1);
            cm.teachSkill(21131105, 1, 1, -1);
            cm.teachSkill(21131109, 1, 1, -1);
            break;
        case 1113:
            cm.gainItem(4036541, -1);
            cm.teachSkill(11131074, 1, 1, -1);
            cm.teachSkill(11131075, 1, 1, -1);
            cm.teachSkill(11131105, 1, 1, -1);
            cm.teachSkill(11131109, 1, 1, -1);
            break;
        case 1213:
            cm.gainItem(4036541, -1);
            cm.teachSkill(12131006, 1, 1, -1);
            cm.teachSkill(12131067, 1, 1, -1);
            cm.teachSkill(12131072, 1, 1, -1);
            cm.teachSkill(12131079, 1, 1, -1);
            break;
        case 1313:
            cm.gainItem(4036541, -1);
            cm.teachSkill(13131003, 1, 1, -1);
            cm.teachSkill(13131007, 1, 1, -1);
            cm.teachSkill(13131012, 1, 1, -1);
            cm.teachSkill(13131056, 1, 1, -1);
            break;
        case 1413:
            cm.gainItem(4036541, -1);
            cm.teachSkill(14131031, 1, 1, -1);
            cm.teachSkill(14131042, 1, 1, -1);
            cm.teachSkill(14131073, 1, 1, -1);
            cm.teachSkill(14131076, 1, 1, -1);
            break;
        case 1513:
            cm.gainItem(4036541, -1);
            cm.teachSkill(15131033, 1, 1, -1);
            cm.teachSkill(15131044, 1, 1, -1);
            cm.teachSkill(15131074, 1, 1, -1);
            cm.teachSkill(15131075, 1, 1, -1);
            break;
        case 710:
            cm.gainItem(4036541, -1);
            cm.teachSkill(7101101, 1, 1, -1);
            cm.teachSkill(7101102, 1, 1, -1);
            cm.teachSkill(7101103, 1, 1, -1);
            cm.teachSkill(7101104, 1, 1, -1);
            cm.teachSkill(7101105, 1, 1, -1);
            cm.teachSkill(7101106, 1, 1, -1);
            cm.teachSkill(7101107, 1, 1, -1);
            cm.teachSkill(7101108, 1, 1, -1);
            break;

    }
}

function fillJobs() {
    const currentJobId = cm.getJobId();
    const hasRequiredItem = cm.hasItem(4036541);

    if (level === 1) {
        jobs.push(...[0, 1000, 2000]);
        return;
    }

    // Allow 4th job only if level is 120 is job id nod 10 is equal to 1
    if (level >= 120 && (currentJobId % 10) === 1) {
        jobs.push(currentJobId + 1);
        return;
    }

    // 3rd job
    if (level >= 70 && (currentJobId % 100) % 10 === 0 && !cm.getPlayer().isBeginnerJob() && currentJobId % 100 !== 0) {
        jobs.push(currentJobId + 1);
        return;
    }

    // 2nd job
    if (!cm.getPlayer().isBeginnerJob() && level >= 30 && currentJobId % 100 === 0) {
        switch (currentJobId) {
            case 100:
                jobs.push(...[110, 120, 130]);
                break;
            case 200:
                jobs.push(...[210, 220, 230]);
                break;
            case 300:
                jobs.push(...[310, 320]);
                break;
            case 400:
                jobs.push(...[410, 420]);
                break;
            case 500:
                jobs.push(...[510, 520]);
                break;
            case 1100:
                jobs.push(1110);
                break;
            case 1200:
                jobs.push(1210);
                break;
            case 1300:
                jobs.push(1310);
                break;
            case 1400:
                jobs.push(1410);
                break;
            case 1500:
                jobs.push(1510);
                break;
            case 2100:
                jobs.push(2110);
                break;
        }
        return;
    }

    // first job beginner
    if (currentJobId === 0 && level >= 8) {
        jobs.push(200);
        if (level >= 10) {
            jobs.push(...[100, 300, 400, 500, 700]);
        }
        return;
    }

    // first job cygnus
    if (currentJobId === 1000 && level >= 10) {
        jobs.push(...[1100, 1200, 1300, 1400, 1500]);
        return;
    }

    // first job aran
    if (currentJobId === 2000 && level >= 10) {
        jobs.push(2100);
        return;
    }

    if (hasRequiredItem) {
        switch (currentJobId) {
            case 112:
                jobs.push(113);
                break;
            case 122:
                jobs.push(123);
                break;
            case 132:
                jobs.push(133);
                break;
            case 212:
                jobs.push(213);
                break;
            case 222:
                jobs.push(223);
                break;
            case 232:
                jobs.push(233);
                break;
            case 312:
                jobs.push(313);
                break;
            case 322:
                jobs.push(323);
                break;
            case 412:
                jobs.push(413);
                break;
            case 422:
                jobs.push(423);
                break;
            case 512:
                jobs.push(513);
                break;
            case 522:
                jobs.push(523);
                break;
            case 2112:
                jobs.push(2113);
                break;
            case 1112:
                jobs.push(1113);
                break;
            case 1212:
                jobs.push(1213);
                break;
            case 1312:
                jobs.push(1313);
                break;
            case 1412:
                jobs.push(1413);
                break;
            case 1512:
                jobs.push(1513);
                break;
            case 700:
                jobs.push(710);
                break;
        }
    }
}