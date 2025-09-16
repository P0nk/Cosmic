// Final QuestBoardManager.java aligned to flat SQL schema
package server.questboard;

import client.Character;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import constants.id.ItemId;
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
                "WHERE q.status = 'OPEN'" +
                "AND DATE_FORMAT(q.created_at, '%Y-%m-%d %H:%i:%s') + INTERVAL 14 DAY > NOW()" +
                "ORDER BY q.quest_id DESC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> quest = new HashMap<>();
                quest.put("quest_id", rs.getInt("quest_id"));
                quest.put("created_by", rs.getInt("created_by"));
                quest.put("creator_name", rs.getString("creator_name")); // newly added
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
        if (player.getMeso() < 10_000_000) {
            System.out.println("[QuestBoard] Not enough mesos to create quest.");
            return false;
        }

        player.gainMeso(-10_000_000, false); // charge fee

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO quest_board (created_by, requirement_itemid, requirement_quantity, reward_meso, reward_nx, reward_item1_id, reward_item1_qty, reward_item2_id, reward_item2_qty) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {

            // Print values for debug
            System.out.println("[QuestBoard] Inserting quest for Player: " + player.getName() + " (ID: " + player.getId() + ")");
            String itemName = ItemInformationProvider.getInstance().getName(itemId);
            System.out.println(" - Requirement: " + itemName + " (ID: " + itemId + ") x" + quantity);
            System.out.println(" - Reward Mesos: " + meso);
            System.out.println(" - Reward NX: " + nx);
            if (item1Id != null) System.out.println(" - Reward Item 1: " + item1Id + " x" + item1Qty);
            if (item2Id != null) System.out.println(" - Reward Item 2: " + item2Id + " x" + item2Qty);

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

    public static boolean fulfillQuest(Character player, int questId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board WHERE quest_id = ? AND status = 'OPEN'")) {
            ps.setInt(1, questId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;

            int reqItemId = rs.getInt("requirement_itemid");
            int reqQty = rs.getInt("requirement_quantity");
            String itemName = ItemInformationProvider.getInstance().getName(reqItemId);
            System.out.println("[QuestBoard] Quest fulfilled by Player: " + player.getName() + " (ID: " + player.getId() + ")");
            System.out.println(" - Submitted: " + itemName + " (ID: " + reqItemId + ") x" + reqQty);

            try (PreparedStatement update = con.prepareStatement("UPDATE quest_board SET status = 'COMPLETED', completed_by = ?, is_reward_claimed = 0 WHERE quest_id = ?")) {
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
        System.out.println("[QuestBoard] Attempting to claim reward for quest ID: " + questId + ", Player ID: " + player.getId() + " (" + player.getName() + ")");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board WHERE quest_id = ? AND is_reward_claimed = 0")) {

            ps.setInt(1, questId);
//            ps.setInt(2, player.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;

            // Requirement info
            int reqItemId = rs.getInt("requirement_itemid");
            int reqQty = rs.getInt("requirement_quantity");
            String reqItemName = ItemInformationProvider.getInstance().getName(reqItemId);
            if (reqItemName == null || reqItemName.isEmpty()) reqItemName = "(Unknown Item)";
            System.out.println("[QuestBoard] Requirement submitted: " + reqItemName + " (ID: " + reqItemId + ") x" + reqQty);

            // Reward info
            int mesos = rs.getInt("reward_meso");
            int nx = rs.getInt("reward_nx");
            int item1 = rs.getInt("reward_item1_id");
            short qty1 = rs.getShort("reward_item1_qty");
            int item2 = rs.getInt("reward_item2_id");
            short qty2 = rs.getShort("reward_item2_qty");

            System.out.println("[QuestBoard] Reward contents:");
            System.out.println(" - Mesos: " + mesos);
            System.out.println(" - NX: " + nx);
            System.out.println(" - Item 1: " + item1 + " x" + qty1);
            System.out.println(" - Item 2: " + item2 + " x" + qty2);

            int item1type = Math.max((item1 / 1000000), 1); // if item is < 2000000 [equip; inventory 1] else inventory = 1st number of itemid
            int item2type = Math.max((item1 / 1000000), 1);
            List<Boolean> checkList = new ArrayList<>();

            if (mesos > 0 && (player.getMeso() + mesos < 0)) { // meso overflow
                checkList.add(false);
            } else {
                checkList.add(true);
            }
            if (nx > 0 && (player.getCashShop().getCash(1) + nx < 0)) { // nx overflow
                checkList.add(false);
            } else {
                checkList.add(true);
            }
            if (item1 > 0 && qty1 > 0) {
                if (hasEnoughSlots(player, item1type) >= 2) { // at least 2 slots
                    checkList.add(true);
                } else {
                    checkList.add(false);
                }
            } else {
                checkList.add(true);
            }
            if (item2 > 0) {
                if (hasEnoughSlots(player, item2type) >= 2) { // at least 2 slots
                    checkList.add(true);
                } else {
                    checkList.add(false);
                }
            } else {
                checkList.add(true);
            }

            System.out.println(checkList);
            if (checkList.contains(false)) {
                return false;
            } else {
                if (mesos > 0) player.gainMeso(mesos, true, false, true);
                if (nx > 0) player.getCashShop().gainCash(1, nx);
                if (qty1 > 0) player.getAbstractPlayerInteraction().gainItem(item1, qty1);
                if (qty2 > 0) player.getAbstractPlayerInteraction().gainItem(item2, qty2);
            }

            try (PreparedStatement upd = con.prepareStatement("UPDATE quest_board SET is_reward_claimed = 1, reward_claimed_on = NOW() WHERE quest_id = ?")) {
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
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board WHERE quest_id = ? AND created_by = ? AND status = 'COMPLETED' AND is_req_claimed = 0")) {
            ps.setInt(1, questId);
            ps.setInt(2, player.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;

            int itemId = rs.getInt("requirement_itemid");
            short qty = rs.getShort("requirement_quantity");
            String itemName = ItemInformationProvider.getInstance().getName(itemId);
            System.out.println("[QuestBoard] Player " + player.getName() + " (ID: " + player.getId() + ") claimed completed quest requirement: " + itemName + " (ID: " + itemId + ") x" + qty);

            player.getAbstractPlayerInteraction().gainItem(itemId, qty);

            try (PreparedStatement upd = con.prepareStatement("UPDATE quest_board SET is_req_claimed = 1, req_claimed_on = NOW() WHERE quest_id = ?")) {
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
        System.out.println("[QuestBoard] Attempting to withdraw quest ID: " + questId + ", Player ID: " + player.getId() + " (" + player.getName() + ")");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement select = con.prepareStatement("SELECT * FROM quest_board WHERE quest_id = ? AND created_by = ? AND status = 'OPEN'")) {

            select.setInt(1, questId);
            select.setInt(2, player.getId());
            ResultSet rs = select.executeQuery();

            if (!rs.next()) {
                System.out.println("[QuestBoard] No matching OPEN quest found to withdraw.");
                return false;
            }

            int reqItemId = rs.getInt("requirement_itemid");
            int reqQty = rs.getInt("requirement_quantity");
            String reqItemName = ItemInformationProvider.getInstance().getName(reqItemId);
            System.out.println("[QuestBoard] Requirement: " + reqItemName + " (ID: " + reqItemId + ") x" + reqQty);

            int mesos = rs.getInt("reward_meso");
            int nx = rs.getInt("reward_nx");
            int item1 = rs.getInt("reward_item1_id");
            short qty1 = rs.getShort("reward_item1_qty");
            int item2 = rs.getInt("reward_item2_id");
            short qty2 = rs.getShort("reward_item2_qty");

            System.out.println("[QuestBoard] Refunding:");
            System.out.println(" - Mesos: " + mesos);
            System.out.println(" - NX: " + nx);
            System.out.println(" - Item 1: " + item1 + " x" + qty1);
            System.out.println(" - Item 2: " + item2 + " x" + qty2);

            if (mesos > 0) player.gainMeso(mesos, true, false, true);
            if (nx > 0) player.getCashShop().gainCash(1, nx);
            if (item1 > 0 && qty1 > 0) player.getAbstractPlayerInteraction().gainItem(item1, qty1);
            if (item2 > 0 && qty2 > 0) player.getAbstractPlayerInteraction().gainItem(item2, qty2);

            try (PreparedStatement upd = con.prepareStatement("UPDATE quest_board SET status = 'WITHDRAWN' WHERE quest_id = ?")) {
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
                "WHERE q.created_by = ?" +
                "ORDER BY q.quest_id ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, player.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> quest = new HashMap<>();
                quest.put("quest_id", rs.getInt("quest_id"));
                quest.put("status", rs.getString("status"));
                quest.put("created_by", rs.getInt("created_by"));
                quest.put("creator_name", rs.getString("creator_name"));  // newly added
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



//    public static List<Map<String, Object>> getPlayerQuests(Character player) {
//        List<Map<String, Object>> list = new ArrayList<>();
//        try (Connection con = DatabaseConnection.getConnection();
//             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board WHERE created_by = ?")) {
//            ps.setInt(1, player.getId());
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                Map<String, Object> quest = new HashMap<>();
//                quest.put("quest_id", rs.getInt("quest_id"));
//                quest.put("status", rs.getString("status"));
//                quest.put("completed_by", rs.getInt("completed_by"));
//                quest.put("is_req_claimed", rs.getInt("is_req_claimed"));
//                quest.put("requirement_itemid", rs.getInt("requirement_itemid"));
//                quest.put("requirement_quantity", rs.getInt("requirement_quantity"));
//                list.add(quest);
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return list;
//    }

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