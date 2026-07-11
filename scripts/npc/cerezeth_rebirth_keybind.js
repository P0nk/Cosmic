var status = 0;
var skills;
var jobs;
var c_job = 0;
var c_skill = 0;
var p_job = 0;
const GameConstants = Java.type('constants.game.GameConstants');
var jobOptions = "";

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
    } else if (mode == 0 && status == 4) {
        cm.dispose();
        return;
    } else {
        cm.dispose();
        return;
    }

    if (status == 0) {
        cm.sendYesNo("Hey there! Do you want to learn some skills?");
    } else if (status == 1) {
        var message = "Please select a class:\r\n";
        rebornDataString = cm.getChar().getAllRebornDataCerezeth(); // get rebirth data, check characters_rebirth table
        rebornData = JSON.parse(rebornDataString); // change it to json object

        if (rebornData.length == 0) {
            cm.sendOk("You have not rebirthed yet!");
            cm.dispose();
        } else {
            var counter = 0;

            for (var rbc = 0; rbc < rebornData.length; rbc++) {
                jobOptions += `#L${counter}##b${GameConstants.getJobName(rebornData[rbc].job)}#l \r\n`;
                counter++;
            }

            cm.sendSimple("I see... which job class? \r\n"  + jobOptions);
        }
        // cm.sendSimple(message);
        // cm.sendSimple("Which key do you want #e#r#q" + c_skill + "##n#k on? #b\r\n" +
        //     "#L59#F1\r\n#L60#F2\r\n#L61#F3\r\n#L62#F4\r\n#L63#F5\r\n#L64#F6\r\n#L65#F7\r\n#L66#F8\r\n#L67#F9\r\n" +
        //     "#L68#F10\r\n#L87#F11\r\n#L88#F12\r\n" +
        //     "#L2#1\r\n#L3#2\r\n#L4#3\r\n#L5#4\r\n#L6#5\r\n#L7#6\r\n#L8#7\r\n#L9#8\r\n#L10#9\r\n#L11#0\r\n#L12#-\r\n#L13#=\r\n" +
        //     "#L16#Q\r\n#L17#W\r\n#L18#E\r\n#L19#R\r\n#L20#T\r\n#L21#Y\r\n#L22#U\r\n#L23#I\r\n#L24#O\r\n#L25#P\r\n#L26#[\r\n#L27#]\r\n" +
        //     "#L30#A\r\n#L31#S\r\n#L32#D\r\n#L33#F\r\n#L34#G\r\n#L35#H\r\n#L36#J\r\n#L37#K\r\n#L38#L\r\n#L39#;\r\n#L40#'\r\n" +
        //     "#L42#Shift\r\n#L44#Z\r\n#L45#X\r\n#L46#C\r\n#L47#V\r\n#L48#B\r\n#L49#N\r\n#L50#M\r\n#L51#,\r\n#L52#.\r\n#L42#Shift\r\n" +
        //     "#L29#Ctrl\r\n#L56#Alt\r\n#L57#SPACE\r\n#L56#Alt\r\n#L29#Ctrl\r\n#L82#Ins\r\n#L71#Hm\r\n#L73#Pup\r\n#L83#Del\r\n#L79#End\r\n#L81#Pdn");
        // }
    } else if (status == 2) {
        if (mode == 1) c_job = rebornData[selection].job;
        else c_job = p_job;
        p_job = c_job;
        skills = cm.getSkillsByJob(c_job);
        var message = skills.length + " Skills Available.\r\n\r\n#b";
        for (var i = 0; i < skills.length; i++) {
            message += "#L" + i + "##s" + skills[i] + "# #q" + skills[i] + "#\r\n";
        }
        cm.sendSimple(message);
    } else if (status == 3) {
        c_skill = skills[selection];
        cm.sendNextPrev("#s" + c_skill + "# #b#q" + c_skill + "##k\r\n\r\n" + cm.getSkillDesc(c_skill));
    } else if (status == 4) {
        cm.sendSimple("Which key do you want #e#r#q" + c_skill + "##n#k on? #b\r\n#L59#F1#L60#F2#L61#F3#L62#F4#L63#F5#L64#F6#L65#F7#L66#F8#L67#F9 \r\n #L68#F10#L87#F11#L88#F12 \r\n#L2#1#L3#2#L4#3#L5#4#L6#5#L7#6#L8#7#L9#8#L10#9#L11#0#L12#-#L13#= \r\n#L16#Q#L17#W#L18#E#L19#R#L20#T#L21#Y#L22#U#L23#I#L24#O#L25#P#L26#[#L27#] \r\n#L30#A#L31#S#L32#D#L33#F#L34#G#L35#H#L36#J#L37#K#L38#L#L39#;#L40#' \r\n#L42#Shift#L44#Z#L45#X#L46#C#L47#V#L48#B#L49#N#L50#M#L51#,#L52#.#L42#Shift \r\n#L29#Ctrl#L56#Alt#L57#SPACE#L56#Alt#L29#Ctrl \r\n#L82#Ins#L71#Hm#L73#Pup#L83#Del#L79#End#L81#Pdn");
    } else if (status == 5) {
        cm.sendOk("Enjoy your new skill!");
        cm.changeKeyBinding(selection, 1, c_skill);
        cm.dispose();
    } else {
        cm.sendOk("See you next time then.");
        cm.dispose();
    }
}