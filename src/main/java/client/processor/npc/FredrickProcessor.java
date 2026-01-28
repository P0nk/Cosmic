/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
              Matthias Butz <matze@odinms.de>
              Jan Christian Meyer <vimes@odinms.de>

    Copyleft (L) 2016 - 2019 RonanLana (HeavenMS)
*/
package client.processor.npc;

import client.Character;
import client.Client;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.manipulator.InventoryManipulator;
import net.server.Server;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.maps.HiredMerchant;
import service.NoteService;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * @author RonanLana - synchronization of Fredrick modules and operation results
 */
public class FredrickProcessor {
    private static final Logger log = LoggerFactory.getLogger(FredrickProcessor.class);
    private static final int[] dailyReminders = new int[]{2, 5, 10, 15, 30, 60, 90, Integer.MAX_VALUE};

    private final NoteService noteService;

    public FredrickProcessor(NoteService noteService) {
        System.out.println("[FredrickDebug] Initializing FredrickProcessor");
        this.noteService = noteService;
    }

    private static byte canRetrieveFromFredrick(Character chr, List<Pair<Item, InventoryType>> items) {
        System.out.println("[FredrickDebug] canRetrieveFromFredrick called for " + chr.getName());
        System.out.println("[FredrickDebug] Items to retrieve count: " + items.size());

        if (!Inventory.checkSpotsAndOwnership(chr, items)) {
            System.out.println("[FredrickDebug] checkSpotsAndOwnership failed (Not enough space/slots)");
            List<Integer> itemids = new LinkedList<>();
            for (Pair<Item, InventoryType> it : items) {
                itemids.add(it.getLeft().getItemId());
            }

            if (chr.canHoldUniques(itemids)) {
                System.out.println("[FredrickDebug] Returning 0x22 (Unique Item restriction)");
                return 0x22;
            } else {
                System.out.println("[FredrickDebug] Returning 0x20 (Inventory full)");
                return 0x20;
            }
        }

        int netMeso = chr.getMerchantNetMeso();
        System.out.println("[FredrickDebug] Merchant Net Meso: " + netMeso);

        if (netMeso > 0) {
            if (!chr.canHoldMeso(netMeso)) {
                System.out.println("[FredrickDebug] canHoldMeso failed. Returning 0x1F (Meso limit)");
                return 0x1F;
            }
        } else {
            if (chr.getMeso() < -1 * netMeso) {
                System.out.println("[FredrickDebug] Player does not have enough meso to pay debt. Returning 0x21");
                return 0x21;
            }
        }

        System.out.println("[FredrickDebug] canRetrieveFromFredrick checks passed. Returning 0x0");
        return 0x0;
    }

    public static int timestampElapsedDays(Timestamp then, long timeNow) {
        // System.out.println("[FredrickDebug] Calculating elapsed days. Then: " + then + ", Now: " + timeNow);
        return (int) ((timeNow - then.getTime()) / DAYS.toMillis(1));
    }

    private static String fredrickReminderMessage(int daynotes) {
        System.out.println("[FredrickDebug] Generating reminder message for daynotes index: " + daynotes);
        String msg;

        if (daynotes < 4) {
            msg = "Hi customer! I am Fredrick, the Union Chief of the Hired Merchant Union. A reminder that " + dailyReminders[daynotes] + " days have passed since you used our service. Please reclaim your stored goods at FM Entrance.";
        } else {
            msg = "Hi customer! I am Fredrick, the Union Chief of the Hired Merchant Union. " + dailyReminders[daynotes] + " days have passed since you used our service. Consider claiming back the items before we move them away for refund.";
        }

        return msg;
    }

    public static void removeFredrickLog(int cid) {
        System.out.println("[FredrickDebug] Removing Fredrick Log for CID: " + cid);
        try (Connection con = DatabaseConnection.getConnection()) {
            removeFredrickLog(con, cid);
        } catch (SQLException sqle) {
            System.out.println("[FredrickDebug] SQLException in removeFredrickLog: " + sqle.getMessage());
            sqle.printStackTrace();
        }
    }

