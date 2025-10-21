package server.creator_shop;

import client.Character;
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import tools.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CreatorShopManager (static utility style, like OrePouchManager)
 * - Logs transactions
 * - Computes/marks unclaimed totals
 * - Handles MESO/NX payments with BCoin/NXT fallback
 */
public class CreatorShopManager {

    // Currency item IDs (adjust if yours are different)
    private static final int BCOIN_ITEM_ID = 4031997; // 1b mesos coin
    private static final int NXT_ITEM_ID   = 4032309; // 1m NX token

    // CashShop types in classic Odin/Heaven: 1 = NX, 2 = Maple Points (usually)
    private static final int NX_TYPE = 1;

    /* ========================= SQL: Transactions ========================= */

    public static void logTransaction(int shopNpcId, Character chr, int itemId, long price, String currencyType) {
        String sql = "INSERT INTO creator_shop_transactions " +
                "(shop_npcid, buyer_id, item_id, price, currency_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, shopNpcId);
            ps.setInt(2, chr.getId());
            ps.setInt(3, itemId);
            ps.setLong(4, price);
            ps.setString(5, currencyType.toUpperCase());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* ========================= Claims ========================= */

    public static long getUnclaimedTotal(int shopNpcId) {
        String sql = "SELECT SUM(price) AS total " +
                "FROM creator_shop_transactions " +
                "WHERE shop_npcid=? AND claim_time IS NULL";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, shopNpcId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    public static void markClaimed(int shopNpcId) {
        String sql = "UPDATE creator_shop_transactions " +
                "SET claim_time=NOW() " +
                "WHERE shop_npcid=? AND claim_time IS NULL";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, shopNpcId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* ========================= Currency Handling ========================= */

    public static boolean hasEnoughCurrency(Character chr, long amount, String currencyType) {
        String type = currencyType.toUpperCase();
        if ("MESO".equals(type)) {
            long mesos     = chr.getMeso();
            long bcoins    = (long) chr.getItemQuantity(BCOIN_ITEM_ID, false) * 1_000_000_000L;
            return (mesos + bcoins) >= amount;
        } else if ("NX".equals(type)) {
            long nx        = chr.getCashShop().getCash(NX_TYPE);
            long nxtTokens = (long) chr.getItemQuantity(NXT_ITEM_ID, false) * 1_000_000L;
            return (nx + nxtTokens) >= amount;
        }
        return false;
    }

    public static void deductCurrency(Character chr, long amount, String currencyType) {
        String type = currencyType.toUpperCase();
        if ("MESO".equals(type)) {
            deductMesoWithBCoinFallback(chr, amount);
        } else if ("NX".equals(type)) {
            deductNxWithNXTItemFallback(chr, amount);
        }
    }

    private static void deductMesoWithBCoinFallback(Character chr, long amount) {
        long mesos = chr.getMeso();
        if (mesos >= amount) {
            chr.gainMeso((int) -amount, false);
            return;
        }

        long remaining = amount - mesos;
        chr.gainMeso((int) -mesos, false);

        long coinsNeeded = (long) Math.ceil(remaining / 1_000_000_000.0);
        for (int i = 0; i < coinsNeeded; i++) {
            if (chr.getItemQuantity(BCOIN_ITEM_ID, false) > 0) {
                // BCoins are ETC items; use InventoryType.ETC
                InventoryManipulator.removeById(
                        chr.getClient(), InventoryType.ETC, BCOIN_ITEM_ID, 1, true, false
                );
            } else {
                break;
            }
        }

        long overpay = coinsNeeded * 1_000_000_000L - remaining;
        if (overpay > 0) {
            chr.gainMeso((int) overpay, false);
        }
    }

    private static void deductNxWithNXTItemFallback(Character chr, long amount) {
        int nx = chr.getCashShop().getCash(NX_TYPE);
        if (nx >= amount) {
            chr.getCashShop().gainCash(NX_TYPE, (int) -amount);
            return;
        }

        long remaining = amount - nx;
        chr.getCashShop().gainCash(NX_TYPE, -nx);

        long tokensNeeded = (long) Math.ceil(remaining / 1_000_000.0);
        for (int i = 0; i < tokensNeeded; i++) {
            if (chr.getItemQuantity(NXT_ITEM_ID, false) > 0) {
                // NXT is also an ETC item
                InventoryManipulator.removeById(
                        chr.getClient(), InventoryType.ETC, NXT_ITEM_ID, 1, true, false
                );
            } else {
                break;
            }
        }

        long overpay = tokensNeeded * 1_000_000L - remaining;
        if (overpay > 0) {
            chr.getCashShop().gainCash(NX_TYPE, (int) overpay);
        }
    }

    /* ========================= Optional: Recent Sales ========================= */

    public static List<String> getRecentTransactions(int shopNpcId, int limit) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT item_id, price, currency_type, transaction_time " +
                "FROM creator_shop_transactions " +
                "WHERE shop_npcid=? ORDER BY transaction_time DESC LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, shopNpcId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId       = rs.getInt("item_id");
                    long price       = rs.getLong("price");
                    String curr      = rs.getString("currency_type");
                    Timestamp ts     = rs.getTimestamp("transaction_time");
                    list.add("#v" + itemId + "# " + price + " " + curr + " (" + ts + ")");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
