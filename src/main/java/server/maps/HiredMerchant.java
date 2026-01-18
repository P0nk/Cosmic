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

    // [NEW] Constructor for DB Loading (Bypasses Client/Character requirements)
    public HiredMerchant(int ownerId, String ownerName, int itemId, String desc, int world, int channel, MapleMap map, long startTime) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.itemId = itemId;
        this.description = desc;
        this.world = world;
        this.channel = channel;
        this.map = map;
        this.start = startTime; // Backdated start time
        this.setPosition(new Point(0, 0)); // Will be updated by load logic
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

    private static void creditMerchantOfflineWithBCoinOverflow(int ownerId, int add) throws SQLException {
        if (add <= 0) {
            System.err.println("[HMERCH][DB] add<=0 skip. ownerId=" + ownerId);
            return;
        }

        final int MESO_PER_BCOIN = 1_000_000_000;
        final int BCOIN_ITEM_ID = 3020002;

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            int merchantMesos = 0;
            try (PreparedStatement ps = con.prepareStatement("SELECT MerchantMesos FROM characters WHERE id = ?")) {
                ps.setInt(1, ownerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        merchantMesos = rs.getInt(1);
                    }
                }
            }

            long total = (long) merchantMesos + (long) add;
            int coinsToAdd = (int) (total / MESO_PER_BCOIN);
            int remainder = (int) (total % MESO_PER_BCOIN);

            System.err.println("[HMERCH][DB] ownerId=" + ownerId
                    + " merchantMesos(before)=" + merchantMesos
                    + " add=" + add
                    + " total=" + total
                    + " coinsToAdd=" + coinsToAdd
                    + " remainder=" + remainder);

            // 1) update remainder into characters.MerchantMesos
            try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET MerchantMesos = ? WHERE id = ?")) {
                ps.setInt(1, remainder);
                ps.setInt(2, ownerId);
                int rows = ps.executeUpdate();
                System.err.println("[HMERCH][DB] UPDATE characters rows=" + rows);
            }

            // 2) touch fredstorage timestamp (decay logic uses this)
            int updated;
            try (PreparedStatement ps = con.prepareStatement("UPDATE fredstorage SET timestamp = NOW() WHERE cid = ?")) {
                ps.setInt(1, ownerId);
                updated = ps.executeUpdate();
                System.err.println("[HMERCH][DB] UPDATE fredstorage rows=" + updated);
            }
            if (updated == 0) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO fredstorage (cid, daynotes, timestamp) VALUES (?, 0, NOW())")) {
                    ps.setInt(1, ownerId);
                    int ins = ps.executeUpdate();
                    System.err.println("[HMERCH][DB] INSERT fredstorage rows=" + ins);
                }
            }

            // 3) insert BCoin items into merchant storage (type=6) + inventorymerchant bundles=1
            if (coinsToAdd > 0) {
                int inventoryItemId;

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO inventoryitems " +
                                "(type, characterid, accountid, itemid, inventorytype, position, quantity, owner, petid, flag, expiration, giftFrom) " +
                                "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {

                    ps.setInt(1, 6);
                    ps.setInt(2, ownerId);
                    ps.setInt(3, BCOIN_ITEM_ID);
                    ps.setInt(4, InventoryType.ETC.getType());
                    ps.setInt(5, 0);
                    ps.setInt(6, coinsToAdd);
                    ps.setString(7, "");
                    ps.setInt(8, -1);
                    ps.setInt(9, 0);
                    ps.setLong(10, -1L);
                    ps.setString(11, "");

                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("No generated key for inventoryitems insert (BCoin).");
                        }
                        inventoryItemId = rs.getInt(1);
                    }
                }

                System.err.println("[HMERCH][DB] Inserted BCoin inventoryitemid=" + inventoryItemId + " qty=" + coinsToAdd);

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO inventorymerchant (inventorymerchantid, inventoryitemid, characterid, bundles) VALUES (DEFAULT, ?, ?, ?)")) {
                    ps.setInt(1, inventoryItemId);
                    ps.setInt(2, ownerId);
                    ps.setInt(3, 1);
                    int rows = ps.executeUpdate();
                    System.err.println("[HMERCH][DB] INSERT inventorymerchant rows=" + rows);
                }
            } else {
                System.err.println("[HMERCH][DB] coinsToAdd=0, no BCoin insert required.");
            }

            con.commit();
            System.err.println("[HMERCH][DB] COMMIT SUCCESS");
        } catch (SQLException e) {
            System.err.println("[HMERCH][DB][ERROR] rollback");
            if (con != null) {
                try { con.rollback(); } catch (SQLException ignore) {}
            }
            throw e;
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); } catch (SQLException ignore) {}
                try { con.close(); } catch (SQLException ignore) {}
            }
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
                System.err.println("[HMERCH][BUY][FAIL] invalid qty/bundles buyer=" + c.getPlayer().getName()
                        + " qty=" + quantity + " exist=" + pItem.isExist() + " bundles=" + pItem.getBundles());
                c.getPlayer().dropMessage(6, "[HMERCH-DBG] FAIL invalid qty/bundles");
                c.sendPacket(PacketCreator.enableActions());
                return;
            } else if (newItem.getInventoryType().equals(InventoryType.EQUIP) && newItem.getQuantity() > 1) {
                System.err.println("[HMERCH][BUY][FAIL] equip qty>1 buyer=" + c.getPlayer().getName()
                        + " item=" + newItem.getItemId() + " qty=" + newItem.getQuantity());
                c.getPlayer().dropMessage(6, "[HMERCH-DBG] FAIL equip qty>1");
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            KarmaManipulator.toggleKarmaFlagToUntradeable(newItem);

            // --- long-safe calc so we can SEE what happens for >=2b pricing tests ---
            long rawPrice = (long) pItem.getPrice() * (long) quantity;
            int price = rawPrice > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawPrice;

            String hdr = "[HMERCH-DBG] buyer=" + c.getPlayer().getName()
                    + " ownerName=" + ownerName
                    + " ownerId=" + ownerId
                    + " itemId=" + pItem.getItem().getItemId()
                    + " unitPrice=" + pItem.getPrice()
                    + " buyBundles=" + quantity
                    + " rawPrice=" + rawPrice
                    + " price(intCap)=" + price;

            System.err.println(hdr);
            c.getPlayer().dropMessage(6, hdr);

            if (c.getPlayer().getMeso() < price) {
                System.err.println("[HMERCH][BUY][FAIL] insufficient mesos buyer=" + c.getPlayer().getName()
                        + " has=" + c.getPlayer().getMeso() + " need=" + price);
                c.getPlayer().dropMessage(1, "You don't have enough mesos to purchase this item.");
                c.getPlayer().dropMessage(6, "[HMERCH-DBG] FAIL insufficient mesos");
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            boolean canBuyResult = canBuy(c, newItem);
            System.err.println("[HMERCH][BUY] canBuy=" + canBuyResult);
            c.getPlayer().dropMessage(6, "[HMERCH-DBG] canBuy=" + canBuyResult);

            if (!canBuyResult) {
                c.getPlayer().dropMessage(1, "Your inventory is full. Please clear a slot before buying this item.");
                c.getPlayer().dropMessage(6, "[HMERCH-DBG] FAIL inventory full");
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            int beforeBuyer = c.getPlayer().getMeso();
            c.getPlayer().gainMeso(-price, false);
            int afterBuyer = c.getPlayer().getMeso();

            int fee = Trade.getFee(price);
            int afterFee = price - fee;
            if (afterFee < 0) afterFee = 0;

            System.err.println("[HMERCH][BUY] buyerMeso " + beforeBuyer + " -> " + afterBuyer
                    + " fee=" + fee + " afterFee=" + afterFee);
            c.getPlayer().dropMessage(6, "[HMERCH-DBG] fee=" + fee + " afterFee=" + afterFee);

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

            // --- CREDIT TO MERCHANT STORAGE (BCoin overflow supported) ---
            Character owner = Server.getInstance().getWorld(world).getPlayerStorage().getCharacterByName(ownerName);
            if (owner != null) {
                owner.dropMessage(6, "[HMERCH-DBG] credited(afterFee)=" + afterFee + " via addMerchantMesosAsBCoinOverflow()");
                System.err.println("[HMERCH][CREDIT] owner ONLINE -> addMerchantMesosAsBCoinOverflow(" + afterFee + ")");
                owner.addMerchantMesosAsBCoinOverflow(afterFee);
            } else {
                System.err.println("[HMERCH][CREDIT] owner OFFLINE -> DB credit with BCoin overflow. afterFee=" + afterFee);
                try {
                    creditMerchantOfflineWithBCoinOverflow(ownerId, afterFee);
                    System.err.println("[HMERCH][CREDIT] offline DB credit DONE");
                } catch (Exception e) {
                    System.err.println("[HMERCH][CREDIT][ERROR] offline DB credit failed");
                    e.printStackTrace();
                }
            }

            try {
                this.saveItems(false);
            } catch (Exception e) {
                System.err.println("[HMERCH][BUY][ERROR] saveItems failed");
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
        List<Integer> prices = new ArrayList<>(); // [FIX] Added Price List

        synchronized (items) { // Ensure thread safety while reading
            for (PlayerShopItem pItems : items) {
                Item newItem = pItems.getItem(); // Note: Be careful modifying this object directly if it's shared
                short newBundle = pItems.getBundles();

                // It's safer to clone the item to avoid modifying the quantity of the live object
                // if saveItems is called while the shop is open.
                // However, following your existing pattern:
                newItem.setQuantity(pItems.getItem().getQuantity());

                if (newBundle > 0) {
                    itemsWithType.add(new Pair<>(newItem, newItem.getInventoryType()));
                    bundles.add(newBundle);
                    prices.add(pItems.getPrice()); // [FIX] Collect Price
                }
            }
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            try {
                // [FIX] Pass 'prices' list to the factory
                ItemFactory.MERCHANT.saveItems(itemsWithType, bundles, prices, this.ownerId, con);
                con.commit();
            } catch (Exception e) {
                System.err.println("[MERCHANT][FATAL] HiredMerchant.saveItems FAILED -> rollback. CID=" + ownerId);
                e.printStackTrace();
                try { con.rollback(); } catch (SQLException re) { re.printStackTrace(); }
                throw e;
            } finally {
                try { con.setAutoCommit(oldAutoCommit); } catch (SQLException ignore) {}
            }
        }

        // Keep your existing Fredrick activity marker
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

    /**
     * [PERSISTENCE] Saves the merchant to DB during server shutdown.
     * Uses the shared connection from Channel to ensure atomic transaction.
     */
    /**
     * [PERSISTENCE] Saves the merchant to DB during server shutdown.
     * Uses the shared connection from Channel to ensure atomic transaction.
     */
    public void savePersistence(Connection con) throws SQLException {
        PreparedStatement ps = null;
        try {
            // 1. Calculate accumulated time open
            long minutesOpen = (System.currentTimeMillis() - this.start) / 60000;

            // 2. Save Metadata
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

            // Serialize blacklist
            StringBuilder bl = new StringBuilder();
            visitorLock.lock();
            try {
                for (String banned : blacklist) {
                    bl.append(banned).append(";");
                }
            } finally {
                visitorLock.unlock();
            }
            ps.setString(11, bl.toString());
            ps.executeUpdate();
            ps.close();

            // 3. Save Items
            // [CRITICAL FIX] REMOVED THE RAW 'DELETE FROM inventoryitems' HERE.
            // That delete was destroying BCoins (Revenue) that were just inserted by sales.
            // We rely on ItemFactory.MERCHANT.saveItems to handle the synchronization safely.

            List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();
            List<Short> bundles = new ArrayList<>();
            List<Integer> prices = new ArrayList<>();

            synchronized (items) {
                for (PlayerShopItem pItems : items) {
                    Item newItem = pItems.getItem();
                    short newBundle = pItems.getBundles();
                    // Avoid modifying live object quantity directly if possible, but standard flow allows it here
                    newItem.setQuantity(pItems.getItem().getQuantity());

                    if (newBundle > 0) {
                        itemsWithType.add(new Pair<>(newItem, newItem.getInventoryType()));
                        bundles.add(newBundle);
                        prices.add(pItems.getPrice());
                    }
                }
            }

            // Pass to factory (Ensure your ItemFactory.saveItemsMerchant has the logic to NOT delete BCoins/3020002)
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
            String desc = rs.getString("description");
            int mapId = rs.getInt("mapid");
            int x = rs.getInt("x");
            int y = rs.getInt("y");
            int minutesOpen = rs.getInt("minutesopen");
            String blString = rs.getString("blacklist");

            // 1. Validate Map
            MapleMap map = cserv.getMapFactory().getMap(mapId);
            if (map == null) return null;

            // 2. Calculate time
            long restoredStart = System.currentTimeMillis() - (minutesOpen * 60000L);

            // 3. Create Instance
            HiredMerchant merch = new HiredMerchant(ownerId, ownerName, itemId, desc, cserv.getWorld(), cserv.getId(), map, restoredStart);
            merch.setPosition(new Point(x, y));

            // 4. Restore Blacklist
            if (blString != null && !blString.isEmpty()) {
                for (String s : blString.split(";")) {
                    if (!s.isEmpty()) merch.addToBlacklist(s);
                }
            }

            // 5. Load Items & Prices
            java.util.Map<Integer, Pair<Integer, Short>> priceInfo = new java.util.HashMap<>();

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT inventoryitemid, price, bundles FROM inventorymerchant WHERE characterid = ?")) {
                ps.setInt(1, ownerId);
                try (ResultSet rs2 = ps.executeQuery()) {
                    while (rs2.next()) {
                        priceInfo.put(rs2.getInt("inventoryitemid"), new Pair<>(rs2.getInt("price"), rs2.getShort("bundles")));
                    }
                }
            }

            List<Pair<Item, InventoryType>> loadedItems = ItemFactory.INVENTORY.loadItems(ownerId, false);

            for (Pair<Item, InventoryType> p : loadedItems) {
                Item item = p.getLeft();

                // [CRITICAL FIX] Do not load BCoins (3020002) into the shop window.
                // These are stored revenue and should remain hidden until retrieved via Fredrick/NPC.
                if (item.getItemId() == 3020002) {
                    continue;
                }

                int uniqueId = item.getUniqueId();

                if (priceInfo.containsKey(uniqueId)) {
                    Pair<Integer, Short> info = priceInfo.get(uniqueId);
                    PlayerShopItem shopItem = new PlayerShopItem(item, info.getRight(), info.getLeft());
                    merch.addItem(shopItem);
                }
            }

            return merch;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
