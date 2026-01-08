/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
              Matthias Butz <matze@odinms.de>
              Jan Christian Meyer <vimes@odinms.de>

    Copyleft (L) 2016 - 2019 RonanLana (HeavenMS)

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
package client.processor.npc;

import client.Character;
import client.Client;
import client.autoban.AutobanFactory;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.manipulator.InventoryManipulator;
import client.inventory.manipulator.KarmaManipulator;
import config.YamlConfig;
import constants.id.ItemId;
import constants.inventory.ItemConstants;
import net.server.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.DueyPackage;
import server.ItemInformationProvider;
import server.Trade;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author RonanLana - synchronization of Duey modules
 * Optimized & Secured by Gemini
 */
public class DueyProcessor {
    private static final Logger log = LoggerFactory.getLogger(DueyProcessor.class);

    public enum Actions {
        TOSERVER_RECV_ITEM(0x00),
        TOSERVER_SEND_ITEM(0x02),
        TOSERVER_CLAIM_PACKAGE(0x04),
        TOSERVER_REMOVE_PACKAGE(0x05),
        TOSERVER_CLOSE_DUEY(0x07),
        TOCLIENT_OPEN_DUEY(0x08),
        TOCLIENT_SEND_ENABLE_ACTIONS(0x09),
        TOCLIENT_SEND_NOT_ENOUGH_MESOS(0x0A),
        TOCLIENT_SEND_INCORRECT_REQUEST(0x0B),
        TOCLIENT_SEND_NAME_DOES_NOT_EXIST(0x0C),
        TOCLIENT_SEND_SAMEACC_ERROR(0x0D),
        TOCLIENT_SEND_RECEIVER_STORAGE_FULL(0x0E),
        TOCLIENT_SEND_RECEIVER_UNABLE_TO_RECV(0x0F),
        TOCLIENT_SEND_RECEIVER_STORAGE_WITH_UNIQUE(0x10),
        TOCLIENT_SEND_MESO_LIMIT(0x11),
        TOCLIENT_SEND_SUCCESSFULLY_SENT(0x12),
        TOCLIENT_RECV_UNKNOWN_ERROR(0x13),
        TOCLIENT_RECV_ENABLE_ACTIONS(0x14),
        TOCLIENT_RECV_NO_FREE_SLOTS(0x15),
        TOCLIENT_RECV_RECEIVER_WITH_UNIQUE(0x16),
        TOCLIENT_RECV_SUCCESSFUL_MSG(0x17),
        TOCLIENT_RECV_PACKAGE_MSG(0x1B);
        final byte code;

        Actions(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }
    }

    // --- Helper Methods ---

