package database.character;

import client.Character;
import database.monsterbook.MonsterCardDao;

public class CharacterSaver {
    private final MonsterCardDao monsterCardDao;

    public CharacterSaver(MonsterCardDao monsterCardDao) {
        this.monsterCardDao = monsterCardDao;
    }

    public void save(Character chr) {
        chr.saveCharToDB(false);

        // Saving monster cards to both MySQL and Postgres for now
        monsterCardDao.save(chr.getId(), chr.getMonsterBook().getCards());
    }

}
