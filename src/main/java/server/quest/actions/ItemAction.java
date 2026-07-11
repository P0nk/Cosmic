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
package server.quest.actions;

import client.Character;
import client.Client;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import constants.inventory.ItemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataTool;
import server.ItemInformationProvider;
import server.quest.Quest;
import server.quest.QuestActionType;
import tools.PacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author Tyler (Twdtwd)
 * @author Ronan
 */
public class ItemAction extends AbstractQuestAction {
    private static final Logger log = LoggerFactory.getLogger(ItemAction.class);
    List<ItemData> items = new ArrayList<>();

    public ItemAction(Quest quest, Data data) {
        super(QuestActionType.ITEM, quest);
        processData(data);
    }


    @Override
    public void processData(Data data) {
        for (Data iEntry : data.getChildren()) {
            int id = DataTool.getInt(iEntry.getChildByPath("id"));
            int count = DataTool.getInt(iEntry.getChildByPath("count"), 1);
            int period = DataTool.getInt(iEntry.getChildByPath("period"), 0);

            Integer prop = null;
            Data propData = iEntry.getChildByPath("prop");
            if (propData != null) {
                prop = DataTool.getInt(propData);
            }

            int gender = 2;
            if (iEntry.getChildByPath("gender") != null) {
                gender = DataTool.getInt(iEntry.getChildByPath("gender"));
            }

            int job = -1;
            if (iEntry.getChildByPath("job") != null) {
                job = DataTool.getInt(iEntry.getChildByPath("job"));
            }

            items.add(new ItemData(Integer.parseInt(iEntry.getName()), id, count, prop, job, gender, period));
        }

        items.sort((o1, o2) -> o1.map - o2.map);
    }

    @Override
    public void run(Character chr, Integer extSelection) {
        List<ItemData> takeItem = new LinkedList<>();
        List<ItemData> giveItem = new LinkedList<>();

        int props = 0, rndProps = 0, accProps = 0;
        for (ItemData item : items) {
            if (item.getProp() != null && item.getProp() != -1 && canGetItem(item, chr)) {
                props += item.getProp();
            }
        }

        int extNum = 0;
        if (props > 0) {
            rndProps = Randomizer.nextInt(props);
        }
        for (ItemData iEntry : items) {
            if (!canGetItem(iEntry, chr)) {
                continue;
            }

            if (iEntry.getProp() != null) {
                if (iEntry.getProp() == -1) {
                    if (extSelection != extNum++) {
                        continue;
                    }
                } else {
                    accProps += iEntry.getProp();

                    if (accProps <= rndProps) {
                        continue;
                    } else {
                        accProps = Integer.MIN_VALUE;
                    }
                }
            }

            if (iEntry.getCount() < 0) { // Remove Item
                takeItem.add(iEntry);
            } else {                    // Give Item
                giveItem.add(iEntry);
            }
        }

        // must take all needed items before giving others

        for (ItemData iEntry : takeItem) {
            int itemid = iEntry.getId(), count = iEntry.getCount();

            InventoryType type = ItemConstants.getInventoryType(itemid);
            int quantity = count * -1; // Invert
            if (type.equals(InventoryType.EQUIP)) {
                if (chr.getInventory(type).countById(itemid) < quantity) {
                    // Not enough in the equip inventoty, so check Equipped...
                    if (chr.getInventory(InventoryType.EQUIPPED).countById(itemid) > quantity) {
                        // Found it equipped, so change the type to equipped.
                        type = InventoryType.EQUIPPED;
                    }
                }
            }

            InventoryManipulator.removeById(chr.getClient(), type, itemid, quantity, true, false);
            chr.sendPacket(PacketCreator.getShowItemGain(itemid, (short) count, true));
        }

        for (ItemData iEntry : giveItem) {
            int itemid = iEntry.getId(), count = iEntry.getCount(), period = iEntry.getPeriod();    // thanks Vcoc for noticing quest milestone item not getting removed from inventory after a while

            InventoryManipulator.addById(chr, itemid, (short) count, "", -1, period > 0 ? (System.currentTimeMillis() + MINUTES.toMillis(period)) : -1);
            chr.sendPacket(PacketCreator.getShowItemGain(itemid, (short) count, true));
        }
    }

