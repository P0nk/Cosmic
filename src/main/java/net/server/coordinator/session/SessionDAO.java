package net.server.coordinator.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SessionDAO {
    private static final Logger log = LoggerFactory.getLogger(SessionDAO.class);

    public static void deleteExpiredHwidAccounts() {
        final String query = "DELETE FROM hwidaccounts WHERE expiresat < CURRENT_TIMESTAMP";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to delete expired hwidaccounts", e);
        }
    }

    public static List<Hwid> getHwidsForAccount(Connection con, int accountId) throws SQLException {
        final List<Hwid> hwids = new ArrayList<>();

        final String query = "SELECT hwid FROM hwidaccounts WHERE accountid = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hwids.add(new Hwid(rs.getString("hwid")));
                }
            }
        }

        return hwids;
    }

    public static void registerAccountAccess(Connection con, int accountId, Hwid hwid, Instant expiry)
            throws SQLException {
        if (hwid == null) {
            throw new IllegalArgumentException("Hwid must not be null");
        }

        final String query = "INSERT INTO hwidaccounts (accountid, hwid, expiresat) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, accountId);
            ps.setString(2, hwid.hwid());
            ps.setTimestamp(3, Timestamp.from(expiry));

            ps.executeUpdate();
        }
    }

    public static List<HwidRelevance> getHwidRelevance(Connection con, int accountId) throws SQLException {
        final List<HwidRelevance> hwidRelevances = new ArrayList<>();

        final String query = "SELECT * FROM hwidaccounts WHERE accountid = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String hwid = rs.getString("hwid");
                    int relevance = rs.getInt("relevance");
                    hwidRelevances.add(new HwidRelevance(hwid, relevance));
                }
            }
        }

        return hwidRelevances;
    }

    public static void updateAccountAccess(Connection con, Hwid hwid, int accountId, Instant expiry, int loginRelevance)
            throws SQLException {
        final String query = "UPDATE hwidaccounts SET relevance = ?, expiresat = ? WHERE accountid = ? AND hwid LIKE ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, loginRelevance);
            ps.setTimestamp(2, Timestamp.from(expiry));
            ps.setInt(3, accountId);
            ps.setString(4, hwid.hwid());

            ps.executeUpdate();
        }
    }
}
