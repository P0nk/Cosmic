package server.subscription;

import tools.DatabaseConnection;

import java.sql.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages player subscriptions purchased via Donor Credits.
 *
 * Benefits (while active):
 * - 1.5x EXP from all sources
 * - 1.5x Meso from monster drops
 * - 5 permanent passive stat points per month (stacking, no WATK/MATK)
 *
 * Pricing (in DC cents, 1 DC = 100 cents):
 * - Monthly: 5 DC (500 cents)
 * - Annual: 50 DC (5000 cents)
 */
public final class SubscriptionManager {

    private SubscriptionManager() {
    }

    // ---- Pricing ----
    public static final int MONTHLY_DC_CENTS = 500; // 5.00 DC
    public static final int ANNUAL_DC_CENTS = 5000; // 50.00 DC

    // Stat points granted per month purchased (stacks permanently)
    public static final int STAT_POINTS_PER_MONTH = 5;

    // Stats that MAY NOT be allocated via subscriptions
    private static final Set<String> BLOCKED_STATS = new HashSet<>(Arrays.asList("watk", "matk"));

    // ---- Public API ----

    /**
     * Returns true if the character has an active (non-expired) subscription.
     */
    public static boolean isSubscribed(int characterId) {
        String sql = "SELECT 1 FROM cosmic.cosmic_subscriptions " +
                "WHERE characterid = ? AND expires_at > NOW() LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[SubscriptionManager] isSubscribed error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Purchases a subscription (or extends it) by deducting DC from the donor
     * ledger.
     *
     * @param accountId   The account's donor ledger ID
     * @param characterId The character buying the sub
     * @param tier        "MONTHLY" or "ANNUAL"
     * @return Result describing success/failure
     */
    public static SubscribeResult subscribe(int accountId, int characterId, String tier) {
        int months;
        int priceCents;
        if ("ANNUAL".equals(tier)) {
            months = 12;
            priceCents = ANNUAL_DC_CENTS;
        } else {
            months = 1;
            priceCents = MONTHLY_DC_CENTS;
        }

        // --- deduct DC atomically ---
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. Check + lock balance
                long bal;
                String selLed = "SELECT dc_balance_cents FROM cosmic.donor_ledger WHERE account_id = ? FOR UPDATE";
                try (PreparedStatement ps = con.prepareStatement(selLed)) {
                    ps.setInt(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return new SubscribeResult(false, "No donor ledger found.");
                        }
                        bal = rs.getLong(1);
                    }
                }

                if (bal < priceCents) {
                    con.rollback();
                    return new SubscribeResult(false, "Insufficient Donor Credits. Need " +
                            formatCents(priceCents) + " DC, have " + formatCents(bal) + " DC.");
                }

                // 2. Deduct balance
                String updLed = "UPDATE cosmic.donor_ledger SET " +
                        "dc_balance_cents = ?, dc_spent_cents_total = dc_spent_cents_total + ?, " +
                        "last_spend_at = NOW() WHERE account_id = ?";
                try (PreparedStatement ps = con.prepareStatement(updLed)) {
                    ps.setLong(1, bal - priceCents);
                    ps.setLong(2, priceCents);
                    ps.setInt(3, accountId);
                    ps.executeUpdate();
                }

                // 3. Log txn
                String insTxn = "INSERT INTO cosmic.donor_txn " +
                        "(account_id, char_id, txn_type, dc_cents_delta, ref) " +
                        "VALUES (?, ?, 'SPEND', ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(insTxn)) {
                    ps.setInt(1, accountId);
                    ps.setInt(2, characterId);
                    ps.setLong(3, -priceCents);
                    ps.setString(4, "SUBSCRIPTION:" + tier);
                    ps.executeUpdate();
                }

                // 4. Upsert subscription row — extend if already exists
                final int m = months;
                String upsert = "INSERT INTO cosmic.cosmic_subscriptions " +
                        "(characterid, tier, started_at, expires_at) " +
                        "VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? MONTH)) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "tier = VALUES(tier), " +
                        "expires_at = DATE_ADD(IF(expires_at > NOW(), expires_at, NOW()), INTERVAL ? MONTH)";
                try (PreparedStatement ps = con.prepareStatement(upsert)) {
                    ps.setInt(1, characterId);
                    ps.setString(2, tier);
                    ps.setInt(3, m); // interval (INSERT)
                    ps.setInt(4, m); // interval (UPDATE)
                    ps.executeUpdate();
                }

