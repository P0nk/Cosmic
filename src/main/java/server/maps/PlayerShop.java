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
package server.maps;

import client.Character;
import client.Client;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import client.inventory.manipulator.KarmaManipulator;
import net.packet.Packet;
import server.Trade;
import tools.PacketCreator;
import tools.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Matze
 * @author Ronan - concurrency protection
 */
public class PlayerShop extends AbstractMapObject {
    private final AtomicBoolean open = new AtomicBoolean(false);
    private final Character owner;
    private final int itemid;

    private final Character[] visitors = new Character[3];
    private final List<PlayerShopItem> items = new ArrayList<>();
    private final List<SoldItem> sold = new LinkedList<>();
    private String description;
    private int boughtnumber = 0;
    private final List<String> bannedList = new ArrayList<>();
    private final List<Pair<Character, String>> chatLog = new LinkedList<>();
    private final Map<Integer, Byte> chatSlot = new LinkedHashMap<>();
    private final Lock visitorLock = new ReentrantLock(true);

    public PlayerShop(Character owner, String description, int itemid) {
        this.setPosition(owner.getPosition());
        this.owner = owner;
        this.description = description;
        this.itemid = itemid;
    }

    public int getChannel() {
        return owner.getClient().getChannel();
    }

    public int getMapId() {
        return owner.getMapId();
    }

    public int getItemId() {
        return itemid;
    }

    public boolean isOpen() {
        return open.get();
    }

    public void setOpen(boolean openShop) {
        open.set(openShop);
    }

    public boolean hasFreeSlot() {
        visitorLock.lock();
        try {
            return visitors[0] == null || visitors[1] == null || visitors[2] == null;
        } finally {
            visitorLock.unlock();
        }
    }

    public byte[] getShopRoomInfo() {
        visitorLock.lock();
        try {
            byte count = 0;
            //if (this.isOpen()) {
            for (Character visitor : visitors) {
                if (visitor != null) {
                    count++;
                }
            }
            //} else {  shouldn't happen since there isn't a "closed" state for player shops.
            //    count = (byte) (visitors.length + 1);
            //}

            return new byte[]{count, (byte) visitors.length};
        } finally {
            visitorLock.unlock();
        }
    }

    public boolean isOwner(Character chr) {
        return owner.equals(chr);
    }

    private void addVisitor(Character visitor) {
        for (int i = 0; i < 3; i++) {
            if (visitors[i] == null) {
                visitors[i] = visitor;
                visitor.setSlot(i);

                this.broadcast(PacketCreator.getPlayerShopNewVisitor(visitor, i + 1));
                owner.getMap().broadcastMessage(PacketCreator.updatePlayerShopBox(this));
                break;
            }
        }
    }

