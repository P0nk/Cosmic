/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package server.expeditions;

import client.Client;
import config.YamlConfig;
import net.server.Server;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.*;
import java.util.*;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;

/**
 * @author Conrad
 * @author Ronan
 */
public class ExpeditionBossLog {

    public enum BossLogEntry {
        ZAKUM(1, 5, 25, 24, false),
        HORNTAIL(2,6, 20, 24, false),
        PINKBEAN(3, 7, 20, 24, false),
        SCARGA(4, 1, 10, 24, false),
        PAPULATUS(5, 2, 30, 24, false),
        VONLEON(6, 8, 18, 24, false),
        CYGNUS(7, 9, 16, 24, false),
        WILLSPIDER(8, 11, 8, 24, false),
        VERUS(9, 12, 8, 24, false),
        DARKNELL(10, 13, 8, 24, false),
        KREXEL(11, 3, 20, 24, false),
        CASTELLAN(12, 4, 20, 24, false),
        LUCID(13, 10, 8, 24, false),
        NLUCID(14, 10, 12, 24, false),
        NWILLSPIDER(15, 10, 12, 24, false),
        NVERUS(16, 10, 12, 24, false),
        NDARKNELL(17, 10, 12, 24, false),
        MAGNUS(18, 10, 8, 24, false);

        private final int entries;
        private final int ordinal;
        private final int timeLength;
        private final int minChannel;
        private final int maxChannel;
        private final boolean week;
        private final int index;

        BossLogEntry(int index, int ordinal, int entries, int timeLength, boolean week) {
            this(index, ordinal, entries, 0, Integer.MAX_VALUE, timeLength, week);
        }

        BossLogEntry(int index, int ordinal, int entries, int minChannel, int maxChannel, int timeLength, boolean week) {
            this.index = index;
            this.ordinal = ordinal;
            this.entries = entries;
            this.minChannel = minChannel;
            this.maxChannel = maxChannel;
            this.timeLength = timeLength;
            this.week = week;
        }

        public int getIndex() { return this.index; }
        public int getEntries() { return this.entries; }
        public int getOrdinal() { return this.ordinal; }
        public boolean getIsWeekly() { return this.week; }

        private static List<Pair<Timestamp, BossLogEntry>> getBossLogResetTimestamps(Calendar timeNow, boolean week) {
            List<Pair<Timestamp, BossLogEntry>> resetTimestamps = new LinkedList<>();

            Timestamp ts = new Timestamp(timeNow.getTime().getTime());  // reset all table entries actually, thanks Conrad
            for (BossLogEntry b : BossLogEntry.values()) {
                if (b.week == week) {
                    resetTimestamps.add(new Pair<>(ts, b));
                }
            }

            return resetTimestamps;
        }

        public static BossLogEntry getBossEntryByName(String name) {
            for (BossLogEntry b : BossLogEntry.values()) {
                if (name.contentEquals(b.name())) {
                    return b;
                }
            }

            return null;
        }

    }

    public static void resetBossLogTable() {
        /*
        Boss logs resets 12am, weekly thursday 12AM - thanks Smitty Werbenjagermanjensen (superadlez) - https://www.reddit.com/r/Maplestory/comments/61tiup/about_reset_time/
        */

        Calendar thursday = Calendar.getInstance();
        thursday.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
        thursday.set(Calendar.HOUR, 0);
        thursday.set(Calendar.MINUTE, 0);
        thursday.set(Calendar.SECOND, 0);

        Calendar now = Calendar.getInstance();

        long weekLength = DAYS.toMillis(7);
        long halfDayLength = HOURS.toMillis(12);

        long deltaTime = now.getTime().getTime() - thursday.getTime().getTime();    // 2x time: get Date into millis
        deltaTime += halfDayLength;
        deltaTime %= weekLength;
        deltaTime -= halfDayLength;

        if (deltaTime < halfDayLength) {
            ExpeditionBossLog.resetBossLogTable(true, thursday);
        }

        now.set(Calendar.HOUR, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);

        ExpeditionBossLog.resetBossLogTable(false, now);
    }

