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
 * Manages 4D draw creation, winner evaluation (Direct/iBet), and Discord
 * announcements.
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
            if (!rs.next())
                return;

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

            List<WinnerInfo> winners = new ArrayList<>();
            List<String> jackpotWinners = new ArrayList<>();

            while (bets.next()) {
                int betId = bets.getInt("bet_id");
                int charId = bets.getInt("char_id");
                String betNumber = bets.getString("bet_number");
                String type = bets.getString("bet_type");
                int amount = bets.getInt("amount");
                String currency = bets.getString("currency_type");
                boolean isIBet = bets.getInt("is_ibet") == 1;

                long basePrize = 0;
                String tier = null;

                // --- CHECKING LOGIC ---
                if (checkMatch(betNumber, first, isIBet)) {
                    basePrize = calculatePrize(type, "1st", currency, amount, betNumber, isIBet);
                    tier = "1st Prize";
                } else if (checkMatch(betNumber, second, isIBet)) {
                    basePrize = calculatePrize(type, "2nd", currency, amount, betNumber, isIBet);
                    tier = "2nd Prize";
                } else if (checkMatch(betNumber, third, isIBet)) {
                    basePrize = calculatePrize(type, "3rd", currency, amount, betNumber, isIBet);
                    tier = "3rd Prize";
                } else {
                    for (String s : starters) {
                        if (checkMatch(betNumber, s, isIBet)) {
                            basePrize = calculatePrize(type, "Starter", currency, amount, betNumber, isIBet);
                            tier = "Starter";
                            break;
                        }
                    }
                    if (basePrize == 0) {
                        for (String c : consolations) {
                            if (checkMatch(betNumber, c, isIBet)) {
                                basePrize = calculatePrize(type, "Consolation", currency, amount, betNumber, isIBet);
                                tier = "Consolation";
                                break;
                            }
                        }
                    }
                }

                if (basePrize > 0) {
                    winners.add(
                            new WinnerInfo(betId, charId, betNumber, type, amount, currency, isIBet, basePrize, tier));
                }
            }
            bets.close();
            getBets.close();

            // --- BATCH PROCESS POT DISTRIBUTION & PAYOUTS ---
            // Group winners by Currency and Tier to calculate Jackpot Splits
            // We need to know COUNT of 1st, 2nd, 3rd winners per currency to split the
            // share
            Map<String, Map<String, Integer>> winnerCounts = new HashMap<>(); // Key: Currency -> Key: Tier -> Count

            for (WinnerInfo w : winners) {
                winnerCounts.putIfAbsent(w.currency, new HashMap<>());
                Map<String, Integer> tiers = winnerCounts.get(w.currency);
                tiers.put(w.tier, tiers.getOrDefault(w.tier, 0) + 1);
            }

            // Calculate Pot Shares
            Map<String, Long> currencyPotShares = new HashMap<>(); // Key: Currency_Tier -> SharePerWinner

            for (String currency : winnerCounts.keySet()) {
                Map<String, Integer> counts = winnerCounts.get(currency);
                String potKey = "4D_" + currency;

                // Priority: 1st > 2nd > 3rd
                if (counts.containsKey("1st Prize")) {
                    long totalShare = GamblingPotManager.distributePot(potKey, 1.0);
                    currencyPotShares.put(currency + "_1st Prize", totalShare / counts.get("1st Prize"));
                } else if (counts.containsKey("2nd Prize")) {
                    long totalShare = GamblingPotManager.distributePot(potKey, 0.60);
                    currencyPotShares.put(currency + "_2nd Prize", totalShare / counts.get("2nd Prize"));
                } else if (counts.containsKey("3rd Prize")) {
                    long totalShare = GamblingPotManager.distributePot(potKey, 0.30);
                    currencyPotShares.put(currency + "_3rd Prize", totalShare / counts.get("3rd Prize"));
                }
            }

            // Apply Rewards
            boolean hasWinner = false;
            for (WinnerInfo w : winners) {
                long potShare = currencyPotShares.getOrDefault(w.currency + "_" + w.tier, 0L);
                long totalPrize = w.basePrize + potShare;
                boolean isJackpot = "1st Prize".equals(w.tier);

                if (isJackpot)
                    hasWinner = true;

                // --- AUTO-CONVERSION ---
                long finalBcoins = 0;
                long finalMesos = totalPrize;
                int prizeItemId = -1;

                if (w.currency.equals("MESO") && totalPrize >= 1_000_000_000L) {
                    finalBcoins = totalPrize / 1_000_000_000L;
                    finalMesos = totalPrize % 1_000_000_000L;
                    prizeItemId = 3020002;
                } else if (w.currency.equals("BCOIN") || w.currency.equals("NXT")) {
                    prizeItemId = 3020002;
                }

                // UPDATE MAIN BET
                try (PreparedStatement update = con.prepareStatement(
                        "UPDATE 4d_bets SET is_winner = 1, prize_item_id = ?, prize_quantity = ? WHERE bet_id = ?")) {
                    update.setInt(1, prizeItemId);
                    update.setLong(2, (finalBcoins > 0) ? finalBcoins : totalPrize);
                    update.setInt(3, w.betId);
                    update.executeUpdate();
                }

                // INSERT REMAINDER
                if (finalBcoins > 0 && finalMesos > 0) {
                    try (PreparedStatement insert = con.prepareStatement(
                            "INSERT INTO 4d_bets (char_id, bet_number, bet_type, draw_date, amount, currency_type, is_ibet, is_winner, prize_item_id, prize_quantity, claimed) "
                                    +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, 1, -1, ?, 0)")) {
                        insert.setInt(1, w.charId);
                        insert.setString(2, w.betNumber);
                        insert.setString(3, w.type);
                        insert.setDate(4, java.sql.Date.valueOf(date));
                        insert.setInt(5, 0); // Visual only
                        insert.setString(6, "MESO");
                        insert.setInt(7, w.isIBet ? 1 : 0);
                        insert.setLong(8, finalMesos);
                        insert.executeUpdate();
                    }
                }

                // BROADCAST
                if (isJackpot) {
                    String playerName = getCharacterNameById(w.charId);
                    if (playerName != null) {
                        jackpotWinners.add(playerName);
                        String msg = "[Merogie 4D] " + playerName + " won 1st Prize! Total: " + totalPrize + " "
                                + w.currency +
                                (potShare > 0 ? " (includes Pot Share!)" : "") + "!";
                        Packet packet = PacketCreator.serverNotice(6, msg);
                        for (World world : Server.getInstance().getWorlds()) {
                            Server.getInstance().broadcastMessage(world.getId(), packet);
                        }
                    }
                }
            }

            getResult.close();

            // --- 3. GAME ANNOUNCEMENT ---
            String resultAnnouncement = "[4D Draw Results] 1st: " + first + " | 2nd: " + second + " | 3rd: " + third +
                    "\r\nStarters: " + String.join(", ", starters) +
                    "\r\nConsolations: " + String.join(", ", consolations);

            if (!hasWinner)
                resultAnnouncement += "\r\nNo 1st Prize winners this round. Better luck next time!";

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
        if (!isIBet)
            return betNum.equals(winNum);
        return sortString(betNum).equals(sortString(winNum));
    }

    private static String sortString(String str) {
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    // ...

    /**
     * Calculates prize amount.
     * Logic: Base Multiplier * Bet Amount (No more ticket count/currency value
     * logic)
     */
    private static long calculatePrize(String betType, String tier, String currency, int amount, String betNumber,
            boolean isIBet) {
        long baseMultiplier = 0;

        // Base payout per 1 unit wagered
        switch (tier) {
            case "1st":
                baseMultiplier = betType.equals("BIG") ? 2000 : 3000;
                break;
            case "2nd":
                baseMultiplier = betType.equals("BIG") ? 1000 : 2000;
                break;
            case "3rd":
                baseMultiplier = betType.equals("BIG") ? 490 : 800;
                break;
            case "Starter":
                baseMultiplier = betType.equals("BIG") ? 250 : 0;
                break;
            case "Consolation":
                baseMultiplier = betType.equals("BIG") ? 60 : 0;
                break;
        }

        if (baseMultiplier == 0)
            return 0;

        // "amount" is now the Raw Amount (e.g., 10000 Mesos), NOT ticket count.
        // Payout = Multiplier * Bet Amount
        long totalPayout = baseMultiplier * amount;

        // NOTE: iBet division removed as requested. Full payout for each winning
        // combination.

        return totalPayout;
    }

    private static class WinnerInfo {
        int betId;
        int charId;
        String betNumber;
        String type;
        // int amount; // Removed unused field
        String currency;
        boolean isIBet;
        long basePrize;
        String tier;

        public WinnerInfo(int betId, int charId, String betNumber, String type, int amount, String currency,
                boolean isIBet, long basePrize, String tier) {
            this.betId = betId;
            this.charId = charId;
            this.betNumber = betNumber;
            this.type = type;
            // this.amount = amount;
            this.currency = currency;
            this.isIBet = isIBet;
            this.basePrize = basePrize;
            this.tier = tier;
        }
    }

    private static void sendDiscordResult(LocalDate date, String first, String second, String third,
            List<String> starters, List<String> consolations, List<String> winners) {
        String webhookUrl = EnvLoader.get("DISCORD_ANNOUNCEMENT_WEBHOOK");
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;

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
        json.append(
                "{\"name\": \"\uD83D\uDD38 Consolations\", \"value\": \"`" + String.join(", ", consolations) + "`\"}");

        if (!winners.isEmpty()) {
            json.append(",");
            String winnerList = String.join(", ", winners);
            json.append("{\"name\": \"\uD83C\uDFC6 Jackpot Winners\", \"value\": \"Congratulations to: **"
                    + DiscordWebhook.escape(winnerList) + "**!\"}");
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
            if (rs.next())
                return rs.getString("name");
        } catch (SQLException e) {
        }
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
        } catch (SQLException e) {
        }
        return null;
    }

    public static List<String> getRecentDrawDates(int limit) {
        List<String> dates = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con
                        .prepareStatement("SELECT draw_date FROM 4d_results ORDER BY draw_date DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                dates.add(rs.getString("draw_date"));
        } catch (SQLException e) {
        }
        return dates;
    }
}