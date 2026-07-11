// scripts/npc/9010002.js
var status = -1;
var maps = [
    [220080000, 105100100, 551030100, 541020700, 800040401, 211042400, 240050400], // Boss Maps
    [400000000, 401000000, 402000600, 410000000, 402000000, 410000416, 402000500, 410000200, 410000300], // Grandis
    [271010000, 273000000, 271030000, 450001000, 450002000, 450015060, 450003000, 450005000, 450006130, 450007040, 450016000, 450009100, 450011120, 450012300, 450011660] // Arcane River
    // Towns and Monster Maps are omitted from menu, but can be kept in the array if needed
];
var chosenMap = -1;
var chosenSection = -1;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === 1) {
        status++;
    } else {
        cm.dispose();
        return;
    }
    if (status === 0) {
        var menu = "Hi! Where would you like to go?\r\n\r\n";
        menu += "#b#L0#  #fMap/MapHelper.img/mark/Zakum#  Boss Maps#l\r\n\r\n";
        menu += "#b#L1#  #fMap/MapHelper.img/mark/Ristonia#  Grandis Towns#l\r\n\r\n";
        menu += "#b#L2#  #fMap/MapHelper.img/mark/Lacheln#  Arcane River & Future Towns#l";
        cm.sendSimple(menu);
    } else if (status == 1) {
        // Grandis and Arcane River require 3 rebirths
        if ((selection === 1 || selection === 2) && cm.getChar().getReborns() < 3) {
            cm.sendOk("#r                                            #fMap/MapHelper.img/mark/Lacheln# \r\n\r\nYou need to have at least 3 rebirths to access Grandis/Arcane River.");
            cm.dispose();
            return;
        }
        chosenSection = selection;
        showMapMenu(chosenSection);
    } else if (status == 2) {
        chosenMap = selection;
        cm.sendYesNo("Do you want to go to #m" + maps[chosenSection][chosenMap] + "#?");
    } else if (status == 3) {
        cm.warp(maps[chosenSection][chosenMap]);
        cm.dispose();
    }
}

function showMapMenu(section) {
    var icons = ["#fMap/MapHelper.img/mark/Zakum#", "#fMap/MapHelper.img/mark/Ristonia#", "#fMap/MapHelper.img/mark/Lacheln#"];
    var selStr = "                                          " + icons[section] + " #b";
    for (var i = 0; i < maps[section].length; i++) {
        selStr += "\r\n#L" + i + "##m" + maps[section][i] + "#";
    }
    cm.sendSimple(selStr);
}