                con.commit();
                return new SubscribeResult(true,
                        "Subscribed! (" + tier + ", +" + m + " month(s)). " +
                                "5 stat points will be granted on the 1st of each month while active.");

            } catch (SQLException e) {
                con.rollback();
                System.err.println("[SubscriptionManager] subscribe error: " + e.getMessage());
                return new SubscribeResult(false, "Database error: " + e.getMessage());
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[SubscriptionManager] subscribe connection error: " + e.getMessage());
            return new SubscribeResult(false, "Connection error.");
        }
    }

    /**
     * Allocates one of the character's unspent subscriber stat points.
     *
     * @param characterId character ID
     * @param stat        one of: str, dex, int, luk, speed, jump (NOT watk/matk)
     * @return result message
     */
    public static SubscribeResult allocateStat(client.Character chr, String stat, int amount) {
        int characterId = chr.getId();
        if (amount <= 0) {
            return new SubscribeResult(false, "Amount must be greater than 0.");
        }
        stat = stat.toLowerCase().trim();

        if (BLOCKED_STATS.contains(stat)) {
            return new SubscribeResult(false, "You cannot allocate points to " + stat.toUpperCase() + ".");
        }

        Set<String> allowed = new HashSet<>(Arrays.asList("str", "dex", "int", "luk", "speed", "jump"));
        if (!allowed.contains(stat)) {
            return new SubscribeResult(false, "Unknown stat '" + stat + "'. Choose: STR, DEX, INT, LUK, Speed, Jump.");
        }

        String col = "passive_" + stat; // e.g. passive_str, passive_speed

        String updateCharSql = "UPDATE characters SET " + col + " = " + col + " + ? WHERE id = ?";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. Check available points
                int available;
                String checkSql = "SELECT unspent_stat_points FROM cosmic.cosmic_subscriptions WHERE characterid = ?";
                try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                    ps.setInt(1, characterId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return new SubscribeResult(false, "No subscription found.");
                        }
                        available = rs.getInt("unspent_stat_points");
                    }
                }

                if (available < amount) {
                    con.rollback();
                    return new SubscribeResult(false, "Not enough unspent stat points available (You have " + available
                            + ", tried to use " + amount + ").");
                }

                // 2. Deduct unspent points
                String deductSql = "UPDATE cosmic.cosmic_subscriptions SET unspent_stat_points = unspent_stat_points - ? WHERE characterid = ?";
                try (PreparedStatement ps = con.prepareStatement(deductSql)) {
                    ps.setInt(1, amount);
                    ps.setInt(2, characterId);
                    ps.executeUpdate();
                }

                // 3. Apply to character
                try (PreparedStatement ps = con.prepareStatement(updateCharSql)) {
                    ps.setInt(1, amount);
                    ps.setInt(2, characterId);
                    ps.executeUpdate();
                }

                // 4. Update the character object in RAM so autosave doesn't overwrite it
                switch (stat) {
                    case "str":
                        chr.setPassiveStr(chr.getPassiveStr() + amount);
                        break;
                    case "dex":
                        chr.setPassiveDex(chr.getPassiveDex() + amount);
                        break;
                    case "int":
                        chr.setPassiveInt(chr.getPassiveInt() + amount);
                        break;
                    case "luk":
                        chr.setPassiveLuk(chr.getPassiveLuk() + amount);
                        break;
                    case "speed":
                        chr.setPassiveSpeed(chr.getPassiveSpeed() + amount);
                        break;
                    case "jump":
                        chr.setPassiveJump(chr.getPassiveJump() + amount);
                        break;
                }

                // force the server to recalculate and send the new stats to the client
                chr.equipChanged();

                con.commit();

                String remaining = String.valueOf(available - amount);
                return new SubscribeResult(true,
                        "+" + amount + " " + stat.toUpperCase() + " applied! Points remaining: " + remaining + ".");

            } catch (SQLException e) {
                con.rollback();
                System.err.println("[SubscriptionManager] allocateStat error: " + e.getMessage());
                return new SubscribeResult(false, "Database error: " + e.getMessage());
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[SubscriptionManager] allocateStat connection error: " + e.getMessage());
            return new SubscribeResult(false, "Connection error.");
        }
    }

    /**
     * Returns a map of subscription info for the given character: tier, expiry,
     * days left, unspent points.
     * Returns null if no subscription exists.
     */
    public static java.util.Map<String, Object> getInfo(int characterId) {
        String sql = "SELECT tier, expires_at, unspent_stat_points, accumulated_stat_points " +
                "FROM cosmic.cosmic_subscriptions WHERE characterid = ?";
        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return null;
                java.util.Map<String, Object> info = new java.util.HashMap<>();
                info.put("tier", rs.getString("tier"));
                Timestamp exp = rs.getTimestamp("expires_at");
                info.put("expiresAt", exp == null ? "" : exp.toString());
                long daysLeft = 0;
                if (exp != null) {
                    daysLeft = Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 86_400_000L);
                }
                info.put("daysLeft", daysLeft);
                info.put("unspentPoints", rs.getInt("unspent_stat_points"));
                info.put("totalPoints", rs.getInt("accumulated_stat_points"));
                info.put("active", daysLeft > 0);
                return info;
            }
        } catch (SQLException e) {
            System.err.println("[SubscriptionManager] getInfo error: " + e.getMessage());
            return null;
        }
    }

    // ---- Helpers ----

    private static String formatCents(long cents) {
        long abs = Math.abs(cents);
        long whole = abs / 100;
        long rem = abs % 100;
        return (cents < 0 ? "-" : "") + whole + "." + (rem < 10 ? "0" + rem : rem);
    }

    // ---- Result DTO ----

    public static final class SubscribeResult {
        public final boolean success;
        public final String message;

        public SubscribeResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
