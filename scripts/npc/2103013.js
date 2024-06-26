/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
var status = -1;
var selected = -1;
var str = "";
var solo = true;

const ASK_ABOUT_THE_PYRAMID = 0;
const ENTER_THE_PYRAMID = 1;
const ENTER_PHARAOH_YETIS_TOMB = 2;
const ASK_ABOUT_PHARAOH_YETIS_TREASURE = 3;
const RECEIVE_THE_PHARAOH_MEDAL = 4;

const DIFFICULTY_EASY = 0;
const DIFFICULTY_NORMAL = 1;
const DIFFICULTY_HARD = 2;
const DIFFICULTY_HELL = 3;

const PROTECTOR_OF_PHARAOH_QUEST = 29932;
const PROTECTOR_OF_PHARAOH_MEDAL = 1142142;
const PYRAMID_DUNES = 926010000;
const NETTS_PYRAMID_END = 926010001;
const SHADES_OF_THE_PYRAMID = 926020001;

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    switch (cm.getPlayer().getMap().getId()) {
        case PYRAMID_DUNES:
            pyramidDunesAction(mode, type, selection);
            break;
        case NETTS_PYRAMID_END:
            pyramidEndAction(mode, type, selection);
            break;
        case SHADES_OF_THE_PYRAMID:
            pyramidBonusAction(mode, type, selection);
            break;
        default:
            pyramidForfeitAction(mode, type, selection);
            break;
    }
}

