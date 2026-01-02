package server.creator_shop;

import client.Character;
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import tools.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CreatorShopManager
 * ----------------------------------------------------------------
 * Handles creator shop transactions, rewards, and currency logic.
 * Supports MESO, NX, BCOIN (#3020002), and NXT (#3020001).
 * ----------------------------------------------------------------
 */
public class CreatorShopManager {

    // Currency item IDs
    private static final int BCOIN_ITEM_ID = 3020002; // 1B meso coin
    private static final int NXT_ITEM_ID   = 3020001; // 1M NX token
    private static final int NX_TYPE = 1; // NX cash type

    private static final long BCOIN_VALUE = 1_000_000_000L;
    private static final long NXT_VALUE   = 1_000_000L;

    // --- Key Alerts (rate-limited) ---
    private static volatile long lastDbAlertAt = 0L;
    private static volatile long lastLogicAlertAt = 0L;
    private static final long ALERT_COOLDOWN_MS = 60_000L;

    private static void alertDb(String msg, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastDbAlertAt >= ALERT_COOLDOWN_MS) {
            lastDbAlertAt = now;
            System.err.println("[CreatorShopManager][DB] " + msg + (t != null ? " err=" + t.getMessage() : ""));
        }
    }

    private static void alertLogic(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastLogicAlertAt >= ALERT_COOLDOWN_MS) {
            lastLogicAlertAt = now;
            System.err.println("[CreatorShopManager][ALERT] " + msg);
        }
    }

    /* ========================= SQL: Transactions ========================= */

    public static void logTransaction(int shopNpcId, Character chr, int itemId, long price, String currencyType) {
        if (chr == null) {
            alertLogic("logTransaction called with null chr. shopNpcId=" + shopNpcId + " itemId=" + itemId);
            return;
        }
        if (price <= 0) {
            alertLogic("logTransaction called with non-positive price. buyerId=" + chr.getId() + " price=" + price + " curr=" + currencyType);
            return;
        }

        String sql = "INSERT INTO creator_shop_transactions " +
                "(shop_npcid, buyer_id, item_id, price, currency_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, shopNpcId);
            ps.setInt(2, chr.getId());
            ps.setInt(3, itemId);
            ps.setLong(4, price);
            ps.setString(5, currencyType == null ? "" : currencyType.toUpperCase());
            ps.executeUpdate();

        } catch (SQLException e) {
            alertDb("Failed to logTransaction. shopNpcId=" + shopNpcId + " buyerId=" + chr.getId() +
                    " itemId=" + itemId + " price=" + price + " curr=" + currencyType, e);
        }
    }

    /* ========================= Claims ========================= */

    public static long getUnclaimedTotal(int shopNpcId) {
        String sql = "SELECT SUM(price) AS total FROM creator_shop_transactions " +
                "WHERE shop_npcid=? AND claim_time IS NULL";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, shopNpcId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }

        } catch (SQLException e) {
            alertDb("Failed to getUnclaimedTotal. shopNpcId=" + shopNpcId, e);
        }
        return 0L;
    }

    public static long getUnclaimedTotalByCurrency(int shopNpcId, String currencyType) {
        String sql = "SELECT SUM(price) AS total FROM creator_shop_transactions " +
                "WHERE shop_npcid=? AND claim_time IS NULL AND currency_type=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, shopNpcId);
            ps.setString(2, currencyType == null ? "" : currencyType.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }

        } catch (SQLException e) {
            alertDb("Failed to getUnclaimedTotalByCurrency. shopNpcId=" + shopNpcId + " curr=" + currencyType, e);
        }
        return 0L;
    }

    public static void markClaimed(int shopNpcId) {
        String sql = "UPDATE creator_shop_transactions SET claim_time=NOW() " +
                "WHERE shop_npcid=? AND claim_time IS NULL";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, shopNpcId);
            ps.executeUpdate();

        } catch (SQLException e) {
            alertDb("Failed to markClaimed. shopNpcId=" + shopNpcId, e);
        }
    }

    /* ========================= Currency Handling ========================= */

    public static boolean hasEnoughCurrency(Character chr, long amount, String currencyType) {
        if (chr == null || currencyType == null || amount <= 0) return false;

        String type = currencyType.toUpperCase();
        switch (type) {
            case "MESO": {
                long mesos = chr.getMeso();
                long bcoinValue = (long) chr.getItemQuantity(BCOIN_ITEM_ID, false) * BCOIN_VALUE;
                return (mesos + bcoinValue) >= amount;
            }
            case "NX": {
                long nx = chr.getCashShop().getCash(NX_TYPE);
                long nxtValue = (long) chr.getItemQuantity(NXT_ITEM_ID, false) * NXT_VALUE;
                return (nx + nxtValue) >= amount;
            }
            case "BCOIN":
                return chr.getItemQuantity(BCOIN_ITEM_ID, false) >= amount;
            case "NXT":
                return chr.getItemQuantity(NXT_ITEM_ID, false) >= amount;
            default:
                // Key alert: unknown currency type used by caller
                alertLogic("hasEnoughCurrency unknown currencyType=" + currencyType + " buyerId=" + chr.getId());
                return false;
        }
    }

    public static void deductCurrency(Character chr, long amount, String currencyType) {
        if (chr == null || currencyType == null || amount <= 0) {
            if (chr != null) {
                alertLogic("deductCurrency invalid input. buyerId=" + chr.getId() + " amount=" + amount + " curr=" + currencyType);
            }
            return;
        }

        String type = currencyType.toUpperCase();

        // Key alert: deduct called without enough funds (should be guarded by hasEnoughCurrency)
        if (!hasEnoughCurrency(chr, amount, type)) {
            alertLogic("deductCurrency called but insufficient funds. buyerId=" + chr.getId() + " amount=" + amount + " curr=" + type);
            return;
        }

        if ("MESO".equals(type)) {
            deductMesoWithBCoinFallback(chr, amount);
        } else if ("NX".equals(type)) {
            deductNxWithNXTItemFallback(chr, amount);
        } else if ("BCOIN".equals(type)) {
            removeItemCurrency(chr, BCOIN_ITEM_ID, amount);
        } else if ("NXT".equals(type)) {
            removeItemCurrency(chr, NXT_ITEM_ID, amount);
        } else {
            alertLogic("deductCurrency unknown currencyType=" + currencyType + " buyerId=" + chr.getId());
        }
    }

    private static void removeItemCurrency(Character chr, int itemId, long amount) {
        long owned = chr.getItemQuantity(itemId, false);
        if (owned < amount) {
            // Should never happen if guarded earlier
            alertLogic("removeItemCurrency insufficient items. buyerId=" + chr.getId() + " itemId=" + itemId + " owned=" + owned + " need=" + amount);
            return;
        }

        // InventoryManipulator takes int qty; chunk if huge (extremely unlikely)
        long remaining = amount;
        while (remaining > 0) {
            int step = (int) Math.min(Integer.MAX_VALUE, remaining);
            InventoryManipulator.removeById(chr.getClient(), InventoryType.ETC, itemId, step, true, false);
            remaining -= step;
        }
    }

    private static void deductMesoWithBCoinFallback(Character chr, long amount) {
        long mesos = chr.getMeso();

        if (amount > (long) Integer.MAX_VALUE) {
            // Key alert: extremely large transaction; gainMeso uses int
            alertLogic("Very large MESO deduction requested (int-capped API). buyerId=" + chr.getId() + " amount=" + amount);
        }

        if (mesos >= amount) {
            safeGainMeso(chr, -amount);
            return;
        }

        long remaining = amount - mesos;

        // Take all mesos
        safeGainMeso(chr, -mesos);

        long coinsNeeded = (long) Math.ceil(remaining / (double) BCOIN_VALUE);
        long coinsUsed = 0;

        for (long i = 0; i < coinsNeeded; i++) {
            if (chr.getItemQuantity(BCOIN_ITEM_ID, false) > 0) {
                InventoryManipulator.removeById(chr.getClient(), InventoryType.ETC, BCOIN_ITEM_ID, 1, true, false);
                coinsUsed++;
            } else {
                break;
            }
        }

        // Since we guarded with hasEnoughCurrency(), coinsUsed should always == coinsNeeded here.
        // But compute overpay from coinsUsed to avoid refund bug if something unexpected happens.
        long paid = coinsUsed * BCOIN_VALUE;
        long overpay = paid - remaining;

        if (overpay > 0) {
            safeGainMeso(chr, overpay);
        } else if (overpay < 0) {
            // Key alert: this indicates unexpected failure removing coins
            alertLogic("BCOIN removal shortfall despite precheck. buyerId=" + chr.getId() +
                    " needRemaining=" + remaining + " coinsUsed=" + coinsUsed + " coinsNeeded=" + coinsNeeded);
        }
    }

    private static void deductNxWithNXTItemFallback(Character chr, long amount) {
        int nx = chr.getCashShop().getCash(NX_TYPE);

        if (amount > (long) Integer.MAX_VALUE) {
            // gainCash uses int; this is a key alert if it ever happens
            alertLogic("Very large NX deduction requested (int-capped API). buyerId=" + chr.getId() + " amount=" + amount);
        }

        if ((long) nx >= amount) {
            chr.getCashShop().gainCash(NX_TYPE, (int) -amount);
            return;
        }

        long remaining = amount - nx;

        // Take all nx
        chr.getCashShop().gainCash(NX_TYPE, -nx);

        long tokensNeeded = (long) Math.ceil(remaining / (double) NXT_VALUE);
        long tokensUsed = 0;

        for (long i = 0; i < tokensNeeded; i++) {
            if (chr.getItemQuantity(NXT_ITEM_ID, false) > 0) {
                InventoryManipulator.removeById(chr.getClient(), InventoryType.ETC, NXT_ITEM_ID, 1, true, false);
                tokensUsed++;
            } else {
                break;
            }
        }

        long paid = tokensUsed * NXT_VALUE;
        long overpay = paid - remaining;

        if (overpay > 0) {
            chr.getCashShop().gainCash(NX_TYPE, (int) Math.min(Integer.MAX_VALUE, overpay));
        } else if (overpay < 0) {
            alertLogic("NXT removal shortfall despite precheck. buyerId=" + chr.getId() +
                    " needRemaining=" + remaining + " tokensUsed=" + tokensUsed + " tokensNeeded=" + tokensNeeded);
        }
    }

    private static void safeGainMeso(Character chr, long delta) {
        // gainMeso accepts int; chunk to avoid overflow
        long remaining = delta;
        while (remaining != 0) {
            if (remaining > 0) {
                int step = (int) Math.min(Integer.MAX_VALUE, remaining);
                chr.gainMeso(step, false);
                remaining -= step;
            } else {
                long abs = -remaining;
                int step = (int) Math.min(Integer.MAX_VALUE, abs);
                chr.gainMeso(-step, false);
                remaining += step;
            }
        }
    }

    /* ========================= Optional: Recent Sales ========================= */

    public static List<String> getRecentTransactions(int shopNpcId, int limit) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT item_id, price, currency_type, transaction_time " +
                "FROM creator_shop_transactions WHERE shop_npcid=? " +
                "ORDER BY transaction_time DESC LIMIT ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, shopNpcId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId = rs.getInt("item_id");
                    long price = rs.getLong("price");
                    String curr = rs.getString("currency_type");
                    Timestamp ts = rs.getTimestamp("transaction_time");
                    list.add("#v" + itemId + "# " + price + " " + curr + " (" + ts + ")");
                }
            }

        } catch (SQLException e) {
            alertDb("Failed to getRecentTransactions. shopNpcId=" + shopNpcId + " limit=" + limit, e);
        }

        return list;
    }
}
