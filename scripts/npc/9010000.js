/*
    Bot Check NPC - Enhanced Edition
    Author: MerogieMS
    
    Features:
    - Counter: "This is your Xth bot check"
    - Next bonus milestone display
    - Difficulty Grade (1-10) based on score
    - Streak tracking with bonus points
    - Speed bonus for fast answers
    - Wrong answer: shows explanation + correct answer before next question
    - Milestone rewards: EXP + GM buffs (scaled by grade, 10min duration)
    
    Quest ID 90100 used for persistent state (4 slots):
      slot 0 = total check count
      slot 1 = current grade (1-10)
      slot 2 = current streak
      slot 3 = total score

    Grade thresholds (cumulative score):
      G1=0, G2=60, G3=160, G4=310, G5=520, G6=800, G7=1160, G8=1620, G9=2200, G10=2900

    Milestone checks (every 10 up to 50, then every 25):
      10, 20, 30, 40, 50, 75, 100, 125, 150...

    GM Buff Items (10-min items per grade tier):
      Tier 1 (G1-2): 2022179  - Rainbow Bagel (1.2x stat)
      Tier 2 (G3-4): 2022430  - Hard Boiled Egg Sandwich (modest ATK/DEF buff)
      Tier 3 (G5-6): 2022112  - Rose Scented Perfume (1.5x atk buff)
      Tier 4 (G7-8): 2022218  - Mr. Smiles (strong all-stat)
      Tier 5 (G9-10): 2022631 - Power Elixir+ equivalent (max-tier buff)
    
    EXP Rewards per milestone (capped, not scaled by rate):
      Check 10: 50,000 EXP
      Check 20: 100,000 EXP
      Check 30: 150,000 EXP
      Check 40: 200,000 EXP
      Check 50: 300,000 EXP
      Check 75: 400,000 EXP
      Check 100: 500,000 EXP
      Check 125+: 500,000 EXP (capped)
*/

var QUEST_ID = 90100;
var SLOT_COUNT = 0;
var SLOT_GRADE = 1;
var SLOT_STREAK = 2;
var SLOT_SCORE = 3;

var GRADE_THRESHOLDS = [0, 60, 160, 310, 520, 800, 1160, 1620, 2200, 2900];

// Buff items per grade tier (grades 1-2, 3-4, 5-6, 7-8, 9-10)
var BUFF_ITEMS = [2022179, 2022430, 2022112, 2022218, 2022631];

var MILESTONE_EXP = {
    10: 50000,
    20: 100000,
    30: 150000,
    40: 200000,
    50: 300000,
    75: 400000,
    100: 500000
};

// Script-session state
var status = -1;
var questionText = "";
var answer = 0;
var op1 = 0, op2 = 0, op3 = 0;
var questionType = 0;   // 0=add, 1=sub, 2=mul, 3=two-step
var questionStartTime = 0;
var lastWasWrong = false;
var wrongExplanation = "";
var wrongCorrectAnswer = 0;
var consecutiveWrong = 0;  // track consecutive wrong answers this session

// Persistence for the explanation page (last failed question operands)
var prevOp1 = 0, prevOp2 = 0, prevOp3 = 0, prevType = 0;

function getState(slot) {
    var rec = cm.getQuestRecord(QUEST_ID);
    if (rec == null) return 0;
    var progress = rec.getProgress(slot);
    if (progress == null || progress == "") return 0;
    return parseInt(progress);
}

function setState(slot, val) {
    cm.setQuestProgress(QUEST_ID, slot, "" + val);
}

function getGradeLabel(g) {
    return "Grade " + g;
}

function getBuffTier(grade) {
    if (grade <= 2) return 0;
    if (grade <= 4) return 1;
    if (grade <= 6) return 2;
    if (grade <= 8) return 3;
    return 4;
}

function recalcGrade(score) {
    for (var i = 9; i >= 0; i--) {
        if (score >= GRADE_THRESHOLDS[i]) return i + 1;
    }
    return 1;
}

function nextMilestone(count) {
    var milestones = [10, 20, 30, 40, 50, 75, 100, 125, 150, 175, 200];
    for (var i = 0; i < milestones.length; i++) {
        if (milestones[i] > count) return milestones[i];
    }
    return Math.ceil((count + 1) / 25) * 25;
}

function isMilestone(count) {
    if (count <= 100 && count % 10 == 0) return true;
    if (count > 100 && count % 25 == 0) return true;
    return false;
}

function getMilestoneExp(count) {
    if (MILESTONE_EXP[count] != null) return MILESTONE_EXP[count];
    return 500000;
}

/**
 * Generates a new mathematical question based on the player's current Difficulty Grade.
 */
