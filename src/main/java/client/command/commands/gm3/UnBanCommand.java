package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import constants.id.MapId;
import server.maps.MapleMap;
import server.maps.Portal;

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
            try (PreparedStatement ps = con
                    .prepareStatement("UPDATE accounts SET banned = 0, banreason = NULL WHERE id = ?")) {
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
                // Ignore if not present
            }

            // 5. Unjail Character (DB)
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE characters SET map = ?, jailexpire = 0 WHERE accountid = ? AND map = ?")) {
                ps.setInt(1, 100000000); // Return to Henesys safely
                ps.setInt(2, accId);
                ps.setInt(3, MapId.JAIL);
                ps.executeUpdate();
            }

            // 6. Unjail if Online
            Character victim = c.getWorldServer().getPlayerStorage().getCharacterByName(targetName);
            if (victim != null) {
                victim.removeJailExpirationTime();
                if (victim.getMapId() == MapId.JAIL) {
                    MapleMap targetMap = c.getChannelServer().getMapFactory().getMap(100000000);
                    Portal targetPortal = targetMap.getPortal(0);
                    victim.changeMap(targetMap, targetPortal);
                }
            }

            gm.message("Successfully unbanned/unjailed " + targetName
                    + ". All locks (Account, IP, MAC, HWID, Jail) have been cleared.");

        } catch (Exception e) {
            e.printStackTrace();
            gm.message("Error occurred while unbanning " + targetName);
        }
    }
}