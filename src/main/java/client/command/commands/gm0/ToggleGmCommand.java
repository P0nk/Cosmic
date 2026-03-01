/*
   @Author: Arthur L - Refactored command content into modules
   (Modified for undercover GM toggle)
*/
package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import tools.DatabaseConnection; // Update this import if your DB connection class is named differently

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ToggleGmCommand extends Command {
    {
        setDescription(""); // Left empty since it's a hidden command!
    }

    @Override
    public void execute(Client c, String[] params) {
        int charId = c.getPlayer().getId();
        int currentGmLevel = c.getPlayer().gmLevel();
        int targetLevel = 0; // Default to 0 if no arguments are provided
        boolean hasArgs = params.length > 0;

        if (hasArgs) {
            try {
                targetLevel = Integer.parseInt(params[0]);
                if (targetLevel < 0 || targetLevel > 1) {
                    c.getPlayer().dropMessage(5, "You can only toggle to GM level 0 (Player) or 1 (Donator).");
                    return;
                }
            } catch (NumberFormatException e) {
                c.getPlayer().dropMessage(5, "Invalid syntax. Use @togglegm or @togglegm [0/1]");
                return;
            }
        }

        try {
            Connection con = DatabaseConnection.getConnection();

            // If we are currently a GM (Rank 2+), we want to go undercover
            if (currentGmLevel > 1) {
                // Save original GM level to the database
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO gm_toggles (character_id, original_gm_level) VALUES (?, ?) ON DUPLICATE KEY UPDATE original_gm_level = ?"
                );
                ps.setInt(1, charId);
                ps.setInt(2, currentGmLevel);
                ps.setInt(3, currentGmLevel);
                ps.executeUpdate();
                ps.close();

                // Update the main characters table
                PreparedStatement psChar = con.prepareStatement("UPDATE characters SET gm = ? WHERE id = ?");
                psChar.setInt(1, targetLevel);
                psChar.setInt(2, charId);
                psChar.executeUpdate();
                psChar.close();

                c.getPlayer().setGMLevel((byte) targetLevel);
                c.getPlayer().dropMessage(5, "GM powers hidden. You are now playing as GM level " + targetLevel + ".");

            } else {
                // We are currently GM 0 or 1. Check the database to see if we are an undercover Admin!
                PreparedStatement ps = con.prepareStatement("SELECT original_gm_level FROM gm_toggles WHERE character_id = ?");
                ps.setInt(1, charId);
                ResultSet rs = ps.executeQuery();

                int savedGmLevel = 0;
                if (rs.next()) {
                    savedGmLevel = rs.getInt("original_gm_level");
                }
                rs.close();
                ps.close();

                if (savedGmLevel > 1) {
                    if (hasArgs) {
                        // They are undercover, but typed @togglegm 0 or 1 to switch between Donator and Player states
                        PreparedStatement psChar = con.prepareStatement("UPDATE characters SET gm = ? WHERE id = ?");
                        psChar.setInt(1, targetLevel);
                        psChar.setInt(2, charId);
                        psChar.executeUpdate();
                        psChar.close();

                        c.getPlayer().setGMLevel((byte) targetLevel);
                        c.getPlayer().dropMessage(5, "Undercover state shifted to GM level " + targetLevel + ".");
                    } else {
                        // No args provided: Time to restore actual GM powers!
                        PreparedStatement ps2 = con.prepareStatement("UPDATE gm_toggles SET original_gm_level = 0 WHERE character_id = ?");
                        ps2.setInt(1, charId);
                        ps2.executeUpdate();
                        ps2.close();

                        PreparedStatement psChar = con.prepareStatement("UPDATE characters SET gm = ? WHERE id = ?");
                        psChar.setInt(1, savedGmLevel);
                        psChar.setInt(2, charId);
                        psChar.executeUpdate();
                        psChar.close();

                        c.getPlayer().setGMLevel((byte) savedGmLevel);
                        c.getPlayer().dropMessage(5, "Welcome back. GM powers restored to level " + savedGmLevel + ".");
                    }
                } else {
                    // A regular player tried to guess the command. Ignore them completely.
                    c.getPlayer().dropMessage(5, "Command does not exist.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error toggling GM state: " + e);
            c.getPlayer().dropMessage(5, "An error occurred.");
        }
    }
}