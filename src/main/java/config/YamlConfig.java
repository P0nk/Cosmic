package config;

import com.esotericsoftware.yamlbeans.YamlReader;
import constants.string.CharsetConstants;
import tools.EnvLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class YamlConfig {
    public static final String CONFIG_FILE_NAME = "config.yaml";
    public static final YamlConfig config = loadConfig();

    public java.util.List<WorldConfig> worlds;
    public ServerConfig server;

    private static YamlConfig loadConfig() {
        try {
            // 1) Load .env first
            EnvLoader.load();

            // 2) Load YAML
            YamlReader reader = new YamlReader(
                    Files.newBufferedReader(Path.of(CONFIG_FILE_NAME), CharsetConstants.CHARSET)
            );
            YamlConfig config = reader.read(YamlConfig.class);
            reader.close();

            // 3) Override selected fields from .env
            applyEnvOverrides(config);

            return config;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not successfully parse config file " + CONFIG_FILE_NAME + ": " + e.getMessage(), e
            );
        }
    }

    private static void applyEnvOverrides(YamlConfig config) {
        if (config == null || config.server == null) return;

        // IP Configuration
        config.server.HOST      = pick("HOST",      config.server.HOST);
        config.server.LANHOST   = pick("LANHOST",   config.server.LANHOST);
        config.server.LOCALHOST = pick("LOCALHOST", config.server.LOCALHOST);

        // Database
        config.server.DB_URL_FORMAT = pick("DB_URL_FORMAT", config.server.DB_URL_FORMAT);
        config.server.DB_HOST       = pick("DB_HOST",       config.server.DB_HOST);
        config.server.DB_USER       = pick("DB_USER",       config.server.DB_USER);
        config.server.DB_PASS       = pick("DB_PASS",       config.server.DB_PASS);

        // Optional numeric override
        String timeout = EnvLoader.get("INIT_CONNECTION_POOL_TIMEOUT");
        if (timeout != null) {
            try {
                config.server.INIT_CONNECTION_POOL_TIMEOUT = Integer.parseInt(timeout);
            } catch (NumberFormatException e) {
                System.err.println("[ENV] INIT_CONNECTION_POOL_TIMEOUT invalid: " + timeout);
            }
        }
    }


    private static String pick(String key, String fallback) {
        String v = EnvLoader.get(key);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }
}
