/* Kyrin
    Pirate Job Advancement
    Victoria Road : Navigation Room (120000101)
*/

var status = -1;
// Simplified Flow Control
var actionx = {"1stJob": false, "2ndJob": false, "3thJobI": false, "3thJobC": false};

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
    if (cm.isQuestStarted(6330)) {
        if (cm.getEventInstance() != null) {
            advQuest = 5;
            cm.sendNext("Hah! You survived! Let's discuss this outside, matey.");
        } else if (cm.getQuestProgressInt(6330, 6331) == 0) {
            advQuest = 1;
            cm.sendNext("Batton down the hatches! Survive my attacks for 2 minutes.");
        } else {
            advQuest = 3;
            cm.teachSkill(5121003, 0, 10, -1);
            cm.forceCompleteQuest(6330);
            cm.sendNext("You have a hull of steel. I'll teach you \"Super Transformation\".");
        }
    } else if (cm.isQuestStarted(6370)) {
        if (cm.getEventInstance() != null) {
            advQuest = 6;
            cm.sendNext("Impressive sailing! Let's discuss this outside.");
        } else if (cm.getQuestProgressInt(6370, 6371) == 0) {
            advQuest = 2;
            cm.sendNext("Man the cannons! Survive my attacks for 2 minutes. Ready?");
        } else {
            advQuest = 4;
            cm.teachSkill(5221006, 0, 10, -1);
            cm.forceCompleteQuest(6370);
            cm.sendNext("Good work! I'll teach you how to command a \"Battleship\". Rule the seas!");
        }

    // ---------------------------------------------------------
    // HALL OF FAME
    // ---------------------------------------------------------
    } else if (parseInt(cm.getJobId() / 100) == jobType && cm.canSpawnPlayerNpc(GameConstants.getHallOfFameMapid(cm.getJob()))) {
        spawnPnpc = true;
        cm.sendYesNo("Ahoy! You've sailed rough waters to reach this level of infamy. What do you say about having a #rstatue in the Hall of Fame#k? It'll cost you #b" + cm.numberWithCommas(spawnPnpcFee) + " mesos.#k");

    } else {
        // ---------------------------------------------------------
        // 1ST JOB ADVANCEMENT (Beginner -> Pirate)
        // ---------------------------------------------------------
        if (cm.getJobId() == 0) {
            actionx["1stJob"] = true;
            cm.sendNext("So, you want to join my crew and become a #rPirate#k? The sea is a harsh mistress. #bYou need to be Level 10 with at least 20 DEX#k. Let me see your sea legs.");

        // ---------------------------------------------------------
        // 2ND JOB ADVANCEMENT (Pirate -> Brawler/Gunslinger)
        // ---------------------------------------------------------
        } else if (cm.getLevel() >= 30 && cm.getJobId() == 500) {
            // IMMEDIATE ADVANCEMENT LOGIC
            // No checks for items, proofs, or quests. Just Level 30 + Job 500.
            actionx["2ndJob"] = true;
            cm.sendSimple("Ahoy! I can see the strength in your eyes, matey. You don't need a test to prove your worth to me. I'll let you advance right now! Which path will you sail?\r\n#b#L0#Brawler (Knuckles)#l\r\n#L1#Gunslinger (Guns)#l");

        // ---------------------------------------------------------
        // 3RD JOB ADVANCEMENT
        // ---------------------------------------------------------
        } else if (actionx["3thJobI"] || (cm.getPlayer().gotPartyQuestItem("JB3") && cm.getLevel() >= 70 && cm.getJobId() % 10 == 0 && parseInt(cm.getJobId() / 100) == 5 && !cm.getPlayer().gotPartyQuestItem("JBP"))) {
            actionx["3thJobI"] = true;
            cm.sendNext("There you are! #b#p2020013##k sent word from Ossyria. You're looking to become a true Captain? To do that, you must defeat my shadow. Go to the hidden door in the engine room.");
        } else if (cm.getPlayer().gotPartyQuestItem("JBP") && !cm.haveItem(4031059)) {
            cm.sendNext("Don't come back until you have the #b#t4031059##k! A Pirate never gives up!");
            cm.dispose();
        } else if (cm.haveItem(4031059) && cm.getPlayer().gotPartyQuestItem("JBP")) {
            actionx["3thJobC"] = true;
            cm.sendNext("Hah! You defeated my clone! You're tougher than a kraken. Take this necklace to #b#p2020011##k in El Nath.");
        } else {
            cm.sendOk("The wind is in your sails. Keep training.");
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
        // Skill Quest Handling
        if (advQuest > 0) {
            if (advQuest < 3) {
                var em = cm.getEventManager(advQuest == 1 ? "4jship" : "4jsuper");
                if (!em.startInstance(cm.getPlayer())) {
                    cm.sendOk("The testing room is occupied. Wait your turn, matey!");
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
                    cm.sendOk("You're short on gold, matey.");
                    cm.dispose();
                    return;
                }
                var PlayerNPC = Java.type('server.life.PlayerNPC');
                var GameConstants = Java.type('constants.game.GameConstants');
                if (PlayerNPC.spawnPlayerNPC(GameConstants.getHallOfFameMapid(cm.getJob()), cm.getPlayer())) {
                    cm.sendOk("Aye! Your legend shall live on!");
                    cm.gainMeso(-spawnPnpcFee);
                } else {
                    cm.sendOk("The Hall of Fame is full! Come back later.");
                }
            }
            cm.dispose();
            return;
        } else {
            // General Exit Logic
            if (mode != 1 || status == 7 || (actionx["1stJob"] && status == 4) || (actionx["3thJobI"] && status == 1)) {
                if (mode == 0 && status == 2 && type == 1) {
                    cm.sendOk("Lost your nerve? Come back when you find it!");
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
                cm.sendYesNo("The life of a Pirate is dangerous. We fight for freedom and treasure! Do you want to join us?");
            } else {
                cm.sendOk("You're too weak. Come back when you're Level 10.");
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
                cm.sendNext("Hah! Welcome aboard! I've given you a gun and a knuckle. Use them well.");
            } else {
                cm.sendNext("Your bags are full! Clear some space!");
                cm.dispose();
            }
        } else if (status == 2) {
            cm.sendNextPrev("I've unlocked your potential and given you some #bSP#k. Don't waste it.");
        } else if (status == 3) {
            cm.sendNextPrev("Once a Pirate, always a Pirate. There is no turning back!");
        } else {
            cm.dispose();
        }

    // -------------------------------------------------------------
    // 2ND JOB: IMMEDIATE ADVANCEMENT LOGIC
    // -------------------------------------------------------------
    } else if (actionx["2ndJob"]) {
        if (status == 0) {
            // Selection happens here from start()'s sendSimple
            // 0 = Brawler, 1 = Gunslinger
            job = (selection == 0) ? 510 : 520;
            cm.sendYesNo("So you want to be a " + (job == 510 ? "#bBrawler#k" : "#bGunslinger#k") + "? There's no turning back once you sign the contract, matey. Are you sure?");
        } else if (status == 1) {
            // Apply Job Change
            cm.changeJobById(job);

            if (job == 510) {
                cm.sendNext("Welcome to the brotherhood! You are now a #bBrawler#k. Let your fists do the talking!");
            } else {
                cm.sendNext("Welcome to the brotherhood! You are now a #bGunslinger#k. Keep your powder dry and your aim true!");
            }
        } else if (status == 2) {
            cm.sendNextPrev("I have just given you a book that gives you the list of skills you can acquire. Your max HP and MP have increased, too.");
        } else if (status == 3) {
            cm.sendNextPrev("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottom left corner.");
        } else if (status == 4) {
            cm.sendNextPrev("Now get out there and make a name for yourself!");
        }

    } else if (actionx["3thJobI"]) {
        if (status == 0) {
            if (cm.getPlayer().gotPartyQuestItem("JB3")) {
                cm.getPlayer().removePartyQuestItem("JB3");
                cm.getPlayer().setPartyQuestItemObtained("JBP");
            }
            cm.sendNextPrev("My clone is strong. Don't underestimate yourself.");
        }
    } else if (actionx["3thJobC"]) {
        cm.getPlayer().removePartyQuestItem("JBP");
        cm.gainItem(4031059, -1);
        cm.gainItem(4031057, 1);
        cm.dispose();
    }
}