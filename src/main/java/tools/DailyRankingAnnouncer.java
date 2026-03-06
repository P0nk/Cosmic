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
    private final String worldName;
    private final int worldId;

    // Constructor to handle the specific world
    public DailyRankingAnnouncer(String worldName) {
        this.worldName = worldName;
        // Map names to IDs (Bera = 1, Scania = 0 based on actual DB despite config)
        this.worldId = worldName.equalsIgnoreCase("Scania") ? 0 : 1;
    }

    @Override
    public void run() {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.err.println("[DailyRanking] No DISCORD_ANNOUNCEMENT_WEBHOOK defined in env.");
            return;
        }

        try {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"username\": \"").append(worldName).append(" Rankings\",");
            json.append("\"embeds\": [{");
            json.append("\"title\": \"\uD83D\uDCCA Daily Leaderboards - ").append(worldName).append("\",");
            json.append("\"color\": ").append(worldName.equalsIgnoreCase("Scania") ? 3447003 : 16763904).append(","); // Blue
                                                                                                                      // for
                                                                                                                      // Scania,
                                                                                                                      // Gold
                                                                                                                      // for
                                                                                                                      // Bera
            json.append("\"fields\": [");

            // --- DATA SECTIONS ---
            appendField(json, "🏆 Top Overall", getTopPlayersQuery(3), false, false);
            json.append(",");

            appendField(json, "⭐ Top Rebirths", getTopRebirthsQuery(3), false, false);
            json.append(",");

            appendField(json, "⚔️ Top Warriors", getJobQuery(100, 3), true, false);
            json.append(",");

            appendField(json, "🔮 Top Magicians", getJobQuery(200, 3), true, false);
            json.append(",");

            appendField(json, "🏹 Top Bowmen", getJobQuery(300, 3), true, false);
            json.append(",");

            appendField(json, "🗡️ Top Thieves", getJobQuery(400, 3), true, false);
            json.append(",");

            appendField(json, "🏴\u200D☠️ Top Pirates", getJobQuery(500, 3), true, false);
            json.append(",");

            appendField(json, "🛡️ Top Guilds", getTopGuildsQuery(3), true, false);
            json.append(",");

            appendField(json, "📜 Quest Masters", getTopQuestersQuery(3), true, false);
            json.append(",");

            appendField(json, "👥 Total Players Today", getTotalPlayersQuery(), true, false);
            json.append(",");

            appendField(json, "⏳ Most Played Today", getMostPlayedQuery(3), false, true);

            json.append("],");
            json.append("\"footer\": {\"text\": \"Stats reset daily. World: ").append(worldName)
                    .append(" | Banned players excluded.\"}");
            json.append("}]}");

            DiscordWebhook.sendEmbedAsync(webhookUrl, json.toString());
            System.out.println("[DailyRanking] " + worldName + " leaderboards sent to Discord.");

            resetDailyStats();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                PreparedStatement ps = con.prepareStatement(query)) {

            // Set worldId for every query since they all now use it
            ps.setInt(1, worldId);

            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    String name = rs.getString(1);
                    int score = rs.getInt(2);
                    String displayScore;

                    if (isPlaytime) {
                        int hours = score / 60;
                        int minutes = score % 60;
                        displayScore = hours > 0 ? String.format("%dh %dm", hours, minutes)
                                : String.format("%dm", minutes);
                    } else {
                        displayScore = String.valueOf(score);
                    }

                    sb.append(rank).append(". **").append(name).append("** (").append(displayScore).append(")\n");
                    rank++;
                }
            }
            if (sb.length() == 0)
                return "No data yet.";
        } catch (SQLException e) {
            System.err.println("[DailyRanking] SQL Error: " + e.getMessage());
            return "Error fetching data.";
        }
        return sb.toString();
    }

    private void resetDailyStats() {
        // We only reset playtime for characters in THIS world
        String query = "UPDATE characters SET dailyPlaytime = 0 WHERE world = ?";
        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, worldId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- SQL QUERIES (Updated to use 'world = ?') ---

    private String getTopPlayersQuery(int limit) {
        return "SELECT c.name, c.level FROM characters c JOIN accounts a ON c.accountid = a.id " +
                "WHERE c.world = ? AND c.gm = 0 AND a.banned = 0 ORDER BY c.level DESC, c.exp DESC LIMIT " + limit;
    }

    private String getTopRebirthsQuery(int limit) {
        return "SELECT c.name, c.reborns FROM characters c JOIN accounts a ON c.accountid = a.id " +
                "WHERE c.world = ? AND c.gm = 0 AND a.banned = 0 AND c.reborns > 0 ORDER BY c.reborns DESC, c.level DESC LIMIT "
                + limit;
    }

    private String getJobQuery(int jobID, int limit) {
        return "SELECT c.name, c.level FROM characters c JOIN accounts a ON c.accountid = a.id " +
                "WHERE c.world = ? AND c.gm = 0 AND a.banned = 0 AND c.job >= " + jobID + " AND c.job < "
                + (jobID + 100) +
                " ORDER BY c.level DESC, c.exp DESC LIMIT " + limit;
    }

    private String getTopGuildsQuery(int limit) {
        // Assuming guilds table has a world column. If not, we filter by the leader's
        // world.
        return "SELECT g.name, g.GP FROM guilds g JOIN characters c ON g.leader = c.id " +
                "JOIN accounts a ON c.accountid = a.id WHERE c.world = ? AND a.banned = 0 ORDER BY g.GP DESC LIMIT "
                + limit;
    }

    private String getTopQuestersQuery(int limit) {
        return "SELECT c.name, COUNT(q.quest) as total_quests FROM characters c " +
                "JOIN accounts a ON c.accountid = a.id JOIN queststatus q ON c.id = q.characterid " +
                "WHERE c.world = ? AND q.status = 2 AND c.gm = 0 AND a.banned = 0 GROUP BY c.id ORDER BY total_quests DESC LIMIT "
                + limit;
    }

    private String getMostPlayedQuery(int limit) {
        return "SELECT c.name, c.dailyPlaytime FROM characters c JOIN accounts a ON c.accountid = a.id " +
                "WHERE c.world = ? AND c.gm = 0 AND a.banned = 0 AND c.dailyPlaytime > 0 ORDER BY c.dailyPlaytime DESC LIMIT "
                + limit;
    }

    private String getTotalPlayersQuery() {
        return "SELECT 'Players', COUNT(*) FROM characters c JOIN accounts a ON c.accountid = a.id " +
                "WHERE c.world = ? AND c.gm = 0 AND a.banned = 0 AND c.dailyPlaytime > 0";
    }
}