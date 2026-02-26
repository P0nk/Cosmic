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
package client.inventory.manipulator;

import server.subordinate.SubordinateManager;
import client.BuffStat;
import client.Character;
import client.Client;
import client.inventory.Equip;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ModifyInventory;
import client.inventory.Pet;
import client.newyear.NewYearCardRecord;
import config.YamlConfig;
import constants.id.ItemId;
import constants.inventory.ItemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.maps.MapleMap;
import tools.PacketCreator;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Matze
 * @author Ronan - improved check space feature and removed redundant object
 *         calls
 */
public class InventoryManipulator {
    private static final Logger log = LoggerFactory.getLogger(InventoryManipulator.class);

    public static boolean addById(Client c, int itemId, short quantity) {
        return addById(c, itemId, quantity, null, -1, -1);
    }

    public static boolean addById(Client c, int itemId, short quantity, long expiration) {
        return addById(c, itemId, quantity, null, -1, (byte) 0, expiration);
    }

    public static boolean addById(Client c, int itemId, short quantity, String owner, int petid) {
        return addById(c, itemId, quantity, owner, petid, -1);
    }

    public static boolean addById(Client c, int itemId, short quantity, String owner, int petid, long expiration) {
        return addById(c, itemId, quantity, owner, petid, (byte) 0, expiration);
    }

    public static boolean addById(Client c, int itemId, short quantity, String owner, int petid, short flag,
            long expiration) {
        Character chr = c.getPlayer();
        InventoryType type = ItemConstants.getInventoryType(itemId);

        Inventory inv = chr.getInventory(type);
        inv.lockInventory();
        try {
            return addByIdInternal(c, chr, type, inv, itemId, quantity, owner, petid, flag, expiration);
        } finally {
            inv.unlockInventory();
        }
    }

