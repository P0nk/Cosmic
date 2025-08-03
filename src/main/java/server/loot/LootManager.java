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
package server.loot;

import client.Character;
import database.drop.DropProvider;
import server.life.MonsterDropEntry;
import server.quest.Quest;

import java.util.Collections;
import java.util.List;

/**
 * @author Ronan
 */
public class LootManager {

    private static boolean isRelevantDrop(MonsterDropEntry dropEntry, List<Character> chrs, List<LootInventory> inventories) {
        if (dropEntry.questid <= 0) {
            return true;
        }

        Quest quest = Quest.getInstance(dropEntry.questid);
        int questStartAmount = quest.getStartItemAmountNeeded(dropEntry.itemId);
        int questCompleteAmount = quest.getCompleteItemAmountNeeded(dropEntry.itemId);

        for (int i = 0; i < chrs.size(); i++) {

            int chrQuestStatus = chrs.get(i).getQuestStatus(dropEntry.questid);
            final int questItemAmount;
            if (chrQuestStatus == 0) {
                questItemAmount = questStartAmount;
            } else if (chrQuestStatus == 1) {
                questItemAmount = questCompleteAmount;
            } else {
                continue;
            }

            LootInventory chrInv = inventories.get(i);
            boolean hasQuestItems = chrInv.hasItem(dropEntry.itemId, questItemAmount);
            if (!hasQuestItems) {
                return true;
            }
        }

        return false;
    }

    public static List<MonsterDropEntry> retrieveRelevantDrops(int monsterId, List<Character> chrs, DropProvider dropProvider) {
        List<MonsterDropEntry> drops = dropProvider.getMonsterDropEntries(monsterId);
        if (drops.isEmpty()) {
            return Collections.emptyList();
        }

        List<LootInventory> inventories = chrs.stream()
                .map(LootInventory::new)
                .toList();

        return drops.stream()
                .filter(entry -> isRelevantDrop(entry, chrs, inventories))
                .toList();
    }

}
