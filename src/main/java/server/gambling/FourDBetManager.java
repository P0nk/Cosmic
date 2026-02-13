package server.gambling;

import tools.DatabaseConnection;
import java.sql.*;
import java.util.*;

/**
 * Handles 4D bet submission, player prize tracking, and bet claim management.
 */
public class FourDBetManager {

    /**
     * Inserts a new bet into the database for a player.
     * Includes support for iBet (System Entry).
     *
     * @param charId       Player ID placing the bet.
     * @param number       4-digit number as a string (0000–9999).
     * @param type         "BIG" or "SMALL" bet type.
     * @param date         Draw date string (yyyy-MM-dd).
     * @param amount       Ticket quantity (as string).
     * @param currencyType Currency used ("NX", "MESO", "BCOIN", etc.).
     * @param isIBet       True if this is a System Entry (permutation bet).
     */
    public static void insertBet(int charId, String number, String type, String date, String amount,
            String currencyType, boolean isIBet) {
        String sql = "INSERT INTO 4d_bets (char_id, bet_number, bet_type, draw_date, amount, currency_type, is_ibet) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, charId);
            ps.setString(2, number);
            ps.setString(3, type);
            ps.setDate(4, java.sql.Date.valueOf(date));
            ps.setInt(5, Integer.parseInt(amount.trim()));
            ps.setString(6, currencyType);
            ps.setInt(7, isIBet ? 1 : 0);

            ps.executeUpdate();

            // --- BONUS POT ACCUMULATION ---
            // 70% of the bet goes to the pot
            long betAmount = Long.parseLong(amount.trim());
            long potContribution = (long) (betAmount * 0.70);
            if (potContribution > 0) {
                // Determine pot key based on currency
                String potKey = "4D_" + currencyType; // e.g., 4D_MESO, 4D_NX
                GamblingPotManager.addToPot(potKey, potContribution);
            }

        } catch (SQLException | NumberFormatException e) {
            System.out.println("[FourDBetManager] insertBet failed: " + e.getMessage());
        }
    }

    /**
     * Returns unclaimed winning bets for a player based on currency type.
     */
    public static List<Map<String, Object>> getUnclaimedWinningBets(int characterId, String currencyType) {
        List<Map<String, Object>> results = new ArrayList<>();
        // Select prize_item_id to distinguish between Currency (-1) and Items (3020002)
        String sql = "SELECT bet_id, prize_quantity, prize_item_id FROM 4d_bets WHERE char_id = ? AND is_winner = 1 AND claimed = 0 AND currency_type = ?";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, characterId);
            ps.setString(2, currencyType);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("bet_id", rs.getInt("bet_id"));
                    row.put("prize_quantity", rs.getLong("prize_quantity")); // Changed to getLong
                    row.put("prize_item_id", rs.getInt("prize_item_id"));
                    results.add(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Marks a specific bet as claimed.
     */
    public static void markBetClaimed(int betId) {
        String sql = "UPDATE 4d_bets SET claimed = 1 WHERE bet_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, betId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[FourDBetManager] markBetClaimed failed: " + e.getMessage());
        }
    }

    /**
     * Fetches the player's past bets (limited count).
     */
    public static List<Map<String, Object>> getPastBets(int charId, int limit) {
        List<Map<String, Object>> bets = new ArrayList<>();
        String sql = "SELECT draw_date, bet_number, bet_type, amount, is_ibet, currency_type FROM 4d_bets " +
                "WHERE char_id = ? ORDER BY bet_id DESC LIMIT ?";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, charId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("draw_date", rs.getString("draw_date"));
                    row.put("number", rs.getString("bet_number"));
                    row.put("bet_type", rs.getString("bet_type"));
                    row.put("amount", rs.getInt("amount"));
                    row.put("currency", rs.getString("currency_type"));
                    row.put("is_ibet", rs.getInt("is_ibet") == 1);
                    bets.add(row);
                }
            }

        } catch (SQLException e) {
            System.out.println("[FourDBetManager] getPastBets failed: " + e.getMessage());
        }

        return bets;
    }
}