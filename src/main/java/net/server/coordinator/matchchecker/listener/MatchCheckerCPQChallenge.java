/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

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
package net.server.coordinator.matchchecker.listener;

import client.Character;
import net.server.coordinator.matchchecker.AbstractMatchCheckerListener;
import net.server.coordinator.matchchecker.MatchCheckerListenerRecipe;
import net.server.world.PartyCharacter;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import server.partyquest.MonsterCarnival;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Ronan
 */
public class MatchCheckerCPQChallenge implements MatchCheckerListenerRecipe {

    public static AbstractMatchCheckerListener loadListener() {
        return (new MatchCheckerCPQChallenge()).getListener();
    }

    private static Character getChallenger(int leaderid, Set<Character> matchPlayers) {
        Character leader = null;
        for (Character chr : matchPlayers) {
            if (chr.getId() == leaderid && chr.getClient() != null) {
                leader = chr;
                break;
            }
        }

        return leader;
    }

    @Override
    public AbstractMatchCheckerListener getListener() {
        return new AbstractMatchCheckerListener() {

            @Override
            public void onMatchCreated(Character leader, Set<Character> nonLeaderMatchPlayers, String message) {
                NPCConversationManager cm = leader.getClient().getCM();
                int npcid = cm.getNpc();

                Character ldr = null;
                for (Character chr : nonLeaderMatchPlayers) {
                    ldr = chr;
                    break;
                }

                Character chr = leader;

                List<PartyCharacter> chrMembers = new LinkedList<>();
                for (PartyCharacter mpc : chr.getParty().getMembers()) {
                    if (mpc.isOnline()) {
                        chrMembers.add(mpc);
                    }
                }

                if (message.contentEquals("cpq1")) {
                    NPCScriptManager.getInstance().startCpqScript("cpqchallenge", ldr.getClient(), npcid, chrMembers);
                } else {
                    NPCScriptManager.getInstance().startCpqScript("cpqchallenge2", ldr.getClient(), npcid, chrMembers);
                }

                cm.sendOk("A challenge has been sent to the group in the room. Please wait a while.");
            }

            @Override
            public void onMatchAccepted(int leaderid, Set<Character> matchPlayers, String message) {
                Character chr = getChallenger(leaderid, matchPlayers);

                Character ldr = null;
                for (Character ch : matchPlayers) {
                    if (ch != chr) {
                        ldr = ch;
                        break;
                    }
                }

                if (message.contentEquals("cpq1")) {
                    ldr.getClient().getCM().startCPQ(chr, ldr.getMapId() + 1);
                } else {
                    ldr.getClient().getCM().startCPQ2(chr, ldr.getMapId() + 1);
                }

                ldr.getParty().setEnemy(chr.getParty());
                chr.getParty().setEnemy(ldr.getParty());
                chr.setChallenged(false);
            }

            @Override
            public void onMatchDeclined(int leaderid, Set<Character> matchPlayers, String message) {
                Character chr = getChallenger(leaderid, matchPlayers);
                chr.dropMessage(5, MonsterCarnival.CANCELED_CHALLENGE_TEXT);
            }

            @Override
            public void onMatchDismissed(int leaderid, Set<Character> matchPlayers, String message) {}
        };
    }
}
