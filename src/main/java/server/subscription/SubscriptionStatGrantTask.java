package server.subscription;

import tools.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs daily. On the 1st of each month, grants 5 unspent stat points to every
 * character whose subscription is currently active and who has not yet received
 * points this month.
 *
 * Register via TimerManager on server startup (24-hour interval,
 * midnight-safe).
 */
public class SubscriptionStatGrantTask implements Runnable {

    @Override
    public void run() {
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() != 1) {
            return; // Only grant on the 1st of the month
        }

        // last_stat_grant stores the year-month of the last grant as a DATE (day is
        // always 1).
        // We grant if last_stat_grant is NULL or < this month's 1st.
        String grantDate = today.toString(); // e.g. "2026-03-01"

        String selectSql = "SELECT characterid FROM cosmic.cosmic_subscriptions " +
                "WHERE expires_at > NOW() " +
                "AND (last_stat_grant IS NULL OR last_stat_grant < ?)";

        String updateSql = "UPDATE cosmic.cosmic_subscriptions SET " +
                "unspent_stat_points = unspent_stat_points + ?, " +
                "accumulated_stat_points = accumulated_stat_points + ?, " +
                "last_stat_grant = ? " +
                "WHERE characterid = ?";

        List<Integer> toGrant = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(selectSql)) {
            ps.setString(1, grantDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    toGrant.add(rs.getInt("characterid"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[SubscriptionStatGrantTask] Error selecting eligible subs: " + e.getMessage());
            return;
        }

        if (toGrant.isEmpty()) {
            System.out.println("[SubscriptionStatGrantTask] No subscribers to grant this month.");
            return;
        }

        int granted = 0;
        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(updateSql)) {
            for (int charId : toGrant) {
                ps.setInt(1, SubscriptionManager.STAT_POINTS_PER_MONTH);
                ps.setInt(2, SubscriptionManager.STAT_POINTS_PER_MONTH);
                ps.setString(3, grantDate);
                ps.setInt(4, charId);
                ps.addBatch();
                granted++;
            }
            ps.executeBatch();
        } catch (SQLException e) {
            System.err.println("[SubscriptionStatGrantTask] Error granting stat points: " + e.getMessage());
            return;
        }

        System.out.println("[SubscriptionStatGrantTask] Granted " + SubscriptionManager.STAT_POINTS_PER_MONTH +
                " stat point(s) to " + granted + " active subscriber(s) for " + grantDate + ".");
    }
}
