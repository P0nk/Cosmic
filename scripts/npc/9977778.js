// JavaScript
var status = 0;
var skills = [];
var jobs;
var c_job = 0;
var c_skill = 0;
var p_job = 0;
var c_macro_slot = 0;
var macro_skill1 = 0;
var macro_skill2 = 0;
var macro_skill3 = 0;
var macro_name = "";
var macroMode = false; // NEW: track macro setup flow
const GameConstants = Java.type('constants.game.GameConstants');
var SkillMacro = Java.type('client.SkillMacro'); // adjust package if needed
var jobOptions = "";
var rebornData;
var rebornDataString;

// NEW: store job -> skills mapping for nicer grouped menus
var jobSkillsMap = {};

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    } else if (mode == 1) {
        status++;
    } else if (mode == 0 && (status == 4 || status >= 15)) {
        cm.dispose();
        return;
    } else {
        cm.dispose();
        return;
    }

    // ==== MAIN MENU ====
    if (status == 0) {
        cm.sendSimple(
            "Hi #r#h #! #kWhat would you like to do today?\r\n" +
            "#b#L0#Learn Previous Rebirth Skills (Keybind)#l\r\n" +
            "#L1#Get 100 SP#l\r\n" +
            "#L2#Set up a Macro#l\r\n" +
            "#L3#Keybind Current Job#l"
        );

    // ==== OPTION: REBIRTH SKILL BIND ====
    } else if (status == 1 && selection === 0) {
        jobOptions = "";
        rebornDataString = cm.getChar().getAllRebornDataCerezeth();
        rebornData = JSON.parse(rebornDataString);

        if (rebornData.length == 0) {
            cm.sendOk("You have not rebirthed yet!");
            cm.dispose();
        } else {
            var counter = 0;
            for (var rbc = 0; rbc < rebornData.length; rbc++) {
                jobOptions += `#L${counter}##b${GameConstants.getJobName(rebornData[rbc].job)}#l \r\n`;
                counter++;
            }
            cm.sendSimple("I see... which job class? \r\n" + jobOptions);
        }

    } else if (status == 1 && selection === 1) {
        cm.getChar().gainSp(100, GameConstants.getSkillBook(cm.getJobId()), false);
        cm.dispose();

    // ==== OPTION: MACRO SETUP ====
    } else if (status == 1 && selection === 2) {
        rebornDataString = cm.getChar().getAllRebornDataCerezeth();
        rebornData = JSON.parse(rebornDataString);

        if (rebornData.length == 0) {
            cm.sendOk("You have not rebirthed yet!");
            cm.dispose();
        } else {
            // Gather skills across rebirth jobs
            skills = [];
            jobSkillsMap = {};
            for (var rbc = 0; rbc < rebornData.length; rbc++) {
                var job_id = rebornData[rbc].job;
                var job_primary_id = roundDownToNearest100(job_id);
                var job_sub_id = extractTensPlaceValue(job_id) * 10;

                for (var i = job_id; i >= job_primary_id; i--) {
                    if (i >= job_primary_id + job_sub_id || i == job_primary_id) {
                        addJobSkillsToSelectionList(i);
                    }
                }
            }

            macroMode = true; // enter macro setup mode
            cm.sendSimple("Which macro slot do you want to configure?\r\n#L0#Slot 1#l\r\n#L1#Slot 2#l\r\n#L2#Slot 3#l");
        }

    // ==== OPTION: KEYBIND CURRENT JOB ====
    } else if (status == 1 && selection === 3) {
        // Create a single-entry rebornData containing the current job so the standard flow can be reused.
        var currentJobId = cm.getJobId();
        rebornData = [{ job: currentJobId }];
        jobOptions = "#L0##b" + GameConstants.getJobName(currentJobId) + "#l \r\n";
        cm.sendSimple("Which job class? \r\n" + jobOptions);

    // ==== KEYBIND JOB -> SKILLS ====
    } else if (!macroMode && status == 2) {
        c_job = rebornData[selection].job;
        p_job = c_job;

        job_primary_id = roundDownToNearest100(c_job);
        job_sub_id = extractTensPlaceValue(c_job) * 10;

        var message = "";
        for (var i = c_job; i >= job_primary_id; i--) {
            if (i >= job_primary_id + job_sub_id || i == job_primary_id) {
                message += addJobSkillsToSelectionList(i);
            }
        }
        cm.sendSimple(message);

    } else if (!macroMode && status == 3) {
        c_skill = skills[selection];
        cm.sendNextPrev("#s" + c_skill + "# #b#q" + c_skill + "##k\r\n\r\n" + cm.getSkillDesc(c_skill));

    } else if (!macroMode && status == 4) {
        cm.sendSimple("Which key do you want #e#r#q" + c_skill + "##n#k on? #b\r\n" +
            "#L59#F1#L60#F2#L61#F3#L62#F4#L63#F5#L64#F6#L65#F7#L66#F8#L67#F9 \r\n" +
            "#L68#F10#L87#F11#L88#F12 \r\n" +
            "#L2#1#L3#2#L4#3#L5#4#L6#5#L7#6#L8#7#L9#8#L10#9#L11#0#L12#-#L13#= \r\n" +
            "#L16#Q#L17#W#L18#E#L19#R#L20#T#L21#Y#L22#U#L23#I#L24#O#L25#P#L26#[#L27#] \r\n" +
            "#L30#A#L31#S#L32#D#L33#F#L34#G#L35#H#L36#J#L37#K#L38#L#L39#;#L40#' \r\n" +
            "#L42#Shift#L44#Z#L45#X#L46#C#L47#V#L48#B#L49#N#L50#M#L51#,#L52#.#L42#Shift \r\n" +
            "#L29#Ctrl#L56#Alt#L57#SPACE#L56#Alt#L29#Ctrl \r\n" +
            "#L82#Ins#L71#Hm#L73#Pup#L83#Del#L79#End#L81#Pdn");

    } else if (!macroMode && status == 5) {
        cm.sendOk("Enjoy your new skill!");
        cm.changeKeyBinding(selection, 1, c_skill);
        cm.getChar().saveCharToDB();
        cm.dispose();

    // ==== MACRO FLOW ====
    } else if (macroMode && status == 2) { // slot chosen
        c_macro_slot = selection;
        cm.sendSimple("Select the FIRST skill for your macro:\r\n" + buildGroupedSkillList(false));

    } else if (macroMode && status == 3) {
        macro_skill1 = (selection >= skills.length ? 0 : skills[selection]);
        cm.sendSimple("Select the SECOND skill (or choose None):\r\n" + buildGroupedSkillList(true));

    } else if (macroMode && status == 4) {
        macro_skill2 = (selection >= skills.length ? 0 : skills[selection]);
        cm.sendSimple("Select the THIRD skill (or choose None):\r\n" + buildGroupedSkillList(true));

    } else if (macroMode && status == 5) {
        macro_skill3 = (selection >= skills.length ? 0 : skills[selection]);
        cm.sendGetText("What do you want to shout when using this macro?");

    } else if (macroMode && status == 6) {
        macro_name = cm.getText();

        // always shout = 1
        var shout = 1;
        var macro = new SkillMacro(macro_skill1, macro_skill2, macro_skill3, macro_name, shout, c_macro_slot);
        cm.getChar().updateMacros(c_macro_slot, macro);
        cm.getChar().saveCharToDB();
        cm.sendOk("Macro set successfully!");
        cm.dispose();

    } else {
        cm.sendOk("See you next time then.");
        cm.dispose();
    }
}


