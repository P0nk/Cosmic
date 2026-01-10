package server.voting;

import client.Character;
import scripting.AbstractPlayerInteraction;
import tools.DatabaseConnection;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class VoteManager {

    public static class RewardSummary {
        public int count = 0;
        public int lifetimeVotes = 0;
        public long totalNx = 0;
        public long totalMeso = 0;
        public int totalLeaves = 0;
        public int totalElixirs = 0;
        public int projectedStreak = 0;
    }

    /**
     * Links votes to the current player using a 3-Priority System:
     * 1. Account Username Match (Highest Trust)
     * 2. Character Name Match (High Trust)
     * 3. IP Address Match (Fallback / Low Trust)
     */
    public static void linkPendingVotes(Character chr) {
        new GTopVoteTask().run();
        int accId = chr.getAccountID();
        String accountName = chr.getClient().getAccountName();
        String sessionIp = chr.getClient().getRemoteAddress();
        if (sessionIp != null && sessionIp.contains("/")) {
            sessionIp = sessionIp.split(":")[0].replace("/", "");
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE vote_records SET account_id = ? WHERE username = ? AND claimed = 0 AND account_id IS NULL")) {
                ps.setInt(1, accId); ps.setString(2, accountName); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE vote_records v SET v.account_id = ? WHERE v.claimed = 0 AND v.account_id IS NULL AND v.username IN (SELECT name FROM characters WHERE accountid = ?)")) {
                ps.setInt(1, accId); ps.setInt(2, accId); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE vote_records SET account_id = ? WHERE ip_address = ? AND claimed = 0 AND account_id IS NULL")) {
                ps.setInt(1, accId); ps.setString(2, sessionIp); ps.executeUpdate();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static RewardSummary calculate(Character chr) {
        RewardSummary summary = new RewardSummary();
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM vote_records WHERE account_id = ? AND claimed = 1")) {
                ps.setInt(1, chr.getAccountID());
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) summary.lifetimeVotes = rs.getInt(1); }
            }
            int currentStreak = 0;
            LocalDate lastDate = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT vote_streak, last_vote_date FROM accounts WHERE id = ?")) {
                ps.setInt(1, chr.getAccountID());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentStreak = rs.getInt("vote_streak");
                        Date sqlDate = rs.getDate("last_vote_date");
                        if (sqlDate != null) lastDate = sqlDate.toLocalDate();
                    }
                }
            }
            List<LocalDate> pendingVotes = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT vote_time FROM vote_records WHERE account_id = ? AND claimed = 0 ORDER BY vote_time ASC")) {
                ps.setInt(1, chr.getAccountID());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp ts = rs.getTimestamp("vote_time");
                        if (ts != null) pendingVotes.add(ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    }
                }
            }
            summary.count = pendingVotes.size();
            for (LocalDate voteDate : pendingVotes) {
                if (lastDate == null) currentStreak = 1;
                else {
                    if (voteDate.isEqual(lastDate.plusDays(1))) currentStreak++;
                    else if (voteDate.isAfter(lastDate.plusDays(1))) currentStreak = 1;
                }
                summary.totalNx += calcNx(currentStreak);
                summary.totalMeso += calcMeso(currentStreak);
                summary.totalLeaves += calcLeaves(currentStreak);
                summary.totalElixirs += calcElixirs(currentStreak);
                lastDate = voteDate;
            }
            summary.projectedStreak = currentStreak;
        } catch (SQLException e) { e.printStackTrace(); }
        return summary;
    }

    public static boolean hasClaimedToday(int accId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT last_vote_date FROM accounts WHERE id = ?")) {
            ps.setInt(1, accId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date lastDate = rs.getDate("last_vote_date");
                    if (lastDate != null) {
                        LocalDate lastClaim = lastDate.toLocalDate();
                        // UPDATED: Check against GMT (which aligns with 8AM SGT)
                        LocalDate today = LocalDate.now(ZoneId.of("GMT"));
                        return lastClaim.isEqual(today);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * Retrieves the last 10 successful claims for the transaction history log.
     */
    public static String getHistory(int accId) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT claimed_date FROM vote_records WHERE account_id = ? AND claimed = 1 ORDER BY claimed_date DESC LIMIT 10")) {
            ps.setInt(1, accId);
            try (ResultSet rs = ps.executeQuery()) {
                int i = 1;
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("claimed_date");
                    String dateStr = (ts != null) ? sdf.format(ts) : "Unknown";
                    sb.append(i).append(". ").append(dateStr).append(" - #gClaimed#k\r\n");
                    i++;
                }
            }
            if (sb.length() == 0) return "No recent transaction history found.";
        } catch (SQLException e) { return "Error retrieving history."; }
        return sb.toString();
    }

    public static String claim(AbstractPlayerInteraction iter, int rewardType) {
        Character chr = iter.getPlayer();

        // 1. LIMIT REMOVED: We no longer check hasClaimedToday() to block.
        // Users can claim as long as they have points.

        RewardSummary summary = calculate(chr);
        if (summary.count <= 0) return "You have no votes to claim.";

        if (rewardType == 2 && !chr.canHold(4001126, summary.totalLeaves)) return "Inventory Full (Etc).";
        if (rewardType == 3 && !chr.canHold(2000005, summary.totalElixirs)) return "Inventory Full (Use).";

        if (rewardType == 0) chr.getCashShop().gainCash(1, (int) summary.totalNx);
        else if (rewardType == 1) chr.gainMeso((int) summary.totalMeso, true);
        else if (rewardType == 2) iter.gainItem(4001126, summary.totalLeaves);
        else if (rewardType == 3) iter.gainItem(2000005, summary.totalElixirs);

        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE vote_records SET claimed = 1, claimed_date = NOW() WHERE account_id = ? AND claimed = 0")) {
                ps.setInt(1, chr.getAccountID()); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET vote_streak = ?, last_vote_date = NOW() WHERE id = ?")) {
                ps.setInt(1, summary.projectedStreak); ps.setInt(2, chr.getAccountID()); ps.executeUpdate();
            }
        } catch (SQLException e) { return "Error saving data."; }
        return "Success";
    }

    private static int calcNx(int s) { int b = (s>0)?(s-1)*5000:0; return Math.min(10000+b, 30000); }
    private static int calcMeso(int s) { int b = (s>0)?(s-1)*5000000:0; return Math.min(1000000+b, 30000000); }
    private static int calcLeaves(int s) { int b = (s>0)?(s-1)*500:0; return Math.min(350+b, 2100); }
    private static int calcElixirs(int s) { int b = (s>0)?(s-1)*300:0; return Math.min(300+b, 1800); }
}