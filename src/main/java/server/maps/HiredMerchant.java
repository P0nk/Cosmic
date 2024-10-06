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
import client.inventory.ItemFactory;
import client.inventory.manipulator.InventoryManipulator;
import client.inventory.manipulator.KarmaManipulator;
import client.processor.npc.FredrickProcessor;
import config.YamlConfig;
import database.character.CharacterSaver;
import net.packet.Packet;
import net.server.Server;
import server.ItemInformationProvider;
import server.Trade;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author XoticStory
 * @author Ronan - concurrency protection
 */
public class HiredMerchant extends AbstractMapObject {
    private static final int VISITOR_HISTORY_LIMIT = 10;
    private static final int BLACKLIST_LIMIT = 20;

    private final int ownerId;
    private final int itemId;
    private final int mesos = 0;
    private final int channel;
    private final int world;
    private final long start;
    private String ownerName = "";
    private String description = "";
    private final List<PlayerShopItem> items = new LinkedList<>();
    private final List<Pair<String, Byte>> messages = new LinkedList<>();
    private final List<SoldItem> sold = new LinkedList<>();
    private final AtomicBoolean open = new AtomicBoolean();
    private boolean published = false;
    private MapleMap map;
    private final Visitor[] visitors = new Visitor[3];
    private final LinkedList<PastVisitor> visitorHistory = new LinkedList<>();
    private final LinkedHashSet<String> blacklist = new LinkedHashSet<>(); // case-sensitive character names
    private final Lock visitorLock = new ReentrantLock(true);

    private record Visitor(Character chr, Instant enteredAt) {}

    public record PastVisitor(String chrName, Duration visitDuration) {}

    public HiredMerchant(final Character owner, String desc, int itemId) {
        this.setPosition(owner.getPosition());
        this.start = System.currentTimeMillis();
        this.ownerId = owner.getId();
        this.channel = owner.getClient().getChannel();
        this.world = owner.getWorld();
        this.itemId = itemId;
        this.ownerName = owner.getName();
        this.description = desc;
        this.map = owner.getMap();
    }

    public void broadcastToVisitorsThreadsafe(Packet packet) {
        visitorLock.lock();
        try {
            broadcastToVisitors(packet);
        } finally {
            visitorLock.unlock();
        }
    }

    private void broadcastToVisitors(Packet packet) {
        for (Visitor visitor : visitors) {
            if (visitor != null) {
                visitor.chr.sendPacket(packet);
            }
        }
    }

    public byte[] getShopRoomInfo() {
        visitorLock.lock();
        try {
            byte count = 0;
            if (this.isOpen()) {
                for (Visitor visitor : visitors) {
                    if (visitor != null) {
                        count++;
                    }
                }
            } else {
                count = (byte) (visitors.length + 1);
            }

            return new byte[]{count, (byte) (visitors.length + 1)};
        } finally {
            visitorLock.unlock();
        }
    }

    public boolean addVisitor(Character visitor) {
        visitorLock.lock();
        try {
            int i = this.getFreeSlot();
            if (i > -1) {
                visitors[i] = new Visitor(visitor, Instant.now());
                broadcastToVisitors(PacketCreator.hiredMerchantVisitorAdd(visitor, i + 1));
                this.getMap().broadcastMessage(PacketCreator.updateHiredMerchantBox(this));

                return true;
            }

            return false;
        } finally {
            visitorLock.unlock();
        }
    }

    public void removeVisitor(Character chr) {
        visitorLock.lock();
        try {
            int slot = getVisitorSlot(chr);
            if (slot < 0) { //Not found
                return;
            }

            Visitor visitor = visitors[slot];
            if (visitor != null && visitor.chr.getId() == chr.getId()) {
                visitors[slot] = null;
                addVisitorToHistory(visitor);
                broadcastToVisitors(PacketCreator.hiredMerchantVisitorLeave(slot + 1));
                this.getMap().broadcastMessage(PacketCreator.updateHiredMerchantBox(this));
            }
        } finally {
            visitorLock.unlock();
        }
    }