// ==== HELPERS ====
function roundDownToNearest100(number) {
    return Math.floor(number / 100) * 100;
}

function extractTensPlaceValue(number) {
    return Math.floor(number / 10) % 10;
}

function addJobSkillsToSelectionList(job_id) {
    var starting_index = skills.length;
    var curJobSkills = cm.getSkillsByJob(job_id);
    if (curJobSkills.length == 0) return "";
    skills = [...skills, ...curJobSkills];

    // store in map
    var jobName = GameConstants.getJobName(job_id);
    if (!jobSkillsMap[jobName]) jobSkillsMap[jobName] = [];
    jobSkillsMap[jobName] = [...jobSkillsMap[jobName], ...curJobSkills];

    var message = "";
    if (starting_index > 0) message += "\r\n\r\n";
    message += "#k" + curJobSkills.length + " " + jobName + " Skills Available.\r\n#b";
    for (var i = starting_index; i < skills.length; i++) {
        message += "#L" + i + "##s" + skills[i] + "# #q" + skills[i] + "##l\r\n";
    }
    return message;
}

function buildGroupedSkillList(allowNone) {
    var msg = "";
    for (var job in jobSkillsMap) {
        msg += "\r\n#e#d" + job + "#n#k\r\n"; // header
        for (var i = 0; i < jobSkillsMap[job].length; i++) {
            var skillId = jobSkillsMap[job][i];
            var globalIndex = skills.indexOf(skillId);
            msg += "#L" + globalIndex + "##s" + skillId + "# #q" + skillId + "##l\r\n";
        }
    }

    if (allowNone) {
        msg += "\r\n#L" + skills.length + "##rNone#k#l\r\n";
    }
    return msg;
}