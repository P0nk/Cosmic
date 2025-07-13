// Final QuestBoardManager.java aligned to flat SQL schema
package server.questboard;

import client.Character;
import constants.id.ItemId;
import server.ItemInformationProvider;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.*;
import java.util.*;

public class QuestBoardManager {

    public static List<Map<String, Object>> getOpenQuests() {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board WHERE status = 'OPEN'");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> quest = new HashMap<>();
                quest.put("quest_id", rs.getInt("quest_id"));
                quest.put("created_by", rs.getInt("created_by"));
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
            System.out.println("[QuestBoard] Inserting quest:");
            System.out.println(" - Player ID: " + player.getId());
            System.out.println(" - Req Item ID: " + itemId);
            System.out.println(" - Req Qty: " + quantity);
            System.out.println(" - Reward Mesos: " + meso);
            System.out.println(" - Reward NX: " + nx);
            System.out.println(" - Reward Item 1: " + item1Id + " x " + item1Qty);
            System.out.println(" - Reward Item 2: " + item2Id + " x " + item2Qty);

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


//    public static boolean createQuest(Character player, int itemId, int quantity,
//                                      long meso, long nx, Integer item1Id, Integer item1Qty, Integer item2Id, Integer item2Qty) {
//        if (player.getMeso() < 10_000_000) return false;
//        player.gainMeso(-10_000_000, false);
//
//        try (Connection con = DatabaseConnection.getConnection();
//             PreparedStatement ps = con.prepareStatement("INSERT INTO quest_board (created_by, requirement_itemid, requirement_quantity, reward_meso, reward_nx, reward_item1_id, reward_item1_qty, reward_item2_id, reward_item2_qty) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
//            ps.setInt(1, player.getId());
//            ps.setInt(2, itemId);
//            ps.setInt(3, quantity);
//            ps.setLong(4, meso);
//            ps.setLong(5, nx);
//            ps.setObject(6, item1Id);
//            ps.setObject(7, item1Qty);
//            ps.setObject(8, item2Id);
//            ps.setObject(9, item2Qty);
//            ps.executeUpdate();
//            return true;
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    public static boolean fulfillQuest(Character player, int questId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board WHERE quest_id = ? AND status = 'OPEN'")) {
            ps.setInt(1, questId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;

//            int reqItemId = rs.getInt("requirement_itemid");
//            short reqQty = rs.getShort("requirement_quantity");

//            if (!player.getAbstractPlayerInteraction().haveItem(reqItemId, reqQty)) return false;
//            player.getAbstractPlayerInteraction().gainItem(reqItemId, (short) -reqQty, true);

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
        System.out.println("[QuestBoard] Attempting to claim reward for quest ID: " + questId + ", Player ID: " + player.getId());

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM quest_board WHERE quest_id = ? AND completed_by = ? AND is_reward_claimed = 0")) {

            ps.setInt(1, questId);
            ps.setInt(2, player.getId());

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("[QuestBoard] No matching quest found or reward already claimed.");
                return false;
            }

            // Log the reward info from DB
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

            // Apply rewards
            if (mesos > 0) {
                player.gainMeso(mesos, true, false, true);
            }

            if (nx > 0) {
                player.getCashShop().gainCash(1, nx);
            }

            if (item1 > 0 && qty1 > 0) {
                player.getAbstractPlayerInteraction().gainItem(item1, qty1);
            }

            if (item2 > 0 && qty2 > 0) {
                player.getAbstractPlayerInteraction().gainItem(item2, qty2);
            }

            // Update reward status
            try (PreparedStatement upd = con.prepareStatement(
                    "UPDATE quest_board SET is_reward_claimed = 1, reward_claimed_on = NOW() WHERE quest_id = ?")) {
                upd.setInt(1, questId);
                int updated = upd.executeUpdate();
                System.out.println("[QuestBoard] Marked quest reward as claimed. Rows updated: " + updated);
            }

            return true;

        } catch (SQLException e) {
            System.out.println("[QuestBoard] SQL exception while claiming reward:");
            e.printStackTrace();
        }

        return false;
    }


//    public static boolean claimReward(Character player, int questId) {
//        try (Connection con = DatabaseConnection.getConnection();
//             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board WHERE quest_id = ? AND completed_by = ? AND is_reward_claimed = 0")) {
//            ps.setInt(1, questId);
//            ps.setInt(2, player.getId());
//            ResultSet rs = ps.executeQuery();
//            if (!rs.next()) return false;
//            player.gainMeso(rs.getInt("reward_meso"), true);
//            player.getCashShop().gainCash(1,(int) rs.getInt("reward_nx") );
//            int item1 = rs.getInt("reward_item1_id");
//            short qty1 = rs.getShort("reward_item1_qty");
//           if (item1 > 0 && qty1 > 0) player.getAbstractPlayerInteraction().gainItem(item1, qty1);
//
//            int item2 = rs.getInt("reward_item2_id");
//            short qty2 = rs.getShort("reward_item2_qty");
//            if (item2 > 0 && qty2 > 0) player.getAbstractPlayerInteraction().gainItem(item2, qty2);
//
//            try (PreparedStatement upd = con.prepareStatement("UPDATE quest_board SET is_reward_claimed = 1, reward_claimed_on = NOW() WHERE quest_id = ?")) {
//                upd.setInt(1, questId);
//                upd.executeUpdate();
//            }
//
//            return true;
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    public static boolean claimRequirements(Character player, int questId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board WHERE quest_id = ? AND created_by = ? AND status = 'COMPLETED' AND is_req_claimed = 0")) {
            ps.setInt(1, questId);
            ps.setInt(2, player.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;

            int itemId = rs.getInt("requirement_itemid");
            short qty = rs.getShort("requirement_quantity");
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
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE quest_board SET status = 'WITHDRAWN' WHERE quest_id = ? AND created_by = ? AND status = 'OPEN'")) {
            ps.setInt(1, questId);
            ps.setInt(2, player.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<Map<String, Object>> getPlayerQuests(Character player) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board WHERE created_by = ?")) {
            ps.setInt(1, player.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> quest = new HashMap<>();
                quest.put("quest_id", rs.getInt("quest_id"));
                quest.put("status", rs.getString("status"));
                quest.put("completed_by", rs.getInt("completed_by"));
                quest.put("is_req_claimed", rs.getInt("is_req_claimed"));
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
}