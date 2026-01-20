package tools;

import tools.DatabaseConnection;
import tools.DiscordWebhook;
import tools.EnvLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DailyRankingAnnouncer implements Runnable {

    private final String webhookUrl = EnvLoader.get("DISCORD_ANNOUNCEMENT_WEBHOOK");

    @Override
    public void run() {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.err.println("[DailyRanking] No DISCORD_ANNOUNCEMENT_WEBHOOK defined in env.");
            return;
        }

        try {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"username\": \"Maple Rankings\",");
            json.append("\"embeds\": [{");
            json.append("\"title\": \"\uD83D\uDCCA Daily Server Leaderboards\",");
            json.append("\"color\": 16763904,");
            json.append("\"fields\": [");

            // --- DATA SECTIONS (Now Filtered for Banned = 0) ---

            // 1. Top Overall
            appendField(json, "🏆 Top Overall", getTopPlayersQuery(3), false, false);
            json.append(",");

            // 2. Top Rebirths (New)
            appendField(json, "⭐ Top Rebirths", getTopRebirthsQuery(3), false, false);
            json.append(",");

            // 3. Job Classes
            appendField(json, "⚔️ Top Warriors", getJobQuery(100, 3), true, false);
            json.append(",");

            appendField(json, "🔮 Top Magicians", getJobQuery(200, 3), true, false);
            json.append(",");

            appendField(json, "🏹 Top Bowmen", getJobQuery(300, 3), true, false);
            json.append(",");

            appendField(json, "🗡️ Top Thieves", getJobQuery(400, 3), true, false);
            json.append(",");

            appendField(json, "🏴‍☠️ Top Pirates", getJobQuery(500, 3), true, false);
            json.append(",");

            // 4. Guilds & Quests
            appendField(json, "🛡️ Top Guilds", getTopGuildsQuery(3), true, false);
            json.append(",");

            appendField(json, "📜 Quest Masters", getTopQuestersQuery(3), true, false);
            json.append(",");

            // 5. Playtime (Updated Logic)
            appendField(json, "⏳ Most Played Today", getMostPlayedQuery(3), false, true);

            // --- END DATA SECTIONS ---

            json.append("],");
            json.append("\"footer\": {\"text\": \"Stats are reset daily at midnight server time. Banned players excluded.\"}");
            json.append("}]}");

            DiscordWebhook.sendEmbedAsync(webhookUrl, json.toString());
            System.out.println("[DailyRanking] Leaderboards sent to Discord.");

            resetDailyStats();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Appends a JSON field object to the string builder.
     * @param isPlaytime If true, formats the integer value as Hours/Minutes.
     */
    private void appendField(StringBuilder sb, String title, String query, boolean inline, boolean isPlaytime) {
        sb.append("{");
        sb.append("\"name\": \"").append(title).append("\",");
        String value = fetchData(query, isPlaytime);
        String safeValue = DiscordWebhook.escape(value);
        sb.append("\"value\": \"").append(safeValue).append("\",");
        sb.append("\"inline\": ").append(inline);
        sb.append("}");
    }

    private String fetchData(String query, boolean isPlaytime) {
        StringBuilder sb = new StringBuilder();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            int rank = 1;
            while (rs.next()) {
                String name = rs.getString(1);
                int score = rs.getInt(2);
                String displayScore;

                if (isPlaytime) {
                    // Assuming dailyPlaytime is stored in MINUTES in the DB
                    int hours = score / 60;
                    int minutes = score % 60;
                    if (hours > 0) {
                        displayScore = String.format("%dh %dm", hours, minutes);
                    } else {
                        displayScore = String.format("%dm", minutes);
                    }
                } else {
                    displayScore = String.valueOf(score); // Levels, Rebirths, GP, etc.
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
        // Important: Ensure this runs AFTER the announcement is sent, otherwise the Most Played board will always be empty!
        String query = "UPDATE characters SET dailyPlaytime = 0";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- SQL QUERIES ---

    private String getTopPlayersQuery(int limit) {
        return "SELECT c.name, c.level " +
                "FROM characters c " +
                "JOIN accounts a ON c.accountid = a.id " +
                "WHERE c.gm = 0 AND a.banned = 0 " +
                "ORDER BY c.level DESC, c.exp DESC LIMIT " + limit;
    }

    private String getTopRebirthsQuery(int limit) {
        return "SELECT c.name, c.reborns " +
                "FROM characters c " +
                "JOIN accounts a ON c.accountid = a.id " +
                "WHERE c.gm = 0 AND a.banned = 0 AND c.reborns > 0 " +
                "ORDER BY c.reborns DESC, c.level DESC LIMIT " + limit;
    }

    private String getJobQuery(int jobID, int limit) {
        return "SELECT c.name, c.level " +
                "FROM characters c " +
                "JOIN accounts a ON c.accountid = a.id " +
                "WHERE c.gm = 0 AND a.banned = 0 " +
                "AND c.job >= " + jobID + " AND c.job < " + (jobID + 100) + " " +
                "ORDER BY c.level DESC, c.exp DESC LIMIT " + limit;
    }

    private String getTopGuildsQuery(int limit) {
        return "SELECT g.name, g.GP " +
                "FROM guilds g " +
                "JOIN characters c ON g.leader = c.id " +
                "JOIN accounts a ON c.accountid = a.id " +
                "WHERE a.banned = 0 " +
                "ORDER BY g.GP DESC LIMIT " + limit;
    }

    private String getTopQuestersQuery(int limit) {
        return "SELECT c.name, COUNT(q.quest) as total_quests " +
                "FROM characters c " +
                "JOIN accounts a ON c.accountid = a.id " +
                "JOIN queststatus q ON c.id = q.characterid " +
                "WHERE q.status = 2 AND c.gm = 0 AND a.banned = 0 " +
                "GROUP BY c.id " +
                "ORDER BY total_quests DESC " +
                "LIMIT " + limit;
    }

    private String getMostPlayedQuery(int limit) {
        return "SELECT c.name, c.dailyPlaytime " +
                "FROM characters c " +
                "JOIN accounts a ON c.accountid = a.id " +
                "WHERE c.gm = 0 AND a.banned = 0 AND c.dailyPlaytime > 0 " +
                "ORDER BY c.dailyPlaytime DESC LIMIT " + limit;
    }
}