function generateQuestion(grade) {
    var r = Math.random();

    if (grade <= 3) {
        questionType = 0; // Addition
        op1 = Math.floor(Math.random() * 51);
        op2 = Math.floor(Math.random() * 51);
        answer = op1 + op2;
        questionText = op1 + " + " + op2;
    } else if (grade <= 5) {
        op1 = Math.floor(Math.random() * 201);
        op2 = Math.floor(Math.random() * (op1 + 1));
        if (r < 0.5) {
            questionType = 0; // Addition
            answer = op1 + op2;
            questionText = op1 + " + " + op2;
        } else {
            questionType = 1; // Subtraction
            answer = op1 - op2;
            questionText = op1 + " - " + op2;
        }
    } else if (grade <= 7) {
        questionType = 2; // Multiplication
        op1 = Math.floor(Math.random() * 11) + 2;
        op2 = Math.floor(Math.random() * 11) + 2;
        answer = op1 * op2;
        questionText = op1 + " * " + op2; // Changed ASCII '*'
    } else if (grade <= 9) {
        questionType = 3; // Mixed
        op1 = Math.floor(Math.random() * 9) + 2;
        op2 = Math.floor(Math.random() * 9) + 2;
        op3 = Math.floor(Math.random() * 51);
        answer = (op1 * op2) + op3;
        questionText = "(" + op1 + " * " + op2 + ") + " + op3;
    } else {
        var pick = Math.floor(Math.random() * 3);
        if (pick == 0) {
            questionType = 2;
            op1 = Math.floor(Math.random() * 11) + 2;
            op2 = Math.floor(Math.random() * 11) + 2;
            answer = op1 * op2;
            questionText = op1 + " * " + op2;
        } else if (pick == 1) {
            questionType = 3;
            op1 = Math.floor(Math.random() * 9) + 2;
            op2 = Math.floor(Math.random() * 9) + 2;
            op3 = Math.floor(Math.random() * 51);
            answer = (op1 * op2) + op3;
            questionText = "(" + op1 + " * " + op2 + ") + " + op3;
        } else {
            questionType = 1;
            op1 = Math.floor(Math.random() * 301) + 50;
            op2 = Math.floor(Math.random() * op1);
            answer = op1 - op2;
            questionText = op1 + " - " + op2;
        }
    }

    // Debug Logging
    console.log("[BotCheck] New Question Generated: Type=" + questionType + ", Grade=" + grade);
    console.log("[BotCheck] Operands: op1=" + op1 + ", op2=" + op2 + ", op3=" + op3);
    console.log("[BotCheck] Expected Answer: " + answer);
}

/**
 * Provides a text-based explanation of the solution for the last question.
 */
function getExplanation() {
    // Uses "prev" variables to ensure it explains the question the user just failed
    if (prevType == 0) {
        return prevOp1 + " + " + prevOp2 + " = " + wrongCorrectAnswer + ". Adding the two numbers gives " + wrongCorrectAnswer + ".";
    } else if (prevType == 1) {
        return prevOp1 + " - " + prevOp2 + " = " + wrongCorrectAnswer + ". Subtracting " + prevOp2 + " from " + prevOp1 + " gives " + wrongCorrectAnswer + ".";
    } else if (prevType == 2) {
        return prevOp1 + " * " + prevOp2 + " = " + wrongCorrectAnswer + ". " + prevOp1 + " groups of " + prevOp2 + " = " + wrongCorrectAnswer + ".";
    } else {
        return "(" + prevOp1 + " * " + prevOp2 + ") + " + prevOp3 + " = " + wrongCorrectAnswer + ". First multiply: " + prevOp1 + " * " + prevOp2 + " = " + (prevOp1 * prevOp2) + ", then add " + prevOp3 + " = " + wrongCorrectAnswer + ".";
    }
}

