/* Grendel the Really Old
    Magician Job Advancement
    Victoria Road : Magic Library (101000003)
*/

var status = -1;
// Track which flow we are in
var actionx = {"1stJob": false, "2ndJob": false, "3thJobI": false, "3thJobC": false};
var job = 210;

var spawnPnpc = false;
var spawnPnpcFee = 7000000;
var jobType = 2; // Magician

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
        // 1st Job Advancement (Beginner -> Magician)
        if (cm.getJobId() == 0) {
            actionx["1stJob"] = true;
            cm.sendNext("Want to be a #rmagician#k? There are some standards to meet. We can't just accept EVERYONE in... #bYour level should be at least 8#k, with at least " + cm.getFirstJobStatRequirement(jobType) + " INT. Let's see.");

        // 2nd Job Advancement (Magician -> Wizard/Cleric)
        } else if (cm.getLevel() >= 30 && cm.getJobId() == 200) {
            actionx["2ndJob"] = true;
            if (cm.haveItem(4031012)) { // Proof of Hero
                cm.sendNext("I see you have done well. I will allow you to take the next step on your long road. Please, choose your path.");
            } else if (cm.haveItem(4031009)) { // Letter to Grendel (Already has it)
                cm.sendOk("Go and see the #b#p1032002##k in the Forest North of Ellinia."); // ID 1032002 or 1072001 depending on source, adjusted to likely Instructor ID
                cm.dispose();
            } else {
                cm.sendYesNo("The progress you have made is astonishing. You look strong, but I need to see if you really are strong enough to pass the test. Do you wish to take the test for the 2nd Job Advancement?");
            }

        // 3rd Job Advancement
        } else if (actionx["3thJobI"] || (cm.getPlayer().gotPartyQuestItem("JB3") && cm.getLevel() >= 70 && cm.getJobId() % 10 == 0 && parseInt(cm.getJobId() / 100) == 2 && !cm.getPlayer().gotPartyQuestItem("JBP"))) {
            actionx["3thJobI"] = true;
            cm.sendNext("There you are. A few days ago, #b#p2020009##k of Ossyria talked to me about you. I see that you are interested in making the leap to the enlightened of the third job advancement for magicians. To achieve that goal, I will have to test your strength in order to see whether you are worthy of the advancement. There is an opening in the middle of a deep forest of evil in Victoria Island, where it'll lead you to a secret passage. Once inside, you'll face a clone of myself. Your task is to defeat him and bring #b#t4031059##k back with you.");
        } else if (cm.getPlayer().gotPartyQuestItem("JBP") && !cm.haveItem(4031059)) {
            cm.sendNext("Please, bring me the #b#t4031059##k from my clone. You can find him inside a hole in space which is deep in a forest of evil.");
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
            if (cm.getLevel() >= 8 && cm.canGetFirstJob(jobType)) {
                cm.sendYesNo("Oh...! You look like someone that can definitely be a part of us... all you need is a little sinister mind, and... yeah... so, what do you think? Wanna be the Magician?");
            } else {
                cm.sendOk("Train a bit more until you reach the base requirements and I can show you the way of the #rMagician#k.");
                cm.dispose();
            }
        } else if (status == 1) {
            if (cm.canHold(1372043)) {
                if (cm.getJobId() == 0) {
                    cm.changeJobById(200);
                    cm.gainItem(1372043, 1); // Wooden Wand
                    cm.resetStats();
                }
                cm.sendNext("Alright, from here out, you are a part of us! You'll be living the life of a wanderer at ..., but just be patient as soon, you'll be living the high life. Alright, it ain't much, but I'll give you some of my abilities... HAAAHHH!!!");
            } else {
                cm.sendNext("Make some room in your inventory and talk back to me.");
                cm.dispose();
            }
        } else if (status == 2) {
            cm.sendNextPrev("You've gotten much stronger now. Plus every single one of your inventories have added slots. A whole row, to be exact. Go see for it yourself. I just gave you a little bit of #bSP#k.");
        } else if (status == 3) {
            cm.sendNextPrev("But remember, skills aren't everything. Your stats should support your skills as a Magician, also. Magicians use INT as their main stat, and LUK as their secondary stat.");
        } else if (status == 4) {
            cm.sendNextPrev("Now, one more word of warning to you. If you fail in battle from this point on, you will lose a portion of your total EXP. Be extra mindful of this, since you have less HP than most.");
        } else if (status == 5) {
            cm.sendNextPrev("This is all I can teach you. Good luck on your journey, young Magician.");
        } else {
            cm.dispose();
        }

    } else if (actionx["2ndJob"]) {
        if (status == 0) {
            if (cm.haveItem(4031012)) {
                cm.sendSimple("Alright, when you have made your decision, click on [I'll choose my occupation] at the bottom.#b\r\n#L0#Please explain to me what being the Wizard (Fire / Poison) is all about.\r\n#L1#Please explain to me what being the Wizard (Ice / Lightning) is all about.\r\n#L2#Please explain to me what being the Cleric is all about.\r\n#L3#I'll choose my occupation!");
            } else {
                // Giving the Letter
                cm.sendNext("Good decision. You look strong, but I need to see if you really are strong enough to pass the test. Here, take my letter first... make sure you don't lose it!");
                if (!cm.isQuestStarted(100006)) {
                    cm.startQuest(100006);
                }
            }
        } else if (status == 1) {
            if (!cm.haveItem(4031012)) {
                if (cm.canHold(4031009)) {
                    if (!cm.haveItem(4031009)) {
                        cm.gainItem(4031009, 1); // Letter to Grendel
                    }
                    cm.sendNextPrev("Please get this letter to #b#p1032002##k who's around #b#m101020000##k near Ellinia. He is taking care of the job of an instructor in place of me.");
                    cm.dispose();
                } else {
                    cm.sendNext("Please, make some space in your inventory.");
                    cm.dispose();
                }
            } else {
                // Explaining Jobs
                if (selection < 3) {
                    if (selection == 0) {
                        cm.sendNext("Magicians that master #rFire/Poison-based magic#k.\r\n\r\n#bWizards#k are a active class that deal magical, elemental damage. #bFire/Poison Wizards#k contains a powerful flame arrow attack and poison attack.");
                    } else if (selection == 1) {
                        cm.sendNext("Magicians that master #rIce/Lightning-based magic#k.\r\n\r\n#bWizards#k are a active class that deal magical, elemental damage. #bIce/Lightning Wizards#k have a freezing ice attack and a striking lightning attack.");
                    } else {
                        cm.sendNext("Magicians that master #rHoly magic#k.\r\n\r\n#bClerics#k are a powerful supportive class. They have the power to #rHeal#k themselves and others. #bClerics#k are especially effective against undead monsters.");
                    }
                    status -= 2;
                } else {
                    cm.sendSimple("Now... have you made up your mind? Please choose the job you'd like to select for your 2nd job advancement. #b\r\n#L0#Wizard (Fire / Poison)\r\n#L1#Wizard (Ice / Lighting)\r\n#L2#Cleric");
                }
            }
        } else if (status == 2) {
            if (cm.haveItem(4031009)) { // Safety check
                cm.dispose();
                return;
            }
            job += selection * 10;
            cm.sendYesNo("So you want to make the second job advancement as the " + (job == 210 ? "#bWizard (Fire / Poison)#k" : job == 220 ? "#bWizard (Ice / Lighting)#k" : "#bCleric#k") + "? You know you won't be able to choose a different job once you make your decision here, right?");
        } else if (status == 3) {
            if (cm.haveItem(4031012)) {
                cm.gainItem(4031012, -1);
            }
            cm.completeQuest(100008);
            cm.sendNext("Alright, you're the " + (job == 210 ? "#bWizard (Fire / Poison)#k" : job == 220 ? "#bWizard (Ice / Lighting)#k" : "#bCleric#k") + " from here on out. Please train yourself each and everyday.");
            if (cm.getJobId() != job) {
                cm.changeJobById(job);
            }
        } else if (status == 4) {
            cm.sendNextPrev("I have just given you a book that gives you the list of skills you can acquire. Your max HP and MP have increased, too.");
        } else if (status == 5) {
            cm.sendNextPrev("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner.");
        } else if (status == 6) {
            cm.sendNextPrev("Please find me after you have advanced much further. I'll be waiting for you.");
        }

    } else if (actionx["3thJobI"]) {
        if (status == 0) {
            if (cm.getPlayer().gotPartyQuestItem("JB3")) {
                cm.getPlayer().removePartyQuestItem("JB3");
                cm.getPlayer().setPartyQuestItemObtained("JBP");
            }
            cm.sendNextPrev("Since he is a clone of myself, you can expect a tough battle ahead. Good luck.");
        }
    } else if (actionx["3thJobC"]) {
        cm.getPlayer().removePartyQuestItem("JBP");
        cm.gainItem(4031059, -1);
        cm.gainItem(4031057, 1);
        cm.dispose();
    }
}