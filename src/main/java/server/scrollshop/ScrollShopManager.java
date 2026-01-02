package server.scrollshop;

import tools.DatabaseConnection;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

/**
 * ScrollShopManager
 * ----------------------------------------------------------------
 * Manages the retrieval of scroll information, spell trace bank, and relationship values.
 * ----------------------------------------------------------------
 */
public class ScrollShopManager {

    /* ========================= Spell Trace Bank ========================= */

    /**
     * Retrieves the current spell trace balance for a specific character.
     * @param characterId The ID of the character.
     * @return The current spell trace balance.
     */
    public static int getSpellTraceBalance(int characterId) {
        String sql = "SELECT spell_trace_balance FROM scrollshop WHERE character_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("spell_trace_balance");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Updates the spell trace balance for a specific character.
     * @param characterId The ID of the character.
     * @param newBalance The new balance to set.
     */
    public static void updateSpellTraceBalance(int characterId, int newBalance) {
        String sql = "INSERT INTO scrollshop (character_id, spell_trace_balance) " +
                "VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE spell_trace_balance=?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Set parameters for the query
            ps.setInt(1, characterId); // Set the character_id
            ps.setInt(2, newBalance);  // Set the initial spell trace balance
            ps.setInt(3, newBalance);  // If record exists, update the spell_trace_balance

            // Execute the query
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* ========================= Relationship Value ========================= */

    /**
     * Retrieves the current relationship value for a specific character.
     * @param characterId The ID of the character.
     * @return The current relationship value.
     */
    public static int getRelationshipValue(int characterId) {
        String sql = "SELECT relationship_value FROM scrollshop WHERE character_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("relationship_value");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Updates the relationship value for a specific character.
     * @param characterId The ID of the character.
     * @param newValue The new relationship value to set.
     */
    public static void updateRelationshipValue(int characterId, int newValue) {
        String sql = "UPDATE scrollshop SET relationship_value=? WHERE character_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, newValue);
            ps.setInt(2, characterId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves scrolls from the basic shop filtered by the given success rate and equipment type prefix.
     * @param success The success rate (e.g., 100, 60, 10).
     * @param categoryPrefix The 5-digit prefix representing the equipment type (e.g., "20438" for Staff scrolls).
     * @return A List of scroll items available for the basic shop based on the success rate and category prefix.
     */
    public static List<Object[]> getShopByCategory(int success, String categoryPrefix) {
        // Construct the SQL query to filter by success rate and category prefix
        String sql = "SELECT * FROM scrollshopitems WHERE itemid LIKE ? AND success = ? AND toSell = 1";

        // Return the scroll items retrieved from the database
        return getScrollsFromDatabase(sql, categoryPrefix, success);
    }

    /**
     * Helper method to retrieve scrolls from the database based on the SQL query.
     * @param sql The SQL query to execute.
     * @param categoryPrefix The category prefix to filter by (e.g., "20438" for Staff).
     * @param success The success rate to filter by.
     * @return A list of Object arrays containing scroll details.
     */
    private static List<Object[]> getScrollsFromDatabase(String sql, String categoryPrefix, int success) {
        List<Object[]> scrollList = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Set the category prefix and success rate parameters for the SQL query
            ps.setString(1, categoryPrefix + "%");
            ps.setInt(2, success);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId = rs.getInt("itemid");

                    StringBuilder summary = new StringBuilder();
                    summary.append("#e#k").append(rs.getString("name")).append(" (").append(itemId).append(")#n\r\n#k#e");

                    String[] fieldNames = {
                            "incMAD", "incPAD", "incINT", "incSTR", "incLUK",
                            "incDEX", "incACC", "incEVA", "incJump", "incSpeed", "incMDD",
                            "incPDD", "incMMP", "incMHP", "cursed",
                            "recover", "reqRUC", "randstat", "preventsSlip", "warmsupport"
                    };

                    // Iterate over each field and append non-null values to the summary
                    for (String fieldName : fieldNames) {
                        String value = rs.getString(fieldName);
                        if (value != null && !value.isEmpty() && !value.equals("0")) {
                            summary.append("\t\t\t\t#e#k[").append(fieldName).append("]: #n#d").append(value).append("\r\n");
                        }
                    }

                    // Add the itemId and the summary to the scroll list
                    scrollList.add(new Object[] {itemId, summary.toString()});
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return scrollList;
    }

    /**
     * Toggles the toSell value for a given item in the scrollshopitems table.
     * @param itemid The ID of the scroll item.
     * @return 1 if toSell is now 1, 0 if toSell is now 0, 2 if not found.
     */
    public static int toggletoSell(int itemid) {
        String selectSql = "SELECT toSell FROM scrollshopitems WHERE itemid=?";
        String updateSql = "UPDATE scrollshopitems SET toSell=? WHERE itemid=?";
        int result = 2;  // Default to not found

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement selectPs = con.prepareStatement(selectSql)) {

            selectPs.setInt(1, itemid);
            try (ResultSet rs = selectPs.executeQuery()) {
                if (rs.next()) {
                    // If item found, toggle the toSell value
                    int currentSaleStatus = rs.getInt("toSell");
                    int newSaleStatus = (currentSaleStatus == 1) ? 0 : 1;  // Toggle logic

                    // Update the toSell value
                    try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                        updatePs.setInt(1, newSaleStatus);
                        updatePs.setInt(2, itemid);
                        updatePs.executeUpdate();
                    }

                    result = newSaleStatus;  // Set result to the new value of toSell
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
}
