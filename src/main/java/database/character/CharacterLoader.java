package database.character;

import client.Character;
import client.Client;
import client.MonsterBook;
import database.monsterbook.MonsterCardDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

public class CharacterLoader {
    private static final Logger log = LoggerFactory.getLogger(CharacterLoader.class);
    private final MonsterCardDao monsterCardDao;

    public CharacterLoader(MonsterCardDao monsterCardDao) {
        this.monsterCardDao = monsterCardDao;
    }

    public Optional<Character> loadForChannel(int chrId, Client client) {
        var monsterBook = loadMonsterBook(chrId);

        final Character chr;
        try {
            chr = Character.loadCharFromDB(chrId, client, true, monsterBook);
        } catch (SQLException e) {
            log.warn("Failed to load character (id {})", chrId, e);
            return Optional.empty();
        }

        return Optional.ofNullable(chr);
    }

    private MonsterBook loadMonsterBook(int chrId) {
        var monsterCards = monsterCardDao.load(chrId);
        return new MonsterBook(monsterCards);
    }
}
