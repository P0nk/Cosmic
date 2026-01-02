package server.buffnpc;

import client.Character;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SelfBuffSkillUpgradeManager {

    // --- Key Alerts (rate-limited) ---
    private static volatile long lastDbAlertAt = 0L;
    private static volatile long lastConfigAlertAt = 0L;
    private static final long ALERT_COOLDOWN_MS = 60_000L;

    private static void alertDb(String msg, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastDbAlertAt >= ALERT_COOLDOWN_MS) {
            lastDbAlertAt = now;
            System.err.println("[SelfBuffSkillUpgradeManager][DB] " + msg + (t != null ? " err=" + t.getMessage() : ""));
        }
    }

    private static void alertConfig(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastConfigAlertAt >= ALERT_COOLDOWN_MS) {
            lastConfigAlertAt = now;
            System.err.println("[SelfBuffSkillUpgradeManager][CONFIG] " + msg);
        }
    }

    /**
     * Unlock a skill for the player and save it to the database.
     * Dependency-safe: signature/return behavior unchanged.
     */
    public static boolean unlockSkill(Character player, int skillId) {
        if (player == null) {
            alertConfig("unlockSkill called with null player. skillId=" + skillId);
            return false;
        }
        if (skillId <= 0) {
            alertConfig("unlockSkill called with invalid skillId=" + skillId + " playerId=" + player.getId());
            return false;
        }

        // Quick pre-check (kept for current behavior). Still handle race safely below.
        if (hasSkill(player, skillId)) {
            return false;
        }

        // If you have UNIQUE(playerid, skillid), this becomes concurrency-safe.
        // Using plain INSERT to avoid behavior changes; duplicate-key handled gracefully.
        final String query = "INSERT INTO unlocked_buffs (playerid, skillid) VALUES (?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, player.getId());
            ps.setInt(2, skillId);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            // Key alert only:
            // - If a UNIQUE constraint exists, duplicate inserts will land here.
            //   In that case, treat as "already unlocked" and return false silently.
            if (isDuplicateKey(e)) {
                return false;
            }

            alertDb("Failed to unlock skill. playerId=" + player.getId() + " skillId=" + skillId, e);
            return false;
        }
    }

    /**
     * Check if the player already has a particular skill.
     * Dependency-safe: signature/return behavior unchanged.
     */
    public static boolean hasSkill(Character player, int skillId) {
        if (player == null) {
            alertConfig("hasSkill called with null player. skillId=" + skillId);
            return false;
        }
        if (skillId <= 0) {
            alertConfig("hasSkill called with invalid skillId=" + skillId + " playerId=" + player.getId());
            return false;
        }

        final String query = "SELECT 1 FROM unlocked_buffs WHERE playerid = ? AND skillid = ? LIMIT 1";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, player.getId());
            ps.setInt(2, skillId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            alertDb("Failed to check skill unlock. playerId=" + player.getId() + " skillId=" + skillId, e);
            return false;
        }
    }

    /**
     * Best-effort duplicate key detection across common JDBC drivers.
     */
    private static boolean isDuplicateKey(SQLException e) {
        // MySQL: SQLState 23000, error code 1062
        if ("23000".equals(e.getSQLState()) && e.getErrorCode() == 1062) {
            return true;
        }
        // Some drivers still use SQLState 23000 for unique violations
        if ("23000".equals(e.getSQLState())) {
            String msg = e.getMessage();
            return msg != null && msg.toLowerCase().contains("duplicate");
        }
        return false;
    }
}