    private static Pair<Integer, Integer> getAccountCharacterIdFromCNAME(String name) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, accountid FROM characters WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Pair<>(rs.getInt("accountid"), rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            log.error("Error retrieving character ID for name: " + name, e);
        }
        return new Pair<>(-1, -1);
    }

    private static DueyPackage loadSinglePackage(int packageId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE PackageId = ?")) {
            ps.setInt(1, packageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return getPackageFromDB(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading single package: " + packageId, e);
        }
        return null;
    }

    private static DueyPackage getPackageFromDB(ResultSet rs) throws SQLException {
        int packageId = rs.getInt("PackageId");
        List<Pair<Item, InventoryType>> dueyItems = ItemFactory.DUEY.loadItems(packageId, false);

        DueyPackage dueypack = dueyItems.isEmpty() ? new DueyPackage(packageId) : new DueyPackage(packageId, dueyItems.get(0).getLeft());

        dueypack.setSender(rs.getString("SenderName"));
        dueypack.setMesos(rs.getInt("Mesos"));
        dueypack.setSentTime(rs.getTimestamp("TimeStamp"), rs.getBoolean("Type"));
        dueypack.setMessage(rs.getString("Message"));

        return dueypack;
    }

    // --- Core Logic ---

    public static void dueySendTalk(Client c, boolean quickDelivery) {
        if (!c.tryacquireClient()) return;
        try {
            long timeNow = System.currentTimeMillis();
            if (timeNow - c.getPlayer().getNpcCooldown() < YamlConfig.config.server.BLOCK_NPC_RACE_CONDT) {
                c.sendPacket(PacketCreator.enableActions());
                return;
            }
            c.getPlayer().setNpcCooldown(timeNow);

            if (quickDelivery) {
                c.sendPacket(PacketCreator.sendDuey(0x1A, null));
            } else {
                c.sendPacket(PacketCreator.sendDuey(0x8, loadPackages(c.getPlayer())));
            }
        } finally {
            c.releaseClient();
        }
    }

    public static void dueySendItem(Client c, byte invTypeId, short itemPos, short amount, int sendMesos, String sendMessage, String recipient, boolean quick) {
        if (!c.tryacquireClient()) return;

        Connection con = null;
        try {
            Character chr = c.getPlayer();

            // --- VALIDATION BLOCK ---
            if (chr.isGM() && chr.gmLevel() < YamlConfig.config.server.MINIMUM_GM_LEVEL_TO_USE_DUEY) {
                chr.message("You cannot use Duey to send items at your GM level.");
                c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_SEND_INCORRECT_REQUEST.getCode()));
                return;
            }

            if (sendMessage != null && sendMessage.length() > 100) {
                AutobanFactory.PACKET_EDIT.alert(chr, chr.getName() + " tried to packet edit with long text on duey.");
                c.disconnect(true, false);
                return;
            }

            // Fix Integer Overflow exploits
            long fee = Trade.getFee(sendMesos);
            long totalCost = (long) sendMesos + fee + (quick ? 0 : 5000);

            if (totalCost < 0 || totalCost > Integer.MAX_VALUE || chr.getMeso() < totalCost || (amount < 1 && sendMesos == 0)) {
                AutobanFactory.PACKET_EDIT.alert(chr, "Duey Meso Hack Attempt or Invalid Amount");
                c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_SEND_NOT_ENOUGH_MESOS.getCode()));
                return;
            }

            if (quick && !chr.haveItem(ItemId.QUICK_DELIVERY_TICKET)) {
                AutobanFactory.PACKET_EDIT.alert(chr, "Duey Quick Delivery without ticket");
                c.disconnect(true, false);
                return;
            }

            Pair<Integer, Integer> recipientData = getAccountCharacterIdFromCNAME(recipient);
            int recipientCid = recipientData.getRight();

            if (recipientCid == -1) {
                c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_SEND_NAME_DOES_NOT_EXIST.getCode()));
                return;
            }
            if (recipientData.getLeft() == c.getAccID()) {
                c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_SEND_SAMEACC_ERROR.getCode()));
                return;
            }

            // --- TRANSACTION BLOCK ---
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // START TRANSACTION

            // 1. Take Mesos/Items (Inventory Logic)
            chr.gainMeso((int) -totalCost, false);
            if (quick) {
                InventoryManipulator.removeById(c, InventoryType.CASH, ItemId.QUICK_DELIVERY_TICKET, (short) 1, false, false);
            }

            // 2. Handle Item logic (if sending item)
            Item itemToSend = null;
            if (invTypeId > 0) {
                Inventory inv = chr.getInventory(InventoryType.getByType(invTypeId));
                inv.lockInventory();
                try {
                    Item item = inv.getItem(itemPos);
                    if (item == null || item.getQuantity() < amount) {
                        con.rollback();
                        c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_SEND_INCORRECT_REQUEST.getCode()));
                        return;
                    }
                    if (item.isUntradeable() || ItemInformationProvider.getInstance().isUnmerchable(item.getItemId())) {
                        con.rollback();
                        c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_SEND_INCORRECT_REQUEST.getCode()));
                        return;
                    }

                    // Remove from player
                    if (ItemConstants.isRechargeable(item.getItemId())) {
                        InventoryManipulator.removeFromSlot(c, InventoryType.getByType(invTypeId), itemPos, item.getQuantity(), true);
                    } else {
                        InventoryManipulator.removeFromSlot(c, InventoryType.getByType(invTypeId), itemPos, amount, true, false);
                    }

                    // Prepare item for DB
                    itemToSend = item.copy();
                    itemToSend.setQuantity(amount);
                    KarmaManipulator.toggleKarmaFlagToUntradeable(itemToSend);
                } finally {
                    inv.unlockInventory();
                }
            }

            // 3. Create Package in DB
            int packageId = -1;
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO `dueypackages` (ReceiverId, SenderName, Mesos, TimeStamp, Message, Type, Checked) VALUES (?, ?, ?, ?, ?, ?, 1)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, recipientCid);
                ps.setString(2, chr.getName());
                ps.setInt(3, sendMesos);
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                ps.setString(5, sendMessage == null ? "" : sendMessage);
                ps.setInt(6, quick ? 1 : 0);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) packageId = rs.getInt(1);
                }
            }

            if (packageId == -1) {
                throw new SQLException("Failed to create package ID");
            }

            // 4. Save Item to DB
            if (itemToSend != null) {
                ItemFactory.DUEY.saveItems(Collections.singletonList(new Pair<>(itemToSend, InventoryType.getByType(itemToSend.getItemType()))), packageId, con);
            }

            con.commit(); // SUCCESS! DB is updated.

            // 5. Force Save Character State
            // Saves inventory to DB immediately to match the transaction
            chr.saveCharToDB(false, false);

            c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_SEND_SUCCESSFULLY_SENT.getCode()));
            notifyRecipient(c, recipient);

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (SQLException ex) { log.error("Rollback failed", ex); }
            c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_SEND_INCORRECT_REQUEST.getCode()));
            log.error("Duey Send Error", e);
            c.sendPacket(PacketCreator.enableActions());
        } finally {
            try { if (con != null) { con.setAutoCommit(true); con.close(); } } catch (SQLException ex) { }
            c.releaseClient();
        }
    }

    public static void dueyClaimPackage(Client c, int packageId) {
        if (!c.tryacquireClient()) return;

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // START TRANSACTION

            // 1. Lock the row immediately so other threads wait (Race Condition Fix)
            DueyPackage dp = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE PackageId = ? FOR UPDATE")) {
                ps.setInt(1, packageId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) dp = getPackageFromDB(rs);
                }
            }

            if (dp == null) {
                con.rollback(); // Package gone
                return;
            }

            // 2. Validation
            if (dp.isDeliveringTime() || !c.getPlayer().canHoldMeso(dp.getMesos())) {
                con.rollback();
                c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_RECV_UNKNOWN_ERROR.getCode()));
                return;
            }

            Item dpItem = dp.getItem();
            if (dpItem != null) {
                if (!InventoryManipulator.checkSpace(c, dpItem.getItemId(), dpItem.getQuantity(), dpItem.getOwner())) {
                    con.rollback();
                    int itemid = dpItem.getItemId();
                    if (ItemInformationProvider.getInstance().isPickupRestricted(itemid) && c.getPlayer().getInventory(ItemConstants.getInventoryType(itemid)).findById(itemid) != null) {
                        c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_RECV_RECEIVER_WITH_UNIQUE.getCode()));
                    } else {
                        c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_RECV_NO_FREE_SLOTS.getCode()));
                    }
                    return;
                }
            }

            // 3. Delete from DB FIRST
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM dueypackages WHERE PackageId = ?")) {
                ps.setInt(1, packageId);
                ps.executeUpdate();
            }
            ItemFactory.DUEY.saveItems(new LinkedList<>(), packageId, con);

            // 4. Update Player Inventory (Memory)
            if (dpItem != null) {
                InventoryManipulator.addFromDrop(c, dpItem, false);
            }
            c.getPlayer().gainMeso(dp.getMesos(), false);

            con.commit(); // SUCCESS

            // 5. Force Save Character State
            // Saves the newly gained item/mesos to DB immediately
            c.getPlayer().saveToDB(false, false);

            c.sendPacket(PacketCreator.removeItemFromDuey(false, packageId));

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (SQLException ex) {}
            log.error("Duey Claim Error", e);
            c.sendPacket(PacketCreator.sendDueyMSG(Actions.TOCLIENT_RECV_UNKNOWN_ERROR.getCode()));
        } finally {
            try { if (con != null) { con.setAutoCommit(true); con.close(); } } catch (SQLException ex) {}
            c.releaseClient();
        }
    }

    public static void dueyRemovePackage(Client c, int packageid, boolean playerRemove) {
        if (!c.tryacquireClient()) return;
        try {
            removePackageFromDB(packageid);
            c.sendPacket(PacketCreator.removeItemFromDuey(playerRemove, packageid));
        } finally {
            c.releaseClient();
        }
    }

    // --- Utility & Database Logic ---

    private static void notifyRecipient(Client c, String recipient) {
        int channel = c.getWorldServer().find(recipient);
        if (channel > -1) {
            Channel rcserv = c.getWorldServer().getChannel(channel);
            if (rcserv != null) {
                Character rChr = rcserv.getPlayerStorage().getCharacterByName(recipient);
                if (rChr != null) {
                    Client rClient = rChr.getClient();
                    if (rClient != null && rClient.isLoggedIn() && !rChr.isAwayFromWorld()) {
                        showDueyNotification(rClient, rChr);
                    }
                }
            }
        }
    }

    private static void showDueyNotification(Client c, Character player) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT SenderName, Type FROM dueypackages WHERE ReceiverId = ? AND Checked = 1 ORDER BY Type DESC")) {
            ps.setInt(1, player.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement ps2 = con.prepareStatement("UPDATE dueypackages SET Checked = 0 where ReceiverId = ?")) {
                        ps2.setInt(1, player.getId());
                        ps2.executeUpdate();
                        c.sendPacket(PacketCreator.sendDueyParcelReceived(rs.getString("SenderName"), rs.getInt("Type") == 1));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error showing Duey notification", e);
        }
    }

    private static void removePackageFromDB(int packageId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM dueypackages WHERE PackageId = ?")) {
            ps.setInt(1, packageId);
            ps.executeUpdate();

            ItemFactory.DUEY.saveItems(new LinkedList<>(), packageId, con);
        } catch (SQLException e) {
            log.error("Error removing package: " + packageId, e);
        }
    }

    private static List<DueyPackage> loadPackages(Character chr) {
        List<DueyPackage> packages = new LinkedList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages dp WHERE ReceiverId = ?")) {
            ps.setInt(1, chr.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DueyPackage dueypack = getPackageFromDB(rs);
                    if (dueypack != null) {
                        packages.add(dueypack);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error loading packages for chr: " + chr.getName(), e);
        }
        return packages;
    }

    // Used by Server Internal calls (Admin commands etc) - No Transaction needed usually as it's not user triggered
    public static void dueyCreatePackage(Item item, int mesos, String sender, int recipientCid) {
        try (Connection con = DatabaseConnection.getConnection()) {
            int packageId = -1;
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO `dueypackages` (ReceiverId, SenderName, Mesos, TimeStamp, Message, Type, Checked) VALUES (?, ?, ?, ?, ?, ?, 1)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, recipientCid);
                ps.setString(2, sender);
                ps.setInt(3, mesos);
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                ps.setString(5, "Created by Admin");
                ps.setInt(6, 0);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) packageId = rs.getInt(1);
                }
            }

            if (packageId != -1) {
                Pair<Item, InventoryType> dueyItem = new Pair<>(item, InventoryType.getByType(item.getItemType()));
                ItemFactory.DUEY.saveItems(Collections.singletonList(dueyItem), packageId, con);
            }
        } catch (SQLException e) {
            log.error("Admin Create Package Error", e);
        }
    }

    public static void runDueyExpireSchedule() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -30);
        final Timestamp ts = new Timestamp(c.getTime().getTime());

        try (Connection con = DatabaseConnection.getConnection()) {
            List<Integer> toRemove = new LinkedList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT `PackageId` FROM dueypackages WHERE `TimeStamp` < ?")) {
                ps.setTimestamp(1, ts);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        toRemove.add(rs.getInt("PackageId"));
                    }
                }
            }

            for (Integer pid : toRemove) {
                removePackageFromDB(pid);
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM dueypackages WHERE `TimeStamp` < ?")) {
                ps.setTimestamp(1, ts);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}