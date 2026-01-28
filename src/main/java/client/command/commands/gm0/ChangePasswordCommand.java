package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import tools.BCrypt;
import tools.DatabaseConnection;
import tools.HexTool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChangePasswordCommand extends Command {

    {
        setDescription("Change your account password. Syntax: @password <old_password> <new_password>");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        // 1. Syntax Check
        if (params.length < 2) {
            player.dropMessage(5, "Syntax: @password <old_password> <new_password>");
            return;
        }

        String oldPass = params[0];
        String newPass = params[1];

        // 2. Length/Security Check
        if (newPass.length() < 6 || newPass.length() > 20) {
            player.dropMessage(5, "New password must be between 6 and 20 characters.");
            return;
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            // 3. Fetch current password hash from DB
            String dbHash = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT password FROM accounts WHERE id = ?")) {
                ps.setInt(1, c.getAccID());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dbHash = rs.getString("password");
                    }
                }
            }

            if (dbHash == null) {
                player.dropMessage(5, "Error: Account not found.");
                return;
            }

            // 4. Verify Old Password
            boolean verified = false;

            // Check if it's already a BCrypt hash (starts with $2)
            if (dbHash.startsWith("$2") && BCrypt.checkpw(oldPass, dbHash)) {
                verified = true;
            }
            // Check legacy hashes (Plaintext, SHA-1, SHA-512)
            else if (dbHash.equals(oldPass) || checkHash(dbHash, "SHA-1", oldPass)
                    || checkHash(dbHash, "SHA-512", oldPass)) {
                verified = true;
            }

            if (!verified) {
                player.dropMessage(1, "Authentication Failed: The old password you entered is incorrect.");
                return;
            }

            // 5. Update with New BCrypt Hash
            String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt(12));

            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET password = ? WHERE id = ?")) {
                ps.setString(1, newHash);
                ps.setInt(2, c.getAccID());
                ps.executeUpdate();
            }

            player.dropMessage(1, "Success! Your password has been changed.");
            player.dropMessage(5, "Please remember your new password.");

        } catch (SQLException e) {
            player.dropMessage(5, "An error occurred while changing your password.");
            e.printStackTrace();
        }
    }

    // Helper method for legacy hash checking
    private static boolean checkHash(String hash, String type, String password) {
        try {
            MessageDigest digester = MessageDigest.getInstance(type);
            digester.update(password.getBytes(StandardCharsets.UTF_8), 0, password.length());
            return HexTool.toHexString(digester.digest()).replace(" ", "").equalsIgnoreCase(hash);
        } catch (Exception e) {
            return false;
        }
    }
}