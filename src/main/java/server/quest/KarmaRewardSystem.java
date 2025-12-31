package server.quest;

import client.Character;
import client.command.commands.gm2.ApCommand;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class KarmaRewardSystem {

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
            e.printStackTrace();
        }
        return 0;
    }

    // Redeem ALL (NPC always spends all)
    public static boolean redeemKarmaPoints(Character chr) {
        int karmaPoints = calculateKarmaPoints(chr);
        if (karmaPoints <= 0) return false;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            int rowsUpdated = markQuestsAsRedeemed(con, chr);

            // If DB update failed / updated nothing, do NOT reward AP
            if (rowsUpdated <= 0) {
                con.rollback();
                return false;
            }

            // Optional stricter rule: ensure we redeemed exactly what we counted
            // If you want strict matching, uncomment:
            // if (rowsUpdated != karmaPoints) {
            //     con.rollback();
            //     return false;
            // }

            rewardAP(chr, karmaPoints);

            con.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            // best-effort rollback (connection might already be closed if exception thrown earlier)
            return false;
        }
    }

    private static int markQuestsAsRedeemed(Connection con, Character chr) throws SQLException {
        String query = "UPDATE cosmic.queststatus " +
                "SET karma_redeemed = 1 " +
                "WHERE characterid = ? AND completed = 1 AND karma_redeemed = 0";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, chr.getId());
            return ps.executeUpdate(); // returns affected row count
        }
    }

    private static void rewardAP(Character chr, int karmaPointsToSpend) {
        int apToReward = karmaPointsToSpend * 2;

        String[] params = { String.valueOf(apToReward) };
        ApCommand apCommand = new ApCommand();
        apCommand.execute(chr.getClient(), params);

        chr.yellowMessage("Thank you, hero! For your good deeds, you have been awarded " + apToReward + " AP!");
    }
}
