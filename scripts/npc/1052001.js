/* Dark Lord
    Thief Job Advancement
    Victoria Road : Thieves' Hideout (103000003)
*/

var status = -1;
var actionx = {"1stJob": false, "2ndJob": false, "3thJobI": false, "3thJobC": false};
var job = 410;

var spawnPnpc = false;
var spawnPnpcFee = 7000000;
var jobType = 4; // Thief

function start() {
    var GameConstants = Java.type('constants.game.GameConstants');

    // Hall of Fame Logic
    if (parseInt(cm.getJobId() / 100) == jobType && cm.canSpawnPlayerNpc(GameConstants.getHallOfFameMapid(cm.getJob()))) {
        spawnPnpc = true;
        var sendStr = "You have walked a path of shadows to reach where you are today. Do you wish to leave a phantom image of yourself in the Hall of Fame? It will remain... while you vanish.";
        if (spawnPnpcFee > 0) {
            sendStr += " The fee is #b " + cm.numberWithCommas(spawnPnpcFee) + " mesos.#k";
        }
        cm.sendYesNo(sendStr);
    } else {
        // 1st Job Advancement (Beginner -> Thief)
        if (cm.getJobId() == 0) {
            actionx["1stJob"] = true;
            cm.sendNext("So... you wish to walk the path of the #rRogue#k? It is a lonely road. #bYour level must be at least 10, with at least 25 DEX#k. Let me see if you have the agility.");

        // 2nd Job Advancement (Thief -> Assassin/Bandit)
        } else if (cm.getLevel() >= 30 && cm.getJobId() == 400) {
            actionx["2ndJob"] = true;
            if (cm.haveItem(4031012)) { // Proof of Hero
                cm.sendNext("You have returned. And I see the Proof of a Hero in your hands. You have passed the test of shadows. I am impressed.");
            } else if (cm.haveItem(4031011)) { // Letter to Dark Lord
                cm.sendOk("Do not waste my time. Take the letter to the #bThief Job Instructor#k at the #rConstruction Site North of Kerning City#k. Do not be seen.");
                cm.dispose();
            } else {
                cm.sendYesNo("You move quietly, but are you deadly? I must test your skills before I teach you the higher arts. Are you ready for the test of the 2nd Job Advancement?");
            }

        // 3rd Job Advancement
        } else if (actionx["3thJobI"] || (cm.getPlayer().gotPartyQuestItem("JB3") && cm.getLevel() >= 70 && cm.getJobId() % 10 == 0 && parseInt(cm.getJobId() / 100) == 4 && !cm.getPlayer().gotPartyQuestItem("JBP"))) {
            actionx["3thJobI"] = true;
            cm.sendNext("I have received word from #b#p2020011##k in Ossyria. You seek the Third Path. To achieve this, you must defeat a clone of myself. It awaits in the deep swamp. Bring me the #b#t4031059##k.");
        } else if (cm.getPlayer().gotPartyQuestItem("JBP") && !cm.haveItem(4031059)) {
            cm.sendNext("Return only when you have the #b#t4031059##k. Failure is not an option.");
            cm.dispose();
        } else if (cm.haveItem(4031059) && cm.getPlayer().gotPartyQuestItem("JBP")) {
            actionx["3thJobC"] = true;
            cm.sendNext("You have defeated my shadow and returned the #b#t4031059##k. You are truly worthy. Take this necklace to #b#p2020011##k in Ossyria.");
        } else {
            cm.sendOk("The shadows protect you.");
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
                    cm.sendOk("You cannot afford the price of fame.");
                    cm.dispose();
                    return;
                }
                var PlayerNPC = Java.type('server.life.PlayerNPC');
                var GameConstants = Java.type('constants.game.GameConstants');
                if (PlayerNPC.spawnPlayerNPC(GameConstants.getHallOfFameMapid(cm.getJob()), cm.getPlayer())) {
                    cm.sendOk("It is done.");
                    cm.gainMeso(-spawnPnpcFee);
                } else {
                    cm.sendOk("The Hall of Fame is full.");
                }
            }
            cm.dispose();
            return;
        } else {
            if (mode != 1 || status == 7 || (actionx["1stJob"] && status == 4) || (cm.haveItem(4031011) && status == 2) || (actionx["3thJobI"] && status == 1)) {
                if (mode == 0 && status == 2 && type == 1) {
                    cm.sendOk("Hesitation leads to death.");
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
                cm.sendYesNo("You have the look of a survivor. Do you wish to join the brotherhood and become a #rThief#k?");
            } else {
                cm.sendOk("You are not ready. Train until you meet the requirements.");
                cm.dispose();
            }
        } else if (status == 1) {
            if (cm.canHold(2070000) && cm.canHoldAll([1472061, 1332063])) {
                if (cm.getJobId() == 0) {
                    cm.changeJobById(400);
                    cm.gainItem(2070015, 500); // Subis
                    cm.gainItem(1472061, 1); // Garnier
                    cm.gainItem(1332063, 1); // Knife
                    cm.resetStats();
                }
                cm.sendNext("You are now a Rogue. You live in the shadows now. I have given you the basic tools of our trade.");
            } else {
                cm.sendNext("Clear your inventory. We travel light.");
                cm.dispose();
            }
        } else if (status == 2) {
            cm.sendNextPrev("I have improved your stats and given you #bSP#k. Use it to sharpen your skills.");
        } else if (status == 3) {
            cm.sendNextPrev("Once you choose this life, there is no turning back. Go.");
        } else {
            cm.dispose();
        }

    } else if (actionx["2ndJob"]) {
        if (status == 0) {
            if (cm.haveItem(4031012)) {
                cm.sendSimple("You have passed the test. Now... choose your specialty.#b\r\n#L0#Explain the Assassin.\r\n#L1#Explain the Bandit.\r\n#L2#I have made my choice.");
            } else {
                // Giving the Letter
                cm.sendNext("Take this letter. It is sealed. If the seal is broken, you fail. Deliver it to the #bThief Job Instructor#k.");
                if (!cm.isQuestStarted(100009)) {
                    cm.startQuest(100009);
                }
            }
        } else if (status == 1) {
            if (!cm.haveItem(4031012)) {
                if (cm.canHold(4031011)) {
                    if (!cm.haveItem(4031011)) {
                        cm.gainItem(4031011, 1); // Letter to Dark Lord
                    }
                    cm.sendNextPrev("He is hiding at the #bConstruction Site North of Kerning City#k. Go.");
                    cm.dispose();
                } else {
                    cm.sendNext("Make space in your inventory.");
                    cm.dispose();
                }
            } else {
                // Explaining Jobs
                if (selection < 2) {
                    if (selection == 0) {    // Assassin
                        cm.sendNext("The #rAssassin#k strikes from the shadows with throwing stars. They rely on critical hits and range. A costly path, but deadly.");
                    } else if (selection == 1) {    // Bandit
                        cm.sendNext("The #rBandit#k strikes up close with daggers. They are agile and use the #rSavage Blow#k to decimate enemies.");
                    }
                    status -= 2;
                } else {
                    cm.sendSimple("Time is money. Choose.#b\r\n#L0#Assassin\r\n#L1#Bandit");
                }
            }
        } else if (status == 2) {
            job = 410 + (selection * 10);
            cm.sendYesNo("So you want to be an " + (job == 410 ? "#bAssassin#k" : "#bBandit#k") + "? There is no turning back.");
        } else if (status == 3) {
            if (cm.haveItem(4031012)) cm.gainItem(4031012, -1);
            if (cm.haveItem(4031011)) cm.gainItem(4031011, -1);

            cm.completeQuest(100011);

            if (job == 410) {
                cm.sendNext("You are now an #bAssassin#k. Your hands must be quick.");
            } else {
                cm.sendNext("You are now a #bBandit#k. Strike fast and hard.");
            }

            if (cm.getJobId() != job) {
                cm.changeJobById(job);
            }
        } else if (status == 4) {
            cm.sendNextPrev("I have given you the skill book. Memorize it.");
        } else if (status == 5) {
            cm.sendNextPrev("I have also granted you some #bSP#k.");
        } else if (status == 6) {
            cm.sendNextPrev("Do not fail me. Dismissed.");
        }

    } else if (actionx["3thJobI"]) {
        if (status == 0) {
            if (cm.getPlayer().gotPartyQuestItem("JB3")) {
                cm.getPlayer().removePartyQuestItem("JB3");
                cm.getPlayer().setPartyQuestItemObtained("JBP");
            }
            cm.sendNextPrev("My clone is as deadly as I am. Be careful.");
        }
    } else if (actionx["3thJobC"]) {
        cm.getPlayer().removePartyQuestItem("JBP");
        cm.gainItem(4031059, -1);
        cm.gainItem(4031057, 1);
        cm.dispose();
    }
}