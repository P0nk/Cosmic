/*
        This file is part of the OdinMS Maple Story Server
        Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
        Matthias Butz <matze@odinms.de>
        Jan Christian Meyer <vimes@odinms.de>

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU Affero General Public License version 3
        as published by the Free Software Foundation. You may not use, modify
        or distribute this program under any other version of the
        GNU Affero General Public License.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU Affero General Public License for more details.

        You should have received a copy of the GNU Affero General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/* Bowman Job Instructor
    Hunter Job Advancement
    Warning Street : The Road to the Dungeon (106010000)
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
        if (mode == 0 && type > 0) {
            cm.dispose();
            return;
        }
        if (mode == 1) {
            status++;
        } else {
            status--;
        }

        // -------------------------------------------------------------
        // REBIRTH PATH
        // If player is a Bowman (300) and has Rebirths, we allow re-entry
        // regardless of previous quest completion status.
        // -------------------------------------------------------------
       if (cm.haveItem(4031012, 1)){
             if (status == 0) {
                        cm.sendNext("Well done great bowman! Now return to Athena to complete your job advancement!");
                   } else if (status == 1) {
                        cm.sendAcceptDecline("Would you like me to send you back to Athena?");
                   } else if (status == 2) {
                        // Warp directly to the Bowman Test Map (108000100)
                        cm.warp(100000201, 0);
                        cm.dispose();
                   }
                   return;
               }

        if (cm.getPlayer().getReborns() > 0 && cm.getJob().getId() == 300 && cm.haveItem(4031010)) {

            if (status == 0) {
                 cm.sendNext("Oh! Its you again! I didn't think I'd see you again! I see the sharp eyes of a veteran bowman in you. You must have been reborn, I'll let my colleague check and see if you have the skills still..");
            } else if (status == 1) {
                 cm.sendAcceptDecline("I see Athena has sent you here once more, I will send you to the testing grounds. Are you ready?");
            } else if (status == 2) {
                  cm.gainItem(4031010, -1);
                 // Warp directly to the Bowman Test Map (108000100)
                 cm.warp(108000100, 0);
                 cm.dispose();
            }
            return;
        }

        // -------------------------------------------------------------
        // STANDARD PATH (First-time players)
        // -------------------------------------------------------------
        if (status == 0) {
            if (cm.isQuestCompleted(100001)) {
                cm.sendOk("You're truly a hero!");
                cm.dispose();
            } else if (cm.isQuestCompleted(100000)) {
                cm.sendNext("Alright I'll let you in! Defeat the monsters inside, collect 30 Dark Marbles, then strike up a conversation with a colleague of mine inside. He'll give you #bThe Proof of a Hero#k, the proof that you've passed the test. Best of luck to you.");
                status = 3;
            } else if (cm.isQuestStarted(100000)) {
                cm.sendNext("Oh, isn't this a letter from #bAthena#k?");
            } else {
                cm.sendOk("I can show you the way once your ready for it.");
                cm.dispose();
            }
        } else if (status == 1) {
            cm.sendNextPrev("So you want to prove your skills? Very well...");
        } else if (status == 2) {
            cm.sendAcceptDecline("I will give you a chance if you're ready.");
        } else if (status == 3) {
            // Only update quest logic if standard path
            if (!cm.isQuestCompleted(100000)) {
                cm.completeQuest(100000);
                cm.startQuest(100001);
                cm.gainItem(4031010, -1); // Remove Letter from Athena
            }
            cm.sendOk("You will have to collect me #b30 #t4031013##k. Good luck.");
        } else if (status == 4) {
            cm.warp(108000100, 0);
            cm.dispose();
        } else {
            cm.dispose();
        }
    }
}