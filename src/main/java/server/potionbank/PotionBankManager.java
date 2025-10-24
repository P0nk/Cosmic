package server.potionbank;

import client.Character;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PotionBankManager
 * Handles persistent storage for the player's potion bank.
 * - Stores accumulated HP and MP healing capacity.
 * - Supports deposit (addBanked) and withdrawals.
 * - Each character has a single row in `cosmic_potion_bank`.
 */
public class PotionBankManager {

    private static final long MAX_CAP = 2_000_000_000L;

    /** Load stored HP value */
    public static long getBankedHP(int charId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT banked_hp FROM cosmic_potion_bank WHERE characterid = ?")) {
            ps.setInt(1, charId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("banked_hp");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /** Load stored MP value */
    public static long getBankedMP(int charId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT banked_mp FROM cosmic_potion_bank WHERE characterid = ?")) {
            ps.setInt(1, charId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("banked_mp");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /**
     * Deposit new totals into the bank.
     * If record doesn’t exist, it’s created automatically.
     */
    public static void addBanked(Character chr, long addHP, long addMP) {
        int charId = chr.getId();
        System.out.println("[PotionBank] addBanked() for " + chr.getName() +
                " addHP=" + addHP + ", addMP=" + addMP);

        long currentHP = getBankedHP(charId);
        long currentMP = getBankedMP(charId);

        long newHP = Math.min(MAX_CAP, currentHP + addHP);
        long newMP = Math.min(MAX_CAP, currentMP + addMP);

        System.out.println("[PotionBank] currentHP=" + currentHP +
                " currentMP=" + currentMP +
                " newHP=" + newHP + " newMP=" + newMP);

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
            System.out.println("[PotionBank] Updated DB for " + chr.getName());
        } catch (SQLException e) {
            System.err.println("[PotionBank] SQL failed for " + chr.getName());
            e.printStackTrace();
        }
    }

    /** Withdraw HP; returns true if successful. */
    public static boolean withdrawHP(Character chr, long amount) {
        int charId = chr.getId();
        long current = getBankedHP(charId);
        if (amount > current) return false;
        updateField(charId, "banked_hp", current - amount);
        return true;
    }

    /** Withdraw MP; returns true if successful. */
    public static boolean withdrawMP(Character chr, long amount) {
        int charId = chr.getId();
        long current = getBankedMP(charId);
        if (amount > current) return false;
        updateField(charId, "banked_mp", current - amount);
        return true;
    }

    /** Internal helper for updating single field. */
    private static void updateField(int charId, String field, long newVal) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE cosmic_potion_bank SET " + field + " = ?, updated_at = CURRENT_TIMESTAMP WHERE characterid = ?")) {
            ps.setLong(1, newVal);
            ps.setInt(2, charId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
