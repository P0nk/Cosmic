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
import tools.IntervalBuilder;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Ronan
 */
public class PartySearchStorage {

    private final List<PartySearchCharacter> storage = new ArrayList<>(20);
    private final IntervalBuilder emptyIntervals = new IntervalBuilder();

    private final Lock psRLock;
    private final Lock psWLock;

    public PartySearchStorage() {
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
        this.psRLock = readWriteLock.readLock();
        this.psWLock = readWriteLock.writeLock();
    }

    public List<PartySearchCharacter> getStorageList() {
        psRLock.lock();
        try {
            return new ArrayList<>(storage);
        } finally {
            psRLock.unlock();
        }
    }

    private Map<Integer, Character> fetchRemainingPlayers() {
        List<PartySearchCharacter> players = getStorageList();
        Map<Integer, Character> remainingPlayers = new HashMap<>(players.size());

        for (PartySearchCharacter psc : players) {
            if (psc.isQueued()) {
                Character chr = psc.getPlayer();
                if (chr != null) {
                    remainingPlayers.put(chr.getId(), chr);
                }
            }
        }

        return remainingPlayers;
    }

    public void updateStorage(Collection<Character> echelon) {
        Map<Integer, Character> newcomers = new HashMap<>();
        for (Character chr : echelon) {
            newcomers.put(chr.getId(), chr);
        }

        Map<Integer, Character> curStorage = fetchRemainingPlayers();
        curStorage.putAll(newcomers);

        List<PartySearchCharacter> pscList = new ArrayList<>(curStorage.size());
        for (Character chr : curStorage.values()) {
            pscList.add(new PartySearchCharacter(chr));
        }

        pscList.sort((c1, c2) -> {
            int levelP1 = c1.getLevel(), levelP2 = c2.getLevel();
            return levelP1 > levelP2 ? 1 : (levelP1 == levelP2 ? 0 : -1);
        });

        psWLock.lock();
        try {
            storage.clear();
            storage.addAll(pscList);
        } finally {
            psWLock.unlock();
        }

        emptyIntervals.clear();
    }

    private static int bsearchStorage(List<PartySearchCharacter> storage, int level) {
        int st = 0, en = storage.size() - 1;

        int mid, idx;
        while (en >= st) {
            idx = (st + en) / 2;
            mid = storage.get(idx).getLevel();

            if (mid == level) {
                return idx;
            } else if (mid < level) {
                st = idx + 1;
            } else {
                en = idx - 1;
            }
        }

        return en;
    }

    public Character callPlayer(int callerCid, int callerMapid, int minLevel, int maxLevel) {
        if (emptyIntervals.inInterval(minLevel, maxLevel)) {
            return null;
        }

        List<PartySearchCharacter> pscList = getStorageList();

        int idx = bsearchStorage(pscList, maxLevel);
        for (int i = idx; i >= 0; i--) {
            PartySearchCharacter psc = pscList.get(i);
            if (!psc.isQueued()) {
                continue;
            }

            if (psc.getLevel() < minLevel) {
                break;
            }

            Character chr = psc.callPlayer(callerCid, callerMapid);
            if (chr != null) {
                return chr;
            }
        }

        emptyIntervals.addInterval(minLevel, maxLevel);
        return null;
    }

    public void detachPlayer(Character chr) {
        PartySearchCharacter toRemove = null;
        for (PartySearchCharacter psc : getStorageList()) {
            Character player = psc.getPlayer();

            if (player != null && player.getId() == chr.getId()) {
                toRemove = psc;
                break;
            }
        }

        if (toRemove != null) {
            psWLock.lock();
            try {
                storage.remove(toRemove);
            } finally {
                psWLock.unlock();
            }
        }
    }

}