function start() {
    lastWasWrong = false;
    var grade = getState(SLOT_GRADE);
    if (grade < 1) grade = 1;

    console.log("[BotCheck] Starting session... Grade=" + grade);
    generateQuestion(grade);
    questionStartTime = Date.now();
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode != 1) {
        status = -1;
        cm.sendNext("#r! Security Verification Required.!#k\r\nYou cannot close this window until you answer the question correctly.");
        return;
    }

    status++;

    // --- PAGE 0: Show wrong-answer explanation ---
    if (status == 0 && lastWasWrong) {
        var expl = getExplanation();
        cm.sendNext("#rX Incorrect!#k\r\n\r\n" +
            "The correct answer was #b" + wrongCorrectAnswer + "#k.\r\n\r\n" +
            "#e" + expl + "#n\r\n\r\n" +
            "Study this and try a new question!");
        return;
    }

    // --- SHOW QUESTION ---
    if (status == 0 || (status == 1 && lastWasWrong)) {
        lastWasWrong = false;

        var count = getState(SLOT_COUNT);
        var grade = getState(SLOT_GRADE); if (grade < 1) grade = 1;
        var streak = getState(SLOT_STREAK);
        var next = nextMilestone(count);

        var streakLine = streak >= 3 ? "\r\n+ Streak: #r" + streak + " correct in a row!#k" : "";

        cm.sendGetNumber(
            "#e[ Security Verification ]#n\r\n" +
            "This is your #b" + (count + 1) + "#k bot check.\r\n" +
            "Next bonus at check #b" + next + "#k.\r\n" +
            "Difficulty: #e#g" + getGradeLabel(grade) + "#k#n" +
            streakLine + "\r\n\r\n" +
            "What is #b" + questionText + "#k?",
            0, -9999, 99999
        );
        questionStartTime = Date.now();
        return;
    }

    // --- EVALUATE ANSWER ---
    var elapsed = (Date.now() - questionStartTime) / 1000;

    var count = getState(SLOT_COUNT);
    var grade = getState(SLOT_GRADE); if (grade < 1) grade = 1;
    var streak = getState(SLOT_STREAK);
    var score = getState(SLOT_SCORE);

    console.log("[BotCheck] Evaluating Answer: Selection=" + selection + ", Correct=" + answer + ", Elapsed=" + elapsed + "s");

    if (selection == answer) {
        consecutiveWrong = 0;
        var pts = 10;
        var speedMsg = "";

        if (elapsed < 5) {
            pts += 10;
            speedMsg = " >> Speed bonus: +10 pts!";
        } else if (elapsed < 10) {
            pts += 5;
            speedMsg = " >> Speed bonus: +5 pts!";
        }

        streak++;
        var streakBonus = 0;
        if (streak % 3 == 0) {
            streakBonus = 5 * Math.floor(streak / 3);
            pts += streakBonus;
        }

        count++;
        score += pts;
        var newGrade = recalcGrade(score);
        var gradeUp = newGrade > grade;

        setState(SLOT_COUNT, count);
        setState(SLOT_GRADE, newGrade);
        setState(SLOT_STREAK, streak);
        setState(SLOT_SCORE, score);

        var msg = "#g[OK] Correct!#k Verification passed.\r\n\r\n";

        if (gradeUp) {
            msg += "#e[Grade Up] You are now #g" + getGradeLabel(newGrade) + "#k#n\r\n";
        }

        msg += "Score: +" + pts + " pts (Total: " + score + ")\r\n";
        if (speedMsg != "") msg += speedMsg + "\r\n";
        if (streakBonus > 0) msg += " + Streak bonus: +" + streakBonus + " pts! (" + streak + " streak)\r\n";
        msg += "Streak: " + streak + " | Grade: " + getGradeLabel(newGrade) + "\r\n";

        if (isMilestone(count)) {
            var exp = getMilestoneExp(count);
            var tier = getBuffTier(newGrade);
            var buffId = BUFF_ITEMS[tier];
            cm.gainExp(exp);
            cm.useItem(buffId);
            msg += "\r\n#e[Bonus] Milestone Reached — Check " + count + "!#n\r\n";
            msg += "  +" + exp.toLocaleString() + " EXP\r\n";
            msg += "  GM Buff applied for 10 minutes! (Tier " + (tier + 1) + ")";
        } else {
            var tier = getBuffTier(newGrade);
            cm.useItem(BUFF_ITEMS[tier]);
            msg += "\r\n#bGM Buff applied! (Grade " + getGradeLabel(newGrade) + " tier, 10 minutes)#k";
        }

        console.log("[BotCheck] Correct! New Score=" + score + ", Grade=" + newGrade);
        cm.sendOk(msg);
        cm.dispose();

    } else {
        // --- WRONG ANSWER HANDLING ---
        console.log("[BotCheck] Wrong Answer! Correct was " + answer);

        // PERSIST current failed state for the explanation page
        prevOp1 = op1;
        prevOp2 = op2;
        prevOp3 = op3;
        prevType = questionType;
        wrongCorrectAnswer = answer;

        streak = 0;
        consecutiveWrong++;

        if (consecutiveWrong >= 2 && grade > 1) {
            grade--;
            setState(SLOT_GRADE, grade);
            console.log("[BotCheck] Grade decreased to " + grade);
        }

        setState(SLOT_STREAK, 0);
        lastWasWrong = true;

        // Generate NEXT question (will NOT overwrite prevOp vars used for explanation)
        generateQuestion(grade);

        status = -1;
        cm.sendNext(
            "#r[X] Wrong answer!#k The window will now explain the correct solution.\r\n\r\n" +
            "Grade: " + getGradeLabel(grade) +
            (consecutiveWrong >= 2 ? "\r\n#rGrade decreased due to consecutive wrong answers.#k" : "")
        );
    }
}