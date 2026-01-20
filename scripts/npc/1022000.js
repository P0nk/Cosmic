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

    // Hall of Fame Logic
    if (parseInt(cm.getJobId() / 100) == jobType && cm.canSpawnPlayerNpc(GameConstants.getHallOfFameMapid(cm.getJob()))) {
        spawnPnpc = true;
        var sendStr = "You have walked a long way to reach the power, wisdom and courage you hold today, haven't you? What do you say about having right now #ra NPC on the Hall of Fame holding the current image of your character#k? Do you like it?";
        if (spawnPnpcFee > 0) {
            sendStr += " I can do it for you, for the fee of #b " + cm.numberWithCommas(spawnPnpcFee) + " mesos.#k";
        }
        cm.sendYesNo(sendStr);
    } else {
        // 1st Job Advancement
        if (cm.getJobId() == 0) {
            actionx["1stJob"] = true;
            cm.sendNext("Do you want to become a #rwarrior#k? You need to meet some criteria in order to do so.#b You should be at least in level 10, and at least " + cm.getFirstJobStatRequirement(jobType) + "#k. Let's see...");

        // 2nd Job Advancement
        } else if (cm.getLevel() >= 30 && cm.getJobId() == 100) {
            actionx["2ndJob"] = true;
            if (cm.haveItem(4031012)) { // Proof of Hero
                cm.sendNext("Oh... you came back safe! I knew you'd breeze through. I'll admit, you are a strong, formidable Warrior! Alright, I'll make you an even stronger Warrior than you already are. But before that, you need to choose one of the three paths that you'll be given. It isn't going to be easy, so if you have and questions, feel free to ask.");
            } else if (cm.haveItem(4031008)) { // Letter to Instructor
                cm.sendOk("Go and see the #b#p1072000##k."); // Instructor
                cm.dispose();
            } else {
                cm.sendNext("The progress you have made is astonishing.");
            }

        // 3rd Job Advancement
        } else if (actionx["3thJobI"] || (cm.getPlayer().gotPartyQuestItem("JB3") && cm.getLevel() >= 70 && (cm.getJobId() % 10 == 0 && parseInt(cm.getJobId() / 100) == 1 && !cm.getPlayer().gotPartyQuestItem("JBP")))) {
            actionx["3thJobI"] = true;
            cm.sendNext("I was waiting for you. Few days ago, I heard about you from #b#p2020008##k in Ossyria. Well... I'd like to test your strength. There is a secret passage near the ant tunnel. Nobody but you can go into that passage. If you go into the passage, you will meet my other self. Beat him and bring #b#t4031059##k to me.");
        } else if (cm.getPlayer().gotPartyQuestItem("JBP") && !cm.haveItem(4031059)) {
            cm.sendNext("Please, bring me the #b#t4031059##k.");
            cm.dispose();
        } else if (cm.haveItem(4031059) && cm.getPlayer().gotPartyQuestItem("JBP")) {
            actionx["3thJobC"] = true;
            cm.sendNext("Wow... You beat my other self and brought #b#t4031059##k to me. Good! this surely proves your strength. In terms of strength, you are ready to advance to 3rd job. As I promised, I will give #b#t4031057##k to you. Give this necklace to #b#p2020008##k in Ossyria and you will be able to take second test of 3rd job advancement. Good Luck~");
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
                    cm.sendOk("Sorry, you don't have enough mesos to purchase your place on the Hall of Fame.");
                    cm.dispose();
                    return;
                }
                var PlayerNPC = Java.type('server.life.PlayerNPC');
                var GameConstants = Java.type('constants.game.GameConstants');
                if (PlayerNPC.spawnPlayerNPC(GameConstants.getHallOfFameMapid(cm.getJob()), cm.getPlayer())) {
                    cm.sendOk("There you go! Hope you will like it.");
                    cm.gainMeso(-spawnPnpcFee);
                } else {
                    cm.sendOk("Sorry, the Hall of Fame is currently full...");
                }
            }
            cm.dispose();
            return;
        } else {
            if (mode != 1 || status == 7 && type != 1 || (actionx["1stJob"] && status == 4) || (cm.haveItem(4031008) && status == 2) || (actionx["3thJobI"] && status == 1)) {
                if (mode == 0 && status == 2 && type == 1) {
                    cm.sendOk("Make up your mind and visit me again.");
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
                cm.sendNextPrev("It is an important and final choice. You will not be able to turn back.");
            } else {
                cm.sendOk("Train a bit more until you reach the base requirements and I can show you the way of the #rWarrior#k.");
                cm.dispose();
            }
        } else if (status == 1) {
            if (cm.canHold(1302077)) {
                if (cm.getJobId() == 0) {
                    cm.changeJobById(100);
                    cm.gainItem(1302077, 1); // Warrior Sword
                    cm.resetStats();
                }
                cm.sendNext("From here on out, you are going to the Warrior path. This is not an easy job, but if you have discipline and confidence in your own body and skills, you will overcome any difficulties in your path. Go, young Warrior!");
            } else {
                cm.sendNext("Make some room in your inventory and talk back to me.");
                cm.dispose();
            }
        } else if (status == 2) {
            cm.sendNextPrev("You've gotten much stronger now. Plus every single one of your inventories have added slots. Go see for it yourself. I just gave you a little bit of #bSP#k.");
        } else if (status == 3) {
            cm.sendNextPrev("Now a reminder. Once you have chosen, you cannot change your mind. Go now, and live as a proud Warrior.");
        } else {
            cm.dispose();
        }

    } else if (actionx["2ndJob"]) {
        if (status == 0) {
            if (cm.haveItem(4031012)) {
                cm.sendSimple("Alright, when you have made your decision, click on [I'll choose my occupation] at the bottom.#b\r\n#L0#Please explain to me what being the Fighter is all about.\r\n#L1#Please explain to me what being the Page is all about.\r\n#L2#Please explain to me what being the Spearman is all about.\r\n#L3#I'll choose my occupation!");
            } else {
                cm.sendNext("Good decision. You look strong, but I need to see if you really are strong enough to pass the test. Here, take my letter first... make sure you don't lose it!");
                if (!cm.isQuestStarted(100003)) {
                    cm.startQuest(100003);
                }
            }
        } else if (status == 1) {
            if (!cm.haveItem(4031012)) {
                if (cm.canHold(4031008)) {
                    if (!cm.haveItem(4031008)) {
                        cm.gainItem(4031008, 1); // Letter to Instructor
                    }
                    cm.sendNextPrev("