    private void addVisitorToHistory(Visitor visitor) {
        Duration visitDuration = Duration.between(visitor.enteredAt, Instant.now());
        visitorHistory.addFirst(new PastVisitor(visitor.chr.getName(), visitDuration));
        while (visitorHistory.size() > VISITOR_HISTORY_LIMIT) {
            visitorHistory.removeLast();
        }
    }

    public int getVisitorSlotThreadsafe(Character visitor) {
        visitorLock.lock();
        try {
            return getVisitorSlot(visitor);
        } finally {
            visitorLock.unlock();
        }
    }

    private int getVisitorSlot(Character visitor) {
        for (int i = 0; i < 3; i++) {
            if (visitors[i] != null && visitors[i].chr.getId() == visitor.getId()) {
                return i;
            }
        }
        return -1; //Actually 0 because of the +1's.
    }

    private void removeAllVisitors() {
        visitorLock.lock();
        try {
            for (int i = 0; i < 3; i++) {
                Visitor visitor = visitors[i];

                if (visitor != null) {
                    final Character visitorChr = visitor.chr;
                    visitorChr.setHiredMerchant(null);
                    visitorChr.sendPacket(PacketCreator.leaveHiredMerchant(i + 1, 0x11));
                    visitorChr.sendPacket(PacketCreator.hiredMerchantMaintenanceMessage());
                    visitors[i] = null;
                    addVisitorToHistory(visitor);
                }
            }

            this.getMap().broadcastMessage(PacketCreator.updateHiredMerchantBox(this));
        } finally {
            visitorLock.unlock();
        }
    }

    private void removeOwner(Character owner) {
        if (owner.getHiredMerchant() == this) {
            owner.sendPacket(PacketCreator.hiredMerchantOwnerLeave());
            owner.sendPacket(PacketCreator.leaveHiredMerchant(0x00, 0x03));
            owner.setHiredMerchant(null);
        }
    }

    public void withdrawMesos(Character chr) {
        if (isOwner(chr)) {
            synchronized (items) {
                chr.withdrawMerchantMesos();
            }
        }
    }

