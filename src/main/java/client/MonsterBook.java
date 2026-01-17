package client;

import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import tools.DatabaseConnection;
import tools.PacketCreator;

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
    private final Set<Integer> redeemedCards = new HashSet<>();
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
        // 1. Check if Book is Full
        lock.lock();
        try {
            if (cards.getOrDefault(cardid, 0) >= 5) {
                // If book is full, we DO NOT add the card.
                // The pickup logic in Character.java will handle the "Item Unavailable" message
                // if we return without doing anything?
                // Actually, just notify the user.
                c.getPlayer().dropMessage(5, "You have already collected 5 of this card.");
                return;
            }
        } finally {
            lock.unlock();
        }

        // [FIXED] Removed "InventoryManipulator.removeById"
        // Why: When picking up a card, it is not in the inventory yet.
        // Trying to remove it causes an error -> function returns -> Card never added.

        // 3. Update Book Logic
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PacketCreator.showForeignCardEffect(c.getPlayer().getId()), false);

        Integer oldQty;
        lock.lock();
        try {
            oldQty = cards.get(cardid);

            if (oldQty != null) {
                if (oldQty < 5) {
                    cards.put(cardid, oldQty + 1);
                }
            } else {
                cards.put(cardid, 1);
                oldQty = 0;

                if (cardid / 1000 >= 2388) {
                    specialCard++;
                } else {
                    normalCard++;
                }
            }
        } finally {
            lock.unlock();
        }

        // 4. Send Success Packet
        if (oldQty < 5) {
            if (oldQty == 0) {
                calculateLevel();
            }
            c.sendPacket(PacketCreator.addCard(false, cardid, oldQty + 1));
            c.sendPacket(PacketCreator.showGainCard());
            c.getPlayer().dropMessage(5, "Monster Card added to your book.");
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

            bookLevel = level;
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

    public boolean isRedeemed(int cardId) {
        lock.lock();
        try {
            return redeemedCards.contains(cardId);
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

    public void loadCards(final int charid) throws SQLException {
        lock.lock();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT cardid, level, redeemed FROM monsterbook WHERE charid = ? ORDER BY cardid ASC")) {
            ps.setInt(1, charid);

            try (ResultSet rs = ps.executeQuery()) {
                int cardid;
                int level;
                boolean redeemed;
                while (rs.next()) {
                    cardid = rs.getInt("cardid");
                    level = rs.getInt("level");
                    redeemed = rs.getInt("redeemed") == 1;

                    if (cardid / 1000 >= 2388) {
                        specialCard++;
                    } else {
                        normalCard++;
                    }
                    cards.put(cardid, level);
                    if (redeemed) {
                        redeemedCards.add(cardid);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        calculateLevel();
    }

    public void saveCards(Connection con, int chrId) throws SQLException {
        final String query = "INSERT INTO monsterbook (charid, cardid, level, redeemed) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE level = ?, redeemed = ?";
        try (final PreparedStatement ps = con.prepareStatement(query)) {
            lock.lock();
            try {
                for (Map.Entry<Integer, Integer> cardAndLevel : cards.entrySet()) {
                    final int card = cardAndLevel.getKey();
                    final int level = cardAndLevel.getValue();
                    final int redeemed = redeemedCards.contains(card) ? 1 : 0;

                    ps.setInt(1, chrId);
                    ps.setInt(2, card);
                    ps.setInt(3, level);
                    ps.setInt(4, redeemed);
                    ps.setInt(5, level);
                    ps.setInt(6, redeemed);

                    ps.addBatch();
                }
            } finally {
                lock.unlock();
            }
            ps.executeBatch();
        }
    }

    public boolean redeemCard(final Client c, final int cardid, final int type) {
        lock.lock();
        try {
            Integer level = cards.get(cardid);
            if (level == null || level < 5) {
                return false;
            }
            if (redeemedCards.contains(cardid)) {
                return false;
            }

            boolean isSpecial = (cardid / 1000 >= 2388);
            int multiplier = isSpecial ? 10 : 1;

            switch (type) {
                case 0 -> giveWatk(c, 2 * multiplier);
                case 1 -> giveMatk(c, 1 * multiplier);
                case 2 -> giveAcc(c, 2 * multiplier);
                case 3 -> giveMDef(c, 5 * multiplier);
                case 4 -> giveWDef(c, 5 * multiplier);
                case 5 -> giveEva(c, 3 * multiplier);
                default -> { return false; }
            }

            redeemedCards.add(cardid);

            c.sendPacket(PacketCreator.showSpecialEffect(12));
            c.sendPacket(PacketCreator.playSound("Game/Quest/Party1"));

            saveRedemptionStatus(c.getPlayer().getId(), cardid);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean redeemBulk(final Client c, final int statType, final boolean isSpecial, final int quantity) {
        lock.lock();
        try {
            int redeemedCount = 0;
            int multiplierPerCard = isSpecial ? 10 : 1;

            List<Integer> cardsToRedeem = new ArrayList<>();

            for (Map.Entry<Integer, Integer> entry : cards.entrySet()) {
                if (redeemedCount >= quantity) break;

                int cardId = entry.getKey();
                int level = entry.getValue();

                if (level >= 5 && !redeemedCards.contains(cardId)) {
                    boolean cardIsSpecial = (cardId / 1000 >= 2388);

                    if (cardIsSpecial == isSpecial) {
                        cardsToRedeem.add(cardId);
                        redeemedCount++;
                    }
                }
            }

            if (redeemedCount < quantity) {
                return false;
            }

            int totalMultiplier = multiplierPerCard * quantity;

            switch (statType) {
                case 0 -> giveWatk(c, 2 * totalMultiplier);
                case 1 -> giveMatk(c, 1 * totalMultiplier);
                case 2 -> giveAcc(c, 2 * totalMultiplier);
                case 3 -> giveMDef(c, 5 * totalMultiplier);
                case 4 -> giveWDef(c, 5 * totalMultiplier);
                case 5 -> giveEva(c, 3 * totalMultiplier);
                default -> { return false; }
            }

            for (Integer cid : cardsToRedeem) {
                redeemedCards.add(cid);
                saveRedemptionStatus(c.getPlayer().getId(), cid);
            }

            return true;

        } catch (Exception e) {
            System.err.println("[MonsterBook] Error in redeemBulk");
            e.printStackTrace();
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void saveRedemptionStatus(int charId, int cardId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE monsterbook SET redeemed = 1 WHERE charid = ? AND cardid = ?")) {
            ps.setInt(1, charId);
            ps.setInt(2, cardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[MonsterBook] Error saving redemption status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void giveWatk(Client c, int amount) {
        Character player = c.getPlayer();
        int current = player.getPassiveWatk();
        int newStat = current + amount;
        player.setPassiveWatk(newStat);
        player.saveCharToDB();
        player.dropMessage(5, "Redeemed " + amount + " Weapon Attack! (Total: " + newStat + ")");
    }

    private void giveMatk(Client c, int amount) {
        Character player = c.getPlayer();
        int current = player.getPassiveMatk();
        int newStat = current + amount;
        player.setPassiveMatk(newStat);
        player.saveCharToDB();
        player.dropMessage(5, "Redeemed " + amount + " Magic Attack! (Total: " + newStat + ")");
    }

    private void giveWDef(Client c, int amount) {
        Character player = c.getPlayer();
        int current = player.getPassiveWdef();
        int newStat = current + amount;
        player.setPassiveWdef(newStat);
        player.saveCharToDB();
        player.dropMessage(5, "Redeemed " + amount + " Weapon Def! (Total: " + newStat + ")");
    }

    private void giveMDef(Client c, int amount) {
        Character player = c.getPlayer();
        int current = player.getPassiveMdef();
        int newStat = current + amount;
        player.setPassiveMdef(newStat);
        player.saveCharToDB();
        player.dropMessage(5, "Redeemed " + amount + " Magic Def! (Total: " + newStat + ")");
    }

    private void giveEva(Client c, int amount) {
        Character player = c.getPlayer();
        int current = player.getPassiveEva();
        int newStat = current + amount;
        player.setPassiveEva(newStat);
        player.saveCharToDB();
        player.dropMessage(5, "Redeemed " + amount + " Avoidability! (Total: " + newStat + ")");
    }

    private void giveAcc(Client c, int amount) {
        Character player = c.getPlayer();
        int current = player.getPassiveAcc();
        int newStat = current + amount;
        player.setPassiveAcc(newStat);
        player.saveCharToDB();
        player.dropMessage(5, "Redeemed " + amount + " Accuracy! (Total: " + newStat + ")");
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
}