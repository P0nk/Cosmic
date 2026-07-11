var jumpquests = Array(105040311, 105040313, 105040316, 280020000,); // Hidden Street - The Deep Forest of Patience <Step 2>, Hidden Street - The Deep Forest of Patience <Step 4>, Hidden Street - The Forest of Patience <Step 2>, Hidden Street - The Deep Forest of Patience <Step 7>
var chosenMap = -1;
var quests = 0;


function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    }
    else {
        if (status >= 3 && mode == 0) {
            cm.sendOk("See you next time!.");
            cm.dispose();
            return;    
        }
        if (mode == 1) {
            status++;
        }
        else {
            status--;
        }
        if (status == 0) {
            cm.sendNext("Hello, I am the MapleRoot's Jump Quest NPC. Once you have completed the Jump Quest, talk to the NPC at the end for a reward!");
        }
        if (status == 1) {
            cm.sendSimple("#fUI/UIWindow.img/QuestIcon/3/0#\r\n#L0#View Jump Quests#l");
        }
        else if (status == 2) {
            if (selection == 0) {
                var selStr = "Select your Jump Quest.#b";
                for (var i = 0; i < jumpquests.length; i++) {
                    selStr += "\r\n#L" + i + "##m" + jumpquests[i] + "#";
                }
                cm.sendSimple(selStr);
                quests = 1;
 
            }
        }
        else if (status == 3) {
            if (quests == 1) {
                cm.sendYesNo("Do you want to go to #m" + jumpquests[selection] + "#?");
                chosenMap = selection;
                quests = 2;
            }
        }
        else if (status == 4) {
            if (quests == 2) {
                cm.warp(jumpquests[chosenMap], 0);
                cm.dispose();
            }
        }
              
    }
}