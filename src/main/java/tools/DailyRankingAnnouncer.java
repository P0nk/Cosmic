package tools;

import tools.DatabaseConnection;
import tools.DiscordWebhook;
import tools.EnvLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DailyRankingAnnouncer implements Runnable {

    // You can hardcode this or fetch from EnvLoader.
    // We fetch here to keep it dynamic.
    private final String webhookUrl = EnvLoader.get("DISCORD_ANNOUNCEMENT_WEBHOOK");

    @Override
    public void run() {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.err.println("[DailyRanking] No DISCORD_ANNOUNCEMENT_WEBHOOK defined in env.");
            return;
        }

        try {
            // 1. Build the JSON Payload
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"username\": \"Maple Rankings\",");
            json.append("\"embeds\": [{");
            json.append("\"title\": \"\uD83D\uDCCA Daily Server Leaderboards\","); // 📊 emoji
            json.append("\"color\": 16763904,"); // Gold Color
            json.append("\"fields\": [");

            // --- DATA SECTIONS ---

            // 1. Top Overall (Level)
            appendField(json, "🏆 Top Overall", getTopPlayersQuery(3), false);
            json.append(",");

            // 2. Top Warriors (Job 100-199)
            appendField(json, "⚔️ Top Warriors", getJobQuery(100, 3), true);
            json.append(",");

            // 3. Top Magicians (Job 200-299)
            appendField(json, "🔮 Top Magicians", getJobQuery(200, 3), true);
            json.append(",");

            // 4. Top Bowmen (Job 300-399)
            appendField(json, "🏹 Top Bowmen", getJobQuery(300, 3), true);
            json.append(",");

            // 5. Top Thieves (Job 400-499)
            appendField(json, "🗡️ Top Thieves", getJobQuery(400, 3), true);
            json.append(",");

            // 6. Top Pirates (Job 500-599)
            appendField(json, "🏴‍☠️ Top Pirates", getJobQuery(500, 3), true);
            json.append(",");

            // 7. Top Guilds (GP)
            appendField(json, "🛡️ Top Guilds", getTopGuildsQuery(3), true);
            json.append(",");

            // 8. Quest Completionists (Count)
            appendField(json, "📜 Quest Masters", getTopQuestersQuery(3), true);
            json.append(",");

            // 9. Most Played Today (Minutes -> Hours)
            // Note: This is the last field, so no comma after this appendField logic
            appendField(json, "⏳ Most Played Today", getMostPlayedQuery(3), false);

            // --- END DATA SECTIONS ---

            json.append("],");
            json.append("\"footer\": {\"text\": \"Stats are reset daily at midnight server time.\"}");
            json.append("}]}");

            // 2. Send to Discord
            DiscordWebhook.sendEmbedAsync(webhookUrl, json.toString());
            System.out.println("[DailyRanking] Leaderboards sent to Discord.");

            // 3. Reset Daily Stats
            resetDailyStats();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendField(StringBuilder sb, String title, String query, boolean inline) {
        sb.append("{");
        sb.append("\"name\": \"").append(title).append("\",");

        // We fetch the data strings (e.g. "1. Name (Lv.200)\n2. Name...")
        String value = fetchData(query);
        // JSON escape the value just in case
        String safeValue = DiscordWebhook.escape(value);

        sb.append("\"value\": \"").append(safeValue).append("\",");
        sb.append("\"inline\": ").append(inline);
        sb.append("}");
    }

    // Generic fetcher that formats: "1. **Name** (Score/Level)"
    private String fetchData(String query) {
        StringBuilder sb = new StringBuilder();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            int rank = 1;
            while (rs.next()) {
                String name = rs.getString(1); // First column is always Name
                int score = rs.getInt(2);      // Second column is the Score (Level, GP, Count, etc)

                String displayScore;

                // Special formatting for Playtime (Minutes -> Time String)
                if (query.contains("dailyPlaytime")) {
                    int hours = score / 60;
                    int minutes = score % 60;
                    displayScore = String.format("%dh %dm", hours, minutes);
                } else {
                    displayScore = String.valueOf(score);
                }

                sb.append(rank).append(". **").append(name).append("** (")
                        .append(displayScore).append(")\n");
                rank++;
            }

            if (sb.length() == 0) return "No data yet.";

        } catch (SQLException e) {
            System.err.println("[DailyRanking] SQL Error: " + e.getMessage());
            return "Error fetching data.";
        }
        return sb.toString();
    }

    private void resetDailyStats() {
        // Resets the dailyPlaytime column for everyone so tomorrow is fresh
        String query = "UPDATE characters SET dailyPlaytime = 0";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.executeUpdate();
            System.out.println("[DailyRanking] Daily playtime stats have been reset.");
        } catch (SQLException e) {
            System.err.println("[DailyRanking] Failed to reset daily stats: " + e.getMessage());
        }
    }

    // --- SQL QUERIES ---

    private String getTopPlayersQuery(int limit) {
        return "SELECT name, level FROM characters WHERE gm = 0 ORDER BY level DESC, exp DESC LIMIT " + limit;
    }

    private String getJobQuery(int jobID, int limit) {
        // Matches job branches (e.g. 100-199 for Warrior)
        return "SELECT name, level FROM characters WHERE gm = 0 AND job >= " + jobID + " AND job < " + (jobID + 100) + " ORDER BY level DESC, exp DESC LIMIT " + limit;
    }

    private String getTopGuildsQuery(int limit) {
        return "SELECT name, GP FROM guilds ORDER BY GP DESC LIMIT " + limit;
    }

    private String getTopQuestersQuery(int limit) {
        // Counts completed quests (status = 2)
        return "SELECT c.name, COUNT(q.quest) as total_quests " +
                "FROM characters c " +
                "JOIN queststatus q ON c.id = q.characterid " +
                "WHERE q.status = 2 AND c.gm = 0 " +
                "GROUP BY c.id " +
                "ORDER BY total_quests DESC " +
                "LIMIT " + limit;
    }

    private String getMostPlayedQuery(int limit) {
        // Fetches only players who played > 0 minutes today
        return "SELECT name, dailyPlaytime FROM characters WHERE gm = 0 AND dailyPlaytime > 0 ORDER BY dailyPlaytime DESC LIMIT " + limit;
    }
}