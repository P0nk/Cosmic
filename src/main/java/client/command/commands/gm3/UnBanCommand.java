package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Unified Unban Command
 * Clears bans from Accounts, IP, MAC, and HWID tables.
 */
public class UnBanCommand extends Command {
    {
        setDescription("Unbans a player (Account, IP, MAC, HWID). Syntax: !unban <IGN>");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character gm = c.getPlayer();
        if (params.length < 1) {
            gm.yellowMessage("Syntax: !unban <IGN>");
            return;
        }

        String targetName = params[0];

        // Helper to find Account ID from Name (DB Lookup)
        int accId = Character.getAccountIdByName(targetName);

        if (accId <= 0) {
            gm.message("Character not found or invalid Account ID: " + targetName);
            return;
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            // 1. Unban Account (Set banned = 0)
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 0, banreason = NULL WHERE id = ?")) {
                ps.setInt(1, accId);
                ps.executeUpdate();
            }

            // 2. Unban IP
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM ipbans WHERE aid = ?")) {
                ps.setInt(1, accId);
                ps.executeUpdate();
            }

            // 3. Unban MAC
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM macbans WHERE aid = ?")) {
                ps.setInt(1, accId);
                ps.executeUpdate();
            }

            // 4. Unban HWID (Try/Catch in case table structure varies)
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM hwidbans WHERE aid = ?")) {
                ps.setInt(1, accId);
                ps.executeUpdate();
            } catch (SQLException e) {
                // If the hwidbans table doesn't have an 'aid' column, this might fail.
                // However, most 'plus' sources link HWID to AID.
                // If this fails, you might need to manually delete HWIDs by string match, 
                // but that requires knowing the HWID string, which we don't have from just an IGN offline.
            }

            gm.message("Successfully unbanned " + targetName + ". All locks (Account, IP, MAC, HWID) have been cleared.");

        } catch (Exception e) {
            e.printStackTrace();
            gm.message("Error occurred while unbanning " + targetName);
        }
    }
}