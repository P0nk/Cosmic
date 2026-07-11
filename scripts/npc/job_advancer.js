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
 * @Name: Robin
 * @NPC ID: 2003
 * @Author: ???
 */

var status = -1;
var possibleJobs = [];
var job;
var newJob;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (status == 0 && mode == 0) {
            cm.sendOk("See you next time!"); // You can Put custom Msg Here
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }
        if (status == 0) {
            newJob = cm.getJobId() + 1;
            if (cm.getJobId() % 10 == 2) {
                cm.sendOk("You don't meet the requirements for a new job. Get back when you're ready!");
                cm.dispose();
            } else if (cm.getJobId() == 900 || cm.getJobId() == 910) {
                cm.dispose();
            } else if (cm.getJobId() % 10 >= 0 && cm.getJobId() % 100 != 0) {
                var secondJob = cm.getJobId() % 10 == 0;
                if ((secondJob && cm.getLevel() < 70) || (!secondJob && cm.getLevel() < 120)) {
                    cm.sendOk("You don't meet the requirements for a new job. Get back when you're ready!");
                    cm.dispose();
                } else
                    cm.sendYesNo("Great job getting to level " + cm.getLevel() + ". Would you like to become a #b" + cm.getJobName(newJob) + "#k ?");
            } else {
                if (cm.getJobId() % 1000 == 0) {
                    if (cm.getLevel() >= 9)
                        for (var i = 1; i < 6; i++)
                            possibleJobs.push(cm.getJobId() + 100 * i);
                    else if (cm.getLevel() >= 8)
                        possibleJobs.push(cm.getJobId() + 200);
                } else if (cm.getLevel() >= 29) {
                    switch (cm.getJobId()) {
                        case 100:
                        case 200:
                            possibleJobs.push(cm.getJobId() + 30);
                        case 300:
                        case 400:
                        case 500:
                            possibleJobs.push(cm.getJobId() + 20);
                        case 1100:
                        case 1200:
                        case 1300:
                        case 1400:
                        case 1500:
                        case 2100:
                            possibleJobs.push(cm.getJobId() + 10);
                            break;
                    }
                }
                if (possibleJobs.length == 0) {
                    cm.sendOk("You don't meet the requirements for a new job. Get back when you're ready!");
                    cm.dispose();
                } else {
                    var text = "These are the available jobs you can take#b";
                    for (var j = 0; j < possibleJobs.length; j++)
                        text += "\r\n#L" + j + "#" + cm.getJobName(possibleJobs[j]) + "#l";
                    cm.sendSimple(text);
                }
            }
        } else if (status == 1 && cm.getJobId() % 100 != 0) {
            cm.changeJobById(cm.getJobId() + 1);
            cm.maxMastery();
            cm.dispose();
        } else if (status == 1) {
            cm.changeJobById(possibleJobs[selection]);
            cm.resetStats();
            if (cm.getJobId() % 10 == 0)
                cm.dispose();
        } else if (status == 2) {
            job = selection;
            cm.sendYesNo("Are you sure you want to job advance?");
        } else if (status == 3) {
            var jobid = possibleJobs[job];
            cm.changeJobById(jobid);
            cm.dispose();
        }
    }
}