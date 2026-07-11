var status;
var jobId = 0;
var jobChangeChecker = 0;
const YamlConfig = Java.type('config.YamlConfig');
const GameConstants = Java.type('constants.game.GameConstants');
let rebornDataString = ""
let rebornData;
let rebornChoice = 0;
// let remainingSkillPoints = ""

function start() {
    status = -1;
    jobChangeChecker = 0;
    if ((cm.getJobId() == 0) || (cm.getJobId() == 100) || (cm.getJobId() == 110) || (cm.getJobId() == 111) || (cm.getJobId() == 120)
        || (cm.getJobId() == 121) || (cm.getJobId() == 130) || (cm.getJobId() == 131) || (cm.getJobId() == 200) || (cm.getJobId() == 210)
        || (cm.getJobId() == 211) || (cm.getJobId() == 220) || (cm.getJobId() == 221) || (cm.getJobId() == 230) || (cm.getJobId() == 231)
        || (cm.getJobId() == 300) || (cm.getJobId() == 310) || (cm.getJobId() == 311) || (cm.getJobId() == 320) || (cm.getJobId() == 321)
        || (cm.getJobId() == 400) || (cm.getJobId() == 410) || (cm.getJobId() == 411) || (cm.getJobId() == 420) || (cm.getJobId() == 421)
        || (cm.getJobId() == 510) || (cm.getJobId() == 511) || (cm.getJobId() == 520) || (cm.getJobId() == 521) || (cm.getJobId() == 500)
        || (cm.getJobId() == 1000) || (cm.getJobId() == 1100) || (cm.getJobId() == 1110) || (cm.getJobId() == 1200)
        || (cm.getJobId() == 1210) || (cm.getJobId() == 1300) || (cm.getJobId() == 1310)
        || (cm.getJobId() == 1400) || (cm.getJobId() == 1410) || (cm.getJobId() == 1500) || (cm.getJobId() == 1510)
        || (cm.getJobId() == 2000) || (cm.getJobId() == 2110) || (cm.getJobId() == 2111)) {
        cm.sendOk("Sorry, you must fully job advance before talking to me.");
        cm.dispose();
        return;
    }
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === 1) {
        status++;
    } else {
        cm.dispose();
        return;
    }
    if (status === 0) {
        cm.sendNext("Come to me when you want to be reborn again. You currently have a total of #r" + cm.getChar().getReborns() + " #krebirths.");
    } else if (status === 1) {
        cm.sendSimple("What do you want me to do today: \r\n \r\n #L0##bI want to be reborn!#l \r\n #L1##bNothing for now...#k#l");
    } else if (status === 2) {
        // Check if the player is at the maximum rebirth level
        if (cm.getChar().getReborns() >= 3) {
            cm.sendOk("You have reached the maximum rebirth level and cannot rebirth further.");
            cm.dispose();
            return;
        }

        // Continue with the existing code for rebirthing
        if (selection === 0) {
            let itemRequired = -1;
            let quantity = 0;
            switch (cm.getChar().getReborns()) {
                case 0:
                    itemRequired = 4033446;
                    quantity = 1;
                    break;
                case 1:
                    itemRequired = 4033442;
                    quantity = 1;
                    break;
                case 2:
                    itemRequired = 4033450;
                    quantity = 1;
                    break;
            }

            if (itemRequired != -1 && cm.hasItem(itemRequired, quantity) && cm.getChar().getLevel() == 200) {
                cm.sendSimple("I see... and which path would you like to take? \r\n\r\n #L0##bExplorer (Beginner)#l \r\n #L1##bCygnus Knight (Noblesse)#l \r\n #L2##bAran (Legend)#l");
            } else {
                cm.sendOk(`${cm.letters('NOPE')}\r\n\r\nI'm sorry but you lack\r\n#r${quantity}x#k #i${itemRequired}# #b#z${itemRequired}#${(quantity === 1 ? '' : 's')}#k\r\n or lack the level requirements to proceed the rebirth process.`);
                cm.dispose();
            }
        } else if (selection === 1) {
            cm.sendOk("See you soon!");
            cm.dispose();
        }
    } else if (status === 3) {
        
         if (jobChangeChecker === 0) {
            // 0 => beginner, 1000 => noblesse, 2000 => legend
            // makes this very easy :-)
            jobId = selection * 1000;

            var job = "";
            if (selection === 0) job = "Beginner";
            else if (selection === 1) job = "Noblesse";
            else if (selection === 2) job = "Legend";
            cm.sendYesNo("Are you sure you want to be reborn as a " + job + "?");
        }
        else if (jobChangeChecker === 1) { // change to previous rebirth class
            rebornDataString = cm.getChar().getAllRebornDataCerezeth(); // get rebirth data
            rebornData = JSON.parse(rebornDataString); // change it to json object
            if (selection === 0) {
                cm.changeRebirthJobCerezeth(rebornData[0].job)
                cm.sendOk("Ok changed job to " + GameConstants.getJobName(rebornData[0].job))
            }
            else if (selection === 1) {
                cm.changeRebirthJobCerezeth(rebornData[1].job)
                cm.sendOk("Ok changed job to " + GameConstants.getJobName(rebornData[1].job))
            }
            else if (selection === 2) {
                cm.changeRebirthJobCerezeth(rebornData[2].job)
                cm.sendOk("Ok changed job to " + GameConstants.getJobName(rebornData[2].job))
            }
            else if (selection === 3) {
                cm.changeRebirthJobCerezeth(rebornData[3].job)
                cm.sendOk("Ok changed job to " + GameConstants.getJobName(rebornData[3].job))
            }
            cm.dispose();
        }
        else {
            cm.sendOk("GG if you see this something went wrong LOOOOL");
            cm.dispose();
        }
    }
    else if (status === 4 && type === 1) {
        cm.getChar().executeRebornAsId(jobId);
        cm.resetStats();
        if (cm.getChar().getReborns() > 0) {
            if (cm.getChar().getReborns() == 1) {
                cm.getPlayer().setPlayerExpRatesCerezeth(YamlConfig.config.server.REBIRTH_FIRST_RATE);
            } 
            else if (cm.getChar().getReborns() == 2) {
                cm.getPlayer().setPlayerExpRatesCerezeth(YamlConfig.config.server.REBIRTH_SECOND_RATE);
            }
            else if (cm.getChar().getReborns() == 3) {
                cm.getPlayer().setPlayerExpRatesCerezeth(YamlConfig.config.server.REBIRTH_THIRD_RATE);
            }           
            else {
                cm.getPlayer().setPlayerExpRatesCerezeth(YamlConfig.config.world.exp_rate);
            }
        }
        cm.sendOk("You have now been reborn. That's a total of #r" + cm.getChar().getReborns() + "#k rebirths");
        cm.dispose();
    }
}