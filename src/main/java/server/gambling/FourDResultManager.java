package server.gambling;

import tools.DatabaseConnection;
import tools.PacketCreator;
import net.packet.Packet;
import net.server.Server;
import net.server.world.World;

import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * Manages 4D draw creation, result storage, and winner evaluation.
 */
public class FourDResultManager {

    private static final int DRAW_HOUR = 0; // 12 AM GMT+8

    /**
     * Checks if a draw exists and is valid for the specified date.
     * Prevents false positives if the check is made before draw time.
     */
    public static boolean hasDrawToday(LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        if (date.equals(LocalDate.now()) && now.toLocalTime().isBefore(LocalTime.of(DRAW_HOUR, 0))) {
            return false; // Too early to consider today's draw valid
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM 4d_results WHERE draw_date = ?")) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.out.println("[FourDResultManager] hasDrawToday SQL error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stores the draw results in the database.
     */
    public static void storeDraw(LocalDate date, String first, String second, String third,
                                 List<String> starters, List<String> consolations) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO 4d_results (draw_date, prize_1st, prize_2nd, prize_3rd, starters, consolations) " +
                             "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ps.setString(2, first);
            ps.setString(3, second);
            ps.setString(4, third);
            ps.setString(5, String.join(",", starters));
            ps.setString(6, String.join(",", consolations));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[FourDResultManager] Failed to insert result: " + e.getMessage());
        }
    }

    /**
     * Evaluates all bets for the given draw date, updates winners, and broadcasts top prize wins.
     */
    public static void evaluateBets(LocalDate date) {
        try (Connection con = DatabaseConnection.getConnection()) {

            // Load draw results
            PreparedStatement getResult = con.prepareStatement("SELECT * FROM 4d_results WHERE draw_date = ?");
            getResult.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = getResult.executeQuery();
            if (!rs.next()) return;

            String first = rs.getString("prize_1st");
            String second = rs.getString("prize_2nd");
            String third = rs.getString("prize_3rd");
            List<String> starters = Arrays.asList(rs.getString("starters").split(","));
            List<String> consolations = Arrays.asList(rs.getString("consolations").split(","));

            // Load bets for the draw
            PreparedStatement getBets = con.prepareStatement(
                    "SELECT bet_id, char_id, bet_number, bet_type, amount FROM 4d_bets WHERE draw_date = ?");
            getBets.setDate(1, java.sql.Date.valueOf(date));
            ResultSet bets = getBets.executeQuery();

            while (bets.next()) {
                int betId = bets.getInt("bet_id");
                int charId = bets.getInt("char_id");
                String number = bets.getString("bet_number");
                String type = bets.getString("bet_type");
                int amount = bets.getInt("amount");

                int basePrize = 0;
                String tier = null;

                if (number.equals(first)) { basePrize = type.equals("BIG") ? 60 : 100; tier = "1st Prize"; }
                else if (number.equals(second)) { basePrize = type.equals("BIG") ? 30 : 50; tier = "2nd Prize"; }
                else if (number.equals(third)) { basePrize = type.equals("BIG") ? 15 : 25; tier = "3rd Prize"; }
                else if (type.equals("BIG") && starters.contains(number)) basePrize = 2;
                else if (type.equals("BIG") && consolations.contains(number)) basePrize = 1;

                int totalPrize = basePrize * amount;

                if (totalPrize > 0) {
                    // Update the bet as a winning entry
                    try (PreparedStatement update = con.prepareStatement(
                            "UPDATE 4d_bets SET is_winner = 1, prize_item_id = ?, prize_quantity = ? WHERE bet_id = ?")) {
                        update.setInt(1, 3020002); // MESO_BCOIN_ID
                        update.setInt(2, totalPrize);
                        update.setInt(3, betId);
                        update.executeUpdate();
                    }

                    // Broadcast 1st prize winners
                    if (number.equals(first)) {
                        String playerName = getCharacterNameById(charId);
                        if (playerName != null) {
                            String msg = "[★ Merogie Pools 4D Winner ★] " + playerName +
                                    " won " + totalPrize + " Meso BCoins with #" + number +
                                    " (" + tier + ", " + type + " bet) on draw " + date + "!";
                            Packet packet = PacketCreator.serverNotice(6, msg);
                            for (World world : Server.getInstance().getWorlds()) {
                                Server.getInstance().broadcastMessage(world.getId(), packet);
                            }
                        }
                    }
                }
            }

            rs.close();
            bets.close();
            getBets.close();
            getResult.close();

        } catch (SQLException e) {
            System.out.println("[FourDResultManager] Evaluation failed: " + e.getMessage());
        }
    }

    /**
     * Retrieves draw result details for the specified date.
     */
    public static Map<String, String> getResultByDate(LocalDate date) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM 4d_results WHERE draw_date = ?")) {

            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Map<String, String> result = new HashMap<>();
                result.put("date", date.toString());
                result.put("first", rs.getString("prize_1st"));
                result.put("second", rs.getString("prize_2nd"));
                result.put("third", rs.getString("prize_3rd"));
                result.put("starters", rs.getString("starters"));
                result.put("consolations", rs.getString("consolations"));
                return result;
            }

        } catch (SQLException e) {
            System.out.println("[FourDResultManager] Failed to fetch result: " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns the most recent draw dates up to a specified limit.
     */
    public static List<String> getRecentDrawDates(int limit) {
        List<String> dates = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT draw_date FROM 4d_results ORDER BY draw_date DESC LIMIT ?")) {

            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                dates.add(rs.getString("draw_date"));
            }

        } catch (SQLException e) {
            System.out.println("[FourDResultManager] Failed to fetch draw dates: " + e.getMessage());
        }
        return dates;
    }

    /**
     * Fetches the name of a character by their ID.
     */
    private static String getCharacterNameById(int id) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?")) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");

        } catch (SQLException e) {
            System.out.println("[FourDResultManager] Failed to fetch player name: " + e.getMessage());
        }
        return null;
    }
}
