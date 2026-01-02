package server.bossshop;

import tools.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Manages purchases of materials in the Boss Shop.
 */
public class BossShopManager {

    private static final int MAX_PURCHASE_PER_DAY = 100;  // Max purchase per material per day (can be configurable)

    /**
     * Tracks a purchase for a player.
     * @param charId The character ID of the player.
     * @param matId The material ID being purchased.
     * @param qty The quantity being purchased.
     * @return "success" if the purchase was valid and successful, or "failed" if the player exceeded the limit.
     */
    public static String buyMat(int charId, int matId, int qty) {
        LocalDate today = LocalDate.now();

        // Debugging: print purchase attempt details
        System.out.println("[BossShopManager] Attempting purchase for charId " + charId +
                " Material ID: " + matId + " Quantity: " + qty);

        // Check if the player has reached the daily purchase limit for this material
        int currentQty = getCurrentPurchaseCount(charId, matId, today);

        // Debugging: print current purchase count
        System.out.println("[BossShopManager] Current purchase count for charId " + charId + " and Material ID " + matId + ": " + currentQty);

        // Check if the total purchases exceed the limit
        if (currentQty + qty > MAX_PURCHASE_PER_DAY) {
            System.out.println("[BossShopManager] Purchase failed: Exceeded daily limit for Material ID " + matId);
            return "failed";  // Exceeded daily limit
        }

        // Insert or update the purchase count in the database
        try (Connection con = DatabaseConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO boss_shop_purchases (date, charId, matId, qty) " +
                            "VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE qty = qty + ?");
            ps.setDate(1, java.sql.Date.valueOf(today));
            ps.setInt(2, charId);
            ps.setInt(3, matId);
            ps.setInt(4, qty);
            ps.setInt(5, qty);  // Increment by the specified quantity
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[BossShopManager] Failed to track purchase: " + e.getMessage());
            return "failed";  // Database error
        }

        // Debugging: print successful purchase message
        System.out.println("[BossShopManager] Purchase successful for charId " + charId + " Material ID: " + matId + " Quantity: " + qty);
        return "success";  // Purchase was successful
    }

    /**
     * Gets the current purchase count for a given player and material for today.
     * @param charId The character ID of the player.
     * @param matId The material ID.
     * @param date The date of the purchase.
     * @return The current quantity of the material purchased today.
     */
    private static int getCurrentPurchaseCount(int charId, int matId, LocalDate date) {
        int count = 0;
        try (Connection con = DatabaseConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT qty FROM boss_shop_purchases WHERE date = ? AND charId = ? AND matId = ?");
            ps.setDate(1, java.sql.Date.valueOf(date));
            ps.setInt(2, charId);
            ps.setInt(3, matId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getInt("qty");
            }
        } catch (SQLException e) {
            System.err.println("[BossShopManager] Failed to get purchase count: " + e.getMessage());
        }

        // Debugging: print current purchase count
        System.out.println("[BossShopManager] Current purchase count for charId " + charId + " Material ID " + matId + ": " + count);
        return count;
    }

    /**
     * Returns the purchase limit for a given material.
     * @param materialId The material's ID (e.g., 4001017 for Zakum Eye of Fire).
     * @return The purchase limit for the material.
     */
    public static int getMaterialPurchaseLimit(int materialId) {
        // Debugging: print the material ID and the corresponding purchase limit
        int limit = 0;
        if (materialId == 4001017) { // Zakum Eye of Fire
            limit = 100;  // Max 10 purchases per day
        } else if (materialId == 4021032) { // Horntail Mana Crystal
            limit = 100;  // Max 10 purchases per day
        } else if (materialId == 4001189) { // Pink Poppin (Pink Bean)
            limit = 100;  // Max 10 purchases per day
        } else if (materialId == 4001694) { // Von Leon Ticket
            limit = 100;  // Max 10 purchases per day
        }

        System.out.println("[BossShopManager] Material ID " + materialId + " Purchase Limit: " + limit);
        return limit;  // Default to 0 if material ID is unknown
    }

    /**
     * Checks eligibility for purchasing materials based on the daily purchase limit.
     * This method will return an array of remaining purchase quantities for each material.
     * @param charId The character ID of the player.
     * @return An array of material purchase limits remaining today.
     */
    public static int[] checkEligibility(int charId) {
        int[] eligibilityArray = new int[4];  // Zakum Eye of Fire (index 0), Horntail Mana Crystal (index 1), Pink Poppin (index 2), Von Leon Ticket (index 3)
        LocalDate today = LocalDate.now();

        // Debugging: print the eligibility check process
        System.out.println("[BossShopManager] Checking eligibility for charId " + charId);

        // Check eligibility for Zakum Eye of Fire
        int currentQtyZakum = getCurrentPurchaseCount(charId, 4001017, today);
        int limitZakum = getMaterialPurchaseLimit(4001017);
        eligibilityArray[0] = Math.max(0, limitZakum - currentQtyZakum);  // Remaining quantity

        // Check eligibility for Horntail Mana Crystal
        int currentQtyHorntail = getCurrentPurchaseCount(charId, 4021032, today);
        int limitHorntail = getMaterialPurchaseLimit(4021032);
        eligibilityArray[1] = Math.max(0, limitHorntail - currentQtyHorntail);  // Remaining quantity

        // Check eligibility for Pink Poppin (Pink Bean)
        int currentQtyPinkPoppin = getCurrentPurchaseCount(charId, 4001189, today);
        int limitPinkPoppin = getMaterialPurchaseLimit(4001189);
        eligibilityArray[2] = Math.max(0, limitPinkPoppin - currentQtyPinkPoppin);  // Remaining quantity

        // Check eligibility for Von Leon Ticket
        int currentQtyVonLeon = getCurrentPurchaseCount(charId, 4001694, today);
        int limitVonLeon = getMaterialPurchaseLimit(4001694);
        eligibilityArray[3] = Math.max(0, limitVonLeon - currentQtyVonLeon);  // Remaining quantity

        // Debugging: print eligibility for each material
        System.out.println("[BossShopManager] Eligibility for Zakum Eye of Fire: " + eligibilityArray[0] + " remaining.");
        System.out.println("[BossShopManager] Eligibility for Horntail Mana Crystal: " + eligibilityArray[1] + " remaining.");
        System.out.println("[BossShopManager] Eligibility for Pink Poppin: " + eligibilityArray[2] + " remaining.");
        System.out.println("[BossShopManager] Eligibility for Von Leon Ticket: " + eligibilityArray[3] + " remaining.");

        return eligibilityArray;
    }
}