    private static void removeFredrickLog(Connection con, int cid) throws SQLException {
        System.out.println("[FredrickDebug] Executing DELETE FROM fredstorage WHERE cid = " + cid);
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM `fredstorage` WHERE `cid` = ?")) {
            ps.setInt(1, cid);
            int rows = ps.executeUpdate();
            System.out.println("[FredrickDebug] Rows deleted from fredstorage: " + rows);
        }
    }

    public static void insertFredrickLog(int cid) {
        System.out.println("[FredrickDebug] Inserting Fredrick Log for CID: " + cid);
        try (Connection con = DatabaseConnection.getConnection()) {

            removeFredrickLog(con, cid);
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO `fredstorage` (`cid`, `daynotes`, `timestamp`) VALUES (?, 0, ?)")) {
                ps.setInt(1, cid);
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                int rows = ps.executeUpdate();
                System.out.println("[FredrickDebug] Rows inserted into fredstorage: " + rows);
            }
        } catch (SQLException sqle) {
            System.out.println("[FredrickDebug] SQLException in insertFredrickLog: " + sqle.getMessage());
            sqle.printStackTrace();
        }
    }

    private static void removeFredrickReminders(List<Pair<Integer, Integer>> expiredCids) {
        System.out.println("[FredrickDebug] Removing reminders for " + expiredCids.size() + " expired CIDs");
        List<String> expiredCnames = new LinkedList<>();
        for (Pair<Integer, Integer> id : expiredCids) {
            String name = Character.getNameById(id.getLeft());
            if (name != null) {
                expiredCnames.add(name);
                System.out.println("[FredrickDebug] Found name for expiration: " + name);
            }
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM `notes` WHERE `from` LIKE ? AND `to` LIKE ?")) {
            ps.setString(1, "FREDRICK");

            for (String cname : expiredCnames) {
                ps.setString(2, cname);
                ps.addBatch();
                System.out.println("[FredrickDebug] Added delete note batch for: " + cname);
            }
            int[] results = ps.executeBatch();
            System.out.println("[FredrickDebug] Executed delete notes batch. Rows affected: " + results.length);
        } catch (SQLException e) {
            System.out.println("[FredrickDebug] SQLException in removeFredrickReminders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runFredrickSchedule() {
        System.out.println("[FredrickDebug] Starting runFredrickSchedule task...");
        try (Connection con = DatabaseConnection.getConnection()) {
            List<Pair<Integer, Integer>> expiredCids = new LinkedList<>();
            List<Pair<Pair<Integer, String>, Integer>> notifCids = new LinkedList<>();

            System.out.println("[FredrickDebug] Querying fredstorage table...");
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM fredstorage f LEFT JOIN (SELECT id, name, world, lastLogoutTime FROM characters) AS c ON c.id = f.cid");
                 ResultSet rs = ps.executeQuery()) {
                long curTime = System.currentTimeMillis();

                while (rs.next()) {
                    int cid = rs.getInt("cid");
                    int world = rs.getInt("world");
                    Timestamp ts = rs.getTimestamp("timestamp");
                    int daynotes = Math.min(dailyReminders.length - 1, rs.getInt("daynotes"));

                    int elapsedDays = timestampElapsedDays(ts, curTime);
                    // System.out.println("[FredrickDebug] Checking CID: " + cid + ", Elapsed Days: " + elapsedDays);

                    if (elapsedDays > 100) {
                        System.out.println("[FredrickDebug] CID " + cid + " EXPIRED (>100 days). Adding to cleanup list.");
                        expiredCids.add(new Pair<>(cid, world));
                    } else {
                        int notifDay = dailyReminders[daynotes];

                        if (elapsedDays >= notifDay) {
                            do {
                                daynotes++;
                                notifDay = dailyReminders[daynotes];
                            } while (elapsedDays >= notifDay);

                            Timestamp logoutTs = rs.getTimestamp("lastLogoutTime");
                            int inactivityDays = timestampElapsedDays(logoutTs, curTime);

                            if (inactivityDays < 7 || daynotes >= dailyReminders.length - 1) {
                                String name = rs.getString("name");
                                System.out.println("[FredrickDebug] CID " + cid + " (" + name + ") due for notification. Daynotes: " + daynotes);
                                notifCids.add(new Pair<>(new Pair<>(cid, name), daynotes));
                            }
                        }
                    }
                }
            }

            if (!expiredCids.isEmpty()) {
                System.out.println("[FredrickDebug] Processing expired items cleanup...");
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM `inventoryitems` WHERE `type` = ? AND `characterid` = ?")) {
                    ps.setInt(1, ItemFactory.MERCHANT.getValue());

                    for (Pair<Integer, Integer> cid : expiredCids) {
                        ps.setInt(2, cid.getLeft());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    System.out.println("[FredrickDebug] Deleted expired items from inventoryitems.");
                }

                try (PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET `MerchantMesos` = 0 WHERE `id` = ?")) {
                    for (Pair<Integer, Integer> cid : expiredCids) {
                        ps.setInt(1, cid.getLeft());
                        ps.addBatch();

                        World wserv = Server.getInstance().getWorld(cid.getRight());
                        if (wserv != null) {
                            Character chr = wserv.getPlayerStorage().getCharacterById(cid.getLeft());
                            if (chr != null) {
                                chr.setMerchantMeso(0);
                                System.out.println("[FredrickDebug] Reset online memory MerchantMeso for CID: " + cid.getLeft());
                            }
                        }
                    }
                    ps.executeBatch();
                    System.out.println("[FredrickDebug] Reset MerchantMesos in DB.");
                }

                removeFredrickReminders(expiredCids);

                try (PreparedStatement ps = con.prepareStatement("DELETE FROM `fredstorage` WHERE `cid` = ?")) {
                    for (Pair<Integer, Integer> cid : expiredCids) {
                        ps.setInt(1, cid.getLeft());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    System.out.println("[FredrickDebug] Deleted expired fredstorage logs.");
                }
            }

            if (!notifCids.isEmpty()) {
                System.out.println("[FredrickDebug] Processing notifications...");
                try (PreparedStatement ps = con.prepareStatement("UPDATE `fredstorage` SET `daynotes` = ? WHERE `cid` = ?")) {
                    for (Pair<Pair<Integer, String>, Integer> cid : notifCids) {
                        ps.setInt(1, cid.getRight());
                        ps.setInt(2, cid.getLeft().getLeft());
                        ps.addBatch();

                        String msg = fredrickReminderMessage(cid.getRight() - 1);
                        noteService.sendNormal(msg, "FREDRICK", cid.getLeft().getRight());
                        System.out.println("[FredrickDebug] Sent note to " + cid.getLeft().getRight());
                    }
                    ps.executeBatch();
                    System.out.println("[FredrickDebug] Updated daynotes in fredstorage.");
                }
            }
        } catch (SQLException e) {
            System.out.println("[FredrickDebug] SQLException in runFredrickSchedule: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("[FredrickDebug] runFredrickSchedule completed.");
    }

    private static boolean deleteFredrickItems(int cid) {
        System.out.println("[FredrickDebug] deleteFredrickItems called for CID: " + cid);
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM `inventoryitems` WHERE `type` = ? AND `characterid` = ?")) {
            ps.setInt(1, ItemFactory.MERCHANT.getValue()); // Type 6
            ps.setInt(2, cid);
            int rows = ps.executeUpdate();
            System.out.println("[FredrickDebug] Items deleted from DB (Type 6): " + rows);
            return true;
        } catch (SQLException e) {
            System.out.println("[FredrickDebug] SQLException in deleteFredrickItems: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void fredrickRetrieveItems(Client c) {
        System.out.println("[FredrickDebug] fredrickRetrieveItems triggered for: " + c.getPlayer().getName());
        if (c.tryacquireClient()) {
            try {
                Character chr = c.getPlayer();

                // 1. Check DB Flag
                if (chr.hasMerchant()) {
                    System.out.println("[FredrickDebug] Fail: chr.hasMerchant() is true.");
                    chr.dropMessage(1, "You cannot use Fredrick while your store is open.\r\nPlease close your Hired Merchant first.");
                    return;
                }

                // 2. Check World Server (Crucial if DB flag desyncs)
                if (Server.getInstance().getWorld(chr.getWorld()).getHiredMerchant(chr.getId()) != null) {
                    System.out.println("[FredrickDebug] Fail: World server reports active HiredMerchant object.");
                    chr.dropMessage(1, "Your store is currently open in the Free Market.\r\nPlease close it before retrieving items.");
                    return;
                }

                List<Pair<Item, InventoryType>> items;
                try {
                    System.out.println("[FredrickDebug] Loading items from ItemFactory for CID: " + chr.getId());
                    items = ItemFactory.MERCHANT.loadItems(chr.getId(), false);
                    System.out.println("[FredrickDebug] Items loaded: " + items.size());

                    byte response = canRetrieveFromFredrick(chr, items);
                    if (response != 0) {
                        System.out.println("[FredrickDebug] canRetrieveFromFredrick returned error code: " + response);
                        chr.sendPacket(PacketCreator.fredrickMessage(response));
                        return;
                    }

                    System.out.println("[FredrickDebug] Withdrawing Merchant Mesos...");
                    chr.withdrawMerchantMesos();

                    System.out.println("[FredrickDebug] Attempting to delete items from DB...");
                    if (deleteFredrickItems(chr.getId())) {
                        System.out.println("[FredrickDebug] DB Deletion successful. Processing memory objects...");
                        HiredMerchant merchant = chr.getHiredMerchant();

                        if (merchant != null) {
                            System.out.println("[FredrickDebug] Clearing active HiredMerchant object items");
                            merchant.clearItems();
                        } else {
                            System.out.println("[FredrickDebug] No active HiredMerchant object found on char.");
                        }

                        System.out.println("[FredrickDebug] Adding items to player inventory...");
                        for (Pair<Item, InventoryType> it : items) {
                            Item item = it.getLeft();
                            System.out.println("[FredrickDebug] Adding Item ID: " + item.getItemId() + ", Qty: " + item.getQuantity() + ", Owner: " + item.getOwner());

                            InventoryManipulator.addFromDrop(chr.getClient(), item, false);

                            String itemName = ItemInformationProvider.getInstance().getName(item.getItemId());
                            log.debug("Chr {} gained {}x {} ({})", chr.getName(), item.getQuantity(), itemName, item.getItemId());
                        }

                        System.out.println("[FredrickDebug] Sending success packet (0x1E)");
                        chr.sendPacket(PacketCreator.fredrickMessage((byte) 0x1E));

                        System.out.println("[FredrickDebug] Removing Fredrick Log...");
                        removeFredrickLog(chr.getId());
                        System.out.println("[FredrickDebug] Retrieval Complete.");
                    } else {
                        System.out.println("[FredrickDebug] deleteFredrickItems failed (SQL Error usually).");
                        chr.message("An unknown error has occured.");
                    }
                } catch (SQLException ex) {
                    System.out.println("[FredrickDebug] Critical SQLException in retrieval: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } finally {
                c.releaseClient();
                System.out.println("[FredrickDebug] Client lock released.");
            }
        } else {
            System.out.println("[FredrickDebug] Failed to acquire client lock.");
        }
    }
}