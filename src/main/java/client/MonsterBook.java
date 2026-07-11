/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

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
package client;

import tools.DatabaseConnection;
import tools.PacketCreator;

//Custom for monsterbook
import client.inventory.Equip;
import client.inventory.Inventory;
import client.inventory.InventoryType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class MonsterBook {
    private int specialCard = 0;
    private int normalCard = 0;
    private int bookLevel = 1;
    private final Map<Integer, Integer> cards = new LinkedHashMap<>();
    private final Lock lock = new ReentrantLock();

    public Set<Entry<Integer, Integer>> getCardSet() {
        lock.lock();
        try {
            return new HashSet<>(cards.entrySet());
        } finally {
            lock.unlock();
        }
    }

    public void addCard(final Client c, final int cardid) {
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PacketCreator.showForeignCardEffect(c.getPlayer().getId()), false);
        int MobItem = 1113232;
        Inventory equipped = c.getPlayer().getInventory(InventoryType.EQUIPPED);
        Equip eq = (Equip) equipped.findById(MobItem);

        if (eq == null) {
            // If not found in EQUIPPED, search in EQUIP inventory
            Inventory equipInventory = c.getPlayer().getInventory(InventoryType.EQUIP);
            eq = (Equip) equipInventory.findById(MobItem);
        }

        Integer qty;
        lock.lock();
        try {
            qty = cards.get(cardid);

            if (qty != null) {
                if (qty < 5) {
                    cards.put(cardid, qty + 1);
                }
            } else {
                cards.put(cardid, 1);
                qty = 0;

                if (cardid / 1000 >= 2388) {
                    specialCard++;
                } else {
                    normalCard++;
                }
            }
        } finally {
            lock.unlock();
        }

        if (qty < 5) {
            if (qty == 0) {     // leveling system only accounts unique cards
                calculateLevel();
            }

            c.sendPacket(PacketCreator.addCard(false, cardid, qty + 1));
            c.sendPacket(PacketCreator.showGainCard());
            if (isTier1(cardid)) { //Tier 1
                if(cards.get(cardid) == 5){
                    c.getPlayer().AddTier1(1);
                    c.getPlayer().UpdateTier1();
                    c.getPlayer().forceUpdateItem(eq);
                    c.getPlayer().applyLinkStatsBoost();
                    c.getPlayer().dropMessage("Tier 1 card completed");
                }
            }
            if (isTier2(cardid)) { //Tier 2
                if(cards.get(cardid) == 5){
                    c.getPlayer().AddTier2(1);
                    c.getPlayer().UpdateTier2();
                    c.getPlayer().forceUpdateItem(eq);
                    c.getPlayer().applyLinkStatsBoost();
                    c.getPlayer().dropMessage("Tier 2 card completed");
                }
            }
            if (isTier3(cardid)) { //Tier 3
                if(cards.get(cardid) == 5){
                    c.getPlayer().AddTier3(1);
                    c.getPlayer().UpdateTier3();
                    c.getPlayer().forceUpdateItem(eq);
                    c.getPlayer().applyLinkStatsBoost();
                    c.getPlayer().dropMessage("Tier 3 card completed");
                }
            }
            if (isTier4(cardid)) { //Tier 4
                if(cards.get(cardid) == 5){
                    c.getPlayer().AddTier4(1);
                    c.getPlayer().UpdateTier4();
                    c.getPlayer().forceUpdateItem(eq);
                    c.getPlayer().applyLinkStatsBoost();
                    c.getPlayer().dropMessage("Tier 4 card completed");
                }
            }
            if (isTier5(cardid)) { //Tier 5
                if(cards.get(cardid) == 5){
                    c.getPlayer().AddTier5(1);
                    c.getPlayer().UpdateTier5();
                    c.getPlayer().forceUpdateItem(eq);
                    c.getPlayer().applyLinkStatsBoost();
                    c.getPlayer().dropMessage("Tier 5 card completed");
                }
            }
            if (isTier6(cardid)) { //Tier 6
                if(cards.get(cardid) == 5){
                    c.getPlayer().AddTier6(1);
                    c.getPlayer().UpdateTier6();
                    c.getPlayer().forceUpdateItem(eq);
                    c.getPlayer().applyLinkStatsBoost();
                    c.getPlayer().dropMessage("Tier 6 card completed");
                }
            }
            if (isTier7(cardid)) { //Tier 7
                if(cards.get(cardid) == 5){
                    c.getPlayer().AddTier7(1);
                    c.getPlayer().UpdateTier7();
                    c.getPlayer().forceUpdateItem(eq);
                    c.getPlayer().applyLinkStatsBoost();
                    c.getPlayer().dropMessage("Tier 7 card completed");
                }
            }
            if (isTier8(cardid)) { //Tier 8
                if(cards.get(cardid) == 5){
                    c.getPlayer().AddTier8(1);
                    c.getPlayer().UpdateTier8();
                    c.getPlayer().forceUpdateItem(eq);
                    c.getPlayer().applyLinkStatsBoost();
                    c.getPlayer().dropMessage("Tier 8 card completed");
                }
            }
            if (isTier9(cardid)) { //Tier 9
                if(cards.get(cardid) == 5){
                    c.getPlayer().AddTier9(1);
                    c.getPlayer().UpdateTier9();
                    c.getPlayer().forceUpdateItem(eq);
                    c.getPlayer().applyLinkStatsBoost();
                    c.getPlayer().dropMessage("Tier 9 card completed");
                }
            }
        } else {
            c.sendPacket(PacketCreator.addCard(true, cardid, 5));
        }
    }

    private void calculateLevel() {
        lock.lock();
        try {
            int collectionExp = (normalCard + specialCard);

            int level = 0, expToNextlevel = 1;
            do {
                level++;
                expToNextlevel += level * 10;
            } while (collectionExp >= expToNextlevel);

            bookLevel = level;  // thanks IxianMace for noticing book level differing between book UI and character info UI
        } finally {
            lock.unlock();
        }
    }

    public int getBookLevel() {
        lock.lock();
        try {
            return bookLevel;
        } finally {
            lock.unlock();
        }
    }

    public Map<Integer, Integer> getCards() {
        lock.lock();
        try {
            return Collections.unmodifiableMap(cards);
        } finally {
            lock.unlock();
        }
    }

    public int getTotalCards() {
        lock.lock();
        try {
            return specialCard + normalCard;
        } finally {
            lock.unlock();
        }
    }

    public int getNormalCard() {
        lock.lock();
        try {
            return normalCard;
        } finally {
            lock.unlock();
        }
    }

    public int getSpecialCard() {
        lock.lock();
        try {
            return specialCard;
        } finally {
            lock.unlock();
        }
    }

    public void loadCards(final int accId) throws SQLException {
        lock.lock();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT cardid, level FROM monsterbook WHERE accountid = ? ORDER BY cardid ASC")) {
            ps.setInt(1, accId);

            try (ResultSet rs = ps.executeQuery()) {
                int cardid;
                int level;
                while (rs.next()) {
                    cardid = rs.getInt("cardid");
                    level = rs.getInt("level");
                    if (cardid / 1000 >= 2388) {
                        specialCard++;
                    } else {
                        normalCard++;
                    }
                    cards.put(cardid, level);
                }
            }
        } finally {
            lock.unlock();
        }

        calculateLevel();
    }

    public void saveCards(Connection con, int accId) throws SQLException {
        final String query = """
                INSERT INTO monsterbook (accountid, cardid, level)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE level = VALUES(level);
                """;
        try (final PreparedStatement ps = con.prepareStatement(query)) {
            for (Map.Entry<Integer, Integer> cardAndLevel : cards.entrySet()) {
                final int card = cardAndLevel.getKey();
                final int level = cardAndLevel.getValue();
                // insert
                ps.setInt(1, accId);
                ps.setInt(2, card);
                ps.setInt(3, level);

                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static int[] getCardTierSize() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM monstercarddata GROUP BY floor(cardid / 1000);", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = ps.executeQuery()) {
            rs.last();
            int[] tierSizes = new int[rs.getRow()];
            rs.beforeFirst();

            while (rs.next()) {
                tierSizes[rs.getRow() - 1] = rs.getInt(1);
            }

            return tierSizes;
        } catch (SQLException e) {
            e.printStackTrace();
            return new int[0];
        }
    }

    //Tiers Boolean so you could actually remove the cards you dont need to be counted...
    public static boolean isTier1(int CardID) {
        return CardID >= 2380000 && CardID <= 2380019;
    }
    public static boolean isTier2(int CardID) {
        return CardID >= 2381000 && CardID <= 2381083;
    }
         
    public static boolean isTier3(int CardID) {
        return CardID >= 2382000 && CardID <= 2382096;
    }
    public static boolean isTier4(int CardID) {
        return CardID >= 2383000 && CardID <= 2383059;
    }
    public static boolean isTier5(int CardID) {
        return CardID >= 2384000 && CardID <= 2384040;
    }
    
    public static boolean isTier6(int CardID) {
        return CardID >= 2385000 && CardID <= 2385025;
    }
    public static boolean isTier7(int CardID) {
        return CardID >= 2386000 && CardID <= 2386524;
    }
    public static boolean isTier8(int CardID) {
        return CardID >= 2387000 && CardID <= 2387600;
    }
    public static boolean isTier9(int CardID) {
        return CardID >= 2388000 && CardID <= 2388600;
    }
}
