package client;

import database.monsterbook.MonsterCard;
import net.jcip.annotations.ThreadSafe;
import tools.PacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// TODO: add tests
@ThreadSafe
public class MonsterBook {
    private final Map<Integer, MonsterCard> cards;
    private int bookLevel;

    public MonsterBook(List<MonsterCard> monsterCards) {
        this.cards = monsterCards.stream()
                .collect(Collectors.toMap(MonsterCard::cardId, Function.identity()));
    }

    public synchronized List<MonsterCard> getCards() {
        return new ArrayList<>(cards.values());
    }

    public synchronized void addCard(int cardId, Client client) {
        var monsterCard = cards.get(cardId);
        if (monsterCard != null && monsterCard.isMaxLevel()) {
            client.sendPacket(PacketCreator.addMonsterCardAlreadyFull());
            return;
        }

        boolean isNewCard = monsterCard == null;
        final MonsterCard card;
        if (isNewCard) {
            card = new MonsterCard(cardId, (byte) 1);
            cards.put(cardId, card);
            calculateAndSetLevel();
        } else {
            card = new MonsterCard(cardId, (byte) (monsterCard.level() + 1));
            cards.put(cardId, card);
        }

        var chr = client.getPlayer();
        chr.sendPacket(PacketCreator.addMonsterCard(card));
        chr.sendPacket(PacketCreator.showMonsterCardEffect());
        chr.getMap().broadcastMessage(chr, PacketCreator.showForeignMonsterCardEffect(chr.getId()), false);
    }

    private synchronized void calculateAndSetLevel() {
        int collectionExp = getTotalCards();

        int level = 0;
        int expToNextLevel = 1;
        do {
            level++;
            expToNextLevel += level * 10;
        } while (collectionExp >= expToNextLevel);

        this.bookLevel = level;
    }

    public synchronized int getBookLevel() {
        return bookLevel;
    }

    public synchronized int getNormalCards() {
        return (int) cards.values().stream()
                .filter(Predicate.not(MonsterCard::isSpecial))
                .count();
    }

    public synchronized int getSpecialCards() {
        return (int) cards.values().stream()
                .filter(MonsterCard::isSpecial)
                .count();
    }

    public synchronized int getTotalCards() {
        return cards.size();
    }

    public synchronized void saveCards(Connection con, int chrId) throws SQLException {
        final String query = """
                INSERT INTO monsterbook (charid, cardid, level)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE level = ?;
                """;
        try (final PreparedStatement ps = con.prepareStatement(query)) {
            for (MonsterCard card : cards.values()) {
                // insert
                ps.setInt(1, chrId);
                ps.setInt(2, card.cardId());
                ps.setInt(3, card.level());

                // update
                ps.setInt(4, card.level());

                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
