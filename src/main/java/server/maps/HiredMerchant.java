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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.awt.Point;

/**
 * @author XoticStory
 * @author Ronan - concurrency protection
 */
public class HiredMerchant extends AbstractMapObject {
    private static final int VISITOR_HISTORY_LIMIT = 10;
    private static final int BLACKLIST_LIMIT = 20;

    private final int ownerId;
    private final int itemId;

    // [FIX] Changed to long to hold accumulated revenue in memory
    // This allows us to buffer sales without hitting the DB constantly
    private long mesos = 0;

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
    private final LinkedHashSet<String> blacklist = new LinkedHashSet<>();
    private final Lock visitorLock = new ReentrantLock(true);

    private record Visitor(Character chr, Instant enteredAt) {}

    public record PastVisitor(String chrName, Duration visitDuration) {}

    public HiredMerchant(int ownerId, String ownerName, int itemId, String desc, int world, int channel, MapleMap map, long startTime) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.itemId = itemId;
        this.description = desc;
        this.world = world;
        this.channel = channel;
        this.map = map;
        this.start = startTime;
        this.setPosition(new Point(0, 0));
    }

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

    // [DEBUG HELPER]
    private void debug(String msg) {
        // Since "console.log.println" is not standard Java, we use System.out.println
        // This will print to your server console/bat window.
        System.out.println("[HM-DEBUG][OwnerID:" + ownerId + "] " + msg);
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

    // [FIXED] Revenue Saving Logic with Debugs
    // Only called on close to prevent race conditions
    private void saveRevenueToDB() {
        if (this.mesos <= 0) {
            debug("saveRevenueToDB: No buffered revenue to save (Memory Mesos: " + this.mesos + ")");
            return;
        }

        debug("saveRevenueToDB: START. Accumulated in Memory: " + this.mesos);

        final int MESO_PER_BCOIN = 1_000_000_000;
        final int BCOIN_ITEM_ID = 3020002;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            long currentDbMesos = 0;
            try (PreparedStatement ps = con.prepareStatement("SELECT MerchantMesos FROM characters WHERE id = ?")) {
                ps.setInt(1, ownerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentDbMesos = rs.getLong("MerchantMesos");
                    }
                }
            }

            long totalRevenue = currentDbMesos + this.mesos;
            int coinsToAdd = (int) (totalRevenue / MESO_PER_BCOIN);
            int remainder = (int) (totalRevenue % MESO_PER_BCOIN);

            debug("saveRevenueToDB: Calc -> ExistingDB: " + currentDbMesos + " + NewMemory: " + this.mesos + " = Total: " + totalRevenue);
            debug("saveRevenueToDB: Result -> BCoins to Insert: " + coinsToAdd + " | Remainder Mesos: " + remainder);

            // 1. Update Remainder
            try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET MerchantMesos = ? WHERE id = ?")) {
                ps.setInt(1, remainder);
                ps.setInt(2, ownerId);
                ps.executeUpdate();
            }

            // 2. Update Timestamp
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO fredstorage (cid, daynotes, timestamp) VALUES (?, 0, NOW()) ON DUPLICATE KEY UPDATE timestamp = NOW()")) {
                ps.setInt(1, ownerId);
                ps.executeUpdate();
            }

            // 3. Insert BCoins
            if (coinsToAdd > 0) {
                int inventoryItemId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO inventoryitems (type, characterid, itemid, inventorytype, position, quantity, owner, flag, expiration, giftFrom) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, 6); // Merchant Type
                    ps.setInt(2, ownerId);
                    ps.setInt(3, BCOIN_ITEM_ID);
                    ps.setInt(4, InventoryType.ETC.getType());
                    ps.setInt(5, 0);
                    ps.setInt(6, coinsToAdd);
                    ps.setString(7, "");
                    ps.setInt(8, 0);
                    ps.setLong(9, -1L);
                    ps.setString(10, "Fredrick");
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) inventoryItemId = rs.getInt(1);
                        else throw new SQLException("Failed to get ID");
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("INSERT INTO inventorymerchant (inventoryitemid, characterid, bundles) VALUES (?, ?, ?)")) {
                    ps.setInt(1, inventoryItemId);
                    ps.setInt(2, ownerId);
                    ps.setInt(3, 1);
                    ps.executeUpdate();
                }
                debug("saveRevenueToDB: Inserted " + coinsToAdd + " BCoins successfully.");
            }

            con.commit();
            con.setAutoCommit(true);

            this.mesos = 0; // Reset memory accumulator
            debug("saveRevenueToDB: END. Memory cleared.");

        } catch (SQLException e) {
            debug("saveRevenueToDB: ERROR - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeVisitor(Character chr) {
        visitorLock.lock();
        try {
            int slot = getVisitorSlot(chr);
            if (slot < 0) return;

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
        return -1;
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
                chr.sendPacket(PacketCreator.updateHiredMerchant(this, chr));
            }
            if (YamlConfig.config.server.USE_ENFORCE_MERCHANT_SAVE) {
                chr.saveCharToDB(false);
            }
        }
    }

    private static boolean canBuy(Client c, Item newItem) {
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
                c.getPlayer().dropMessage(1, "The item is not available.");
                c.sendPacket(PacketCreator.enableActions());
                return;
            } else if (newItem.getInventoryType().equals(InventoryType.EQUIP) && newItem.getQuantity() > 1) {
                c.getPlayer().dropMessage(1, "You can only buy one of this item at a time.");
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            if (!canBuy(c, newItem)) {
                c.getPlayer().dropMessage(1, "Your inventory is full.");
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            KarmaManipulator.toggleKarmaFlagToUntradeable(newItem);

            long rawPrice = (long) pItem.getPrice() * (long) quantity;
            int price = rawPrice > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawPrice;

            long playerMesos = c.getPlayer().getMeso();
            long deficit = price - playerMesos;

            // [LOGIC START] Auto-Conversion & Payment
            if (deficit > 0) {
                final int BCOIN_ID = 3020002;
                final long BCOIN_RATE = 1_000_000_000L;

                int bcoinsNeeded = (int) Math.ceil((double) deficit / (double) BCOIN_RATE);

                // Debugging
                int hasCoins = c.getPlayer().getItemQuantity(BCOIN_ID, false);
                debug("[BUY-CONVERT] Check: User needs " + bcoinsNeeded + " coins. Has: " + hasCoins);

                if (hasCoins >= bcoinsNeeded) {
                    try {
                        // [FIX] Dynamically find the correct inventory type
                        // Check ETC first
                        InventoryType coinType = InventoryType.ETC;
                        if (c.getPlayer().getInventory(InventoryType.ETC).findById(BCOIN_ID) == null) {
                            // If not in ETC, check SETUP (common for ID 3xxxxxx)
                            if (c.getPlayer().getInventory(InventoryType.SETUP).findById(BCOIN_ID) != null) {
                                coinType = InventoryType.SETUP;
                            } else if (c.getPlayer().getInventory(InventoryType.CASH).findById(BCOIN_ID) != null) {
                                coinType = InventoryType.CASH;
                            }
                        }

                        // Perform Removal using the DETECTED type
                        InventoryManipulator.removeById(c, coinType, BCOIN_ID, bcoinsNeeded, true, false);

                        // Calculate Math virtually
                        long totalWealth = playerMesos + (bcoinsNeeded * BCOIN_RATE);
                        long newBalance = totalWealth - price;

                        // Update Player Mesos
                        c.getPlayer().gainMeso((int) (newBalance - playerMesos), false);

                        c.getPlayer().dropMessage(5, "[Auto-Convert] Used " + bcoinsNeeded + " B-Coins to cover purchase.");
                        debug("[BUY-CONVERT] Success. New Balance: " + newBalance);

                    } catch (RuntimeException e) {
                        System.err.println("[HM-ERROR] Auto-Convert Failed for " + c.getPlayer().getName() + ": " + e.getMessage());
                        c.getPlayer().dropMessage(1, "Error: Could not process B-Coins. Please convert them manually.");
                        c.sendPacket(PacketCreator.enableActions());
                        return;
                    }
                } else {
                    c.getPlayer().dropMessage(1, "You need " + bcoinsNeeded + " B-Coins (or more mesos) to buy this.");
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }
            } else {
                // Standard Payment
                c.getPlayer().gainMeso(-price, false);
            }
            // [LOGIC END] Payment Complete

            // Calculate Seller Fee
            int fee = Trade.getFee(price);
            int afterFee = price - fee;
            if (afterFee < 0) afterFee = 0;

            synchronized (sold) {
                sold.add(new SoldItem(c.getPlayer().getName(), pItem.getItem().getItemId(), newItem.getQuantity(), afterFee));
            }

            pItem.setBundles((short) (pItem.getBundles() - quantity));
            if (pItem.getBundles() < 1) {
                pItem.setDoesExist(false);
            }

            if (YamlConfig.config.server.USE_ANNOUNCE_SHOPITEMSOLD) {
                announceItemSold(newItem, afterFee, getQuantityLeft(pItem.getItem().getItemId()));
            }

            // Memory accumulator
            this.mesos += afterFee;

            debug("[BUY] Buyer: " + c.getPlayer().getName() + " | ItemID: " + newItem.getItemId() + " | Qty: " + quantity + " | Price: " + price);

            Character owner = Server.getInstance().getWorld(world).getPlayerStorage().getCharacterByName(ownerName);
            if (owner != null) {
                owner.dropMessage(5, "[Merchant] Sold item. Revenue banked: " + afterFee);
            }

            try {
                this.saveItems(false);
            } catch (Exception e) {
                debug("[BUY] Error saving items: " + e.getMessage());
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
        debug("[CLOSE] Force close triggered.");
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
            debug("[CLOSE] 1. Saving Items...");
            saveItems(true);

            debug("[CLOSE] 2. Saving Revenue...");
            saveRevenueToDB();

            debug("[CLOSE] 3. Triggering Fredrick Log...");
            FredrickProcessor.insertFredrickLog(this.ownerId);

            synchronized (items) {
                items.clear();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        if (owner != null) {
            owner.setHasMerchant(false);
        } else {
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE characters SET HasMerchant = 0 WHERE id = ?")) {
                ps.setInt(1, ownerId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        map = null;
        debug("[CLOSE] Completed.");
    }

    public void closeOwnerMerchant(Character chr) {
        if (this.isOwner(chr)) {
            this.closeShop(chr.getClient(), false);
            chr.setHasMerchant(false);
        }
    }

    private void closeShop(Client c, boolean timeout) {
        debug("[CLOSE] closeShop() triggered. Timeout=" + timeout);
        map.removeMapObject(this);
        map.broadcastMessage(PacketCreator.removeHiredMerchantBox(ownerId));
        c.getChannelServer().removeHiredMerchant(ownerId);

        this.removeAllVisitors();
        this.removeOwner(c.getPlayer());

        try {
            List<PlayerShopItem> copyItems = getItems();

            if (check(c.getPlayer(), copyItems) && !timeout) {
                debug("[CLOSE] Returning items to inventory directly.");
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
                debug("[CLOSE] Saving items (Persistence)...");
                this.saveItems(timeout);

                debug("[CLOSE] Saving Revenue...");
                this.saveRevenueToDB();

                synchronized (items) {
                    if (!items.isEmpty() || this.mesos > 0) {
                        FredrickProcessor.insertFredrickLog(this.ownerId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Character player = c.getWorldServer().getPlayerStorage().getCharacterById(ownerId);
            if (player != null) {
                player.setHasMerchant(false);
            } else {
                try (Connection con = DatabaseConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement("UPDATE characters SET HasMerchant = 0 WHERE id = ?")) {
                    ps.setInt(1, ownerId);
                    ps.executeUpdate();
                }
            }

            if (YamlConfig.config.server.USE_ENFORCE_MERCHANT_SAVE) {
                c.getPlayer().saveCharToDB(false);
            }
            synchronized (items) {
                items.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Server.getInstance().getWorld(world).unregisterHiredMerchant(this);
        debug("[CLOSE] closeShop() completed.");
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

    public String getOwner() { return ownerName; }
    public void clearItems() { synchronized (items) { items.clear(); } }
    public int getOwnerId() { return ownerId; }
    public String getDescription() { return description; }
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
    public List<PlayerShopItem> getItems() { synchronized (items) { return Collections.unmodifiableList(items); } }
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
            if (items.size() >= 16) return false;
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
            try { this.saveItems(false); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
    private void removeFromSlot(int slot) {
        items.remove(slot);
        try { this.saveItems(false); } catch (SQLException ex) { ex.printStackTrace(); }
    }
    private int getFreeSlot() {
        for (int i = 0; i < 3; i++) {
            if (visitors[i] == null) return i;
        }
        return -1;
    }

    public void setDescription(String description) { this.description = description; }
    public boolean isPublished() { return published; }
    public boolean isOpen() { return open.get(); }
    public void setOpen(boolean set) { open.getAndSet(set); published = true; }
    public int getItemId() { return itemId; }
    public boolean isOwner(Character chr) { return chr.getId() == ownerId; }
    public void sendMessage(Character chr, String msg) {
        String message = chr.getName() + " : " + msg;
        byte slot = (byte) (getVisitorSlot(chr) + 1);
        synchronized (messages) { messages.add(new Pair<>(message, slot)); }
        broadcastToVisitorsThreadsafe(PacketCreator.hiredMerchantChat(message, slot));
    }
    public List<PlayerShopItem> sendAvailableBundles(int itemid) {
        List<PlayerShopItem> list = new LinkedList<>();
        List<PlayerShopItem> all = new ArrayList<>();
        if (!open.get()) return list;
        synchronized (items) { all.addAll(items); }
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
        List<Integer> prices = new ArrayList<>();

        synchronized (items) {
            for (PlayerShopItem pItems : items) {
                // [FIX] Crucial Copy for Stack Size Bug
                Item newItem = pItems.getItem().copy();
                newItem.setQuantity(pItems.getItem().getQuantity());

                if (pItems.getBundles() > 0) {
                    itemsWithType.add(new Pair<>(newItem, newItem.getInventoryType()));
                    bundles.add(pItems.getBundles());
                    prices.add(pItems.getPrice());
                }
            }
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                ItemFactory.MERCHANT.saveItems(itemsWithType, bundles, prices, this.ownerId, con);
                con.commit();
            } catch (Exception e) {
                try { con.rollback(); } catch (SQLException re) { re.printStackTrace(); }
                throw e;
            } finally {
                try { con.setAutoCommit(oldAutoCommit); } catch (SQLException ignore) {}
            }
        }
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

    public int getChannel() { return channel; }
    public int getTimeOpen() {
        double openTime = (System.currentTimeMillis() - start) / 60000;
        openTime /= 1440;
        openTime *= 1318;
        return (int) Math.ceil(openTime);
    }
    public void clearMessages() { synchronized (messages) { messages.clear(); } }
    public List<Pair<String, Byte>> getMessages() {
        synchronized (messages) {
            List<Pair<String, Byte>> msgList = new LinkedList<>();
            msgList.addAll(messages);
            return msgList;
        }
    }
    public List<PastVisitor> getVisitorHistory() { return Collections.unmodifiableList(visitorHistory); }
    public void addToBlacklist(String chrName) {
        visitorLock.lock();
        try {
            if (blacklist.size() >= BLACKLIST_LIMIT) return;
            blacklist.add(chrName);
        } finally { visitorLock.unlock(); }
    }
    public void removeFromBlacklist(String chrName) {
        visitorLock.lock();
        try { blacklist.remove(chrName); } finally { visitorLock.unlock(); }
    }
    public Set<String> getBlacklist() { return Collections.unmodifiableSet(blacklist); }
    private boolean isBlacklisted(String chrName) {
        visitorLock.lock();
        try { return blacklist.contains(chrName); } finally { visitorLock.unlock(); }
    }
    public int getMapId() { return map.getId(); }
    public MapleMap getMap() { return map; }
    public List<SoldItem> getSold() { synchronized (sold) { return Collections.unmodifiableList(sold); } }
    public long getMesos() { return mesos; }

    @Override
    public MapObjectType getType() { return MapObjectType.HIRED_MERCHANT; }
    @Override
    public void sendDestroyData(Client client) {}
    @Override
    public void sendSpawnData(Client client) { client.sendPacket(PacketCreator.spawnHiredMerchantBox(this)); }

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
        public String getBuyer() { return buyer; }
        public int getItemId() { return itemid; }
        public short getQuantity() { return quantity; }
        public int getMesos() { return mesos; }
    }

    public void savePersistence(Connection con) throws SQLException {
        PreparedStatement ps = null;
        try {
            long minutesOpen = (System.currentTimeMillis() - this.start) / 60000;
            ps = con.prepareStatement("REPLACE INTO hiredmerchants (ownerid, ownername, itemid, description, world, channel, mapid, x, y, minutesopen, isopen, blacklist) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)");
            ps.setInt(1, ownerId);
            ps.setString(2, ownerName);
            ps.setInt(3, itemId);
            ps.setString(4, description);
            ps.setInt(5, world);
            ps.setInt(6, channel);
            ps.setInt(7, map.getId());
            ps.setInt(8, getPosition().x);
            ps.setInt(9, getPosition().y);
            ps.setInt(10, (int) minutesOpen);
            StringBuilder bl = new StringBuilder();
            visitorLock.lock();
            try {
                for (String banned : blacklist) { bl.append(banned).append(";"); }
            } finally { visitorLock.unlock(); }
            ps.setString(11, bl.toString());
            ps.executeUpdate();
            ps.close();

            List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();
            List<Short> bundles = new ArrayList<>();
            List<Integer> prices = new ArrayList<>();
            synchronized (items) {
                for (PlayerShopItem pItems : items) {
                    Item newItem = pItems.getItem().copy();
                    newItem.setQuantity(pItems.getItem().getQuantity());
                    if (pItems.getBundles() > 0) {
                        itemsWithType.add(new Pair<>(newItem, newItem.getInventoryType()));
                        bundles.add(pItems.getBundles());
                        prices.add(pItems.getPrice());
                    }
                }
            }
            ItemFactory.MERCHANT.saveItems(itemsWithType, bundles, prices, this.ownerId, con);
        } finally {
            if (ps != null && !ps.isClosed()) ps.close();
        }
    }

    public static HiredMerchant loadFromPersistence(net.server.channel.Channel cserv, ResultSet rs) {
        try {
            int ownerId = rs.getInt("ownerid");
            String ownerName = rs.getString("ownername");
            int itemId = rs.getInt("itemid");
            String description = rs.getString("description");
            int mapId = rs.getInt("mapid");
            int x = rs.getInt("x");
            int y = rs.getInt("y");
            int minutesOpen = rs.getInt("minutesopen");
            String blString = rs.getString("blacklist");

            System.out.println("[HM-DEBUG][RESTORE] Starting restore for owner: " + ownerId);

            MapleMap map = cserv.getMapFactory().getMap(mapId);
            if (map == null) {
                System.out.println("[HM-DEBUG][RESTORE] Invalid Map ID: " + mapId);
                return null;
            }

            long restoredStart = System.currentTimeMillis() - (minutesOpen * 60000L);

            HiredMerchant merch = new HiredMerchant(ownerId, ownerName, itemId, description, cserv.getWorld(), cserv.getId(), map, restoredStart);
            merch.setPosition(new java.awt.Point(x, y));

            if (blString != null && !blString.isEmpty()) {
                for (String s : blString.split(";")) {
                    if (!s.isEmpty()) merch.addToBlacklist(s);
                }
            }

            java.util.Map<Integer, Pair<Integer, Short>> priceInfo = new java.util.HashMap<>();
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT inventoryitemid, price, bundles FROM inventorymerchant WHERE characterid = ?")) {
                ps.setInt(1, ownerId);
                try (ResultSet rs2 = ps.executeQuery()) {
                    while (rs2.next()) {
                        int invId = rs2.getInt("inventoryitemid");
                        int price = rs2.getInt("price");
                        short bundles = rs2.getShort("bundles");
                        priceInfo.put(invId, new Pair<>(price, bundles));
                    }
                }
            }

            List<Pair<Item, InventoryType>> loadedItems = ItemFactory.MERCHANT.loadItems(ownerId, false);
            int addedCount = 0;

            for (Pair<Item, InventoryType> p : loadedItems) {
                Item item = p.getLeft();
                int uniqueId = item.getUniqueId();
                if (item.getItemId() == 3020002) continue;

                if (priceInfo.containsKey(uniqueId)) {
                    Pair<Integer, Short> info = priceInfo.get(uniqueId);
                    int price = info.getLeft();
                    short bundles = info.getRight();
                    PlayerShopItem shopItem = new PlayerShopItem(item, bundles, price);
                    merch.addItem(shopItem);
                    addedCount++;
                    System.out.println("[HM-DEBUG][RESTORE] Loaded Item: " + item.getItemId() + " | Price: " + price + " | Bundle: " + bundles);
                } else {
                    System.out.println("[HM-DEBUG][RESTORE] Skipping Item (No Price Info): " + item.getItemId());
                }
            }

            if (addedCount == 0) {
                System.out.println("[HM-DEBUG][RESTORE] Shop has 0 items. Returning null to trigger fallback.");
                return null;
            }

            System.out.println("[HM-DEBUG][RESTORE] Success. Total Items: " + addedCount);
            return merch;

        } catch (Exception e) {
            System.out.println("[HM-DEBUG][RESTORE] Exception: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}