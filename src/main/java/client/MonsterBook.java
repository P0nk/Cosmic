package client;

import tools.DatabaseConnection;
import tools.PacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

    // Add this method to client.MonsterBook class

    public boolean redeemBulk(final Client c, final int statType, final boolean isSpecial, final int quantity) {
        lock.lock();
        System.out.println("[Debug] redeemBulk called. Stat: " + statType + " | Special: " + isSpecial + " | Qty: " + quantity);
        try {
            int redeemedCount = 0;
            int multiplierPerCard = isSpecial ? 10 : 1;

            // We need to collect IDs to update DB later
            java.util.List<Integer> cardsToRedeem = new java.util.ArrayList<>();

            for (Map.Entry<Integer, Integer> entry : cards.entrySet()) {
                // If we have redeemed enough, stop looping
                if (redeemedCount >= quantity) break;

                int cardId = entry.getKey();
                int level = entry.getValue();

                // Check Validity: Level 5, Not Redeemed
                if (level >= 5 && !redeemedCards.contains(cardId)) {
                    // Check Tier: Special (>= 2388) vs Normal (< 2388)
                    boolean cardIsSpecial = (cardId / 1000 >= 2388);

                    if (cardIsSpecial == isSpecial) {
                        cardsToRedeem.add(cardId);
                        redeemedCount++;
                    }
                }
            }

            System.out.println("[Debug] Found " + redeemedCount + " valid cards to redeem.");

            if (redeemedCount < quantity) {
                System.out.println("[Debug] Not enough cards found. Aborting.");
                return false; // Should not happen if Client checks correctly, but safety first
            }

            // Apply Stats
            int totalMultiplier = multiplierPerCard * quantity;

            switch (statType) {
                case 0: giveWatk(c, 2 * totalMultiplier); break;
                case 1: giveMatk(c, 1 * totalMultiplier); break;
                case 2: giveAcc(c, 2 * totalMultiplier); break;
                case 3: giveMDef(c, 5 * totalMultiplier); break;
                case 4: giveWDef(c, 5 * totalMultiplier); break;
                case 5: giveEva(c, 3 * totalMultiplier); break;
                default: return false;
            }

            // Mark as Redeemed in Memory and DB
            for (Integer cid : cardsToRedeem) {
                redeemedCards.add(cid);
                saveRedemptionStatus(c.getPlayer().getId(), cid);
            }

            // Visuals
//            c.sendPacket(PacketCreator.showSpecialEffect(12));
//            c.sendPacket(PacketCreator.playSound("Game/Quest/Party1"));

            return true;

        } catch (Exception e) {
            System.err.println("[MonsterBook] Error in redeemBulk");
            e.printStackTrace();
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void addCard(final Client c, final int cardid) {
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PacketCreator.showForeignCardEffect(c.getPlayer().getId()), false);

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
            if (qty == 0) {
                calculateLevel();
            }
            c.sendPacket(PacketCreator.addCard(false, cardid, qty + 1));
            c.sendPacket(PacketCreator.showGainCard());
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
        final String query = """
                INSERT INTO monsterbook (charid, cardid, level, redeemed)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE level = ?, redeemed = ?;
                """;
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

    // --- REDEMPTION SYSTEM START ---

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

            // Check if card is special (Boss/Rare) and apply multiplier if desired.
            // Currently using 1x multiplier for normal cards as per your base values.
            boolean isSpecial = (cardid / 1000 >= 2388);
            int multiplier = isSpecial ? 10 : 1;

            switch (type) {
                case 0: // +2 Watk
                    giveWatk(c, 2 * multiplier);
                    break;
                case 1: // +1 Matk
                    giveMatk(c, 1 * multiplier);
                    break;
                case 2: // +2 Acc
                    giveAcc(c, 2 * multiplier);
                    break;
                case 3: // +5 MDef
                    giveMDef(c, 5 * multiplier);
                    break;
                case 4: // +5 WDef
                    giveWDef(c, 5 * multiplier);
                    break;
                case 5: // +3 Eva
                    giveEva(c, 3 * multiplier);
                    break;
                default:
                    return false;
            }

            redeemedCards.add(cardid);

            // Visuals & Sound
            c.sendPacket(PacketCreator.showSpecialEffect(12)); // Quest Clear Effect
            c.sendPacket(PacketCreator.playSound("Game/Quest/Party1")); // Success Sound

            // Save the specific card redemption status to DB
            saveRedemptionStatus(c.getPlayer().getId(), cardid);
            return true;

        } catch (Exception e) {
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

    // --- STAT UPDATING METHODS (BACKEND) ---

    private void giveWatk(Client c, int amount) {
        Character player = c.getPlayer();
        // 1. Get Current
        int current = player.getPassiveWatk();
        // 2. Calculate New
        int newStat = current + amount;
        // 3. Set New (Update)
        player.setPassiveWatk(newStat);
        // 4. Save to DB
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

    // --- REDEMPTION SYSTEM END ---

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