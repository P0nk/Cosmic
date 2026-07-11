package server.expeditions;

import net.server.Server;
import net.server.world.World;
import tools.DatabaseConnection;
import tools.TimeConversion;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ExpeditionLeaderboard {
    public static String getGlobalLeaderboardStringForBoss(ExpeditionBossLog.BossLogEntry boss) {
        List<LeaderboardRecord> bossLogRecords = getGlobalLeaderboardForBoss(boss);

        StringBuilder leaderBoardString = new StringBuilder("Top 10 Fastest Timings to clear #e#r" + boss.name() + "#n#k:\r\n");

        if (bossLogRecords.isEmpty()) {
            leaderBoardString.append("#rNo available entries at this time.#k");
            return leaderBoardString.toString();
        }

        for (int i = 0; i < bossLogRecords.size(); i++) {
            LeaderboardRecord record = bossLogRecords.get(i);

            leaderBoardString.append((i + 1)).append(". ").append(record.getRecords().stream()
                    .sorted(Comparator.comparingLong(ExpeditionBossLogRecord::getDamageDealt))
                    .map(e -> "#b" + e.getCharacterName() + "#k")
                    .collect(Collectors.joining(", "))).append("\r\n");

            leaderBoardString.append("    Completion Time: #b").append(record.getFormattedCompletionTime()).append("#k on #r").append(record.getFormattedAttemptTime()).append("#k.\r\n");
        }

        return leaderBoardString.toString();
    }

    public static String getPersonalLeaderboardStringForBoss(int characterId, ExpeditionBossLog.BossLogEntry boss) {
        List<LeaderboardRecord> bossLogRecords = getPersonalLeaderboardForBoss(characterId, boss);

        StringBuilder leaderBoardString = new StringBuilder("Your Top 3 Fastest Timings to clear #e#r" + boss.name() + "#n#k:\r\n");

        if (bossLogRecords.isEmpty()) {
            leaderBoardString.append("#rNo available entries at this time.#k");
            return leaderBoardString.toString();
        }

        for (int i = 0; i < bossLogRecords.size(); i++) {
            LeaderboardRecord record = bossLogRecords.get(i);
            leaderBoardString.append((i + 1)).append(". ").append(
                    record.getRecords().stream()
                            .sorted((record1, record2) -> {
                                if (record1.getCharacterId() == characterId) {
                                    return -1;
                                } else if (record2.getCharacterId() == characterId) {
                                    return 1;
                                } else {
                                    return Long.compare(record1.getDamageDealt(), record2.getDamageDealt());
                                }
                            })
                            .map(e -> "#b" + e.getCharacterName() + "#k")
                            .collect(Collectors.joining(", "))
            ).append("\r\n");

            leaderBoardString.append("    Completion Time: #b").append(record.getFormattedCompletionTime()).append("#k on #r").append(record.getFormattedAttemptTime()).append("#k.\r\n");
        }

        return leaderBoardString.toString();
    }

    public static List<LeaderboardRecord> getGlobalLeaderboardForBoss(ExpeditionBossLog.BossLogEntry boss) {
        List<LeaderboardRecord> bossLogRecords = new ArrayList<>();

        String sqlGetTopParties = "SELECT DISTINCT bl.party_uuid, MIN(bl.duration) as min_duration, DATE_FORMAT(MIN(bl.attempttime), '%M %d, %Y') as formattedAttemptTime " +
                "FROM " + getBossLogLeaderboardTable(boss.getIsWeekly()) + " bl " +
                "JOIN mapleroot.characters c ON bl.characterid = c.id " +
                "WHERE bl.bosstype = ? " +
                "GROUP BY bl.party_uuid " +
                "ORDER BY min_duration ASC LIMIT 10";

        String sqlGetPartyMembers = "SELECT bl.*, c.name " +
                "FROM " + getBossLogLeaderboardTable(boss.getIsWeekly()) + " bl " +
                "JOIN mapleroot.characters c ON bl.characterid = c.id " +
                "WHERE bl.party_uuid = ? " +
                "ORDER BY bl.damage DESC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement psTopParties = con.prepareStatement(sqlGetTopParties)) {

            psTopParties.setString(1, boss.name());

            readLeaderboardRecords(boss, bossLogRecords, sqlGetPartyMembers, con, psTopParties);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bossLogRecords;
    }

    public static List<LeaderboardRecord> getPersonalLeaderboardForBoss(int characterId, ExpeditionBossLog.BossLogEntry boss) {
        List<LeaderboardRecord> bossLogRecords = new ArrayList<>();

        String sqlGetTopParties = "SELECT DISTINCT bl.party_uuid, MIN(bl.duration) as min_duration, DATE_FORMAT(MIN(bl.attempttime), '%M %d, %Y') as formattedAttemptTime " +
                "FROM " + getBossLogLeaderboardTable(boss.getIsWeekly()) + " bl " +
                "JOIN mapleroot.characters c ON bl.characterid = c.id " +
                "WHERE bl.characterid = ? AND bl.bosstype = ? " +
                "GROUP BY bl.party_uuid " +
                "ORDER BY min_duration ASC LIMIT 3";

        String sqlGetPartyMembers = "SELECT bl.*, c.name " +
                "FROM " + getBossLogLeaderboardTable(boss.getIsWeekly()) + " bl " +
                "JOIN mapleroot.characters c ON bl.characterid = c.id " +
                "WHERE bl.party_uuid = ? " +
                "ORDER BY bl.damage DESC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement psTopParties = con.prepareStatement(sqlGetTopParties)) {

            psTopParties.setInt(1, characterId);
            psTopParties.setString(2, boss.name());

            readLeaderboardRecords(boss, bossLogRecords, sqlGetPartyMembers, con, psTopParties);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bossLogRecords;
    }

    public static void updateLeaderboardFromExpedition(ExpeditionType type, String partyUUID) {
        ExpeditionBossLog.BossLogEntry boss = ExpeditionBossLog.BossLogEntry.getBossEntryByName(type.name());
        if (boss == null) {
            return;
        }

        List<ExpeditionBossLogRecord> partyRecords = ExpeditionBossLog.getBossLogRecordsForParty(type, partyUUID);
        List<LeaderboardRecord> leaderboardRecords = getGlobalLeaderboardForBoss(boss);

        announceNewRecordIfAny(leaderboardRecords, partyRecords, boss);

        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO " + getBossLogLeaderboardTable(boss.getIsWeekly()) + " (characterid, bosstype, damage, duration, attempttime, party_uuid) VALUES (?, ?, ?, ?, ?, ?)")) {
                for (ExpeditionBossLogRecord record : partyRecords) {
                    ps.setInt(1, record.getCharacterId());
                    ps.setString(2, record.getBossType());
                    ps.setLong(3, record.getDamageDealt());
                    ps.setLong(4, record.getDuration());
                    ps.setTimestamp(5, record.getAttemptTime());
                    ps.setObject(6, record.getPartyUuid());
                    ps.addBatch();
                }

                ps.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String getBossLogLeaderboardTable(boolean isWeekly) {
        return isWeekly ? "bosslog_leaderboard_weekly" : "bosslog_leaderboard_daily";
    }

    private static void readLeaderboardRecords(ExpeditionBossLog.BossLogEntry boss, List<LeaderboardRecord> bossLogRecords, String sqlGetPartyMembers, Connection con, PreparedStatement psTopParties) throws SQLException {
        List<String> topPartyUUIDs = new ArrayList<>();

        try (ResultSet rsTopParties = psTopParties.executeQuery()) {
            while (rsTopParties.next()) {
                String partyUuid = rsTopParties.getString("party_uuid");
                String formattedAttemptTime = rsTopParties.getString("formattedAttemptTime");
                String formattedCompletionTime = TimeConversion.millisecondsToTimeString(rsTopParties.getLong("min_duration"));

                LeaderboardRecord leaderboardRecord = new LeaderboardRecord(partyUuid, formattedAttemptTime, formattedCompletionTime);
                bossLogRecords.add(leaderboardRecord);
                topPartyUUIDs.add(partyUuid);
            }
        }

        for (String partyUuid : topPartyUUIDs) {
            try (PreparedStatement psPartyMembers = con.prepareStatement(sqlGetPartyMembers)) {
                psPartyMembers.setString(1, partyUuid);

                try (ResultSet rsPartyMembers = psPartyMembers.executeQuery()) {
                    for (LeaderboardRecord leaderboardRecord : bossLogRecords) {
                        if (leaderboardRecord.getPartyUuid().equals(partyUuid)) {
                            while (rsPartyMembers.next()) {
                                int id = rsPartyMembers.getInt("id");
                                int characterId = rsPartyMembers.getInt("characterid");
                                long duration = rsPartyMembers.getLong("duration");
                                long damageDealt = rsPartyMembers.getLong("damage");
                                // boolean complete = rsPartyMembers.getBoolean("bl.complete");
                                Timestamp timestamp = rsPartyMembers.getTimestamp("attempttime");
                                String characterName = rsPartyMembers.getString("name");

                                ExpeditionBossLogRecord record = new ExpeditionBossLogRecord(id, characterId, characterName, boss.name(), timestamp, damageDealt, duration, partyUuid, true);
                                leaderboardRecord.addRecord(record);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void announceNewRecordIfAny(List<LeaderboardRecord> leaderboardRecords, List<ExpeditionBossLogRecord> partyRecords, ExpeditionBossLog.BossLogEntry boss) {
        if (leaderboardRecords.isEmpty()) {
            Server server = Server.getInstance();
            String characterNamesString = getCharacterNameListString(partyRecords);

            for (World world : server.getWorlds()) {
                world.dropMessage(6, "Congratulations to " + characterNamesString + " for achieving the fastest clear time of " + TimeConversion.millisecondsToTimeString(partyRecords.get(0).getDuration()) + " on " + boss.name() + "!");
            }
        } else {
            long shortestTime = leaderboardRecords.get(0).getRecords().get(0).getDuration();
            if (partyRecords.get(0).getDuration() < shortestTime) {
                Server server = Server.getInstance();
                String characterNamesString = getCharacterNameListString(partyRecords);

                for (World world : server.getWorlds()) {
                    world.dropMessage(6, "Congratulations to " + characterNamesString + " for achieving the fastest clear time of " + TimeConversion.millisecondsToTimeString(partyRecords.get(0).getDuration()) + " on " + boss.name() + "!");
                }
            }
        }
    }

    private static String getCharacterNameListString(List<ExpeditionBossLogRecord> partyRecords) {
        List<String> characterNames = partyRecords.stream()
                .map(ExpeditionBossLogRecord::getCharacterName)
                .toList(); // Java 16 and later

        if (characterNames.isEmpty()) {
            return ""; // Handle empty case, return an empty string or a suitable message.
        }

        if (characterNames.size() == 1) {
            return characterNames.get(0); // If there's only one character, return it directly.
        }

        // Join all but the last name with commas, and add "and" before the last name.
        return characterNames.stream()
                .limit(characterNames.size() - 1)
                .collect(Collectors.joining(", "))
                + " and " + characterNames.get(characterNames.size() - 1);
    }

}
