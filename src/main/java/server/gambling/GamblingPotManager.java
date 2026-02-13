package server.gambling;

import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Manages shared pots for server gambling games (e.g., 4D, Lottery).
 */
public class GamblingPotManager {

    /**
     * Adds an amount to the specified game pot.
     * Uses atomic DB update (current_amount = current_amount + ?).
     *
     * @param gameKey Check 'gambling_pots' table for keys (e.g., "4D_MESO").
     * @param amount  Amount to add.
     */
    public static void addToPot(String gameKey, long amount) {
        if (amount <= 0)
            return;
        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE gambling_pots SET current_amount = current_amount + ? WHERE game_key = ?")) {
            ps.setLong(1, amount);
            ps.setString(2, gameKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[GamblingPotManager] Error adding to pot (" + gameKey + "): " + e.getMessage());
        }
    }

    /**
     * Retrieves the current total amount in the pot.
     */
    public static long getPot(String gameKey) {
        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con
                        .prepareStatement("SELECT current_amount FROM gambling_pots WHERE game_key = ?")) {
            ps.setString(1, gameKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("current_amount");
                }
            }
        } catch (SQLException e) {
            System.err.println("[GamblingPotManager] Error getting pot (" + gameKey + "): " + e.getMessage());
        }
        return 0;
    }

    /**
     * Distributes a percentage of the pot and returns the distributed amount.
     * Example: distributePot("4D_MESO", 0.60) takes 60% out of the pot.
     *
     * @param gameKey    Game key.
     * @param percentage Percentage to take (0.0 - 1.0).
     * @return The absolute amount taken from the pot.
     */
    public static long distributePot(String gameKey, double percentage) {
        if (percentage <= 0 || percentage > 1.0)
            return 0;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false); // Transaction for atomic read-modify-write

            // 1. Get Current Amount with Lock
            long currentPot = 0;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT current_amount FROM gambling_pots WHERE game_key = ? FOR UPDATE")) {
                ps.setString(1, gameKey);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentPot = rs.getLong("current_amount");
                    }
                }
            }

            if (currentPot <= 0) {
                con.rollback();
                return 0;
            }

            // 2. Calculate Share
            long share = (long) (currentPot * percentage);
            long newPot = currentPot - share;

            // 3. Update DB
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE gambling_pots SET current_amount = ? WHERE game_key = ?")) {
                ps.setLong(1, newPot);
                ps.setString(2, gameKey);
                ps.executeUpdate();
            }

            con.commit();
            con.setAutoCommit(true);
            return share;

        } catch (SQLException e) {
            System.err.println("[GamblingPotManager] Error distributing pot (" + gameKey + "): " + e.getMessage());
        }
        return 0;
    }
}
