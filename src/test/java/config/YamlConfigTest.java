package config;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import constants.string.CharsetConstants;
import tools.EnvironmentVariables;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class YamlConfigTest {
    private FileSystem fs;

    @BeforeEach
    void setUp() {
        fs = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    void tearDown() throws IOException {
        fs.close();
    }

    @Test
    void load_noEnvironmentVariables() throws IOException {
        int flag = 1;
        String serverMessage = "Hello World";
        int channels = 2;
        String dbHost = "localhost";
        String dbUser = "user";
        String dbPass = "pass";
        int worlds = 2;
        String yamlContent = String.format(
            """
            worlds:
              - flag: %d
                server_message: "%s"
                channels: %d
              - flag: 0
                server_message: "Another World"
                channels: 1
            server:
              DB_HOST: "%s"
              DB_USER: "%s"
              DB_PASS: "%s"
              WORLDS: %d
            """,
            flag, serverMessage, channels, dbHost, dbUser, dbPass, worlds
        );
        Path configPath = fs.getPath("/config.yaml");
        Files.writeString(configPath, yamlContent, CharsetConstants.CHARSET);

        YamlConfig config = YamlConfig.load(configPath);

        assertNotNull(config.worlds);
        assertEquals(2, config.worlds.size());
        assertEquals(1, config.worlds.get(0).flag);
        assertEquals(serverMessage, config.worlds.get(0).server_message);
        assertEquals(channels, config.worlds.get(0).channels);

        assertNotNull(config.server);
        assertEquals(dbHost, config.server.DB_HOST);
        assertEquals(dbUser, config.server.DB_USER);
        assertEquals(dbPass, config.server.DB_PASS);
        assertEquals(worlds, config.server.WORLDS);
    }

    @Test
    void load_replaceEnvironmentVariables() throws IOException {
        String dbUser = "user";
        String dbPass = "pass";
        mockEnvironmentVariables(Map.of(
            "DB_USER", dbUser,
            "DB_PASS", dbPass
        ));

        String yamlContent = 
            """
            server:
              DB_USER: "${DB_USER}"
              DB_PASS: "${DB_PASS}"
            """;
        Path configPath = fs.getPath("/config.yaml");
        Files.writeString(configPath, yamlContent, CharsetConstants.CHARSET);

        YamlConfig config = YamlConfig.load(configPath);
        assertNotNull(config.server);
        assertEquals(dbUser, config.server.DB_USER);
        assertEquals(dbPass, config.server.DB_PASS);
    }

    private void mockEnvironmentVariables(Map<String, String> envVars) {
        EnvironmentVariables mock = Mockito.mock(EnvironmentVariables.class);
        Mockito.when(mock.getAll()).thenReturn(envVars);
        EnvironmentVariables.setInstance(mock);
    }

    @Test
    void load_useDefaultValueForEnvironmentVariablesNotPresent() throws IOException {
        EnvironmentVariables.setInstance(null);

        String dbUser = "user";
        String dbPass = "pass";
        String yamlContent = String.format(
            """
            server:
              DB_USER: "${DB_USER:%s}"
              DB_PASS: "${DB_PASS:%s}"
            """,
            dbUser, dbPass
        );
        Path configPath = fs.getPath("/config.yaml");
        Files.writeString(configPath, yamlContent, CharsetConstants.CHARSET);

        YamlConfig config = YamlConfig.load(configPath);
        assertNotNull(config.server);
        assertEquals(dbUser, config.server.DB_USER);
        assertEquals(dbPass, config.server.DB_PASS);
    }

    @Test
    void load_throwExceptionWhenFileDoesNotExist() {
        Path configPath = fs.getPath("/config.yaml");

        assertThrows(RuntimeException.class, () -> {
            YamlConfig.load(configPath);
        });
    }

    @Test
    void load_throwExceptionWhenFailedToParseConfig() throws IOException {
        Path configPath = fs.getPath("/config.yaml");
        Files.writeString(configPath, "invalid_field: value", CharsetConstants.CHARSET);

        assertThrows(RuntimeException.class, () -> {
            YamlConfig.load(configPath);
        });
    }
}
