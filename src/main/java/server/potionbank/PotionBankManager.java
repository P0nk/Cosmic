package server.potionbank;

import client.Character;
import tools.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * PotionBankManager
 * Handles persistent storage for the player's potion bank.
 * - Stores accumulated HP and MP healing capacity.
 * - Supports deposit (addBanked) and withdrawals.
 * - Each character has a single row in `cosmic_potion_bank`.
 */
public class PotionBankManager {

    private static final long MAX_CAP = 2_000_000_000L;

    // --- Key Alerts (rate-limited) ---
    private static volatile long lastDbAlertAt = 0L;
    private static final long ALERT_COOLDOWN_MS = 60_000L;

    private static void alertDb(String msg, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastDbAlertAt >= ALERT_COOLDOWN_MS) {
            lastDbAlertAt = now;
            System.err.println("[PotionBankManager][DB] " + msg + (t != null ? " err=" + t.getMessage() : ""));
        }
    }

    private static void alertLogic(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastDbAlertAt >= ALERT_COOLDOWN_MS) {
            lastDbAlertAt = now;
            System.err.println("[PotionBankManager][ALERT] " + msg);
        }
    }

    /** Load stored HP value */
    public static long getBankedHP(int charId) {
        String sql = "SELECT banked_hp FROM cosmic_potion_bank WHERE characterid = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, charId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("banked_hp");
            }
        } catch (SQLException e) {
            alertDb("Failed to load banked HP. charId=" + charId, e);
        }
        return 0;
    }

    /** Load stored MP value */
    public static long getBankedMP(int charId) {
        String sql = "SELECT banked_mp FROM cosmic_potion_bank WHERE characterid = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, charId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("banked_mp");
            }
        } catch (SQLException e) {
            alertDb("Failed to load banked MP. charId=" + charId, e);
        }
        return 0;
    }

    /**
     * Deposit new totals into the bank.
     * If record doesn’t exist, it’s created automatically.
     */
    public static void addBanked(Character chr, long addHP, long addMP) {
        int charId = chr.getId();

        if (addHP < 0 || addMP < 0) {
            alertLogic("Attempt to deposit negative values. charId=" + charId + " addHP=" + addHP + " addMP=" + addMP);
            return;
        }

        long currentHP = getBankedHP(charId);
        long currentMP = getBankedMP(charId);

        long newHP = Math.min(MAX_CAP, currentHP + addHP);
        long newMP = Math.min(MAX_CAP, currentMP + addMP);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO cosmic_potion_bank (characterid, banked_hp, banked_mp) " +
                             "VALUES (?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE " +
                             "banked_hp = VALUES(banked_hp), " +
                             "banked_mp = VALUES(banked_mp), " +
                             "updated_at = CURRENT_TIMESTAMP")) {

            ps.setInt(1, charId);
            ps.setLong(2, newHP);
            ps.setLong(3, newMP);
            ps.executeUpdate();

        } catch (SQLException e) {
            alertDb("Failed to update banked totals. charId=" + charId, e);
        }
    }

    /** Withdraw HP; returns true if successful. */
    public static boolean withdrawHP(Character chr, long amount) {
        int charId = chr.getId();

        if (amount <= 0) {
            alertLogic("Attempt to withdraw non-positive amount. charId=" + charId + " amount=" + amount);
            return false;
        }

        long current = getBankedHP(charId);
        if (amount > current) {
            alertLogic("Insufficient HP balance. charId=" + charId + " current=" + current + " amount=" + amount);
            return false;
        }

        updateField(charId, "banked_hp", current - amount);
        return true;
    }

    /** Withdraw MP; returns true if successful. */
    public static boolean withdrawMP(Character chr, long amount) {
        int charId = chr.getId();

        if (amount <= 0) {
            alertLogic("Attempt to withdraw non-positive amount. charId=" + charId + " amount=" + amount);
            return false;
        }

        long current = getBankedMP(charId);
        if (amount > current) {
            alertLogic("Insufficient MP balance. charId=" + charId + " current=" + current + " amount=" + amount);
            return false;
        }

        updateField(charId, "banked_mp", current - amount);
        return true;
    }

    /** Internal helper for updating single field. */
    private static void updateField(int charId, String field, long newVal) {
        if (newVal < 0) {
            alertLogic("Attempt to update field with negative value. charId=" + charId + " field=" + field + " newVal=" + newVal);
            return;
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE cosmic_potion_bank SET " + field + " = ?, updated_at = CURRENT_TIMESTAMP WHERE characterid = ?")) {
            ps.setLong(1, newVal);
            ps.setInt(2, charId);
            ps.executeUpdate();

        } catch (SQLException e) {
            alertDb("Failed to update field. charId=" + charId + " field=" + field + " newVal=" + newVal, e);
        }
    }

}
