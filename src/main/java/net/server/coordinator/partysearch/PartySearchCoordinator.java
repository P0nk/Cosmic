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
package net.server.coordinator.partysearch;

import client.Character;
import client.Job;
import config.YamlConfig;
import constants.id.MapId;
import net.server.coordinator.world.InviteCoordinator;
import net.server.coordinator.world.InviteCoordinator.InviteType;
import net.server.world.Party;
import provider.Data;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;
import tools.PacketCreator;
import tools.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Ronan
 */
public class PartySearchCoordinator {

    private final Map<Job, PartySearchStorage> storage = new HashMap<>();
    private final Map<Job, PartySearchEchelon> upcomers = new HashMap<>();

    private final List<Character> leaderQueue = new LinkedList<>();
    private final Lock leaderQueueRLock;
    private final Lock leaderQueueWLock;

    private final Map<Integer, Character> searchLeaders = new HashMap<>();
    private final Map<Integer, LeaderSearchMetadata> searchSettings = new HashMap<>();

    private final Map<Character, LeaderSearchMetadata> timeoutLeaders = new HashMap<>();

    private int updateCount = 0;

    private static final Map<Integer, Set<Integer>> mapNeighbors = fetchNeighbouringMaps();
    private static final Map<Integer, Job> jobTable = instantiateJobTable();

    public PartySearchCoordinator() {
        for (Job job : jobTable.values()) {
            storage.put(job, new PartySearchStorage());
            upcomers.put(job, new PartySearchEchelon());
        }

        ReadWriteLock leaderQueueLock = new ReentrantReadWriteLock(true);
        this.leaderQueueRLock = leaderQueueLock.readLock();
        this.leaderQueueWLock = leaderQueueLock.writeLock();
    }

    private static Map<Integer, Set<Integer>> fetchNeighbouringMaps() {
        Map<Integer, Set<Integer>> mapLinks = new HashMap<>();

        Data data = DataProviderFactory.getDataProvider(WZFiles.ETC).getData("MapNeighbors.img");
        if (data != null) {
            for (Data mapdata : data.getChildren()) {
                int mapid = Integer.parseInt(mapdata.getName());

                Set<Integer> neighborMaps = new HashSet<>();
                mapLinks.put(mapid, neighborMaps);

                for (Data neighbordata : mapdata.getChildren()) {
                    int neighborid = DataTool.getInt(neighbordata, MapId.NONE);

                    if (neighborid != MapId.NONE) {
                        neighborMaps.add(neighborid);
                    }
                }
            }
        }

        return mapLinks;
    }

    public static boolean isInVicinity(int callerMapid, int calleeMapid) {
        Set<Integer> vicinityMapids = mapNeighbors.get(calleeMapid);

        if (vicinityMapids != null) {
            return vicinityMapids.contains(calleeMapid);
        } else {
            int callerRange = callerMapid / 10000000;
            if (callerRange >= 90) {
                return callerRange == (calleeMapid / 1000000);
            } else {
                return callerRange == (calleeMapid / 10000000);
            }
        }
    }

    private static Map<Integer, Job> instantiateJobTable() {
        Map<Integer, Job> table = new HashMap<>();

        List<Pair<Integer, Integer>> jobSearchTypes = new LinkedList<Pair<Integer, Integer>>() {{
            add(new Pair<>(Job.HERALD.getId(), 0));
            add(new Pair<>(Job.RETRIBUTE.getId(), 0));
            add(new Pair<>(Job.DARKOMEN.getId(), 0));
            add(new Pair<>(Job.FIREGRANDMASTER.getId(), 0));
            add(new Pair<>(Job.ICEGRANDMASTER.getId(), 0));
            add(new Pair<>(Job.HOLYGRANDMASTER.getId(), 0));
            add(new Pair<>(Job.SHARPEYES.getId(), 0));
            add(new Pair<>(Job.CURSEDBOLT.getId(), 0));
            add(new Pair<>(Job.VOID.getId(), 0));
            add(new Pair<>(Job.INVERSE.getId(), 0));
            add(new Pair<>(Job.CAPTAIN.getId(), 0));
            add(new Pair<>(Job.SHARPSHOT.getId(), 0));
            add(new Pair<>(Job.SYNTH_MASTER.getId(), 0));
            add(new Pair<>(Job.DAWNWARRIOR5.getId(), 0));
            add(new Pair<>(Job.BLAZEWIZARD5.getId(), 0));
            add(new Pair<>(Job.WINDARCHER5.getId(), 0));
            add(new Pair<>(Job.NIGHTWALKER5.getId(), 0));
            add(new Pair<>(Job.THUNDERBREAKER5.getId(), 0));
            add(new Pair<>(Job.MAPLELEAF_BRIGADIER.getId(), 0));
            add(new Pair<>(0, 0));
            add(new Pair<>(Job.ARAN1.getId(), 0));
            add(new Pair<>(100, 3));
            add(new Pair<>(Job.DAWNWARRIOR1.getId(), 0));
            add(new Pair<>(200, 3));
            add(new Pair<>(Job.BLAZEWIZARD1.getId(), 0));
            add(new Pair<>(500, 2));
            add(new Pair<>(Job.THUNDERBREAKER1.getId(), 0));
            add(new Pair<>(400, 2));
            add(new Pair<>(Job.NIGHTWALKER1.getId(), 0));
            add(new Pair<>(300, 2));
            add(new Pair<>(Job.WINDARCHER1.getId(), 0));
            add(new Pair<>(Job.EVAN1.getId(), 0));
        }};

        int i = 0;
        for (Pair<Integer, Integer> p : jobSearchTypes) {
            table.put(i, Job.getById(p.getLeft()));
            i++;

            for (int j = 1; j <= p.getRight(); j++) {
                table.put(i, Job.getById(p.getLeft() + 10 * j));
                i++;
            }
        }

        return table;
    }

