package config;

import com.esotericsoftware.yamlbeans.YamlReader;
import constants.string.CharsetConstants;
import tools.EnvironmentVariables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.text.StringSubstitutor;


public class YamlConfig {
    public static final String CONFIG_FILE_NAME = "config.yaml";
    public static final YamlConfig config = load(Path.of(CONFIG_FILE_NAME));

    public List<WorldConfig> worlds;
    public ServerConfig server;

    static YamlConfig load(Path path) {
        String content = replaceEnvironmentVariables(readFile(path));

        try (YamlReader reader = new YamlReader(content)) {
            return reader.read(YamlConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to parse config file " + path.toString()
                + ": " + e.getMessage()
            );
        }
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path, CharsetConstants.CHARSET);
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to read config file " + path.toString()
                + ": " + e.getMessage()
            );
        }
    }

    private static String replaceEnvironmentVariables(String content) {
        return new StringSubstitutor(EnvironmentVariables.instance().getAll())
            .setValueDelimiter(':')
            .replace(content);
    }
}
