package server.life;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobSkillFactoryTest {

    @TempDir
    static Path wzPath;

    @BeforeAll
    static void setWzPath() {
        System.setProperty("wz-path", "%s/wz".formatted(wzPath.toString()));
        writeTestFileToTempDir();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static void writeTestFileToTempDir() {
        try {
            String testFileContents = readTestFileContents();
            writeTempDirFile(testFileContents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readTestFileContents() throws IOException {
        return new String(MobSkillFactoryTest.class
                .getClassLoader()
                .getResourceAsStream("MobSkill-test.img.xml")
                .readAllBytes()
        );
    }

    private static void writeTempDirFile(String fileContents) throws IOException {
        Path tempDirDirectory = wzPath.resolve("wz/Skill.wz");
        Files.createDirectories(tempDirDirectory);
        Path tempDirFile = Files.createFile(tempDirDirectory.resolve("MobSkill.img.xml"));
        Files.writeString(tempDirFile, fileContents);
    }

    @Test
    void shouldLoadExistingMobSkill() {
        Optional<MobSkill> possibleSkill = MobSkillFactory.getMobSkill(MobSkillType.ATTACK_UP, 1);

        assertTrue(possibleSkill.isPresent());
        MobSkill mobSkill = possibleSkill.get();
        assertAll("MobSkill",
                () -> assertEquals(115, mobSkill.getX()),
                () -> assertEquals(5, mobSkill.getMpCon()),
                () -> assertEquals(40_000, mobSkill.getCoolTime()),
                () -> assertEquals(30_000, mobSkill.getDuration())
        );
    }

    @Test
    void shouldThrowExceptionOnNonExisting() {
        assertThrows(IllegalArgumentException.class, () -> MobSkillFactory.getMobSkillOrThrow(MobSkillType.DEFENSE_UP, 1));
    }

}
