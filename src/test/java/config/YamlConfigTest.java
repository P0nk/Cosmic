package config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class YamlConfigTest {
    private static final String BASE = "server:\n  DB_HOST: \"localhost\"\n  DB_USER: \"root\"\n  DB_PASS: \"\"\n  USE_DEBUG: true\n";

    private Path write(Path dir, String name, String content) throws Exception {
        Path p = dir.resolve(name);
        Files.writeString(p, content);
        return p;
    }

    @Test
    void localOverridesDbPassAndKeepsOtherKeys(@TempDir Path dir) throws Exception {
        Path base = write(dir, "config.yaml", BASE);
        Path local = write(dir, "config.local.yaml", "server:\n  DB_PASS: \"secret\"\n");
        YamlConfig c = YamlConfig.load(base, local, null);
        assertEquals("secret", c.server.DB_PASS);
        assertEquals("localhost", c.server.DB_HOST);
        assertEquals("root", c.server.DB_USER);
        assertTrue(c.server.USE_DEBUG);
    }

    @Test
    void missingLocalFileLeavesConfigUnchanged(@TempDir Path dir) throws Exception {
        Path base = write(dir, "config.yaml", BASE);
        YamlConfig c = YamlConfig.load(base, dir.resolve("nope.yaml"), null);
        assertEquals("", c.server.DB_PASS);
        assertEquals("localhost", c.server.DB_HOST);
        assertNull(YamlConfig.findLocalConfig(null, dir, null));
    }

    @Test
    void envPasswordWinsOverLocalFile(@TempDir Path dir) throws Exception {
        Path base = write(dir, "config.yaml", BASE);
        Path local = write(dir, "config.local.yaml", "server:\n  DB_PASS: \"fromfile\"\n");
        assertEquals("fromenv", YamlConfig.load(base, local, "fromenv").server.DB_PASS);
    }

    @Test
    void lookupOrder(@TempDir Path dir) throws Exception {
        Path wd = Files.createDirectory(dir.resolve("wd"));
        Path main = Files.createDirectory(dir.resolve("main"));
        Path mainFile = write(main, "config.local.yaml", "a: b\n");
        assertEquals(mainFile, YamlConfig.findLocalConfig(null, wd, main));
        Path wdFile = write(wd, "config.local.yaml", "a: b\n");
        assertEquals(wdFile, YamlConfig.findLocalConfig(null, wd, main));
        Path envFile = write(dir, "env.yaml", "a: b\n");
        assertEquals(envFile, YamlConfig.findLocalConfig(envFile.toString(), wd, main));
    }
}