    private static boolean addByIdInternal(Client c, Character chr, InventoryType type, Inventory inv, int itemId,
            short quantity, String owner, int petid, short flag, long expiration) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        if (!type.equals(InventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(c, itemId);
            List<Item> existing = inv.listById(itemId);
            if (!ItemConstants.isRechargeable(itemId) && petid == -1) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax
                                    && ((eItem.getOwner().equals(owner) || owner == null) && eItem.getFlag() == flag)) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                eItem.setExpiration(expiration);
                                // [MODIFIED] Added c.getPlayer()
                                c.sendPacket(PacketCreator.modifyInventory(true,
                                        Collections.singletonList(new ModifyInventory(1, eItem)), c.getPlayer()));
                            }
                        } else {
                            break;
                        }
                    }
                }
                boolean sandboxItem = (flag & ItemConstants.SANDBOX) == ItemConstants.SANDBOX;
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        Item nItem = new Item(itemId, (short) 0, newQ, petid);
                        nItem.setFlag(flag);
                        nItem.setExpiration(expiration);
                        short newSlot = inv.addItem(nItem);
                        if (newSlot == -1) {
                            c.sendPacket(PacketCreator.getInventoryFull());
                            c.sendPacket(PacketCreator.getShowInventoryFull());
                            return false;
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        // [MODIFIED] Added c.getPlayer()
                        c.sendPacket(PacketCreator.modifyInventory(true,
                                Collections.singletonList(new ModifyInventory(0, nItem)), c.getPlayer()));
                        if (sandboxItem) {
                            chr.setHasSandboxItem();
                        }
                    } else {
                        c.sendPacket(PacketCreator.enableActions());
                        return false;
                    }
                }
            } else {
                Item nItem = new Item(itemId, (short) 0, quantity, petid);
                nItem.setFlag(flag);
                nItem.setExpiration(expiration);
                short newSlot = inv.addItem(nItem);
                if (newSlot == -1) {
                    c.sendPacket(PacketCreator.getInventoryFull());
                    c.sendPacket(PacketCreator.getShowInventoryFull());
                    return false;
                }
                // [MODIFIED] Added c.getPlayer()
                c.sendPacket(PacketCreator.modifyInventory(true,
                        Collections.singletonList(new ModifyInventory(0, nItem)), c.getPlayer()));
                if (InventoryManipulator.isSandboxItem(nItem)) {
                    chr.setHasSandboxItem();
                }
            }
        } else if (quantity == 1) {
            Item nEquip = ii.getEquipById(itemId);
            nEquip.setFlag(flag);
            nEquip.setExpiration(expiration);
            if (owner != null) {
                nEquip.setOwner(owner);
            }
            short newSlot = inv.addItem(nEquip);
            if (newSlot == -1) {
                c.sendPacket(PacketCreator.getInventoryFull());
                c.sendPacket(PacketCreator.getShowInventoryFull());
                return false;
            }
            // [MODIFIED] Added c.getPlayer()
            c.sendPacket(PacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nEquip)),
                    c.getPlayer()));
            if (InventoryManipulator.isSandboxItem(nEquip)) {
                chr.setHasSandboxItem();
            }
        } else {
            throw new RuntimeException("Trying to create equip with non-one quantity");
        }
        return true;
    }

    public static boolean addFromDrop(Client c, Item item) {
        return addFromDrop(c, item, true);
    }

    public static boolean addFromDrop(Client c, Item item, boolean show) {
        return addFromDrop(c, item, show, item.getPetId());
    }

    public static boolean addFromDrop(Client c, Item item, boolean show, int petId) {
        Character chr = c.getPlayer();
        InventoryType type = item.getInventoryType();

        Inventory inv = chr.getInventory(type);
        inv.lockInventory();
        try {
            return addFromDropInternal(c, chr, type, inv, item, show, petId);
        } finally {
            inv.unlockInventory();
        }
    }

    private static boolean addFromDropInternal(Client c, Character chr, InventoryType type, Inventory inv, Item item,
            boolean show, int petId) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        int itemid = item.getItemId();
        if (ii.isPickupRestricted(itemid) && chr.haveItemWithId(itemid, true)) {
            c.sendPacket(PacketCreator.getInventoryFull());
            c.sendPacket(PacketCreator.showItemUnavailable());
            return false;
        }
        short quantity = item.getQuantity();

        if (!type.equals(InventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(c, itemid);
            List<Item> existing = inv.listById(itemid);
            if (!ItemConstants.isRechargeable(itemid) && petId == -1) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && item.getFlag() == eItem.getFlag()
                                    && item.getOwner().equals(eItem.getOwner())) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                item.setPosition(eItem.getPosition());
                                // [MODIFIED] Added c.getPlayer()
                                c.sendPacket(PacketCreator.modifyInventory(true,
                                        Collections.singletonList(new ModifyInventory(1, eItem)), c.getPlayer()));
                            }
                        } else {
                            break;
                        }
                    }
                }
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    Item nItem = new Item(itemid, (short) 0, newQ, petId);
                    nItem.setExpiration(item.getExpiration());
                    nItem.setOwner(item.getOwner());
                    nItem.setFlag(item.getFlag());
                    short newSlot = inv.addItem(nItem);
                    if (newSlot == -1) {
                        c.sendPacket(PacketCreator.getInventoryFull());
                        c.sendPacket(PacketCreator.getShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    nItem.setPosition(newSlot);
                    item.setPosition(newSlot);
                    // [MODIFIED] Added c.getPlayer()
                    c.sendPacket(PacketCreator.modifyInventory(true,
                            Collections.singletonList(new ModifyInventory(0, nItem)), c.getPlayer()));
                    if (InventoryManipulator.isSandboxItem(nItem)) {
                        chr.setHasSandboxItem();
                    }
                }
            } else {
                Item nItem = new Item(itemid, (short) 0, quantity, petId);
                nItem.setExpiration(item.getExpiration());
                nItem.setFlag(item.getFlag());

                short newSlot = inv.addItem(nItem);
                if (newSlot == -1) {
                    c.sendPacket(PacketCreator.getInventoryFull());
                    c.sendPacket(PacketCreator.getShowInventoryFull());
                    return false;
                }
                nItem.setPosition(newSlot);
                item.setPosition(newSlot);
                // [MODIFIED] Added c.getPlayer()
                c.sendPacket(PacketCreator.modifyInventory(true,
                        Collections.singletonList(new ModifyInventory(0, nItem)), c.getPlayer()));
                if (InventoryManipulator.isSandboxItem(nItem)) {
                    chr.setHasSandboxItem();
                }
                c.sendPacket(PacketCreator.enableActions());
            }
        } else if (quantity == 1) {
            short newSlot = inv.addItem(item);
            if (newSlot == -1) {
                c.sendPacket(PacketCreator.getInventoryFull());
                c.sendPacket(PacketCreator.getShowInventoryFull());
                return false;
            }
            item.setPosition(newSlot);
            // [MODIFIED] Added c.getPlayer()
            c.sendPacket(PacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, item)),
                    c.getPlayer()));
            if (InventoryManipulator.isSandboxItem(item)) {
                chr.setHasSandboxItem();
            }
        } else {
            log.warn("Tried to pickup Equip id {} containing more than 1 quantity --> {}", itemid, quantity);
            c.sendPacket(PacketCreator.getInventoryFull());
            c.sendPacket(PacketCreator.showItemUnavailable());
            return false;
        }
        if (show) {
            c.sendPacket(PacketCreator.getShowItemGain(itemid, item.getQuantity()));
        }
        return true;
    }

    private static boolean haveItemWithId(Inventory inv, int itemid) {
        return inv.findById(itemid) != null;
    }

    public static boolean checkSpace(Client c, int itemid, int quantity, String owner) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        InventoryType type = ItemConstants.getInventoryType(itemid);
        Character chr = c.getPlayer();
        Inventory inv = chr.getInventory(type);

        if (ii.isPickupRestricted(itemid)) {
            if (haveItemWithId(inv, itemid)) {
                return false;
            } else if (ItemConstants.isEquipment(itemid)
                    && haveItemWithId(chr.getInventory(InventoryType.EQUIPPED), itemid)) {
                return false;
            }
        }

        if (!type.equals(InventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(c, itemid);
            List<Item> existing = inv.listById(itemid);

            final int numSlotsNeeded;
            if (ItemConstants.isRechargeable(itemid)) {
                numSlotsNeeded = 1;
            } else {
                if (existing.size() > 0) // first update all existing slots to slotMax
                {
                    for (Item eItem : existing) {
                        short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner.equals(eItem.getOwner())) {
                            short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }

                if (slotMax > 0) {
                    numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
                } else {
                    numSlotsNeeded = 1;
                }
            }

            return !inv.isFull(numSlotsNeeded - 1);
        } else {
            return !inv.isFull();
        }
    }

    public static int checkSpaceProgressively(Client c, int itemid, int quantity, String owner, int usedSlots,
            boolean useProofInv) {
        // return value --> bit0: if has space for this one;
        // value after: new slots filled;
        // assumption: equipments always have slotMax == 1.

        int returnValue;

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        InventoryType type = !useProofInv ? ItemConstants.getInventoryType(itemid) : InventoryType.CANHOLD;
        Character chr = c.getPlayer();
        Inventory inv = chr.getInventory(type);

        if (ii.isPickupRestricted(itemid)) {
            if (haveItemWithId(inv, itemid)) {
                return 0;
            } else if (ItemConstants.isEquipment(itemid)
                    && haveItemWithId(chr.getInventory(InventoryType.EQUIPPED), itemid)) {
                return 0; // thanks Captain & Aika & Vcoc for pointing out inventory checkup on player
                          // trades missing out one-of-a-kind items.
            }
        }

        if (!type.equals(InventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(c, itemid);
            final int numSlotsNeeded;

            if (ItemConstants.isRechargeable(itemid)) {
                numSlotsNeeded = 1;
            } else {
                List<Item> existing = inv.listById(itemid);

                if (existing.size() > 0) // first update all existing slots to slotMax
                {
                    for (Item eItem : existing) {
                        short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner.equals(eItem.getOwner())) {
                            short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }

                if (slotMax > 0) {
                    numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
                } else {
                    numSlotsNeeded = 1;
                }
            }

            returnValue = ((numSlotsNeeded + usedSlots) << 1);
            returnValue += (numSlotsNeeded == 0 || !inv.isFullAfterSomeItems(numSlotsNeeded - 1, usedSlots)) ? 1 : 0;
            // System.out.print(" needed " + numSlotsNeeded + " used " + usedSlots + " rval
            // " + returnValue);
        } else {
            returnValue = ((quantity + usedSlots) << 1);
            returnValue += (!inv.isFullAfterSomeItems(0, usedSlots)) ? 1 : 0;
            // System.out.print(" eqpneeded " + 1 + " used " + usedSlots + " rval " +
            // returnValue);
        }

        return returnValue;
    }

    public static void removeFromSlot(Client c, InventoryType type, short slot, short quantity, boolean fromDrop) {
        removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static void removeFromSlot(Client c, InventoryType type, short slot, short quantity, boolean fromDrop,
            boolean consume) {
        Character chr = c.getPlayer();
        Inventory inv = chr.getInventory(type);
        Item item = inv.getItem(slot);
        boolean allowZero = consume && ItemConstants.isRechargeable(item.getItemId());

        if (type == InventoryType.EQUIPPED) {
            inv.lockInventory();
            try {
                chr.unequippedItem((Equip) item);
                inv.removeItem(slot, quantity, allowZero);
            } finally {
                inv.unlockInventory();
            }

            announceModifyInventory(c, item, fromDrop, allowZero);
        } else {
            int petid = item.getPetId();
            if (petid > -1) { // thanks Vcoc for finding a d/c issue with equipped pets and pets remaining on
                              // DB here
                int petIdx = chr.getPetIndex(petid);
                if (petIdx > -1) {
                    Pet pet = chr.getPet(petIdx);
                    chr.unequipPet(pet, true);
                }

                inv.removeItem(slot, quantity, allowZero);
                if (type != InventoryType.CANHOLD) {
                    announceModifyInventory(c, item, fromDrop, allowZero);
                }

                // thanks Robin Schulz for noticing pet issues when moving pets out of inventory
            } else {
                inv.removeItem(slot, quantity, allowZero);
                if (type != InventoryType.CANHOLD) {
                    announceModifyInventory(c, item, fromDrop, allowZero);
                }
            }
        }
    }

    private static void announceModifyInventory(Client c, Item item, boolean fromDrop, boolean allowZero) {
        if (item.getQuantity() == 0 && !allowZero) {
            // [MODIFIED] Added c.getPlayer()
            c.sendPacket(PacketCreator.modifyInventory(fromDrop,
                    Collections.singletonList(new ModifyInventory(3, item)), c.getPlayer()));
        } else {
            // [MODIFIED] Added c.getPlayer()
            c.sendPacket(PacketCreator.modifyInventory(fromDrop,
                    Collections.singletonList(new ModifyInventory(1, item)), c.getPlayer()));
        }
    }

    public static void removeById(Client c, InventoryType type, int itemId, int quantity, boolean fromDrop,
            boolean consume) {
        int removeQuantity = quantity;
        Inventory inv = c.getPlayer().getInventory(type);
        int slotLimit = type == InventoryType.EQUIPPED ? 128 : inv.getSlotLimit();

        for (short i = 0; i <= slotLimit; i++) {
            Item item = inv.getItem((short) (type == InventoryType.EQUIPPED ? -i : i));
            if (item != null) {
                if (item.getItemId() == itemId || item.getCashId() == itemId) {
                    if (removeQuantity <= item.getQuantity()) {
                        removeFromSlot(c, type, item.getPosition(), (short) removeQuantity, fromDrop, consume);
                        removeQuantity = 0;
                        break;
                    } else {
                        removeQuantity -= item.getQuantity();
                        removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume);
                    }
                }
            }
        }
        if (removeQuantity > 0 && type != InventoryType.CANHOLD) {
            throw new RuntimeException("[Hack] Not enough items available of Item:" + itemId
                    + ", Quantity (After Quantity/Over Current Quantity): " + (quantity - removeQuantity) + "/"
                    + quantity);
        }
    }

    private static boolean isSameOwner(Item source, Item target) {
        return source.getOwner().equals(target.getOwner());
    }

    public static void move(Client c, InventoryType type, short src, short dst) {
        Inventory inv = c.getPlayer().getInventory(type);

        if (src < 0 || dst < 0) {
            return;
        }
        if (dst > inv.getSlotLimit()) {
            return;
        }
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Item source = inv.getItem(src);
        Item initialTarget = inv.getItem(dst);
        if (source == null) {
            return;
        }
        short olddstQ = -1;
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }
        short oldsrcQ = source.getQuantity();
        short slotMax = ii.getSlotMax(c, source.getItemId());
        inv.move(src, dst, slotMax);
        final List<ModifyInventory> mods = new ArrayList<>();
        if (!(type.equals(InventoryType.EQUIP) || type.equals(InventoryType.CASH)) && initialTarget != null
                && initialTarget.getItemId() == source.getItemId() && !ItemConstants.isRechargeable(source.getItemId())
                && isSameOwner(source, initialTarget)) {
            if ((olddstQ + oldsrcQ) > slotMax) {
                mods.add(new ModifyInventory(1, source));
                mods.add(new ModifyInventory(1, initialTarget));
            } else {
                mods.add(new ModifyInventory(3, source));
                mods.add(new ModifyInventory(1, initialTarget));
            }
        } else {
            mods.add(new ModifyInventory(2, source, src));
        }
        // [MODIFIED] Added c.getPlayer()
        c.sendPacket(PacketCreator.modifyInventory(true, mods, c.getPlayer()));
    }

    public static void equip(Client c, short src, short dst) {
        // [DEBUG] Print destination slot to confirm Medal Slot ID
        // System.out.println("[DEBUG] Equipping Item. Source: " + src + " | Destination
        // (dst): " + dst);

        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        Character chr = c.getPlayer();
        Inventory eqpInv = chr.getInventory(InventoryType.EQUIP);
        Inventory eqpdInv = chr.getInventory(InventoryType.EQUIPPED);

        Equip source = (Equip) eqpInv.getItem(src);
        if (source == null) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        // === NEW: server-side rebirth req-level enforcement ===
        int requiredLevel = SubordinateManager.getEffectiveReqLevelForEquip(source);
        if (chr.getLevel() < requiredLevel) {
            chr.dropMessage(5, "You must be level " + requiredLevel + " to equip this item.");
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        // keep original wearable checks (jobs/stats/etc)
        if (!ii.canWearEquipment(chr, source, dst)) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        } else if ((ItemId.isExplorerMount(source.getItemId()) && chr.isCygnus()) ||
                ((ItemId.isCygnusMount(source.getItemId())) && !chr.isCygnus())) {// Adventurer taming equipment
            return;
        }
        boolean itemChanged = false;
        if (ii.isUntradeableOnEquip(source.getItemId())) {
            short flag = source.getFlag(); // thanks BHB for noticing flags missing after equipping these
            flag |= ItemConstants.UNTRADEABLE;
            source.setFlag(flag);

            itemChanged = true;
        }
        switch (dst) {
            case -6: // unequip the overall
                Item top = eqpdInv.getItem((short) -5);
                if (top != null && ItemConstants.isOverall(top.getItemId())) {
                    if (eqpInv.isFull()) {
                        c.sendPacket(PacketCreator.getInventoryFull());
                        c.sendPacket(PacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, eqpInv.getNextFreeSlot());
                }
                break;
            case -5:
                final Item bottom = eqpdInv.getItem((short) -6);
                if (bottom != null && ItemConstants.isOverall(source.getItemId())) {
                    if (eqpInv.isFull()) {
                        c.sendPacket(PacketCreator.getInventoryFull());
                        c.sendPacket(PacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -6, eqpInv.getNextFreeSlot());
                }
                break;
            case -10: // check if weapon is two-handed
                Item weapon = eqpdInv.getItem((short) -11);
                if (weapon != null && ii.isTwoHanded(weapon.getItemId())) {
                    if (eqpInv.isFull()) {
                        c.sendPacket(PacketCreator.getInventoryFull());
                        c.sendPacket(PacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -11, eqpInv.getNextFreeSlot());
                }
                break;
            case -11:
                Item shield = eqpdInv.getItem((short) -10);
                if (shield != null && ii.isTwoHanded(source.getItemId())) {
                    if (eqpInv.isFull()) {
                        c.sendPacket(PacketCreator.getInventoryFull());
                        c.sendPacket(PacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -10, eqpInv.getNextFreeSlot());
                }
                break;
            case -18:
                if (chr.getMount() != null) {
                    chr.getMount().setItemId(source.getItemId());
                }
                break;
        }

        eqpInv.removeSlot(src);

        Equip target;
        eqpdInv.lockInventory();
        try {
            target = (Equip) eqpdInv.getItem(dst);
            if (target != null) {
                chr.unequippedItem(target);
                eqpdInv.removeSlot(dst);
            }
        } finally {
            eqpdInv.unlockInventory();
        }

        final List<ModifyInventory> mods = new ArrayList<>();
        if (itemChanged) {
            mods.add(new ModifyInventory(3, source));
            mods.add(new ModifyInventory(0, source.copy()));// to prevent crashes
        }

        source.setPosition(dst);

        eqpdInv.lockInventory();
        try {
            if (source.getRingId() > -1) {
                chr.getRingById(source.getRingId()).equip();
            }
            // ==========================================
            // [START] Inject Passive Stats onto Medal
            // ==========================================
            if (dst == -49) {
                System.out.println("[MedalEquip] Injecting passive stats for " + chr.getName());
                System.out.println(" - Passive STR: " + chr.getPassiveStr());
                System.out.println(" - Passive DEX: " + chr.getPassiveDex());
                System.out.println(" - Passive Speed: " + chr.getPassiveSpeed());
                System.out.println(" - Passive Jump: " + chr.getPassiveJump());

                source.setStr((short) Math.min(source.getStr() + chr.getPassiveStr(), Short.MAX_VALUE));
                source.setDex((short) Math.min(source.getDex() + chr.getPassiveDex(), Short.MAX_VALUE));
                source.setInt((short) Math.min(source.getInt() + chr.getPassiveInt(), Short.MAX_VALUE));
                source.setLuk((short) Math.min(source.getLuk() + chr.getPassiveLuk(), Short.MAX_VALUE));
                source.setWatk((short) Math.min(source.getWatk() + chr.getPassiveWatk(), Short.MAX_VALUE));
                source.setMatk((short) Math.min(source.getMatk() + chr.getPassiveMatk(), Short.MAX_VALUE));
                source.setWdef((short) Math.min(source.getWdef() + chr.getPassiveWdef(), Short.MAX_VALUE));
                source.setMdef((short) Math.min(source.getMdef() + chr.getPassiveMdef(), Short.MAX_VALUE));
                source.setAcc((short) Math.min(source.getAcc() + chr.getPassiveAcc(), Short.MAX_VALUE));
                source.setAvoid((short) Math.min(source.getAvoid() + chr.getPassiveEva(), Short.MAX_VALUE));
                source.setSpeed((short) Math.min(source.getSpeed() + chr.getPassiveSpeed(), Short.MAX_VALUE));
                source.setJump((short) Math.min(source.getJump() + chr.getPassiveJump(), Short.MAX_VALUE));

                System.out.println("[MedalEquip] New Equip Speed: " + source.getSpeed());
                System.out.println("[MedalEquip] New Equip Jump: " + source.getJump());
            }
            // ==========================================
            // [END] Inject Passive Stats onto Medal
            // ==========================================

            chr.equippedItem(source);
            eqpdInv.addItemFromDB(source);
        } finally {
            eqpdInv.unlockInventory();
        }

        if (target != null) {
            target.setPosition(src);
            eqpInv.addItemFromDB(target);
        }
        if (chr.getBuffedValue(BuffStat.BOOSTER) != null && ItemConstants.isWeapon(source.getItemId())) {
            chr.cancelBuffStats(BuffStat.BOOSTER);
        }

        final List<ModifyInventory> modsList = new ArrayList<>();
        if (itemChanged) {
            modsList.add(new ModifyInventory(3, source));
            modsList.add(new ModifyInventory(0, source.copy()));// to prevent crashes
        }

        // === FIX: Force "Add" packet for Medals to trigger addItemInfo ===
        if (dst == -49) {
            // 1. Remove the item from the Source Slot (Inventory)
            // We create a temporary copy to ensure the packet uses the correct 'src'
            // position
            Item removeClone = source.copy();
            removeClone.setPosition(src);
            modsList.add(new ModifyInventory(3, removeClone));

            // 2. Add the item to the Destination Slot (Equip Slot -49)
            // This triggers Mode 0 -> which calls addItemInfo -> which runs your Stat
            // Injection
            modsList.add(new ModifyInventory(0, source));
        } else {
            // Standard behavior for all other items (Move)
            modsList.add(new ModifyInventory(2, source, src));
        }

        c.sendPacket(PacketCreator.modifyInventory(true, modsList, c.getPlayer()));
        chr.equipChanged();

        // [OPTIONAL] You can keep your recalc here just to be safe server-side
        if (dst == -49) {
            chr.recalcLocalStats();
        }
    }

    public static void unequip(Client c, short src, short dst) {
        Character chr = c.getPlayer();
        Inventory eqpInv = chr.getInventory(InventoryType.EQUIP);
        Inventory eqpdInv = chr.getInventory(InventoryType.EQUIPPED);

        Equip source = (Equip) eqpdInv.getItem(src);
        Equip target = (Equip) eqpInv.getItem(dst);

        if (dst < 0) {
            return;
        }
        if (source == null) {
            return;
        }
        // Note: This check usually blocks swapping if target exists, ensuring dst is
        // empty.
        if (target != null && src <= 0) {
            c.sendPacket(PacketCreator.getInventoryFull());
            return;
        }

        eqpdInv.lockInventory();
        try {
            if (source.getRingId() > -1) {
                chr.getRingById(source.getRingId()).unequip();
            }
            chr.unequippedItem(source);
            eqpdInv.removeSlot(src);
        } finally {
            eqpdInv.unlockInventory();
        }

        if (target != null) {
            eqpInv.removeSlot(dst);
        }

        // ========================================================================
        // [START] Remove Passive Stats from Medal (Keep DB clean)
        // ========================================================================
        if (src == -49) {
            source.setStr((short) Math.max(0, source.getStr() - chr.getPassiveStr()));
            source.setDex((short) Math.max(0, source.getDex() - chr.getPassiveDex()));
            source.setInt((short) Math.max(0, source.getInt() - chr.getPassiveInt()));
            source.setLuk((short) Math.max(0, source.getLuk() - chr.getPassiveLuk()));
            source.setWatk((short) Math.max(0, source.getWatk() - chr.getPassiveWatk()));
            source.setMatk((short) Math.max(0, source.getMatk() - chr.getPassiveMatk()));
            source.setWdef((short) Math.max(0, source.getWdef() - chr.getPassiveWdef()));
            source.setMdef((short) Math.max(0, source.getMdef() - chr.getPassiveMdef()));
            source.setAcc((short) Math.max(0, source.getAcc() - chr.getPassiveAcc()));
            source.setAvoid((short) Math.max(0, source.getAvoid() - chr.getPassiveEva()));
            source.setSpeed((short) Math.max(0, source.getSpeed() - chr.getPassiveSpeed()));
            source.setJump((short) Math.max(0, source.getJump() - chr.getPassiveJump()));
        }
        // ========================================================================
        // [END] Remove Passive Stats from Medal
        // ========================================================================

        // Update position object to new Inventory slot
        source.setPosition(dst);
        eqpInv.addItemFromDB(source);

        if (target != null) {
            target.setPosition(src);
            eqpdInv.addItemFromDB(target);
        }

        // ========================================================================
        // [FIX START] Handle Medal Visuals (Remove + Add to clear spoofed stats)
        // ========================================================================
        List<ModifyInventory> modsList = new ArrayList<>();

        if (src == -49) { // If unequipped item is a Medal
            // 1. Packet: Remove from Equip Slot (-49)
            // We create a clone because 'source' has already been updated to 'dst' above.
            // The client needs to know which slot to remove FROM (-49).
            Item removeClone = source.copy();
            removeClone.setPosition(src);
            modsList.add(new ModifyInventory(3, removeClone));

            // 2. Packet: Add to Inventory Slot (dst)
            // This triggers Mode 0 -> calls addItemInfo.
            // Since source.getPosition() is now positive (dst), addItemInfo writes CLEAN
            // stats.
            modsList.add(new ModifyInventory(0, source));
        } else {
            // Standard Move for all other items
            modsList.add(new ModifyInventory(2, source, src));
        }

        c.sendPacket(PacketCreator.modifyInventory(true, modsList, c.getPlayer()));
        // ========================================================================
        // [FIX END]
        // ========================================================================

        chr.equipChanged();

        // Force Stat Recalculation if Medal was unequipped
        if (src == -49) {
            chr.recalcLocalStats();
            c.sendPacket(PacketCreator.enableActions());
        }
    }

    private static boolean isDisappearingItemDrop(Item it) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        if (ii.isDropRestricted(it.getItemId())) {
            return true;
        } else if (ii.isCash(it.getItemId())) {
            if (YamlConfig.config.server.USE_ENFORCE_UNMERCHABLE_CASH) { // thanks Ari for noticing cash drops not
                                                                         // available server-side
                return true;
            } else {
                return ItemConstants.isPet(it.getItemId()) && YamlConfig.config.server.USE_ENFORCE_UNMERCHABLE_PET;
            }
        } else if (isDroppedItemRestricted(it)) {
            return true;
        } else {
            return ItemId.isWeddingRing(it.getItemId());
        }
    }

    public static void drop(Client c, InventoryType type, short src, short quantity) {
        if (src < 0) {
            type = InventoryType.EQUIPPED;
        }

        Character chr = c.getPlayer();
        Inventory inv = chr.getInventory(type);
        Item source = inv.getItem(src);

        if (chr.isGM() && chr.gmLevel() < YamlConfig.config.server.MINIMUM_GM_LEVEL_TO_DROP) {
            chr.message("You cannot drop items at your GM level.");
            log.info("GM %s tried to drop item id %d", chr.getName(), source.getItemId());
            return;
        }

        if (chr.getTrade() != null || chr.getMiniGame() != null || source == null) { // Only check needed would prob be
                                                                                     // merchants (to see if the player
                                                                                     // is in one)
            return;
        }
        int itemId = source.getItemId();

        MapleMap map = chr.getMap();
        if ((!ItemConstants.isRechargeable(itemId) && source.getQuantity() < quantity) || quantity < 0) {
            return;
        }

        int petid = source.getPetId();
        if (petid > -1) {
            int petIdx = chr.getPetIndex(petid);
            if (petIdx > -1) {
                Pet pet = chr.getPet(petIdx);
                chr.unequipPet(pet, true);
            }
        }

        Point dropPos = new Point(chr.getPosition());
        if (quantity < source.getQuantity() && !ItemConstants.isRechargeable(itemId)) {
            Item target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            // [MODIFIED] Added c.getPlayer()
            c.sendPacket(PacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(1, source)),
                    c.getPlayer()));

            if (ItemConstants.isNewYearCardEtc(itemId)) {
                if (itemId == ItemId.NEW_YEARS_CARD_SEND) {
                    NewYearCardRecord.removeAllNewYearCard(true, chr);
                    c.getAbstractPlayerInteraction().removeAll(ItemId.NEW_YEARS_CARD_SEND);
                } else {
                    NewYearCardRecord.removeAllNewYearCard(false, chr);
                    c.getAbstractPlayerInteraction().removeAll(ItemId.NEW_YEARS_CARD_RECEIVED);
                }
            }

            if (isDisappearingItemDrop(target)) {
                map.disappearingItemDrop(chr, chr, target, dropPos);
            } else {
                map.spawnItemDrop(chr, chr, target, dropPos, true, true);
            }
        } else {
            if (type == InventoryType.EQUIPPED) {
                inv.lockInventory();
                try {
                    chr.unequippedItem((Equip) source);
                    inv.removeSlot(src);
                } finally {
                    inv.unlockInventory();
                }
            } else {
                inv.removeSlot(src);
            }

            // [MODIFIED] Added c.getPlayer()
            c.sendPacket(PacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(3, source)),
                    c.getPlayer()));
            if (src < 0) {
                chr.equipChanged();
            } else if (ItemConstants.isNewYearCardEtc(itemId)) {
                if (itemId == ItemId.NEW_YEARS_CARD_SEND) {
                    NewYearCardRecord.removeAllNewYearCard(true, chr);
                    c.getAbstractPlayerInteraction().removeAll(ItemId.NEW_YEARS_CARD_SEND);
                } else {
                    NewYearCardRecord.removeAllNewYearCard(false, chr);
                    c.getAbstractPlayerInteraction().removeAll(ItemId.NEW_YEARS_CARD_RECEIVED);
                }
            }

            if (isDisappearingItemDrop(source)) {
                map.disappearingItemDrop(chr, chr, source, dropPos);
            } else {
                map.spawnItemDrop(chr, chr, source, dropPos, true, true);
            }
        }

        int quantityNow = chr.getItemQuantity(itemId, false);
        if (itemId == chr.getItemEffect()) {
            if (quantityNow <= 0) {
                chr.setItemEffect(0);
                map.broadcastMessage(PacketCreator.itemEffect(chr.getId(), 0));
            }
        } else if (itemId == ItemId.CHALKBOARD_1 || itemId == ItemId.CHALKBOARD_2) {
            if (source.getQuantity() <= 0) {
                chr.setChalkboard(null);
            }
        } else if (itemId == ItemId.ARPQ_SPIRIT_JEWEL) {
            chr.updateAriantScore(quantityNow);
        }
    }

    private static boolean isDroppedItemRestricted(Item it) {
        return YamlConfig.config.server.USE_ERASE_UNTRADEABLE_DROP && it.isUntradeable();
    }

    public static boolean isSandboxItem(Item it) {
        return (it.getFlag() & ItemConstants.SANDBOX) == ItemConstants.SANDBOX;
    }
}
