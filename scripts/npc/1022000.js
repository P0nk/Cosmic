/* Dances with Balrog
    Warrior Job Advancement
    Victoria Road : Warriors' Sanctuary (102000003)
*/

var status = -1;
// Track which flow we are in
var actionx = {"1stJob": false, "2ndJob": false, "3thJobI": false, "3thJobC": false};
var job = 110;
var sel = -1;

var spawnPnpc = false;
var spawnPnpcFee = 7000000;
var jobType = 1; // Warrior

function start() {
    var GameConstants = Java.type('constants.game.GameConstants');

    // Hall of Fame Logic
    if (parseInt(cm.getJobId() / 100) == jobType && cm.canSpawnPlayerNpc(GameConstants.getHallOfFameMapid(cm.getJob()))) {
        spawnPnpc = true;
        var sendStr = "Hau! You have walked a long, hard path to reach this level of power. The spirits of our ancestors smile upon you. What do you say about having #ra NPC on the Hall of Fame holding your image#k? Do you wish to be immortalized?";
        if (spawnPnpcFee > 0) {
            sendStr += " I can do it for the fee of #b " + cm.numberWithCommas(spawnPnpcFee) + " mesos.#k";
        }
        cm.sendYesNo(sendStr);
    } else {
        // 1st Job Advancement (Beginner -> Warrior)
        if (cm.getJobId() == 0) {
            actionx["1stJob"] = true;
            cm.sendNext("Hau! You wish to become a #rWarrior#k? Strength is the law of Perion. #bYour level must be at least 10, with at least 35 STR#k. Let me look at your muscles.");

        // 2nd Job Advancement (Warrior -> Fighter/Page/Spearman)
        } else if (cm.getLevel() >= 30 && cm.getJobId() == 100) {
            actionx["2ndJob"] = true;
            if (cm.haveItem(4031012)) { // Proof of Hero
                cm.sendNext("Hau! I see the fire in your eyes. You have returned with the Proof of a Hero. I admit, you are a true warrior of Perion. I will help you unlock your hidden potential. But first... you must choose your destiny.");
            } else if (cm.haveItem(4031008)) { // Warrior Letter
                cm.sendOk("Do not delay. Go and see the #rWarrior Job Instructor#k in the #bWest Rocky Mountain IV#k. The spirits await.");
                cm.dispose();
            } else {
                cm.sendYesNo("Hau! You have grown much since I first gave you a sword. But are you strong enough for the next step? The spirits demand a test. Do you have the courage to face it?");
            }

        // 3rd Job Advancement
        } else if (actionx["3thJobI"] || (cm.getPlayer().gotPartyQuestItem("JB3") && cm.getLevel() >= 70 && cm.getJobId() % 10 == 0 && parseInt(cm.getJobId() / 100) == 1 && !cm.getPlayer().gotPartyQuestItem("JBP"))) {
            actionx["3thJobI"] = true;
            cm.sendNext("The spirits have whispered your name. I see you are interested in the third job advancement. To achieve this, I must test your strength against a clone of myself. Defeat him and bring back the #b#t4031059##k.");
        } else if (cm.getPlayer().gotPartyQuestItem("JBP") && !cm.haveItem(4031059)) {
            cm.sendNext("Do not return until you have the #b#t4031059##k. Strength comes to those who persevere.");
            cm.dispose();
        } else if (cm.haveItem(4031059) && cm.getPlayer().gotPartyQuestItem("JBP")) {
            actionx["3thJobC"] = true;
            cm.sendNext("Hau! Magnificent! You have defeated my clone and returned safely. You have proven your worth. Take this necklace to #b#p2020008##k in Ossyria.");
        } else {
            cm.sendOk("You have chosen wisely. Walk with the spirits.");
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
                    cm.sendOk("You do not have enough mesos. Honor has a price.");
                    cm.dispose();
                    return;
                }
                var PlayerNPC = Java.type('server.life.PlayerNPC');
                var GameConstants = Java.type('constants.game.GameConstants');
                if (PlayerNPC.spawnPlayerNPC(GameConstants.getHallOfFameMapid(cm.getJob()), cm.getPlayer())) {
                    cm.sendOk("It is done. Your spirit shall remain here forever.");
                    cm.gainMeso(-spawnPnpcFee);
                } else {
                    cm.sendOk("The Hall of Fame is full. The spirits are crowded.");
                }
            }
            cm.dispose();
            return;
        } else {
            // General Exit Logic
            if (mode != 1 || status == 7 && type != 1 || (actionx["1stJob"] && status == 4) || (cm.haveItem(4031008) && status == 2) || (actionx["3thJobI"] && status == 1)) {
                if (mode == 0 && status == 2 && type == 1) {
                    cm.sendOk("Cowardice is not the way of the Warrior...");
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
                cm.sendNextPrev("This is an important choice. Once you pick up the sword, you cannot put it down. Are you ready?");
            } else {
                cm.sendOk("You are still weak. Train until you meet the requirements, young one.");
                cm.dispose();
            }
        } else if (status == 1) {
            if (cm.canHold(1402001)) {
                if (cm.getJobId() == 0) {
                    cm.changeJobById(100);
                    cm.gainItem(1402001, 1); // Sword
                    cm.resetStats();
                }
                cm.sendNext("Hau! You are now a Warrior! Your life will be full of battle, but also glory. I have given you a sword. Use it to protect the weak.");
            } else {
                cm.sendNext("Your inventory is full. Make room for your destiny.");
                cm.dispose();
            }
        } else if (status == 2) {
            cm.sendNextPrev("You are stronger now. I have also given you some #bSP#k. Use it to learn new techniques.");
        } else if (status == 3) {
            cm.sendNextPrev("Go now, and show the world the power of Perion!");
        } else {
            cm.dispose();
        }

    } else if (actionx["2ndJob"]) {
        if (status == 0) {
            if (cm.haveItem(4031012)) {
                cm.sendSimple("Now, tell me. Which path does your spirit guide you to?#b\r\n#L0#Please explain the Fighter.\r\n#L1#Please explain the Page.\r\n#L2#Please explain the Spearman.\r\n#L3#I have made my decision.");
            } else {
                // Giving the Letter to start the test
                cm.sendNext("Good. You look ready. But looks can deceive. Take this letter... treat it as your own life!");
                if (!cm.isQuestStarted(100003)) {
                    cm.startQuest(100003);
                }
            }
        } else if (status == 1) {
            if (!cm.haveItem(4031012)) {
                if (cm.canHold(4031008)) {
                    if (!cm.haveItem(4031008)) {
                        cm.gainItem(4031008, 1); // Warrior Letter
                    }
                    cm.sendNextPrev("Take this letter to the #rWarrior Job Instructor#k. He is training in #b#m102020300##k. Go quickly!");
                    cm.dispose();
                } else {
                    cm.sendNext("You have no space to hold this letter.");
                    cm.dispose();
                }
            } else {
                // Explaining Jobs
                if (selection < 3) {
                    if (selection == 0) { // Fighter
                        cm.sendNext("The #rFighter#k embodies pure power and rage. They use axes and swords to crush their enemies. They can enter a #rRage#k to increase their attack power.");
                    } else if (selection == 1) { // Page
                        cm.sendNext("The #rPage#k seeks balance. They use swords and blunt weapons, often infusing them with elemental power. They can #rThreaten#k enemies to weaken them.");
                    } else if (selection == 2) { // Spearman
                        cm.sendNext("The #rSpearman#k strikes from a distance with Polearms and Spears. Their #rHyper Body#k strengthens the vitality of their allies.");
                    }
                    status -= 2;
                } else {
                    cm.sendSimple("The spirits are listening... Choose your destiny!#b\r\n#L0#Fighter\r\n#L1#Page\r\n#L2#Spearman");
                }
            }
        } else if (status == 2) {
            // 0 = Fighter (110), 1 = Page (120), 2 = Spearman (130)
            sel = selection;
            job = 110 + (sel * 10);

            var jobName = (job == 110 ? "Fighter" : job == 120 ? "Page" : "Spearman");
            cm.sendYesNo("So you have chosen to be a #b" + jobName + "#k? There is no turning back once the ritual is done. Are you sure?");
        } else if (status == 3) {
            // Consume Proof and Letter
            if (cm.haveItem(4031012)) cm.gainItem(4031012, -1);
            if (cm.haveItem(4031008)) cm.gainItem(4031008, -1);

            // Complete Quest
            cm.completeQuest(100005);

            cm.sendNext("Hau! It is done. You are now a #b" + (job == 110 ? "Fighter" : job == 120 ? "Page" : "Spearman") + "#k. The spirits of Perion are with you.");
            if (cm.getJobId() != job) {
                cm.changeJobById(job);
            }
        } else if (status == 4) {
            cm.sendNextPrev("I have given you a book of skills for your new path. Your body feels stronger, does it not?");
        } else if (status == 5) {
            cm.sendNextPrev("I have also granted you a small amount of #bSP#k. Train hard, young warrior.");
        } else if (status == 6) {
            cm.sendNextPrev("We shall meet again when you have mastered this path.");
        }

    } else if (actionx["3thJobI"]) {
        if (status == 0) {
            if (cm.getPlayer().gotPartyQuestItem("JB3")) {
                cm.getPlayer().removePartyQuestItem("JB3");
                cm.getPlayer().setPartyQuestItemObtained("JBP");
            }
            cm.sendNextPrev("My clone is strong. Do not underestimate him. Good luck.");
        }
    } else if (actionx["3thJobC"]) {
        cm.getPlayer().removePartyQuestItem("JBP");
        cm.gainItem(4031059, -1);
        cm.gainItem(4031057, 1);
        cm.dispose();
    }
}