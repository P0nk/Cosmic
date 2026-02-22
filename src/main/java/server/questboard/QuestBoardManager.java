// Final QuestBoardManager.java aligned to flat SQL schema
package server.questboard;

import client.Character;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator; // Added import
import constants.id.ItemId;
import scripting.AbstractPlayerInteraction; // Added import
import server.ItemInformationProvider;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.*;
import java.util.*;

public class QuestBoardManager {

    public static List<Map<String, Object>> getOpenQuests() {
        List<Map<String, Object>> list = new ArrayList<>();
        String query = "SELECT q.*, c.name AS creator_name " +
                "FROM quest_board q " +
                "LEFT JOIN cosmic.characters c ON q.created_by = c.id " +
                "WHERE q.status = 'OPEN' " +
                "AND DATE_FORMAT(q.created_at, '%Y-%m-%d %H:%i:%s') + INTERVAL 14 DAY > NOW() " +
                "ORDER BY q.quest_id DESC";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(query);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> quest = new HashMap<>();
                quest.put("quest_id", rs.getInt("quest_id"));
                quest.put("created_by", rs.getInt("created_by"));
                quest.put("creator_name", rs.getString("creator_name"));
                quest.put("requirement_itemid", rs.getInt("requirement_itemid"));
                quest.put("requirement_quantity", rs.getInt("requirement_quantity"));
                quest.put("reward_meso", rs.getLong("reward_meso"));
                quest.put("reward_nx", rs.getLong("reward_nx"));
                quest.put("reward_item1_id", rs.getInt("reward_item1_id"));
                quest.put("reward_item1_qty", rs.getInt("reward_item1_qty"));
                quest.put("reward_item2_id", rs.getInt("reward_item2_id"));
                quest.put("reward_item2_qty", rs.getInt("reward_item2_qty"));
                list.add(quest);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static boolean createQuest(Character player, int itemId, int quantity,
            long meso, long nx, Integer item1Id, Integer item1Qty, Integer item2Id, Integer item2Qty) {
        if (player.getWorld() == 1) {
            player.message("The Quest Board is not available in your world.");
            return false;
        }

        if (player.getMeso() < 10_000_000) {
            System.out.println("[QuestBoard] Not enough mesos to create quest.");
            return false;
        }

        player.gainMeso(-10_000_000, false); // charge fee

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO quest_board (created_by, requirement_itemid, requirement_quantity, reward_meso, reward_nx, reward_item1_id, reward_item1_qty, reward_item2_id, reward_item2_qty) "
                                +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {

            // Print values for debug
            System.out.println(
                    "[QuestBoard] Inserting quest for Player: " + player.getName() + " (ID: " + player.getId() + ")");
            String itemName = ItemInformationProvider.getInstance().getName(itemId);
            System.out.println(" - Requirement: " + itemName + " (ID: " + itemId + ") x" + quantity);
            System.out.println(" - Reward Mesos: " + meso);
            System.out.println(" - Reward NX: " + nx);
            if (item1Id != null)
                System.out.println(" - Reward Item 1: " + item1Id + " x" + item1Qty);
            if (item2Id != null)
                System.out.println(" - Reward Item 2: " + item2Id + " x" + item2Qty);

            ps.setInt(1, player.getId());
            ps.setInt(2, itemId);
            ps.setInt(3, quantity);
            ps.setLong(4, meso);
            ps.setLong(5, nx);
            ps.setObject(6, item1Id);
            ps.setObject(7, item1Qty);
            ps.setObject(8, item2Id);
            ps.setObject(9, item2Qty);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("[QuestBoard] Quest inserted successfully.");
                return true;
            } else {
                System.out.println("[QuestBoard] Insert failed — no rows affected.");
            }

        } catch (SQLException e) {
            System.out.println("[QuestBoard] SQL Exception during quest insert:");
            e.printStackTrace();
        }

        return false;
    }

