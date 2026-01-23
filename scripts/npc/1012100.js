/* Athena Pierce
    Bowman Job Advancement
    Victoria Road : Bowman Instructional School (100000201)
*/

var status = -1;
// Track which flow we are in
var actionx = {"1stJob": false, "2ndJob": false, "3thJobI": false, "3thJobC": false};
var job = 310;

var spawnPnpc = false;
var spawnPnpcFee = 7000000;
var jobType = 3; // Bowman

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
        // 1st Job Advancement (Beginner -> Bowman)
        if (cm.getJobId() == 0) {
            actionx["1stJob"] = true;
            cm.sendNext("So you decided to become a #rbowman#k? There are some standards to meet, y'know... #bYour level should be at least 10, with at least " + cm.getFirstJobStatRequirement(jobType) + " DEX#k. Let's see.");

        // 2nd Job Advancement (Bowman -> Hunter/Crossbowman)
        } else if (cm.getLevel() >= 30 && cm.getJobId() == 300) {
            actionx["2ndJob"] = true;
            if (cm.haveItem(4031012)) { // Proof of Hero
                cm.sendNext("Haha...I knew you'd breeze through that test. I'll admit, you are a great bowman. I'll make you much stronger than you're right now. Before that, however... you'll need to choose one of two paths given to you. It'll be a difficult decision for you to make, but... if there's any question to ask, please do so.");
            } else if (cm.haveItem(4031010)) { // Bowman Letter
                cm.sendOk("Go and see the #rBowman Job Instructor#k in the Road to the Dungeon.");
                cm.dispose();
            } else {
                cm.sendYesNo("Hmmm... you have grown a lot since I last saw you. I don't see the weakling I saw before, and instead, look much more like a bowman now. Well, what do you think? Don't you want to get even more powerful than that? Pass a simple test and I'll do just that for you. Do you want to do it?");
            }

        // 3rd Job Advancement
        } else if (actionx["3thJobI"] || (cm.getPlayer().gotPartyQuestItem("JB3") && cm.getLevel() >= 70 && cm.getJobId() % 10 == 0 && parseInt(cm.getJobId() / 100) == 3 && !cm.getPlayer().gotPartyQuestItem("JBP"))) {
            actionx["3thJobI"] = true;
            cm.sendNext("There you are. A few days ago, #b#p2020010##k of Ossyria talked to me about you. I see that you are interested in making the leap to the amazing world of the third job advancement for archers. To achieve that goal, I will have to test your strength in order to see whether you are worthy of the advancement. There is an opening in the middle of a deep forest in Victoria Island, where it'll lead you to a secret passage. Once inside, you'll face a clone of myself. Your task is to defeat her and bring #b#t4031059##k back with you.");
        } else if (cm.getPlayer().gotPartyQuestItem("JBP") && !cm.haveItem(4031059)) {
            cm.sendNext("Please, bring me the #b#t4031059##k.");
            cm.dispose();
        } else if (cm.haveItem(4031059) && cm.getPlayer().gotPartyQuestItem("JBP")) {
            actionx["3thJobC"] = true;
            cm.sendNext("Nice work. You have defeated my clone and brought #b#t4031059##k back safely. You have now proven yourself worthy of the 3rd job advancement from the physical standpoint. Now you should give this necklace to #b#p2020011##k in Ossyria to take on the second part of the test. Good luck. You'll need it.");
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
                cm.sendNextPrev("It is an important and final choice. You will not be able to turn back.");
            } else {
                cm.sendOk("Train a bit more until you reach the base requirements and I can show you the way of the #rBowman#k.");
                cm.dispose();
            }
        } else if (status == 1) {
            if (cm.canHold(1452051) && cm.canHold(2070000)) {
                if (cm.getJobId() == 0) {
                    cm.changeJobById(300);
                    cm.gainItem(1452051, 1); // Bow
                    cm.gainItem(2060000, 1000); // Arrows
                    cm.gainItem(1462092, 1); // Crossbow
                    cm.gainItem(2061000, 1000); // XBow Arrows

                    cm.resetStats();
                }
                cm.sendNext("Alright, from here out, you are a part of us! You'll be living the life of a wanderer, but just be patient. Alright, it ain't much, but I'll give you some of my abilities... HAAAHHH!!!");
            } else {
                cm.sendNext("Make some room in your inventory and talk back to me.");
                cm.dispose();
            }
        } else if (status == 2) {
            cm.sendNextPrev("You've gotten much stronger now. Plus every single one of your inventories have added slots. Go see for it yourself. I just gave you a little bit of #bSP#k.");
        } else if (status == 3) {
            cm.sendNextPrev("Now a reminder. Once you have chosen, you cannot change your mind. Go now, and live as a proud Bowman.");
        } else {
            cm.dispose();
        }

    } else if (actionx["2ndJob"]) {
        if (status == 0) {
            if (cm.haveItem(4031012)) {
                cm.sendSimple("Alright, when you have made your decision, click on [I'll choose my occupation] at the bottom.#b\r\n#L0#Please explain to me what being the Hunter is all about.\r\n#L1#Please explain to me what being the Crossbowman is all about.\r\n#L3#I'll choose my occupation!");
            } else {
                // Giving the Letter to start the test
                cm.sendNext("Good decision. You look strong, but I need to see if you really are strong enough to pass the test. Here, take my letter first... make sure you don't lose it!");
                if (!cm.isQuestStarted(100000)) {
                    cm.startQuest(100000);
                }
            }
        } else if (status == 1) {
            if (!cm.haveItem(4031012)) {
                if (cm.canHold(4031010)) {
                    if (!cm.haveItem(4031010)) {
                        cm.gainItem(4031010, 1); // Bowman Letter
                    }
                    cm.sendNextPrev("Please get this letter to #rBowman Job Instructor#k who's around #b#m106010000##k near Henesys. She is taking care of the job of an instructor in place of me.");
                    cm.dispose();
                } else {
                    cm.sendNext("Please, make some space in your inventory.");
                    cm.dispose();
                }
            } else {
                // Explaining Jobs
                if (selection < 3) {
                    if (selection == 0) {    // Hunter
                        cm.sendNext("Archers that master #rBows#k.\r\n\r\n#bHunters#k have a higher damage/minute output in early levels. #bHunters#k get #rArrow Bomb#k, an attack that can cause up to 6 enemies to get stunned.");
                    } else if (selection == 1) {    // Crossbowman
                        cm.sendNext("Archers that master #rCrossbows#k.\r\n\r\n#bCrossbowmans'#k attack power grows higher the higher level you are. #bCrossbowmans#k get #rIron Arrow#k, a stronger attack that can go through walls.");
                    }
                    status -= 2;
                } else {
                    cm.sendSimple("Now... have you made up your mind? Please choose the job you'd like to select for your 2nd job advancement. #b\r\n#L0#Hunter\r\n#L1#Crossbowman");
                }
            }
        } else if (status == 2) {
            // [FIX] Reset Job ID Calculation logic
            // 0 = Hunter (310), 1 = Crossbowman (320)
            job = 310 + (selection * 10);

            cm.sendYesNo("So you want to make the second job advancement as the " + (job == 310 ? "#bHunter#k" : "#bCrossbowman#k") + "? You know you won't be able to choose a different job once you make your decision here, right?");
        } else if (status == 3) {
            // Consume Proof
            if (cm.haveItem(4031012)) {
                cm.gainItem(4031012, -1);
            }
            // [FIX] Clean up Letter if it still exists
            if (cm.haveItem(4031010)) {
                cm.gainItem(4031010, -1);
            }

            // Standard quest completion for Bowman 2nd Job
            cm.completeQuest(100002);

            cm.sendNext("Alright, you're the " + (job == 310 ? "#bHunter#k" : "#bCrossbowman#k") + " from here on out. Please train yourself each and everyday.");
            if (cm.getJobId() != job) {
                cm.changeJobById(job);
            }
        } else if (status == 4) {
            cm.sendNextPrev("I have just given you a book that gives you the list of skills you can acquire as a " + (job == 310 ? "hunter" : "crossbowman") + ". Your max HP and MP have increased, too.");
        } else if (status == 5) {
            cm.sendNextPrev("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottom-left corner.");
        } else if (status == 6) {
            cm.sendNextPrev("Please find me after you have advanced much further. I'll be waiting for you.");
        }

    } else if (actionx["3thJobI"]) {
        if (status == 0) {
            if (cm.getPlayer().gotPartyQuestItem("JB3")) {
                cm.getPlayer().removePartyQuestItem("JB3");
                cm.getPlayer().setPartyQuestItemObtained("JBP");
            }
            cm.sendNextPrev("Since she is a clone of myself, you can expect a tough battle ahead. Good luck.");
        }
    } else if (actionx["3thJobC"]) {
        cm.getPlayer().removePartyQuestItem("JBP");
        cm.gainItem(4031059, -1);
        cm.gainItem(4031057, 1);
        cm.dispose();
    }
}