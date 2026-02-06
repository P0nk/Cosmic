/*
    Charity Box - 9110100
    Function: Donate 100 mesos for a blessing
*/

var status = -1;
var cost = 100;

function start() {
    status = -1;
    if (checkAndRestoreHerosWill()) return;
    action(1, 0, 0);
}

function checkAndRestoreHerosWill() {
    var jobId = cm.getPlayer().getJob().getId();
    var skillId = 0;

    if (jobId == 112) skillId = 1121011; // Hero
    else if (jobId == 122) skillId = 1221011; // Paladin
    else if (jobId == 132) skillId = 1321011; // Dark Knight
    else if (jobId == 212) skillId = 2121008; // F/P Archmage
    else if (jobId == 222) skillId = 2221008; // I/L Archmage
    else if (jobId == 232) skillId = 2321009; // Bishop
    else if (jobId == 312) skillId = 3121009; // Bowmaster
    else if (jobId == 322) skillId = 3221008; // Marksman
    else if (jobId == 412) skillId = 4121009; // Night Lord
    else if (jobId == 422) skillId = 4221008; // Shadower
    else if (jobId == 512) skillId = 5121009; // Buccaneer
    else if (jobId == 522) skillId = 5221010; // Corsair
    else if (jobId == 434) skillId = 4341008; // Dual Blade

    if (skillId != 0) {
        if (cm.getSkillLevel(skillId) <= 0) {
            cm.teachSkill(skillId, 1, 5, -1); // Level 1, Master Level 5, No Expiration
            cm.sendOk("I noticed you were missing #bHero's Will#k despite your tough journey. I have restored it for you.");
            cm.dispose();
            return true;
        }
    }
    return false;
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) {
        status++;
    } else {
        status--;
    }

    if (status == 0) {
        cm.sendYesNo("Would you like to donate #b" + cost + " mesos#k to the shrine for good luck?");
    } else if (status == 1) {
        if (cm.getMeso() >= cost) {
            cm.gainMeso(-cost);
            // Show a random effect or just a message
            cm.sendOk("May the spirits of Mushroom Shrine bless you.");
        } else {
            cm.sendOk("You don't have enough mesos to donate.");
        }
        cm.dispose();
    }
}
