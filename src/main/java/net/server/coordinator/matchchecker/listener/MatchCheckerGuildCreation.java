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
import config.YamlConfig;
import constants.game.GameConstants;
import constants.id.MapId;
import net.packet.Packet;
import net.server.Server;
import net.server.coordinator.matchchecker.AbstractMatchCheckerListener;
import net.server.coordinator.matchchecker.MatchCheckerListenerRecipe;
import net.server.guild.Guild;
import net.server.guild.GuildCharacter;
import net.server.guild.GuildPackets;
import net.server.world.Party;

import java.util.Set;

/**
 * @author Ronan
 */
public class MatchCheckerGuildCreation implements MatchCheckerListenerRecipe {

    private static void broadcastGuildCreationDismiss(Set<Character> nonLeaderMatchPlayers) {
        for (Character chr : nonLeaderMatchPlayers) {
            if (chr.isLoggedinWorld()) {
                chr.sendPacket(GuildPackets.genericGuildMessage((byte) 0x26));
            }
        }
    }

    public static AbstractMatchCheckerListener loadListener() {
        return (new MatchCheckerGuildCreation()).getListener();
    }

    @Override
    public AbstractMatchCheckerListener getListener() {
        return new AbstractMatchCheckerListener() {

            @Override
            public void onMatchCreated(Character leader, Set<Character> nonLeaderMatchPlayers, String message) {
                Packet createGuildPacket = GuildPackets.createGuildMessage(leader.getName(), message);

                for (Character chr : nonLeaderMatchPlayers) {
                    if (chr.isLoggedinWorld()) {
                        chr.sendPacket(createGuildPacket);
                    }
                }
            }

            @Override
            public void onMatchAccepted(int leaderid, Set<Character> matchPlayers, String message) {
                Character leader = null;
                for (Character chr : matchPlayers) {
                    if (chr.getId() == leaderid) {
                        leader = chr;
                        break;
                    }
                }

                if (leader == null || !leader.isLoggedinWorld()) {
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                matchPlayers.remove(leader);

                if (leader.getGuildId() > 0) {
                    leader.dropMessage(1, "You cannot create a new Guild while in one.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                int partyid = leader.getPartyId();
                if (partyid == -1 || !leader.isPartyLeader()) {
                    leader.dropMessage(1, "You cannot establish the creation of a new Guild without leading a party.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                if (leader.getMapId() != MapId.GUILD_HQ) {
                    leader.dropMessage(1,
                            "You cannot establish the creation of a new Guild outside of the Guild Headquarters.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                for (Character chr : matchPlayers) {
                    if (leader.getMap().getCharacterById(chr.getId()) == null) {
                        leader.dropMessage(1,
                                "You cannot establish the creation of a new Guild if one of the members is not present here.");
                        broadcastGuildCreationDismiss(matchPlayers);
                        return;
                    }
                }
                if (leader.getMeso() < YamlConfig.config.server.CREATE_GUILD_COST) {
                    leader.dropMessage(1,
                            "You do not have "
                                    + GameConstants.numberWithCommas(YamlConfig.config.server.CREATE_GUILD_COST)
                                    + " mesos to create a Guild.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }

                int gid = Server.getInstance().createGuild(leader.getId(), message);
                if (gid == 0) {
                    leader.sendPacket(GuildPackets.genericGuildMessage((byte) 0x23));
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                leader.gainMeso(-YamlConfig.config.server.CREATE_GUILD_COST, true, false, true);

                leader.getMGC().setGuildId(gid);
                Guild guild = Server.getInstance().getGuild(leader.getGuildId(), leader.getWorld(), leader); // initialize
                                                                                                             // guild
                                                                                                             // structure
                Server.getInstance().changeRank(gid, leader.getId(), 1);

                leader.sendPacket(GuildPackets.showGuildInfo(leader));
                leader.dropMessage(1, "You have successfully created a Guild.");
                leader.saveGuildStatus(); // Persist rank 1 immediately

                for (Character chr : matchPlayers) {
                    boolean cofounder = chr.getPartyId() == partyid;

                    GuildCharacter mgc = chr.getMGC();
                    mgc.setGuildId(gid);
                    mgc.setGuildRank(cofounder ? 2 : 5);
                    mgc.setAllianceRank(5);

                    Server.getInstance().addGuildMember(mgc, chr);

                    if (chr.isLoggedinWorld()) {
                        chr.sendPacket(GuildPackets.showGuildInfo(chr));

                        if (cofounder) {
                            chr.dropMessage(1, "You have successfully cofounded a Guild.");
                        } else {
                            chr.dropMessage(1, "You have successfully joined the new Guild.");
                        }
                    }

                    chr.saveGuildStatus(); // update database
                }

                guild.broadcastNameChanged();
                guild.broadcastEmblemChanged();
            }

            @Override
            public void onMatchDeclined(int leaderid, Set<Character> matchPlayers, String message) {
                for (Character chr : matchPlayers) {
                    if (chr.getId() == leaderid && chr.getClient() != null) {
                        Party.leaveParty(chr.getParty(), chr.getClient());
                    }

                    if (chr.isLoggedinWorld()) {
                        chr.sendPacket(GuildPackets.genericGuildMessage((byte) 0x26));
                    }
                }
            }

            @Override
            public void onMatchDismissed(int leaderid, Set<Character> matchPlayers, String message) {

                Character leader = null;
                for (Character chr : matchPlayers) {
                    if (chr.getId() == leaderid) {
                        leader = chr;
                        break;
                    }
                }

                String msg;
                if (leader != null && leader.getParty() == null) {
                    msg = "The Guild creation has been dismissed since the leader left the founding party.";
                } else {
                    msg = "The Guild creation has been dismissed since a member was already in a party when they answered.";
                }

                for (Character chr : matchPlayers) {
                    if (chr.getId() == leaderid && chr.getClient() != null) {
                        Party.leaveParty(chr.getParty(), chr.getClient());
                    }

                    if (chr.isLoggedinWorld()) {
                        chr.message(msg);
                        chr.sendPacket(GuildPackets.genericGuildMessage((byte) 0x26));
                    }
                }
            }
        };
    }
}
