/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client.inventory;

import tools.DatabaseConnection;
import tools.Pair;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Flav
 */
public enum ItemFactory {

    INVENTORY(1, false),
    STORAGE(2, true),
    CASH_EXPLORER(3, true),
    CASH_CYGNUS(4, true),
    CASH_ARAN(5, true),
    MERCHANT(6, false),
    CASH_OVERALL(7, true),
    MARRIAGE_GIFTS(8, false),
    DUEY(9, false),
    OREBAG(10, true);
    private final int value;
    private final boolean account;

    private static final int lockCount = 400;
    private static final Lock[] locks = new Lock[lockCount];  // thanks Masterrulax for pointing out a bottleneck issue here

    static {
        for (int i = 0; i < lockCount; i++) {
            locks[i] = new ReentrantLock(true);
        }
    }

    ItemFactory(int value, boolean account) {
        this.value = value;
        this.account = account;
    }

    public int getValue() {
        return value;
    }

    public List<Pair<Item, InventoryType>> loadItems(int id, boolean login) throws SQLException {
        return value != 6
                ? loadItemsCommon(id, login)
                : loadItemsMerchant(id, login);
    }

    public void saveItems(List<Pair<Item, InventoryType>> items, int id, Connection con) throws SQLException {
        saveItems(items, null, id, con);
    }

    public void saveItems(List<Pair<Item, InventoryType>> items, List<Short> bundlesList, int id, Connection con) throws SQLException {
        // thanks Arufonsu, MedicOP, BHB for pointing a "synchronized" bottleneck here

        if (value != 6) {
            saveItemsCommon(items, id, con);
        } else {
            saveItemsMerchant(items, bundlesList, id, con);
        }
    }

    private static Equip loadEquipFromResultSet(ResultSet rs) throws SQLException {
        Equip equip = new Equip(rs.getInt("itemid"), (short) rs.getInt("position"));
        equip.setOwner(rs.getString("owner"));
        equip.setQuantity((short) rs.getInt("quantity"));
        equip.setAcc((short) rs.getInt("acc"));
        equip.setAvoid((short) rs.getInt("avoid"));
        equip.setDex((short) rs.getInt("dex"));
        equip.setHands((short) rs.getInt("hands"));
        equip.setHp((short) rs.getInt("hp"));
        equip.setInt((short) rs.getInt("int"));
        equip.setJump((short) rs.getInt("jump"));
        equip.setVicious((short) rs.getInt("vicious"));
        equip.setFlag((short) rs.getInt("flag"));
        equip.setLuk((short) rs.getInt("luk"));
        equip.setMatk((short) rs.getInt("matk"));
        equip.setMdef((short) rs.getInt("mdef"));
        equip.setMp((short) rs.getInt("mp"));
        equip.setSpeed((short) rs.getInt("speed"));
        equip.setStr((short) rs.getInt("str"));
        equip.setWatk((short) rs.getInt("watk"));
        equip.setWdef((short) rs.getInt("wdef"));
        equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
        equip.setLevel(rs.getByte("level"));
        equip.setItemExp(rs.getInt("itemexp"));
        equip.setItemLevel(rs.getByte("itemlevel"));
        equip.setExpiration(rs.getLong("expiration"));
        equip.setGiftFrom(rs.getString("giftFrom"));
        equip.setRingId(rs.getInt("ringid"));

        return equip;
    }

