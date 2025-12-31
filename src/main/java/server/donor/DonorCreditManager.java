package server.donor;

import client.Client;
import client.Character;
import client.inventory.Pet;
import client.inventory.manipulator.InventoryManipulator;
import constants.inventory.ItemConstants;
import tools.DatabaseConnection;

import java.sql.*;
import java.util.*;

public final class DonorCreditManager {

    private DonorCreditManager() {}

    // ====== CONFIG ======
    // 10.00 DC bonus per $50 lifetime donated
    private static final int MILESTONE_SGD_CENTS = 5000;     // $50.00
    private static final int BONUS_DC_CENTS_PER_MILESTONE = 1000; // 10.00 DC

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
                }
            }
        } catch (SQLException e) {
            // keep minimal, actionable error
            System.err.println("[DonorCreditManager] getStatusByAccountId SQL error: " + e.getMessage());
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
                    r.put("at", rs.getTimestamp("created_at").toString());
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DonorCreditManager] getRecentTxns SQL error: " + e.getMessage());
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
            System.err.println("[DonorCreditManager] getCategories SQL error: " + e.getMessage());
        }
        return cats;
    }

    public static List<Map<String, Object>> getItemsByCategory(String category) {
        List<Map<String, Object>> list = new ArrayList<>();
        // [FIX 1] Added 'period' to the SELECT statement
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

                    // [FIX 2] Put period into the map so JS can read it
                    r.put("period", rs.getInt("period"));

                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DonorCreditManager] getItemsByCategory SQL error: " + e.getMessage());
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

                // Lock ledger row
                String sel = "SELECT lifetime_cents, dc_balance_cents FROM cosmic.donor_ledger WHERE account_id = ? FOR UPDATE";
                long oldBal;

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
                    ps.executeUpdate();
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
                // Lock ledger (same as before)
                long bal;
                String selLed = "SELECT dc_balance_cents FROM cosmic.donor_ledger WHERE account_id = ? FOR UPDATE";
                try (PreparedStatement ps = con.prepareStatement(selLed)) {
                    ps.setInt(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Ledger row missing for account_id=" + accountId);
                        bal = rs.getLong(1);
                    }
                }

                // [FIX 3] Added 'period' variable and to SQL
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

                        // [FIX 4] Read period from DB (default 0 if null/missing)
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

                int totalQty = baseQty * times;
                if (totalQty <= 0) throw new SQLException("Quantity overflow.");

                if (!InventoryManipulator.checkSpace(c, itemId, totalQty, "")) {
                    throw new SQLException("Not enough inventory space.");
                }

                long newBal = bal - totalPrice;

                // Deduct balance (same as before)
                String updLed = "UPDATE cosmic.donor_ledger SET " +
                        "dc_balance_cents = ?, " +
                        "dc_spent_cents_total = dc_spent_cents_total + ?, " +
                        "last_spend_at = NOW() " +
                        "WHERE account_id = ?";
                try (PreparedStatement ps = con.prepareStatement(updLed)) {
                    ps.setLong(1, newBal);
                    ps.setLong(2, totalPrice);
                    ps.setInt(3, accountId);
                    ps.executeUpdate();
                }

                // Decrement stock (same as before)
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

                // Log txn (same as before)
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

                // [FIX 5] Pass 'period' to the grantItem function
                grantItem(c, itemId, (short) totalQty, period);

                con.commit();
                return new PurchaseResult(itemId, totalQty, totalPrice, newBal);

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } catch (Exception e) {
                con.rollback();
                throw new SQLException("Purchase failed: " + e.getMessage());
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public static void grantItem(Client c, int itemId, short quantity, int durationDays) {
        // Parameter 1: Client (c) - Passed in directly
        // Parameter 4: Owner Name - Retrieved from c.getPlayer().getName()
        String ownerName = c.getPlayer().getName();

        if (ItemConstants.isPet(itemId)) {
            // --- PET HANDLING (The 6 Parameters Fix) ---

            // Parameter 3: Quantity
            // Force quantity to 1 for pets, regardless of input
            short petQuantity = 1;

            // Parameter 5: Unique ID (petId)
            // CRITICAL: Must be generated by database first!
            // Matches Printout: "petid: 21"
            int uniqueId = Pet.createPet(itemId);


            // Parameter 6: Expiration (Fixed 1 Year)
            long expiration = System.currentTimeMillis() + java.util.concurrent.TimeUnit.DAYS.toMillis(365);

            // The Fix: Passing all 6 parameters exactly as ItemCommand does
            InventoryManipulator.addById(
                    c,              // 1. Client
                    itemId,         // 2. Item ID (e.g., 5000042)
                    petQuantity,    // 3. Quantity (Always 1)
                    ownerName,      // 4. Owner Name (e.g., "Merogie")
                    uniqueId,       // 5. Unique/Cash ID (e.g., 21)
                    expiration      // 6. Expiration Timestamp
            );

        } else {
            // --- NON-PET HANDLING ---
            // For regular items, uniqueId is usually -1 or 0, and expiration might be -1
            InventoryManipulator.addById(c, itemId, quantity, ownerName, -1, -1);
        }
    }



    private static void ensureLedgerRow(int accountId) {
        String ins = "INSERT IGNORE INTO cosmic.donor_ledger (account_id) VALUES (?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(ins)) {
            ps.setInt(1, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DonorCreditManager] ensureLedgerRow SQL error: " + e.getMessage());
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
}
