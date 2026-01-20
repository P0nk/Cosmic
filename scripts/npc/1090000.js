/* Kyrin
    Pirate Job Advancement
    Victoria Road : Navigation Room (120000101)
*/

var status = -1;
var actionx = {"1stJob": false, "2ndJob": false, "2ndJobReborn": false, "3thJobI": false, "3thJobC": false};
var job = 510;
var advQuest = 0;

var spawnPnpc = false;
var spawnPnpcFee = 7000000;
var jobType = 5; // Pirate

function start() {
    var GameConstants = Java.type('constants.game.GameConstants');

    // ---------------------------------------------------------
    // SKILL QUESTS (Super Transformation / Battleship)
    // ---------------------------------------------------------
    if (cm.isQuestStarted(6330)) { // Super Transformation
        if (cm.getEventInstance() != null) {
            advQuest = 5;
            cm.sendNext("Not bad at all. Let's discuss this outside!");
        } else if (cm.getQuestProgressInt(6330, 6331) == 0) {
            advQuest = 1;
            cm.sendNext("You're ready, right? Now try to withstand my attacks for 2 minutes. I won't go easy on you.");
        } else {
            advQuest = 3;
            cm.teachSkill(5121003, 0, 10, -1);
            cm.forceCompleteQuest(6330);
            cm.sendNext("Congratulations. You have managed to pass my test. I'll teach you a new skill called \"Super Transformation\".");
        }
    } else if (cm.isQuestStarted(6370)) { // Battleship
        if (cm.getEventInstance() != null) {
            advQuest = 6;
            cm.sendNext("Not bad at all. Let's discuss this outside!");
        } else if (cm.getQuestProgressInt(6370, 6371) == 0) {
            advQuest = 2;
            cm.sendNext("You're ready, right? Now try to withstand my attacks for 2 minutes. I won't go easy on you.");
        } else {
            advQuest = 4;
            cm.teachSkill(5221006, 0, 10, -1);
            cm.forceCompleteQuest(6370);
            cm.sendNext("Congratulations. You have managed to pass my test. I'll teach you a new skill called \"Battleship\".");
        }

    // ---------------------------------------------------------
    // HALL OF FAME
    // ---------------------------------------------------------
    } else if (parseInt(cm.getJobId() / 100) == jobType && cm.canSpawnPlayerNpc(GameConstants.getHallOfFameMapid(cm.getJob()))) {
        spawnPnpc = true;
        var sendStr = "You have walked a long way... What do you say about having a NPC on the Hall of Fame? Fee: " + cm.numberWithCommas(spawnPnpcFee) + " mesos.";
        cm.sendYesNo(sendStr);

    } else {
        // ---------------------------------------------------------
        // 1ST JOB ADVANCEMENT (Beginner -> Pirate)
        // ---------------------------------------------------------
        if (cm.getJobId() == 0) {
            actionx["1stJob"] = true;
            cm.sendNext("Want to be a #rpirate#k? There are some standards to meet. #bYour level should be at least 10, with 20 DEX minimum#k. Let's see.");

        // ---------------------------------------------------------
        // 2ND JOB ADVANCEMENT (Pirate -> Brawler/Gunslinger)
        // ---------------------------------------------------------
        } else if (cm.getLevel() >= 30 && cm.getJobId() == 500) {

            // 1. Check if they have the PROOF (Completion)
            if (cm.haveItem(4031858) || cm.haveItem(4031859)) {
                actionx["2ndJob"] = true;
                cm.sendNext("I see you have done well. I will allow you to take the next step on your long road.");

            // 2. Check if Reborn (Bypass Quests)
            } else if (cm.getPlayer().getReborns() > 0) {
                actionx["2ndJobReborn"] = true;
                cm.sendNext("I see the spirit of a veteran pirate in you. Since you have been reborn, you do not need to hunt the crystals manually. Let's get you to the test.");

            // 3. Standard Quest Path
            } else if (cm.isQuestCompleted(2191) || cm.isQuestCompleted(2192)) {
                 // Fallback if they finished quest but lost the item, or old method
                 actionx["2ndJob"] = true;
                 cm.sendNext("I see you have done well. I will allow you to take the next step on your long road.");
            } else {
                // hasn't started test yet
                actionx["2ndJob"] = true;
                cm.sendNext("The progress you have made is astonishing. But you must pass the test first.");
            }

        // ---------------------------------------------------------
        // 3RD JOB ADVANCEMENT
        // ---------------------------------------------------------
        } else if (actionx["3thJobI"] || (cm.getPlayer().gotPartyQuestItem("JB3") && cm.getLevel() >= 70 && cm.getJobId() % 10 == 0 && parseInt(cm.getJobId() / 100) == 5 && !cm.getPlayer().gotPartyQuestItem("JBP"))) {
            actionx["3thJobI"] = true;
            cm.sendNext("There you are. A few days ago, #b#p2020013##k of Ossyria talked to me about you. I see that you are interested in making the leap to the world of the third job advancement.");
        } else if (cm.getPlayer().gotPartyQuestItem("JBP") && !cm.haveItem(4031059)) {
            cm.sendNext("Please, bring me the #b#t4031059##k.");
            cm.dispose();
        } else if (cm.haveItem(4031059) && cm.getPlayer().gotPartyQuestItem("JBP")) {
            actionx["3thJobC"] = true;
            cm.sendNext("Nice work. You have defeated my clone. You have proven yourself worthy.");
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
        // Skill Quest Logic
        if (advQuest > 0) {
            if (advQuest < 3) {
                var em = cm.getEventManager(advQuest == 1 ? "4jship" : "4jsuper");
                if (!em.startInstance(cm.getPlayer())) {
                    cm.sendOk("Someone is already challenging the test. Please try again later.");
                }
            } else {
                if (advQuest < 6) cm.setQuestProgress(6330, 6331, 2);
                else cm.setQuestProgress(6370, 6371, 2);
                cm.warp(120000101);
            }
            cm.dispose();
            return;
        }

        // Hall of Fame Logic
        else if (spawnPnpc) {
            if (mode > 0) {
                if (cm.getMeso() < spawnPnpcFee) {
                    cm.sendOk("Sorry, you don't have enough mesos.");
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
            if (cm.getLevel() >= 10 && cm.canGetFirstJob(jobType)) {
                cm.sendYesNo("Oh...! You look like someone that can definitely be a part of us... all you need is a little slang, and... yeah... so, what do you think? Wanna be the Pirate?");
            } else {
                cm.sendOk("Train a bit more until you reach the base requirements and I can show you the way of the #rPirate#k.");
                cm.dispose();
            }
        } else if (status == 1) {
            if (cm.canHold(2070000) && cm.canHoldAll([1482000, 1492000])) {
                if (cm.getJobId() == 0) {
                    cm.changeJobById(500);
                    cm.gainItem(1492000, 1); // Pistol
                    cm.gainItem(1482000, 1); // Knuckle
                    cm.gainItem(2330000, 1000); // Bullets
                    cm.resetStats();
                }
                cm.sendNext("Alright, from here out, you are a part of us! You'll be living the life of a wanderer at ..., but just be patient as soon, you'll be living the high life. Alright, it ain't much, but I'll give you some of my abilities... HAAAHHH!!!");
            } else {
                cm.sendNext("Make some room in your inventory and talk back to me.");
                cm.dispose();
            }
        } else if (status == 2) {
            cm.sendNextPrev("You've gotten much stronger now. Plus every single one of your inventories have added slots. Go see for it yourself. I just gave you a little bit of #bSP#k.");
        } else if (status == 3) {
            cm.sendNextPrev("Now a reminder. Once you have chosen, you cannot change up your mind and try to pick another path. Go now, and live as a proud Pirate.");
        } else {
            cm.dispose();
        }

    // -------------------------------------------------------------
    // 2ND JOB: REBIRTH ENTRY PATH
    // -------------------------------------------------------------
    } else if (actionx["2ndJobReborn"]) {
        if (status == 0) {
            cm.sendSimple("Which path are you taking this time? I will give you the required crystals directly.\r\n#b#L0#Brawler (Knuckles)#l\r\n#L1#Gunslinger (Guns)#l");
        } else if (status == 1) {
            var mapId = 0;
            var crystalId = 0;

            if (selection == 0) { // Brawler
                mapId = 108000502;
                crystalId = 4031856;
            } else { // Gunslinger
                mapId = 108000501;
                crystalId = 4031857;
            }

            if (cm.getPlayerCount(mapId) > 0) {
                cm.sendOk("All the training maps are currently in use. Please try again later.");
                cm.dispose();
            } else if (cm.canHold(crystalId, 15)) {
                cm.gainItem(crystalId, 15);
                cm.warp(mapId, 0);
                cm.dispose();
            } else {
                cm.sendOk("Please make space in your Etc inventory for the crystals.");
                cm.dispose();
            }
        }

    // -------------------------------------------------------------
    // 2ND JOB: ADVANCEMENT / STANDARD ENTRY
    // -------------------------------------------------------------
    } else if (actionx["2ndJob"]) {
        if (status == 0) {
            if (cm.haveItem(4031858) || cm.haveItem(4031859)) {
                // Has Proof, ready to advance
                cm.sendSimple("Alright, it looks like you are ready. Click on [I'll choose my occupation] at the bottom.#b\r\n#L0#Please explain to me what being the Brawler is all about.\r\n#L1#Please explain to me what being the Gunslinger is all about.\r\n#L3#I'll choose my occupation!");
            } else if (cm.isQuestCompleted(2191) || cm.isQuestCompleted(2192)) {
                // Fallback for old quest system
                cm.sendSimple("Alright, when you have made your decision, click on [I'll choose my occupation] at the bottom.#b\r\n#L0#Please explain to me what being the Brawler is all about.\r\n#L1#Please explain to me what being the Gunslinger is all about.\r\n#L3#I'll choose my occupation!");
            } else {
                // Standard Player: Has not started test
                cm.sendYesNo("Would you like to take the test now?");
            }
        } else if (status == 1) {
            // Logic for entering test (Standard Player)
            if (!cm.haveItem(4031858) && !cm.haveItem(4031859) && !cm.isQuestCompleted(2191) && !cm.isQuestCompleted(2192)) {
                var map = 0;
                if (cm.isQuestStarted(2191)) {
                    map = 108000502;
                } else {
                    map = 108000501; // Default to Gunslinger map if quest 2192 or neither
                }

                if (cm.getPlayerCount(map) > 0) {
                    cm.sendOk("All the training maps are currently in use. Please try again later.");
                    cm.dispose();
                } else {
                    cm.warp(map, 0);
                    cm.dispose();
                }
            }
            // Logic for Advancement (Has Proof)
            else {
                if (selection < 3) {
                    if (selection == 0) {    //brawler
                        cm.sendNext("Pirates that master #rKnuckles#k.\r\n\r\n#bBrawlers#k are melee, close-ranged fist fighters who deal lots of damage and have high HP.");
                    } else if (selection == 1) {    //gunslinger
                        cm.sendNext("Pirates that master #rGuns#k.\r\n\r\n#bGunslingers#k are faster and ranged attackers.");
                    }
                    status -= 2;
                } else {
                    cm.sendSimple("Now... have you made up your mind? #b\r\n#L0#Brawler\r\n#L1#Gunslinger");
                }
            }
        } else if (status == 2) {
            // Determine Job based on Proof Item first (Rebirth Safe)
            if (cm.haveItem(4031858)) {
                job = 510; // Brawler Proof
            } else if (cm.haveItem(4031859)) {
                job = 520; // Gunslinger Proof
            }
            // Fallback to Selection if user somehow has both or neither but passed checks (Legacy)
            else {
                job = (selection == 0) ? 510 : 520;
            }

            cm.sendYesNo("So you want to make the second job advancement as the " + (job == 510 ? "#bBrawler#k" : "#bGunslinger#k") + "? You know you won't be able to choose a different job for the 2nd job advancement once you make your decision here, right?");
        } else if (status == 3) {
            // Consume Proofs
            if (cm.haveItem(4031858)) cm.gainItem(4031858, -1);
            if (cm.haveItem(4031859)) cm.gainItem(4031859, -1);

            // Fallback consumption for old letter
            if (cm.haveItem(4031012)) cm.gainItem(4031012, -1);

            if (job == 510) {
                cm.sendNext("From here on out, you are a #bBrawler#k. Brawlers rule the world with the power of their bare fists...which means they need to train their body more than others.");
            } else {
                cm.sendNext("From here on out, you are a #bGunslinger#k. Gunslingers are notable for their long-range attacks with sniper-like accuracy.");
            }

            if (cm.getJobId() != job) {
                cm.changeJobById(job);
            }
        } else if (status == 4) {
            cm.sendNextPrev("I have just given you a book that gives you the list of skills you can acquire. Your max HP and MP have increased, too.");
        } else if (status == 5) {
            cm.sendNextPrev("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottom left corner.");
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