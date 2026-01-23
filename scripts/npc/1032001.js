/* Grendel the Really Old
    Magician Job Advancement
    Victoria Road : Magic Library (101000003)
*/

var status = -1;
var actionx = {"1stJob": false, "2ndJob": false, "3thJobI": false, "3thJobC": false};
var job = 210;
var sel = -1;

var spawnPnpc = false;
var spawnPnpcFee = 7000000;
var jobType = 2; // Magician

function start() {
    var GameConstants = Java.type('constants.game.GameConstants');

    // Hall of Fame Logic
    if (parseInt(cm.getJobId() / 100) == jobType && cm.canSpawnPlayerNpc(GameConstants.getHallOfFameMapid(cm.getJob()))) {
        spawnPnpc = true;
        var sendStr = "You have walked a long path of enlightenment to reach the wisdom you hold today. What do you say about having a #rNPC on the Hall of Fame#k holding your image? Do you wish to be immortalized in history?";
        if (spawnPnpcFee > 0) {
            sendStr += " I can do it for the fee of #b " + cm.numberWithCommas(spawnPnpcFee) + " mesos.#k";
        }
        cm.sendYesNo(sendStr);
    } else {
        // 1st Job Advancement (Beginner -> Magician)
        if (cm.getJobId() == 0) {
            actionx["1stJob"] = true;
            cm.sendNext("You wish to walk the path of the #rMagician#k? It requires a sharp mind. #bYour level should be at least 8, with at least " + cm.getFirstJobStatRequirement(jobType) + " INT#k. Let me sense your aura.");

        // 2nd Job Advancement (Magician -> Wizard/Cleric)
        } else if (cm.getLevel() >= 30 && cm.getJobId() == 200) {
            actionx["2ndJob"] = true;
            if (cm.haveItem(4031012)) { // Proof of Hero
                cm.sendNext("Ah... I sense a powerful object in your possession. You have returned with the Proof of a Hero. Truly magnificent. You are ready to ascend to the next level of magical mastery.");
            } else if (cm.haveItem(4031009)) { // Letter to Grendel
                cm.sendOk("Do not tarry, young one. Go and see the #bMagician Job Instructor#k in the #rForest North of Ellinia#k. The dimension of testing awaits.");
                cm.dispose();
            } else {
                cm.sendYesNo("Harrumph! You have grown... your mana pool is deeper than before. But are you wise enough for the next step? I must test your intellect and resolve. Do you wish to take the test for the 2nd Job Advancement?");
            }

        // 3rd Job Advancement
        } else if (actionx["3thJobI"] || (cm.getPlayer().gotPartyQuestItem("JB3") && cm.getLevel() >= 70 && cm.getJobId() % 10 == 0 && parseInt(cm.getJobId() / 100) == 2 && !cm.getPlayer().gotPartyQuestItem("JBP"))) {
            actionx["3thJobI"] = true;
            cm.sendNext("There you are. I have been meditating on your progress. I see that you are interested in the third job advancement. To achieve this, I must test your strength against a clone of myself. Defeat him and bring back the #b#t4031059##k.");
        } else if (cm.getPlayer().gotPartyQuestItem("JBP") && !cm.haveItem(4031059)) {
            cm.sendNext("Please, bring me the #b#t4031059##k from my clone. Focus your mind, and you shall succeed.");
            cm.dispose();
        } else if (cm.haveItem(4031059) && cm.getPlayer().gotPartyQuestItem("JBP")) {
            actionx["3thJobC"] = true;
            cm.sendNext("Excellent. You have defeated my clone and returned safely. Your wisdom now rivals my own... almost. Take this necklace to #b#p2020009##k in Ossyria.");
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
                    cm.sendOk("Knowledge is free, but the Hall of Fame is not. You lack the mesos.");
                    cm.dispose();
                    return;
                }
                var PlayerNPC = Java.type('server.life.PlayerNPC');
                var GameConstants = Java.type('constants.game.GameConstants');
                if (PlayerNPC.spawnPlayerNPC(GameConstants.getHallOfFameMapid(cm.getJob()), cm.getPlayer())) {
                    cm.sendOk("It is done. May your image inspire future generations.");
                    cm.gainMeso(-spawnPnpcFee);
                } else {
                    cm.sendOk("The Hall of Fame is currently full...");
                }
            }
            cm.dispose();
            return;
        } else {
            if (mode != 1 || status == 7 || (actionx["1stJob"] && status == 4) || (cm.haveItem(4031009) && status == 2) || (actionx["3thJobI"] && status == 1)) {
                if (mode == 0 && status == 2 && type == 1) {
                    cm.sendOk("The path of magic is not for the wavering mind...");
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
                cm.sendYesNo("Oh...! You look like someone with the spark of intelligence. Do you wish to become a #rMagician#k?");
            } else {
                cm.sendOk("Train a bit more. Read more books. Return when you meet the requirements.");
                cm.dispose();
            }
        } else if (status == 1) {
            if (cm.canHold(1372043)) {
                if (cm.getJobId() == 0) {
                    cm.changeJobById(200);
                    cm.gainItem(1372043, 1); // Wooden Wand
                    cm.resetStats();
                }
                cm.sendNext("Huzzah! You are now a Magician! I have given you a wand. It is humble, but it channels your will. Go forth and study the elements!");
            } else {
                cm.sendNext("Your inventory is full. Order is the first step to wisdom. Make space.");
                cm.dispose();
            }
        } else if (status == 2) {
            cm.sendNextPrev("I have also expanded your inventory slots and given you some #bSP#k. Use it to learn the basics of magic.");
        } else if (status == 3) {
            cm.sendNextPrev("Remember, Magicians rely on #bINT#k for power and #bLUK#k for equipment usage. Do not neglect your studies.");
        } else if (status == 4) {
            cm.sendNextPrev("Be careful. We are physically weak. If you die, you will lose experience. Keep your distance and strike with your mind.");
        } else {
            cm.dispose();
        }

    } else if (actionx["2ndJob"]) {
        if (status == 0) {
            if (cm.haveItem(4031012)) {
                cm.sendSimple("You have returned! Now, the final choice lies before you. Which path of magic calls to you?#b\r\n#L0#Wizard (Fire / Poison)\r\n#L1#Wizard (Ice / Lightning)\r\n#L2#Cleric\r\n#L3#I have made my decision.");
            } else {
                // Giving the Letter
                cm.sendNext("Excellent attitude. But knowledge must be tested. Take this letter... it is enchanted, do not lose it!");
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
                    cm.sendNextPrev("Deliver this letter to the #bMagician Job Instructor#k. He is meditating in the #bForest North of Ellinia#k. Go now.");
                    cm.dispose();
                } else {
                    cm.sendNext("Please, make some space in your inventory.");
                    cm.dispose();
                }
            } else {
                // Explaining Jobs
                if (selection < 3) {
                    if (selection == 0) {
                        cm.sendNext("The #rFire/Poison Wizard#k harnesses the destructive power of alchemy. They use Poison Breath to weaken foes and Fire Arrows to burn them.");
                    } else if (selection == 1) {
                        cm.sendNext("The #rIce/Lightning Wizard#k commands the weather. They can freeze enemies solid with Cold Beam or strike groups with Thunder Bolt.");
                    } else {
                        cm.sendNext("The #rCleric#k walks the path of light. They are the only ones who can #rHeal#k wounds and are the bane of all Undead monsters.");
                    }
                    status -= 2;
                } else {
                    cm.sendSimple("The cosmos awaits your decision... Choose your path!#b\r\n#L0#Wizard (Fire / Poison)\r\n#L1#Wizard (Ice / Lighting)\r\n#L2#Cleric");
                }
            }
        } else if (status == 2) {
            // 0 = Fire/Poison (210), 1 = Ice/Lightning (220), 2 = Cleric (230)
            sel = selection;
            job = 210 + (sel * 10);

            var jobName = (job == 210 ? "Wizard (Fire / Poison)" : job == 220 ? "Wizard (Ice / Lightning)" : "Cleric");
            cm.sendYesNo("So you wish to become a #b" + jobName + "#k? Once the spell is cast, it cannot be undone. Are you certain?");
        } else if (status == 3) {
            // Consume Proof and Letter
            if (cm.haveItem(4031012)) cm.gainItem(4031012, -1);
            if (cm.haveItem(4031009)) cm.gainItem(4031009, -1);

            cm.completeQuest(100008);

            cm.sendNext("Huzzah! You are now a #b" + (job == 210 ? "Wizard (Fire / Poison)" : job == 220 ? "Wizard (Ice / Lightning)" : "Cleric") + "#k! Use your magic to bring balance to the world.");
            if (cm.getJobId() != job) {
                cm.changeJobById(job);
            }
        } else if (status == 4) {
            cm.sendNextPrev("I have given you a spellbook containing new techniques. Study them well.");
        } else if (status == 5) {
            cm.sendNextPrev("I have also granted you a small amount of #bSP#k. Do not waste it.");
        } else if (status == 6) {
            cm.sendNextPrev("Come see me again when you have mastered these arts. I will be here, in the library.");
        }

    } else if (actionx["3thJobI"]) {
        if (status == 0) {
            if (cm.getPlayer().gotPartyQuestItem("JB3")) {
                cm.getPlayer().removePartyQuestItem("JB3");
                cm.getPlayer().setPartyQuestItemObtained("JBP");
            }
            cm.sendNextPrev("My clone is a reflection of my younger, more volatile self. Be careful.");
        }
    } else if (actionx["3thJobC"]) {
        cm.getPlayer().removePartyQuestItem("JBP");
        cm.gainItem(4031059, -1);
        cm.gainItem(4031057, 1);
        cm.dispose();
    }
}