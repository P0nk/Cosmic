package server.donor;

import client.Client;
import client.Character;
import client.inventory.Pet;
import client.inventory.manipulator.InventoryManipulator;
import constants.inventory.ItemConstants;
import tools.DatabaseConnection;
import client.SkillFactory;
import net.server.Server;
import net.server.channel.Channel;
import tools.PacketCreator;

import java.sql.*;
import java.util.*;

public final class DonorCreditManager {

    private DonorCreditManager() {}

    // ====== CONFIG ======
    // 10.00 DC bonus per $50 lifetime donated
    private static final int MILESTONE_SGD_CENTS = 5000;          // $50.00
    private static final int BONUS_DC_CENTS_PER_MILESTONE = 1000; // 10.00 DC

    // ====== Key Alerts (rate-limited) ======
    private static volatile long lastDbAlertAt = 0L;
    private static volatile long lastLogicAlertAt = 0L;
    private static final long ALERT_COOLDOWN_MS = 60_000L;

    private static void alertDb(String msg, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastDbAlertAt >= ALERT_COOLDOWN_MS) {
            lastDbAlertAt = now;
            System.err.println("[DonorCreditManager][DB] " + msg + (t != null ? " err=" + t.getMessage() : ""));
        }
    }

    private static void alertLogic(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastLogicAlertAt >= ALERT_COOLDOWN_MS) {
            lastLogicAlertAt = now;
            System.err.println("[DonorCreditManager][ALERT] " + msg);
        }
    }

    // ====== Status DTO via Map for easy JS usage ======

    public static Map<String, Object> getStatusByAccountId(int accountId) {
        Map<String, Object> m = new HashMap<>();
        ensureLedgerRow(accountId);

        String sql = "SELECT lifetime_cents, dc_balance_cents, dc_earned_cents_total, dc_spent_cents_total, last_credit_at, last_spend_at " +
                "FROM cosmic.donor_ledger WHERE account_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long lifetime = rs.getLong("lifetime_cents");
                    long bal = rs.getLong("dc_balance_cents");
                    long earned = rs.getLong("dc_earned_cents_total");
                    long spent = rs.getLong("dc_spent_cents_total");

                    m.put("accountId", accountId);
                    m.put("lifetimeCents", lifetime);
                    m.put("balanceCents", bal);
                    m.put("earnedCents", earned);
                    m.put("spentCents", spent);
                    m.put("milestones", (int) (lifetime / MILESTONE_SGD_CENTS));

                    long nextMilestoneAt = ((lifetime / MILESTONE_SGD_CENTS) + 1L) * MILESTONE_SGD_CENTS;
                    long toNext = Math.max(0, nextMilestoneAt - lifetime);
                    m.put("toNextMilestoneCents", toNext);
                    m.put("milestoneBonusCentsPer", BONUS_DC_CENTS_PER_MILESTONE);

                    Timestamp lc = rs.getTimestamp("last_credit_at");
                    Timestamp ls = rs.getTimestamp("last_spend_at");
                    m.put("lastCreditAt", lc == null ? "" : lc.toString());
                    m.put("lastSpendAt", ls == null ? "" : ls.toString());
                } else {
                    // Should not happen if ensureLedgerRow works
                    alertLogic("donor_ledger row missing after ensureLedgerRow. accountId=" + accountId);
                }
            }

        } catch (SQLException e) {
            alertDb("getStatusByAccountId failed. accountId=" + accountId, e);
        }

        return m;
    }

    public static List<Map<String, Object>> getRecentTxns(int accountId, int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, txn_type, dc_cents_delta, donation_amount_cents, milestone_bonus_cents, ref, created_at " +
                "FROM cosmic.donor_txn WHERE account_id = ? ORDER BY id DESC LIMIT ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, accountId);
            ps.setInt(2, Math.max(1, Math.min(limit, 20)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> r = new HashMap<>();
                    r.put("id", rs.getLong("id"));
                    r.put("type", rs.getString("txn_type"));
                    r.put("dcDeltaCents", rs.getLong("dc_cents_delta"));
                    r.put("donationCents", rs.getInt("donation_amount_cents"));
                    r.put("bonusCents", rs.getInt("milestone_bonus_cents"));
                    r.put("ref", rs.getString("ref"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    r.put("at", ts == null ? "" : ts.toString());
                    list.add(r);
                }
            }

        } catch (SQLException e) {
            alertDb("getRecentTxns failed. accountId=" + accountId + " limit=" + limit, e);
        }

        return list;
    }

    // ====== Shop Catalog ======

    public static List<String> getCategories() {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM cosmic.donor_shop_items WHERE is_enabled = 1 ORDER BY category ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) cats.add(rs.getString(1));

        } catch (SQLException e) {
            alertDb("getCategories failed.", e);
        }
        return cats;
    }

    public static List<Map<String, Object>> getItemsByCategory(String category) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, itemid, quantity, price_dc_cents, stock, sort_order, notes, period " +
                "FROM cosmic.donor_shop_items WHERE is_enabled = 1 AND category = ? " +
                "ORDER BY sort_order ASC, id ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, category);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> r = new HashMap<>();
                    r.put("id", rs.getInt("id"));
                    r.put("itemid", rs.getInt("itemid"));
                    r.put("qty", rs.getInt("quantity"));
                    r.put("priceCents", rs.getInt("price_dc_cents"));

                    int stock = rs.getInt("stock");
                    r.put("stock", rs.wasNull() ? null : stock);

                    r.put("notes", rs.getString("notes"));
                    r.put("period", rs.getInt("period"));
                    list.add(r);
                }
            }

        } catch (SQLException e) {
            alertDb("getItemsByCategory failed. category=" + category, e);
        }
        return list;
    }

    // ====== Admin Credit Donation ======

    /**
     * Credits DC based on donation amount in SGD cents (1 SGD = 1 DC).
     * Also applies milestone bonus: +10.00 DC per each new $50 lifetime milestone crossed.
     */
    public static CreditResult creditDonation(int accountId, int donationAmountCents, String adminRef) throws SQLException {
        if (donationAmountCents <= 0) throw new SQLException("Donation amount must be > 0");

        ensureLedgerRow(accountId);

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                long oldLifetime;
                long newLifetime;
                long bonusCents;
                long baseCents = donationAmountCents;

                long oldBal;

                // Lock ledger row
                String sel = "SELECT lifetime_cents, dc_balance_cents FROM cosmic.donor_ledger WHERE account_id = ? FOR UPDATE";
                try (PreparedStatement ps = con.prepareStatement(sel)) {
                    ps.setInt(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Ledger row missing for account_id=" + accountId);
                        oldLifetime = rs.getLong("lifetime_cents");
                        oldBal = rs.getLong("dc_balance_cents");
                    }
                }

                newLifetime = oldLifetime + donationAmountCents;

                long milestonesBefore = oldLifetime / MILESTONE_SGD_CENTS;
                long milestonesAfter = newLifetime / MILESTONE_SGD_CENTS;
                long newMilestones = Math.max(0, milestonesAfter - milestonesBefore);

                bonusCents = newMilestones * BONUS_DC_CENTS_PER_MILESTONE;

                long creditedTotal = baseCents + bonusCents;
                long newBal = oldBal + creditedTotal;

                // Update ledger
                String upd = "UPDATE cosmic.donor_ledger SET " +
                        "lifetime_cents = ?, " +
                        "dc_balance_cents = ?, " +
                        "dc_earned_cents_total = dc_earned_cents_total + ?, " +
                        "last_credit_at = NOW() " +
                        "WHERE account_id = ?";

                try (PreparedStatement ps = con.prepareStatement(upd)) {
                    ps.setLong(1, newLifetime);
                    ps.setLong(2, newBal);
                    ps.setLong(3, creditedTotal);
                    ps.setInt(4, accountId);
                    int updated = ps.executeUpdate();
                    if (updated <= 0) {
                        alertLogic("creditDonation updated 0 rows. accountId=" + accountId);
                        throw new SQLException("Failed to update ledger.");
                    }
                }

                // Insert txn
                String ins = "INSERT INTO cosmic.donor_txn (account_id, char_id, txn_type, dc_cents_delta, donation_amount_cents, milestone_bonus_cents, ref) " +
                        "VALUES (?, NULL, 'CREDIT', ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(ins)) {
                    ps.setInt(1, accountId);
                    ps.setLong(2, creditedTotal);
                    ps.setInt(3, donationAmountCents);
                    ps.setInt(4, (int) bonusCents);
                    ps.setString(5, adminRef);
                    ps.executeUpdate();
                }

                con.commit();
                return new CreditResult(donationAmountCents, (int) bonusCents, creditedTotal, newBal, newLifetime);

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // ====== Purchase (Atomic spend + item grant) ======

    /** Single-buy wrapper (keeps old API for scripts that call 2-arg version). */
    public static PurchaseResult buyFromShop(Client c, int shopItemId) throws SQLException {
        return buyFromShop(c, shopItemId, 1);
    }

    public static PurchaseResult buyFromShop(Client c, int shopItemId, int times) throws SQLException {
        if (times <= 0) throw new SQLException("Invalid quantity.");
        if (times > 10000) throw new SQLException("Quantity too large.");

        Character chr = c.getPlayer();
        int accountId = c.getAccID();

        ensureLedgerRow(accountId);

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Lock ledger
                long bal;
                String selLed = "SELECT dc_balance_cents FROM cosmic.donor_ledger WHERE account_id = ? FOR UPDATE";
                try (PreparedStatement ps = con.prepareStatement(selLed)) {
                    ps.setInt(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Ledger row missing for account_id=" + accountId);
                        bal = rs.getLong(1);
                    }
                }

                int itemId, baseQty, priceCents, period;
                Integer stock = null;
                String cat;
                String notes;

                String selItem = "SELECT category, itemid, quantity, price_dc_cents, stock, notes, period " +
                        "FROM cosmic.donor_shop_items WHERE id = ? AND is_enabled = 1 FOR UPDATE";

                try (PreparedStatement ps = con.prepareStatement(selItem)) {
                    ps.setInt(1, shopItemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Item not found or disabled.");
                        cat = rs.getString("category");
                        itemId = rs.getInt("itemid");
                        baseQty = rs.getInt("quantity");
                        priceCents = rs.getInt("price_dc_cents");
                        period = rs.getInt("period");

                        int s = rs.getInt("stock");
                        if (!rs.wasNull()) stock = s;
                        notes = rs.getString("notes");
                    }
                }

                if (priceCents <= 0) throw new SQLException("Invalid price.");
                if (baseQty <= 0) throw new SQLException("Invalid quantity.");
                if (stock != null && stock < times) throw new SQLException("Out of stock.");

                long totalPrice = (long) priceCents * (long) times;
                if (bal < totalPrice) throw new SQLException("Insufficient Donor Credits.");

                long totalQtyLong = (long) baseQty * (long) times;
                if (totalQtyLong <= 0 || totalQtyLong > Short.MAX_VALUE) {
                    // InventoryManipulator.addById uses short quantity in grantItem below
                    throw new SQLException("Quantity overflow.");
                }
                int totalQty = (int) totalQtyLong;

                if (!InventoryManipulator.checkSpace(c, itemId, totalQty, "")) {
                    throw new SQLException("Not enough inventory space.");
                }

                long newBal = bal - totalPrice;

                // Deduct balance
                String updLed = "UPDATE cosmic.donor_ledger SET " +
                        "dc_balance_cents = ?, " +
                        "dc_spent_cents_total = dc_spent_cents_total + ?, " +
                        "last_spend_at = NOW() " +
                        "WHERE account_id = ?";
                try (PreparedStatement ps = con.prepareStatement(updLed)) {
                    ps.setLong(1, newBal);
                    ps.setLong(2, totalPrice);
                    ps.setInt(3, accountId);
                    int updated = ps.executeUpdate();
                    if (updated <= 0) {
                        alertLogic("buyFromShop updated 0 ledger rows. accountId=" + accountId);
                        throw new SQLException("Failed to deduct balance.");
                    }
                }

                // Decrement stock
                if (stock != null) {
                    String updStock = "UPDATE cosmic.donor_shop_items SET stock = stock - ? WHERE id = ? AND stock >= ?";
                    try (PreparedStatement ps = con.prepareStatement(updStock)) {
                        ps.setInt(1, times);
                        ps.setInt(2, shopItemId);
                        ps.setInt(3, times);
                        int updated = ps.executeUpdate();
                        if (updated <= 0) throw new SQLException("Out of stock.");
                    }
                }

                // Log txn
                String ref = "SHOP:" + cat + " itemId=" + itemId + " qty=" + baseQty + " x" + times +
                        (notes != null ? (" " + notes) : "");
                String insTxn = "INSERT INTO cosmic.donor_txn (account_id, char_id, txn_type, dc_cents_delta, donation_amount_cents, milestone_bonus_cents, ref) " +
                        "VALUES (?, ?, 'SPEND', ?, NULL, NULL, ?)";
                try (PreparedStatement ps = con.prepareStatement(insTxn)) {
                    ps.setInt(1, accountId);
                    ps.setInt(2, chr.getId());
                    ps.setLong(3, -totalPrice);
                    ps.setString(4, ref);
                    ps.executeUpdate();
                }

                // Grant item (pass period)
                grantItem(c, itemId, (short) totalQty, period);

                con.commit();
                return new PurchaseResult(itemId, totalQty, totalPrice, newBal);

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } catch (Exception e) {
                con.rollback();
                alertLogic("buyFromShop unexpected error. accountId=" + accountId + " shopItemId=" + shopItemId + " err=" + e.getMessage());
                throw new SQLException("Purchase failed: " + e.getMessage());
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Grants an item to player. durationDays applies to non-pets (expiration).
     * Dependency-safe: signature unchanged.
     */
    public static void grantItem(Client c, int itemId, short quantity, int durationDays) {
        if (c == null || c.getPlayer() == null) {
            alertLogic("grantItem called with null client/player. itemId=" + itemId);
            return;
        }

        String ownerName = c.getPlayer().getName();

        // Compute expiration for non-pets if durationDays > 0.
        long expiration = -1L;
        if (durationDays > 0) {
            expiration = System.currentTimeMillis() + java.util.concurrent.TimeUnit.DAYS.toMillis(durationDays);
        }

        try {
            if (ItemConstants.isPet(itemId)) {
                // Pets: force qty 1 + create unique id, fixed expiry 1 year (keep your current logic)
                short petQuantity = 1;

                int uniqueId = Pet.createPet(itemId);
                if (uniqueId <= 0) {
                    alertLogic("Pet.createPet returned invalid uniqueId=" + uniqueId + " itemId=" + itemId);
                    return;
                }

                long petExpiration = System.currentTimeMillis() + java.util.concurrent.TimeUnit.DAYS.toMillis(365);

                InventoryManipulator.addById(
                        c,
                        itemId,
                        petQuantity,
                        ownerName,
                        uniqueId,
                        petExpiration
                );
            } else {
                InventoryManipulator.addById(c, itemId, quantity, ownerName, -1, expiration);
            }
        } catch (Exception e) {
            alertLogic("grantItem failed. charId=" + c.getPlayer().getId() + " itemId=" + itemId + " qty=" + quantity + " err=" + e.getMessage());
        }
    }

    private static void ensureLedgerRow(int accountId) {
        String ins = "INSERT IGNORE INTO cosmic.donor_ledger (account_id) VALUES (?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(ins)) {
            ps.setInt(1, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            alertDb("ensureLedgerRow failed. accountId=" + accountId, e);
        }
    }

    // ====== Simple result holders ======

    public static final class CreditResult {
        public final int donationCents;
        public final int bonusCents;
        public final long creditedTotalCents;
        public final long newBalanceCents;
        public final long newLifetimeCents;

        public CreditResult(int donationCents, int bonusCents, long creditedTotalCents, long newBalanceCents, long newLifetimeCents) {
            this.donationCents = donationCents;
            this.bonusCents = bonusCents;
            this.creditedTotalCents = creditedTotalCents;
            this.newBalanceCents = newBalanceCents;
            this.newLifetimeCents = newLifetimeCents;
        }
    }

    public static final class PurchaseResult {
        public final int itemId;
        public final int qty;
        public final long priceCents;
        public final long newBalanceCents;

        public PurchaseResult(int itemId, int qty, long priceCents, long newBalanceCents) {
            this.itemId = itemId;
            this.qty = qty;
            this.priceCents = priceCents;
            this.newBalanceCents = newBalanceCents;
        }
    }
    // ==========================================================
    // NEW METHOD: Support for Script-based Custom Purchases
    // ==========================================================
    public static boolean deductFunds(int accountId, int amountCents, String description) {
        if (amountCents <= 0) return false;

        ensureLedgerRow(accountId);

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. Check Balance
                long bal;
                String sel = "SELECT dc_balance_cents FROM cosmic.donor_ledger WHERE account_id = ? FOR UPDATE";
                try (PreparedStatement ps = con.prepareStatement(sel)) {
                    ps.setInt(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return false;
                        bal = rs.getLong(1);
                    }
                }

                if (bal < amountCents) return false; // Insufficient funds

                // 2. Deduct
                long newBal = bal - amountCents;
                String upd = "UPDATE cosmic.donor_ledger SET dc_balance_cents = ?, dc_spent_cents_total = dc_spent_cents_total + ?, last_spend_at = NOW() WHERE account_id = ?";
                try (PreparedStatement ps = con.prepareStatement(upd)) {
                    ps.setLong(1, newBal);
                    ps.setLong(2, amountCents);
                    ps.setInt(3, accountId);
                    ps.executeUpdate();
                }

                // 3. Log Transaction
                String ins = "INSERT INTO cosmic.donor_txn (account_id, txn_type, dc_cents_delta, ref, created_at) VALUES (?, 'SPEND', ?, ?, NOW())";
                try (PreparedStatement ps = con.prepareStatement(ins)) {
                    ps.setInt(1, accountId);
                    ps.setLong(2, -amountCents);
                    ps.setString(3, description);
                    ps.executeUpdate();
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                alertDb("deductFunds failed for acc=" + accountId, e);
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            alertDb("deductFunds connection fail", e);
            return false;
        }
    }
// ========================================================================
    // NEW: World Buff Logic (Replicated from BuffWorldCommand + Announcement)
    // ========================================================================

    public static void applyWorldBuff(int worldId, String buyerName) {
        // 1. Prepare Announcement Packet (Type 6 = Lightblue Notice)
        String msg = "[Donor] " + buyerName + " has purchased the World Buff! Happy Mapling!";
        byte[] packet = PacketCreator.serverNotice(6, msg).getBytes(); // .getBytes() assuming your Packet class has it, or just pass the Object if broadcastPacket accepts it.
        // NOTE: In most sources, serverNotice returns a 'Packet' object.
        // If your broadcastPacket method takes a Packet, remove .getBytes().

        // 2. Iterate Worlds/Channels
        for (Channel ch : Server.getInstance().getChannelsFromWorld(worldId)) {

            // Broadcast the message to the channel
            ch.broadcastPacket(PacketCreator.serverNotice(6, msg));

            // Apply buffs to all players
            for (Character chr : ch.getPlayerStorage().getAllCharacters()) {
                if (chr != null) {
                    applySkill(chr, 9101001); // GM Super Body
                    applySkill(chr, 9101002); // GM Super Haste
                    applySkill(chr, 9101003); // GM Super HS
                    applySkill(chr, 9101008); // GM Super HB
                    applySkill(chr, 1005);    // Echo of Hero (Source specific ID)
                    applySkill(chr, 5121009); // Speed Infusion
                    applySkill(chr, 3121002); // Sharp Eyes
                    applySkill(chr, 4111001); // Meso Up
                }
            }
        }
    }

    // Helper to keep the loop clean
    private static void applySkill(Character chr, int skillId) {
        try {
            var skill = SkillFactory.getSkill(skillId);
            if (skill != null) {
                skill.getEffect(skill.getMaxLevel()).applyTo(chr, true);
            }
        } catch (Exception e) {
            // Ignore if skill doesn't exist to prevent crash
        }
    }

}