    public void takeItemBack(int slot, Character chr, CharacterSaver chrSaver) {
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
                chr.sendPacket(PacketCreator.updateHiredMerchant(this, chr));
            }
        }
    }

    private static boolean canBuy(Client c, Item newItem) {    // thanks xiaokelvin (Conrad) for noticing a leaked test code here
        return InventoryManipulator.checkSpace(c, newItem.getItemId(), newItem.getQuantity(), newItem.getOwner()) && InventoryManipulator.addFromDrop(c, newItem, false);
    }

    private int getQuantityLeft(int itemid) {
        synchronized (items) {
            int count = 0;

            for (PlayerShopItem mpsi : items) {
                if (mpsi.getItem().getItemId() == itemid) {
                    count += (mpsi.getBundles() * mpsi.getItem().getQuantity());
                }
            }

            return count;
        }
    }

    public void buy(Client c, int item, short quantity) {
        synchronized (items) {
            PlayerShopItem pItem = items.get(item);
            Item newItem = pItem.getItem().copy();

            newItem.setQuantity((short) ((pItem.getItem().getQuantity() * quantity)));
            if (quantity < 1 || !pItem.isExist() || pItem.getBundles() < quantity) {
                c.sendPacket(PacketCreator.enableActions());
                return;
            } else if (newItem.getInventoryType().equals(InventoryType.EQUIP) && newItem.getQuantity() > 1) {
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            KarmaManipulator.toggleKarmaFlagToUntradeable(newItem);

            int price = (int) Math.min((float) pItem.getPrice() * quantity, Integer.MAX_VALUE);
            if (c.getPlayer().getMeso() >= price) {
                if (canBuy(c, newItem)) {
                    c.getPlayer().gainMeso(-price, false);
                    price -= Trade.getFee(price);  // thanks BHB for pointing out trade fees not applying here

                    synchronized (sold) {
                        sold.add(new SoldItem(c.getPlayer().getName(), pItem.getItem().getItemId(), newItem.getQuantity(), price));
                    }

                    pItem.setBundles((short) (pItem.getBundles() - quantity));
                    if (pItem.getBundles() < 1) {
                        pItem.setDoesExist(false);
                    }

                    if (YamlConfig.config.server.USE_ANNOUNCE_SHOPITEMSOLD) {   // idea thanks to Vcoc
                        announceItemSold(newItem, price, getQuantityLeft(pItem.getItem().getItemId()));
                    }

                    Character owner = Server.getInstance().getWorld(world).getPlayerStorage().getCharacterByName(ownerName);
                    if (owner != null) {
                        owner.addMerchantMesos(price);
                    } else {
                        try (Connection con = DatabaseConnection.getConnection()) {
                            long merchantMesos = 0;
                            try (PreparedStatement ps = con.prepareStatement("SELECT MerchantMesos FROM characters WHERE id = ?")) {
                                ps.setInt(1, ownerId);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        merchantMesos = rs.getInt(1);
                                    }
                                }
                            }
                            merchantMesos += price;

                            try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET MerchantMesos = ? WHERE id = ?", PreparedStatement.RETURN_GENERATED_KEYS)) {
                                ps.setInt(1, (int) Math.min(merchantMesos, Integer.MAX_VALUE));
                                ps.setInt(2, ownerId);
                                ps.executeUpdate();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    c.getPlayer().dropMessage(1, "Your inventory is full. Please clear a slot before buying this item.");
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }
            } else {
                c.getPlayer().dropMessage(1, "You don't have enough mesos to purchase this item.");
                c.sendPacket(PacketCreator.enableActions());
                return;
            }
            try {
                this.saveItems(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void announceItemSold(Item item, int mesos, int inStore) {
        String qtyStr = (item.getQuantity() > 1) ? " x " + item.getQuantity() : "";

        Character player = Server.getInstance().getWorld(world).getPlayerStorage().getCharacterById(ownerId);
        if (player != null && player.isLoggedinWorld()) {
            player.dropMessage(6, "[Hired Merchant] Item '" + ItemInformationProvider.getInstance().getName(item.getItemId()) + "'" + qtyStr + " has been sold for " + mesos + " mesos. (" + inStore + " left)");
        }
    }

    public void forceClose() {
        //Server.getInstance().getChannel(world, channel).removeHiredMerchant(ownerId);
        map.broadcastMessage(PacketCreator.removeHiredMerchantBox(getOwnerId()));
        map.removeMapObject(this);

        Character owner = Server.getInstance().getWorld(world).getPlayerStorage().getCharacterById(ownerId);

        visitorLock.lock();
        try {
            setOpen(false);
            removeAllVisitors();

            if (owner != null && owner.isLoggedinWorld() && this == owner.getHiredMerchant()) {
                closeOwnerMerchant(owner);
            }
        } finally {
            visitorLock.unlock();
        }

        Server.getInstance().getWorld(world).unregisterHiredMerchant(this);

        try {
            saveItems(true);
            synchronized (items) {
                items.clear();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        Character player = Server.getInstance().getWorld(world).getPlayerStorage().getCharacterById(ownerId);
        if (player != null) {
            player.setHasMerchant(false);
        } else {
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE characters SET HasMerchant = 0 WHERE id = ?", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, ownerId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        map = null;
    }

    public void closeOwnerMerchant(Character chr) {
        if (this.isOwner(chr)) {
            this.closeShop(chr.getClient(), false);
            chr.setHasMerchant(false);
        }
    }

    private void closeShop(Client c, boolean timeout) {
        map.removeMapObject(this);
        map.broadcastMessage(PacketCreator.removeHiredMerchantBox(ownerId));
        c.getChannelServer().removeHiredMerchant(ownerId);

        this.removeAllVisitors();
        this.removeOwner(c.getPlayer());

        try {
            List<PlayerShopItem> copyItems = getItems();
            if (check(c.getPlayer(), copyItems) && !timeout) {
                for (PlayerShopItem mpsi : copyItems) {
                    if (mpsi.isExist()) {
                        if (mpsi.getItem().getInventoryType().equals(InventoryType.EQUIP)) {
                            InventoryManipulator.addFromDrop(c, mpsi.getItem(), false);
                        } else {
                            InventoryManipulator.addById(c, mpsi.getItem().getItemId(), (short) (mpsi.getBundles() * mpsi.getItem().getQuantity()), mpsi.getItem().getOwner(), -1, mpsi.getItem().getFlag(), mpsi.getItem().getExpiration());
                        }
                    }
                }

                synchronized (items) {
                    items.clear();
                }
            }

            try {
                this.saveItems(timeout);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // thanks Rohenn for noticing a possible dupe scenario on closing shop
            Character player = c.getWorldServer().getPlayerStorage().getCharacterById(ownerId);
            if (player != null) {
                player.setHasMerchant(false);
            } else {
                try (Connection con = DatabaseConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement("UPDATE characters SET HasMerchant = 0 WHERE id = ?", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, ownerId);
                    ps.executeUpdate();
                }
            }

            synchronized (items) {
                items.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Server.getInstance().getWorld(world).unregisterHiredMerchant(this);
    }

    public synchronized void visitShop(Character chr) {
        visitorLock.lock();
        try {
            if (this.isOwner(chr)) {
                this.setOpen(false);
                this.removeAllVisitors();

                chr.sendPacket(PacketCreator.getHiredMerchant(chr, this, false));
            } else if (!this.isOpen()) {
                chr.sendPacket(PacketCreator.getMiniRoomError(18));
                return;
            } else if (isBlacklisted(chr.getName())) {
                chr.sendPacket(PacketCreator.getMiniRoomError(17));
                return;
            } else if (!this.addVisitor(chr)) {
                chr.sendPacket(PacketCreator.getMiniRoomError(2));
                return;
            } else {
                chr.sendPacket(PacketCreator.getHiredMerchant(chr, this, false));
            }
            chr.setHiredMerchant(this);
        } finally {
            visitorLock.unlock();
        }
    }

    public String getOwner() {
        return ownerName;
    }

    public void clearItems() {
        synchronized (items) {
            items.clear();
        }
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getDescription() {
        return description;
    }

    public Character[] getVisitorCharacters() {
        visitorLock.lock();
        try {
            Character[] copy = new Character[3];
            for (int i = 0; i < visitors.length; i++) {
                Visitor visitor = visitors[i];
                if (visitor != null) {
                    copy[i] = visitor.chr;
                }
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

    public boolean addItem(PlayerShopItem item) {
        synchronized (items) {
            if (items.size() >= 16) {
                return false;
            }

            items.add(item);
            return true;
        }
    }

    public void clearInexistentItems() {
        synchronized (items) {
            for (int i = items.size() - 1; i >= 0; i--) {
                if (!items.get(i).isExist()) {
                    items.remove(i);
                }
            }

            try {
                this.saveItems(false);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void removeFromSlot(int slot) {
        items.remove(slot);

        try {
            this.saveItems(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private int getFreeSlot() {
        for (int i = 0; i < 3; i++) {
            if (visitors[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isOpen() {
        return open.get();
    }

    public void setOpen(boolean set) {
        open.getAndSet(set);
        published = true;
    }

    public int getItemId() {
        return itemId;
    }

    public boolean isOwner(Character chr) {
        return chr.getId() == ownerId;
    }

    public void sendMessage(Character chr, String msg) {
        String message = chr.getName() + " : " + msg;
        byte slot = (byte) (getVisitorSlot(chr) + 1);

        synchronized (messages) {
            messages.add(new Pair<>(message, slot));
        }
        broadcastToVisitorsThreadsafe(PacketCreator.hiredMerchantChat(message, slot));
    }

    public List<PlayerShopItem> sendAvailableBundles(int itemid) {
        List<PlayerShopItem> list = new LinkedList<>();
        List<PlayerShopItem> all = new ArrayList<>();

        if (!open.get()) {
            return list;
        }

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

    public void saveItems(boolean shutdown) throws SQLException {
        List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();
        List<Short> bundles = new ArrayList<>();

        for (PlayerShopItem pItems : getItems()) {
            Item newItem = pItems.getItem();
            short newBundle = pItems.getBundles();

            if (shutdown) { //is "shutdown" really necessary?
                newItem.setQuantity(pItems.getItem().getQuantity());
            } else {
                newItem.setQuantity(pItems.getItem().getQuantity());
            }
            if (newBundle > 0) {
                itemsWithType.add(new Pair<>(newItem, newItem.getInventoryType()));
                bundles.add(newBundle);
            }
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            ItemFactory.MERCHANT.saveItems(itemsWithType, bundles, this.ownerId, con);
        }

        FredrickProcessor.insertFredrickLog(this.ownerId);
    }

    private static boolean check(Character chr, List<PlayerShopItem> items) {
        List<Pair<Item, InventoryType>> li = new ArrayList<>();
        for (PlayerShopItem item : items) {
            Item it = item.getItem().copy();
            it.setQuantity((short) (it.getQuantity() * item.getBundles()));

            li.add(new Pair<>(it, it.getInventoryType()));
        }

        return Inventory.checkSpotsAndOwnership(chr, li);
    }

    public int getChannel() {
        return channel;
    }

    public int getTimeOpen() {
        double openTime = (System.currentTimeMillis() - start) / 60000;
        openTime /= 1440;   // heuristics since engineered method to count time here is unknown
        openTime *= 1318;

        return (int) Math.ceil(openTime);
    }

    public void clearMessages() {
        synchronized (messages) {
            messages.clear();
        }
    }

    public List<Pair<String, Byte>> getMessages() {
        synchronized (messages) {
            List<Pair<String, Byte>> msgList = new LinkedList<>();
            msgList.addAll(messages);

            return msgList;
        }
    }

    public List<PastVisitor> getVisitorHistory() {
        return Collections.unmodifiableList(visitorHistory);
    }

    public void addToBlacklist(String chrName) {
        visitorLock.lock();
        try {
            if (blacklist.size() >= BLACKLIST_LIMIT) {
                return;
            }
            blacklist.add(chrName);
        } finally {
            visitorLock.unlock();
        }
    }

    public void removeFromBlacklist(String chrName) {
        visitorLock.lock();
        try {
            blacklist.remove(chrName);
        } finally {
            visitorLock.unlock();
        }
    }

    public Set<String> getBlacklist() {
        return Collections.unmodifiableSet(blacklist);
    }

    private boolean isBlacklisted(String chrName) {
        visitorLock.lock();
        try {
            return blacklist.contains(chrName);
        } finally {
            visitorLock.unlock();
        }
    }

    public int getMapId() {
        return map.getId();
    }

    public MapleMap getMap() {
        return map;
    }

    public List<SoldItem> getSold() {
        synchronized (sold) {
            return Collections.unmodifiableList(sold);
        }
    }

    public int getMesos() {
        return mesos;
    }

    @Override
    public MapObjectType getType() {
        return MapObjectType.HIRED_MERCHANT;
    }

    @Override
    public void sendDestroyData(Client client) {}

    @Override
    public void sendSpawnData(Client client) {
        client.sendPacket(PacketCreator.spawnHiredMerchantBox(this));
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
