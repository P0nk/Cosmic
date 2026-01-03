package server.quest;

import client.Character;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class KarmaRewardSystem {

    // TODO: Remember to change this back to 100 after testing is complete!
    private static final int MILESTONE_THRESHOLD = 100;

    public static int calculateKarmaPoints(Character chr) {
        String query = "SELECT COUNT(*) FROM cosmic.queststatus " +
                "WHERE characterid = ? AND completed = 1 AND karma_redeemed = 0";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, chr.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[KarmaRewardSystem] Error calculating points for charID " + chr.getId());
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean redeemKarmaPoints(Character chr) {
        int karmaPoints = calculateKarmaPoints(chr);
        if (karmaPoints <= 0) return false;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            int rowsUpdated = markQuestsAsRedeemed(con, chr);

            if (rowsUpdated <= 0) {
                System.err.println("[KarmaRewardSystem] Critical: Points calculated but DB update failed for charID " + chr.getId());
                con.rollback();
                return false;
            }

            // Reward AP
            rewardAP(chr, karmaPoints);

            con.commit();
            return true;

        } catch (Exception e) {
            System.err.println("[KarmaRewardSystem] Transaction failed for charID " + chr.getId());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean claimNextMilestone(Character chr) {
        String checkQuery = "SELECT COUNT(*) FROM cosmic.queststatus WHERE characterid = ? AND karma_redeemed = 1";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(checkQuery)) {

            ps.setInt(1, chr.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt(1) < MILESTONE_THRESHOLD) {
                    return false; // Not enough eligible quests
                }
            }

            // Update rows from state 1 -> 2
            String updateQuery = "UPDATE cosmic.queststatus SET karma_redeemed = 2 " +
                    "WHERE characterid = ? AND karma_redeemed = 1 LIMIT ?";

            try (PreparedStatement updatePs = con.prepareStatement(updateQuery)) {
                updatePs.setInt(1, chr.getId());
                updatePs.setInt(2, MILESTONE_THRESHOLD);
                updatePs.executeUpdate();
            }

            // Reward Bonus AP
            rewardBonusAP(chr, 100);
            return true;

        } catch (SQLException e) {
            System.err.println("[KarmaRewardSystem] Error claiming milestone for charID " + chr.getId());
            e.printStackTrace();
            return false;
        }
    }

    public static int getRedeemedMilestoneCount(Character chr) {
        String query = "SELECT COUNT(*) FROM cosmic.queststatus WHERE characterid = ? AND karma_redeemed = 2";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, chr.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) / MILESTONE_THRESHOLD;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int markQuestsAsRedeemed(Connection con, Character chr) throws SQLException {
        String query = "UPDATE cosmic.queststatus " +
                "SET karma_redeemed = 1 " +
                "WHERE characterid = ? AND completed = 1 AND karma_redeemed = 0";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, chr.getId());
            return ps.executeUpdate();
        }
    }

    private static void rewardAP(Character chr, int karmaPointsToSpend) {
        int apToReward = karmaPointsToSpend * 2;

        // 'false' = Force update packet to client so UI refreshes immediately
        chr.gainAp(apToReward, false);

        chr.yellowMessage("[Karma] Thank you, hero! For your good deeds, you have been awarded " + apToReward + " AP!");
    }

    private static void rewardBonusAP(Character chr, int amount) {
        // 'false' = Force update packet to client so UI refreshes immediately
        chr.gainAp(amount, false);

        chr.yellowMessage("[Milestone] Bonus! You received an additional " + amount + " AP!");
    }

    public static int getCompletedQuestCount(Character chr) {
        String query = "SELECT COUNT(*) FROM cosmic.queststatus WHERE characterid = ? AND completed = 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, chr.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}