    private static void resetBossLogTable(boolean week, Calendar c) {
        List<Pair<Timestamp, BossLogEntry>> resetTimestamps = BossLogEntry.getBossLogResetTimestamps(c, week);

        try (Connection con = DatabaseConnection.getConnection()) {
            for (Pair<Timestamp, BossLogEntry> p : resetTimestamps) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM " + getBossLogTable(week) + " WHERE attempttime <= ? AND bosstype LIKE ?")) {
                    ps.setTimestamp(1, p.getLeft());
                    ps.setString(2, p.getRight().name());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String getBossLogTable(boolean week) {
        return week ? "bosslog_weekly" : "bosslog_daily";
    }


    public static int getPlayerEntryCount(int cid, BossLogEntry boss) {
        int ret_count = 0;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM " + getBossLogTable(boss.week) + " WHERE characterid = ? AND bosstype LIKE ?")) {
            ps.setInt(1, cid);
            ps.setString(2, boss.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret_count = rs.getInt(1);
                } else {
                    ret_count = -1;
                }
            }
            return ret_count;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }



    public static int countPlayerEntries(int cid, BossLogEntry boss) {
        int ret_count = 0;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM " + getBossLogTable(boss.week) + " WHERE characterid = ? AND bosstype LIKE ?")) {
            ps.setInt(1, cid);
            ps.setString(2, boss.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret_count = rs.getInt(1);
                } else {
                    ret_count = -1;
                }
            }
            return ret_count;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void insertPlayerEntry(int cid, BossLogEntry boss) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO " + getBossLogTable(boss.week) + " (characterid, bosstype) VALUES (?,?)")) {
            ps.setInt(1, cid);
            ps.setString(2, boss.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean attemptBoss(int cid, int channel, Expedition exped, boolean log) {
        if (!YamlConfig.config.server.USE_ENABLE_DAILY_EXPEDITIONS) {
            return true;
        }

        BossLogEntry boss = BossLogEntry.getBossEntryByName(exped.getType().name());
        if (boss == null) {
            return true;
        }

        if (channel < boss.minChannel || channel > boss.maxChannel) {
            return false;
        }

        if (countPlayerEntries(cid, boss) >= boss.entries) {
            return false;
        }

        return true;
    }

    public static Map<BossLogEntry, BossLogData> getWeeklyBossEntries(int cid, boolean showZeros) {
        return getBossEntries(cid, showZeros, true);
    }

    public static Map<BossLogEntry, BossLogData> getBossEntries(int cid, boolean showZeros, boolean weekly) {
        Map<BossLogEntry, BossLogData> bossData = new LinkedHashMap<>();

        // Initialize all types in order based on ordinal
        for (BossLogEntry e : Arrays.stream(BossLogEntry.values()).sorted(Comparator.comparingInt(BossLogEntry::getOrdinal)).toList()) {
            bossData.put(e, new BossLogData());
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT bosstype, complete FROM " + getBossLogTable(weekly) + " WHERE characterid = ?" )) {
                ps.setInt(1, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String boss = rs.getString("bosstype");
                        boolean completed = rs.getBoolean("complete");

                        BossLogData data = bossData.get(BossLogEntry.getBossEntryByName(boss));
                        if (data != null) {
                            data.updateAttempts(completed);
                            bossData.put(BossLogEntry.getBossEntryByName(boss), data);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // remove entries if all have 0 count
        if (!showZeros) {
            int zeroCount = 0;
            for (Map.Entry<BossLogEntry, BossLogData> entry : bossData.entrySet()) {
                if (entry.getValue().getAttempts() == 0) {
                    zeroCount += 1;
                }
            }

            if (zeroCount == bossData.size())
                return new LinkedHashMap<>();
        }

        return bossData;
    }

    // Count successful boss encounters for use of reward limitation
    public static int countPlayerEntriesByHwid(int cid, BossLogEntry boss) {
        int count;
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM " + getBossLogTable(boss.week) + " WHERE " +
                            "characterid IN (SELECT c.id FROM characters c JOIN accounts a ON c.accountid=a.id WHERE " +
                            "a.hwid = (SELECT a1.hwid FROM accounts a1 JOIN characters c1 ON c1.accountid=a1.id WHERE " +
                            "c1.id=? and bosstype LIKE ?)) AND completed=1")) {
                ps.setInt(1, cid);
                ps.setString(2, boss.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        count = rs.getInt(1);
                    } else {
                        count = -1;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return count;
    }

    public static void removePlayerEntry(int cid, BossLogEntry boss, int removeCount) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM " + getBossLogTable(boss.week) + " WHERE characterid = ? and bosstype LIKE ? LIMIT ?");
            ps.setInt(1, cid);
            ps.setString(2, boss.name());
            ps.setInt(3, removeCount);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean reachedBossRewardLimit(int cid, ExpeditionType type) {
        BossLogEntry boss = BossLogEntry.getBossEntryByName(type.name());

        return boss != null && countPlayerEntriesByHwid(cid, boss) > YamlConfig.config.server.EXPEDITION_HWID_LIMIT * boss.entries;
    }

    public static void setExpeditionCompleted(Client c, ExpeditionType type) {
        setExpeditionCompleted(c.getPlayer().getId(), type);
    }

    public static void setExpeditionCompleted(Client c, ExpeditionType type, long duration) {
        setExpeditionCompleted(c.getPlayer().getId(), type, duration, 0, null);
    }

    public static void setExpeditionCompleted(Client c, ExpeditionType type, long duration, long damageDealt) {
        setExpeditionCompleted(c.getPlayer().getId(), type, duration, damageDealt, null);
    }

    public static void setExpeditionCompleted(Client c, ExpeditionType type, long duration, long damageDealt, String partyUUID) {
        setExpeditionCompleted(c.getPlayer().getId(), type, duration, damageDealt, partyUUID);
    }

    public static void setExpeditionCompleted(int id, ExpeditionType type) {
        setExpeditionCompleted(id, type, 0, 0, null);
    }

    public static void setExpeditionCompleted(int id, ExpeditionType type, long duration, long damageDealt, String partyUUID) {
        BossLogEntry boss = BossLogEntry.getBossEntryByName(type.name());

        if (boss != null) {
            try (Connection con = DatabaseConnection.getConnection()) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE " + getBossLogTable(boss.week) + " SET complete=1, duration=?, damage=?, party_uuid=? WHERE " +
                                "characterid=? and bosstype LIKE ? ORDER BY attempttime DESC LIMIT 1")) {
                    ps.setLong(1, duration);
                    ps.setLong(2, damageDealt);
                    ps.setString(3, partyUUID);
                    ps.setInt(4, id);
                    ps.setString(5, boss.name());
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static List<ExpeditionBossLogRecord> getBossLogRecordsForParty(ExpeditionType type, String partyUUID) {
        BossLogEntry boss = BossLogEntry.getBossEntryByName(type.name());
        if (boss == null) {
            return new ArrayList<>();
        }

        List<ExpeditionBossLogRecord> bossLogRecords = new ArrayList<>();

        String sql = "SELECT bl.*, c.name AS characterName " +
                "FROM " + getBossLogTable(boss.week) + " bl " +
                "JOIN mapleroot.characters c ON bl.characterid = c.id " +
                "WHERE bl.party_uuid = ? AND bl.bosstype LIKE ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, partyUUID);
            ps.setString(2, boss.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("bl.id");
                    int characterId = rs.getInt("bl.characterid");
                    String characterName = rs.getString("characterName");
                    long duration = rs.getLong("bl.duration");
                    long damageDealt = rs.getLong("bl.damage");
                    boolean complete = rs.getBoolean("bl.complete");
                    Timestamp timestamp = rs.getTimestamp("bl.attempttime");

                    ExpeditionBossLogRecord record = new ExpeditionBossLogRecord(id, characterId, characterName, boss.name(), timestamp, damageDealt, duration, partyUUID, complete);
                    bossLogRecords.add(record);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bossLogRecords;
    }
}
