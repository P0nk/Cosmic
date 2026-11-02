package config;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import constants.string.CharsetConstants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class YamlConfig {
    public static final String CONFIG_FILE_NAME = "config.yaml";
    public static final String LOCAL_CONFIG_FILE_NAME = "config.local.yaml";
    public static final String LOCAL_CONFIG_ENV = "COSMIC_LOCAL_CONFIG";
    public static final String DB_PASS_ENV = "COSMIC_DB_PASS";
    public static final YamlConfig config = loadConfig();

    public List<WorldConfig> worlds;
    public ServerConfig server;

    private static YamlConfig loadConfig() {
        try {
            Path local = findLocalConfig(System.getenv(LOCAL_CONFIG_ENV), Path.of("").toAbsolutePath(), findMainCheckout());
            return load(Path.of(CONFIG_FILE_NAME), local, System.getenv(DB_PASS_ENV));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not read config file " + YamlConfig.CONFIG_FILE_NAME + ": " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Could not successfully parse config file " + YamlConfig.CONFIG_FILE_NAME + ": " + e.getMessage());
        }
    }

    /**
     * Loads the base config, merges the optional local override file over it key-by-key (nested maps merged),
     * then applies the optional DB password override. Values are never logged.
     */
    @SuppressWarnings("unchecked")
    static YamlConfig load(Path base, Path localOverride, String dbPassOverride) throws IOException {
        Object baseTree = readTree(base);
        if (localOverride != null && Files.isRegularFile(localOverride)) {
            Object localTree = readTree(localOverride);
            if (baseTree instanceof Map && localTree instanceof Map) {
                mergeInto((Map<String, Object>) baseTree, (Map<String, Object>) localTree);
            }
        }
        if (dbPassOverride != null && baseTree instanceof Map) {
            Object server = ((Map<String, Object>) baseTree).get("server");
            if (!(server instanceof Map)) {
                server = new LinkedHashMap<String, Object>();
                ((Map<String, Object>) baseTree).put("server", server);
            }
            ((Map<String, Object>) server).put("DB_PASS", dbPassOverride);
        }

        StringWriter sw = new StringWriter();
        YamlWriter writer = new YamlWriter(sw);
        writer.getConfig().writeConfig.setWriteRootTags(false);
        writer.getConfig().writeConfig.setWriteClassname(com.esotericsoftware.yamlbeans.YamlConfig.WriteClassName.NEVER);
        writer.write(baseTree);
        writer.close();

        YamlReader reader = new YamlReader(new StringReader(sw.toString()));
        try {
            return reader.read(YamlConfig.class);
        } finally {
            reader.close();
        }
    }

    private static Object readTree(Path path) throws IOException {
        try (YamlReader reader = new YamlReader(Files.newBufferedReader(path, CharsetConstants.CHARSET))) {
            return reader.read();
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeInto(Map<String, Object> base, Map<String, Object> override) {
        for (Map.Entry<String, Object> e : override.entrySet()) {
            Object existing = base.get(e.getKey());
            if (existing instanceof Map && e.getValue() instanceof Map) {
                mergeInto((Map<String, Object>) existing, (Map<String, Object>) e.getValue());
            } else {
                base.put(e.getKey(), e.getValue());
            }
        }
    }

    /** Order: env path, working dir, main repo checkout. Returns null if none exists. */
    static Path findLocalConfig(String envPath, Path workingDir, Path mainCheckout) {
        if (envPath != null && !envPath.isBlank()) {
            Path p = Path.of(envPath);
            return Files.isRegularFile(p) ? p : null;
        }
        Path p = workingDir.resolve(LOCAL_CONFIG_FILE_NAME);
        if (Files.isRegularFile(p)) {
            return p;
        }
        if (mainCheckout != null) {
            p = mainCheckout.resolve(LOCAL_CONFIG_FILE_NAME);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    /** Main checkout = parent of the git common dir (resolves worktrees to the primary repo). Null if unknown. */
    private static Path findMainCheckout() {
        try {
            Process proc = new ProcessBuilder("git", "rev-parse", "--path-format=absolute", "--git-common-dir")
                    .redirectErrorStream(true).start();
            String out = new String(proc.getInputStream().readAllBytes(), CharsetConstants.CHARSET).trim();
            if (!proc.waitFor(5, TimeUnit.SECONDS) || proc.exitValue() != 0 || out.isEmpty()) {
                return null;
            }
            return Path.of(out).getParent();
        } catch (Exception e) {
            return null;
        }
    }
}
