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
        System.out.println("[DEBUG] redeemCard started. CardID: " + cardid + " Type: " + type); // DEBUG 1

        lock.lock();
        try {
            Integer level = cards.get(cardid);
            System.out.println("[DEBUG] Card Level fetched: " + level); // DEBUG 2

            if (level == null || level < 5) {
                System.out.println("[DEBUG] returning false (Level < 5 or null)"); // DEBUG 3
                return false;
            }
            if (redeemedCards.contains(cardid)) {
                System.out.println("[DEBUG] returning false (Already redeemed)"); // DEBUG 4
                return false;
            }

            boolean isSpecial = (cardid / 1000 >= 2388);
            int multiplier = isSpecial ? 10 : 1;
            System.out.println("[DEBUG] isSpecial: " + isSpecial + " Multiplier: " + multiplier); // DEBUG 5

            System.out.println("[DEBUG] Applying Stats via switch..."); // DEBUG 6
            switch (type) {
                case 0:
                    System.out.println("[DEBUG] Giving Watk..."); // DEBUG 7a
                    giveWatk(c, 2 * multiplier);
                    break;
                case 1:
                    System.out.println("[DEBUG] Giving Matk..."); // DEBUG 7b
                    giveMatk(c, 1 * multiplier);
                    break;
                case 2:
                    System.out.println("[DEBUG] Giving Def..."); // DEBUG 7c
                    giveDef(c, 5 * multiplier);
                    break;
                case 3:
                    System.out.println("[DEBUG] Giving Eva..."); // DEBUG 7d
                    giveEva(c, 4 * multiplier);
                    break;
                case 4:
                    System.out.println("[DEBUG] Giving Acc..."); // DEBUG 7e
                    giveAcc(c, 1 * multiplier);
                    break;
                default:
                    System.out.println("[DEBUG] Default switch case (false)"); // DEBUG 7f
                    return false;
            }

            System.out.println("[DEBUG] Stats applied. Adding to set..."); // DEBUG 8
            redeemedCards.add(cardid);

            // Visuals & Sound
            System.out.println("[DEBUG] Sending packets..."); // DEBUG 9
            c.sendPacket(PacketCreator.showSpecialEffect(12)); // Quest Clear Effect
            c.sendPacket(PacketCreator.playSound("Game/Quest/Party1")); // Success Sound

            // Immediate Save
            System.out.println("[DEBUG] Saving Redemption Status to DB..."); // DEBUG 10
            saveRedemptionStatus(c.getPlayer().getId(), cardid);
            System.out.println("[DEBUG] Save Complete."); // DEBUG 11

            return true;
        } catch (Exception e) { // Catch ANY crash to print trace
            System.err.println("[CRITICAL DEBUG] Crash detected inside redeemCard!");
            e.printStackTrace();
            throw e;
        } finally {
            lock.unlock();
            System.out.println("[DEBUG] Lock released."); // DEBUG 12
        }
    }

    private void saveRedemptionStatus(int charId, int cardId) {
        System.out.println("[DEBUG] Inside saveRedemptionStatus. CID: " + charId + " Card: " + cardId); // DEBUG 13
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE monsterbook SET redeemed = 1 WHERE charid = ? AND cardid = ?")) {
            System.out.println("[DEBUG] DB Connection obtained. Preparing statement..."); // DEBUG 14
            ps.setInt(1, charId);
            ps.setInt(2, cardId);
            int rows = ps.executeUpdate();
            System.out.println("[DEBUG] Query executed. Rows updated: " + rows); // DEBUG 15
        } catch (SQLException e) {
            System.err.println("[MonsterBook] Error saving redemption status: " + e.getMessage());
            e.printStackTrace(); // Print full trace
        }
    }

    private void giveWatk(Client c, int amount) {
        System.out.println("[DEBUG] inside giveWatk. Amount: " + amount); // DEBUG 16
        c.getPlayer().dropMessage(5, "Redeemed " + amount + " Weapon Attack!");
        System.out.println("[DEBUG] dropMessage sent."); // DEBUG 17
    }

    private void giveMatk(Client c, int amount) {
        System.out.println("[DEBUG] inside giveMatk. Amount: " + amount); // DEBUG 18
        c.getPlayer().dropMessage(5, "Redeemed " + amount + " Magic Attack!");
    }

    private void giveDef(Client c, int amount) {
        c.getPlayer().dropMessage(5, "Redeemed " + amount + " Weapon Def!");
    }

    private void giveEva(Client c, int amount) {
        c.getPlayer().dropMessage(5, "Redeemed " + amount + " Avoidability!");
    }

    private void giveAcc(Client c, int amount) {
        c.getPlayer().dropMessage(5, "Redeemed " + amount + " Accuracy!");
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