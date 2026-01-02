package server.bossshop;

import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Manages purchases of materials in the Boss Shop.
 *
 * Dependency-safe notes:
 * - Keeps class name, package, and public method signatures the same.
 * - Returns "success"/"failed" exactly as before.
 * - checkEligibility() returns int[4] in the same material order as before.
 */
public class BossShopManager {

    // If you want a default limit for unknown materials, set it here.
    // Keeping it 0 means "unknown material cannot be bought".
    private static final int DEFAULT_LIMIT_UNKNOWN_MATERIAL = 0;

    // Material IDs in the same order used by checkEligibility() return array
    private static final int[] MAT_IDS = {
            4001017, // Zakum Eye of Fire
            4021032, // Horntail Mana Crystal
            4001189, // Pink Poppin
            4001694  // Von Leon Ticket
    };

    /**
     * Tracks a purchase for a player.
     * @param charId The character ID of the player.
     * @param matId The material ID being purchased.
     * @param qty The quantity being purchased.
     * @return "success" if the purchase was valid and successful, or "failed" otherwise.
     */
    public static String buyMat(int charId, int matId, int qty) {
        if (qty <= 0) {
            System.err.println("[BossShopManager] Invalid purchase qty (<=0). charId=" + charId + " matId=" + matId + " qty=" + qty);
            return "failed";
        }

        LocalDate today = LocalDate.now();
        int limit = getMaterialPurchaseLimit(matId);

        if (limit <= 0) {
            // Key alert: config issue or unknown material
            System.err.println("[BossShopManager] Purchase blocked: material has no valid daily limit. charId=" + charId + " matId=" + matId + " limit=" + limit);
            return "failed";
        }

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            int currentQty = 0;

            // Lock the row for this (date, charId, matId) to prevent concurrent overshoot
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT qty FROM boss_shop_purchases WHERE date = ? AND charId = ? AND matId = ? FOR UPDATE")) {
                ps.setDate(1, Date.valueOf(today));
                ps.setInt(2, charId);
                ps.setInt(3, matId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentQty = rs.getInt("qty");
                    }
                }
            }

            if (currentQty + qty > limit) {
                con.rollback();
                return "failed";
            }

            // Insert or increment
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO boss_shop_purchases (date, charId, matId, qty) " +
                            "VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE qty = qty + ?")) {
                ps.setDate(1, Date.valueOf(today));
                ps.setInt(2, charId);
                ps.setInt(3, matId);
                ps.setInt(4, qty);
                ps.setInt(5, qty);
                ps.executeUpdate();
            }

            con.commit();
            return "success";

        } catch (SQLException e) {
            System.err.println("[BossShopManager] Failed to track purchase. charId=" + charId + " matId=" + matId + " qty=" + qty + " err=" + e.getMessage());

            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException re) {
                    System.err.println("[BossShopManager] Rollback failed. err=" + re.getMessage());
                }
            }
            return "failed";

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException ignore) {
                    // ignore
                }
                try {
                    con.close();
                } catch (SQLException ignore) {
                    // ignore
                }
            }
        }
    }

    /**
     * Gets the current purchase count for a given player and material for today.
     * @param charId The character ID of the player.
     * @param matId The material ID.
     * @param date The date of the purchase.
     * @return The current quantity of the material purchased today.
     */
    private static int getCurrentPurchaseCount(int charId, int matId, LocalDate date) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT qty FROM boss_shop_purchases WHERE date = ? AND charId = ? AND matId = ?")) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, charId);
            ps.setInt(3, matId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("qty");
                }
            }

        } catch (SQLException e) {
            // Key alert only
            System.err.println("[BossShopManager] Failed to get purchase count. charId=" + charId + " matId=" + matId + " err=" + e.getMessage());
        }
        return 0;
    }

    /**
     * Returns the purchase limit for a given material.
     * @param materialId The material's ID (e.g., 4001017 for Zakum Eye of Fire).
     * @return The purchase limit for the material.
     */
    public static int getMaterialPurchaseLimit(int materialId) {
        // Keep your current behavior (all 100), but fix the misleading comment.
        // If you later want per-item limits, only change these values.
        if (materialId == 4001017) { // Zakum Eye of Fire
            return 100;
        } else if (materialId == 4021032) { // Horntail Mana Crystal
            return 100;
        } else if (materialId == 4001189) { // Pink Poppin
            return 100;
        } else if (materialId == 4001694) { // Von Leon Ticket
            return 100;
        }

        return DEFAULT_LIMIT_UNKNOWN_MATERIAL;
    }

    /**
     * Checks eligibility for purchasing materials based on the daily purchase limit.
     * Returns an array of remaining purchase quantities for each material in fixed order:
     * [Zakum Eye of Fire, Horntail Mana Crystal, Pink Poppin, Von Leon Ticket]
     * @param charId The character ID of the player.
     * @return An array of material purchase limits remaining today.
     */
    public static int[] checkEligibility(int charId) {
        int[] eligibilityArray = new int[4];
        LocalDate today = LocalDate.now();

        for (int i = 0; i < MAT_IDS.length; i++) {
            int matId = MAT_IDS[i];
            int limit = getMaterialPurchaseLimit(matId);

            if (limit <= 0) {
                // Unknown/disabled material -> 0 remaining
                eligibilityArray[i] = 0;
                continue;
            }

            int currentQty = getCurrentPurchaseCount(charId, matId, today);
            eligibilityArray[i] = Math.max(0, limit - currentQty);
        }

        return eligibilityArray;
    }
}
