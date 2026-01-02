package server.teleport;

import tools.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TeleportSavedMapManager
 * - Handles account-based saved teleport locations + limit management.
 *
 * Tables assumed:
 *  1) cosmic.tp_locations (accountid INT, mapid INT, PRIMARY KEY(accountid, mapid))
 *  2) cosmic.tp_limits    (accountid INT PRIMARY KEY, tplimit INT)
 */
public final class TeleportSavedMapManager {

    private TeleportSavedMapManager() {}

    // ============================== Account lookup ==============================

    public static int getAccountIdByCharacterName(String characterName) {
        int accountId = -1;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT accountid FROM cosmic.characters WHERE name = ?")) {

            ps.setString(1, characterName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    accountId = rs.getInt("accountid");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return accountId;
    }

    // ============================== Saved maps ==============================

    public static List<Integer> getSavedMaps(int accountId) {
        List<Integer> maps = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT mapid FROM cosmic.tp_locations WHERE accountid = ? ORDER BY mapid")) {

            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    maps.add(rs.getInt("mapid"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return maps;
    }

    /**
     * Returns true if (accountid, mapid) already exists.
     */
    public static boolean hasSavedMap(int accountId, int mapId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT 1 FROM cosmic.tp_locations WHERE accountid = ? AND mapid = ? LIMIT 1")) {

            ps.setInt(1, accountId);
            ps.setInt(2, mapId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Safe save: checks for existing PK combo first.
     * Returns:
     *  - true  => inserted successfully
     *  - false => already existed OR failed
     */
    public static boolean saveCurrentMap(int accountId, int mapId) {
        // Fast path: if already exists, do nothing (prevents duplicate PK error)
        if (hasSavedMap(accountId, mapId)) {
            return false;
        }

        // Insert (extra safety: could also use INSERT IGNORE if you prefer)
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO cosmic.tp_locations (accountid, mapid) VALUES (?, ?)")) {

            ps.setInt(1, accountId);
            ps.setInt(2, mapId);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            // In case of race condition (double-click / double call), ignore dup PK cleanly
            if (isDuplicateKey(e)) {
                return false;
            }
            e.printStackTrace();
        }
        return false;
    }

    public static void removeSavedMap(int accountId, int mapId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM cosmic.tp_locations WHERE accountid = ? AND mapid = ?")) {

            ps.setInt(1, accountId);
            ps.setInt(2, mapId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ============================== Limit management ==============================

    public static int getMapLimit(int accountId) {
        int limit = 10;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT tplimit FROM cosmic.tp_limits WHERE accountid = ?")) {

            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    limit = rs.getInt("tplimit");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return limit;
    }

    /**
     * Increases tplimit by increment; inserts row if missing.
     */
    public static void increaseMapLimit(int accountId, int increment) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO cosmic.tp_limits (accountid, tplimit) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE tplimit = tplimit + VALUES(tplimit) - tplimit + ?")) {
            // The UPDATE expression above looks weird, so let's keep it simple:
            // We'll just do: tplimit = tplimit + ?
        }
        catch(SQLException ignored){}
        // Re-implement cleanly below:
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO cosmic.tp_limits (accountid, tplimit) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE tplimit = tplimit + ?")) {

            ps.setInt(1, accountId);
            ps.setInt(2, 10 + increment); // if first time, base default 10 + increment
            ps.setInt(3, increment);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ============================== Utilities ==============================

    /**
     * Use this for logging / debug messages; your JS can call cm.getMap().getMapName() already,
     * but if you want a backend map name, you need WZ lookup or map factory, NOT getMap().getMapName().
     *
     * So I’m intentionally NOT implementing "getMapName(mapId)" here,
     * because the correct implementation depends on your server’s map factory / data provider.
     */
    public static boolean isDuplicateKey(SQLException e) {
        // MySQL dup key SQLState is usually "23000" and error code 1062
        return "23000".equals(e.getSQLState()) || e.getErrorCode() == 1062;
    }
}
