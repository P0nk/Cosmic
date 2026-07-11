package server.transactions;

import client.Character;
import client.inventory.Equip;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import config.YamlConfig;
import constants.game.GameConstants;
import server.responses.SelectionListStringResult;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TransactionService {

    public static SelectionListStringResult getTransactionSelectionListString(int userId) {
        List<Transaction> transactions = getLastTransactions(userId, YamlConfig.config.server.MAXIMUM_TRANSACTIONS_FOR_BUYBACK);

        if (transactions.isEmpty()) {
            return new SelectionListStringResult(false,"#rThere are no previous transactions at this time.#k");
        }

        StringBuilder stringBuilder = new StringBuilder("Last transactions:\r\n\r\n");
        IntStream.range(0, transactions.size()).forEach(i -> {
                        Transaction transaction = transactions.get(i);
                        int totalItems = transaction.getItems().size();
                        int totalMesos = transaction.getTotalPurchasePrice();
                        String transactionIndicator = transaction.isBuybackUsed() ? " [Used]" : "";

                        String transactionText = String.format("#L%d#%d. %d items (%s mesos)%s\r\n",
                                i,
                                i + 1,
                                totalItems,
                                GameConstants.numberWithCommas(totalMesos),
                                transactionIndicator);

                        stringBuilder.append(transactionText);
                });

        return new SelectionListStringResult(true, stringBuilder.toString());
    }

    public static SelectionListStringResult getTransactionItemListString(int userId, int transactionIndex) {
        List<Transaction> transactions = getLastTransactions(userId, YamlConfig.config.server.MAXIMUM_TRANSACTIONS_FOR_BUYBACK);

        if (transactions.isEmpty()) {
            return new SelectionListStringResult(false,"#rThere are no previous transactions at this time. (haxor?)#k");
        }

        if (transactionIndex < 0 || transactionIndex > transactions.size()) {
            return new SelectionListStringResult(false,"#rInvalid selection. (haxor?)#k ");
        }

        Transaction transaction = transactions.get(transactionIndex);

        if (transaction.isBuybackUsed()) {
            return new SelectionListStringResult(false, "This transaction has already been bought back.");
        }

        return getItemListString(transactionIndex, transaction);
    }

    private static SelectionListStringResult getItemListString(int transactionIndex, Transaction transaction) {
        StringBuilder itemListString = new StringBuilder("Are you sure you want to buy back this transaction?\r\n\r\nItems contained in this transaction:\r\n");
        AtomicInteger formatCounter = new AtomicInteger();

        List<String> itemStrings = transaction.getItems().stream()
                .map(item -> {
                    String suffix = formatCounter.incrementAndGet() % 4 == 0 ? "\r\n" : "";
                    return String.format("#i%s#   %s", item.getItemId(), suffix);
                })
                .collect(Collectors.toList());

        itemListString.append(String.join("", itemStrings));

        return new SelectionListStringResult(true, itemListString.toString());
    }

    public static SelectionListStringResult processTransaction(Character player, int transactionIndex) {
        List<Transaction> transactions = getLastTransactions(player.getId(), YamlConfig.config.server.MAXIMUM_TRANSACTIONS_FOR_BUYBACK);

        if (transactionIndex >= transactions.size()) {
            return new SelectionListStringResult(false, "Invalid transaction number.");
        }

        Transaction transaction = transactions.get(transactionIndex);
        if (transaction.isBuybackUsed()) {
            return new SelectionListStringResult(false, "This transaction has already been bought back.");
        }

        int buybackPrice = transaction.getTotalPurchasePrice();
        if (player.getMeso() < buybackPrice) {
            return new SelectionListStringResult(false, "You don't have enough mesos to complete this buyback");
        }

        List<Pair<Item, InventoryType>> validationList = TransactionService.convertTransactionItemsToItemPairs(transaction.getItems());
        if (!player.canHold(validationList)) {
            return new SelectionListStringResult(false, "You don't have enough space to complete this buyback");
        }

        if (updateTransactionBuybackStatus(transaction.getTransactionId(), true))
        {
            try {
                List<TransactionItem> transactionItems = transaction.getItems();
                List<Item> items = TransactionService.convertTransactionItemsToItems(transactionItems);
                for (Item item : items) {
                    InventoryManipulator.add(player.getClient(), item, false);
                }

                player.gainMeso(-buybackPrice, true);
            } catch (Exception e) {
                return new SelectionListStringResult(false, "Interrupted, failed to process some or all items. Please context support for additional assistance.");
            }

            return new SelectionListStringResult(true, "Bought back items for " + GameConstants.numberWithCommas(buybackPrice) + " mesos.");
        } else {
            return new SelectionListStringResult(false, "Failed to update the transaction. Aborted.");
        }
    }

    // ======== MAPPERS ========
    public static List<TransactionItem> convertItemsToTransactionItems(List<Item> items) {
        List<TransactionItem> transactionItems = new ArrayList<>();
        for (Item item : items) {
            TransactionItem transactionItem;
            if (item instanceof Equip) {
                Equip equip = (Equip) item;
                TransactionEquip transactionEquip = new TransactionEquip();
                transactionEquip.setUpgradeSlots(equip.getUpgradeSlots());
                transactionEquip.setLevel(equip.getLevel());
                transactionEquip.setStr(equip.getStr());
                transactionEquip.setDex(equip.getDex());
                transactionEquip.setIntStat(equip.getInt());
                transactionEquip.setLuk(equip.getLuk());
                transactionEquip.setHp(equip.getHp());
                transactionEquip.setMp(equip.getMp());
                transactionEquip.setWatk(equip.getWatk());
                transactionEquip.setMatk(equip.getMatk());
                transactionEquip.setWdef(equip.getWdef());
                transactionEquip.setMdef(equip.getMdef());
                transactionEquip.setAcc(equip.getAcc());
                transactionEquip.setAvoid(equip.getAvoid());
                transactionEquip.setHands(equip.getHands());
                transactionEquip.setSpeed(equip.getSpeed());
                transactionEquip.setJump(equip.getJump());
                transactionEquip.setLocked(0);
                transactionEquip.setVicious(equip.getVicious());
                transactionEquip.setItemLevel(equip.getItemLevel());
                transactionEquip.setItemExp(equip.getItemExp());
                transactionEquip.setRingId(equip.getRingId());
                transactionItem = transactionEquip;
            } else {
                transactionItem = new TransactionItem();
            }
            transactionItem.setType(item.getItemType());
            transactionItem.setItemId(item.getItemId());
            transactionItem.setInventoryType(item.getInventoryType().getType());
            transactionItem.setPosition(item.getPosition());
            transactionItem.setQuantity(item.getQuantity());
            transactionItem.setOwner(item.getOwner());
            transactionItem.setPetId(item.getPetId());
            transactionItem.setFlag(item.getFlag());
            transactionItem.setExpiration(item.getExpiration());
            transactionItem.setGiftFrom(item.getGiftFrom());
            transactionItems.add(transactionItem);
        }
        return transactionItems;
    }

    public static List<Item> convertTransactionItemsToItems(List<TransactionItem> transactionItems) {
        List<Item> items = new ArrayList<>();
        for (TransactionItem transactionItem : transactionItems) {
            Item item;
            if (transactionItem instanceof TransactionEquip) {
                TransactionEquip transactionEquip = (TransactionEquip) transactionItem;
                Equip equip = new Equip(transactionEquip.getItemId(), (short) transactionEquip.getPosition());
                equip.setUpgradeSlots((byte) transactionEquip.getUpgradeSlots());
                equip.setLevel(transactionEquip.getLevel());
                equip.setStr(transactionEquip.getStr());
                equip.setDex(transactionEquip.getDex());
                equip.setInt(transactionEquip.getIntStat());
                equip.setLuk(transactionEquip.getLuk());
                equip.setHp(transactionEquip.getHp());
                equip.setMp(transactionEquip.getMp());
                equip.setWatk(transactionEquip.getWatk());
                equip.setMatk(transactionEquip.getMatk());
                equip.setWdef(transactionEquip.getWdef());
                equip.setMdef(transactionEquip.getMdef());
                equip.setAcc(transactionEquip.getAcc());
                equip.setAvoid(transactionEquip.getAvoid());
                equip.setHands(transactionEquip.getHands());
                equip.setSpeed(transactionEquip.getSpeed());
                equip.setJump(transactionEquip.getJump());
                // equip.setLocked(transactionEquip.getLocked());
                equip.setVicious(transactionEquip.getVicious());
                equip.setItemLevel(transactionEquip.getItemLevel());
                equip.setItemExp(transactionEquip.getItemExp());
                equip.setRingId(transactionEquip.getRingId());
                item = equip;
            } else {
                item = new Item(transactionItem.getItemId(), (byte) transactionItem.getPosition(), (short) transactionItem.getQuantity(), transactionItem.getPetId());
            }
            item.setOwner(transactionItem.getOwner());
            item.setExpiration(transactionItem.getExpiration());
            item.setGiftFrom(transactionItem.getGiftFrom());
            item.setFlag((short) transactionItem.getFlag());
            items.add(item);
        }
        return items;
    }


    public static List<Pair<Item, InventoryType>> convertTransactionItemsToItemPairs(List<TransactionItem> transactionItems) {
        List<Pair<Item, InventoryType>> itemPairs = new ArrayList<>();
        for (TransactionItem transactionItem : transactionItems) {
            Item item = getItemFromTransactionItem(transactionItem);
            InventoryType inventoryType = InventoryType.getByType((byte) transactionItem.getInventoryType());
            itemPairs.add(new Pair<>(item, inventoryType));
        }
        return itemPairs;
    }

    private static Item getItemFromTransactionItem(TransactionItem transactionItem) {
        Item item = new Item(transactionItem.getItemId(), (short) transactionItem.getPosition(), (short) transactionItem.getQuantity(), transactionItem.getPetId());
        item.setOwner(transactionItem.getOwner());
        item.setFlag((short) transactionItem.getFlag());
        item.setExpiration(transactionItem.getExpiration());
        item.setGiftFrom(transactionItem.getGiftFrom());
        return item;
    }

    // ======== DATABASE =========
    public static int createTransaction(int userId) throws SQLException {
        String insertTransactionSQL = "INSERT INTO mapleroot.transactions (user_id) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(insertTransactionSQL, PreparedStatement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, userId);
            preparedStatement.executeUpdate();

            try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public static void createTransactionItems(int transactionId, List<TransactionItem> items) throws SQLException {
        String insertTransactionItemSQL = "INSERT INTO mapleroot.transaction_items (transaction_id, type, itemid, inventorytype, " +
                "position, quantity, owner, petid, flag, expiration, giftFrom) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertTransactionEquipSQL = "INSERT INTO mapleroot.transaction_equips (transaction_item_id, upgradeslots, level, str, dex, `int`, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, locked, vicious, itemlevel, itemexp, ringid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement itemPreparedStatement = conn.prepareStatement(insertTransactionItemSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement equipPreparedStatement = conn.prepareStatement(insertTransactionEquipSQL)) {

            for (TransactionItem item : items) {
                itemPreparedStatement.setInt(1, transactionId);
                itemPreparedStatement.setInt(2, item.getType());
                itemPreparedStatement.setInt(3, item.getItemId());
                itemPreparedStatement.setInt(4, item.getInventoryType());
                itemPreparedStatement.setInt(5, item.getPosition());
                itemPreparedStatement.setInt(6, item.getQuantity());
                itemPreparedStatement.setString(7, item.getOwner());
                itemPreparedStatement.setInt(8, item.getPetId());
                itemPreparedStatement.setInt(9, item.getFlag());
                itemPreparedStatement.setLong(10, item.getExpiration());
                itemPreparedStatement.setString(11, item.getGiftFrom());
                itemPreparedStatement.executeUpdate();

                try (ResultSet generatedKeys = itemPreparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int transactionItemId = generatedKeys.getInt(1);

                        if (item instanceof TransactionEquip) {
                            TransactionEquip equip = (TransactionEquip) item;

                            equipPreparedStatement.setInt(1, transactionItemId);
                            equipPreparedStatement.setInt(2, equip.getUpgradeSlots());
                            equipPreparedStatement.setInt(3, equip.getLevel());
                            equipPreparedStatement.setInt(4, equip.getStr());
                            equipPreparedStatement.setInt(5, equip.getDex());
                            equipPreparedStatement.setInt(6, equip.getIntStat());
                            equipPreparedStatement.setInt(7, equip.getLuk());
                            equipPreparedStatement.setInt(8, equip.getHp());
                            equipPreparedStatement.setInt(9, equip.getMp());
                            equipPreparedStatement.setInt(10, equip.getWatk());
                            equipPreparedStatement.setInt(11, equip.getMatk());
                            equipPreparedStatement.setInt(12, equip.getWdef());
                            equipPreparedStatement.setInt(13, equip.getMdef());
                            equipPreparedStatement.setInt(14, equip.getAcc());
                            equipPreparedStatement.setInt(15, equip.getAvoid());
                            equipPreparedStatement.setInt(16, equip.getHands());
                            equipPreparedStatement.setInt(17, equip.getSpeed());
                            equipPreparedStatement.setInt(18, equip.getJump());
                            equipPreparedStatement.setInt(19, equip.getLocked());
                            equipPreparedStatement.setInt(20, equip.getVicious());
                            equipPreparedStatement.setInt(21, equip.getItemLevel());
                            equipPreparedStatement.setInt(22, equip.getItemExp());
                            equipPreparedStatement.setInt(23, equip.getRingId());
                            equipPreparedStatement.addBatch();
                        }
                    }
                }
            }

            equipPreparedStatement.executeBatch();
        }
    }

    public static List<Transaction> getLastTransactions(int userId, int transactionCount) {
        List<Transaction> transactions = new ArrayList<>();
        String selectTransactionsSQL = "SELECT * FROM mapleroot.transactions WHERE user_id = ? ORDER BY transaction_date DESC LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement transactionPreparedStatement = conn.prepareStatement(selectTransactionsSQL)) {

            transactionPreparedStatement.setInt(1, userId);
            transactionPreparedStatement.setInt(2, transactionCount);
            try (ResultSet rs = transactionPreparedStatement.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionId(rs.getInt("transaction_id"));
                    transaction.setUserId(rs.getInt("user_id"));
                    transaction.setTransactionDate(rs.getTimestamp("transaction_date").toInstant());
                    transaction.setBuybackUsed(rs.getBoolean("buyback_used"));
                    transaction.setItems(getTransactionItemsByTransactionId(rs.getInt("transaction_id")));
                    transactions.add(transaction);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    public static Transaction getTransactionById(int transactionId) throws SQLException {
        Transaction transaction = null;
        String selectTransactionSQL = "SELECT * FROM mapleroot.transactions WHERE transaction_id = ?";
        String selectTransactionItemsSQL = "SELECT ti.*, te.upgradeslots, te.level, te.str, te.dex, te.int, te.luk, te.hp, te.mp, te.watk, te.matk, te.wdef, te.mdef, te.acc, te.avoid, te.hands, te.speed, te.jump, te.locked, te.vicious, te.itemlevel, te.itemexp, te.ringid " +
                "FROM mapleroot.transaction_items ti " +
                "LEFT JOIN mapleroot.transaction_equips te ON ti.transaction_item_id = te.transaction_item_id " +
                "WHERE ti.transaction_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement transactionPreparedStatement = conn.prepareStatement(selectTransactionSQL);
             PreparedStatement itemPreparedStatement = conn.prepareStatement(selectTransactionItemsSQL)) {

            transactionPreparedStatement.setInt(1, transactionId);
            try (ResultSet rs = transactionPreparedStatement.executeQuery()) {
                if (rs.next()) {
                    transaction = new Transaction();
                    transaction.setTransactionId(rs.getInt("transaction_id"));
                    transaction.setUserId(rs.getInt("user_id"));
                    transaction.setTransactionDate(rs.getTimestamp("transaction_date").toInstant());
                    transaction.setBuybackUsed(rs.getBoolean("buyback_used"));
                }
            }

            if (transaction != null) {
                itemPreparedStatement.setInt(1, transactionId);
                try (ResultSet rs = itemPreparedStatement.executeQuery()) {
                    List<TransactionItem> items = new ArrayList<>();
                    readTransactionItems(rs, items);
                    transaction.setItems(items);
                }
            }
        }
        return transaction;
    }

    private static List<TransactionItem> getTransactionItemsByTransactionId(int transactionId) throws SQLException {
        List<TransactionItem> items = new ArrayList<>();
        String selectTransactionItemsSQL = "SELECT ti.*, te.upgradeslots, te.level, te.str, te.dex, te.int, te.luk, te.hp, te.mp, te.watk, te.matk, te.wdef, te.mdef, te.acc, te.avoid, te.hands, te.speed, te.jump, te.locked, te.vicious, te.itemlevel, te.itemexp, te.ringid " +
                "FROM mapleroot.transaction_items ti " +
                "LEFT JOIN mapleroot.transaction_equips te ON ti.transaction_item_id = te.transaction_item_id " +
                "WHERE ti.transaction_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(selectTransactionItemsSQL)) {

            preparedStatement.setInt(1, transactionId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                readTransactionItems(rs, items);
            }
        }
        return items;
    }

    private static void readTransactionItems(ResultSet rs, List<TransactionItem> items) throws SQLException {
        while (rs.next()) {
            InventoryType type = InventoryType.getByType(rs.getByte("ti.type"));
            TransactionItem item;

            if (type.equals(InventoryType.EQUIP)) {
                TransactionEquip equip = new TransactionEquip();
                equip.setUpgradeSlots(rs.getInt("te.upgradeslots"));
                equip.setLevel(rs.getByte("te.level"));
                equip.setStr(rs.getShort("te.str"));
                equip.setDex(rs.getShort("te.dex"));
                equip.setIntStat(rs.getShort("te.int"));
                equip.setLuk(rs.getShort("te.luk"));
                equip.setHp(rs.getShort("te.hp"));
                equip.setMp(rs.getShort("te.mp"));
                equip.setWatk(rs.getShort("te.watk"));
                equip.setMatk(rs.getShort("te.matk"));
                equip.setWdef(rs.getShort("te.wdef"));
                equip.setMdef(rs.getShort("te.mdef"));
                equip.setAcc(rs.getShort("te.acc"));
                equip.setAvoid(rs.getShort("te.avoid"));
                equip.setHands(rs.getShort("te.hands"));
                equip.setSpeed(rs.getShort("te.speed"));
                equip.setJump(rs.getShort("te.jump"));
                equip.setLocked(rs.getInt("te.locked"));
                equip.setVicious(rs.getInt("te.vicious"));
                equip.setItemLevel(rs.getByte("te.itemlevel"));
                equip.setItemExp(rs.getInt("te.itemexp"));
                equip.setRingId(rs.getInt("te.ringid"));
                item = equip;
            } else {
                item = new TransactionItem();
            }

            item.setTransactionItemId(rs.getInt("ti.transaction_item_id"));
            item.setType(rs.getInt("ti.type"));
            item.setItemId(rs.getInt("ti.itemid"));
            item.setInventoryType(rs.getInt("ti.inventorytype"));
            item.setPosition(rs.getInt("ti.position"));
            item.setQuantity(rs.getInt("ti.quantity"));
            item.setOwner(rs.getString("ti.owner"));
            item.setPetId(rs.getInt("ti.petid"));
            item.setFlag(rs.getInt("ti.flag"));
            item.setExpiration(rs.getLong("ti.expiration"));
            item.setGiftFrom(rs.getString("ti.giftFrom"));
            items.add(item);
        }
    }


    public static boolean updateTransactionBuybackStatus(int transactionId, boolean buybackUsed) {
        String updateTransactionSQL = "UPDATE mapleroot.transactions SET buyback_used = ? WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(updateTransactionSQL)) {
            preparedStatement.setBoolean(1, buybackUsed);
            preparedStatement.setInt(2, transactionId);
            int affectedRows = preparedStatement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteTransaction(int transactionId) throws SQLException {
        String deleteTransactionSQL = "DELETE FROM mapleroot.transactions WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(deleteTransactionSQL)) {
            preparedStatement.setInt(1, transactionId);
            int affectedRows = preparedStatement.executeUpdate();
            return affectedRows > 0;
        }
    }

    public static boolean deleteTransactionItems(List<Integer> transactionItemIds) throws SQLException {
        String deleteTransactionEquipSQL = "DELETE FROM mapleroot.transaction_equips WHERE transaction_item_id = ?";
        String deleteTransactionItemSQL = "DELETE FROM mapleroot.transaction_items WHERE transaction_item_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement equipPreparedStatement = conn.prepareStatement(deleteTransactionEquipSQL);
             PreparedStatement itemPreparedStatement = conn.prepareStatement(deleteTransactionItemSQL)) {

            for (int transactionItemId : transactionItemIds) {
                equipPreparedStatement.setInt(1, transactionItemId);
                equipPreparedStatement.addBatch();
            }
            equipPreparedStatement.executeBatch();

            for (int transactionItemId : transactionItemIds) {
                itemPreparedStatement.setInt(1, transactionItemId);
                itemPreparedStatement.addBatch();
            }
            int[] affectedRows = itemPreparedStatement.executeBatch();

            for (int count : affectedRows) {
                if (count == 0) {
                    return false;
                }
            }
            return true;
        }
    }
}


