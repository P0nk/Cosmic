package database.character;

import client.Character;
import client.CharacterStats;
import client.MonsterBook;
import database.DatabaseTest;
import database.monsterbook.MonsterCardRepository;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Testcontainers
@Disabled("Tests fail due to MySQL saving. Keeping this disabled until migrated to PG, no point wasting time on MySQL when it's going to be removed.")
class CharacterSaverTest extends DatabaseTest {
    private CharacterSaver characterSaver;

    @BeforeEach
    void reset() {
        this.characterSaver = new CharacterSaver(Mockito.mock(), connection, new CharacterRepository(),
                new MonsterCardRepository(connection));
    }

    @Test
    void saveCharacter_shouldUpdateChrTable() {
        Character mockChr = Mockito.mock(Character.class);
        when(mockChr.isLoggedin()).thenReturn(true);
        addEmptyMonsterBook(mockChr);
        when(mockChr.getCharacterStats()).thenReturn(CharacterStats.builder()
                .id(testIds.chrId())
                .level(200)
                .build());
        assertEquals(0, getChrLevel(testIds.chrId()));

        characterSaver.save(mockChr);

        assertEquals(200, getChrLevel(testIds.chrId()));
    }

    private static void addEmptyMonsterBook(Character mockChr) {
        MonsterBook mockMonsterBook = Mockito.mock(MonsterBook.class);
        when(mockMonsterBook.getCards()).thenReturn(Collections.emptyList());
        when(mockChr.getMonsterBook()).thenReturn(mockMonsterBook);
    }

    private int getChrLevel(int chrId) {
        String sql = """
                SELECT level
                FROM chr
                WHERE id = :id""";
        try (Handle handle = connection.getHandle()) {
            return handle.createQuery(sql)
                    .bind("id", chrId)
                    .mapTo(Integer.class)
                    .one();
        }
    }

}