function pyramidDunesAction(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
    } else {
        if (mode === 0 && type > 0) {
            cm.dispose();
            return;
        }
        if (mode === 1) {
            status++;
        } else {
            status--;
        }

        if (status === 0) {
            str = "I am Duarte.\r\n\r\n#b"
            str += "#L" + ASK_ABOUT_THE_PYRAMID + "#Ask about the Pyramid#l\r\n";
            str += "#L" + ENTER_THE_PYRAMID + "##eEnter the Pyramid#n#l\r\n\r\n";
            str += "#L" + ENTER_PHARAOH_YETIS_TOMB + "#Enter Pharaoh Yeti's Tomb#l\r\n";
            str += "#L" + ASK_ABOUT_PHARAOH_YETIS_TREASURE + "#Ask about Pharaoh Yeti's treasures#l\r\n";
            str += "#L" + RECEIVE_THE_PHARAOH_MEDAL + "#Receive the <Protector of Pharaoh> Medal#l\r\n";
            cm.sendSimple(str);
        } else if (status === 1) {
            if (selection !== -1) {
                selected = selection;
            }
            switch (selected) {
                case ASK_ABOUT_THE_PYRAMID:
                    str = "This is the pyramid of Nett, the god of chaos and revenge. For a long time, it was buried deep in the desert, ";
                    str += "but Nett has ordered it to rise above ground. If you are unafraid of chaos and possible death, you may challenge ";
                    str += "Pharaoh Yeti, who lies asleep in side the Pyramid. Whatever the outcome, the choice is yours to make.";
                    cm.sendNext(str);
                    break;
                case ENTER_THE_PYRAMID:
                    str = "You fools who know no fear of Nett's wrath, it is now time to choose your destiny!\r\n\r\n#b";
                    str += "#L0#Enter alone#l\r\n";
                    str += "#L1#Enter with a party of 2 or more";
                    cm.sendSimple(str);
                    break;
                case ENTER_PHARAOH_YETIS_TOMB:
                    str = "What gem have you brought?\r\n\r\n";
                    str += "#L0##i4001322# #z4001322##l\r\n";
                    str += "#L1##i4001323# #z4001323##l\r\n";
                    str += "#L2##i4001324# #z4001324##l\r\n";
                    str += "#L3##i4001325# #z4001325##l\r\n";
                    cm.sendSimple(str);
                    break;
                case ASK_ABOUT_PHARAOH_YETIS_TREASURE:
                    cm.sendNext("Inside Pharaoh Yeti's Tomb, you can acquire a #e#b#z2022613##k#n by proving yourself capable of defeating the #bPharaoh Jr. Yeti#k, the Pharaoh's clone. Inside that box lies a very special treasure. It is the #e#b#t1132012##k#n.\r\n#i1132012:# #z1132012#\r\n\r\nAnd if you are somehow able to survive Hell Mode, you will receive the #e#b#t1132013##k#n.\r\n\r\n#i1132013:# #z1132013#\r\n\r\nThough, of course, Nett won't allow that to happen.");
                    cm.dispose();
                    break;
                case RECEIVE_THE_PHARAOH_MEDAL:
                    // var progress = Number(cm.getPlayer().getQuest(PROTECTOR_OF_PHARAOH_QUEST).getProgress(7760));
                    //
                    // if (progress >= 50000) {
                    //     cm.sendYesNo("It would seem that you are deserving of the #b<Protector of Pharaoh>#k Medal. You have eliminated a total of #b" + progress + "#k monsters in Nett's Pyramid thus far. Would you like to receive the #bProtector of Pharaoh Medal#k?");
                    // } else {
                    //     cm.sendNext("You don't deserve to be called a Protector of Pharaoh. You must eliminate over #b50,000#k monsters.\r\n\r\n#bThe total number of monsters that you have eliminated thus far is " + progress);
                    //     cm.dispose();
                    // }
                    cm.sendNext("Not yet implemented");
                    cm.dispose();
                    break;
            }
        } else if (status === 2) {
            switch (selected) {
                case ASK_ABOUT_THE_PYRAMID:
                    str = "Once you enter the Pyramid, you will be faced with the wrath of Nett. Since you don't look too sharp, I will offer ";
                    str += "you some advice and rules to follow. Remember them well.\r\n\r\n";
                    str += "#b1. Be careful that your #r#eAct Gauage#n#b does not decrease. The only way to maintain your Gauage level is to battle the monsters without stopping.\r\n";
                    str += "2. Those who are unable will pay dearly. Be careful to not cause any #rMiss#b.\r\n";
                    str += "3. Be wary of the Pharaoh Jr. Yeti with the #v04032424# mark. Make the mistake of attacking him and you will regret it.\r\n";
                    str += "4. Be wise about using the skill that is given to you for Kill accomplishments.";
                    cm.sendNextPrev(str);
                    break;
                case ENTER_THE_PYRAMID:
                    if (selection === 1) {
                        if (cm.getParty() == null || cm.getParty().getMembers().size() < 2 || cm.getParty().getMembers().size() > 4) {
                            // TODO: Get the GMS-like text for this
                            cm.sendOk("You are not currently in a party with between 2 and 4 members.");
                            cm.dispose();
                            return;
                        }
                        solo = false;
                    }
                    str = "You who lack fear of death's cruelty, make your decision!\r\n\r\n";
                    str += "#L" + DIFFICULTY_EASY + "##i3994115##l";
                    str += "#L" + DIFFICULTY_NORMAL + "##i3994116##l";
                    str += "#L" + DIFFICULTY_HARD + "##i3994117##l";
                    str += "#L" + DIFFICULTY_HELL + "##i3994118##l";
                    cm.sendSimple(str);
                    break;
                case ENTER_PHARAOH_YETIS_TOMB:
                    var requiredId = 4001322 + selection;
                    if (cm.haveItem(requiredId)) {
                        if (!cm.getPlayer().startPyramidBonus(selection)) {
                            cm.sendOk("Something went wrong");
                        } else {
                            cm.gainItem(requiredId, -1);
                        }
                    } else {
                        // TODO: Find GMS-like text for this
                        cm.sendOk("You'll need a gem to enter Pharaoh Yeti's Tomb. Are you sure you have one?");
                    }
                    cm.dispose();
                    break;
                case RECEIVE_THE_PHARAOH_MEDAL:
                    if (!cm.canHold(PROTECTOR_OF_PHARAOH_MEDAL, 1)) {
                        cm.sendNext("Please make sure you have enough space in your Equip inventory.");
                        cm.dispose();
                        return;
                    }
                    cm.gainItem(PROTECTOR_OF_PHARAOH_MEDAL, 1, true, true);
                    cm.earnTitle("You've acquired the <Protector of Pharaoh> title");
                    cm.completeQuest(PROTECTOR_OF_PHARAOH_QUEST, 9000066);
                    cm.dispose();
                    break;
                default:
                    cm.dispose();
            }
        } else if (status === 3) {
            switch (selected) {
                case ASK_ABOUT_THE_PYRAMID:
                    cm.sendNextPrev("Those who are able to withstand Nett's wrath will be honored, but those who fail will face destruction. This is all the advice I can give you. The rest is in your hands.");
                    break;
                case ENTER_THE_PYRAMID:
                    const PyramidProcessor = Java.type('server.partyquest.pyramid.PyramidProcessor');
                    var difficultyId = selection;
                    var pyramid;

                    if (solo) pyramid = PyramidProcessor.createSoloPyramidInstance(cm.getPlayer(), difficultyId);
                    else pyramid = PyramidProcessor.createPartyPyramidInstance(cm.getParty(), difficultyId);

                    // Check to make sure all characters are in the entrance map
                    if (!pyramid.checkCharactersArePresent()) {
                        cm.sendOk("Please make sure all party members are present in the map before starting the Party Quest.");
                        cm.dispose();
                        return;
                    }

                    // Check to make sure all characters are of the right level
                    if (!pyramid.checkCharacterLevels()) {
                        cm.sendOk("You or one of your party members are not in the correct level range for this difficulty. You must be between #bLv. " + pyramid.getMinLevel() + " - " + pyramid.getMaxLevel() + "#k.");
                        cm.dispose();
                        return;
                    }

                    pyramid.start();
                    cm.dispose();
                    break;
                default:
                    cm.dispose();
                    break;
            }
        } else if (status === 4) {
            switch (selected) {
                case ASK_ABOUT_THE_PYRAMID:
                    cm.dispose();
                    break;
                default:
                    cm.dispose();
                    break;
            }
        }
    }
}

