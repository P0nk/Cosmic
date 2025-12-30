package server.quest;

import client.Character;
import client.Client;
import client.command.commands.gm2.ApCommand;
import tools.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class KarmaRewardSystem {

    // Method to calculate Karma Points (count completed quests with karma_redeemed = 0)
    public static int calculateKarmaPoints(Character chr) {
        String query = "SELECT COUNT(*) FROM cosmic.queststatus WHERE characterid = ? AND completed = 1 AND karma_redeemed = 0";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, chr.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);  // Return the total number of Karma Points
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Method to redeem Karma Points (spend Karma Points and give character attribute points)
    public static boolean redeemKarmaPoints(Character chr, int pointsToSpend) {
        int karmaPoints = calculateKarmaPoints(chr);

        // Ensure the player has enough Karma Points
        if (karmaPoints >= pointsToSpend) {
            // Reward the player with Attribute Points (AP) for Karma Points
            rewardAP(chr, pointsToSpend);

            // Deduct Karma Points
            deductKarmaPoints(chr, pointsToSpend);

            // Update karma_redeemed for the completed quests
            markQuestsAsRedeemed(chr, pointsToSpend);
            return true;
        }

        return false;  // Not enough Karma Points to redeem
    }

    // Method to reward AP (Attribute Points) for Karma Points redeemed
    private static void rewardAP(Character chr, int pointsToSpend) {
        // Multiply the Karma Points by 2 to reward 2 AP for each Karma Point
        int apToReward = pointsToSpend * 2;  // 2 AP for each Karma Point redeemed

        // Call the APCommand's functionality to reward the player with AP
        String[] params = { String.valueOf(apToReward) };  // Format as if the player used the AP command with the new AP value
        ApCommand apCommand = new ApCommand();
        apCommand.execute(chr.getClient(), params);

        // Send the message in the in-game chat to thank the player for their good deeds
        String thankYouMessage = "Thank you, hero! For your good deeds, you have been awarded " + apToReward + " AP!";
        chr.yellowMessage(thankYouMessage);  // Sends a yellow message in the chat
    }


    // Method to deduct Karma Points from the character after spending them
    private static void deductKarmaPoints(Character chr, int pointsToDeduct) {
        String query = "UPDATE cosmic.queststatus SET karma_redeemed = 1 WHERE characterid = ? AND karma_redeemed = 0 LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, chr.getId());
            ps.setInt(2, pointsToDeduct);  // Only mark a limited number of quests as redeemed
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to mark the redeemed quests in the quest status table
    private static void markQuestsAsRedeemed(Character chr, int pointsToRedeem) {
        String query = "UPDATE cosmic.queststatus SET karma_redeemed = 1 WHERE characterid = ? AND karma_redeemed = 0 LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, chr.getId());
            ps.setInt(2, pointsToRedeem);  // Ensure we only mark as redeemed for the number of Karma Points spent
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