    // New Atomic Method for completing quest
    public static String completeQuest(Character player, int questId) {
        if (player.getWorld() == 1) {
            return "The Quest Board is not available in your world.";
        }

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Start transaction

            // 1. Lock the row
            PreparedStatement select = con.prepareStatement("SELECT * FROM quest_board WHERE quest_id = ? FOR UPDATE");
            select.setInt(1, questId);
            ResultSet rs = select.executeQuery();

            if (!rs.next()) {
                con.rollback();
                return "Quest not found.";
            }

            // 2. Validate Status
            String status = rs.getString("status");
            if (!"OPEN".equalsIgnoreCase(status)) {
                con.rollback();
                return "This quest is no longer open.";
            }

            // 3. Load Data
            int reqItemId = rs.getInt("requirement_itemid");
            int reqQty = rs.getInt("requirement_quantity");
            long rewardMeso = rs.getLong("reward_meso");
            long rewardNx = rs.getLong("reward_nx");
            int item1Id = rs.getInt("reward_item1_id");
            int item1Qty = rs.getInt("reward_item1_qty");
            int item2Id = rs.getInt("reward_item2_id");
            int item2Qty = rs.getInt("reward_item2_qty");

            // 4. Validate Requirements (Double check atomic)
            if (player.getItemQuantity(reqItemId, false) < reqQty) {
                con.rollback();
                return "You do not have the required items.";
            }

            // 5. Validate Inventory Space for Rewards
            // Using InventoryManipulator.checkSpace
            if (item1Id > 0 && item1Qty > 0
                    && !InventoryManipulator.checkSpace(player.getClient(), item1Id, item1Qty, "")) {
                con.rollback();
                return "Please make some space in your inventory.";
            }
            if (item2Id > 0 && item2Qty > 0
                    && !InventoryManipulator.checkSpace(player.getClient(), item2Id, item2Qty, "")) {
                con.rollback();
                return "Please make some space in your inventory.";
            }
            if (rewardMeso > 0 && (player.getMeso() + rewardMeso < 0)) { // Overflow check
                con.rollback();
                return "You are holding too much meso.";
            }
            if (rewardNx > 0 && (player.getCashShop().getCash(1) + rewardNx < 0)) { // Overflow check
                con.rollback();
                return "You are holding too much NX.";
            }

            // 6. Update DB Status
            PreparedStatement update = con.prepareStatement(
                    "UPDATE quest_board SET status = 'COMPLETED', completed_by = ?, is_reward_claimed = 1, reward_claimed_on = NOW() WHERE quest_id = ?");
            update.setInt(1, player.getId());
            update.setInt(2, questId);
            int rows = update.executeUpdate();

            if (rows == 0) {
                con.rollback();
                return "Error updating quest status.";
            }

            // 7. Commit Transaction (DB State is now secured)
            con.commit();
            con.setAutoCommit(true);

            // 8. Process Game Actions (Take Items, Give Rewards)

            // Take Requirements
            AbstractPlayerInteraction.gainItem(player.getClient(), reqItemId, (short) -reqQty);

            // Give Rewards
            if (rewardMeso > 0)
                player.gainMeso((int) rewardMeso, true);
            if (rewardNx > 0)
                player.getCashShop().gainCash(1, (int) rewardNx);
            if (item1Id > 0 && item1Qty > 0)
                AbstractPlayerInteraction.gainItem(player.getClient(), item1Id, (short) item1Qty);
            if (item2Id > 0 && item2Qty > 0)
                AbstractPlayerInteraction.gainItem(player.getClient(), item2Id, (short) item2Qty);

            System.out.println("[QuestBoard] Quest " + questId + " completed atomically by " + player.getName());

            return "Success";

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (con != null)
                    con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return "An internal error occurred.";
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    // Deprecated but kept for backward compatibility if needed
    public static boolean fulfillQuest(Character player, int questId) {
        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con
                        .prepareStatement("SELECT * FROM quest_board WHERE quest_id = ? AND status = 'OPEN'")) {
            ps.setInt(1, questId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                return false;

            int reqItemId = rs.getInt("requirement_itemid");
            int reqQty = rs.getInt("requirement_quantity");
            String itemName = ItemInformationProvider.getInstance().getName(reqItemId);
            System.out.println(
                    "[QuestBoard] Quest fulfilled by Player: " + player.getName() + " (ID: " + player.getId() + ")");
            System.out.println(" - Submitted: " + itemName + " (ID: " + reqItemId + ") x" + reqQty);

            try (PreparedStatement update = con.prepareStatement(
                    "UPDATE quest_board SET status = 'COMPLETED', completed_by = ?, is_reward_claimed = 0 WHERE quest_id = ?")) {
                update.setInt(1, player.getId());
                update.setInt(2, questId);
                update.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean claimReward(Character player, int questId) {
        if (player.getWorld() == 1) {
            player.message("The Quest Board is not available in your world.");
            return false;
        }

        System.out.println("[QuestBoard] Attempting to claim reward for quest ID: " + questId + ", Player ID: "
                + player.getId() + " (" + player.getName() + ")");

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con
                        .prepareStatement("SELECT * FROM quest_board WHERE quest_id = ? AND is_reward_claimed = 0")) {

            ps.setInt(1, questId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                return false;

            int reqItemId = rs.getInt("requirement_itemid");
            int reqQty = rs.getInt("requirement_quantity");
            long mesos = rs.getLong("reward_meso");
            long nx = rs.getLong("reward_nx");
            int item1 = rs.getInt("reward_item1_id");
            int qty1 = rs.getInt("reward_item1_qty");
            int item2 = rs.getInt("reward_item2_id");
            int qty2 = rs.getInt("reward_item2_qty");

            // Corrected check using InventoryManipulator
            if (item1 > 0 && !InventoryManipulator.checkSpace(player.getClient(), item1, qty1, ""))
                return false;
            if (item2 > 0 && !InventoryManipulator.checkSpace(player.getClient(), item2, qty2, ""))
                return false;

            // Give Rewards
            if (mesos > 0)
                player.gainMeso((int) mesos, true);
            if (nx > 0)
                player.getCashShop().gainCash(1, (int) nx);
            if (item1 > 0 && qty1 > 0)
                AbstractPlayerInteraction.gainItem(player.getClient(), item1, (short) qty1);
            if (item2 > 0 && qty2 > 0)
                AbstractPlayerInteraction.gainItem(player.getClient(), item2, (short) qty2);

            try (PreparedStatement upd = con.prepareStatement(
                    "UPDATE quest_board SET is_reward_claimed = 1, reward_claimed_on = NOW() WHERE quest_id = ?")) {
                upd.setInt(1, questId);
                upd.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean claimRequirements(Character player, int questId) {
        if (player.getWorld() == 1) {
            player.message("The Quest Board is not available in your world.");
            return false;
        }

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT * FROM quest_board WHERE quest_id = ? AND created_by = ? AND status = 'COMPLETED' AND is_req_claimed = 0")) {
            ps.setInt(1, questId);
            ps.setInt(2, player.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                return false;

            int itemId = rs.getInt("requirement_itemid");
            short qty = rs.getShort("requirement_quantity");
            String itemName = ItemInformationProvider.getInstance().getName(itemId);
            System.out.println("[QuestBoard] Player " + player.getName() + " (ID: " + player.getId()
                    + ") claimed completed quest requirement: " + itemName + " (ID: " + itemId + ") x" + qty);

            AbstractPlayerInteraction.gainItem(player.getClient(), itemId, qty);

            try (PreparedStatement upd = con.prepareStatement(
                    "UPDATE quest_board SET is_req_claimed = 1, req_claimed_on = NOW() WHERE quest_id = ?")) {
                upd.setInt(1, questId);
                upd.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean withdrawQuest(Character player, int questId) {
        if (player.getWorld() == 1) {
            player.message("The Quest Board is not available in your world.");
            return false;
        }

        System.out.println("[QuestBoard] Attempting to withdraw quest ID: " + questId + ", Player ID: " + player.getId()
                + " (" + player.getName() + ")");

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement select = con.prepareStatement(
                        "SELECT * FROM quest_board WHERE quest_id = ? AND created_by = ? AND status = 'OPEN'")) {

            select.setInt(1, questId);
            select.setInt(2, player.getId());
            ResultSet rs = select.executeQuery();

            if (!rs.next()) {
                System.out.println("[QuestBoard] No matching OPEN quest found to withdraw.");
                return false;
            }

            int reqItemId = rs.getInt("requirement_itemid");
            int reqQty = rs.getInt("requirement_quantity");

            long mesos = rs.getLong("reward_meso");
            long nx = rs.getLong("reward_nx");
            int item1 = rs.getInt("reward_item1_id");
            short qty1 = rs.getShort("reward_item1_qty");
            int item2 = rs.getInt("reward_item2_id");
            short qty2 = rs.getShort("reward_item2_qty");

            if (mesos > 0)
                player.gainMeso((int) mesos, true);
            if (nx > 0)
                player.getCashShop().gainCash(1, (int) nx);
            if (item1 > 0 && qty1 > 0)
                AbstractPlayerInteraction.gainItem(player.getClient(), item1, (short) qty1);
            if (item2 > 0 && qty2 > 0)
                AbstractPlayerInteraction.gainItem(player.getClient(), item2, (short) qty2);

            try (PreparedStatement upd = con
                    .prepareStatement("UPDATE quest_board SET status = 'WITHDRAWN' WHERE quest_id = ?")) {
                upd.setInt(1, questId);
                int updated = upd.executeUpdate();
                System.out.println("[QuestBoard] Quest marked as WITHDRAWN. Rows updated: " + updated);
                return updated > 0;
            }

        } catch (SQLException e) {
            System.out.println("[QuestBoard] SQL exception while withdrawing quest:");
            e.printStackTrace();
        }

        return false;
    }

    public static List<Map<String, Object>> getPlayerQuests(Character player) {
        List<Map<String, Object>> list = new ArrayList<>();
        String query = "SELECT q.*, c.name AS creator_name " +
                "FROM quest_board q " +
                "LEFT JOIN cosmic.characters c ON q.created_by = c.id " +
                "WHERE q.created_by = ?";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, player.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> quest = new HashMap<>();
                quest.put("quest_id", rs.getInt("quest_id"));
                quest.put("status", rs.getString("status"));
                quest.put("created_by", rs.getInt("created_by"));
                quest.put("creator_name", rs.getString("creator_name"));
                quest.put("completed_by", rs.getInt("completed_by"));
                quest.put("is_req_claimed", rs.getInt("is_req_claimed"));
                quest.put("requirement_itemid", rs.getInt("requirement_itemid"));
                quest.put("requirement_quantity", rs.getInt("requirement_quantity"));
                list.add(quest);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static List<Pair<Integer, String>> getItemInformationProvider() {
        return ItemInformationProvider.getInstance().getAllItems();
    }

    public static Inventory getInventory(Character player, int type) {
        return player.getInventory(InventoryType.getByType((byte) type));
    }

    public static int hasEnoughSlots(Character player, int inv) {
        return getInventory(player, inv).getNumFreeSlot();
    }
}