function pyramidEndAction(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
    } else {
        if (mode === 0 && type > 0) {
            cm.dispose();
            return;
        }
        if (mode === 1) {
            status++;
        } else {
            status--;
        }

        if (status === 0) {
            str = "Your allotted time has passed. Do you want to leave now?\r\n\r\n#b";
            str += "#L0#Leave#l";
            cm.sendSimple(str);
        } else if (status === 1) {
            cm.warp(SHADES_OF_THE_PYRAMID);
            cm.dispose();
        }
    }
}

function pyramidBonusAction(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
    } else {
        if (mode === 0 && type > 0) {
            cm.dispose();
            return;
        }
        if (mode === 1) {
            status++;
        } else {
            status--;
        }

        var pyramidCharacterStats = cm.getPlayer().getPyramidCharacterStats();
        if (pyramidCharacterStats == null || pyramidCharacterStats.getRank().getCode() === 4) {
            cm.warp(PYRAMID_DUNES);
            cm.dispose();
            return;
        }

        var difficulty = cm.getPlayer().getPyramidCharacterStats().getDifficulty().getMode();
        if (status === 0) {
            str = "Stop! You've successfully passed Nett's test. By Nett's grace, you will now be given the opportunity to enter Pharaoh Yeti's Tomb. Do you wish to enter it now?\r\n\r\n#b";
            str += "#L0#Yes, I will go now#l\r\n";
            str += "#L1#No, I will go later#l\r\n";
            cm.sendSimple(str);
        } else if (status === 1) {
            if (selection === 0) {
                if (!cm.getPlayer().startPyramidBonus(difficulty)) {
                    cm.sendOk("Something went wrong..");
                }
                cm.dispose();
            } else if (selection === 1) {
                cm.sendNext("I will give you Pharaoh Yeti's Gem. You will be able to enter Pharaoh Yeti's Tomb anytime with this Gem. Check to see if you have at least 1 empty slot in your Etc window.");
            }
        } else if (status === 2) {
            var jewelId = -1;
            switch (difficulty) {
                case 0: // EASY
                    jewelId = 4001322;
                    break;
                case 1: // NORMAL
                    jewelId = 4001323;
                    break;
                case 2: // HARD
                    jewelId = 4001324;
                    break;
                case 3: // HELL
                    jewelId = 4001325;
                    break;
            }

            if (!cm.canHold(jewelId, 1)) {
                cm.sendOk("Please make sure you have enough space in your Etc inventory.");
                cm.dispose();
                return;
            }
            cm.getPlayer().setPyramidCharacterStats(null);
            cm.warp(PYRAMID_DUNES);
            cm.gainItem(jewelId, 1);
            cm.dispose();
        }
    }
}

function pyramidForfeitAction(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
    } else {
        if (mode === 0 && type > 0) {
            cm.dispose();
            return;
        }
        if (mode === 1) {
            status++;
        } else {
            status--;
        }

        const PyramidProcessor = Java.type('server.partyquest.pyramid.PyramidProcessor');
        var pyramid = PyramidProcessor.getPyramidForCharacter(cm.getPlayer().getId());

        if (pyramid == null) {
            cm.warp(PYRAMID_DUNES);
            cm.dispose();
            return;
        }

        if (status === 0) {
            str = "Do you want to forfeit the challenge and leave?\r\n\r\n#b";
            str += "#L0# Leave#l\r\n";
            cm.sendSimple(str);
        } else if (status === 1) {
            pyramid.forfeitChallenge(cm.getPlayer());
            cm.dispose();
        }
    }
}