    public void forceRemoveVisitor(Character visitor) {
        if (visitor == owner) {
            owner.getMap().removeMapObject(this);
            owner.setPlayerShop(null);
        }

        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null && visitors[i].getId() == visitor.getId()) {
                    visitors[i].setPlayerShop(null);
                    visitors[i] = null;
                    visitor.setSlot(-1);

                    this.broadcast(PacketCreator.getPlayerShopRemoveVisitor(i + 1));
                    owner.getMap().broadcastMessage(PacketCreator.updatePlayerShopBox(this));
                    return;
                }
            }
        } finally {
            visitorLock.unlock();
        }
    }

    public void removeVisitor(Character visitor) {
        if (visitor == owner) {
            owner.getMap().removeMapObject(this);
            owner.setPlayerShop(null);
        } else {
            visitorLock.lock();
            try {
                for (int i = 0; i < 3; i++) {
                    if (visitors[i] != null && visitors[i].getId() == visitor.getId()) {
                        visitor.setSlot(-1);    //absolutely cant remove player slot for late players without dc'ing them... heh

                        for (int j = i; j < 2; j++) {
                            if (visitors[j] != null) {
                                owner.sendPacket(PacketCreator.getPlayerShopRemoveVisitor(j + 1));
                            }
                            visitors[j] = visitors[j + 1];
                            if (visitors[j] != null) {
                                visitors[j].setSlot(j);
                            }
                        }
                        visitors[2] = null;
                        for (int j = i; j < 2; j++) {
                            if (visitors[j] != null) {
                                owner.sendPacket(PacketCreator.getPlayerShopNewVisitor(visitors[j], j + 1));
                            }
                        }

                        this.broadcastRestoreToVisitors();
                        owner.getMap().broadcastMessage(PacketCreator.updatePlayerShopBox(this));
                        return;
                    }
                }
            } finally {
                visitorLock.unlock();
            }

            owner.getMap().broadcastMessage(PacketCreator.updatePlayerShopBox(this));
        }
    }

    public boolean isVisitor(Character visitor) {
        visitorLock.lock();
        try {
            return visitors[0] == visitor || visitors[1] == visitor || visitors[2] == visitor;
        } finally {
            visitorLock.unlock();
        }
    }

    public boolean addItem(PlayerShopItem item) {
        synchronized (items) {
            if (items.size() >= 16) {
                return false;
            }

            items.add(item);
            return true;
        }
    }

    private void removeFromSlot(int slot) {
        items.remove(slot);
    }

    private static boolean canBuy(Client c, Item newItem) {
        return InventoryManipulator.checkSpace(c, newItem.getItemId(), newItem.getQuantity(), newItem.getOwner()) && InventoryManipulator.addFromDrop(c, newItem, false);
    }

    public void takeItemBack(int slot, Character chr) {
        synchronized (items) {
            PlayerShopItem shopItem = items.get(slot);
            if (shopItem.isExist()) {
                if (shopItem.getBundles() > 0) {
                    Item iitem = shopItem.getItem().copy();
                    iitem.setQuantity((short) (shopItem.getItem().getQuantity() * shopItem.getBundles()));

                    if (!Inventory.checkSpot(chr, iitem)) {
                        chr.sendPacket(PacketCreator.serverNotice(1, "Have a slot available on your inventory to claim back the item."));
                        chr.sendPacket(PacketCreator.enableActions());
                        return;
                    }

                    InventoryManipulator.addFromDrop(chr.getClient(), iitem, true);
                }

                removeFromSlot(slot);
                chr.sendPacket(PacketCreator.getPlayerShopItemUpdate(this));
            }
        }
    }

    /**
     * no warnings for now o.o
     *
     * @param c
     * @param item
     * @param quantity
     */
    public boolean buy(Client c, int item, short quantity) {
        synchronized (items) {
            if (isVisitor(c.getPlayer())) {
                PlayerShopItem pItem = items.get(item);
                Item newItem = pItem.getItem().copy();

                newItem.setQuantity((short) ((pItem.getItem().getQuantity() * quantity)));
                if (quantity < 1 || !pItem.isExist() || pItem.getBundles() < quantity) {
                    c.sendPacket(PacketCreator.enableActions());
                    return false;
                } else if (newItem.getInventoryType().equals(InventoryType.EQUIP) && newItem.getQuantity() > 1) {
                    c.sendPacket(PacketCreator.enableActions());
                    return false;
                }

                KarmaManipulator.toggleKarmaFlagToUntradeable(newItem);

                visitorLock.lock();
                try {
                    int price = (int) Math.min((float) pItem.getPrice() * quantity, Integer.MAX_VALUE);

                    if (c.getPlayer().getMeso() >= price) {
                        if (!owner.canHoldMeso(price)) {    // thanks Rohenn for noticing owner hold check misplaced
                            c.getPlayer().dropMessage(1, "Transaction failed since the shop owner can't hold any more mesos.");
                            c.sendPacket(PacketCreator.enableActions());
                            return false;
                        }

                        if (canBuy(c, newItem)) {
                            c.getPlayer().gainMeso(-price, false);
                            price -= Trade.getFee(price);  // thanks BHB for pointing out trade fees not applying here
                            owner.gainMeso(price, true);

                            SoldItem soldItem = new SoldItem(c.getPlayer().getName(), pItem.getItem().getItemId(), quantity, price);
                            owner.sendPacket(PacketCreator.getPlayerShopOwnerUpdate(soldItem, item));

                            synchronized (sold) {
                                sold.add(soldItem);
                            }

                            pItem.setBundles((short) (pItem.getBundles() - quantity));
                            if (pItem.getBundles() < 1) {
                                pItem.setDoesExist(false);
                                if (++boughtnumber == items.size()) {
                                    owner.setPlayerShop(null);
                                    this.setOpen(false);
                                    this.closeShop();
                                    owner.dropMessage(1, "Your items are sold out, and therefore your shop is closed.");
                                }
                            }
                        } else {
                            c.getPlayer().dropMessage(1, "Your inventory is full. Please clear a slot before buying this item.");
                            c.sendPacket(PacketCreator.enableActions());
                            return false;
                        }
                    } else {
                        c.getPlayer().dropMessage(1, "You don't have enough mesos to purchase this item.");
                        c.sendPacket(PacketCreator.enableActions());
                        return false;
                    }

                    return true;
                } finally {
                    visitorLock.unlock();
                }
            } else {
                return false;
            }
        }
    }

    public void broadcastToVisitors(Packet packet) {
        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null) {
                    visitors[i].sendPacket(packet);
                }
            }
        } finally {
            visitorLock.unlock();
        }
    }

    public void broadcastRestoreToVisitors() {
        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null) {
                    visitors[i].sendPacket(PacketCreator.getPlayerShopRemoveVisitor(i + 1));
                }
            }

            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null) {
                    visitors[i].sendPacket(PacketCreator.getPlayerShop(this, false));
                }
            }

            recoverChatLog();
        } finally {
            visitorLock.unlock();
        }
    }

    public void removeVisitors() {
        List<Character> visitorList = new ArrayList<>(3);

        visitorLock.lock();
        try {
            try {
                for (int i = 0; i < 3; i++) {
                    if (visitors[i] != null) {
                        visitors[i].sendPacket(PacketCreator.shopErrorMessage(10, 1));
                        visitorList.add(visitors[i]);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            visitorLock.unlock();
        }

        for (Character mc : visitorList) {
            forceRemoveVisitor(mc);
        }
        if (owner != null) {
            forceRemoveVisitor(owner);
        }
    }

    public void broadcast(Packet packet) {
        Client client = owner.getClient();
        if (client != null) {
            client.sendPacket(packet);
        }
        broadcastToVisitors(packet);
    }

    private byte getVisitorSlot(Character chr) {
        byte s = 0;
        for (Character mc : getVisitors()) {
            s++;
            if (mc != null) {
                if (mc.getName().equalsIgnoreCase(chr.getName())) {
                    break;
                }
            } else if (s == 3) {
                s = 0;
            }
        }

        return s;
    }

    public void chat(Client c, String chat) {
        byte s = getVisitorSlot(c.getPlayer());

        synchronized (chatLog) {
            chatLog.add(new Pair<>(c.getPlayer(), chat));
            if (chatLog.size() > 25) {
                chatLog.remove(0);
            }
            chatSlot.put(c.getPlayer().getId(), s);
        }

        broadcast(PacketCreator.getPlayerShopChat(c.getPlayer(), chat, s));
    }

    private void recoverChatLog() {
        synchronized (chatLog) {
            for (Pair<Character, String> it : chatLog) {
                Character chr = it.getLeft();
                Byte pos = chatSlot.get(chr.getId());

                broadcastToVisitors(PacketCreator.getPlayerShopChat(chr, it.getRight(), pos));
            }
        }
    }

    private void clearChatLog() {
        synchronized (chatLog) {
            chatLog.clear();
        }
    }

    public void closeShop() {
        clearChatLog();
        removeVisitors();
        owner.getMap().broadcastMessage(PacketCreator.removePlayerShopBox(this));
    }

    public void sendShop(Client c) {
        visitorLock.lock();
        try {
            c.sendPacket(PacketCreator.getPlayerShop(this, isOwner(c.getPlayer())));
        } finally {
            visitorLock.unlock();
        }
    }

    public Character getOwner() {
        return owner;
    }

    public Character[] getVisitors() {
        visitorLock.lock();
        try {
            Character[] copy = new Character[3];
            for (int i = 0; i < visitors.length; i++) {
                copy[i] = visitors[i];
            }

            return copy;
        } finally {
            visitorLock.unlock();
        }
    }

    public List<PlayerShopItem> getItems() {
        synchronized (items) {
            return Collections.unmodifiableList(items);
        }
    }

    public boolean hasItem(int itemid) {
        for (PlayerShopItem mpsi : getItems()) {
            if (mpsi.getItem().getItemId() == itemid && mpsi.isExist() && mpsi.getBundles() > 0) {
                return true;
            }
        }

        return false;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void banPlayer(String name) {
        if (!bannedList.contains(name)) {
            bannedList.add(name);
        }

        Character target = null;
        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                if (visitors[i] != null && visitors[i].getName().equals(name)) {
                    target = visitors[i];
                    break;
                }
            }
        } finally {
            visitorLock.unlock();
        }

        if (target != null) {
            target.sendPacket(PacketCreator.shopErrorMessage(5, 1));
            removeVisitor(target);
        }
    }

    public boolean isBanned(String name) {
        return bannedList.contains(name);
    }

    public synchronized boolean visitShop(Character chr) {
        if (this.isBanned(chr.getName())) {
            chr.dropMessage(1, "You have been banned from this store.");
            return false;
        }

        visitorLock.lock();
        try {
            if (!open.get()) {
                chr.dropMessage(1, "This store is not yet open.");
                return false;
            }

            if (this.hasFreeSlot() && !this.isVisitor(chr)) {
                this.addVisitor(chr);
                chr.setPlayerShop(this);
                this.sendShop(chr.getClient());

                return true;
            }

            return false;
        } finally {
            visitorLock.unlock();
        }
    }

    public List<PlayerShopItem> sendAvailableBundles(int itemid) {
        List<PlayerShopItem> list = new LinkedList<>();
        List<PlayerShopItem> all = new ArrayList<>();

        synchronized (items) {
            all.addAll(items);
        }

        for (PlayerShopItem mpsi : all) {
            if (mpsi.getItem().getItemId() == itemid && mpsi.getBundles() > 0 && mpsi.isExist()) {
                list.add(mpsi);
            }
        }
        return list;
    }

    public List<SoldItem> getSold() {
        synchronized (sold) {
            return Collections.unmodifiableList(sold);
        }
    }

    @Override
    public void sendDestroyData(Client client) {
        client.sendPacket(PacketCreator.removePlayerShopBox(this));
    }

    @Override
    public void sendSpawnData(Client client) {
        client.sendPacket(PacketCreator.updatePlayerShopBox(this));
    }

    @Override
    public MapObjectType getType() {
        return MapObjectType.SHOP;
    }

    public class SoldItem {

        int itemid, mesos;
        short quantity;
        String buyer;

        public SoldItem(String buyer, int itemid, short quantity, int mesos) {
            this.buyer = buyer;
            this.itemid = itemid;
            this.quantity = quantity;
            this.mesos = mesos;
        }

        public String getBuyer() {
            return buyer;
        }

        public int getItemId() {
            return itemid;
        }

        public short getQuantity() {
            return quantity;
        }

        public int getMesos() {
            return mesos;
        }
    }
}