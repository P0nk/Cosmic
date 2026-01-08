package server.gambling;

import tools.DatabaseConnection;
import tools.DiscordWebhook; // Import your Webhook tool
import tools.EnvLoader;      // Import EnvLoader to get the URL
import tools.PacketCreator;
import net.packet.Packet;
import net.server.Server;
import net.server.world.World;

import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * Manages 4D draw creation, result storage, winner evaluation, and Discord announcements.
 */
public class FourDResultManager {

    private static final int DRAW_HOUR = 0; // 12 AM GMT+8

    /**
     * Checks if a draw exists and is valid for the specified date.
     */
    public static boolean hasDrawToday(LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        if (date.equals(LocalDate.now()) && now.toLocalTime().isBefore(LocalTime.of(DRAW_HOUR, 0))) {
            return false;
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

            boolean hasWinner = false;
            List<String> jackpotWinners = new ArrayList<>(); // Track 1st prize winners for Discord

            while (bets.next()) {
                int betId = bets.getInt("bet_id");
                int charId = bets.getInt("char_id");
                String number = bets.getString("bet_number");
                String type = bets.getString("bet_type");
                int amount = bets.getInt("amount");

                int basePrize = 0;
                String tier = null;

                if (number.equals(first)) { basePrize = type.equals("BIG") ? 2000 : 3000; tier = "1st Prize"; }
                else if (number.equals(second)) { basePrize = type.equals("BIG") ? 1000 : 2000; tier = "2nd Prize"; }
                else if (number.equals(third)) { basePrize = type.equals("BIG") ? 490 : 800; tier = "3rd Prize"; }
                else if (type.equals("BIG") && starters.contains(number)) basePrize = 250;
                else if (type.equals("BIG") && consolations.contains(number)) basePrize = 60;

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
                        hasWinner = true;
                        String playerName = getCharacterNameById(charId);
                        if (playerName != null) {
                            jackpotWinners.add(playerName); // Add to local list for Discord
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

            // --- 1. GAME ANNOUNCEMENT ---
            String resultAnnouncement = "[4D Draw Results] 1st: " + first + " | 2nd: " + second + " | 3rd: " + third +
                    "\r\nStarters: " + String.join(", ", starters) +
                    "\r\nConsolations: " + String.join(", ", consolations);

            if (!hasWinner) {
                resultAnnouncement += "\r\nNo 1st Prize winners this round. Better luck next time!";
            }

            Packet broadcastPacket = PacketCreator.serverNotice(6, resultAnnouncement);
            for (World world : Server.getInstance().getWorlds()) {
                Server.getInstance().broadcastMessage(world.getId(), broadcastPacket);
            }

            // --- 2. DISCORD ANNOUNCEMENT ---
            sendDiscordResult(date, first, second, third, starters, consolations, jackpotWinners);

        } catch (SQLException e) {
            System.out.println("[FourDResultManager] Evaluation failed: " + e.getMessage());
        }
    }

    /**
     * Constructs and sends the Discord Embed.
     */
    private static void sendDiscordResult(LocalDate date, String first, String second, String third,
                                          List<String> starters, List<String> consolations, List<String> winners) {

        // Fetch Webhook URL from EnvLoader (same as DailyRanking)
        String webhookUrl = EnvLoader.get("DISCORD_ANNOUNCEMENT_WEBHOOK");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.out.println("[FourDResultManager] No Discord Webhook configured. Skipping.");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"username\": \"Merogie Pools\",");
        json.append("\"embeds\": [{");
        json.append("\"title\": \"\uD83C\uDFB1 4D Draw Results\","); // 🎱 emoji
        json.append("\"description\": \"**Draw Date:** " + date.toString() + "\",");
        json.append("\"color\": 16763904,"); // Gold
        json.append("\"fields\": [");

        // Top 3 Prizes
        json.append("{\"name\": \"\uD83E\uDD47 1st Prize\", \"value\": \"**" + first + "**\", \"inline\": true},");
        json.append("{\"name\": \"\uD83E\uDD48 2nd Prize\", \"value\": \"**" + second + "**\", \"inline\": true},");
        json.append("{\"name\": \"\uD83E\uDD49 3rd Prize\", \"value\": \"**" + third + "**\", \"inline\": true},");

        // Starters (Formatted in code block for alignment)
        json.append("{\"name\": \"\uD83D\uDD39 Starters\", \"value\": \"`" + String.join(", ", starters) + "`\"},");

        // Consolations
        json.append("{\"name\": \"\uD83D\uDD38 Consolations\", \"value\": \"`" + String.join(", ", consolations) + "`\"}");

        // Add Winners Field if anyone won 1st prize
        if (!winners.isEmpty()) {
            json.append(",");
            String winnerList = String.join(", ", winners);
            json.append("{\"name\": \"\uD83C\uDFC6 Jackpot Winners\", \"value\": \"Congratulations to: **" + DiscordWebhook.escape(winnerList) + "**!\"}");
        }

        json.append("],");
        json.append("\"footer\": {\"text\": \"To claim prizes, visit the 4D NPC.\"}");
        json.append("}]}");

        // Send
        DiscordWebhook.sendEmbedAsync(webhookUrl, json.toString());
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

    // ... (Keep getResultByDate and getRecentDrawDates as they were in your original code) ...

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
}