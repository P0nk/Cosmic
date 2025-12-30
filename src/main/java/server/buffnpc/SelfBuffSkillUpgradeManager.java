package server.buffnpc;

import client.Character;
import tools.DatabaseConnection;
import java.sql.*;

public class SelfBuffSkillUpgradeManager {

    // Unlock a skill for the player and save it to the database
    public static boolean unlockSkill(Character player, int skillId) {
        // First, check if the player already has the skill
        if (hasSkill(player, skillId)) {
            System.out.println("Skill already unlocked: " + skillId);
            return false; // Return false if the player already has the skill
        }

        // SQL query to insert the unlocked skill into the database
        String query = "INSERT INTO unlocked_buffs (playerid, skillid) VALUES (?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            // Bind the player's ID and the skill they unlocked
            ps.setInt(1, player.getId());  // player.getId() will give the player’s unique ID
            ps.setInt(2, skillId);

            // Execute the query
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0; // Return true if the skill was successfully saved
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Return false if there was an error
        }
    }

    // Check if the player already has a particular skill
    public static boolean hasSkill(Character player, int skillId) {
        // SQL query to check if the skill already exists for the player
        String query = "SELECT COUNT(*) FROM unlocked_buffs WHERE playerid = ? AND skillid = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, player.getId());
            ps.setInt(2, skillId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;  // If the count is greater than 0, the skill is unlocked
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
