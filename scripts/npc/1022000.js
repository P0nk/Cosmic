/* Dances with Balrog
    Warrior Job Advancement
    Victoria Road : Warriors' Sanctuary (102000003)
*/

var status = -1;
var actionx = {"1stJob": false, "2ndJob": false, "3thJobI": false, "3thJobC": false};
var job = 110;

var spawnPnpc = false;
var spawnPnpcFee = 7000000;
var jobType = 1; // Warrior

function start() {
    var GameConstants = Java.type('constants.game.GameConstants');
    if (parseInt(cm.getJobId() / 100) == jobType && cm.canSpawnPlayerNpc(GameConstants.getHallOfFameMapid(cm.getJob()))) {
        spawnPnpc = true;
        cm.sendYesNo("You have walked a long way... What do you say about having a NPC on the Hall of Fame? Fee: " + cm.numberWithCommas(spawnPnpcFee) + " mesos.");
    } else {
        if (cm.getJobId() == 0) {
            actionx["1stJob"] = true;
            cm.sendNext("Want to be a #rWarrior#k? The requirements are Level 10 and 35 STR.");
        } else if (cm.getLevel() >= 30 && cm.getJobId() == 100) {
            actionx["2ndJob"] = true;
            if (cm.haveItem(4031012)) {
                cm.sendNext("I see you have done well. I will allow you to take the next step on your long road.");
            } else if (cm.haveItem(4031008)) {
                cm.sendOk("Go and see the #b#p102020300##k in the West Rocky Mountain IV.");
                cm.dispose();
            } else {
                cm.sendYesNo("You look strong. Do you wish to take the test for the 2nd Job Advancement?");
            }
        } else if (actionx["3thJobI"] || (cm.getPlayer().gotPartyQuestItem("JB3") && cm.getLevel() >= 70 && cm.getJobId() % 10 == 0 && parseInt(cm.getJobId() / 100) == 1 && !cm.getPlayer().gotPartyQuestItem("JBP"))) {
            actionx["3thJobI"] = true;
            cm.sendNext("I see that you are interested in the third job advancement. I will have to test your strength against a clone of myself.");
        } else if (cm.getPlayer().gotPartyQuestItem("JBP") && !cm.haveItem(4031059)) {
            cm.sendNext("Please, bring me the #b#t4031059##k.");
            cm.dispose();
        } else if (cm.haveItem(4031059) && cm.getPlayer().gotPartyQuestItem("JBP")) {
            actionx["3thJobC"] = true;
            cm.sendNext("Nice work. You have defeated my clone. Give this necklace to #b#p2020008##k in Ossyria.");
        } else {
            cm.sendOk("You have chosen wisely.");
            cm.dispose();
        }
    }
}

function action(mode, type, selection) {
    status++;
    if (mode == -1 && selection == -1) {
        cm.dispose();
        return;
    } else if (mode == 0 && type != 1) {
        status -= 2;
    }

    if (status == -1) {
        start();
        return;
    } else {
        if (spawnPnpc) {
            if (mode > 0) {
                if (cm.getMeso() < spawnPnpcFee) {
                    cm.sendOk("Sorry, you don't have enough mesos.");
                } else {
                    var PlayerNPC = Java.type('server.life.PlayerNPC');
                    var GameConstants = Java.type('constants.game.GameConstants');
                    if (PlayerNPC.spawnPlayerNPC(GameConstants.getHallOfFameMapid(cm.getJob()), cm.getPlayer())) {
                        cm.sendOk("There you go!");
                        cm.gainMeso(-spawnPnpcFee);
                    } else {
                        cm.sendOk("The Hall of Fame is full.");
                    }
                }
            }
            cm.dispose();
            return;
        } else {
            if (mode != 1 || status == 7 || (actionx["1stJob"] && status == 4) || (cm.haveItem(4031008) && status == 2) || (actionx["3thJobI"] && status == 1)) {
                if (mode == 0 && status == 2 && type == 1) {
                    cm.sendOk("You know there is no other choice...");
                }
                if (!(mode == 0 && type != 1)) {
                    cm.dispose();
                    return;
                }
            }
        }
    }

    if (actionx["1stJob"]) {
        if (status == 0) {
            if (cm.getLevel() >= 10 && cm.canGetFirstJob(jobType)) {
                cm.sendYesNo("Do you want to become a Warrior?");
            } else {
                cm.sendOk("Train a bit more.");
                cm.dispose();
            }
        } else if (status == 1) {
            if (cm.canHold(1402001)) {
                cm.changeJobById(100);
                cm.gainItem(1402001, 1); // Sword
                cm.resetStats();
                cm.sendNext("You are now a Warrior. Use your power wisely.");
            } else {
                cm.sendNext("Make some room in your inventory.");
                cm.dispose();
            }
        } else if (status == 2) {
             cm.sendNextPrev("I have given you some SP to start with.");
        } else {
             cm.dispose();
        }

    } else if (actionx["2ndJob"]) {
        if (status == 0) {
            if (cm.haveItem(4031012)) {
                cm.sendSimple("Choose your path:#b\r\n#L0#Fighter\r\n#L1#Page\r\n#L2#Spearman");
            } else {
                cm.sendNext("Take this letter to #b#p102020300##k.");
                if (!cm.isQuestStarted(100003)) cm.startQuest(100003);
            }
        } else if (status == 1) {
            if (!cm.haveItem(4031012)) {
                if (cm.canHold(4031008)) {
                    if (!cm.haveItem(4031008)) cm.gainItem(4031008, 1);
                    cm.dispose();
                } else {
                    cm.sendNext("Make space in your inventory.");
                    cm.dispose();
                }
            } else {
                if (selection == 0) cm.sendNext("Fighters have high power.");
                else if (selection == 1) cm.sendNext("Pages use elemental magic.");
                else cm.sendNext("Spearmen use range and power.");
                status -= 2; // Loop explanation
            }
        } else if (status == 2) {
            // [FIX] Reset Job Calculation
            job = 110 + (selection * 10);
            cm.sendYesNo("Are you sure you want to become a " + ((job == 110) ? "Fighter" : (job == 120) ? "Page" : "Spearman") + "?");
        } else if (status == 3) {
            if (cm.haveItem(4031012)) cm.gainItem(4031012, -1);
            if (cm.haveItem(4031008)) cm.gainItem(4031008, -1);

            cm.completeQuest(100005);
            cm.changeJobById(job);
            cm.sendNext("Congratulations on your advancement!");
        } else if (status == 4) {
            cm.dispose();
        }

    } else if (actionx["3thJobI"]) {
        if (status == 0) {
            if (cm.getPlayer().gotPartyQuestItem("JB3")) {
                cm.getPlayer().removePartyQuestItem("JB3");
                cm.getPlayer().setPartyQuestItemObtained("JBP");
            }
            cm.sendNextPrev("Good luck fighting my clone.");
        }
    } else if (actionx["3thJobC"]) {
        cm.getPlayer().removePartyQuestItem("JBP");
        cm.gainItem(4031059, -1);
        cm.gainItem(4031057, 1);
        cm.dispose();
    }
}