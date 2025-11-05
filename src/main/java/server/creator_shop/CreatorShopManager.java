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
        String sql = "SELECT SUM(price) AS total FROM creator_shop_transactions " +
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

    public static long getUnclaimedTotalByCurrency(int shopNpcId, String currencyType) {
        String sql = "SELECT SUM(price) AS total FROM creator_shop_transactions " +
                "WHERE shop_npcid=? AND claim_time IS NULL AND currency_type=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, shopNpcId);
            ps.setString(2, currencyType.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    /* ========================= Currency Handling ========================= */

    public static boolean hasEnoughCurrency(Character chr, long amount, String currencyType) {
        String type = currencyType.toUpperCase();
        switch (type) {
            case "MESO":
                long mesos = chr.getMeso();
                long bcoinValue = (long) chr.getItemQuantity(BCOIN_ITEM_ID, false) * 1_000_000_000L;
                return (mesos + bcoinValue) >= amount;

            case "NX":
                long nx = chr.getCashShop().getCash(NX_TYPE);
                long nxtValue = (long) chr.getItemQuantity(NXT_ITEM_ID, false) * 1_000_000L;
                return (nx + nxtValue) >= amount;

            case "BCOIN":
                return chr.getItemQuantity(BCOIN_ITEM_ID, false) >= amount;

            case "NXT":
                return chr.getItemQuantity(NXT_ITEM_ID, false) >= amount;

            default:
                return false;
        }
    }

    public static void deductCurrency(Character chr, long amount, String currencyType) {
        String type = currencyType.toUpperCase();
        if ("MESO".equals(type)) {
            deductMesoWithBCoinFallback(chr, amount);
        } else if ("NX".equals(type)) {
            deductNxWithNXTItemFallback(chr, amount);
        } else if ("BCOIN".equals(type)) {
            removeItemCurrency(chr, BCOIN_ITEM_ID, amount);
        } else if ("NXT".equals(type)) {
            removeItemCurrency(chr, NXT_ITEM_ID, amount);
        }
    }

    private static void removeItemCurrency(Character chr, int itemId, long amount) {
        long owned = chr.getItemQuantity(itemId, false);
        if (owned < amount) return;
        InventoryManipulator.removeById(
                chr.getClient(), InventoryType.ETC, itemId, (int) amount, true, false
        );
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
                InventoryManipulator.removeById(
                        chr.getClient(), InventoryType.ETC, BCOIN_ITEM_ID, 1, true, false
                );
            } else break;
        }

        long overpay = coinsNeeded * 1_000_000_000L - remaining;
        if (overpay > 0) chr.gainMeso((int) overpay, false);
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
                InventoryManipulator.removeById(
                        chr.getClient(), InventoryType.ETC, NXT_ITEM_ID, 1, true, false
                );
            } else break;
        }

        long overpay = tokensNeeded * 1_000_000L - remaining;
        if (overpay > 0) chr.getCashShop().gainCash(NX_TYPE, (int) overpay);
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
            e.printStackTrace();
        }
        return list;
    }
}
