var status = 0;
var cost = 10000000;

function start() {
    cm.sendNext("#eHi i am the NPC in charge of exchanging all 4th job skills for a mere sum of 10m mesos.");
}

function action(mode, type, selection) {
    status++;
    if (mode == -1) {
        if (mode == 0)
            cm.sendNext("Welcome to MapleRoot!");
        cm.dispose();
        return;
    }
    if (status == 1) {
        if (mode == 0) {
            cm.sendNext("Ok, I understand. Come again later.");
            cm.dispose();
            return;
        }
        cm.sendYesNo("This will add all the 4th job skills that require either Questing or Skillbooks, are you sure you want them?");
        cost *= cm.getJobId() == 0 ? 10 : 1;
    } else if (status == 2) {
        if (cm.getMeso() < cost) {
            cm.sendNext("You don't have enough mesos, come back when you have 10m mesos.");
            cm.dispose();
        } else {
            if (mode == 1) {
                cm.gainMeso(-cost);
                // Skill teaching logic
                var skillTeachingArray = [
    [3221007, 1321007, 2321006, 5221006],
    [1121010, 1121000, 1221000, 1321000],
    [2121000, 2221000, 2321000, 3121000, 3221000],
    [4121000, 4221000, 5121000, 5121009],
    [5220011, 5220002, 5221000, 1320008],
    [1320009, 1120004, 1120003, 1120005, 1121008],
    [1121001, 1121006, 1121002, 1220005, 1220010],
    [1221009, 1220006, 1221001, 1221007],
    [1221002, 1320005, 1320006, 1321001, 1321003],
    [1321002, 2121005, 2121004, 2121002],
    [2121007, 2121006, 2221007, 2221006],
    [2221003, 2221005, 2221004, 2221002, 2321007],
    [2321003, 2321008, 2321005, 2321004],
    [2321002, 3120005, 3121008, 3121003, 3121007],
    [3121006, 3121002, 3121004, 3221006, 3220004],
    [3221003, 3221005, 3221001, 3221002, 4120002],
    [4121008, 4121006, 4121007],
    [4120005, 4221007, 4220002],
    [4221006, 4220005, 5121001],
    [5121002, 5121010, 5220001],
    [5221003, 5221004, 5221007, 5221008, 20001004],
    [21120001, 21120002, 21120004, 21120005, 21120006],
    [21120007, 21120009, 21120010, 21121000, 21121003],
    [11110000, 11110005, 11111001, 11111002, 11111003],
    [11111004, 11111006, 11111007, 12110000, 12110001],
    [12111002, 12111003, 12111004, 12111005, 12111006],
    [13110003, 13111000, 13111001, 13111002, 13111004],
    [14110003, 14110004],
    [14111000, 14111001, 14111002, 14111005, 14111006],
    [15110000, 15111001, 15111004],
    [15111005, 15111006, 15111007, 2121003, 1221003]
];

for (var i = 0; i < skillTeachingArray.length; i++) {
    var skills = skillTeachingArray[i];
    for (var j = 0; j < skills.length; j++) {
        var skillId = skills[j];
        var skillLevel = (skillId === 1320008 || skillId === 1320009) ? 25 : 10;
        cm.teachSkill(skillId, 0, skillLevel, -1);
        cm.teachSkill(1005, 1, 1, -1);
        cm.teachSkill(10001005, 1, 1, -1);
        cm.teachSkill(20001005, 1, 1, -1);
        cm.teachSkill(1121011, 1, 5, -1);
        cm.teachSkill(1321010, 1, 5, -1);
        cm.teachSkill(2121008, 1, 5, -1);
        cm.teachSkill(2221008, 1, 5, -1);
        cm.teachSkill(2321009, 1, 5, -1);
        cm.teachSkill(3121009, 1, 5, -1);
        cm.teachSkill(4111002, 30, 30, -1);
        cm.teachSkill(4121009, 1, 5, -1);
        cm.teachSkill(4221008, 1, 5, -1);
        cm.teachSkill(5111013, 1, 10, -1);
        cm.teachSkill(5121008, 1, 5, -1);
        cm.teachSkill(5221010, 1, 5, -1);
        cm.teachSkill(14111000, 30, 30, -1);
        cm.teachSkill(11121204, 1, 30, -1);
        cm.teachSkill(1121012, 1, 5, -1);
        cm.teachSkill(1221017, 1, 5, -1);
        cm.teachSkill(1321011, 1, 5, -1);
        cm.teachSkill(1321012, 1, 5, -1);
        cm.teachSkill(1321013, 1, 5, -1);
        cm.teachSkill(1321016, 1, 5, -1);
        cm.teachSkill(2121052, 1, 5, -1);
        cm.teachSkill(2121054, 1, 5, -1);
        cm.teachSkill(2221009, 1, 5, -1);
        cm.teachSkill(2221011, 1, 5, -1);
        cm.teachSkill(3121015, 1, 5, -1);
        cm.teachSkill(3221009, 1, 5, -1);
        cm.teachSkill(3221010, 1, 5, -1);
        cm.teachSkill(4121010, 1, 5, -1);
        cm.teachSkill(4221009, 1, 5, -1);
        cm.teachSkill(4221010, 1, 5, -1);
        cm.teachSkill(5121011, 1, 5, -1);
        cm.teachSkill(5121012, 1, 5, -1);
        cm.teachSkill(5121013, 1, 5, -1);
        cm.teachSkill(5221016, 1, 5, -1);
        cm.teachSkill(5221017, 1, 5, -1);
        cm.teachSkill(5221018, 1, 5, -1);
        cm.teachSkill(7001000, 1, 5, -1);
        cm.teachSkill(7001001, 1, 5, -1);
        cm.teachSkill(7001002, 1, 5, -1);
        cm.teachSkill(7001003, 1, 5, -1);
        cm.teachSkill(7001004, 1, 5, -1);
        cm.teachSkill(7001006, 1, 5, -1);
        cm.teachSkill(11121004, 1, 5, -1);
        cm.teachSkill(11121014, 1, 5, -1);
        cm.teachSkill(11121101, 1, 5, -1);
        cm.teachSkill(11121102, 1, 5, -1);
        cm.teachSkill(11121203, 1, 5, -1);
        cm.teachSkill(12121002, 1, 5, -1);
        cm.teachSkill(12121012, 1, 5, -1);
        cm.teachSkill(12121054, 1, 5, -1);
        cm.teachSkill(12121055, 1, 5, -1);
        cm.teachSkill(13121001, 1, 5, -1);
        cm.teachSkill(13121002, 1, 5, -1);
        cm.teachSkill(13121008, 1, 5, -1);
        cm.teachSkill(13121052, 1, 5, -1);
        cm.teachSkill(13121054, 1, 5, -1);
        cm.teachSkill(14121001, 1, 5, -1);
        cm.teachSkill(14121003, 1, 5, -1);
        cm.teachSkill(14121006, 1, 5, -1);
        cm.teachSkill(14121007, 1, 5, -1);
        cm.teachSkill(15121001, 1, 5, -1);
        cm.teachSkill(15121002, 1, 5, -1);
        cm.teachSkill(15121003, 1, 5, -1);
        cm.teachSkill(15121052, 1, 5, -1);





    }
}
cm.sendOk("Chin up, face straight, the path is yours to take.");
} else {
cm.sendOk("Ok, I understand. Come again later.");
}
cm.dispose();
}
}
}