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
/* The Forgotten Temple Manager
 *
 * Deep Place of Temple - Forgotten Twilight (270050000)
 * Vs Von Leon Recruiter NPC
 *
 * @author Ronan
 */

var status = 0;
var expedition;
var expedMembers;
var player;
var em;
const ExpeditionType = Java.type('server.expeditions.ExpeditionType');
var exped = ExpeditionType.NORMALLOTUS;
var expedName = "The Trial of Earth 65";
var expedBoss = "Lotus";
var expedMap = "The Trial of Earth 65";

var list = "What would you like to do?#b\r\n\r\n#L1#View current Expedition members#l\r\n#L2#Start the fight!#l\r\n#L3#Stop the expedition.#l";
var choice = "I am Lotus would you like to play?#b\r\n\r\n#L1#Fight Normal Lotus#l\r\n#L2#Fight Hard Lotus#l\r\n#L3#Maybe not.#l";

function start() {
    cm.sendNext("Looking for me?");
}

function action(mode, type, selection) {
    em = cm.getEventManager("NormalLotusBattle");
    player = cm.getPlayer();
    if (mode !== 1) {
        cm.sendOk("Let's play again sometime...");
        cm.dispose();
    } else {
        if (status == 0) {
            if (player.getLevel() < exped.getMinLevel() || player.getLevel() > exped.getMaxLevel()) { //Don't fit requirement, thanks Conrad
                cm.sendOk("You do not meet the criteria to battle " + expedBoss + "!");
                cm.dispose();
            } else if (expedition == null) { //Start an expedition
                cm.sendSimple("#e#b<Expedition: " + expedName + ">\r\n#k#n" + em.getProperty("party") + "\r\n\r\nWould you like to assemble a to try to take on the trial" + "#k?\r\n#b#L1#Normal Mode#l\r\n#L2#Hard Mode#l\r\n#L3#No, I think I'll wait a bit...#l");
                status = 8;
            } else if (expedition.isLeader(player)) { //If you're the leader, manage the exped
                if (expedition.isInProgress()) {
                    cm.sendOk("Your expedition is already in progress, for those who remain battling lets pray for those brave souls.");
                    cm.dispose();
                } else {
                    cm.sendSimple(list);
                    status = 3;
                }
            } else if (expedition.isRegistering()) { //If the expedition is registering
                if (expedition.contains(player)) { //If you're in it but it hasn't started, be patient
                    cm.sendOk("You have already registered for the expedition. Please wait for #r" + expedition.getLeader().getName() + "#k to begin it.");
                    cm.dispose();
                } else { //If you aren't in it, you're going to get added
                    cm.sendOk(expedition.addMember(cm.getPlayer()));
                    cm.dispose();
                }
            } else if (expedition.isInProgress()) { //Only if the expedition is in progress
                if (expedition.contains(player)) { //If you're registered, warp you in
                    var eim = em.getInstance(expedName + player.getClient().getChannel());
                    if (eim.getIntProperty("canJoin") == 1) {
                        eim.registerPlayer(player);
                    } else {
                        cm.sendOk("Your expedition already started the battle against " + expedBoss + ". Lets pray for those brave souls.");
                    }

                    cm.dispose();
                } else { //If you're not in by now, tough luck
                    cm.sendOk("Another expedition has taken the initiative to challenge " + expedBoss + ", lets pray for those brave souls.");
                    cm.dispose();
                }
            }
        } else if (status == 4) {
            if (selection == 1 || selection == 2) {
                expedition = cm.getExpedition(exped);
                if (expedition != null) {
                    cm.sendOk("Someone already taken the initiative to be the leader of the expedition. Try joining them!");
                    cm.dispose();
                    return;
                }

                var res = cm.createExpedition(exped);
                if (res == 0) {
                    cm.sendOk("The #r" + expedBoss + " Expedition#k has been created.\r\n\r\nTalk to me again to view the current team, or start the fight!");
                } else if (res > 0) {
                    cm.sendOk("Sorry, you've already reached the quota of attempts for this expedition! Try again another day...");
                } else {
                    cm.sendOk("An unexpected error has occurred when starting the expedition, please try again later.");
                }

                cm.dispose();

            } else if (selection == 3) {
                cm.sendOk("Sure, not everyone's up to challenging " + expedBoss + ".");
                cm.dispose();

            }
        } else if (status == 3) {
            if (selection == 1) {
                if (expedition == null) {
                    cm.sendOk("The expedition could not be loaded.");
                    cm.dispose();
                    return;
                }
                expedMembers = expedition.getMemberList();
                var size = expedMembers.size();
                if (size == 1) {
                    cm.sendOk("You are the only member of the expedition.");
                    cm.dispose();
                    return;
                }
                var text = "The following members make up your expedition (Click on them to expel them):\r\n";
                text += "\r\n\t\t1." + expedition.getLeader().getName();
                for (var i = 1; i < size; i++) {
                    text += "\r\n#b#L" + (i + 1) + "#" + (i + 1) + ". " + expedMembers.get(i).getValue() + "#l\n";
                }
                cm.sendSimple(text);
                status = 7;
            } else if (selection == 2) {
                var min = exped.getMinSize();

                var size = expedition.getMemberList().size();
                if (size < min) {
                    cm.sendOk("You need at least " + min + " players registered in your expedition.");
                    cm.dispose();
                    return;
                }

                cm.sendOk("The expedition will begin and you will now be escorted to the #b" + expedMap + "#k.");
                status = 4;
            } else if (selection == 3) {
                const PacketCreator = Java.type('tools.PacketCreator');
                player.getMap().broadcastMessage(PacketCreator.serverNotice(6, expedition.getLeader().getName() + " has ended the expedition."));
                cm.endExpedition(expedition);
                cm.sendOk("The expedition has now ended. Sometimes the best strategy is to run away.");
                cm.dispose();

            }
        } else if (status == 6) {
            if (em == null) {
                cm.sendOk("The event could not be initialized, please report this on the discord.");
                cm.dispose();
                return;
            }

            em.setProperty("leader", player.getName());
            em.setProperty("channel", player.getClient().getChannel());
            if (!em.startInstance(expedition)) {
                cm.sendOk("Another expedition has taken the initiative to challenge " + expedBoss + ", lets pray for those brave souls.");
                cm.dispose();
                return;
            }

            cm.dispose();

        } else if (status == 7) {
            if (selection > 0) {
                var banned = expedMembers.get(selection - 1);
                expedition.ban(banned);
                cm.sendOk("You have banned " + banned.getValue() + " from the expedition.");
                cm.dispose();
            } else {
                cm.sendSimple(list);
                status = 3;
            }
        } else if (status == 8) {
            if (selection == 1) {
                em = cm.getEventManager("NormalLotusBattle");
                exped = ExpeditionType.NORMALLOTUS;
                expedition = cm.getExpedition(exped);
                expedName = "The Trial of Earth 65 [NORMAL]"
                cm.sendSimple("You're sure?\r\n\r\n #L1#Yes#l\r\n #L2#No#l");
                status = 4;
            } else if (selection == 2) {
                em = cm.getEventManager("HardLotusBattle");
                exped = ExpeditionType.HARDLOTUS;
                expedition = cm.getExpedition(exped);
                expedName = "The Trial of Earth 65 [HARD]";
                cm.sendOk("As you wish.");
                status = 4;
            } else {
                cm.sendOk("Come back and play with me sometime...");
                cm.dispose();
            }
        }
    }
}