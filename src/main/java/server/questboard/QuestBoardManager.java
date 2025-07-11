package server.questboard;

import tools.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class QuestBoardManager {

    private static Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    // -------- QUEST_BOARD (Main Metadata) --------
    public static void insertQuest(int questId, String title, String description, int createdBy, boolean isGM, Timestamp deadline, boolean repeatable, int maxCompletions, long taxPaid) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO quest_board (quest_id, title, description, created_by, is_gm_quest, deadline, repeatable, max_completions, tax_paid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, questId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setInt(4, createdBy);
            ps.setBoolean(5, isGM);
            ps.setTimestamp(6, deadline);
            ps.setBoolean(7, repeatable);
            ps.setInt(8, maxCompletions);
            ps.setLong(9, taxPaid);
            ps.executeUpdate();
        }
    }

    public static ResultSet getQuestById(Connection con, int questId) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board WHERE quest_id = ?");
        ps.setInt(1, questId);
        return ps.executeQuery();
    }

    public static int getMaxQuestId() throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT MAX(quest_id) AS max_id FROM quest_board");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("max_id") : -1;
        }
    }

    // -------- QUEST_BOARD_REQUIREMENTS --------
    public static void insertRequirement(int questId, int itemId, int quantity) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO quest_board_requirements (quest_id, item_id, quantity) VALUES (?, ?, ?)")) {
            ps.setInt(1, questId);
            ps.setInt(2, itemId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        }
    }

    public static List<Map<String, Object>> getRequirementsByQuestIdAsList(int questId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board_requirements WHERE quest_id = ?")) {
            ps.setInt(1, questId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("item_id", rs.getInt("item_id"));
                row.put("quantity", rs.getInt("quantity"));
                results.add(row);
            }
        }
        return results;
    }

    // -------- QUEST_BOARD_ITEM_REWARDS --------
    public static void insertItemReward(int questId, int itemId, long quantity) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO quest_board_item_rewards (quest_id, item_id, quantity) VALUES (?, ?, ?)")) {
            ps.setInt(1, questId);
            ps.setInt(2, itemId);
            ps.setLong(3, quantity);
            ps.executeUpdate();
        }
    }

    public static List<Map<String, Object>> getItemRewardsByQuestIdAsList(int questId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board_item_rewards WHERE quest_id = ?")) {
            ps.setInt(1, questId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("item_id", rs.getInt("item_id"));
                row.put("quantity", rs.getLong("quantity"));
                results.add(row);
            }
        }
        return results;
    }

    // -------- QUEST_BOARD_CURRENCY_REWARDS --------
    public static void insertCurrencyReward(int questId, String rewardType, long amount, int nxType) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO quest_board_currency_rewards (quest_id, reward_type, amount, nx_type) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, questId);
            ps.setString(2, rewardType);
            ps.setLong(3, amount);
            ps.setInt(4, nxType);
            ps.executeUpdate();
        }
    }

    public static List<Map<String, Object>> getCurrencyRewardsByQuestIdAsList(int questId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_board_currency_rewards WHERE quest_id = ?")) {
            ps.setInt(1, questId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("reward_type", rs.getString("reward_type"));
                row.put("amount", rs.getLong("amount"));
                row.put("nx_type", rs.getInt("nx_type"));
                results.add(row);
            }
        }
        return results;
    }

    // -------- QUEST_BOARD_CLAIMS --------
    public static void insertClaim(int questId, int creatorId) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO quest_board_claims (quest_id, creator_id) VALUES (?, ?)")) {
            ps.setInt(1, questId);
            ps.setInt(2, creatorId);
            ps.executeUpdate();
        }
    }

    // -------- QUEST_BOARD_SUBMISSIONS --------
    public static void insertSubmission(int questId, int characterId, boolean rewardClaimed) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO quest_board_submissions (quest_id, character_id, reward_claimed) VALUES (?, ?, ?)")) {
            ps.setInt(1, questId);
            ps.setInt(2, characterId);
            ps.setBoolean(3, rewardClaimed);
            ps.executeUpdate();
        }
    }

    // -------- QUEST_BOARD_LOGS --------
    public static void insertLog(int questId, int actorId, String action, String details) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO quest_board_logs (quest_id, actor_id, action, details) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, questId);
            ps.setInt(2, actorId);
            ps.setString(3, action);
            ps.setString(4, details);
            ps.executeUpdate();
        }
    }
}