    @Override
    public boolean check(Character chr, Integer extSelection) {
        List<Pair<Item, InventoryType>> gainList = new LinkedList<>();
        List<Pair<Item, InventoryType>> selectList = new LinkedList<>();
        List<Pair<Item, InventoryType>> randomList = new LinkedList<>();

        List<Integer> allSlotUsed = new ArrayList(5);
        for (byte i = 0; i < 5; i++) {
            allSlotUsed.add(0);
        }

        for (ItemData item : items) {
            if (!canGetItem(item, chr)) {
                continue;
            }

            InventoryType type = ItemConstants.getInventoryType(item.getId());
            if (item.getProp() != null) {
                Item toItem = new Item(item.getId(), (short) 0, (short) item.getCount());

                if (item.getProp() < 0) {
                    selectList.add(new Pair<>(toItem, type));
                } else {
                    randomList.add(new Pair<>(toItem, type));
                }

            } else {
                // Make sure they can hold the item.
                Item toItem = new Item(item.getId(), (short) 0, (short) item.getCount());
                gainList.add(new Pair<>(toItem, type));

                if (item.getCount() < 0) {
                    // Make sure they actually have the item.
                    int quantity = item.getCount() * -1;

                    int freeSlotCount = chr.getInventory(type).freeSlotCountById(item.getId(), quantity);
                    if (freeSlotCount == -1) {
                        if (type.equals(InventoryType.EQUIP) && chr.getInventory(InventoryType.EQUIPPED).countById(item.getId()) > quantity) {
                            continue;
                        }

                        announceInventoryLimit(Collections.singletonList(item.getId()), chr);
                        return false;
                    } else {
                        int idx = type.getType() - 1;   // more slots available from the given items!
                        allSlotUsed.set(idx, allSlotUsed.get(idx) - freeSlotCount);
                    }
                }
            }
        }

        if (!randomList.isEmpty()) {
            int result;
            Client c = chr.getClient();

            List<Integer> rndUsed = new ArrayList(5);
            for (byte i = 0; i < 5; i++) {
                rndUsed.add(allSlotUsed.get(i));
            }

            for (Pair<Item, InventoryType> it : randomList) {
                int idx = it.getRight().getType() - 1;

                result = InventoryManipulator.checkSpaceProgressively(c, it.getLeft().getItemId(), it.getLeft().getQuantity(), "", rndUsed.get(idx), false);
                if (result % 2 == 0) {
                    announceInventoryLimit(Collections.singletonList(it.getLeft().getItemId()), chr);
                    return false;
                }

                allSlotUsed.set(idx, Math.max(allSlotUsed.get(idx), result >> 1));
            }
        }

        if (!selectList.isEmpty()) {
            Pair<Item, InventoryType> selected = selectList.get(extSelection);
            gainList.add(selected);
        }

        if (!canHold(chr, gainList)) {
            List<Integer> gainItemids = new LinkedList<>();
            for (Pair<Item, InventoryType> it : gainList) {
                gainItemids.add(it.getLeft().getItemId());
            }

            announceInventoryLimit(gainItemids, chr);
            return false;
        }
        return true;
    }

    private void announceInventoryLimit(List<Integer> itemids, Character chr) {
        for (Integer id : itemids) {
            if (ItemInformationProvider.getInstance().isPickupRestricted(id) && chr.haveItemWithId(id, true)) {
                chr.dropMessage(1, "Please check if you already have a similar one-of-a-kind item in your inventory.");
                return;
            }
        }

        chr.dropMessage(1, "Please check if you have enough space in your inventory.");
    }

    private boolean canHold(Character chr, List<Pair<Item, InventoryType>> gainList) {
        List<Integer> toAddItemids = new LinkedList<>();
        List<Integer> toAddQuantity = new LinkedList<>();
        List<Integer> toRemoveItemids = new LinkedList<>();
        List<Integer> toRemoveQuantity = new LinkedList<>();

        for (Pair<Item, InventoryType> item : gainList) {
            Item it = item.getLeft();

            if (it.getQuantity() > 0) {
                toAddItemids.add(it.getItemId());
                toAddQuantity.add((int) it.getQuantity());
            } else {
                toRemoveItemids.add(it.getItemId());
                toRemoveQuantity.add(-1 * ((int) it.getQuantity()));
            }
        }

        // thanks onechord for noticing quests unnecessarily giving out "full inventory" from quests that also takes items from players
        return chr.getAbstractPlayerInteraction().canHoldAllAfterRemoving(toAddItemids, toAddQuantity, toRemoveItemids, toRemoveQuantity);
    }

    private boolean canGetItem(ItemData item, Character chr) {
        if (item.getGender() != 2 && item.getGender() != chr.getGender()) {
            return false;
        }

        if (item.job > 0) {
            final List<Integer> code = getJobBy5ByteEncoding(item.getJob());
            boolean jobFound = false;
            for (int codec : code) {
                if (codec / 100 == chr.getJob().getId() / 100) {
                    jobFound = true;
                    break;
                }
            }
            return jobFound;
        }

        return true;
    }

    public boolean restoreLostItem(Character chr, int itemid) {
        if (!ItemInformationProvider.getInstance().isQuestItem(itemid)) {
            return false;
        }

        // thanks danielktran (MapleHeroesD)
        for (ItemData item : items) {
            if (item.getId() == itemid) {
                int missingQty = item.getCount() - chr.countItem(itemid);
                if (missingQty > 0) {
                    if (!chr.canHold(itemid, missingQty)) {
                        chr.dropMessage(1, "Please check if you have enough space in your inventory.");
                        return false;
                    }

                    InventoryManipulator.addById(chr.getClient(), item.getId(), (short) missingQty);
                    log.debug("Chr {} obtained {}x {} from questId {}", chr, itemid, missingQty, questID);
                }
                return true;
            }
        }

        return false;
    }

    private class ItemData {
        private final int map, id, count, job, gender, period;
        private final Integer prop;

        public ItemData(int map, int id, int count, Integer prop, int job, int gender, int period) {
            this.map = map;
            this.id = id;
            this.count = count;
            this.prop = prop;
            this.job = job;
            this.gender = gender;
            this.period = period;
        }

        public int getId() {
            return id;
        }

        public int getCount() {
            return count;
        }

        public Integer getProp() {
            return prop;
        }

        public int getJob() {
            return job;
        }

        public int getGender() {
            return gender;
        }

        public int getPeriod() {
            return period;
        }
    }
} 
