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

    // Cache provider so we don't reload for every call
    private static final DataProvider MAP_PROVIDER =
            DataProviderFactory.getDataProvider(WZFiles.MAP);

    private static final DataProvider STRING_PROVIDER =
            DataProviderFactory.getDataProvider(WZFiles.STRING);

    private static final Map<Integer, String> MAP_NAME_CACHE = new ConcurrentHashMap<>();

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

    public static boolean saveCurrentMap(int accountId, int mapId) {
        if (hasSavedMap(accountId, mapId)) {
            return false; // already saved
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
     * Returns map name for a given mapId using Map.wz data.
     * Works even when player is not currently in that map.
     */
    public static String getMapName(int mapId) {
        // Fast cache
        String cached = MAP_NAME_CACHE.get(mapId);
        if (cached != null) {
            System.out.println("[TP][MapName] (cache hit) mapId=" + mapId + " -> " + cached);
            return cached;
        }

        System.out.println("[TP][MapName] ===== START =====");
        System.out.println("[TP][MapName] mapId = " + mapId);

        try {
            Data mapImgRoot = STRING_PROVIDER.getData("Map.img");
            System.out.println("[TP][MapName] Map.img found? " + (mapImgRoot != null));

            if (mapImgRoot == null) {
                System.out.println("[TP][MapName] Map.img is null -> Unknown Map");
                return cacheAndReturn(mapId, "Unknown Map");
            }

            // Your Map.img structure is: Map.img/<regionName>/<mapId>
            // Example regions: maple, victoria, ossyria, elin, etc...
            Data foundNode = null;
            String foundRegion = null;

            // Print root children (regions)
            System.out.println("[TP][MapName] Scanning region folders under Map.img...");
            for (Data region : mapImgRoot.getChildren()) {
                if (region == null) continue;

                String regionName = region.getName();
                System.out.println("[TP][MapName] - region: " + regionName);

                // Try mapId directly under this region folder
                Data candidate = region.getChildByPath(String.valueOf(mapId));
                if (candidate != null) {
                    foundNode = candidate;
                    foundRegion = regionName;
                    System.out.println("[TP][MapName] FOUND mapId under region: " + foundRegion);
                    break;
                }
            }

            System.out.println("[TP][MapName] foundNode? " + (foundNode != null));

            if (foundNode == null) {
                System.out.println("[TP][MapName] No entry for mapId in any region folder -> Unknown Map");
                return cacheAndReturn(mapId, "Unknown Map");
            }

            // In String.wz Map.img nodes: streetName / mapName (no "info/")
            String streetName = DataTool.getString("streetName", foundNode, "");
            String mapName = DataTool.getString("mapName", foundNode, "");

            System.out.println("[TP][MapName] region     = " + foundRegion);
            System.out.println("[TP][MapName] streetName = '" + streetName + "'");
            System.out.println("[TP][MapName] mapName    = '" + mapName + "'");

            String result;
            if (!streetName.isEmpty() && !mapName.isEmpty()) {
                result = streetName + " : " + mapName;
            } else if (!mapName.isEmpty()) {
                result = mapName;
            } else if (!streetName.isEmpty()) {
                result = streetName;
            } else {
                result = "Unknown Map";
            }

            System.out.println("[TP][MapName] RESULT = " + result);
            return cacheAndReturn(mapId, result);

        } catch (Exception e) {
            System.out.println("[TP][MapName] EXCEPTION:");
            e.printStackTrace();
            return cacheAndReturn(mapId, "Unknown Map");
        }
    }

    private static String cacheAndReturn(int mapId, String name) {
        // Avoid caching "Unknown Map" forever if you’d like; but for now cache everything.
        MAP_NAME_CACHE.put(mapId, name);
        return name;
    }

}
