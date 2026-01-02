package server.inventory;

import client.inventory.Item;
import tools.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OrePouchManager handles loading and saving a character's ore pouch contents.
 * This stores simple stackable items (like ores) outside of the main inventory.
 */
public class OrePouchManager {

    // --- Key Alerts (rate-limited) ---
    private static volatile long lastDbAlertAt = 0L;
    private static volatile long lastDataAlertAt = 0L;
    private static final long ALERT_COOLDOWN_MS = 60_000L;

    private static void alertDb(String msg, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastDbAlertAt >= ALERT_COOLDOWN_MS) {
            lastDbAlertAt = now;
            System.err.println("[OrePouchManager][DB] " + msg + (t != null ? " err=" + t.getMessage() : ""));
        }
    }

    private static void alertData(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastDataAlertAt >= ALERT_COOLDOWN_MS) {
            lastDataAlertAt = now;
            System.err.println("[OrePouchManager][ALERT] " + msg);
        }
    }

    /**
     * Loads the list of ore items from the database for a given character.
     *
     * @param characterId the character ID
     * @return list of Item objects stored in the ore pouch
     */
    public static List<Item> loadOrePouchItems(int characterId) {
        List<Item> ores = new ArrayList<>();

        String sql = "SELECT itemid, quantity FROM ore_pouch WHERE character_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, characterId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId = rs.getInt("itemid");
                    int qtyInt = rs.getInt("quantity");

                    if (itemId <= 0) {
                        alertData("Invalid itemId in ore_pouch. characterId=" + characterId + " itemId=" + itemId);
                        continue;
                    }

                    // Clamp quantity into short range (Item uses short quantity)
                    if (qtyInt <= 0) {
                        alertData("Non-positive quantity in ore_pouch. characterId=" + characterId + " itemId=" + itemId + " qty=" + qtyInt);
                        continue;
                    }

                    int clamped = Math.min(qtyInt, Short.MAX_VALUE);
                    if (clamped != qtyInt) {
                        alertData("Quantity overflow in ore_pouch (clamped). characterId=" + characterId + " itemId=" + itemId + " qty=" + qtyInt);
                    }

                    short quantity = (short) clamped;

                    // Create a basic stackable item (ETC ores)
                    Item item = new Item(itemId, (byte) 0, quantity);
                    ores.add(item);
                }
            }

        } catch (SQLException e) {
            alertDb("Failed to load ore pouch items. characterId=" + characterId, e);
        }

        return ores;
    }

    /**
     * Saves the current list of ore pouch items for a character.
     * This method wipes existing entries and inserts the new state.
     *
     * @param characterId the character ID
     * @param items       list of Item objects to save
     */
    public static void saveOrePouchItems(int characterId, List<Item> items) {
        if (items == null) items = new ArrayList<>();

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // Delete existing pouch entries
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM ore_pouch WHERE character_id = ?")) {
                ps.setInt(1, characterId);
                ps.executeUpdate();
            }

            // Insert new entries (skip if empty)
            if (!items.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO ore_pouch (character_id, itemid, quantity) VALUES (?, ?, ?)")) {

                    for (Item item : items) {
                        if (item == null) continue;

                        int itemId = item.getItemId();
                        int qty = item.getQuantity();

                        if (itemId <= 0 || qty <= 0) {
                            alertData("Skipping invalid ore pouch item on save. characterId=" + characterId +
                                    " itemId=" + itemId + " qty=" + qty);
                            continue;
                        }

                        // DB column is int; still keep it sane
                        if (qty > Short.MAX_VALUE) {
                            alertData("Ore pouch save qty overflow (clamped). characterId=" + characterId +
                                    " itemId=" + itemId + " qty=" + qty);
                            qty = Short.MAX_VALUE;
                        }

                        ps.setInt(1, characterId);
                        ps.setInt(2, itemId);
                        ps.setInt(3, qty);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                }
            }

            con.commit();

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException re) {
                    alertDb("Rollback failed while saving ore pouch. characterId=" + characterId, re);
                }
            }
            alertDb("Failed to save ore pouch items. characterId=" + characterId, e);

        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); } catch (SQLException ignore) {}
                try { con.close(); } catch (SQLException ignore) {}
            }
        }
    }
}