    public static List<Pair<Item, Integer>> loadEquippedItems(int id, boolean isAccount, boolean login) throws SQLException {
        List<Pair<Item, Integer>> items = new ArrayList<>();

        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ");
        query.append("(SELECT id, accountid FROM characters) AS accountterm ");
        query.append("RIGHT JOIN ");
        query.append("(SELECT * FROM (`inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`))) AS equipterm");
        query.append(" ON accountterm.id=equipterm.characterid ");
        query.append("WHERE accountterm.`");
        query.append(isAccount ? "accountid" : "characterid");
        query.append("` = ?");
        query.append(login ? " AND `inventorytype` = " + InventoryType.EQUIPPED.getType() : "");

        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(query.toString())) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Integer cid = rs.getInt("characterid");
                        items.add(new Pair<>(loadEquipFromResultSet(rs), cid));
                    }
                }
            }
        }

        return items;
    }

    private List<Pair<Item, InventoryType>> loadItemsCommon(int id, boolean login) throws SQLException {
        List<Pair<Item, InventoryType>> items = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection()) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`) WHERE `type` = ? AND `");
            query.append(account ? "accountid" : "characterid").append("` = ?");

            if (login) {
                query.append(" AND `inventorytype` = ").append(InventoryType.EQUIPPED.getType());
            }

            try (PreparedStatement ps = con.prepareStatement(query.toString())) {
                ps.setInt(1, value);
                ps.setInt(2, id);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        InventoryType mit = InventoryType.getByType(rs.getByte("inventorytype"));

                        if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                            items.add(new Pair<>(loadEquipFromResultSet(rs), mit));
                        } else {
                            int petid = rs.getInt("petid");
                            if (rs.wasNull()) {
                                petid = -1;
                            }

                            Item item = new Item(rs.getInt("itemid"), (byte) rs.getInt("position"), (short) rs.getInt("quantity"), petid);
                            item.setOwner(rs.getString("owner"));
                            item.setExpiration(rs.getLong("expiration"));
                            item.setGiftFrom(rs.getString("giftFrom"));
                            item.setFlag((short) rs.getInt("flag"));
                            items.add(new Pair<>(item, mit));
                        }
                    }
                }
            }
        }
        return items;
    }

    private void saveItemsCommon(List<Pair<Item, InventoryType>> items, int id, Connection con) throws SQLException {
        Lock lock = locks[id % lockCount];
        lock.lock();
        try {
            StringBuilder query = new StringBuilder();
            query.append("DELETE `inventoryitems`, `inventoryequipment` FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`) WHERE `type` = ? AND `");
            query.append(account ? "accountid" : "characterid").append("` = ?");

            try (PreparedStatement ps = con.prepareStatement(query.toString())) {
                ps.setInt(1, value);
                ps.setInt(2, id);
                ps.executeUpdate();
            }

            try (PreparedStatement psItem = con.prepareStatement("INSERT INTO `inventoryitems` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                if (!items.isEmpty()) {
                    for (Pair<Item, InventoryType> pair : items) {
                        Item item = pair.getLeft();
                        InventoryType mit = pair.getRight();
                        psItem.setInt(1, value);
                        psItem.setString(2, account ? null : String.valueOf(id));
                        psItem.setString(3, account ? String.valueOf(id) : null);
                        psItem.setInt(4, item.getItemId());
                        psItem.setInt(5, mit.getType());
                        psItem.setInt(6, item.getPosition());
                        psItem.setInt(7, item.getQuantity());
                        psItem.setString(8, item.getOwner());
                        psItem.setInt(9, item.getPetId());      // thanks Daddy Egg for alerting a case of unique petid constraint breach getting raised
                        psItem.setInt(10, item.getFlag());
                        psItem.setLong(11, item.getExpiration());
                        psItem.setString(12, item.getGiftFrom());
                        psItem.executeUpdate();

                        if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                            try (PreparedStatement psEquip = con.prepareStatement("INSERT INTO `inventoryequipment` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                                try (ResultSet rs = psItem.getGeneratedKeys()) {
                                    if (!rs.next()) {
                                        throw new RuntimeException("Inserting item failed.");
                                    }

                                    psEquip.setInt(1, rs.getInt(1));
                                }

                                Equip equip = (Equip) item;
                                psEquip.setInt(2, equip.getUpgradeSlots());
                                psEquip.setInt(3, equip.getLevel());
                                psEquip.setInt(4, equip.getStr());
                                psEquip.setInt(5, equip.getDex());
                                psEquip.setInt(6, equip.getInt());
                                psEquip.setInt(7, equip.getLuk());
                                psEquip.setInt(8, equip.getHp());
                                psEquip.setInt(9, equip.getMp());
                                psEquip.setInt(10, equip.getWatk());
                                psEquip.setInt(11, equip.getMatk());
                                psEquip.setInt(12, equip.getWdef());
                                psEquip.setInt(13, equip.getMdef());
                                psEquip.setInt(14, equip.getAcc());
                                psEquip.setInt(15, equip.getAvoid());
                                psEquip.setInt(16, equip.getHands());
                                psEquip.setInt(17, equip.getSpeed());
                                psEquip.setInt(18, equip.getJump());
                                psEquip.setInt(19, 0);
                                psEquip.setInt(20, equip.getVicious());
                                psEquip.setInt(21, equip.getItemLevel());
                                psEquip.setInt(22, equip.getItemExp());
                                psEquip.setInt(23, equip.getRingId());
                                psEquip.executeUpdate();
                            }
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private List<Pair<Item, InventoryType>> loadItemsMerchant(int id, boolean login) throws SQLException {
        List<Pair<Item, InventoryType>> items = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection()) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`) WHERE `type` = ? AND `");
            query.append(account ? "accountid" : "characterid").append("` = ?");

            if (login) {
                query.append(" AND `inventorytype` = ").append(InventoryType.EQUIPPED.getType());
            }

            try (PreparedStatement ps = con.prepareStatement(query.toString())) {
                ps.setInt(1, value);
                ps.setInt(2, id);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        short bundles = 0;
                        try (PreparedStatement psBundle = con.prepareStatement("SELECT `bundles` FROM `inventorymerchant` WHERE `inventoryitemid` = ?")) {
                            psBundle.setInt(1, rs.getInt("inventoryitemid"));

                            try (ResultSet rs2 = psBundle.executeQuery()) {
                                if (rs2.next()) {
                                    bundles = rs2.getShort("bundles");
                                }
                            }
                        }

                        InventoryType mit = InventoryType.getByType(rs.getByte("inventorytype"));

                        if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                            items.add(new Pair<>(loadEquipFromResultSet(rs), mit));
                        } else {
                            if (bundles > 0) {
                                int petid = rs.getInt("petid");
                                if (rs.wasNull()) {
                                    petid = -1;
                                }

                                Item item = new Item(rs.getInt("itemid"), (byte) rs.getInt("position"), (short) (bundles * rs.getInt("quantity")), petid);
                                item.setOwner(rs.getString("owner"));
                                item.setExpiration(rs.getLong("expiration"));
                                item.setGiftFrom(rs.getString("giftFrom"));
                                item.setFlag((short) rs.getInt("flag"));
                                items.add(new Pair<>(item, mit));
                            }
                        }
                    }
                }
            }
        }
        return items;
    }

    private void saveItemsMerchant(List<Pair<Item, InventoryType>> items, List<Short> bundlesList, int id, Connection con) throws SQLException {
        Lock lock = locks[id % lockCount];
        lock.lock();
        try {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM `inventorymerchant` WHERE `characterid` = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            StringBuilder query = new StringBuilder();
            query.append("DELETE `inventoryitems`, `inventoryequipment` FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`) WHERE `type` = ? AND `");
            query.append(account ? "accountid" : "characterid").append("` = ?");

            try (PreparedStatement ps = con.prepareStatement(query.toString())) {
                ps.setInt(1, value);
                ps.setInt(2, id);
                ps.executeUpdate();
            }

            int i = 0;
            for (Pair<Item, InventoryType> pair : items) {
                final Item item = pair.getLeft();
                final Short bundles = bundlesList.get(i);
                final InventoryType mit = pair.getRight();
                i++;

                final int genKey;
                // Item
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO `inventoryitems` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, value);
                    ps.setString(2, account ? null : String.valueOf(id));
                    ps.setString(3, account ? String.valueOf(id) : null);
                    ps.setInt(4, item.getItemId());
                    ps.setInt(5, mit.getType());
                    ps.setInt(6, item.getPosition());
                    ps.setInt(7, item.getQuantity());
                    ps.setString(8, item.getOwner());
                    ps.setInt(9, item.getPetId());
                    ps.setInt(10, item.getFlag());
                    ps.setLong(11, item.getExpiration());
                    ps.setString(12, item.getGiftFrom());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new RuntimeException("Inserting item failed.");
                        }

                        genKey = rs.getInt(1);
                    }
                }

                // Merchant
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO `inventorymerchant` VALUES (DEFAULT, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, genKey);
                    ps.setInt(2, id);
                    ps.setInt(3, bundles);
                    ps.executeUpdate();
                }

                // Equipment
                if (mit.equals(InventoryType.EQUIP) || mit.equals(InventoryType.EQUIPPED)) {
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO `inventoryequipment` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, genKey);

                        Equip equip = (Equip) item;
                        ps.setInt(2, equip.getUpgradeSlots());
                        ps.setInt(3, equip.getLevel());
                        ps.setInt(4, equip.getStr());
                        ps.setInt(5, equip.getDex());
                        ps.setInt(6, equip.getInt());
                        ps.setInt(7, equip.getLuk());
                        ps.setInt(8, equip.getHp());
                        ps.setInt(9, equip.getMp());
                        ps.setInt(10, equip.getWatk());
                        ps.setInt(11, equip.getMatk());
                        ps.setInt(12, equip.getWdef());
                        ps.setInt(13, equip.getMdef());
                        ps.setInt(14, equip.getAcc());
                        ps.setInt(15, equip.getAvoid());
                        ps.setInt(16, equip.getHands());
                        ps.setInt(17, equip.getSpeed());
                        ps.setInt(18, equip.getJump());
                        ps.setInt(19, 0);
                        ps.setInt(20, equip.getVicious());
                        ps.setInt(21, equip.getItemLevel());
                        ps.setInt(22, equip.getItemExp());
                        ps.setInt(23, equip.getRingId());
                        ps.executeUpdate();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }
}