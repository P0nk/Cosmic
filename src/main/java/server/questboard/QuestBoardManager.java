package server.questboard;

import client.Character;
import tools.DatabaseConnection;

import java.sql.*;
import java.util.*;

public class QuestBoardManager {

    public static boolean registerFullQuest(int creatorId, String title, String description, String deadline, boolean repeatable, int maxCompletions, long taxPaid,
                                            Map<Integer, Integer> requirements,
                                            List<ItemReward> itemRewards,
                                            List<CurrencyReward> currencyRewards) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            ps = con.prepareStatement(
                    "INSERT INTO quest_board (title, description, created_by, is_gm_quest, deadline, repeatable, max_completions, status, tax_paid) " +
                            "VALUES (?, ?, ?, FALSE, ?, ?, ?, 'OPEN', ?)", Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, title);
            ps.setString(2, description);
            ps.setInt(3, creatorId);
            if (deadline == null || deadline.isEmpty()) {
                ps.setNull(4, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(4, Timestamp.valueOf(deadline + " 23:59:59"));
            }
            ps.setBoolean(5, repeatable);
            ps.setInt(6, maxCompletions);
            ps.setLong(7, taxPaid);

            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Quest insert failed.");

            rs = ps.getGeneratedKeys();
            if (!rs.next()) throw new SQLException("No quest_id returned.");
            int questId = rs.getInt(1);

            // Insert Requirements
            if (requirements != null) {
                ps = con.prepareStatement("INSERT INTO quest_board_requirements (quest_id, item_id, quantity) VALUES (?, ?, ?)");
                for (Map.Entry<Integer, Integer> entry : requirements.entrySet()) {
                    ps.setInt(1, questId);
                    ps.setInt(2, entry.getKey());
                    ps.setInt(3, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Insert Item Rewards
            if (itemRewards != null) {
                ps = con.prepareStatement("INSERT INTO quest_board_item_rewards (quest_id, item_id, quantity) VALUES (?, ?, ?)");
                for (ItemReward r : itemRewards) {
                    ps.setInt(1, questId);
                    ps.setInt(2, r.itemId);
                    ps.setLong(3, r.quantity);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Insert Currency Rewards
            if (currencyRewards != null) {
                ps = con.prepareStatement("INSERT INTO quest_board_currency_rewards (quest_id, reward_type, amount, nx_type) VALUES (?, ?, ?, ?)");
                for (CurrencyReward r : currencyRewards) {
                    ps.setInt(1, questId);
                    ps.setString(2, r.type.name());
                    ps.setLong(3, r.amount);
                    ps.setInt(4, r.nxType);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (con != null) con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (con != null) con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Existing methods unchanged...

    public static class ItemReward {
        public final int itemId;
        public final int quantity;
        public ItemReward(int itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    public static class CurrencyReward {
        public final RewardType type;
        public final long amount;
        public final int nxType;
        public CurrencyReward(RewardType type, long amount, int nxType) {
            this.type = type;
            this.amount = amount;
            this.nxType = nxType;
        }
    }

    public enum RewardType {
        MESO, NX
    }
}
