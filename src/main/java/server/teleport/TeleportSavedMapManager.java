package server.teleport;

import tools.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// WZ map name lookup
import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;

public final class TeleportSavedMapManager {

    private TeleportSavedMapManager() {}

    // Providers (cached)
    private static final DataProvider MAP_PROVIDER =
            DataProviderFactory.getDataProvider(WZFiles.MAP);     // kept in case you want existence checks later
    private static final DataProvider STRING_PROVIDER =
            DataProviderFactory.getDataProvider(WZFiles.STRING);

    // MapId -> resolved display name
    private static final Map<Integer, String> MAP_NAME_CACHE = new ConcurrentHashMap<>();

    // Toggle if you want occasional diagnostics without spam
    private static final boolean LOG_MAPNAME_MISS = true;

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
     * @return true if inserted, false if already existed (or duplicate-race)
     */
    public static boolean saveCurrentMap(int accountId, int mapId) {
        if (hasSavedMap(accountId, mapId)) {
            return false;
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO cosmic.tp_locations (accountid, mapid) VALUES (?, ?)")) {

            ps.setInt(1, accountId);
            ps.setInt(2, mapId);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            // Double-click / race condition safety (MySQL dup key)
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
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

    public static void increaseMapLimit(int accountId, int increment) {
        int base = 10;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO cosmic.tp_limits (accountid, tplimit) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE tplimit = tplimit + ?")) {

            ps.setInt(1, accountId);
            ps.setInt(2, base + increment);
            ps.setInt(3, increment);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ============================== Map name lookup ==============================

    /**
     * Resolves map display name via String.wz -> Map.img -> <region> -> <mapId>
     * Returns "Unknown Map" if not found.
     */
    public static String getMapName(int mapId) {
        String cached = MAP_NAME_CACHE.get(mapId);
        if (cached != null) {
            return cached;
        }

        String resolved = resolveMapNameFromStringWz(mapId);
        MAP_NAME_CACHE.put(mapId, resolved);

        if (LOG_MAPNAME_MISS && "Unknown Map".equals(resolved)) {
            System.out.println("[TP][MapName] Unknown mapId=" + mapId + " (not found in String.wz/Map.img)");
        }

        return resolved;
    }

    private static String resolveMapNameFromStringWz(int mapId) {
        try {
            Data mapImgRoot = STRING_PROVIDER.getData("Map.img");
            if (mapImgRoot == null) {
                return "Unknown Map";
            }

            Data foundNode = null;

            // Map.img/<regionName>/<mapId>
            for (Data region : mapImgRoot.getChildren()) {
                if (region == null) continue;

                Data candidate = region.getChildByPath(String.valueOf(mapId));
                if (candidate != null) {
                    foundNode = candidate;
                    break;
                }
            }

            if (foundNode == null) {
                return "Unknown Map";
            }

            String streetName = DataTool.getString("streetName", foundNode, "");
            String mapName = DataTool.getString("mapName", foundNode, "");

            if (!streetName.isEmpty() && !mapName.isEmpty()) {
                return streetName + " : " + mapName;
            }
            if (!mapName.isEmpty()) return mapName;
            if (!streetName.isEmpty()) return streetName;

            return "Unknown Map";

        } catch (Exception e) {
            // Keep a single meaningful print, instead of spamming every lookup.
            if (LOG_MAPNAME_MISS) {
                System.out.println("[TP][MapName] Exception resolving mapId=" + mapId + ": " + e.getMessage());
            }
            return "Unknown Map";
        }
    }
}
