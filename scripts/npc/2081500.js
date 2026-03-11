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
/*
 *@Author:  Moogra
 *@NPC:     4th Job Pirate Advancement NPC
 *@Purpose: Handles 4th job.
 */

var status;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        if (status == 0) {
            if (cm.getLevel() < 120 || Math.floor(cm.getJobId() / 100) != 5) {
                cm.sendOk("Please don't bother me right now, I am trying to concentrate.");
                cm.dispose();
            } else if (!cm.isQuestCompleted(6944)) {
                cm.sendOk("You have not yet passed my trials. I can not advance you until you do so.");
                cm.dispose();
            } else if (cm.getJobId() % 10 == 1) {
                cm.sendYesNo("You did a marvellous job passing my test. Are you ready to advance to your 4th job?");
            } else if (cm.getJobId() % 10 == 2) {
                cm.sendSimple("If I must, I can teach you the art of your class.\r\n#b#L0#Teach me the skills of my class.#l");
            } else {
                cm.sendOk("You must be a 3rd job advancement to advance to 4th job.");
                cm.dispose();
            }
        } else if (status == 1) {
            if (mode == 1 && cm.getJobId() % 10 == 1) {
                if (cm.canHold(2280003, 1)) {
                    cm.changeJobById(cm.getJobId() + 1);
                    cm.gainItem(2280003, 1);
                    cm.sendOk("You have successfully advanced to 4th job. Talk to me again to learn your skills.");
                } else {
                    cm.sendOk("Please have one slot available on #bUSE#k inventory to receive a skill book.");
                }
            } else if (mode == 1 && cm.getJobId() % 10 == 2) {
                var skills = [];
                var hwSkill = 0;
                if (cm.getJobId() == 512) {
                    skills = [5121000, 5121001, 5121002, 5121003, 5121004, 5121005, 5121007, 5121009, 5121010];
                    hwSkill = 5121008;
                } else if (cm.getJobId() == 522) {
                    skills = [5221000, 5220001, 5220002, 5221003, 5221004, 5221006, 5221007, 5221008, 5221009, 5221010, 5220011];
                    hwSkill = 5221005;
                }

                for (var i = 0; i < skills.length; i++) {
                    if (cm.getPlayer().getSkillLevel(skills[i]) <= 0) {
                        cm.teachSkill(skills[i], 0, 10, -1);
                    }
                }
                if (hwSkill != 0 && cm.getPlayer().getSkillLevel(hwSkill) <= 0) {
                    cm.teachSkill(hwSkill, 1, 5, -1);
                }
                cm.sendOk("It is done. Leave me now.");
            }
            cm.dispose();
        }
    }
}