    private class LeaderSearchMetadata {
        private final int minLevel;
        private final int maxLevel;
        private final List<Job> searchedJobs;

        private int reentryCount;

        private List<Job> decodeSearchedJobs(int jobsSelected) {
            List<Job> searchedJobs = new LinkedList<>();

            int topByte = (int) ((Math.log(jobsSelected) / Math.log(2)) + 1e-5);

            for (int i = 0; i <= topByte; i++) {
                if (jobsSelected % 2 == 1) {
                    Job job = jobTable.get(i);
                    if (job != null) {
                        searchedJobs.add(job);
                    }
                }

                jobsSelected = jobsSelected >> 1;
                if (jobsSelected == 0) {
                    break;
                }
            }

            return searchedJobs;
        }

        private LeaderSearchMetadata(int minLevel, int maxLevel, int jobs) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.searchedJobs = decodeSearchedJobs(jobs);
            this.reentryCount = 0;
        }

    }

    public void attachPlayer(Character chr) {
        upcomers.get(getPartySearchJob(chr.getJob())).attachPlayer(chr);
    }

    public void detachPlayer(Character chr) {
        Job psJob = getPartySearchJob(chr.getJob());

        if (!upcomers.get(psJob).detachPlayer(chr)) {
            storage.get(psJob).detachPlayer(chr);
        }
    }

    public void updatePartySearchStorage() {
        for (Entry<Job, PartySearchEchelon> psUpdate : upcomers.entrySet()) {
            storage.get(psUpdate.getKey()).updateStorage(psUpdate.getValue().exportEchelon());
        }
    }

    private static Job getPartySearchJob(Job job) {
        if (job.getJobNiche() == 0) {
            return Job.BEGINNER;
        } else if (job.getId() < 600) { // explorers
            return Job.getById((job.getId() / 10) * 10);
        } else if (job.getId() >= 1000) {
            return Job.getById((job.getId() / 100) * 100);
        } else {
            return Job.MAPLELEAF_BRIGADIER;
        }
    }

    private Character fetchPlayer(int callerCid, int callerMapid, Job job, int minLevel, int maxLevel) {
        return storage.get(getPartySearchJob(job)).callPlayer(callerCid, callerMapid, minLevel, maxLevel);
    }

    private void addQueueLeader(Character leader) {
        leaderQueueRLock.lock();
        try {
            leaderQueue.add(leader);
        } finally {
            leaderQueueRLock.unlock();
        }
    }

    private void removeQueueLeader(Character leader) {
        leaderQueueRLock.lock();
        try {
            leaderQueue.remove(leader);
        } finally {
            leaderQueueRLock.unlock();
        }
    }

    public void registerPartyLeader(Character leader, int minLevel, int maxLevel, int jobs) {
        if (searchLeaders.containsKey(leader.getId())) {
            return;
        }

        searchSettings.put(leader.getId(), new LeaderSearchMetadata(minLevel, maxLevel, jobs));
        searchLeaders.put(leader.getId(), leader);
        addQueueLeader(leader);
    }

    private void registerPartyLeader(Character leader, LeaderSearchMetadata settings) {
        if (searchLeaders.containsKey(leader.getId())) {
            return;
        }

        searchSettings.put(leader.getId(), settings);
        searchLeaders.put(leader.getId(), leader);
        addQueueLeader(leader);
    }

    public void unregisterPartyLeader(Character leader) {
        Character toRemove = searchLeaders.remove(leader.getId());
        if (toRemove != null) {
            removeQueueLeader(toRemove);
            searchSettings.remove(leader.getId());
        } else {
            unregisterLongTermPartyLeader(leader);
        }
    }

    private Character searchPlayer(Character leader) {
        LeaderSearchMetadata settings = searchSettings.get(leader.getId());
        if (settings != null) {
            int minLevel = settings.minLevel, maxLevel = settings.maxLevel;
            Collections.shuffle(settings.searchedJobs);

            int leaderCid = leader.getId();
            int leaderMapid = leader.getMapId();
            for (Job searchJob : settings.searchedJobs) {
                Character chr = fetchPlayer(leaderCid, leaderMapid, searchJob, minLevel, maxLevel);
                if (chr != null) {
                    return chr;
                }
            }
        }

        return null;
    }

    private boolean sendPartyInviteFromSearch(Character chr, Character leader) {
        if (chr == null) {
            return false;
        }

        int partyid = leader.getPartyId();
        if (partyid < 0) {
            return false;
        }

        if (InviteCoordinator.createInvite(InviteType.PARTY, leader, partyid, chr.getId())) {
            chr.disablePartySearchInvite(leader.getId());
            chr.sendPacket(PacketCreator.partySearchInvite(leader));
            return true;
        } else {
            return false;
        }
    }

    private Pair<List<Character>, List<Character>> fetchQueuedLeaders() {
        List<Character> queuedLeaders, nextLeaders;

        leaderQueueWLock.lock();
        try {
            int splitIdx = Math.min(leaderQueue.size(), 100);

            queuedLeaders = new LinkedList<>(leaderQueue.subList(0, splitIdx));
            nextLeaders = new LinkedList<>(leaderQueue.subList(splitIdx, leaderQueue.size()));
        } finally {
            leaderQueueWLock.unlock();
        }

        return new Pair<>(queuedLeaders, nextLeaders);
    }

    private void registerLongTermPartyLeaders(List<Pair<Character, LeaderSearchMetadata>> recycledLeaders) {
        leaderQueueRLock.lock();
        try {
            for (Pair<Character, LeaderSearchMetadata> p : recycledLeaders) {
                timeoutLeaders.put(p.getLeft(), p.getRight());
            }
        } finally {
            leaderQueueRLock.unlock();
        }
    }

    private void unregisterLongTermPartyLeader(Character leader) {
        leaderQueueRLock.lock();
        try {
            timeoutLeaders.remove(leader);
        } finally {
            leaderQueueRLock.unlock();
        }
    }

    private void reinstateLongTermPartyLeaders() {
        Map<Character, LeaderSearchMetadata> timeoutLeadersCopy;
        leaderQueueWLock.lock();
        try {
            timeoutLeadersCopy = new HashMap<>(timeoutLeaders);
            timeoutLeaders.clear();
        } finally {
            leaderQueueWLock.unlock();
        }

        for (Entry<Character, LeaderSearchMetadata> e : timeoutLeadersCopy.entrySet()) {
            registerPartyLeader(e.getKey(), e.getValue());
        }
    }

    public void runPartySearch() {
        Pair<List<Character>, List<Character>> queuedLeaders = fetchQueuedLeaders();

        List<Character> searchedLeaders = new LinkedList<>();
        List<Character> recalledLeaders = new LinkedList<>();
        List<Character> expiredLeaders = new LinkedList<>();

        for (Character leader : queuedLeaders.getLeft()) {
            Character chr = searchPlayer(leader);
            if (sendPartyInviteFromSearch(chr, leader)) {
                searchedLeaders.add(leader);
            } else {
                LeaderSearchMetadata settings = searchSettings.get(leader.getId());
                if (settings != null) {
                    if (settings.reentryCount < YamlConfig.config.server.PARTY_SEARCH_REENTRY_LIMIT) {
                        settings.reentryCount += 1;
                        recalledLeaders.add(leader);
                    } else {
                        expiredLeaders.add(leader);
                    }
                }
            }
        }

        leaderQueueRLock.lock();
        try {
            leaderQueue.clear();
            leaderQueue.addAll(queuedLeaders.getRight());

            try {
                leaderQueue.addAll(25, recalledLeaders);
            } catch (IndexOutOfBoundsException e) {
                leaderQueue.addAll(recalledLeaders);
            }
        } finally {
            leaderQueueRLock.unlock();
        }

        for (Character leader : searchedLeaders) {
            Party party = leader.getParty();
            if (party != null && party.getMembers().size() < 6) {
                addQueueLeader(leader);
            } else {
                if (leader.isLoggedinWorld()) {
                    leader.dropMessage(5, "Your Party Search token session has finished as your party reached full capacity.");
                }
                searchLeaders.remove(leader.getId());
                searchSettings.remove(leader.getId());
            }
        }

        List<Pair<Character, LeaderSearchMetadata>> recycledLeaders = new LinkedList<>();
        for (Character leader : expiredLeaders) {
            searchLeaders.remove(leader.getId());
            LeaderSearchMetadata settings = searchSettings.remove(leader.getId());

            if (leader.isLoggedinWorld()) {
                if (settings != null) {
                    recycledLeaders.add(new Pair<>(leader, settings));
                    if (YamlConfig.config.server.USE_DEBUG && leader.isGM()) {
                        leader.dropMessage(5, "Your Party Search token session is now on waiting queue for up to 7 minutes, to get it working right away please stop your Party Search and retry again later.");
                    }
                } else {
                    leader.dropMessage(5, "Your Party Search token session expired, please stop your Party Search and retry again later.");
                }
            }
        }

        if (!recycledLeaders.isEmpty()) {
            registerLongTermPartyLeaders(recycledLeaders);
        }

        updateCount++;
        if (updateCount % 77 == 0) {
            reinstateLongTermPartyLeaders();
        }
    }

}
