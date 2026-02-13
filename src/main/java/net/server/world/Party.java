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
package net.server.world;

import client.Character;
import client.Client;
import config.YamlConfig;
import net.server.coordinator.matchchecker.MatchCheckerCoordinator;
import net.server.coordinator.matchchecker.MatchCheckerListenerFactory.MatchCheckerType;
import scripting.event.EventInstanceManager;
import server.maps.Door;
import server.maps.MapleMap;
import server.partyquest.MonsterCarnival;
import tools.PacketCreator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Party {

    private final int id;
    private Party enemy = null;
    private int leaderId;
    private final List<PartyCharacter> members = new LinkedList<>();
    private List<PartyCharacter> pqMembers = null;

    private final Map<Integer, Integer> histMembers = new HashMap<>();
    private int nextEntry = 0;

    private final Map<Integer, Door> doors = new HashMap<>();

    private final Lock lock = new ReentrantLock(true);

    public Party(int id, PartyCharacter chrfor) {
        this.leaderId = chrfor.getId();
        this.id = id;
    }

    public boolean containsMembers(PartyCharacter member) {
        lock.lock();
        try {
            return members.contains(member);
        } finally {
            lock.unlock();
        }
    }

    public void addMember(PartyCharacter member) {
        lock.lock();
        try {
            histMembers.put(member.getId(), nextEntry);
            nextEntry++;

            members.add(member);
        } finally {
            lock.unlock();
        }
    }

    public void removeMember(PartyCharacter member) {
        lock.lock();
        try {
            histMembers.remove(member.getId());

            members.remove(member);
        } finally {
            lock.unlock();
        }
    }

    public void setLeader(PartyCharacter victim) {
        this.leaderId = victim.getId();
    }

    public void updateMember(PartyCharacter member) {
        lock.lock();
        try {
            for (int i = 0; i < members.size(); i++) {
                if (members.get(i).getId() == member.getId()) {
                    members.set(i, member);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public PartyCharacter getMemberById(int id) {
        lock.lock();
        try {
            for (PartyCharacter chr : members) {
                if (chr.getId() == id) {
                    return chr;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public Collection<PartyCharacter> getMembers() {
        lock.lock();
        try {
            return new LinkedList<>(members);
        } finally {
            lock.unlock();
        }
    }

    public List<PartyCharacter> getPartyMembers() {
        lock.lock();
        try {
            return new LinkedList<>(members);
        } finally {
            lock.unlock();
        }
    }

    public List<PartyCharacter> getPartyMembersOnline() {
        lock.lock();
        try {
            List<PartyCharacter> ret = new LinkedList<>();

            for (PartyCharacter mpc : members) {
                if (mpc.isOnline()) {
                    ret.add(mpc);
                }
            }

            return ret;
        } finally {
            lock.unlock();
        }
    }

    // used whenever entering PQs: will draw every party member that can attempt a target PQ while ingnoring those unfit.
    public Collection<PartyCharacter> getEligibleMembers() {
        return Collections.unmodifiableList(pqMembers);
    }

    public void setEligibleMembers(List<PartyCharacter> eliParty) {
        pqMembers = eliParty;
    }

    public int getId() {
        return id;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public PartyCharacter getLeader() {
        lock.lock();
        try {
            for (PartyCharacter mpc : members) {
                if (mpc.getId() == leaderId) {
                    return mpc;
                }
            }

            return null;
        } finally {
            lock.unlock();
        }
    }

    public Party getEnemy() {
        return enemy;
    }

    public void setEnemy(Party enemy) {
        this.enemy = enemy;
    }

    public List<Integer> getMembersSortedByHistory() {
        List<Entry<Integer, Integer>> histList;

        lock.lock();
        try {
            histList = new LinkedList<>(histMembers.entrySet());
        } finally {
            lock.unlock();
        }

        histList.sort((o1, o2) -> (o1.getValue()).compareTo(o2.getValue()));

        List<Integer> histSort = new LinkedList<>();
        for (Entry<Integer, Integer> e : histList) {
            histSort.add(e.getKey());
        }

        return histSort;
    }

    public byte getPartyDoor(int cid) {
        List<Integer> histList = getMembersSortedByHistory();
        byte slot = 0;
        for (Integer e : histList) {
            if (e == cid) {
                break;
            }
            slot++;
        }

        return slot;
    }

    public void addDoor(Integer owner, Door door) {
        lock.lock();
        try {
            this.doors.put(owner, door);
        } finally {
            lock.unlock();
        }
    }

    public void removeDoor(Integer owner) {
        lock.lock();
        try {
            this.doors.remove(owner);
        } finally {
            lock.unlock();
        }
    }

    public Map<Integer, Door> getDoors() {
        lock.lock();
        try {
            return Collections.unmodifiableMap(doors);
        } finally {
            lock.unlock();
        }
    }

    public void assignNewLeader(Client c) {
        World world = c.getWorldServer();
        PartyCharacter newLeadr = null;

        lock.lock();
        try {
            for (PartyCharacter mpc : members) {
                if (mpc.getId() != leaderId && (newLeadr == null || newLeadr.getLevel() < mpc.getLevel())) {
                    newLeadr = mpc;
                }
            }
        } finally {
            lock.unlock();
        }

        if (newLeadr != null) {
            world.updateParty(this.getId(), PartyOperation.CHANGE_LEADER, newLeadr);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    public PartyCharacter getMemberByPos(int pos) {
        int i = 0;
        for (PartyCharacter chr : members) {
            if (pos == i) {
                return chr;
            }
            i++;
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Party other = (Party) obj;
        return id == other.id;
    }

    public static boolean createParty(Character player, boolean silentCheck) {
        Party party = player.getParty();
        if (party == null) {
            if (player.getLevel() < 10 && !YamlConfig.config.server.USE_PARTY_FOR_STARTERS) {
                player.sendPacket(PacketCreator.partyStatusMessage(10));
                return false;
            } else if (player.getAriantColiseum() != null) {
                player.dropMessage(5, "You cannot request a party creation while participating the Ariant Battle Arena.");
                return false;
            }

            PartyCharacter partyplayer = new PartyCharacter(player);
            party = player.getWorldServer().createParty(partyplayer);
            player.setParty(party);
            player.setMPC(partyplayer);
            player.getMap().addPartyMember(player, party.getId());
            player.silentPartyUpdate();

            player.updatePartySearchAvailability(false);
            player.partyOperationUpdate(party, null);

            player.sendPacket(PacketCreator.partyCreated(party, partyplayer.getId()));

            return true;
        } else {
            if (!silentCheck) {
                player.sendPacket(PacketCreator.partyStatusMessage(16));
            }

            return false;
        }
    }

    public static boolean joinParty(Character player, int partyid, boolean silentCheck) {
        Party party = player.getParty();
        World world = player.getWorldServer();

        if (party == null) {
            party = world.getParty(partyid);
            if (party != null) {
                if (party.getMembers().size() < 6) {
                    PartyCharacter partyplayer = new PartyCharacter(player);
                    player.getMap().addPartyMember(player, party.getId());

                    world.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                    player.receivePartyMemberHP();
                    player.updatePartyMemberHP();

                    player.resetPartySearchInvite(party.getLeaderId());
                    player.updatePartySearchAvailability(false);
                    player.partyOperationUpdate(party, null);
                    return true;
                } else {
                    if (!silentCheck) {
                        player.sendPacket(PacketCreator.partyStatusMessage(17));
                    }
                }
            } else {
                player.sendPacket(PacketCreator.serverNotice(5, "You couldn't join the party since it had already been disbanded."));
            }
        } else {
            if (!silentCheck) {
                player.sendPacket(PacketCreator.serverNotice(5, "You can't join the party as you are already in one."));
            }
        }

        return false;
    }

    public static void leaveParty(Party party, Client c) {
        World world = c.getWorldServer();
        Character player = c.getPlayer();
        PartyCharacter partyplayer = player.getMPC();

        if (party != null && partyplayer != null) {
            if (partyplayer.getId() == party.getLeaderId()) {
                c.getWorldServer().removeMapPartyMembers(party.getId());

                MonsterCarnival mcpq = player.getMonsterCarnival();
                if (mcpq != null) {
                    mcpq.leftParty(player.getId());
                }

                world.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);

                EventInstanceManager eim = player.getEventInstance();
                if (eim != null) {
                    eim.disbandParty();
                }
            } else {
                MapleMap map = player.getMap();
                if (map != null) {
                    map.removePartyMember(player, party.getId());
                }

                MonsterCarnival mcpq = player.getMonsterCarnival();
                if (mcpq != null) {
                    mcpq.leftParty(player.getId());
                }

                world.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);

                EventInstanceManager eim = player.getEventInstance();
                if (eim != null) {
                    eim.leftParty(player);
                }
            }

            player.setParty(null);

            MatchCheckerCoordinator mmce = c.getWorldServer().getMatchCheckerCoordinator();
            if (mmce.getMatchConfirmationLeaderid(player.getId()) == player.getId() && mmce.getMatchConfirmationType(player.getId()) == MatchCheckerType.GUILD_CREATION) {
                mmce.dismissMatchConfirmation(player.getId());
            }
        }
    }

    public static void expelFromParty(Party party, Client c, int expelCid) {
        World world = c.getWorldServer();
        Character player = c.getPlayer();
        PartyCharacter partyplayer = player.getMPC();

        if (party != null && partyplayer != null) {
            if (partyplayer.equals(party.getLeader())) {
                PartyCharacter expelled = party.getMemberById(expelCid);
                if (expelled != null) {
                    Character emc = expelled.getPlayer();
                    if (emc != null) {
                        List<Character> partyMembers = emc.getPartyMembersOnline();

                        MapleMap map = emc.getMap();
                        if (map != null) {
                            map.removePartyMember(emc, party.getId());
                        }

                        MonsterCarnival mcpq = player.getMonsterCarnival();
                        if (mcpq != null) {
                            mcpq.leftParty(emc.getId());
                        }

                        EventInstanceManager eim = emc.getEventInstance();
                        if (eim != null) {
                            eim.leftParty(emc);
                        }

                        emc.setParty(null);
                        world.updateParty(party.getId(), PartyOperation.EXPEL, expelled);

                        emc.updatePartySearchAvailability(true);
                        emc.partyOperationUpdate(party, partyMembers);
                    } else {
                        world.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                    }
                }
            }
        }
    }
}
