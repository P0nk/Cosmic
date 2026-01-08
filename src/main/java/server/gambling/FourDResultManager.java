package server.gambling;

import tools.DatabaseConnection;
import tools.DiscordWebhook;
import tools.EnvLoader;
import tools.PacketCreator;
import net.packet.Packet;
import net.server.Server;
import net.server.world.World;

import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * Manages 4D draw creation, winner evaluation (Direct/iBet), and Discord announcements.
 */
public class FourDResultManager {

    private static final int DRAW_HOUR = 0; // 12 AM GMT+8

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
     * Evaluates all bets for the given draw date.
     * Supports iBet logic (permutation matching and prize splitting).
     */
    public static void evaluateBets(LocalDate date) {
        try (Connection con = DatabaseConnection.getConnection()) {

            // 1. Load Draw Results
            PreparedStatement getResult = con.prepareStatement("SELECT * FROM 4d_results WHERE draw_date = ?");
            getResult.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = getResult.executeQuery();
            if (!rs.next()) return;

            String first = rs.getString("prize_1st");
            String second = rs.getString("prize_2nd");
            String third = rs.getString("prize_3rd");
            List<String> starters = Arrays.asList(rs.getString("starters").split(","));
            List<String> consolations = Arrays.asList(rs.getString("consolations").split(","));

            // 2. Load Bets
            PreparedStatement getBets = con.prepareStatement(
                    "SELECT bet_id, char_id, bet_number, bet_type, amount, currency_type, is_ibet FROM 4d_bets WHERE draw_date = ?");
            getBets.setDate(1, java.sql.Date.valueOf(date));
            ResultSet bets = getBets.executeQuery();

            boolean hasWinner = false;
            List<String> jackpotWinners = new ArrayList<>();

            while (bets.next()) {
                int betId = bets.getInt("bet_id");
                int charId = bets.getInt("char_id");
                String betNumber = bets.getString("bet_number");
                String type = bets.getString("bet_type"); // "BIG" or "SMALL"
                int amount = bets.getInt("amount"); // Number of Tickets
                String currency = bets.getString("currency_type");
                boolean isIBet = bets.getInt("is_ibet") == 1;

                long totalPrize = 0;
                String tier = null;

                // --- CHECKING LOGIC ---
                if (checkMatch(betNumber, first, isIBet)) {
                    totalPrize = calculatePrize(type, "1st", currency, amount, betNumber, isIBet);
                    tier = "1st Prize";
                }
                else if (checkMatch(betNumber, second, isIBet)) {
                    totalPrize = calculatePrize(type, "2nd", currency, amount, betNumber, isIBet);
                    tier = "2nd Prize";
                }
                else if (checkMatch(betNumber, third, isIBet)) {
                    totalPrize = calculatePrize(type, "3rd", currency, amount, betNumber, isIBet);
                    tier = "3rd Prize";
                }
                else {
                    for (String s : starters) {
                        if (checkMatch(betNumber, s, isIBet)) {
                            totalPrize = calculatePrize(type, "Starter", currency, amount, betNumber, isIBet);
                            tier = "Starter"; break;
                        }
                    }
                    if (totalPrize == 0) {
                        for (String c : consolations) {
                            if (checkMatch(betNumber, c, isIBet)) {
                                totalPrize = calculatePrize(type, "Consolation", currency, amount, betNumber, isIBet);
                                tier = "Consolation"; break;
                            }
                        }
                    }
                }

                // --- REWARD UPDATE ---
                if (totalPrize > 0) {
                    // Cap Mesos at Java Max Integer (2.1B) just in case
                    if (currency.equals("MESO") && totalPrize > 2147483647L) totalPrize = 2147483647L;

                    try (PreparedStatement update = con.prepareStatement(
                            "UPDATE 4d_bets SET is_winner = 1, prize_item_id = ?, prize_quantity = ? WHERE bet_id = ?")) {
                        // -1 for currencies (MESO/NX), ItemID for Items (BCOIN)
                        update.setInt(1, currency.equals("BCOIN") ? 3020002 : -1);
                        update.setInt(2, (int) totalPrize);
                        update.setInt(3, betId);
                        update.executeUpdate();
                    }

                    // --- BROADCAST JACKPOT ---
                    if ("1st Prize".equals(tier)) {
                        hasWinner = true;
                        String playerName = getCharacterNameById(charId);
                        if (playerName != null) {
                            jackpotWinners.add(playerName);
                            String msg = "[Merogie 4D] " + playerName + " won " + totalPrize + " " + currency +
                                    " with #" + betNumber + (isIBet ? " (iBet)" : "") + "!";
                            Packet packet = PacketCreator.serverNotice(6, msg);
                            for (World world : Server.getInstance().getWorlds()) {
                                Server.getInstance().broadcastMessage(world.getId(), packet);
                            }
                        }
                    }
                }
            }

            rs.close(); bets.close(); getBets.close(); getResult.close();

            // --- 3. GAME ANNOUNCEMENT ---
            String resultAnnouncement = "[4D Draw Results] 1st: " + first + " | 2nd: " + second + " | 3rd: " + third +
                    "\r\nStarters: " + String.join(", ", starters) +
                    "\r\nConsolations: " + String.join(", ", consolations);

            if (!hasWinner) resultAnnouncement += "\r\nNo 1st Prize winners this round. Better luck next time!";

            Packet broadcastPacket = PacketCreator.serverNotice(6, resultAnnouncement);
            for (World world : Server.getInstance().getWorlds()) {
                Server.getInstance().broadcastMessage(world.getId(), broadcastPacket);
            }

            // --- 4. DISCORD ANNOUNCEMENT ---
            sendDiscordResult(date, first, second, third, starters, consolations, jackpotWinners);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- LOGIC HELPERS ---

    private static boolean checkMatch(String betNum, String winNum, boolean isIBet) {
        if (!isIBet) return betNum.equals(winNum);
        return sortString(betNum).equals(sortString(winNum));
    }

    private static String sortString(String str) {
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    /**
     * Calculates prize amount.
     * Logic: Base Multiplier * Currency Value * Ticket Count / Permutations (if iBet)
     */
    private static long calculatePrize(String betType, String tier, String currency, int amount, String betNumber, boolean isIBet) {
        long baseMultiplier = 0;

        // Base payout per 1 unit wagered
        switch (tier) {
            case "1st": baseMultiplier = betType.equals("BIG") ? 2000 : 3000; break;
            case "2nd": baseMultiplier = betType.equals("BIG") ? 1000 : 2000; break;
            case "3rd": baseMultiplier = betType.equals("BIG") ? 490  : 800;  break;
            case "Starter":     baseMultiplier = betType.equals("BIG") ? 250 : 0; break;
            case "Consolation": baseMultiplier = betType.equals("BIG") ? 60  : 0; break;
        }

        if (baseMultiplier == 0) return 0;

        // Determine value of "1 Ticket" based on Currency
        long currencyValue = 1;
        if (currency.equals("MESO")) currencyValue = 1000000; // 1 Ticket = 1M Mesos
        else if (currency.equals("NX")) currencyValue = 500;  // 1 Ticket = 500 NX
        else currencyValue = 1; // Items (BCOIN) = 1 Item

        long totalPayout = baseMultiplier * amount * currencyValue;

        // Reduce payout for iBet based on permutation count
        if (isIBet) {
            int permutations = getPermutationCount(betNumber);
            totalPayout = totalPayout / permutations;
        }

        return totalPayout;
    }

    private static int getPermutationCount(String number) {
        Map<Character, Integer> counts = new HashMap<>();
        for (char c : number.toCharArray()) counts.put(c, counts.getOrDefault(c, 0) + 1);

        int uniqueDigits = counts.size();
        if (uniqueDigits == 4) return 24; // abcd (1234)
        if (uniqueDigits == 3) return 12; // aabc (1123)
        if (uniqueDigits == 2) {
            for (int count : counts.values()) {
                if (count == 3) return 4; // aaab (1112)
            }
            return 6; // aabb (1122)
        }
        return 1; // aaaa (1111)
    }

    private static void sendDiscordResult(LocalDate date, String first, String second, String third,
                                          List<String> starters, List<String> consolations, List<String> winners) {
        String webhookUrl = EnvLoader.get("DISCORD_ANNOUNCEMENT_WEBHOOK");
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"username\": \"Merogie Pools\",");
        json.append("\"embeds\": [{");
        json.append("\"title\": \"\uD83C\uDFB1 4D Draw Results\",");
        json.append("\"description\": \"**Draw Date:** " + date.toString() + "\",");
        json.append("\"color\": 16763904,");
        json.append("\"fields\": [");
        json.append("{\"name\": \"\uD83E\uDD47 1st Prize\", \"value\": \"**" + first + "**\", \"inline\": true},");
        json.append("{\"name\": \"\uD83E\uDD48 2nd Prize\", \"value\": \"**" + second + "**\", \"inline\": true},");
        json.append("{\"name\": \"\uD83E\uDD49 3rd Prize\", \"value\": \"**" + third + "**\", \"inline\": true},");
        json.append("{\"name\": \"\uD83D\uDD39 Starters\", \"value\": \"`" + String.join(", ", starters) + "`\"},");
        json.append("{\"name\": \"\uD83D\uDD38 Consolations\", \"value\": \"`" + String.join(", ", consolations) + "`\"}");

        if (!winners.isEmpty()) {
            json.append(",");
            String winnerList = String.join(", ", winners);
            json.append("{\"name\": \"\uD83C\uDFC6 Jackpot Winners\", \"value\": \"Congratulations to: **" + DiscordWebhook.escape(winnerList) + "**!\"}");
        }

        json.append("],");
        json.append("\"footer\": {\"text\": \"To claim prizes, visit the 4D NPC.\"}");
        json.append("}]}");

        DiscordWebhook.sendEmbedAsync(webhookUrl, json.toString());
    }

    private static String getCharacterNameById(int id) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) { }
        return null;
    }

    public static Map<String, String> getResultByDate(LocalDate date) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM 4d_results WHERE draw_date = ?")) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, String> result = new HashMap<>();
                result.put("first", rs.getString("prize_1st"));
                result.put("second", rs.getString("prize_2nd"));
                result.put("third", rs.getString("prize_3rd"));
                result.put("starters", rs.getString("starters"));
                result.put("consolations", rs.getString("consolations"));
                return result;
            }
        } catch (SQLException e) { }
        return null;
    }

    public static List<String> getRecentDrawDates(int limit) {
        List<String> dates = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT draw_date FROM 4d_results ORDER BY draw_date DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) dates.add(rs.getString("draw_date"));
        } catch (SQLException e) { }
        return dates;
    }
}